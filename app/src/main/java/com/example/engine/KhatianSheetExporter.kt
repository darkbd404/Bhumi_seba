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
        surveyorProfile: SurveyorProfile
    ): PngExporter.ExportResult = withContext(Dispatchers.Default) {
        val bmp = buildKhatianSheetBitmap(context, doc, surveyorProfile)
        val safeKh = doc.khatianNumber.ifBlank { "Custom" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "Khatian_${safeKh}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        PngExporter.saveBitmapToPictures(context, bmp, fileName)
    }

    suspend fun exportCustomKhatianPdf(
        context: Context,
        doc: CustomKhatianDocument,
        surveyorProfile: SurveyorProfile
    ): PngExporter.ExportResult = withContext(Dispatchers.Default) {
        val bmp = buildKhatianSheetBitmap(context, doc, surveyorProfile)
        val safeKh = doc.khatianNumber.ifBlank { "Custom" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "Khatian_${safeKh}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        PngExporter.saveSheetBitmapAsPdf(context, bmp, fileName)
    }

    internal fun buildKhatianSheetBitmap(
        context: Context,
        doc: CustomKhatianDocument,
        surveyorProfile: SurveyorProfile
    ): Bitmap {
        val sheetW = 2100
        val rowCount = max(max(doc.owners.size, doc.dags.size), 5)
        val rowH = 56
        val tableHeaderH = 66
        val bodyH = rowCount * rowH + tableHeaderH
        val headerH = 210
        val summaryH = 128
        val footerH = 260
        val margin = 52
        val sheetH = max(1420, margin * 2 + headerH + bodyH + summaryH + footerH + 40)

        val bmp = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(COLOR_PAPER)

        // Subtle Diagonal Watermark
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(14, 27, 77, 62)
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.translate(sheetW / 2f, sheetH / 2f)
        canvas.rotate(-24f)
        canvas.drawText("ভূমি সেবা ডিজিটাল • কাস্টম খতিয়ান", 0f, 0f, wmPaint)
        canvas.restore()

        // Outer Double Cartographic Ledger Border
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        val bPad = 20f
        canvas.drawRect(bPad, bPad, sheetW - bPad, sheetH - bPad, outerBorder)
        canvas.drawRect(bPad + 10f, bPad + 10f, sheetW - bPad - 10f, sheetH - bPad - 10f, innerBorder)

        val cLeft = margin.toFloat()
        val cRight = (sheetW - margin).toFloat()
        val contentW = cRight - cLeft
        var y = margin.toFloat()

        // 1. Header with App Emblem & Classic Bangladesh Khatian Title
        val logoSize = 104f
        val logoBmp = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_art) }.getOrNull()
        val logoRect = RectF(cLeft, y, cLeft + logoSize, y + logoSize)
        if (logoBmp != null) {
            PngExporter.drawCircularBitmap(canvas, logoBmp, logoRect, COLOR_INK_GREEN, 3.5f)
            logoBmp.recycle()
        }

        val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 24f
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

        canvas.drawText("ডিজিটাল খতিয়ান ফরম — বাংলাদেশ ফরম নং ৫৪৬৩ (অনুকরণ)", sheetW / 2f, y + 34f, centerSubPaint)
        canvas.drawText(doc.khatianType.ifBlank { "আরএস (RS) খতিয়ান" }, sheetW / 2f, y + 78f, centerTitlePaint)

        val khNoBox = RectF(cRight - 270f, y + 8f, cRight, y + 92f)
        canvas.drawRoundRect(khNoBox, 12f, 12f, khatianBadgePaint)
        canvas.drawText("খতিয়ান নং: ${doc.khatianNumber.ifBlank { "-" }}", khNoBox.centerX(), khNoBox.centerY() + 8f, badgeTextPaint)

        y += 118f

        // Admin info bar (Div / Dist / Upazila / Mouza / JL / Resa / Tauzi)
        val metaBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_CARD }
        val metaStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRID
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
        }
        val metaRect = RectF(cLeft, y, cRight, y + 78f)
        canvas.drawRect(metaRect, metaBg)
        canvas.drawRect(metaRect, metaStroke)

        val metaFields = listOf(
            "বিভাগ" to doc.division.ifBlank { "-" },
            "জেলা" to doc.district.ifBlank { "-" },
            "উপজেলা/থানা" to doc.upazila.ifBlank { "-" },
            "মৌজা" to doc.mouzaName.ifBlank { "-" },
            "জে.এল. নং" to doc.jlNumber.ifBlank { "-" },
            "রেঃ সাঃ নং" to doc.resaNumber.ifBlank { "-" },
            "তৌজি নং" to doc.tauziNumber.ifBlank { "-" }
        )
        val fieldW = contentW / metaFields.size
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        metaFields.forEachIndexed { idx, (lbl, value) ->
            val fx = cLeft + idx * fieldW
            if (idx > 0) canvas.drawLine(fx, metaRect.top, fx, metaRect.bottom, metaStroke)
            PngExporter.drawFittedText(canvas, lbl, fx + 12f, metaRect.top + 28f, fieldW - 24f, labelPaint)
            PngExporter.drawFittedText(canvas, value, fx + 12f, metaRect.top + 60f, fieldW - 24f, valPaint)
        }

        y = metaRect.bottom + 18f

        // 2. Authentic Tabular Khatian Grid
        // Column proportions summing to 1.0f:
        // [Owner(0.28), Share(0.09)] | [Dag(0.10), Class(0.11), DagTotal(0.14), ThisKhatian(0.15), Remarks(0.13)]
        val colWeights = floatArrayOf(0.28f, 0.09f, 0.10f, 0.11f, 0.14f, 0.15f, 0.13f)
        val colTitles = arrayOf(
            "মালিক, প্রজা বা ইজারাদারের নাম, পিতা/স্বামী ও ঠিকানা",
            "অংশ",
            "দাগ নং",
            "জমির শ্রেণি",
            "দাগের মোট জমি",
            "অত্র খতিয়ানের জমি",
            "মন্তব্য"
        )
        val colXs = FloatArray(colWeights.size + 1)
        colXs[0] = cLeft
        for (i in colWeights.indices) {
            colXs[i + 1] = colXs[i] + contentW * colWeights[i]
        }

        val hdrFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E3EAD8") }
        val hdrRect = RectF(cLeft, y, cRight, y + tableHeaderH)
        canvas.drawRect(hdrRect, hdrFill)
        canvas.drawRect(hdrRect, metaStroke)

        val colHdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        for (i in colTitles.indices) {
            if (i > 0) {
                val stroke = if (i == 2) outerBorder else metaStroke
                canvas.drawLine(colXs[i], y, colXs[i], y + bodyH, stroke)
            }
            val cw = colXs[i + 1] - colXs[i] - 16f
            PngExporter.drawWrappedLines(canvas, colTitles[i], colXs[i] + 8f, y + 26f, cw, 24f, 2, colHdrPaint)
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 20f
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowStroke = Paint().apply {
            color = Color.argb(95, 40, 71, 60)
            strokeWidth = 1.4f
        }

        var rowY = y + tableHeaderH
        val totalOwnedDecimal = doc.dags.sumOf { it.thisKhatianDecimal }
        for (r in 0 until rowCount) {
            val rBottom = rowY + rowH
            if (r % 2 == 1) {
                val altPaint = Paint().apply { color = Color.argb(26, 27, 77, 62) }
                canvas.drawRect(cLeft, rowY, cRight, rBottom, altPaint)
            }
            canvas.drawLine(cLeft, rBottom, cRight, rBottom, rowStroke)

            val owner = doc.owners.getOrNull(r)
            if (owner != null) {
                val ownerMain = buildString {
                    append(owner.name.ifBlank { "মালিক #${r + 1}" })
                    if (owner.fatherOrHusband.isNotBlank()) append(" (${owner.fatherOrHusband})")
                }
                val cw0 = colXs[1] - colXs[0] - 18f
                PngExporter.drawFittedText(canvas, ownerMain, colXs[0] + 10f, rowY + 26f, cw0, cellBoldPaint)
                if (owner.address.isNotBlank()) {
                    PngExporter.drawFittedText(canvas, owner.address, colXs[0] + 10f, rowY + 47f, cw0, labelPaint)
                }
                val cw1 = colXs[2] - colXs[1] - 14f
                val shareDisplay = owner.shareText.ifBlank { String.format(Locale.US, "%.3f", owner.shareRatio) }
                PngExporter.drawFittedText(canvas, shareDisplay, colXs[1] + 8f, rowY + 26f, cw1, cellBoldPaint)
                if (totalOwnedDecimal > 0.0 && owner.shareRatio > 0.0) {
                    val ownerDec = totalOwnedDecimal * owner.shareRatio
                    PngExporter.drawFittedText(
                        canvas,
                        String.format(Locale.US, "%.2f শতক", ownerDec),
                        colXs[1] + 8f,
                        rowY + 47f,
                        cw1,
                        labelPaint
                    )
                }
            }

            val dag = doc.dags.getOrNull(r)
            if (dag != null) {
                PngExporter.drawFittedText(canvas, dag.dagNumber.ifBlank { "-" }, colXs[2] + 10f, rowY + 34f, colXs[3] - colXs[2] - 16f, cellBoldPaint)
                PngExporter.drawFittedText(canvas, dag.landClass.ifBlank { "নাল" }, colXs[3] + 10f, rowY + 34f, colXs[4] - colXs[3] - 16f, cellPaint)
                val totAcre = dag.totalDecimal / 100.0
                val totStr = String.format(Locale.US, "%.4f একর (%.2f শতক)", totAcre, dag.totalDecimal)
                PngExporter.drawFittedText(canvas, totStr, colXs[4] + 10f, rowY + 34f, colXs[5] - colXs[4] - 16f, cellPaint)

                val khAcre = dag.thisKhatianDecimal / 100.0
                val khStr = String.format(Locale.US, "%.4f একর (%.2f শতক)", khAcre, dag.thisKhatianDecimal)
                PngExporter.drawFittedText(canvas, khStr, colXs[5] + 10f, rowY + 28f, colXs[6] - colXs[5] - 16f, cellBoldPaint)
                PngExporter.drawFittedText(
                    canvas,
                    String.format(Locale.US, "অংশ: %.3f", dag.thisKhatianShare),
                    colXs[5] + 10f,
                    rowY + 48f,
                    colXs[6] - colXs[5] - 16f,
                    labelPaint
                )
                PngExporter.drawFittedText(canvas, dag.remarks.ifBlank { "-" }, colXs[6] + 10f, rowY + 34f, colXs[7] - colXs[6] - 16f, cellPaint)
            }

            rowY = rBottom
        }

        // Outer table outline
        val tableRect = RectF(cLeft, y, cRight, rowY)
        canvas.drawRect(tableRect, metaStroke)
        // Center double divider between Owners and Dags
        canvas.drawLine(colXs[2] - 2f, y, colXs[2] - 2f, rowY, outerBorder)

        // 3. Totals Summary Strip
        val sumTop = rowY + 14f
        val sumRect = RectF(cLeft, sumTop, cRight, sumTop + summaryH - 12f)
        canvas.drawRoundRect(sumRect, 12f, 12f, metaBg)
        canvas.drawRoundRect(sumRect, 12f, 12f, metaStroke)

        val totalArea = LandCalculators.fromSqFeet(totalOwnedDecimal * LandCalculators.SQ_FT_PER_DECIMAL)
        val totalShareSum = doc.owners.sumOf { it.shareRatio }
        val summaryMain = String.format(
            Locale.US,
            "মোট জমি: %.4f একর  |  %.2f শতাংশ  |  %.2f কাঠা  |  %.0f বর্গফুট  |  মোট দাগ: %dটি  |  মালিক অংশ যোগফল: %.3f",
            totalArea.acre, totalArea.decimal, totalArea.katha, totalArea.sqFeet, doc.dags.size, totalShareSum
        )
        val summaryMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        PngExporter.drawFittedText(canvas, summaryMain, cLeft + 18f, sumRect.top + 38f, contentW - 36f, summaryMainPaint)

        val extraLine = buildString {
            append("খাজনা / কর: ${doc.totalKhajna.ifBlank { "প্রযোজ্য নয়" }}")
            if (doc.notes.isNotBlank()) append("   |   বিশেষ মন্তব্য / পূর্ব খতিয়ান: ${doc.notes}")
        }
        PngExporter.drawFittedText(canvas, extraLine, cLeft + 18f, sumRect.top + 78f, contentW - 36f, cellPaint)

        // 4. Bottom Surveyor Profile Card (Photo + Name + Phone + Gmail + Date + Seal)
        val footerTop = sumRect.bottom + 16f
        val footerBottom = (sheetH - margin + 10).toFloat()
        val leftNoteW = contentW * 0.56f
        val leftNoteRect = RectF(cLeft, footerTop, cLeft + leftNoteW, footerBottom)
        val rightProfRect = RectF(cLeft + leftNoteW + 16f, footerTop, cRight, footerBottom)

        canvas.drawRoundRect(leftNoteRect, 12f, 12f, metaBg)
        canvas.drawRoundRect(leftNoteRect, 12f, 12f, innerBorder)

        val noteHdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_GREEN
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        PngExporter.drawFittedText(canvas, "কাস্টম ডিজিটাল খতিয়ান প্রত্যয়ন ও হিসাব সূত্র", leftNoteRect.left + 18f, leftNoteRect.top + 36f, leftNoteW - 36f, noteHdrPaint)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        PngExporter.drawFittedText(
            canvas,
            "• অরিজিনাল পর্চা/খতিয়ান অনুসরণে ডিজিটাল ফরম্যাটে লিপিবদ্ধ। প্রস্তুতের তারিখ: $dateStr",
            leftNoteRect.left + 18f,
            leftNoteRect.top + 78f,
            leftNoteW - 36f,
            cellPaint
        )
        PngExporter.drawFittedText(
            canvas,
            "• হিসাব সূত্র: ১ একর = ১০০ শতাংশ | ১ শতাংশ = ৪৩৫.৬ বর্গফুট | ১ কাঠা = ৭২০ বর্গফুট",
            leftNoteRect.left + 18f,
            leftNoteRect.top + 114f,
            leftNoteW - 36f,
            cellPaint
        )
        PngExporter.drawFittedText(
            canvas,
            "• তথ্যসমূহ মালিক ও সার্ভেয়ারের ইনপুট অনুযায়ী প্রস্তুত (ভূমি সেবা ডিজিটাল)।",
            leftNoteRect.left + 18f,
            leftNoteRect.top + 150f,
            leftNoteW - 36f,
            labelPaint
        )

        val profBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE5C7") }
        canvas.drawRoundRect(rightProfRect, 12f, 12f, profBg)
        canvas.drawRoundRect(rightProfRect, 12f, 12f, outerBorder)

        val rPad = 18f
        PngExporter.drawFittedText(
            canvas,
            "প্রস্তুতকারক / সার্ভেয়ার বিবরণ",
            rightProfRect.left + rPad,
            rightProfRect.top + 32f,
            rightProfRect.width() - rPad * 2,
            noteHdrPaint
        )

        val avatarSize = 118f
        val avatarTop = rightProfRect.top + 48f
        val avatarRect = RectF(rightProfRect.left + rPad, avatarTop, rightProfRect.left + rPad + avatarSize, avatarTop + avatarSize)
        val avatarBmp = PngExporter.loadSurveyorPhotoBitmap(context, surveyorProfile.photoUri)
        if (avatarBmp != null) {
            PngExporter.drawCircularBitmap(canvas, avatarBmp, avatarRect, COLOR_INK_GREEN, 4f)
            avatarBmp.recycle()
        }

        val infoLeft = avatarRect.right + 16f
        val infoW = (rightProfRect.right - rPad - infoLeft).coerceAtLeast(160f)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cleanName = surveyorProfile.name.ifBlank { "Abdus Salam" }
        val cleanDesig = surveyorProfile.designation.ifBlank { "ডিজিটাল সার্ভেয়ার • ভূমি সেবা ডিজিটাল" }
        val cleanPhone = surveyorProfile.phone.ifBlank { "01620230864" }
        val cleanEmail = surveyorProfile.email.ifBlank { "salam230864@gmail.com" }

        PngExporter.drawFittedText(canvas, cleanName, infoLeft, avatarTop + 26f, infoW, namePaint)
        PngExporter.drawFittedText(canvas, cleanDesig, infoLeft, avatarTop + 54f, infoW, colHdrPaint)
        PngExporter.drawFittedText(canvas, "মোবা: $cleanPhone", infoLeft, avatarTop + 86f, infoW, cellBoldPaint)
        PngExporter.drawFittedText(canvas, "Gmail: $cleanEmail", infoLeft, avatarTop + 114f, infoW, cellBoldPaint)

        val sealRect = RectF(rightProfRect.left + rPad, rightProfRect.bottom - 44f, rightProfRect.right - rPad, rightProfRect.bottom - 12f)
        canvas.drawRoundRect(sealRect, 8f, 8f, khatianBadgePaint)
        canvas.drawText("ডিজিটাল কাস্টম খতিয়ান • ভূমি সেবা ডিজিটাল", sealRect.centerX(), sealRect.centerY() + 7f, badgeTextPaint)

        return bmp
    }
}
