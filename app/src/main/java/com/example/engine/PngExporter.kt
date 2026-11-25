package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.MapLayerOption
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object PngExporter {

    data class ExportResult(
        val uri: Uri?,
        val displayPath: String,
        val width: Int,
        val height: Int
    )

    /**
     * Exports ONLY the selected plots (or the whole sheet when `selectedOnly == false`)
     * directly from vector PDF / high-res image source into a crisp Ultra-HD PNG.
     */
    suspend fun exportSelectedMouzaPlotsPng(
        context: Context,
        sourceBitmap: Bitmap,
        plots: List<MouzaPlot>,
        sqFeetPerGridPixel: Double,
        khatianTitle: String,
        docFile: File? = null,
        isPdf: Boolean = true,
        pageIndex: Int = 0,
        pageWidthPts: Int = 1000,
        pageHeightPts: Int = 1000,
        scale: MouzaScale = MouzaScale.SCALE_16,
        calibrationMultiplier: Double = 1.0,
        selectedOnly: Boolean = true
    ): ExportResult = withContext(Dispatchers.Default) {
        if (selectedOnly) {
            require(plots.isNotEmpty()) { "কোনো দাগ সিলেক্ট করা হয়নি।" }
        }

        // Compute normalized bounding box [nLeft, nTop, nRight, nBottom] across selected plots
        var nMinX = 1f
        var nMinY = 1f
        var nMaxX = 0f
        var nMaxY = 0f

        if (!selectedOnly || plots.isEmpty()) {
            nMinX = 0f; nMinY = 0f; nMaxX = 1f; nMaxY = 1f
        } else {
            for (p in plots) {
                val poly = p.effectivePolygon()
                if (!poly.isNullOrEmpty()) {
                    for ((nx, ny) in poly) {
                        nMinX = min(nMinX, nx)
                        nMinY = min(nMinY, ny)
                        nMaxX = max(nMaxX, nx)
                        nMaxY = max(nMaxY, ny)
                    }
                } else {
                    val gw = p.gridWidth.toFloat().coerceAtLeast(1f)
                    val gh = p.gridHeight.toFloat().coerceAtLeast(1f)
                    nMinX = min(nMinX, p.minX / gw)
                    nMinY = min(nMinY, p.minY / gh)
                    nMaxX = max(nMaxX, (p.maxX + 1) / gw)
                    nMaxY = max(nMaxY, (p.maxY + 1) / gh)
                }
            }
            val span = max(nMaxX - nMinX, nMaxY - nMinY).coerceAtLeast(0.02f)
            val pad = (span * 0.08f).coerceIn(0.01f, 0.06f)
            nMinX = (nMinX - pad).coerceIn(0f, 0.98f)
            nMinY = (nMinY - pad).coerceIn(0f, 0.98f)
            nMaxX = (nMaxX + pad).coerceIn(nMinX + 0.02f, 1f)
            nMaxY = (nMaxY + pad).coerceIn(nMinY + 0.02f, 1f)
        }

        val spanW = (nMaxX - nMinX).coerceAtLeast(0.01f)
        val spanH = (nMaxY - nMinY).coerceAtLeast(0.01f)
        val worldAspect = (spanH * pageHeightPts.coerceAtLeast(100)) /
            (spanW * pageWidthPts.coerceAtLeast(100))

        val targetMaxPx = 1800
        val mapOutW: Int
        val mapOutH: Int
        if (worldAspect <= 1f) {
            mapOutW = targetMaxPx
            mapOutH = (targetMaxPx * worldAspect).roundToInt().coerceIn(420, 2200)
        } else {
            mapOutH = targetMaxPx
            mapOutW = (targetMaxPx / worldAspect).roundToInt().coerceIn(420, 2200)
        }

        // Render high-res cropped region directly from PDF/image when available
        val hdTile = if (docFile != null) {
            MouzaPdfEngine.renderViewportTile(
                docFile = docFile,
                isPdf = isPdf,
                pageIndex = pageIndex,
                normLeft = nMinX,
                normTop = nMinY,
                normRight = nMaxX,
                normBottom = nMaxY,
                targetWidthPx = mapOutW,
                targetHeightPx = mapOutH
            )
        } else null

        val usableHdTile = if (hdTile != null && hasInkPixels(hdTile.bitmap)) {
            hdTile
        } else {
            hdTile?.bitmap?.recycle()
            null
        }

        val actualW = usableHdTile?.bitmap?.width ?: mapOutW
        val actualH = usableHdTile?.bitmap?.height ?: mapOutH
        val footerHeight = 175
        val outBitmap = Bitmap.createBitmap(actualW, actualH + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawColor(Color.WHITE)

        val layerBitmap = Bitmap.createBitmap(actualW, actualH, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)
        if (usableHdTile != null) {
            layerCanvas.drawBitmap(
                usableHdTile.bitmap,
                Rect(0, 0, usableHdTile.bitmap.width, usableHdTile.bitmap.height),
                RectF(0f, 0f, actualW.toFloat(), actualH.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            usableHdTile.bitmap.recycle()
        } else {
            val srcRect = Rect(
                (nMinX * sourceBitmap.width).toInt().coerceIn(0, sourceBitmap.width - 2),
                (nMinY * sourceBitmap.height).toInt().coerceIn(0, sourceBitmap.height - 2),
                (nMaxX * sourceBitmap.width).roundToInt().coerceIn(2, sourceBitmap.width),
                (nMaxY * sourceBitmap.height).roundToInt().coerceIn(2, sourceBitmap.height)
            )
            layerCanvas.drawBitmap(
                sourceBitmap,
                srcRect,
                RectF(0f, 0f, actualW.toFloat(), actualH.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
        }

        // If selectedOnly, clip to selected plots so exterior clutter is masked cleanly
        if (selectedOnly && plots.isNotEmpty()) {
            val maskBitmap = Bitmap.createBitmap(actualW, actualH, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskBitmap)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            for (plot in plots) {
                val poly = plot.effectivePolygon()
                if (poly != null && poly.size >= 3) {
                    val path = Path()
                    poly.forEachIndexed { idx, (nx, ny) ->
                        val px = ((nx - nMinX) / spanW) * actualW
                        val py = ((ny - nMinY) / spanH) * actualH
                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    maskCanvas.drawPath(path, maskPaint)
                } else {
                    val gw = plot.gridWidth.toFloat().coerceAtLeast(1f)
                    val gh = plot.gridHeight.toFloat().coerceAtLeast(1f)
                    val cellW = (actualW / (gw * spanW)).coerceAtLeast(1f)
                    val cellH = (actualH / (gh * spanH)).coerceAtLeast(1f)
                    for (idx in plot.maskIndices) {
                        val gx = idx % plot.gridWidth
                        val gy = idx / plot.gridWidth
                        val px = (((gx / gw) - nMinX) / spanW) * actualW
                        val py = (((gy / gh) - nMinY) / spanH) * actualH
                        maskCanvas.drawRect(px - 0.5f, py - 0.5f, px + cellW + 0.5f, py + cellH + 0.5f, maskPaint)
                    }
                }
            }

            val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            layerCanvas.drawBitmap(maskBitmap, 0f, 0f, xferPaint)
            maskBitmap.recycle()
        }

        canvas.drawBitmap(layerBitmap, 0f, 0f, null)
        layerBitmap.recycle()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeJoin = Paint.Join.ROUND
        }
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val edgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val edgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        var totalSqFt = 0.0
        for (plot in plots) {
            val plotSqFt = plot.exactAreaSqFeet ?: (plot.pixelCount * sqFeetPerGridPixel)
            totalSqFt += plotSqFt

            val argb = Color.argb(
                (plot.color.alpha * 255).toInt(),
                (plot.color.red * 255).toInt(),
                (plot.color.green * 255).toInt(),
                (plot.color.blue * 255).toInt()
            )
            tintPaint.color = Color.argb(46, Color.red(argb), Color.green(argb), Color.blue(argb))
            borderPaint.color = argb
            edgeTextPaint.color = argb

            var centerX = 0f
            var centerY = 0f

            val poly = plot.effectivePolygon()
            if (poly != null && poly.size >= 3) {
                val path = Path()
                var sumX = 0f
                var sumY = 0f
                val screenPts = poly.map { (nx, ny) ->
                    val px = ((nx - nMinX) / spanW) * actualW
                    val py = ((ny - nMinY) / spanH) * actualH
                    sumX += px; sumY += py
                    px to py
                }
                screenPts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, tintPaint)
                canvas.drawPath(path, borderPaint)

                centerX = plot.centroidNormalized?.let { ((it.first - nMinX) / spanW) * actualW }
                    ?: (sumX / screenPts.size)
                centerY = plot.centroidNormalized?.let { ((it.second - nMinY) / spanH) * actualH }
                    ?: (sumY / screenPts.size)

                // Draw side edge lengths in feet along edges long enough to read cleanly
                for (i in poly.indices) {
                    val pA = poly[i]
                    val pB = poly[(i + 1) % poly.size]
                    val sA = screenPts[i]
                    val sB = screenPts[(i + 1) % screenPts.size]
                    val pxLen = hypot(sB.first - sA.first, sB.second - sA.second)
                    if (pxLen >= 58f) {
                        val ft = MouzaPdfEngine.computeDistanceFeet(
                            pA, pB, pageWidthPts, pageHeightPts, scale, calibrationMultiplier
                        )
                        val label = LandCalculators.formatCompactFeet(ft)
                        var ang = Math.toDegrees(atan2((sB.second - sA.second).toDouble(), (sB.first - sA.first).toDouble())).toFloat()
                        if (ang > 90f) ang -= 180f
                        if (ang < -90f) ang += 180f
                        val mx = (sA.first + sB.first) / 2f
                        val my = (sA.second + sB.second) / 2f
                        canvas.save()
                        canvas.translate(mx, my)
                        canvas.rotate(ang)
                        canvas.drawText(label, 0f, -6f, edgeStrokePaint)
                        canvas.drawText(label, 0f, -6f, edgeTextPaint)
                        canvas.restore()
                    }
                }
            } else {
                val gw = plot.gridWidth.toFloat().coerceAtLeast(1f)
                val gh = plot.gridHeight.toFloat().coerceAtLeast(1f)
                centerX = ((((plot.minX + plot.maxX) * 0.5f / gw) - nMinX) / spanW) * actualW
                centerY = ((((plot.minY + plot.maxY) * 0.5f / gh) - nMinY) / spanH) * actualH
            }

            val area = LandCalculators.fromSqFeet(plotSqFt)
            val tag = buildString {
                if (plot.dagNumber.isNotBlank()) append("দাগ ${plot.dagNumber} • ")
                else append("#${plot.plotIndex} • ")
                append(String.format(Locale.US, "%.2f শতক", area.decimal))
            }
            val tw = labelPaint.measureText(tag)
            pillPaint.color = Color.argb(220, Color.red(argb), Color.green(argb), Color.blue(argb))
            val pillRect = RectF(centerX - tw / 2f - 16f, centerY - 24f, centerX + tw / 2f + 16f, centerY + 14f)
            canvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
            canvas.drawText(tag, centerX, centerY + 3f, labelPaint)
        }

        val totalArea = LandCalculators.fromSqFeet(totalSqFt)
        drawFooterStamp(
            canvas = canvas,
            width = actualW,
            topY = actualH.toFloat(),
            footerHeight = footerHeight.toFloat(),
            headerText = if (khatianTitle.isNotBlank()) "Bhumi Survey BD • $khatianTitle" else "Bhumi Survey BD • Selected Mouza Plots (${plots.size})",
            line1 = String.format(
                Locale.US,
                "মোট: %.2f শতাংশ (Decimal)  |  %.2f কাঠা  |  %.3f বিঘা",
                totalArea.decimal, totalArea.katha, totalArea.bigha
            ),
            line2 = String.format(
                Locale.US,
                "%.1f বর্গফুট (Sq.Ft)  |  %.1f বর্গমিটার (Sq.M)  |  %.4f একর",
                totalArea.sqFeet, totalArea.sqMeters, totalArea.acre
            )
        )

        val fileName = "Mouza_${if (selectedOnly) "Selected" else "Full"}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    /**
     * Renders a crisp Mouza-style selected plot PNG for RealMapScreen by directly fetching
     * the map tiles at optimal zoom, masking/highlighting the selected plot, and drawing
     * side lengths in feet + center area pill + emerald summary footer.
     */
    suspend fun exportSelectedMapPlotPng(
        context: Context,
        pointsLatLon: List<Pair<Double, Double>>,
        mode: String,
        layer: MapLayerOption,
        showLabelsOverlay: Boolean,
        areaSqMeters: Double,
        fallbackBitmap: Bitmap?,
        fallbackCropRect: RectF?,
        summaryHeader: String,
        summaryLine1: String,
        summaryLine2: String
    ): ExportResult = withContext(Dispatchers.IO) {
        if (pointsLatLon.isEmpty()) {
            return@withContext exportSelectedMapRegionPng(
                context = context,
                fullMapBitmap = fallbackBitmap ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888),
                cropRectNormalized = fallbackCropRect,
                summaryHeader = summaryHeader,
                summaryLine1 = summaryLine1,
                summaryLine2 = summaryLine2
            )
        }

        fun ll2m(lat: Double, lon: Double): Pair<Double, Double> {
            val s = sin(Math.toRadians(lat.coerceIn(-85.0511, 85.0511)))
            val mx = (lon + 180.0) / 360.0
            val my = 0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)
            return mx to my
        }

        val mercPts = pointsLatLon.map { (lat, lon) -> ll2m(lat, lon) }

        var minMx = mercPts.minOf { it.first }
        var maxMx = mercPts.maxOf { it.first }
        var minMy = mercPts.minOf { it.second }
        var maxMy = mercPts.maxOf { it.second }

        if (mode == "circle" && mercPts.size >= 2) {
            val rM = hypot(mercPts[1].first - mercPts[0].first, mercPts[1].second - mercPts[0].second)
            minMx = mercPts[0].first - rM
            maxMx = mercPts[0].first + rM
            minMy = mercPts[0].second - rM
            maxMy = mercPts[0].second + rM
        }

        val rawSpanX = (maxMx - minMx).coerceAtLeast(1e-6)
        val rawSpanY = (maxMy - minMy).coerceAtLeast(1e-6)
        val pad = max(rawSpanX, rawSpanY) * 0.16
        val boxMinX = (minMx - pad).coerceIn(0.0, 0.999999)
        val boxMinY = (minMy - pad).coerceIn(0.0, 0.999999)
        val boxMaxX = (maxMx + pad).coerceIn(boxMinX + 1e-6, 1.0)
        val boxMaxY = (maxMy + pad).coerceIn(boxMinY + 1e-6, 1.0)

        val spanX = boxMaxX - boxMinX
        val spanY = boxMaxY - boxMinY
        val aspect = (spanY / spanX).coerceIn(0.48, 2.0)

        val maxDim = 1500
        val outW: Int
        val outH: Int
        if (aspect <= 1.0) {
            outW = maxDim
            outH = (maxDim * aspect).roundToInt().coerceIn(540, maxDim)
        } else {
            outH = maxDim
            outW = (maxDim / aspect).roundToInt().coerceIn(540, maxDim)
        }

        // Choose crisp native tile zoom level covering the full plot within 36 tiles
        val idealZ = log2(outW / (256.0 * spanX))
        var tileZ = floor(idealZ).toInt().coerceIn(4, layer.maxNativeZoom)
        var nTiles = 2.0.pow(tileZ.toDouble()).toInt()
        var txMin = floor(boxMinX * nTiles).toInt().coerceIn(0, nTiles - 1)
        var txMax = floor(boxMaxX * nTiles).toInt().coerceIn(txMin, nTiles - 1)
        var tyMin = floor(boxMinY * nTiles).toInt().coerceIn(0, nTiles - 1)
        var tyMax = floor(boxMaxY * nTiles).toInt().coerceIn(tyMin, nTiles - 1)
        while ((txMax - txMin + 1) * (tyMax - tyMin + 1) > 36 && tileZ > 4) {
            tileZ--
            nTiles = 2.0.pow(tileZ.toDouble()).toInt()
            txMin = floor(boxMinX * nTiles).toInt().coerceIn(0, nTiles - 1)
            txMax = floor(boxMaxX * nTiles).toInt().coerceIn(txMin, nTiles - 1)
            tyMin = floor(boxMinY * nTiles).toInt().coerceIn(0, nTiles - 1)
            tyMax = floor(boxMaxY * nTiles).toInt().coerceIn(tyMin, nTiles - 1)
        }

        val mapLayerBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val mapLayerCanvas = Canvas(mapLayerBitmap)
        mapLayerCanvas.drawColor(Color.parseColor("#EBF2EE"))

        val baseTileCoords = buildList {
            for (ty in tyMin..tyMax) {
                for (tx in txMin..txMax) {
                    add(tx to ty)
                }
            }
        }.take(36)

        var fetchedCount = 0
        val baseTiles = coroutineScope {
            baseTileCoords.map { (tx, ty) ->
                async {
                    val urlStr = layer.tileUrl
                        .replace("{z}", tileZ.toString())
                        .replace("{x}", tx.toString())
                        .replace("{y}", ty.toString())
                    Triple(tx, ty, downloadTileBitmap(urlStr))
                }
            }.awaitAll()
        }

        val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG)
        for ((tx, ty, bmp) in baseTiles) {
            if (bmp != null) {
                fetchedCount++
                val tMinX = tx.toDouble() / nTiles
                val tMaxX = (tx + 1).toDouble() / nTiles
                val tMinY = ty.toDouble() / nTiles
                val tMaxY = (ty + 1).toDouble() / nTiles
                val dst = RectF(
                    (((tMinX - boxMinX) / spanX) * outW).toFloat(),
                    (((tMinY - boxMinY) / spanY) * outH).toFloat(),
                    (((tMaxX - boxMinX) / spanX) * outW).toFloat() + 1f,
                    (((tMaxY - boxMinY) / spanY) * outH).toFloat() + 1f
                )
                mapLayerCanvas.drawBitmap(bmp, null, dst, tilePaint)
                bmp.recycle()
            }
        }

        // Optional Esri road/place label overlay
        if (fetchedCount > 0 && layer.supportsLabelOverlay && showLabelsOverlay) {
            val refZ = min(tileZ, 18)
            val refN = 2.0.pow(refZ.toDouble()).toInt()
            val rxMin = floor(boxMinX * refN).toInt().coerceIn(0, refN - 1)
            val rxMax = floor(boxMaxX * refN).toInt().coerceIn(rxMin, refN - 1)
            val ryMin = floor(boxMinY * refN).toInt().coerceIn(0, refN - 1)
            val ryMax = floor(boxMaxY * refN).toInt().coerceIn(ryMin, refN - 1)
            val refCoords = buildList {
                for (ry in ryMin..ryMax) {
                    for (rx in rxMin..rxMax) {
                        add(rx to ry)
                    }
                }
            }.take(25)
            val roadsUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}"
            val placesUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
            val overlays = coroutineScope {
                refCoords.flatMap { (rx, ry) ->
                    listOf(
                        async {
                            Triple(rx, ry, downloadTileBitmap(roadsUrl.replace("{z}", "$refZ").replace("{x}", "$rx").replace("{y}", "$ry")))
                        },
                        async {
                            Triple(rx, ry, downloadTileBitmap(placesUrl.replace("{z}", "$refZ").replace("{x}", "$rx").replace("{y}", "$ry")))
                        }
                    )
                }.awaitAll()
            }
            for ((rx, ry, bmp) in overlays) {
                if (bmp != null) {
                    val dst = RectF(
                        ((((rx.toDouble() / refN) - boxMinX) / spanX) * outW).toFloat(),
                        ((((ry.toDouble() / refN) - boxMinY) / spanY) * outH).toFloat(),
                        (((((rx + 1).toDouble() / refN) - boxMinX) / spanX) * outW).toFloat() + 1f,
                        (((((ry + 1).toDouble() / refN) - boxMinY) / spanY) * outH).toFloat() + 1f
                    )
                    mapLayerCanvas.drawBitmap(bmp, null, dst, tilePaint)
                    bmp.recycle()
                }
            }
        }

        // Offline fallback: use captured WebView bitmap if tile fetch returned nothing
        if (fetchedCount == 0 && fallbackBitmap != null && hasInkPixels(fallbackBitmap, excludeDarkCanvas = true)) {
            val bw = fallbackBitmap.width
            val bh = fallbackBitmap.height
            val crop = if (fallbackCropRect != null) {
                Rect(
                    (fallbackCropRect.left * bw).toInt().coerceIn(0, bw - 10),
                    (fallbackCropRect.top * bh).toInt().coerceIn(0, bh - 10),
                    (fallbackCropRect.right * bw).toInt().coerceIn(10, bw),
                    (fallbackCropRect.bottom * bh).toInt().coerceIn(10, bh)
                )
            } else {
                Rect(0, 0, bw, bh)
            }
            mapLayerCanvas.drawBitmap(fallbackBitmap, crop, RectF(0f, 0f, outW.toFloat(), outH.toFloat()), tilePaint)
        }
        fallbackBitmap?.recycle()

        val screenPts = mercPts.map { (mx, my) ->
            (((mx - boxMinX) / spanX) * outW).toFloat() to (((my - boxMinY) / spanY) * outH).toFloat()
        }

        val isClosedShape = (mode == "circle" && screenPts.size >= 2) || (mode != "distance" && screenPts.size >= 3)

        // Mask exterior cleanly to pure white just like Mouza Noksha cutout when closed plot is selected
        if (isClosedShape && fetchedCount > 0) {
            val maskBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskBitmap)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            if (mode == "circle") {
                val c = screenPts[0]
                val p = screenPts[1]
                val rPx = hypot(p.first - c.first, p.second - c.second)
                maskCanvas.drawCircle(c.first, c.second, rPx, maskPaint)
            } else {
                val path = Path()
                screenPts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                maskCanvas.drawPath(path, maskPaint)
            }
            val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            mapLayerCanvas.drawBitmap(maskBitmap, 0f, 0f, xferPaint)
            maskBitmap.recycle()
        }

        val footerHeight = 175
        val outBitmap = Bitmap.createBitmap(outW, outH + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(mapLayerBitmap, 0f, 0f, null)
        mapLayerBitmap.recycle()

        // Draw crisp Mouza-style borders, side lengths in feet, vertices, and center area badge
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeJoin = Paint.Join.ROUND
        }
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(36, 15, 107, 84)
            style = Paint.Style.FILL
        }
        val edgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 7f
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val edgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        fun haversineFeet(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(b.first - a.first)
            val dLon = Math.toRadians(b.second - a.second)
            val la1 = Math.toRadians(a.first)
            val la2 = Math.toRadians(b.first)
            val s1 = sin(dLat / 2.0)
            val s2 = sin(dLon / 2.0)
            val h = s1 * s1 + cos(la1) * cos(la2) * s2 * s2
            val meters = 2.0 * r * atan2(sqrt(h), sqrt(1.0 - h))
            return meters * 3.280839895
        }

        if (mode == "circle" && screenPts.size >= 2) {
            val c = screenPts[0]
            val p = screenPts[1]
            val rPx = hypot(p.first - c.first, p.second - c.second)
            canvas.drawCircle(c.first, c.second, rPx, tintPaint)
            canvas.drawCircle(c.first, c.second, rPx, borderPaint)
            val radFt = haversineFeet(pointsLatLon[0], pointsLatLon[1])
            val lbl = "R: ${LandCalculators.formatCompactFeet(radFt)}"
            val mx = (c.first + p.first) / 2f
            val my = (c.second + p.second) / 2f
            canvas.drawText(lbl, mx, my - 8f, edgeStrokePaint)
            canvas.drawText(lbl, mx, my - 8f, edgeTextPaint)
        } else if (mode == "distance" && screenPts.size >= 2) {
            val path = Path()
            screenPts.forEachIndexed { idx, (px, py) ->
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            borderPaint.color = Color.parseColor("#E08A00")
            canvas.drawPath(path, borderPaint)
            for (i in 0 until screenPts.size - 1) {
                val sA = screenPts[i]
                val sB = screenPts[i + 1]
                val ft = haversineFeet(pointsLatLon[i], pointsLatLon[i + 1])
                val lbl = LandCalculators.formatCompactFeet(ft)
                val mx = (sA.first + sB.first) / 2f
                val my = (sA.second + sB.second) / 2f
                canvas.drawText(lbl, mx, my - 10f, edgeStrokePaint)
                canvas.drawText(lbl, mx, my - 10f, edgeTextPaint)
            }
        } else if (screenPts.size >= 3) {
            val path = Path()
            screenPts.forEachIndexed { idx, (px, py) ->
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            canvas.drawPath(path, tintPaint)
            canvas.drawPath(path, borderPaint)

            for (i in screenPts.indices) {
                val j = (i + 1) % screenPts.size
                val sA = screenPts[i]
                val sB = screenPts[j]
                val pxLen = hypot(sB.first - sA.first, sB.second - sA.second)
                if (pxLen >= 52f) {
                    val ft = haversineFeet(pointsLatLon[i], pointsLatLon[j])
                    val lbl = LandCalculators.formatCompactFeet(ft)
                    var ang = Math.toDegrees(
                        atan2((sB.second - sA.second).toDouble(), (sB.first - sA.first).toDouble())
                    ).toFloat()
                    if (ang > 90f) ang -= 180f
                    if (ang < -90f) ang += 180f
                    val mx = (sA.first + sB.first) / 2f
                    val my = (sA.second + sB.second) / 2f
                    canvas.save()
                    canvas.translate(mx, my)
                    canvas.rotate(ang)
                    canvas.drawText(lbl, 0f, -10f, edgeStrokePaint)
                    canvas.drawText(lbl, 0f, -10f, edgeTextPaint)
                    canvas.restore()
                }
            }
        }

        // Vertex pins
        val pinFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
        val pinStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        screenPts.forEach { (px, py) ->
            canvas.drawCircle(px, py, 10f, pinFill)
            canvas.drawCircle(px, py, 10f, pinStroke)
        }

        // Center area pill badge
        if (isClosedShape && areaSqMeters > 0.0) {
            val area = LandCalculators.fromSqFeet(areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER)
            val cx = if (mode == "circle") screenPts[0].first else screenPts.map { it.first }.average().toFloat()
            val cy = if (mode == "circle") screenPts[0].second else screenPts.map { it.second }.average().toFloat()
            val badgeText = String.format(Locale.US, "জমি • %.2f শতক", area.decimal)
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(225, 15, 107, 84)
            }
            val tw = badgePaint.measureText(badgeText)
            canvas.drawRoundRect(
                RectF(cx - tw / 2f - 20f, cy - 26f, cx + tw / 2f + 20f, cy + 16f),
                16f, 16f, pillPaint
            )
            canvas.drawText(badgeText, cx, cy + 4f, badgePaint)
        }

        drawFooterStamp(
            canvas = canvas,
            width = outW,
            topY = outH.toFloat(),
            footerHeight = footerHeight.toFloat(),
            headerText = summaryHeader,
            line1 = summaryLine1,
            line2 = summaryLine2
        )

        val fileName = "Map_Survey_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    private fun downloadTileBitmap(urlStr: String): Bitmap? = runCatching {
        val ua = if (urlStr.contains("openstreetmap.org") || urlStr.contains("opentopomap.org")) {
            "BhumiSurveyBD/1.0 (Android Land Survey Tool)"
        } else {
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", ua)
            setRequestProperty("Accept", "image/webp,image/png,image/*;q=0.8")
            connectTimeout = 7000
            readTimeout = 7000
        }
        if (conn.responseCode in 200..299) {
            conn.inputStream.use { input -> BitmapFactory.decodeStream(input) }
        } else {
            conn.disconnect()
            null
        }
    }.getOrNull()

    private fun hasInkPixels(bitmap: Bitmap, excludeDarkCanvas: Boolean = false): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 8 || h < 8) return false
        val stepX = max(1, w / 24)
        val stepY = max(1, h / 24)
        var nonBlank = 0
        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val c = bitmap.getPixel(x, y)
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                if (excludeDarkCanvas) {
                    // Detect if bitmap is just solid #14221D / #182923 empty WebView canvas
                    if (kotlin.math.abs(r - 24) > 12 || kotlin.math.abs(g - 41) > 12 || kotlin.math.abs(b - 35) > 12) {
                        nonBlank++
                    }
                } else {
                    if (r < 245 || g < 245 || b < 245) nonBlank++
                }
                if (nonBlank >= 8) return true
            }
        }
        return false
    }

    suspend fun exportSelectedMapRegionPng(
        context: Context,
        fullMapBitmap: Bitmap,
        cropRectNormalized: RectF?,
        summaryHeader: String,
        summaryLine1: String,
        summaryLine2: String
    ): ExportResult = withContext(Dispatchers.Default) {
        val bw = fullMapBitmap.width
        val bh = fullMapBitmap.height

        val cropRect = if (cropRectNormalized != null) {
            val left = (cropRectNormalized.left * bw).toInt().coerceIn(0, bw - 10)
            val top = (cropRectNormalized.top * bh).toInt().coerceIn(0, bh - 10)
            val right = (cropRectNormalized.right * bw).toInt().coerceIn(left + 10, bw)
            val bottom = (cropRectNormalized.bottom * bh).toInt().coerceIn(top + 10, bh)
            Rect(left, top, right, bottom)
        } else {
            Rect(0, 0, bw, bh)
        }

        val cw = cropRect.width().coerceAtLeast(320)
        val ch = cropRect.height().coerceAtLeast(240)
        val scale = if (cw < 1080) (1080f / cw).coerceAtMost(2.5f) else 1f
        val outW = (cw * scale).toInt()
        val outH = (ch * scale).toInt()
        val footerHeight = 165

        val outBitmap = Bitmap.createBitmap(outW, outH + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            fullMapBitmap,
            cropRect,
            RectF(0f, 0f, outW.toFloat(), outH.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        drawFooterStamp(
            canvas = canvas,
            width = outW,
            topY = outH.toFloat(),
            footerHeight = footerHeight.toFloat(),
            headerText = summaryHeader,
            line1 = summaryLine1,
            line2 = summaryLine2
        )

        val fileName = "Map_Survey_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    private fun drawFooterStamp(
        canvas: Canvas,
        width: Int,
        topY: Float,
        footerHeight: Float,
        headerText: String,
        line1: String,
        line2: String
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#0F6B54") }
        canvas.drawRect(0f, topY, width.toFloat(), topY + footerHeight, bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAD165")
            textSize = (width * 0.028f).coerceIn(24f, 36f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (width * 0.024f).coerceIn(20f, 30f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8F3EA")
            textSize = (width * 0.020f).coerceIn(18f, 25f)
        }

        val padX = 28f
        canvas.drawText(headerText, padX, topY + 46f, titlePaint)
        canvas.drawText(line1, padX, topY + 95f, bodyPaint)
        canvas.drawText(line2, padX, topY + 138f, subPaint)
    }

    private fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String): ExportResult {
        val w = bitmap.width
        val h = bitmap.height
        val relativePath = "${Environment.DIRECTORY_PICTURES}/BhumiSurveyBD"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bitmap.recycle()
            ExportResult(uri, "Pictures/BhumiSurveyBD/$fileName", w, h)
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, "BhumiSurveyBD").apply { mkdirs() }
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
        }
    }
}
