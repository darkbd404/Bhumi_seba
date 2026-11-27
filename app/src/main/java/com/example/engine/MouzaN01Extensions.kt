package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.model.CalibrationRefLine
import com.example.model.GlobalSurveySettings
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.ParcelTableRow
import com.example.model.PdfInspectionReport
import com.example.model.PolygonVertex
import com.example.model.SurveyorProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class MultiLineCalibOutput(
    val scaleXMultiplier: Double,
    val scaleYMultiplier: Double,
    val avgMultiplier: Double,
    val updatedLines: List<CalibrationRefLine>,
    val rmsDeviationPct: Double,
    val paperShrinkageNoteBn: String
)

object MouzaN01Extensions {

    /**
     * Pure-Kotlin PDF capabilities & uncompressed vector stream inspector.
     * Detects encryption, image vs vector objects, text scale suggestions (e.g. 32"=1 mile),
     * and extracts candidate polygons from uncompressed PDF drawing streams.
     */
    fun inspectPdfStructure(file: File): PdfInspectionReport {
        if (!file.exists()) {
            return PdfInspectionReport(
                isPdf = false,
                pageCount = 0,
                isEncrypted = false,
                hasVectorPaths = false,
                hasTextObjects = false,
                vectorSegmentCount = 0,
                disclosureBn = "ফাইলটি খুঁজে পাওয়া যায়নি।"
            )
        }
        val bytes = runCatching {
            val maxRead = min(file.length(), 4L * 1024L * 1024L).toInt()
            file.inputStream().use { ins ->
                val buf = ByteArray(maxRead)
                val read = ins.read(buf)
                if (read > 0) buf.copyOf(read) else ByteArray(0)
            }
        }.getOrElse { ByteArray(0) }

        val text = String(bytes, Charsets.ISO_8859_1)
        val isPdf = text.startsWith("%PDF") || file.extension.equals("pdf", ignoreCase = true)
        if (!isPdf) {
            return PdfInspectionReport(
                isPdf = false,
                pageCount = 1,
                isEncrypted = false,
                hasVectorPaths = false,
                hasTextObjects = false,
                vectorSegmentCount = 0,
                disclosureBn = "এটি একটি রাস্টার ছবি — অটো-ফিল ও পলিগন পয়েন্টার দিয়ে ভেক্টর দাগ তৈরি হবে।"
            )
        }

        val isEncrypted = text.contains("/Encrypt")
        val hasFlate = text.contains("/FlateDecode")
        val hasTextObjects = text.contains("Tj") || text.contains("TJ") || text.contains("/Font")

        val mediaBoxRegex = Regex("""/MediaBox\s*\[\s*(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s*]""")
        val mbMatch = mediaBoxRegex.find(text)
        val mbX0 = mbMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val mbY0 = mbMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
        val mbX1 = mbMatch?.groupValues?.get(3)?.toFloatOrNull() ?: 1000f
        val mbY1 = mbMatch?.groupValues?.get(4)?.toFloatOrNull() ?: 1000f
        val pageW = (mbX1 - mbX0).roundToInt().coerceAtLeast(100)
        val pageH = (mbY1 - mbY0).roundToInt().coerceAtLeast(100)

        val pageCount = Regex("""/Type\s*/Page[^s]""").findAll(text).count().coerceAtLeast(1)

        // Extract uncompressed PDF move/line/rect path sequences
        val extractedPolys = ArrayList<List<Pair<Float, Float>>>()
        var currentPoly = ArrayList<Pair<Float, Float>>()
        var vectorSegments = 0

        val opRegex = Regex("""(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+(m|l)\b|(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+re\b|\b(h|S|s|f|F|B|b|n)\b""")
        opRegex.findAll(text).forEach { m ->
            when {
                m.groupValues[3] == "m" -> {
                    if (currentPoly.size >= 3) extractedPolys.add(currentPoly)
                    currentPoly = ArrayList()
                    val x = m.groupValues[1].toFloatOrNull() ?: 0f
                    val y = m.groupValues[2].toFloatOrNull() ?: 0f
                    val nx = ((x - mbX0) / pageW.toFloat()).coerceIn(0f, 1f)
                    val ny = (1f - (y - mbY0) / pageH.toFloat()).coerceIn(0f, 1f)
                    currentPoly.add(nx to ny)
                }
                m.groupValues[3] == "l" -> {
                    val x = m.groupValues[1].toFloatOrNull() ?: 0f
                    val y = m.groupValues[2].toFloatOrNull() ?: 0f
                    val nx = ((x - mbX0) / pageW.toFloat()).coerceIn(0f, 1f)
                    val ny = (1f - (y - mbY0) / pageH.toFloat()).coerceIn(0f, 1f)
                    currentPoly.add(nx to ny)
                    vectorSegments++
                }
                m.groupValues[4].isNotEmpty() -> {
                    val rx = m.groupValues[4].toFloatOrNull() ?: 0f
                    val ry = m.groupValues[5].toFloatOrNull() ?: 0f
                    val rw = m.groupValues[6].toFloatOrNull() ?: 0f
                    val rh = m.groupValues[7].toFloatOrNull() ?: 0f
                    if (abs(rw) > 8f && abs(rh) > 8f && abs(rw) < pageW * 0.96f) {
                        val x0 = ((rx - mbX0) / pageW.toFloat()).coerceIn(0f, 1f)
                        val x1 = (((rx + rw) - mbX0) / pageW.toFloat()).coerceIn(0f, 1f)
                        val y0 = (1f - ((ry + rh) - mbY0) / pageH.toFloat()).coerceIn(0f, 1f)
                        val y1 = (1f - (ry - mbY0) / pageH.toFloat()).coerceIn(0f, 1f)
                        extractedPolys.add(listOf(x0 to y0, x1 to y0, x1 to y1, x0 to y1))
                        vectorSegments += 4
                    }
                }
                m.groupValues[8] in listOf("h", "S", "s", "f", "F", "B", "b") -> {
                    if (currentPoly.size >= 3) {
                        extractedPolys.add(currentPoly)
                    }
                    currentPoly = ArrayList()
                }
            }
        }
        if (currentPoly.size >= 3) extractedPolys.add(currentPoly)

        // Detect scale suggestion from visible ASCII/UTF-8 text
        val utf8Text = runCatching { String(bytes, Charsets.UTF_8) }.getOrDefault(text)
        var detectedScaleLabel: String? = null
        var suggestedFpi: Double? = null
        val scalePatterns = listOf(
            Regex("""1\s*:\s*3[,]?960|16\s*(?:"|''|in|inch|ইঞ্চি|")\s*=\s*1\s*(?:mile|মাইল)|১৬\s*"?\s*=\s*১\s*মাইল""", RegexOption.IGNORE_CASE) to (330.0 to "16\" = 1 Mile (1:3,960)"),
            Regex("""1\s*:\s*1[,]?980|32\s*(?:"|''|in|inch|ইঞ্চি|")\s*=\s*1\s*(?:mile|মাইল)|৩২\s*"?\s*=\s*১\s*মাইল""", RegexOption.IGNORE_CASE) to (165.0 to "32\" = 1 Mile (1:1,980)"),
            Regex("""1\s*:\s*990|64\s*(?:"|''|in|inch|ইঞ্চি|")\s*=\s*1\s*(?:mile|মাইল)|৬৪\s*"?\s*=\s*১\s*মাইল""", RegexOption.IGNORE_CASE) to (82.5 to "64\" = 1 Mile (1:990)"),
            Regex("""1\s*:\s*792|80\s*(?:"|''|in|inch|ইঞ্চি|")\s*=\s*1\s*(?:mile|মাইল)|৮০\s*"?\s*=\s*১\s*মাইল""", RegexOption.IGNORE_CASE) to (66.0 to "80\" = 1 Mile (1:792)")
        )
        for ((rx, pair) in scalePatterns) {
            if (rx.containsMatchIn(utf8Text)) {
                suggestedFpi = pair.first
                detectedScaleLabel = pair.second
                break
            }
        }

        val hasVectors = extractedPolys.isNotEmpty() || vectorSegments > 0
        val disclosure = when {
            isEncrypted -> "এই PDF পাসওয়ার্ড/এনক্রিপশন দ্বারা সুরক্ষিত। আনলক করা কপি ব্যবহার করুন।"
            hasVectors -> "আনকম্প্রেসড ভেক্টর পাথ (${extractedPolys.size}টি সম্ভাব্য পলিগন) পাওয়া গেছে — সরাসরি প্লটে কনভার্ট করা যাবে।"
            hasFlate -> "এই PDF-এর স্ট্রিম /FlateDecode কমপ্রেসড অথবা স্ক্যানড ইমেজ। সরাসরি কাঁচা ভেক্টর পলিগন পড়া যায় না — তবে 'অটো-ফিল' ও 'পলিগন পয়েন্টার' নিখুঁতভাবে কাজ করবে।"
            else -> "স্ক্যানড/ইমেজ-ভিত্তিক মৌজা নকশা — অটো-ফিল বা পলিগন দিয়ে দাগ চিহ্নিত করুন।"
        }

        return PdfInspectionReport(
            isPdf = true,
            pageCount = pageCount,
            isEncrypted = isEncrypted,
            hasVectorPaths = hasVectors,
            hasTextObjects = hasTextObjects,
            vectorSegmentCount = vectorSegments,
            extractedVectorPolygons = extractedPolys.take(80),
            detectedScaleText = detectedScaleLabel,
            suggestedFeetPerInch = suggestedFpi,
            pageWidthPts = pageW,
            pageHeightPts = pageH,
            disclosureBn = disclosure
        )
    }

    /**
     * Multi-line least-squares scale calibration + separate X/Y shrinkage detection
     * + paper physical size verification.
     */
    fun calibrateScaleMultiLine(
        lines: List<CalibrationRefLine>,
        pageWidthPts: Int,
        pageHeightPts: Int,
        baseScale: MouzaScale,
        expectedPaperWidthIn: Double = 0.0,
        expectedPaperHeightIn: Double = 0.0
    ): MultiLineCalibOutput {
        val valid = lines.filter { it.actualFeet > 0.0 && it.measuredFeetBefore > 0.0 }
        if (valid.isEmpty()) {
            return MultiLineCalibOutput(
                scaleXMultiplier = 1.0,
                scaleYMultiplier = 1.0,
                avgMultiplier = 1.0,
                updatedLines = lines,
                rmsDeviationPct = 0.0,
                paperShrinkageNoteBn = ""
            )
        }

        val pageFtW = (pageWidthPts / 72.0) * baseScale.feetPerInch
        val pageFtH = (pageHeightPts / 72.0) * baseScale.feetPerInch

        var sumXWeight = 0.0
        var sumXRatio = 0.0
        var sumYWeight = 0.0
        var sumYRatio = 0.0
        var sumAllRatio = 0.0

        valid.forEach { ln ->
            val dxFt = abs(ln.p2Norm.first - ln.p1Norm.first) * pageFtW
            val dyFt = abs(ln.p2Norm.second - ln.p1Norm.second) * pageFtH
            val baseDist = hypot(dxFt, dyFt).coerceAtLeast(1e-6)
            val ratio = ln.actualFeet / baseDist
            sumAllRatio += ratio
            val wx = dxFt / baseDist
            val wy = dyFt / baseDist
            sumXWeight += wx
            sumXRatio += wx * ratio
            sumYWeight += wy
            sumYRatio += wy * ratio
        }

        val avgMult = (sumAllRatio / valid.size).coerceIn(0.2, 5.0)
        val scaleX = if (sumXWeight > 0.25) (sumXRatio / sumXWeight).coerceIn(0.2, 5.0) else avgMult
        val scaleY = if (sumYWeight > 0.25) (sumYRatio / sumYWeight).coerceIn(0.2, 5.0) else avgMult

        val updated = lines.map { ln ->
            val dxFt = (ln.p2Norm.first - ln.p1Norm.first) * pageFtW * scaleX
            val dyFt = (ln.p2Norm.second - ln.p1Norm.second) * pageFtH * scaleY
            val calibratedDist = hypot(dxFt, dyFt).coerceAtLeast(1e-6)
            val errPct = if (ln.actualFeet > 0.0) {
                abs(calibratedDist - ln.actualFeet) / ln.actualFeet * 100.0
            } else 0.0
            ln.copy(deviationPct = errPct)
        }

        val rmsDev = if (updated.isNotEmpty()) {
            sqrt(updated.sumOf { it.deviationPct * it.deviationPct } / updated.size)
        } else 0.0

        val actualPaperW = pageWidthPts / 72.0
        val actualPaperH = pageHeightPts / 72.0
        val paperNote = buildString {
            if (expectedPaperWidthIn > 0.5 && expectedPaperHeightIn > 0.5) {
                val pctW = ((actualPaperW - expectedPaperWidthIn) / expectedPaperWidthIn) * 100.0
                val pctH = ((actualPaperH - expectedPaperHeightIn) / expectedPaperHeightIn) * 100.0
                append(
                    String.format(
                        Locale.US,
                        "পেপার মাপ: %.1f\"×%.1f\" (প্রত্যাশিত %.1f\"×%.1f\" • পার্থক্য X:%.1f%%, Y:%.1f%%)",
                        actualPaperW, actualPaperH, expectedPaperWidthIn, expectedPaperHeightIn, pctW, pctH
                    )
                )
            } else {
                append(String.format(Locale.US, "পেজ সাইজ: %.2f\" × %.2f\" • X/Y স্কেল: %.3f / %.3f", actualPaperW, actualPaperH, scaleX, scaleY))
            }
        }

        return MultiLineCalibOutput(
            scaleXMultiplier = scaleX,
            scaleYMultiplier = scaleY,
            avgMultiplier = (scaleX + scaleY) * 0.5,
            updatedLines = updated,
            rmsDeviationPct = rmsDev,
            paperShrinkageNoteBn = paperNote
        )
    }

    /**
     * Map Cleaner: Adaptive threshold + optional despeckle/sharpen/invert for faded Mouza scans.
     */
    fun cleanMouzaBitmap(
        src: Bitmap,
        contrastGain: Float = 1.35f,
        inkThreshold: Int = 188,
        despeckle: Boolean = true,
        invert: Boolean = false
    ): Bitmap {
        val maxDim = 1600
        val ratio = min(1f, maxDim.toFloat() / max(src.width, src.height))
        val w = (src.width * ratio).roundToInt().coerceAtLeast(32)
        val h = (src.height * ratio).roundToInt().coerceAtLeast(32)
        val work = if (ratio < 0.99f) Bitmap.createScaledBitmap(src, w, h, true) else src.copy(Bitmap.Config.ARGB_8888, false)
        val n = w * h
        val px = IntArray(n)
        work.getPixels(px, 0, w, 0, 0, w, h)
        if (work !== src) work.recycle()

        val luma = IntArray(n)
        for (i in 0 until n) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val raw = (r * 77 + g * 150 + b * 29) shr 8
            val boosted = (((raw - 128) * contrastGain) + 128f).roundToInt().coerceIn(0, 255)
            luma[i] = boosted
        }

        val outPx = IntArray(n)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val idx = row + x
                var v = luma[idx]
                if (despeckle && x in 1 until (w - 1) && y in 1 until (h - 1) && v < inkThreshold) {
                    var darkNeighbors = 0
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            if (luma[(y + dy) * w + (x + dx)] < inkThreshold) darkNeighbors++
                        }
                    }
                    if (darkNeighbors <= 1) v = 255 // isolated noise speck
                }
                val clean = if (v < inkThreshold) (v * 0.45f).roundToInt().coerceIn(0, 90) else 255
                val finalGray = if (invert) 255 - clean else clean
                outPx[idx] = Color.rgb(finalGray, finalGray, finalGray)
            }
        }
        return Bitmap.createBitmap(outPx, w, h, Bitmap.Config.ARGB_8888)
    }

    fun rotateBitmapDeg(src: Bitmap, degrees: Int): Bitmap {
        val norm = ((degrees % 360) + 360) % 360
        if (norm == 0) return src
        val m = Matrix().apply { postRotate(norm.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /**
     * Splits a normalized polygon into two non-empty sub-polygons across the line through (p1Norm, p2Norm).
     */
    fun splitNormalizedPolygonByLine(
        polyNorm: List<Pair<Float, Float>>,
        p1Norm: Pair<Float, Float>,
        p2Norm: Pair<Float, Float>
    ): Pair<List<Pair<Float, Float>>, List<Pair<Float, Float>>>? {
        if (polyNorm.size < 3) return null
        val dx = (p2Norm.first - p1Norm.first).toDouble()
        val dy = (p2Norm.second - p1Norm.second).toDouble()
        val len = hypot(dx, dy)
        if (len < 1e-6) return null
        val nx = -dy / len
        val ny = dx / len
        val limit = p1Norm.first.toDouble() * nx + p1Norm.second.toDouble() * ny
        val verts = polyNorm.map { PolygonVertex(it.first.toDouble(), it.second.toDouble()) }
        val partA = PolygonDividerEngine.clipPolygonByHalfPlane(verts, nx, ny, limit, keepGreater = false)
        val partB = PolygonDividerEngine.clipPolygonByHalfPlane(verts, nx, ny, limit, keepGreater = true)
        if (partA.size < 3 || partB.size < 3) return null
        if (PolygonDividerEngine.polygonAreaSqFt(partA) < 1e-8 || PolygonDividerEngine.polygonAreaSqFt(partB) < 1e-8) return null
        return partA.map { it.x.toFloat().coerceIn(0f, 1f) to it.y.toFloat().coerceIn(0f, 1f) } to
            partB.map { it.x.toFloat().coerceIn(0f, 1f) to it.y.toFloat().coerceIn(0f, 1f) }
    }

    /**
     * N01: Non-destructive annotated PDF export:
     * Page 1: High-res base Mouza raster + crisp vector plot fills/boundaries/holes, side dimensions, North arrow & Scale bar.
     * Page 2: Complete Parcel Schedule Table + Surveyor Profile + Disclaimer.
     */
    fun exportNonDestructiveAnnotatedPdf(
        context: Context,
        sourceBitmap: Bitmap,
        plots: List<MouzaPlot>,
        parcelRows: List<ParcelTableRow>,
        title: String,
        scale: MouzaScale,
        pageWidthPts: Int,
        pageHeightPts: Int,
        scaleXMult: Double,
        scaleYMult: Double,
        surveyorProfile: SurveyorProfile,
        settings: GlobalSurveySettings = GlobalSurveySettings()
    ): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "AnnotatedExports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(dir, "Mouza_Annotated_${ts}.pdf")

        val pageW = 842
        val pageH = 595 // A4 Landscape for Sheet View

        fun drawPage1(c1: Canvas) {
            c1.drawColor(Color.parseColor("#FAF6EB"))

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F5B48")
                textSize = 14f
                isFakeBoldText = true
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3E554D")
                textSize = 9.5f
            }
            c1.drawText("মৌজা অ্যানোটেটেড ভেক্টর শিট (নন-ডেস্ট্রাক্টিভ) — $title", 28f, 28f, headerPaint)
            c1.drawText("স্কেল: ${scale.labelBn} (${scale.ratioLabel}) | X/Y: ${String.format(Locale.US, "%.3f/%.3f", scaleXMult, scaleYMult)} | সার্ভেয়ার: ${surveyorProfile.name} (${surveyorProfile.phone})", 28f, 43f, subPaint)

            val mapRect = RectF(28f, 54f, pageW - 28f, pageH - 52f)
            val bmpAspect = sourceBitmap.width.toFloat() / sourceBitmap.height.coerceAtLeast(1).toFloat()
            val boxAspect = mapRect.width() / mapRect.height()
            val drawRect = if (bmpAspect > boxAspect) {
                val h = mapRect.width() / bmpAspect
                val top = mapRect.centerY() - h / 2f
                RectF(mapRect.left, top, mapRect.right, top + h)
            } else {
                val w = mapRect.height() * bmpAspect
                val left = mapRect.centerX() - w / 2f
                RectF(left, mapRect.top, left + w, mapRect.bottom)
            }

            val framePaint = Paint().apply {
                color = Color.parseColor("#8CA39A")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            c1.drawBitmap(sourceBitmap, null, drawRect, Paint(Paint.FILTER_BITMAP_FLAG))
            c1.drawRect(drawRect, framePaint)

            fun mapX(nx: Float) = drawRect.left + nx * drawRect.width()
            fun mapY(ny: Float) = drawRect.top + ny * drawRect.height()

            val pageFtW = (pageWidthPts / 72.0) * scale.feetPerInch * scaleXMult
            val pageFtH = (pageHeightPts / 72.0) * scale.feetPerInch * scaleYMult
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val strokeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

            plots.forEach { plot ->
                val poly = plot.effectivePolygon() ?: return@forEach
                if (poly.size < 3) return@forEach
                val path = Path().apply {
                    poly.forEachIndexed { idx, (nx, ny) ->
                        val px = mapX(nx); val py = mapY(ny)
                        if (idx == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                val rgb = Color.argb(
                    if (plot.isRoad) 95 else 65,
                    (plot.color.red * 255).roundToInt(),
                    (plot.color.green * 255).roundToInt(),
                    (plot.color.blue * 255).roundToInt()
                )
                val solid = Color.argb(
                    255,
                    (plot.color.red * 255).roundToInt(),
                    (plot.color.green * 255).roundToInt(),
                    (plot.color.blue * 255).roundToInt()
                )
                c1.drawPath(path, Paint().apply { color = rgb; style = Paint.Style.FILL })
                c1.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = solid; style = Paint.Style.STROKE; strokeWidth = 1.8f })

                plot.holePolygonsNormalized.forEach { hole ->
                    if (hole.size >= 3) {
                        val hp = Path().apply {
                            hole.forEachIndexed { idx, (hx, hy) ->
                                val px = mapX(hx); val py = mapY(hy)
                                if (idx == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        c1.drawPath(hp, Paint().apply { color = Color.argb(170, 255, 255, 255); style = Paint.Style.FILL })
                        c1.drawPath(hp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 1.2f })
                    }
                }

                if (!plot.hideMeasures) {
                    for (i in poly.indices) {
                        val a = poly[i]
                        val b = poly[(i + 1) % poly.size]
                        val lenFt = hypot((b.first - a.first) * pageFtW, (b.second - a.second) * pageFtH)
                        val sx = mapX((a.first + b.first) * 0.5f)
                        val sy = mapY((a.second + b.second) * 0.5f)
                        val txt = LandCalculators.formatCompactFeet(lenFt)
                        labelPaint.color = solid
                        c1.drawText(txt, sx, sy, strokeTextPaint)
                        c1.drawText(txt, sx, sy, labelPaint)
                    }
                }

                val cx = mapX(poly.map { it.first }.average().toFloat())
                val cy = mapY(poly.map { it.second }.average().toFloat())
                val row = parcelRows.find { it.plotId == plot.id }
                val dagLbl = if (plot.dagNumber.isNotBlank()) "দাগ ${plot.dagNumber}" else "দাগ #${plot.plotIndex}"
                val areaLbl = if (row != null) String.format(Locale.US, "%.2f শতক", row.decimal) else ""
                labelPaint.color = Color.parseColor("#111D18")
                c1.drawText(dagLbl, cx, cy - 3f, strokeTextPaint)
                c1.drawText(dagLbl, cx, cy - 3f, labelPaint)
                if (areaLbl.isNotEmpty() && !plot.hideMeasures) {
                    c1.drawText(areaLbl, cx, cy + 9f, strokeTextPaint)
                    c1.drawText(areaLbl, cx, cy + 9f, labelPaint)
                }
            }

            val nx = pageW - 54f
            val ny = 84f
            c1.drawCircle(nx, ny, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })
            c1.drawCircle(nx, ny, 16f, framePaint)
            c1.drawText("উ / N", nx, ny + 4f, labelPaint.apply { color = Color.parseColor("#0F5B48") })

            c1.drawText(
                "মূল PDF অপরিবর্তিত রেখে নতুন অ্যানোটেটেড PDF রপ্তানি করা হয়েছে • তারিখ: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())}",
                28f,
                pageH - 22f,
                subPaint
            )
        }

        fun drawPage2(c2: Canvas) {
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F5B48")
                textSize = 14f
                isFakeBoldText = true
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3E554D")
                textSize = 9.5f
            }
            val framePaint = Paint().apply {
                color = Color.parseColor("#8CA39A")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            c2.drawColor(Color.parseColor("#FAF6EB"))
            var y = 46f
            c2.drawText("দাগভিত্তিক পরিমাপ তালিকা (Parcel Schedule)", 36f, y, headerPaint)
            y += 18f
            c2.drawText("মৌজা / প্রজেক্ট: $title  |  ইউনিট: ${settings.activePreset().nameBn}", 36f, y, subPaint)
            y += 18f
            val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B2622"); textSize = 9.8f }
            c2.drawText("দাগ        | খতিয়ান | শতাংশ      | ±ত্রুটি    | কাঠা     | বর্গফুট    | পরিসীমা (ফুট) | মন্তব্য", 36f, y, bodyP.apply { isFakeBoldText = true })
            y += 12f
            c2.drawLine(36f, y, 560f, y, framePaint)
            y += 16f
            bodyP.isFakeBoldText = false
            parcelRows.take(36).forEach { r ->
                val line = String.format(
                    Locale.US,
                    "%-10s | %-6s | %-10.3f | ±%-7.3f | %-8.3f | %-10.1f | %-12.2f | %s",
                    r.dagNumber.take(10), r.khatianNumber.take(6), r.decimal, r.errorDecimal, r.katha, r.sqFeet, r.perimeterFt, r.remarks.take(18)
                )
                c2.drawText(line, 36f, y, bodyP)
                y += 16f
            }
            val usable = parcelRows.filterNot { it.isRoad }
            y += 8f
            c2.drawLine(36f, y, 560f, y, framePaint)
            y += 18f
            c2.drawText(
                String.format(
                    Locale.US,
                    "মোট ব্যবহারযোগ্য (${usable.size} দাগ): %.3f শতক • %.3f কাঠা • %.1f বর্গফুট",
                    usable.sumOf { it.decimal }, usable.sumOf { it.katha }, usable.sumOf { it.sqFeet }
                ),
                36f,
                y,
                headerPaint.apply { textSize = 11.5f }
            )
            c2.drawText(
                "সতর্কতা: এটি ডিজিটাল সার্ভে প্রাক্কলন; আইনগতভাবে চূড়ান্ত নয়।",
                36f,
                815f,
                subPaint
            )
        }

        val nativeOk = runCatching {
            val doc = PdfDocument()
            try {
                val page1 = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
                drawPage1(page1.canvas)
                doc.finishPage(page1)
                if (parcelRows.isNotEmpty()) {
                    val page2 = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
                    drawPage2(page2.canvas)
                    doc.finishPage(page2)
                }
                FileOutputStream(outFile).use { doc.writeTo(it) }
                outFile.exists() && outFile.length() > 128
            } finally {
                runCatching { doc.close() }
            }
        }.getOrDefault(false)

        if (!nativeOk) {
            val pages = mutableListOf<Bitmap>()
            try {
                val b1 = Bitmap.createBitmap(pageW * 2, pageH * 2, Bitmap.Config.ARGB_8888)
                Canvas(b1).apply { scale(2f, 2f); drawPage1(this) }
                pages += b1
                if (parcelRows.isNotEmpty()) {
                    val b2 = Bitmap.createBitmap(595 * 2, 842 * 2, Bitmap.Config.ARGB_8888)
                    Canvas(b2).apply { scale(2f, 2f); drawPage2(this) }
                    pages += b2
                }
                val bytes = ExportStreamWriter.encodeBitmapsAsPortablePdfBytes(pages)
                FileOutputStream(outFile).use { it.write(bytes) }
            } finally {
                pages.forEach { if (!it.isRecycled) it.recycle() }
            }
        }
        return outFile
    }

    fun exportCleanedPdf(context: Context, cleanedBmp: Bitmap, title: String): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "CleanedMouzaDocs").apply { mkdirs() }
        val file = File(dir, "Cleaned_${System.currentTimeMillis()}.pdf")
        val nativeOk = runCatching {
            val doc = PdfDocument()
            try {
                val info = PdfDocument.PageInfo.Builder(cleanedBmp.width.coerceAtLeast(300), cleanedBmp.height.coerceAtLeast(300), 1).create()
                val page = doc.startPage(info)
                page.canvas.drawBitmap(cleanedBmp, 0f, 0f, null)
                doc.finishPage(page)
                FileOutputStream(file).use { doc.writeTo(it) }
                file.exists() && file.length() > 128
            } finally {
                runCatching { doc.close() }
            }
        }.getOrDefault(false)
        if (!nativeOk) {
            val bytes = ExportStreamWriter.encodeBitmapsAsPortablePdfBytes(listOf(cleanedBmp))
            FileOutputStream(file).use { it.write(bytes) }
        }
        return file
    }
}
