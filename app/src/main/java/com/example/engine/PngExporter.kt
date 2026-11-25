package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.R
import com.example.model.MapLayerOption
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.SurveyorProfile
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

    data class SheetPlotRow(
        val label: String,
        val detail: String
    )

    private const val COLOR_PARCHMENT = 0xFFFAF4E2.toInt()
    private const val COLOR_PARCHMENT_CARD = 0xFFF3EAD1.toInt()
    private const val COLOR_INK_GREEN = 0xFF1B4D3E.toInt()
    private const val COLOR_INK_GOLD = 0xFF8C6B32.toInt()
    private const val COLOR_INK_DARK = 0xFF1E2A24.toInt()
    private const val COLOR_INK_MUTED = 0xFF4F5D55.toInt()

    /**
     * Builds an authentic Classic Mouza Map PNG (preserving surrounding plots/context + north rose +
     * graphic scale bar + measurement formula + surveyor profile photo, name, phone & Gmail).
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
        selectedOnly: Boolean = true,
        surveyorProfile: SurveyorProfile = SurveyorProfile()
    ): ExportResult = withContext(Dispatchers.Default) {
        val outBitmap = buildMouzaSheetBitmap(
            context = context,
            sourceBitmap = sourceBitmap,
            plots = plots,
            sqFeetPerGridPixel = sqFeetPerGridPixel,
            khatianTitle = khatianTitle,
            docFile = docFile,
            isPdf = isPdf,
            pageIndex = pageIndex,
            pageWidthPts = pageWidthPts,
            pageHeightPts = pageHeightPts,
            scale = scale,
            calibrationMultiplier = calibrationMultiplier,
            selectedOnly = selectedOnly,
            surveyorProfile = surveyorProfile
        )
        val fileName = "MouzaSheet_${if (selectedOnly) "Selected" else "Full"}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    /**
     * Exports the identical classic Mouza sheet layout as a high-resolution PDF document.
     */
    suspend fun exportSelectedMouzaPlotsPdf(
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
        selectedOnly: Boolean = true,
        surveyorProfile: SurveyorProfile = SurveyorProfile()
    ): ExportResult = withContext(Dispatchers.Default) {
        val outBitmap = buildMouzaSheetBitmap(
            context = context,
            sourceBitmap = sourceBitmap,
            plots = plots,
            sqFeetPerGridPixel = sqFeetPerGridPixel,
            khatianTitle = khatianTitle,
            docFile = docFile,
            isPdf = isPdf,
            pageIndex = pageIndex,
            pageWidthPts = pageWidthPts,
            pageHeightPts = pageHeightPts,
            scale = scale,
            calibrationMultiplier = calibrationMultiplier,
            selectedOnly = selectedOnly,
            surveyorProfile = surveyorProfile
        )
        val fileName = "MouzaSheet_${if (selectedOnly) "Selected" else "Full"}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        saveSheetBitmapAsPdf(context, outBitmap, fileName)
    }

    internal fun computeMouzaContextCropBounds(
        plots: List<MouzaPlot>,
        selectedOnly: Boolean
    ): FloatArray {
        if (!selectedOnly || plots.isEmpty()) {
            return floatArrayOf(0f, 0f, 1f, 1f)
        }
        var nMinX = 1f
        var nMinY = 1f
        var nMaxX = 0f
        var nMaxY = 0f
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
        // Generous surrounding context (36% padding) so neighboring plots, Dag numbers, and boundaries remain clearly visible
        val span = max(nMaxX - nMinX, nMaxY - nMinY).coerceAtLeast(0.03f)
        val padX = ((nMaxX - nMinX).coerceAtLeast(0.04f) * 0.40f).coerceIn(0.06f, 0.22f)
        val padY = (span * 0.36f).coerceIn(0.06f, 0.22f)
        val left = (nMinX - padX).coerceIn(0f, 0.94f)
        val top = (nMinY - padY).coerceIn(0f, 0.94f)
        val right = (nMaxX + padX).coerceIn(left + 0.04f, 1f)
        val bottom = (nMaxY + padY).coerceIn(top + 0.04f, 1f)
        return floatArrayOf(left, top, right, bottom)
    }

    private suspend fun buildMouzaSheetBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        plots: List<MouzaPlot>,
        sqFeetPerGridPixel: Double,
        khatianTitle: String,
        docFile: File?,
        isPdf: Boolean,
        pageIndex: Int,
        pageWidthPts: Int,
        pageHeightPts: Int,
        scale: MouzaScale,
        calibrationMultiplier: Double,
        selectedOnly: Boolean,
        surveyorProfile: SurveyorProfile
    ): Bitmap {
        if (selectedOnly) {
            require(plots.isNotEmpty()) { "কোনো দাগ সিলেক্ট করা হয়নি।" }
        }

        val bounds = computeMouzaContextCropBounds(plots, selectedOnly)
        val nMinX = bounds[0]
        val nMinY = bounds[1]
        val nMaxX = bounds[2]
        val nMaxY = bounds[3]

        val spanW = (nMaxX - nMinX).coerceAtLeast(0.01f)
        val spanH = (nMaxY - nMinY).coerceAtLeast(0.01f)
        val worldAspect = (spanH * pageHeightPts.coerceAtLeast(100)) /
            (spanW * pageWidthPts.coerceAtLeast(100))

        val targetMaxPx = 1800
        val mapOutW: Int
        val mapOutH: Int
        if (worldAspect <= 1f) {
            mapOutW = targetMaxPx
            mapOutH = (targetMaxPx * worldAspect).roundToInt().coerceIn(580, 2100)
        } else {
            mapOutH = targetMaxPx
            mapOutW = (targetMaxPx / worldAspect).roundToInt().coerceIn(680, 2100)
        }

        val hdTile = if (docFile != null && docFile.exists()) {
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

        val usableHdTile = if (hdTile != null && MouzaPdfEngine.hasInkPixels(hdTile.bitmap)) {
            hdTile
        } else {
            hdTile?.bitmap?.recycle()
            null
        }

        val mapW = max(1240, usableHdTile?.bitmap?.width ?: mapOutW)
        val mapH = if (usableHdTile != null) {
            (usableHdTile.bitmap.height.toFloat() * mapW / usableHdTile.bitmap.width.coerceAtLeast(1)).roundToInt()
        } else {
            (mapOutH.toFloat() * mapW / mapOutW.coerceAtLeast(1)).roundToInt()
        }.coerceIn(560, 2200)

        val mapBitmap = Bitmap.createBitmap(mapW, mapH, Bitmap.Config.ARGB_8888)
        val mapCanvas = Canvas(mapBitmap)
        mapCanvas.drawColor(Color.WHITE)

        if (usableHdTile != null) {
            mapCanvas.drawBitmap(
                usableHdTile.bitmap,
                Rect(0, 0, usableHdTile.bitmap.width, usableHdTile.bitmap.height),
                RectF(0f, 0f, mapW.toFloat(), mapH.toFloat()),
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
            mapCanvas.drawBitmap(
                sourceBitmap,
                srcRect,
                RectF(0f, 0f, mapW.toFloat(), mapH.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
        }

        // Keep FULL surrounding Mouza context! Apply a gentle parchment vignette tint ONLY slightly
        // so neighboring Dag numbers & boundary lines remain crisp and recognizable, while highlighted plots pop.
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            strokeJoin = Paint.Join.ROUND
        }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 9.5f
            strokeJoin = Paint.Join.ROUND
        }
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val edgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val edgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        var totalSqFt = 0.0
        var totalPerimeterFt = 0.0
        val plotRows = mutableListOf<SheetPlotRow>()

        for (plot in plots) {
            val plotSqFt = plot.exactAreaSqFeet ?: (plot.pixelCount * sqFeetPerGridPixel)
            totalSqFt += plotSqFt

            val argb = Color.argb(
                (plot.color.alpha * 255).toInt(),
                (plot.color.red * 255).toInt(),
                (plot.color.green * 255).toInt(),
                (plot.color.blue * 255).toInt()
            )
            tintPaint.color = Color.argb(62, Color.red(argb), Color.green(argb), Color.blue(argb))
            borderPaint.color = argb
            edgeTextPaint.color = Color.rgb(
                (Color.red(argb) * 0.72f).toInt(),
                (Color.green(argb) * 0.72f).toInt(),
                (Color.blue(argb) * 0.72f).toInt()
            )

            var centerX = 0f
            var centerY = 0f
            var plotPerimFt = 0.0

            val poly = plot.effectivePolygon()
            if (poly != null && poly.size >= 3) {
                val path = Path()
                var sumX = 0f
                var sumY = 0f
                val screenPts = poly.map { (nx, ny) ->
                    val px = ((nx - nMinX) / spanW) * mapW
                    val py = ((ny - nMinY) / spanH) * mapH
                    sumX += px; sumY += py
                    px to py
                }
                screenPts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                mapCanvas.drawPath(path, tintPaint)
                mapCanvas.drawPath(path, haloPaint)
                mapCanvas.drawPath(path, borderPaint)

                centerX = plot.centroidNormalized?.let { ((it.first - nMinX) / spanW) * mapW }
                    ?: (sumX / screenPts.size)
                centerY = plot.centroidNormalized?.let { ((it.second - nMinY) / spanH) * mapH }
                    ?: (sumY / screenPts.size)

                val edgeSummaries = mutableListOf<String>()
                for (i in poly.indices) {
                    val pA = poly[i]
                    val pB = poly[(i + 1) % poly.size]
                    val sA = screenPts[i]
                    val sB = screenPts[(i + 1) % screenPts.size]
                    val ft = MouzaPdfEngine.computeDistanceFeet(
                        pA, pB, pageWidthPts, pageHeightPts, scale, calibrationMultiplier
                    )
                    plotPerimFt += ft
                    if (edgeSummaries.size < 4) {
                        edgeSummaries.add(LandCalculators.formatCompactFeet(ft))
                    }
                    val pxLen = hypot(sB.first - sA.first, sB.second - sA.second)
                    if (pxLen >= 52f) {
                        val label = LandCalculators.formatCompactFeet(ft)
                        var ang = Math.toDegrees(
                            atan2((sB.second - sA.second).toDouble(), (sB.first - sA.first).toDouble())
                        ).toFloat()
                        if (ang > 90f) ang -= 180f
                        if (ang < -90f) ang += 180f
                        val mx = (sA.first + sB.first) / 2f
                        val my = (sA.second + sB.second) / 2f
                        mapCanvas.save()
                        mapCanvas.translate(mx, my)
                        mapCanvas.rotate(ang)
                        mapCanvas.drawText(label, 0f, -6f, edgeStrokePaint)
                        mapCanvas.drawText(label, 0f, -6f, edgeTextPaint)
                        mapCanvas.restore()
                    }
                }
                totalPerimeterFt += plotPerimFt

                // Draw corner pins
                val pinFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
                val pinStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = argb
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                screenPts.forEach { (px, py) ->
                    mapCanvas.drawCircle(px, py, 6.5f, pinFill)
                    mapCanvas.drawCircle(px, py, 6.5f, pinStroke)
                }

                val pArea = LandCalculators.fromSqFeet(plotSqFt)
                val edgeInfo = if (edgeSummaries.isNotEmpty()) " | বাহু: ${edgeSummaries.joinToString("×")}" else ""
                plotRows.add(
                    SheetPlotRow(
                        label = buildString {
                            append("প্লট #${plot.plotIndex}")
                            if (plot.dagNumber.isNotBlank()) append(" (দাগ ${plot.dagNumber})")
                            if (plot.khatianNumber.isNotBlank()) append(" • খ:${plot.khatianNumber}")
                        },
                        detail = String.format(
                            Locale.US,
                            "%.2f শতক • %.2f কাঠা (%.0f Sq.Ft)$edgeInfo",
                            pArea.decimal, pArea.katha, pArea.sqFeet
                        )
                    )
                )
            } else {
                val gw = plot.gridWidth.toFloat().coerceAtLeast(1f)
                val gh = plot.gridHeight.toFloat().coerceAtLeast(1f)
                centerX = ((((plot.minX + plot.maxX) * 0.5f / gw) - nMinX) / spanW) * mapW
                centerY = ((((plot.minY + plot.maxY) * 0.5f / gh) - nMinY) / spanH) * mapH
                val pArea = LandCalculators.fromSqFeet(plotSqFt)
                plotRows.add(
                    SheetPlotRow(
                        label = "প্লট #${plot.plotIndex}${if (plot.dagNumber.isNotBlank()) " (দাগ ${plot.dagNumber})" else ""}",
                        detail = String.format(Locale.US, "%.2f শতক • %.2f কাঠা (%.0f Sq.Ft)", pArea.decimal, pArea.katha, pArea.sqFeet)
                    )
                )
            }

            val area = LandCalculators.fromSqFeet(plotSqFt)
            val tag = buildString {
                if (plot.dagNumber.isNotBlank()) append("দাগ ${plot.dagNumber} • ")
                else append("#${plot.plotIndex} • ")
                append(String.format(Locale.US, "%.2f শতক", area.decimal))
            }
            val tw = labelPaint.measureText(tag)
            pillPaint.color = Color.argb(228, Color.red(argb), Color.green(argb), Color.blue(argb))
            val pillRect = RectF(centerX - tw / 2f - 16f, centerY - 24f, centerX + tw / 2f + 16f, centerY + 14f)
            mapCanvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
            mapCanvas.drawText(tag, centerX, centerY + 3f, labelPaint)
        }

        // Feet represented across map width for accurate graphic scale bar
        val pageFeetW = (pageWidthPts / 72.0) * scale.feetPerInch * sqrt(calibrationMultiplier)
        val mapSpanFeetW = (spanW * pageFeetW).coerceAtLeast(10.0)
        val totalArea = LandCalculators.fromSqFeet(totalSqFt)
        val sheetTitle = if (khatianTitle.isNotBlank()) khatianTitle else "ডিজিটাল মৌজা নকশা শিট (${plots.size}টি প্লট)"
        val calibLabel = String.format(Locale.US, "%.2fx", calibrationMultiplier)
        val methodExplanation =
            "কিভাবে পরিমাপ হলো: ${scale.labelBn} (${scale.ratioLabel}, ক্যালিব্রেশন $calibLabel) স্কেলে গাউসিয়ান বহুভুজ (Shoelace) ও স্কেল রেশিও সূত্রে পরিমাপ। ১ শতাংশ = ৪৩৫.৬ বর্গফুট • ১ কাঠা = ৭২০ বর্গফুট • ১ চেইন = ৬৬ ফুট।"

        return composeClassicMouzaSheet(
            context = context,
            mapBitmap = mapBitmap,
            sheetTitle = sheetTitle,
            scaleLabel = "${scale.labelEn} (${scale.ratioLabel})",
            mapSpanFeetWidth = mapSpanFeetW,
            totalSqFt = totalSqFt,
            totalPerimeterFeet = totalPerimeterFt,
            plotRows = plotRows,
            methodExplanation = methodExplanation,
            surveyorProfile = surveyorProfile,
            sheetCode = "BS-MOUZA-${pageIndex + 1}"
        ).also {
            mapBitmap.recycle()
        }
    }

    /**
     * Renders a crisp Mouza-style satellite/map sheet PNG with generous surrounding context,
     * classic double border, compass rose, scale bar, methodology, and surveyor details.
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
        summaryLine2: String,
        mouzaScale: MouzaScale = MouzaScale.SCALE_16,
        surveyorProfile: SurveyorProfile = SurveyorProfile()
    ): ExportResult = withContext(Dispatchers.IO) {
        val outBitmap = buildRealMapSheetBitmap(
            context = context,
            pointsLatLon = pointsLatLon,
            mode = mode,
            layer = layer,
            showLabelsOverlay = showLabelsOverlay,
            areaSqMeters = areaSqMeters,
            fallbackBitmap = fallbackBitmap,
            fallbackCropRect = fallbackCropRect,
            summaryHeader = summaryHeader,
            mouzaScale = mouzaScale,
            surveyorProfile = surveyorProfile
        )
        val fileName = "Map_MouzaSheet_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    suspend fun exportSelectedMapPlotPdf(
        context: Context,
        pointsLatLon: List<Pair<Double, Double>>,
        mode: String,
        layer: MapLayerOption,
        showLabelsOverlay: Boolean,
        areaSqMeters: Double,
        fallbackBitmap: Bitmap?,
        fallbackCropRect: RectF?,
        summaryHeader: String,
        mouzaScale: MouzaScale = MouzaScale.SCALE_16,
        surveyorProfile: SurveyorProfile = SurveyorProfile()
    ): ExportResult = withContext(Dispatchers.IO) {
        val outBitmap = buildRealMapSheetBitmap(
            context = context,
            pointsLatLon = pointsLatLon,
            mode = mode,
            layer = layer,
            showLabelsOverlay = showLabelsOverlay,
            areaSqMeters = areaSqMeters,
            fallbackBitmap = fallbackBitmap,
            fallbackCropRect = fallbackCropRect,
            summaryHeader = summaryHeader,
            mouzaScale = mouzaScale,
            surveyorProfile = surveyorProfile
        )
        val fileName = "Map_MouzaSheet_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        saveSheetBitmapAsPdf(context, outBitmap, fileName)
    }

    private suspend fun buildRealMapSheetBitmap(
        context: Context,
        pointsLatLon: List<Pair<Double, Double>>,
        mode: String,
        layer: MapLayerOption,
        showLabelsOverlay: Boolean,
        areaSqMeters: Double,
        fallbackBitmap: Bitmap?,
        fallbackCropRect: RectF?,
        summaryHeader: String,
        mouzaScale: MouzaScale,
        surveyorProfile: SurveyorProfile
    ): Bitmap {
        if (pointsLatLon.isEmpty()) {
            val rawBmp = fallbackBitmap ?: Bitmap.createBitmap(1200, 840, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).drawColor(Color.parseColor("#EBF2EE"))
            }
            val sqFt = areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER
            return composeClassicMouzaSheet(
                context = context,
                mapBitmap = rawBmp,
                sheetTitle = summaryHeader,
                scaleLabel = "${mouzaScale.ratioLabel} • ${layer.titleEn}",
                mapSpanFeetWidth = 300.0,
                totalSqFt = sqFt,
                totalPerimeterFeet = 0.0,
                plotRows = emptyList(),
                methodExplanation = "কিভাবে পরিমাপ হলো: ${layer.titleEn} স্যাটেলাইট ও মৌজা স্কেল (${mouzaScale.labelBn}) অনুপাতে হিসাব। ১ শতাংশ = ৪৩৫.৬ বর্গফুট।",
                surveyorProfile = surveyorProfile,
                sheetCode = "BS-SAT-01"
            ).also {
                if (fallbackBitmap != null && !fallbackBitmap.isRecycled) fallbackBitmap.recycle()
            }
        }

        fun ll2m(lat: Double, lon: Double): Pair<Double, Double> {
            val s = sin(Math.toRadians(lat.coerceIn(-85.0511, 85.0511)))
            val mx = (lon + 180.0) / 360.0
            val my = 0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)
            return mx to my
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
        // 42% surrounding pad so roads, neighboring land plots, trees, and buildings show cleanly
        val pad = max(rawSpanX, rawSpanY) * 0.42
        val boxMinX = (minMx - pad).coerceIn(0.0, 0.999999)
        val boxMinY = (minMy - pad).coerceIn(0.0, 0.999999)
        val boxMaxX = (maxMx + pad).coerceIn(boxMinX + 1e-6, 1.0)
        val boxMaxY = (maxMy + pad).coerceIn(boxMinY + 1e-6, 1.0)

        val spanX = boxMaxX - boxMinX
        val spanY = boxMaxY - boxMinY
        val aspect = (spanY / spanX).coerceIn(0.55, 1.55)

        val outW = 1680
        val outH = (outW * aspect).roundToInt().coerceIn(760, 1880)

        val idealZ = log2(outW / (256.0 * spanX))
        var tileZ = floor(idealZ).toInt().coerceIn(4, layer.maxNativeZoom)
        var nTiles = 2.0.pow(tileZ.toDouble()).toInt()
        var txMin = floor(boxMinX * nTiles).toInt().coerceIn(0, nTiles - 1)
        var txMax = floor(boxMaxX * nTiles).toInt().coerceIn(txMin, nTiles - 1)
        var tyMin = floor(boxMinY * nTiles).toInt().coerceIn(0, nTiles - 1)
        var tyMax = floor(boxMaxY * nTiles).toInt().coerceIn(tyMin, nTiles - 1)
        while ((txMax - txMin + 1) * (tyMax - tyMin + 1) > 56 && tileZ > 4) {
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
        }.take(56)

        fun resolveTileUrl(tmpl: String, z: Int, x: Int, y: Int): String {
            val shard = ((x + y) and 3).toString()
            return tmpl
                .replace("{s}", shard)
                .replace("{z}", z.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
        }

        var fetchedCount = 0
        val baseTiles = coroutineScope {
            baseTileCoords.map { (tx, ty) ->
                async {
                    var chosenBmp = downloadTileBitmap(resolveTileUrl(layer.tileUrl, tileZ, tx, ty))
                    var srcRect: Rect? = null
                    if (chosenBmp == null && tileZ > 15) {
                        for (dz in 1..3) {
                            val pz = tileZ - dz
                            if (pz < 4) break
                            val pScale = 1 shl dz
                            val ptx = tx / pScale
                            val pty = ty / pScale
                            val pBmp = downloadTileBitmap(resolveTileUrl(layer.tileUrl, pz, ptx, pty))
                            if (pBmp != null) {
                                val subW = pBmp.width / pScale
                                val subH = pBmp.height / pScale
                                val sx = (tx % pScale) * subW
                                val sy = (ty % pScale) * subH
                                srcRect = Rect(sx, sy, (sx + subW).coerceAtMost(pBmp.width), (sy + subH).coerceAtMost(pBmp.height))
                                chosenBmp = pBmp
                                break
                            }
                        }
                    }
                    Triple(tx, ty, chosenBmp to srcRect)
                }
            }.awaitAll()
        }

        val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        for ((tx, ty, bmpPair) in baseTiles) {
            val (bmp, srcCrop) = bmpPair
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
                mapLayerCanvas.drawBitmap(bmp, srcCrop, dst, tilePaint)
                bmp.recycle()
            }
        }

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

        // Keep FULL surrounding satellite/roads intact (NO DST_IN erasing!)
        val screenPts = mercPts.map { (mx, my) ->
            (((mx - boxMinX) / spanX) * outW).toFloat() to (((my - boxMinY) / spanY) * outH).toFloat()
        }

        val isClosedShape = (mode == "circle" && screenPts.size >= 2) || (mode != "distance" && screenPts.size >= 3)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeJoin = Paint.Join.ROUND
        }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeJoin = Paint.Join.ROUND
        }
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(56, 15, 107, 84)
            style = Paint.Style.FILL
        }
        val edgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 7f
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val edgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B4F3E")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        var totalPerimeterFt = 0.0
        val edgeSummaries = mutableListOf<String>()

        if (mode == "circle" && screenPts.size >= 2) {
            val c = screenPts[0]
            val p = screenPts[1]
            val rPx = hypot(p.first - c.first, p.second - c.second)
            mapLayerCanvas.drawCircle(c.first, c.second, rPx, tintPaint)
            mapLayerCanvas.drawCircle(c.first, c.second, rPx, haloPaint)
            mapLayerCanvas.drawCircle(c.first, c.second, rPx, borderPaint)
            val radFt = haversineFeet(pointsLatLon[0], pointsLatLon[1])
            totalPerimeterFt = 2.0 * PI * radFt
            edgeSummaries.add("ব্যাসার্ধ ${LandCalculators.formatCompactFeet(radFt)}")
            val lbl = "R: ${LandCalculators.formatCompactFeet(radFt)}"
            val mx = (c.first + p.first) / 2f
            val my = (c.second + p.second) / 2f
            mapLayerCanvas.drawText(lbl, mx, my - 8f, edgeStrokePaint)
            mapLayerCanvas.drawText(lbl, mx, my - 8f, edgeTextPaint)
        } else if (mode == "distance" && screenPts.size >= 2) {
            val path = Path()
            screenPts.forEachIndexed { idx, (px, py) ->
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            borderPaint.color = Color.parseColor("#E08A00")
            mapLayerCanvas.drawPath(path, haloPaint)
            mapLayerCanvas.drawPath(path, borderPaint)
            for (i in 0 until screenPts.size - 1) {
                val sA = screenPts[i]
                val sB = screenPts[i + 1]
                val ft = haversineFeet(pointsLatLon[i], pointsLatLon[i + 1])
                totalPerimeterFt += ft
                if (edgeSummaries.size < 4) edgeSummaries.add(LandCalculators.formatCompactFeet(ft))
                val lbl = LandCalculators.formatCompactFeet(ft)
                val mx = (sA.first + sB.first) / 2f
                val my = (sA.second + sB.second) / 2f
                mapLayerCanvas.drawText(lbl, mx, my - 10f, edgeStrokePaint)
                mapLayerCanvas.drawText(lbl, mx, my - 10f, edgeTextPaint)
            }
        } else if (screenPts.size >= 3) {
            val path = Path()
            screenPts.forEachIndexed { idx, (px, py) ->
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            mapLayerCanvas.drawPath(path, tintPaint)
            mapLayerCanvas.drawPath(path, haloPaint)
            mapLayerCanvas.drawPath(path, borderPaint)

            for (i in screenPts.indices) {
                val j = (i + 1) % screenPts.size
                val sA = screenPts[i]
                val sB = screenPts[j]
                val ft = haversineFeet(pointsLatLon[i], pointsLatLon[j])
                totalPerimeterFt += ft
                if (edgeSummaries.size < 4) edgeSummaries.add(LandCalculators.formatCompactFeet(ft))
                val pxLen = hypot(sB.first - sA.first, sB.second - sA.second)
                if (pxLen >= 52f) {
                    val lbl = LandCalculators.formatCompactFeet(ft)
                    var ang = Math.toDegrees(
                        atan2((sB.second - sA.second).toDouble(), (sB.first - sA.first).toDouble())
                    ).toFloat()
                    if (ang > 90f) ang -= 180f
                    if (ang < -90f) ang += 180f
                    val mx = (sA.first + sB.first) / 2f
                    val my = (sA.second + sB.second) / 2f
                    mapLayerCanvas.save()
                    mapLayerCanvas.translate(mx, my)
                    mapLayerCanvas.rotate(ang)
                    mapLayerCanvas.drawText(lbl, 0f, -10f, edgeStrokePaint)
                    mapLayerCanvas.drawText(lbl, 0f, -10f, edgeTextPaint)
                    mapLayerCanvas.restore()
                }
            }
        }

        val pinFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
        val pinStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        screenPts.forEach { (px, py) ->
            mapLayerCanvas.drawCircle(px, py, 9f, pinFill)
            mapLayerCanvas.drawCircle(px, py, 9f, pinStroke)
        }

        val sqFeet = areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER
        if (isClosedShape && areaSqMeters > 0.0) {
            val area = LandCalculators.fromSqFeet(sqFeet)
            val cx = if (mode == "circle") screenPts[0].first else screenPts.map { it.first }.average().toFloat()
            val cy = if (mode == "circle") screenPts[0].second else screenPts.map { it.second }.average().toFloat()
            val badgeText = String.format(Locale.US, "জমি • %.2f শতক", area.decimal)
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 27f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 15, 107, 84)
            }
            val tw = badgePaint.measureText(badgeText)
            mapLayerCanvas.drawRoundRect(
                RectF(cx - tw / 2f - 20f, cy - 26f, cx + tw / 2f + 20f, cy + 16f),
                16f, 16f, pillPaint
            )
            mapLayerCanvas.drawText(badgeText, cx, cy + 4f, badgePaint)
        }

        // Real ground width in feet across boxMinX..boxMaxX at center latitude
        val avgLat = pointsLatLon.map { it.first }.average()
        val lonSpanDeg = spanX * 360.0
        val groundSpanFtW = (lonSpanDeg * 111320.0 * cos(Math.toRadians(avgLat)) * 3.280839895).coerceAtLeast(20.0)

        val centerCoord = String.format(
            Locale.US,
            "%.5f°N, %.5f°E",
            avgLat,
            pointsLatLon.map { it.second }.average()
        )
        val mouzaInchesPerimeter = totalPerimeterFt / mouzaScale.feetPerInch.coerceAtLeast(1.0)
        val totalLinks = totalPerimeterFt / 0.66
        val areaStats = LandCalculators.fromSqFeet(sqFeet)
        val rows = listOf(
            SheetPlotRow(
                label = "জিপিএস: $centerCoord",
                detail = if (isClosedShape) {
                    String.format(
                        Locale.US,
                        "%.2f শতক • %.2f কাঠা • %.0f Sq.Ft | নকশা (${mouzaScale.ratioLabel}): %.2f\" (%.0f লিংক)",
                        areaStats.decimal, areaStats.katha, areaStats.sqFeet,
                        mouzaInchesPerimeter, totalLinks
                    )
                } else {
                    String.format(
                        Locale.US,
                        "দৈর্ঘ্য: %.2f ফু (%.1f লিংক) • নকশা (${mouzaScale.ratioLabel}): %.2f\" • পিন: %dটি",
                        totalPerimeterFt, totalLinks, mouzaInchesPerimeter, pointsLatLon.size
                    )
                }
            )
        )

        val methodExplanation =
            "কিভাবে পরিমাপ হলো: ${layer.titleEn} (Z$tileZ) স্যাটেলাইট ম্যাপ ও মৌজা স্কেল ${mouzaScale.labelBn} (${mouzaScale.ratioLabel}, ১\"=${mouzaScale.feetPerInch.toInt()} ফু) অনুপাতে হিসাব। ১ শতাংশ = ৪৩৫.৬ বর্গফুট।"

        return composeClassicMouzaSheet(
            context = context,
            mapBitmap = mapLayerBitmap,
            sheetTitle = summaryHeader.ifBlank { "স্যাটেলাইট মৌজা পরিমাপ শিট" },
            scaleLabel = "${mouzaScale.ratioLabel} • ${layer.titleEn} Z$tileZ",
            mapSpanFeetWidth = groundSpanFtW,
            totalSqFt = sqFeet,
            totalPerimeterFeet = totalPerimeterFt,
            plotRows = rows,
            methodExplanation = methodExplanation,
            surveyorProfile = surveyorProfile,
            sheetCode = "BS-MAP-Z$tileZ"
        ).also {
            mapLayerBitmap.recycle()
        }
    }

    /**
     * Wraps any annotated map viewport into a classic, antique Bangladesh Mouza Map sheet:
     * - Parchment margin & double cartographic borders with corner registration ticks
     * - Top Mouza title header with custom app emblem, scale stamp, & date
     * - Compass Rose (N / উ) & Dynamic Feet Scale Bar inside the map frame
     * - Bottom 2-column Ledger: Left (Area Table + "কিভাবে পরিমাপ হলো"), Right (Surveyor Photo, Name, Phone, Gmail)
     */
    private fun composeClassicMouzaSheet(
        context: Context,
        mapBitmap: Bitmap,
        sheetTitle: String,
        scaleLabel: String,
        mapSpanFeetWidth: Double,
        totalSqFt: Double,
        totalPerimeterFeet: Double,
        plotRows: List<SheetPlotRow>,
        methodExplanation: String,
        surveyorProfile: SurveyorProfile,
        sheetCode: String
    ): Bitmap {
        val mapW = mapBitmap.width.coerceAtLeast(1100)
        val mapH = mapBitmap.height.coerceAtLeast(620)
        val outerMargin = (mapW * 0.030f).roundToInt().coerceIn(32, 54)
        val headerH = (mapW * 0.090f).roundToInt().coerceIn(106, 148)
        val footerH = (mapW * 0.240f).roundToInt().coerceIn(300, 390)

        val sheetW = mapW + outerMargin * 2
        val sheetH = outerMargin * 2 + headerH + mapH + footerH + 24

        val outBitmap = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // 1. Classic Parchment Canvas
        canvas.drawColor(COLOR_PARCHMENT)

        // 2. Outer Double Cartographic Border + Corner Registration Marks
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val bPad = 14f
        canvas.drawRect(bPad, bPad, sheetW - bPad, sheetH - bPad, outerBorder)
        canvas.drawRect(bPad + 8f, bPad + 8f, sheetW - bPad - 8f, sheetH - bPad - 8f, innerBorder)

        // 3. Top Header Band (App Icon + Mouza Header + Scale & Date Stamp)
        val contentLeft = outerMargin.toFloat()
        val contentRight = (sheetW - outerMargin).toFloat()
        val headerTop = bPad + 16f
        val headerBottom = headerTop + headerH - 8f

        val logoBmp = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_art) }.getOrNull()
        val logoSize = (headerH * 0.72f).coerceIn(64f, 96f)
        val logoRect = RectF(contentLeft, headerTop + (headerH - logoSize) / 2f - 4f, contentLeft + logoSize, headerTop + (headerH + logoSize) / 2f - 4f)
        if (logoBmp != null) {
            drawCircularBitmap(canvas, logoBmp, logoRect, COLOR_INK_GREEN, 3.5f)
            logoBmp.recycle()
        }

        val titleLeft = contentLeft + logoSize + 16f
        val stampBoxW = (mapW * 0.26f).coerceIn(260f, 360f)
        val titleMaxW = (contentRight - stampBoxW - 18f - titleLeft).coerceAtLeast(220f)

        val govPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GOLD
            textSize = (mapW * 0.015f).coerceIn(18f, 24f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = (mapW * 0.025f).coerceIn(28f, 38f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = (mapW * 0.016f).coerceIn(19f, 25f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        drawFittedText(canvas, "ভূমি সেবা ডিজিটাল • ডিজিটাল মৌজা সার্ভে শিট", titleLeft, headerTop + 28f, titleMaxW, govPaint)
        drawFittedText(canvas, sheetTitle, titleLeft, headerTop + 66f, titleMaxW, titlePaint)
        drawFittedText(
            canvas,
            "আশেপাশের দাগ ও সীমানাসহ সংরক্ষিত জরিপ কপি",
            titleLeft,
            headerTop + 98f,
            titleMaxW,
            subtitlePaint
        )

        // Top-right Stamp Meta Box
        val stampRect = RectF(contentRight - stampBoxW, headerTop + 4f, contentRight, headerBottom - 6f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PARCHMENT_CARD }
        val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        canvas.drawRoundRect(stampRect, 10f, 10f, cardFill)
        canvas.drawRoundRect(stampRect, 10f, 10f, cardStroke)

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = (mapW * 0.0135f).coerceIn(16f, 21f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            textSize = (mapW * 0.012f).coerceIn(15f, 19f)
        }
        drawFittedText(canvas, "স্কেল: $scaleLabel", stampRect.left + 12f, stampRect.top + 28f, stampBoxW - 24f, metaPaint)
        drawFittedText(canvas, "তারিখ: $dateStr", stampRect.left + 12f, stampRect.top + 56f, stampBoxW - 24f, metaSubPaint)
        drawFittedText(canvas, "রেফারেন্স: $sheetCode", stampRect.left + 12f, stampRect.top + 82f, stampBoxW - 24f, metaSubPaint)

        // 4. Draw Map Frame + Map Bitmap
        val mapTop = headerBottom + 8f
        val mapRect = RectF(contentLeft, mapTop, contentLeft + mapW, mapTop + mapH)
        canvas.drawBitmap(mapBitmap, Rect(0, 0, mapBitmap.width, mapBitmap.height), mapRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val mapFrameStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawRect(mapRect, mapFrameStroke)

        // Draw North Compass Rose inside Top-Right of Map
        drawCompassRose(canvas, mapRect.right - 56f, mapRect.top + 56f, 36f)

        // Draw Graphic Scale Bar inside Bottom-Left of Map
        drawGraphicScaleBar(canvas, mapRect.left + 20f, mapRect.bottom - 52f, mapW.toFloat(), mapSpanFeetWidth)

        // 5. Bottom Vintage Ledger & Surveyor Profile Card
        val ledgerTop = mapRect.bottom + 14f
        val ledgerBottom = sheetH - bPad - 16f
        val ledgerTotalW = contentRight - contentLeft
        val leftColW = ledgerTotalW * 0.61f
        val rightColLeft = contentLeft + leftColW + 14f
        val rightColRect = RectF(rightColLeft, ledgerTop, contentRight, ledgerBottom)
        val leftColRect = RectF(contentLeft, ledgerTop, contentLeft + leftColW, ledgerBottom)

        canvas.drawRoundRect(leftColRect, 12f, 12f, cardFill)
        canvas.drawRoundRect(leftColRect, 12f, 12f, cardStroke)

        val totalArea = LandCalculators.fromSqFeet(totalSqFt)
        val ledgerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = (mapW * 0.017f).coerceIn(20f, 26f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val ledgerMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = (mapW * 0.0185f).coerceIn(22f, 28f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val ledgerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            textSize = (mapW * 0.014f).coerceIn(17f, 21f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = (mapW * 0.013f).coerceIn(16f, 20f)
        }
        val methodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = (mapW * 0.0125f).coerceIn(15f, 19f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val lx = leftColRect.left + 16f
        val lMaxW = leftColRect.width() - 32f
        var curY = leftColRect.top + 28f
        drawFittedText(canvas, "পরিমাপ বিবরণী ও মৌজা হিসাব (মোট সারসংক্ষেপ)", lx, curY, lMaxW, ledgerTitlePaint)
        curY += 34f

        val primarySummary = String.format(
            Locale.US,
            "মোট: %.2f শতাংশ (Decimal)  |  %.2f কাঠা  |  %.3f বিঘা  |  %.4f একর",
            totalArea.decimal, totalArea.katha, totalArea.bigha, totalArea.acre
        )
        drawFittedText(canvas, primarySummary, lx, curY, lMaxW, ledgerMainPaint)
        curY += 28f

        val secondarySummary = String.format(
            Locale.US,
            "%.1f বর্গফুট (Sq.Ft)  |  %.1f বর্গমিটার (Sq.M)  |  মোট পরিধি: %.1f ফুট",
            totalArea.sqFeet, totalArea.sqMeters, totalPerimeterFeet
        )
        drawFittedText(canvas, secondarySummary, lx, curY, lMaxW, ledgerSubPaint)
        curY += 12f

        val dividerPaint = Paint().apply {
            color = Color.argb(90, 140, 107, 50)
            strokeWidth = 1.5f
        }
        canvas.drawLine(lx, curY, leftColRect.right - 16f, curY, dividerPaint)
        curY += 22f

        val shownRows = plotRows.take(3)
        for (r in shownRows) {
            drawFittedText(canvas, "• ${r.label}: ${r.detail}", lx, curY, lMaxW, rowPaint)
            curY += 24f
        }
        if (plotRows.size > 3) {
            drawFittedText(canvas, "... আরও ${plotRows.size - 3}টি দাগ সংযুক্ত", lx, curY, lMaxW, rowPaint)
            curY += 22f
        }

        // Draw "কিভাবে পরিমাপ হলো" at bottom of left column wrapped cleanly up to 2 lines
        val methodStartY = max(curY + 8f, leftColRect.bottom - 52f)
        drawWrappedLines(canvas, methodExplanation, lx, methodStartY, lMaxW, 22f, 2, methodPaint)

        // Right Column: Surveyor Details Box (Photo, Name, Phone Number, Gmail)
        val profileBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE5C7") }
        canvas.drawRoundRect(rightColRect, 12f, 12f, profileBg)
        canvas.drawRoundRect(rightColRect, 12f, 12f, cardStroke)

        val rPad = 14f
        val rInnerLeft = rightColRect.left + rPad
        val rMaxW = rightColRect.width() - rPad * 2

        val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = (mapW * 0.015f).coerceIn(18f, 23f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawFittedText(canvas, "প্রস্তুতকারক / সার্ভেয়ার বিবরণ", rInnerLeft, rightColRect.top + 28f, rMaxW, hdrPaint)
        canvas.drawLine(rInnerLeft, rightColRect.top + 38f, rightColRect.right - rPad, rightColRect.top + 38f, dividerPaint)

        val avatarSize = (rightColRect.height() * 0.56f).coerceIn(104f, 152f)
        val avatarTop = rightColRect.top + 52f
        val avatarRect = RectF(rInnerLeft, avatarTop, rInnerLeft + avatarSize, avatarTop + avatarSize)
        val avatarBmp = loadSurveyorPhotoBitmap(context, surveyorProfile.photoUri)
        if (avatarBmp != null) {
            drawCircularBitmap(canvas, avatarBmp, avatarRect, COLOR_INK_GREEN, 4f)
            avatarBmp.recycle()
        } else {
            val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_GREEN }
            canvas.drawOval(avatarRect, placeholder)
        }

        val infoLeft = avatarRect.right + 14f
        val infoMaxW = (rightColRect.right - rPad - infoLeft).coerceAtLeast(120f)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = (mapW * 0.018f).coerceIn(22f, 27f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val desigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = (mapW * 0.013f).coerceIn(15f, 19f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = (mapW * 0.0138f).coerceIn(16f, 21f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val cleanName = surveyorProfile.name.ifBlank { "Abdus Salam" }
        val cleanDesig = surveyorProfile.designation.ifBlank { "ডিজিটাল সার্ভেয়ার • ভূমি সেবা ডিজিটাল" }
        val cleanPhone = surveyorProfile.phone.ifBlank { "01620230864" }
        val cleanEmail = surveyorProfile.email.ifBlank { "salam230864@gmail.com" }

        var iy = avatarTop + 26f
        drawFittedText(canvas, cleanName, infoLeft, iy, infoMaxW, namePaint)
        iy += 28f
        drawFittedText(canvas, cleanDesig, infoLeft, iy, infoMaxW, desigPaint)
        iy += 30f
        drawFittedText(canvas, "মোবা: $cleanPhone", infoLeft, iy, infoMaxW, contactPaint)
        iy += 28f
        drawFittedText(canvas, "Gmail: $cleanEmail", infoLeft, iy, infoMaxW, contactPaint)

        // Official Bottom Seal Pill
        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_GREEN }
        val sealTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAD165")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val sealRect = RectF(rInnerLeft, rightColRect.bottom - 42f, rightColRect.right - rPad, rightColRect.bottom - 12f)
        canvas.drawRoundRect(sealRect, 8f, 8f, sealPaint)
        canvas.drawText("ডিজিটাল জরিপ ও নকশা শিট • ভূমি সেবা ডিজিটাল", sealRect.centerX(), sealRect.centerY() + 6f, sealTextPaint)

        return outBitmap
    }

    private fun drawCompassRose(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(228, 250, 244, 226) }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val northArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C62828")
            style = Paint.Style.FILL
        }
        val southArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.FILL
        }
        val lbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C62828")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawCircle(cx, cy, radius, bg)
        canvas.drawCircle(cx, cy, radius, ring)

        val nPath = Path().apply {
            moveTo(cx, cy - radius * 0.68f)
            lineTo(cx - 8f, cy)
            lineTo(cx + 8f, cy)
            close()
        }
        val sPath = Path().apply {
            moveTo(cx, cy + radius * 0.68f)
            lineTo(cx - 7f, cy)
            lineTo(cx + 7f, cy)
            close()
        }
        canvas.drawPath(nPath, northArrow)
        canvas.drawPath(sPath, southArrow)
        canvas.drawText("N / উ", cx, cy - radius - 6f, lbl)
    }

    private fun drawGraphicScaleBar(
        canvas: Canvas,
        left: Float,
        top: Float,
        mapWidthPx: Float,
        mapSpanFeetWidth: Double
    ) {
        val feetPerPx = mapSpanFeetWidth / mapWidthPx.coerceAtLeast(100f)
        val niceStepsFt = doubleArrayOf(10.0, 20.0, 50.0, 100.0, 200.0, 330.0, 500.0, 1000.0)
        val targetPx = mapWidthPx * 0.18
        val chosenFt = niceStepsFt.minByOrNull { kotlin.math.abs((it / feetPerPx) - targetPx) } ?: 100.0
        val barWidthPx = (chosenFt / feetPerPx).toFloat().coerceIn(110f, mapWidthPx * 0.34f)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(232, 250, 244, 226) }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val box = RectF(left, top, left + barWidthPx + 30f, top + 40f)
        canvas.drawRoundRect(box, 8f, 8f, bg)
        canvas.drawRoundRect(box, 8f, 8f, border)

        val barLeft = left + 15f
        val barTop = top + 22f
        val darkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_GREEN }
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val quarter = barWidthPx / 4f
        for (i in 0 until 4) {
            val r = RectF(barLeft + i * quarter, barTop, barLeft + (i + 1) * quarter, barTop + 8f)
            canvas.drawRect(r, if (i % 2 == 0) darkFill else whiteFill)
            canvas.drawRect(r, border)
        }

        val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("স্কেল: 0 — ${chosenFt.toInt()} ফুট", barLeft, top + 16f, txt)
    }

    internal fun drawCircularBitmap(
        canvas: Canvas,
        src: Bitmap,
        dst: RectF,
        borderColor: Int,
        borderWidth: Float
    ) {
        val size = min(dst.width(), dst.height()).roundToInt().coerceAtLeast(32)
        val sq = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val sqCanvas = Canvas(sq)
        val clipPath = Path().apply {
            addOval(RectF(0f, 0f, size.toFloat(), size.toFloat()), Path.Direction.CW)
        }
        sqCanvas.clipPath(clipPath)

        val scale = max(size.toFloat() / src.width, size.toFloat() / src.height)
        val drawW = src.width * scale
        val drawH = src.height * scale
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate((size - drawW) / 2f, (size - drawH) / 2f)
        }
        sqCanvas.drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

        canvas.drawBitmap(sq, Rect(0, 0, size, size), dst, Paint(Paint.FILTER_BITMAP_FLAG))
        sq.recycle()

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }
        canvas.drawOval(dst, ring)
    }

    internal fun loadSurveyorPhotoBitmap(context: Context, photoUri: String?): Bitmap? {
        if (!photoUri.isNullOrBlank()) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { return it }
        }
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_dev_avatar)
        }.getOrNull()
    }

    internal fun drawFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        val originalSize = paint.textSize
        var cur = originalSize
        while (cur > 11f && paint.measureText(text) > maxWidth) {
            cur -= 1f
            paint.textSize = cur
        }
        canvas.drawText(text, x, y, paint)
        paint.textSize = originalSize
    }

    internal fun drawWrappedLines(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        lineStepY: Float,
        maxLines: Int,
        paint: Paint
    ) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
                if (lines.size >= maxLines - 1) break
            }
        }
        if (current.isNotEmpty() && lines.size < maxLines) {
            lines.add(current.toString())
        }
        lines.forEachIndexed { idx, line ->
            drawFittedText(canvas, line, x, startY + idx * lineStepY, maxWidth, paint)
        }
    }

    private fun downloadTileBitmap(urlStr: String): Bitmap? = runCatching {
        val ua = if (urlStr.contains("openstreetmap.org") || urlStr.contains("opentopomap.org")) {
            "BhumiSebaDigital/1.0 (Android Land Survey Tool)"
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
        summaryLine2: String,
        surveyorProfile: SurveyorProfile = SurveyorProfile()
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
        val scale = if (cw < 1200) (1200f / cw).coerceAtMost(2.5f) else 1f
        val outW = (cw * scale).toInt()
        val outH = (ch * scale).toInt()
        val cropped = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(cropped).drawBitmap(
            fullMapBitmap,
            cropRect,
            RectF(0f, 0f, outW.toFloat(), outH.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        val sheet = composeClassicMouzaSheet(
            context = context,
            mapBitmap = cropped,
            sheetTitle = summaryHeader,
            scaleLabel = "স্যাটেলাইট ম্যাপ ক্যাপচার",
            mapSpanFeetWidth = 300.0,
            totalSqFt = 0.0,
            totalPerimeterFeet = 0.0,
            plotRows = listOf(SheetPlotRow("সারসংক্ষেপ", "$summaryLine1 • $summaryLine2")),
            methodExplanation = "কিভাবে পরিমাপ হলো: WGS-84 স্যাটেলাইট ম্যাপ ও হ্যাভারসিন (Haversine) জ্যামিতি সূত্রে হিসাব।",
            surveyorProfile = surveyorProfile,
            sheetCode = "BS-REGION"
        )
        cropped.recycle()
        val fileName = "Map_Survey_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, sheet, fileName)
    }

    internal fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String): ExportResult {
        val w = bitmap.width
        val h = bitmap.height
        val relativePath = "${Environment.DIRECTORY_PICTURES}/BhumiSebaDigital"

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
            ExportResult(uri, "Pictures/BhumiSebaDigital/$fileName", w, h)
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, "BhumiSebaDigital").apply { mkdirs() }
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
        }
    }

    internal fun saveSheetBitmapAsPdf(context: Context, bitmap: Bitmap, fileName: String): ExportResult {
        val w = bitmap.width
        val h = bitmap.height
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(w, h, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        pdfDoc.finishPage(page)

        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/BhumiSebaDigital"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfDoc.writeTo(out)
                    }
                    values.clear()
                    values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                ExportResult(uri, "Documents/BhumiSebaDigital/$fileName", w, h)
            } else {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetDir = File(docsDir, "BhumiSebaDigital").apply { mkdirs() }
                val file = File(targetDir, fileName)
                FileOutputStream(file).use { out ->
                    pdfDoc.writeTo(out)
                }
                ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
            }
        } finally {
            pdfDoc.close()
            bitmap.recycle()
        }
    }
}
