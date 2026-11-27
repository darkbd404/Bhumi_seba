package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.R
import com.example.model.CustomKhatianDocument
import com.example.model.SurveyorProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object KhatianSheetExporter {

    private const val COLOR_PAPER = 0xFFFAF5E8.toInt()
    private const val COLOR_CARD = 0xFFF2E9D0.toInt()
    private const val COLOR_INK_GREEN = 0xFF1B4D3E.toInt()
    private const val COLOR_INK_GOLD = 0xFF8C6B32.toInt()
    private const val COLOR_INK_DARK = 0xFF1D2421.toInt()
    private const val COLOR_INK_MUTED = 0xFF4F5D55.toInt()
    private const val COLOR_GRID = 0xFF28473C.toInt()

    suspend fun exportCustomKhatianPng(
        context: Context,
        doc: CustomKhatianDocument,
        surveyorProfile: SurveyorProfile,
        desktopOutputDir: java.io.File? = null
    ): PngExporter.ExportResult = withContext(Dispatchers.Default) {
        val bmp = buildKhatianSheetBitmap(context, doc, surveyorProfile)
        val safeKh = doc.khatianNumber.ifBlank { "Custom" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "Khatian_${safeKh}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        ExportStreamWriter.savePngBitmap(
            context = context,
            bitmap = bmp,
            fileName = fileName,
            desktopFallbackDir = desktopOutputDir
        )
    }

    suspend fun exportCustomKhatianPdf(
        context: Context,
        doc: CustomKhatianDocument,
        surveyorProfile: SurveyorProfile,
        desktopOutputDir: java.io.File? = null
    ): PngExporter.ExportResult = withContext(Dispatchers.Default) {
        val bmp = buildKhatianSheetBitmap(context, doc, surveyorProfile)
        val safeKh = doc.khatianNumber.ifBlank { "Custom" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "Khatian_${safeKh}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        ExportStreamWriter.savePdfBitmap(
            context = context,
            bitmap = bmp,
            fileName = fileName,
            desktopFallbackDir = desktopOutputDir
        )
    }

    suspend fun exportCustomKhatianBytes(
        context: Context,
        doc: CustomKhatianDocument,
        surveyorProfile: SurveyorProfile,
        asPdf: Boolean
    ): ByteArray = withContext(Dispatchers.Default) {
        val bmp = buildKhatianSheetBitmap(context, doc, surveyorProfile)
        try {
            if (asPdf) {
                runCatching {
                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    try {
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
                        val page = pdfDoc.startPage(pageInfo)
                        page.canvas.drawBitmap(bmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                        pdfDoc.finishPage(page)
                        ExportStreamWriter.encodeToByteArray { out -> pdfDoc.writeTo(out) }
                    } finally {
                        runCatching { pdfDoc.close() }
                    }
                }.getOrElse {
                    ExportStreamWriter.encodeBitmapsAsPortablePdfBytes(listOf(bmp))
                }
            } else {
                ExportStreamWriter.encodeToByteArray { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        } finally {
            bmp.recycle()
        }
    }

    internal fun buildKhatianSheetBitmap(
        context: Context,
        doc: CustomKhatianDocument,
        surveyorProfile: SurveyorProfile
    ): Bitmap {
        val sheetW = 2100
        val rowCount = max(max(doc.owners.size, doc.dags.size), 5)
        val rowH = 52
        val tableHeaderH = 84
        val totalsRowH = 44
        val bodyH = rowCount * rowH + tableHeaderH + totalsRowH
        val headerH = 192
        val summaryH = 88
        val footerH = 186
        val margin = 46
        val sheetH = margin * 2 + headerH + bodyH + summaryH + footerH + 28

        val bmp = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        com.example.ui.components.VintagePaperRenderer.drawAgedStampPaperBackground(
            canvas = canvas,
            w = sheetW,
            h = sheetH,
            showStampCrest = false
        )

        // Subtle Diagonal Watermark
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(14, 107, 76, 37)
            textSize = 92f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.translate(sheetW / 2f, sheetH / 2f)
        canvas.rotate(-22f)
        canvas.drawText("ভূমি সেবা ডিজিটাল • প্রাচীন খতিয়ান শৈলী (খসড়া)", 0f, 0f, wmPaint)
        canvas.restore()

        // Outer Double Cartographic Ledger Border
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
        val bPad = 18f
        canvas.drawRect(bPad, bPad, sheetW - bPad, sheetH - bPad, outerBorder)
        canvas.drawRect(bPad + 9f, bPad + 9f, sheetW - bPad - 9f, sheetH - bPad - 9f, innerBorder)

        val cLeft = margin.toFloat()
        val cRight = (sheetW - margin).toFloat()
        val contentW = cRight - cLeft
        var y = margin.toFloat()

        // 1. Header with App Emblem & Classic Bangladesh Form 5462/5463 Title
        val logoSize = 90f
        val logoBmp = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_art) }.getOrNull()
        val logoRect = RectF(cLeft, y, cLeft + logoSize, y + logoSize)
        if (logoBmp != null) {
            PngExporter.drawCircularBitmap(canvas, logoBmp, logoRect, COLOR_INK_GREEN, 3.5f)
            logoBmp.recycle()
        }

        val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 35f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val khatianBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAD165")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("ডিজিটাল কাস্টম খতিয়ান ফরম (অনুকরণ)", sheetW / 2f, y + 30f, centerSubPaint)
        canvas.drawText(
            "${doc.khatianType.ifBlank { "আরএস (RS) খতিয়ান" }} — খতিয়ান নং: ${doc.khatianNumber.ifBlank { "-" }}",
            sheetW / 2f,
            y + 72f,
            centerTitlePaint
        )

        val khNoBox = RectF(cRight - 270f, y + 6f, cRight, y + 84f)
        canvas.drawRoundRect(khNoBox, 12f, 12f, khatianBadgePaint)
        canvas.drawText("খতিয়ান নং: ${doc.khatianNumber.ifBlank { "-" }}", khNoBox.centerX(), khNoBox.centerY() + 8f, badgeTextPaint)

        y += 102f

        // Admin info bar (Div / Dist / Upazila / Mouza / JL / Resa / Tauzi)
        val metaBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_CARD }
        val metaStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRID
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        val metaRect = RectF(cLeft, y, cRight, y + 72f)
        canvas.drawRect(metaRect, metaBg)
        canvas.drawRect(metaRect, metaStroke)

        val metaFields = buildList {
            add("বিভাগ" to doc.division.ifBlank { "-" })
            add("জেলা" to doc.district.ifBlank { "-" })
            add("উপজেলা/থানা" to doc.upazila.ifBlank { "-" })
            if (doc.unionName.isNotBlank()) add("ইউনিয়ন" to doc.unionName)
            add("মৌজা" to doc.mouzaName.ifBlank { "-" })
            add("জে.এল. নং" to doc.jlNumber.ifBlank { "-" })
            add("রেঃ সাঃ নং" to doc.resaNumber.ifBlank { "-" })
            add("তৌজি নং" to doc.tauziNumber.ifBlank { "-" })
        }
        val fieldW = contentW / metaFields.size
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        metaFields.forEachIndexed { idx, (lbl, value) ->
            val fx = cLeft + idx * fieldW
            if (idx > 0) canvas.drawLine(fx, metaRect.top, fx, metaRect.bottom, metaStroke)
            PngExporter.drawFittedText(canvas, lbl, fx + 12f, metaRect.top + 25f, fieldW - 24f, labelPaint)
            PngExporter.drawFittedText(canvas, value, fx + 12f, metaRect.top + 55f, fieldW - 24f, valPaint)
        }

        y = metaRect.bottom + 14f

        // 2. Authentic Tabular Khatian Grid (Form 5462 / 5463 with 2-tier official headers & 9 columns)
        val colWeights = floatArrayOf(0.22f, 0.06f, 0.06f, 0.06f, 0.10f, 0.15f, 0.07f, 0.16f, 0.12f)
        val colTitles = arrayOf(
            "মালিক, অকৃষি প্রজা বা ইজারাদারের নাম ও ঠিকানা",
            "অংশ",
            "রাজস্ব",
            "দাগ নং",
            "জমির শ্রেণী",
            "দাগের মোট পরিমাণ",
            "দাগের মধ্যে অত্র খতিয়ানের অংশ",
            "অংশানুযায়ী জমির পরিমাণ",
            "দখল বিষয়ক বা অন্যান্য বিশেষ মন্তব্য"
        )
        val colNums = arrayOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯")
        val colXs = FloatArray(colWeights.size + 1)
        colXs[0] = cLeft
        for (i in colWeights.indices) {
            colXs[i + 1] = colXs[i] + contentW * colWeights[i]
        }

        val hdrFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E3EAD8") }
        val hdrRect = RectF(cLeft, y, cRight, y + tableHeaderH)
        canvas.drawRect(hdrRect, hdrFill)
        canvas.drawRect(hdrRect, metaStroke)

        val subHdrY = y + 48f
        canvas.drawLine(cLeft, subHdrY, cRight, subHdrY, metaStroke)

        val colHdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val colNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val col5SplitX = (colXs[4] + colXs[5]) / 2f
        val col6SplitX = colXs[5] + (colXs[6] - colXs[5]) * 0.42f
        val col8SplitX = colXs[7] + (colXs[8] - colXs[7]) * 0.40f

        for (i in colTitles.indices) {
            if (i > 0) {
                val stroke = if (i == 3) outerBorder else metaStroke
                canvas.drawLine(colXs[i], y, colXs[i], y + bodyH, stroke)
            }
            val cx = (colXs[i] + colXs[i + 1]) / 2f
            val cw = colXs[i + 1] - colXs[i] - 10f
            val lines = PngExporter.wrapTextToLines(colTitles[i], cw, 2, colHdrPaint)
            if (lines.size <= 1) {
                canvas.drawText(colTitles[i], cx, y + 30f, colHdrPaint)
            } else {
                canvas.drawText(lines[0], cx, y + 21f, colHdrPaint)
                canvas.drawText(lines[1], cx, y + 41f, colHdrPaint)
            }
            when (i) {
                4 -> {
                    canvas.drawLine(col5SplitX, subHdrY, col5SplitX, y + bodyH - totalsRowH, rowPaintStroke())
                    canvas.drawText("কৃষি ৫(ক)", (colXs[4] + col5SplitX) / 2f, subHdrY + 24f, colNumPaint)
                    canvas.drawText("অকৃষি ৫(খ)", (col5SplitX + colXs[5]) / 2f, subHdrY + 24f, colNumPaint)
                }
                5 -> {
                    canvas.drawLine(col6SplitX, subHdrY, col6SplitX, y + bodyH - totalsRowH, rowPaintStroke())
                    canvas.drawText("একর ৬(ক)", (colXs[5] + col6SplitX) / 2f, subHdrY + 24f, colNumPaint)
                    canvas.drawText("শতাংশ ৬(খ)", (col6SplitX + colXs[6]) / 2f, subHdrY + 24f, colNumPaint)
                }
                7 -> {
                    canvas.drawLine(col8SplitX, subHdrY, col8SplitX, y + bodyH, rowPaintStroke())
                    canvas.drawText("একর ৮(ক)", (colXs[7] + col8SplitX) / 2f, subHdrY + 24f, colNumPaint)
                    canvas.drawText("শতাংশ ৮(খ)", (col8SplitX + colXs[8]) / 2f, subHdrY + 24f, colNumPaint)
                }
                else -> {
                    canvas.drawText(colNums[i], cx, subHdrY + 24f, colNumPaint)
                }
            }
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 18f
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellCenterBold = Paint(cellBoldPaint).apply { textAlign = Paint.Align.CENTER }
        val cellCenterPaint = Paint(cellPaint).apply { textAlign = Paint.Align.CENTER }
        val rowStroke = Paint().apply {
            color = Color.argb(95, 40, 71, 60)
            strokeWidth = 1.4f
        }

        var rowY = y + tableHeaderH
        val totalOwnedDecimal = doc.dags.sumOf { it.thisKhatianDecimal }
        for (r in 0 until rowCount) {
            val rBottom = rowY + rowH
            if (r % 2 == 1) {
                val altPaint = Paint().apply { color = Color.argb(24, 27, 77, 62) }
                canvas.drawRect(cLeft, rowY, cRight, rBottom, altPaint)
            }
            canvas.drawLine(cLeft, rBottom, cRight, rBottom, rowStroke)

            val owner = doc.owners.getOrNull(r)
            if (owner != null) {
                val ownerMain = buildString {
                    append(owner.name.ifBlank { "মালিক #${r + 1}" })
                    if (owner.fatherOrHusband.isNotBlank()) append(" (${owner.fatherOrHusband})")
                }
                val cw0 = colXs[1] - colXs[0] - 14f
                PngExporter.drawFittedText(canvas, ownerMain, colXs[0] + 8f, rowY + 22f, cw0, cellBoldPaint)
                if (owner.address.isNotBlank()) {
                    PngExporter.drawFittedText(canvas, owner.address, colXs[0] + 8f, rowY + 42f, cw0, labelPaint)
                }
                val shareDisplay = owner.shareText.ifBlank { String.format(Locale.US, "%.3f", owner.shareRatio) }
                canvas.drawText(shareDisplay, (colXs[1] + colXs[2]) / 2f, rowY + 24f, cellCenterBold)
                if (totalOwnedDecimal > 0.0 && owner.shareRatio > 0.0) {
                    val ownerDec = totalOwnedDecimal * owner.shareRatio
                    val subP = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER; textSize = 14f }
                    canvas.drawText(String.format(Locale.US, "%.2f", ownerDec), (colXs[1] + colXs[2]) / 2f, rowY + 43f, subP)
                }
                val revTxt = owner.revenue.ifBlank { if (r == 0) doc.totalKhajna.ifBlank { "-" } else "" }
                if (revTxt.isNotBlank()) {
                    canvas.drawText(revTxt, (colXs[2] + colXs[3]) / 2f, rowY + 32f, cellCenterPaint)
                }
            }

            val dag = doc.dags.getOrNull(r)
            if (dag != null) {
                canvas.drawText(dag.dagNumber.ifBlank { "-" }, (colXs[3] + colXs[4]) / 2f, rowY + 32f, cellCenterBold)

                val cls = dag.landClass.ifBlank { "নাল" }
                val isNonAgri = cls.contains("অকৃষি") || cls.contains("বাণিজ্য") || cls.contains("দোকান") || cls.contains("মিল")
                if (isNonAgri) {
                    canvas.drawText(cls, (col5SplitX + colXs[5]) / 2f, rowY + 32f, cellCenterPaint)
                } else {
                    canvas.drawText(cls, (colXs[4] + col5SplitX) / 2f, rowY + 32f, cellCenterPaint)
                }

                // Column 6: Whole Acre 6(ক) | Decimal 6(খ)
                val totWholeAc = kotlin.math.floor(dag.totalDecimal / 100.0).toInt()
                val totRemDec = dag.totalDecimal - totWholeAc * 100.0
                if (totWholeAc > 0) {
                    canvas.drawText(totWholeAc.toString(), (colXs[5] + col6SplitX) / 2f, rowY + 32f, cellCenterBold)
                }
                canvas.drawText(String.format(Locale.US, "%.2f", totRemDec), (col6SplitX + colXs[6]) / 2f, rowY + 32f, cellCenterBold)

                // Column 7: Share
                canvas.drawText(
                    String.format(Locale.US, "%.3f", dag.thisKhatianShare),
                    (colXs[6] + colXs[7]) / 2f,
                    rowY + 32f,
                    cellCenterBold
                )

                // Column 8: Whole Acre 8(ক) | Decimal 8(খ)
                val khWholeAc = kotlin.math.floor(dag.thisKhatianDecimal / 100.0).toInt()
                val khRemDec = dag.thisKhatianDecimal - khWholeAc * 100.0
                val greenCenterBold = Paint(cellCenterBold).apply { color = COLOR_INK_GREEN }
                if (khWholeAc > 0) {
                    canvas.drawText(khWholeAc.toString(), (colXs[7] + col8SplitX) / 2f, rowY + 32f, greenCenterBold)
                }
                canvas.drawText(String.format(Locale.US, "%.2f", khRemDec), (col8SplitX + colXs[8]) / 2f, rowY + 32f, greenCenterBold)

                // Column 9: Remarks
                PngExporter.drawFittedText(canvas, dag.remarks.ifBlank { "-" }, colXs[8] + 8f, rowY + 32f, colXs[9] - colXs[8] - 14f, cellPaint)
            }
            rowY = rBottom
        }

        // Table Bottom Totals Row (like original government Khatian: মোট অংশ ১.০০০ & মোট জমি ২ | ৯২.০০)
        val totBottom = rowY + totalsRowH
        canvas.drawRect(cLeft, rowY, cRight, totBottom, hdrFill)
        val totalShareSum = doc.owners.sumOf { it.shareRatio }
        canvas.drawText(String.format(Locale.US, "%.3f", totalShareSum), (colXs[1] + colXs[2]) / 2f, rowY + 29f, cellCenterBold)
        canvas.drawText("মোট জমি:", (colXs[6] + colXs[7]) / 2f, rowY + 29f, cellCenterBold)
        val sumWholeAc = kotlin.math.floor(totalOwnedDecimal / 100.0).toInt()
        val sumRemDec = totalOwnedDecimal - sumWholeAc * 100.0
        val totGreenBold = Paint(cellCenterBold).apply { color = COLOR_INK_GREEN; textSize = 19f }
        canvas.drawText(sumWholeAc.toString(), (colXs[7] + col8SplitX) / 2f, rowY + 29f, totGreenBold)
        canvas.drawText(String.format(Locale.US, "%.2f", sumRemDec), (col8SplitX + colXs[8]) / 2f, rowY + 29f, totGreenBold)
        rowY = totBottom

        // Outer table outline + Center double divider
        val tableRect = RectF(cLeft, y, cRight, rowY)
        canvas.drawRect(tableRect, metaStroke)
        canvas.drawLine(colXs[3] - 2f, y, colXs[3] - 2f, rowY, outerBorder)

        // 3. Compact Totals Summary Strip
        val sumTop = rowY + 12f
        val sumRect = RectF(cLeft, sumTop, cRight, sumTop + summaryH - 10f)
        canvas.drawRoundRect(sumRect, 10f, 10f, metaBg)
        canvas.drawRoundRect(sumRect, 10f, 10f, metaStroke)

        val totalArea = LandCalculators.fromSqFeet(totalOwnedDecimal * LandCalculators.SQ_FT_PER_DECIMAL)
        val summaryMain = String.format(
            Locale.US,
            "মোট জমি: %.4f একর  |  %.2f শতাংশ  |  %.2f কাঠা  |  %.0f বর্গফুট  |  মোট দাগ: %dটি  |  মালিক অংশ: %.3f",
            totalArea.acre, totalArea.decimal, totalArea.katha, totalArea.sqFeet, doc.dags.size, totalShareSum
        )
        val summaryMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        PngExporter.drawFittedText(canvas, summaryMain, cLeft + 16f, sumRect.top + 31f, contentW - 32f, summaryMainPaint)

        val extraLine = buildString {
            append("খাজনা / কর: ${doc.totalKhajna.ifBlank { "প্রযোজ্য নয়" }}")
            if (doc.possessionNote.isNotBlank()) append("   |   দখল মন্তব্য: ${doc.possessionNote}")
            if (doc.notes.isNotBlank()) append("   |   পূর্ব খতিয়ান ও বিশেষ নোট: ${doc.notes}")
            if (doc.customFields.isNotEmpty()) {
                append("   |   অন্যান্য: " + doc.customFields.joinToString(", ") { "${it.label}: ${it.value}" })
            }
        }
        PngExporter.drawFittedText(canvas, extraLine, cLeft + 16f, sumRect.top + 61f, contentW - 32f, cellPaint)

        // 4. Compact Bottom Certification & Surveyor Details Card (ZERO wasted space below details)
        val footerTop = sumRect.bottom + 12f
        val footerBottom = footerTop + footerH
        val leftNoteW = contentW * 0.56f
        val leftNoteRect = RectF(cLeft, footerTop, cLeft + leftNoteW, footerBottom)
        val rightProfRect = RectF(cLeft + leftNoteW + 14f, footerTop, cRight, footerBottom)

        canvas.drawRoundRect(leftNoteRect, 10f, 10f, metaBg)
        canvas.drawRoundRect(leftNoteRect, 10f, 10f, innerBorder)

        val noteHdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        PngExporter.drawFittedText(canvas, "কাস্টম ডিজিটাল খতিয়ান প্রত্যয়ন ও হিসাব সূত্র", leftNoteRect.left + 16f, leftNoteRect.top + 28f, leftNoteW - 32f, noteHdrPaint)
        canvas.drawLine(leftNoteRect.left + 16f, leftNoteRect.top + 38f, leftNoteRect.right - 16f, leftNoteRect.top + 38f, rowStroke)

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        PngExporter.drawFittedText(
            canvas,
            "• অরিজিনাল পর্চা/খতিয়ান অনুসরণে ডিজিটাল কপি। তারিখ: $dateStr",
            leftNoteRect.left + 16f,
            leftNoteRect.top + 66f,
            leftNoteW - 32f,
            cellPaint
        )
        PngExporter.drawFittedText(
            canvas,
            "• হিসাব সূত্র: ১ একর = ১০০ শতাংশ | ১ শতাংশ = ৪৩৫.৬ বর্গফুট | ১ কাঠা = ৭২০ বর্গফুট",
            leftNoteRect.left + 16f,
            leftNoteRect.top + 96f,
            leftNoteW - 32f,
            cellPaint
        )
        PngExporter.drawFittedText(
            canvas,
            "• ১, ২, ৩ ও ৬–৮ নং কলাম অনুযায়ী অংশ ও জমির পরিমাণ যাচাইকৃত।",
            leftNoteRect.left + 16f,
            leftNoteRect.top + 126f,
            leftNoteW - 32f,
            labelPaint
        )
        PngExporter.drawFittedText(
            canvas,
            "✓ মালিক ও সার্ভেয়ারের ইনপুট অনুযায়ী প্রস্তুত (ভূমি সেবা ডিজিটাল)",
            leftNoteRect.left + 16f,
            leftNoteRect.top + 158f,
            leftNoteW - 32f,
            Paint(cellBoldPaint).apply { color = COLOR_INK_GREEN; textSize = 17f }
        )

        val profBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE5C7") }
        canvas.drawRoundRect(rightProfRect, 10f, 10f, profBg)
        canvas.drawRoundRect(rightProfRect, 10f, 10f, outerBorder)

        val rPad = 16f
        PngExporter.drawFittedText(
            canvas,
            "প্রস্তুতকারক / সার্ভেয়ার বিবরণ",
            rightProfRect.left + rPad,
            rightProfRect.top + 28f,
            rightProfRect.width() - rPad * 2,
            noteHdrPaint
        )
        canvas.drawLine(rightProfRect.left + rPad, rightProfRect.top + 38f, rightProfRect.right - rPad, rightProfRect.top + 38f, rowStroke)

        val avatarSize = 94f
        val avatarTop = rightProfRect.top + 46f
        val avatarRect = RectF(rightProfRect.left + rPad, avatarTop, rightProfRect.left + rPad + avatarSize, avatarTop + avatarSize)
        val avatarBmp = PngExporter.loadSurveyorPhotoBitmap(context, surveyorProfile.photoUri)
        if (avatarBmp != null) {
            PngExporter.drawCircularBitmap(canvas, avatarBmp, avatarRect, COLOR_INK_GREEN, 3.5f)
            avatarBmp.recycle()
        }

        val infoLeft = avatarRect.right + 14f
        val infoW = (rightProfRect.right - rPad - infoLeft).coerceAtLeast(160f)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cleanName = surveyorProfile.name.ifBlank { "Abdus Salam" }
        val cleanDesig = surveyorProfile.designation.ifBlank { "ডিজিটাল সার্ভেয়ার • ভূমি সেবা ডিজিটাল" }
        val cleanPhone = surveyorProfile.phone.ifBlank { "01620230864" }
        val cleanEmail = surveyorProfile.email.ifBlank { "salam230864@gmail.com" }

        PngExporter.drawFittedText(canvas, cleanName, infoLeft, avatarTop + 22f, infoW, namePaint)
        PngExporter.drawFittedText(canvas, cleanDesig, infoLeft, avatarTop + 46f, infoW, Paint(cellBoldPaint).apply { color = COLOR_INK_GREEN; textSize = 17f })
        PngExporter.drawFittedText(canvas, "মোবা: $cleanPhone", infoLeft, avatarTop + 72f, infoW, cellBoldPaint)
        PngExporter.drawFittedText(canvas, "Gmail: $cleanEmail", infoLeft, avatarTop + 96f, infoW, cellBoldPaint)

        val sealRect = RectF(rightProfRect.left + rPad, rightProfRect.bottom - 36f, rightProfRect.right - rPad, rightProfRect.bottom - 8f)
        canvas.drawRoundRect(sealRect, 7f, 7f, khatianBadgePaint)
        canvas.drawText(
            "ডিজিটাল কাস্টম খতিয়ান • ভূমি সেবা ডিজিটাল",
            sealRect.centerX(),
            sealRect.centerY() + 6f,
            Paint(badgeTextPaint).apply { textSize = 17f }
        )

        val disclaimerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8C1D18")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ব্যক্তিগত ব্যবহারের কপি — সরকারি রেজিস্টার/রেকর্ড নয়; নিবন্ধিত দলিল/সরকারি পর্চাই আইনত গ্রাহ্য", sheetW / 2f, sheetH - 8f, disclaimerPaint)

        return bmp
    }

    private fun rowPaintStroke() = Paint().apply {
        color = Color.argb(120, 40, 71, 60)
        strokeWidth = 1.4f
    }
}
