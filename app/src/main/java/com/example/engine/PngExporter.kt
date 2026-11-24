package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

        val actualW = hdTile?.bitmap?.width ?: mapOutW
        val actualH = hdTile?.bitmap?.height ?: mapOutH
        val footerHeight = 175
        val outBitmap = Bitmap.createBitmap(actualW, actualH + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawColor(Color.WHITE)

        val layerBitmap = Bitmap.createBitmap(actualW, actualH, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)
        if (hdTile != null) {
            layerCanvas.drawBitmap(
                hdTile.bitmap,
                Rect(0, 0, hdTile.bitmap.width, hdTile.bitmap.height),
                RectF(0f, 0f, actualW.toFloat(), actualH.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            hdTile.bitmap.recycle()
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
