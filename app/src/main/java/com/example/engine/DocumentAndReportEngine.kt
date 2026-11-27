package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.example.model.DeedDraftDocument
import com.example.model.DeedTemplateType
import com.example.model.OcrExtractedField
import com.example.model.SurveyReportConfig
import com.example.model.SurveyorProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class ExportedFileResult(
    val uri: Uri?,
    val displayPath: String
)

object DocumentAndReportEngine {

    const val LEGAL_DRAFT_DISCLAIMER_BN =
        "সতর্কতা: এটি শুধুমাত্র প্রাথমিক খসড়া (Draft)। রেজিস্ট্রিকৃত সরকারি দলিল বা আইনি সনদ হিসেবে গণ্য করার আগে অবশ্যই বিজ্ঞ আইনজীবী/দলিল লেখক দ্বারা যাচাই করুন।"
    const val LEGAL_DRAFT_DISCLAIMER_EN =
        "LEGAL NOTICE: This document is an UNCERTIFIED DRAFT for reference only. Review with a qualified advocate or sub-registrar before execution."

    fun buildDeedText(doc: DeedDraftDocument, profile: SurveyorProfile = SurveyorProfile()): String {
        val bn = doc.languageBn
        val title = doc.title.ifBlank { if (bn) doc.templateType.titleBn else doc.templateType.titleEn }
        val date = doc.deedDate.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()) }
        val disclaimer = if (bn) LEGAL_DRAFT_DISCLAIMER_BN else LEGAL_DRAFT_DISCLAIMER_EN

        return if (bn) {
            """
============================================================
$title
============================================================
[$disclaimer]

তারিখ: $date

১. প্রথম পক্ষ (দাতা / বিক্রেতা / মালিক):
   নাম: ${doc.firstPartyName.ifBlank { "......................." }}
   পিতা/স্বামী: ${doc.firstPartyFather.ifBlank { "......................." }}
   এনআইডি (NID): ${doc.firstPartyNid.ifBlank { "......................." }}
   ঠিকানা: ${doc.firstPartyAddress.ifBlank { "......................." }}

২. দ্বিতীয় পক্ষ (গ্রহীতা / ক্রেতা / অংশীদার):
   নাম: ${doc.secondPartyName.ifBlank { "......................." }}
   পিতা/স্বামী: ${doc.secondPartyFather.ifBlank { "......................." }}
   এনআইডি (NID): ${doc.secondPartyNid.ifBlank { "......................." }}
   ঠিকানা: ${doc.secondPartyAddress.ifBlank { "......................." }}

৩. জমির তফসিল বিবরণী:
   জেলা: ${doc.district.ifBlank { "..........." }} | উপজেলা/থানা: ${doc.upazila.ifBlank { "..........." }}
   মৌজা: ${doc.mouza.ifBlank { "..........." }} | জে.এল. নং: ${doc.jlNumber.ifBlank { "..........." }}
   জরিপের ধরন: ${doc.khatianType} | খতিয়ান নং: ${doc.khatianNumber.ifBlank { "..........." }}
   দাগ নং: ${doc.dagNumber.ifBlank { "..........." }} | জমির শ্রেণি: ${doc.landClass}
   জমির পরিমাণ: ${doc.areaDecimal.ifBlank { "..........." }} শতাংশ
   পণ / মূল্য: ${doc.transactionValueBdt.ifBlank { "প্রযোজ্য নয় / বর্ণিত" }} টাকা

৪. চৌহদ্দি (চারিদিকের সীমানা):
   উত্তরে: ${doc.boundaryNorth.ifBlank { "......................." }}
   দক্ষিণে: ${doc.boundarySouth.ifBlank { "......................." }}
   পূর্বে: ${doc.boundaryEast.ifBlank { "......................." }}
   পশ্চিমে: ${doc.boundaryWest.ifBlank { "......................." }}

৫. শর্তাবলি ও বক্তব্য:
${doc.termsAndConditions.ifBlank { defaultTermsForTemplate(doc.templateType, true) }}

${if (doc.customNotes.isNotBlank()) "অতিরিক্ত নোট:\n${doc.customNotes}\n" else ""}
৬. সাক্ষীগণের স্বাক্ষর:
   ১ম সাক্ষী: ${doc.witness1.ifBlank { "_____________________" }}
   ২য় সাক্ষী: ${doc.witness2.ifBlank { "_____________________" }}

৭. স্বাক্ষর ও টিপসই:
   প্রথম পক্ষের স্বাক্ষর ও টিপসই: ____________________
   দ্বিতীয় পক্ষের স্বাক্ষর ও টিপসই: ____________________

প্রস্তুতকারক / সার্ভেয়ার সহায়তা: ${profile.name} (${profile.phone})
[$disclaimer]
            """.trimIndent()
        } else {
            """
============================================================
$title
============================================================
[$disclaimer]

Date: $date

1. First Party (Transferor / Executant):
   Name: ${doc.firstPartyName.ifBlank { "......................." }}
   Father/Spouse: ${doc.firstPartyFather.ifBlank { "......................." }}
   NID No: ${doc.firstPartyNid.ifBlank { "......................." }}
   Address: ${doc.firstPartyAddress.ifBlank { "......................." }}

2. Second Party (Transferee / Recipient):
   Name: ${doc.secondPartyName.ifBlank { "......................." }}
   Father/Spouse: ${doc.secondPartyFather.ifBlank { "......................." }}
   NID No: ${doc.secondPartyNid.ifBlank { "......................." }}
   Address: ${doc.secondPartyAddress.ifBlank { "......................." }}

3. Property Schedule:
   District: ${doc.district.ifBlank { "..........." }} | Upazila: ${doc.upazila.ifBlank { "..........." }}
   Mouza: ${doc.mouza.ifBlank { "..........." }} | JL No: ${doc.jlNumber.ifBlank { "..........." }}
   Record Type: ${doc.khatianType} | Khatian No: ${doc.khatianNumber.ifBlank { "..........." }}
   Dag No: ${doc.dagNumber.ifBlank { "..........." }} | Class: ${doc.landClass}
   Area: ${doc.areaDecimal.ifBlank { "..........." }} Decimal
   Consideration Value: ${doc.transactionValueBdt.ifBlank { "N/A" }} BDT

4. Four-Side Boundaries (Chouhoddi):
   North: ${doc.boundaryNorth.ifBlank { "......................." }}
   South: ${doc.boundarySouth.ifBlank { "......................." }}
   East:  ${doc.boundaryEast.ifBlank { "......................." }}
   West:  ${doc.boundaryWest.ifBlank { "......................." }}

5. Terms & Covenants:
${doc.termsAndConditions.ifBlank { defaultTermsForTemplate(doc.templateType, false) }}

6. Witnesses:
   Witness 1: ${doc.witness1.ifBlank { "_____________________" }}
   Witness 2: ${doc.witness2.ifBlank { "_____________________" }}

7. Signatures & Thumbprints:
   First Party Signature / LTI:  ____________________
   Second Party Signature / LTI: ____________________

Prepared via Digital Survey Workstation • ${profile.name} (${profile.phone})
[$disclaimer]
            """.trimIndent()
        }
    }

    fun defaultTermsForTemplate(type: DeedTemplateType, bn: Boolean): String {
        return when (type) {
            DeedTemplateType.SAFKABALA_SALE -> if (bn) {
                "প্রথম পক্ষ অদ্য নিম্ন তফসিল বর্ণিত ভূমি ন্যায্য মূল্যে দ্বিতীয় পক্ষের নিকট নিষ্কণ্টকভাবে বিক্রয় ও দখল হস্তান্তর করিলেন।"
            } else {
                "The First Party hereby conveys absolute title, ownership, and peaceful possession of the scheduled land to the Second Party for full consideration."
            }
            DeedTemplateType.BONTANNAMA_PARTITION -> if (bn) {
                "সহ-শরিকগণ আপস সম্মতিতে নির্ধারিত হিস্যা ও চৌহদ্দি অনুযায়ী নিম্ন বর্ণিত ভূমি পৃথকভাবে বণ্টন ও দখল গ্রহণ করিলেন।"
            } else {
                "Co-sharers mutually partition and take exclusive possession of their respective demarcated portions as specified in the schedule."
            }
            DeedTemplateType.HEBA_GIFT -> if (bn) {
                "প্রথম পক্ষ স্বেচ্ছায়, সুস্থ মস্তিষ্কে ও স্নেহের বশবর্তী হইয়া কোনো বিনিময় ছাড়াই দ্বিতীয় পক্ষকে নিম্ন বর্ণিত ভূমি দান/হেবা করিলেন।"
            } else {
                "Out of natural love and affection without monetary consideration, the Donor voluntarily gifts and delivers possession of the scheduled property."
            }
            DeedTemplateType.BAYNA_AGREEMENT -> if (bn) {
                "নির্ধারিত সময়ের মধ্যে অবশিষ্ট মূল্য পরিশোধ সাপেক্ষে সাফ-কবলা দলিল রেজিস্ট্রি করিয়া দিবার শর্তে এই বায়না চুক্তি সম্পাদিত হইল।"
            } else {
                "Subject to payment of the balance consideration within the stipulated period, the Vendor agrees to execute a registered Sale Deed."
            }
            else -> if (bn) {
                "উভয় পক্ষ সজ্ঞানে ও সাক্ষীগণের উপস্থিতিতে নিম্নবর্ণিত শর্তসমূহ মানিয়া চলিতে অঙ্গীকারবদ্ধ হইলেন।"
            } else {
                "Both parties agree in sound mind and in presence of witnesses to abide by the covenants stated herein."
            }
        }
    }

    /** Builds genuine OpenXML .docx binary archive in pure Kotlin */
    fun buildDocxBytes(title: String, plainBody: String): ByteArray {
        fun escXml(s: String): String = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val paragraphsXml = plainBody.lines().joinToString("\n") { line ->
            val bold = line.startsWith("===") || line.startsWith("১.") || line.startsWith("২.") ||
                line.startsWith("৩.") || line.startsWith("৪.") || line.startsWith("৫.") ||
                line.startsWith("৬.") || line.startsWith("৭.") || line.startsWith("1.") ||
                line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.")
            val rPr = if (bold) "<w:rPr><w:b/><w:sz w:val=\"24\"/></w:rPr>" else "<w:rPr><w:sz w:val=\"22\"/></w:rPr>"
            "<w:p><w:r>$rPr<w:t xml:space=\"preserve\">${escXml(line)}</w:t></w:r></w:p>"
        }

        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:rPr><w:b/><w:sz w:val="30"/></w:rPr><w:t>${escXml(title)}</w:t></w:r></w:p>
                $paragraphsXml
                <w:sectPr><w:pgSz w:w="11906" w:h="16838"/></w:sectPr>
              </w:body>
            </w:document>
        """.trimIndent()

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypes.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(rels.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    fun saveBinaryDocument(
        context: Context?,
        fileNameWithExt: String,
        mimeType: String,
        bytes: ByteArray,
        desktopFallbackDir: File? = null
    ): ExportedFileResult {
        if (context == null) {
            val dir = desktopFallbackDir ?: File(System.getProperty("java.io.tmpdir"), "BhumiSurveyDocs")
            dir.mkdirs()
            val f = File(dir, fileNameWithExt)
            FileOutputStream(f).use { it.write(bytes) }
            return ExportedFileResult(Uri.fromFile(f), f.absolutePath)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameWithExt)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/BhumiSurvey")
            }
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            if (uri != null) {
                val stream = context.contentResolver.openOutputStream(uri)
                if (stream != null) {
                    stream.use { it.write(bytes) }
                    return ExportedFileResult(uri, "Documents/BhumiSurvey/$fileNameWithExt")
                }
            }
        }
        val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir, "BhumiSurvey")
        fallbackDir.mkdirs()
        val f = File(fallbackDir, fileNameWithExt)
        FileOutputStream(f).use { it.write(bytes) }
        return ExportedFileResult(Uri.fromFile(f), f.absolutePath)
    }

    fun renderDeedDraftBitmap(doc: DeedDraftDocument, profile: SurveyorProfile = SurveyorProfile()): Bitmap {
        val text = buildDeedText(doc, profile)
        val lines = text.lines()
        val w = 1654
        val lineH = 34
        val h = maxOf(2339, 260 + lines.size * lineH)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val border = Paint().apply {
            color = Color.parseColor("#0F6B54")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(36f, 36f, w - 36f, h - 36f, border)

        val noticePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B71C1C")
            textSize = 24f
            isFakeBoldText = true
        }
        canvas.drawText(
            if (doc.languageBn) LEGAL_DRAFT_DISCLAIMER_BN else LEGAL_DRAFT_DISCLAIMER_EN,
            64f,
            84f,
            noticePaint
        )

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
        }
        var y = 138f
        lines.forEach { ln ->
            bodyPaint.isFakeBoldText = ln.startsWith("===") || ln.matches(Regex("^[১২৩৪৫৬৭1-7]\\.\\s.*"))
            canvas.drawText(ln, 64f, y, bodyPaint)
            y += lineH
        }
        return bmp
    }

    /** Renders A4 / A3 Survey Report with North Arrow, Scale, Coordinates, QR Verification Block, and Signatures */
    fun renderSurveyReportBitmap(
        config: SurveyReportConfig,
        profile: SurveyorProfile = SurveyorProfile()
    ): Bitmap {
        val isA3 = config.pageSize.equals("A3", ignoreCase = true)
        val isLand = config.orientation.equals("Landscape", ignoreCase = true)
        val shortSide = if (isA3) 1754 else 1240
        val longSide = if (isA3) 2480 else 1754
        val w = if (isLand) longSide else shortSide
        val h = if (isLand) shortSide else longSide

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#0F6B54")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRect(28f, 28f, w - 28f, h - 28f, borderPaint)

        // Header banner
        val headerBg = Paint().apply { color = Color.parseColor("#0F6B54") }
        canvas.drawRect(28f, 28f, w - 28f, 152f, headerBg)

        val headerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            isFakeBoldText = true
        }
        val headerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8F3DC")
            textSize = 22f
        }
        canvas.drawText(config.title.ifBlank { config.reportType.titleBn }, 56f, 84f, headerTitle)
        canvas.drawText(
            "${config.reportType.titleBn} • ${config.pageSize} ${config.orientation} • স্কেল: ${config.scaleLabel}",
            56f,
            124f,
            headerSub
        )

        // North arrow on top right
        if (config.includeNorthArrow) {
            val nx = w - 110f
            val ny = 92f
            val nPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 4f
                textSize = 22f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val arrowPath = Path().apply {
                moveTo(nx, ny - 36f)
                lineTo(nx - 14f, ny + 12f)
                lineTo(nx, ny + 2f)
                lineTo(nx + 14f, ny + 12f)
                close()
            }
            canvas.drawPath(arrowPath, nPaint)
            canvas.drawText("N", nx, ny + 34f, nPaint)
        }

        var curY = 204f
        val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            textSize = 26f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
        }
        val rowBg = Paint().apply { color = Color.parseColor("#F1F8F5") }

        fun drawKeyValueBox(heading: String, rows: List<Pair<String, String>>) {
            canvas.drawText(heading, 56f, curY, sectionTitle)
            curY += 16f
            rows.forEachIndexed { idx, (k, v) ->
                if (idx % 2 == 0) {
                    canvas.drawRect(56f, curY, w - 56f, curY + 40f, rowBg)
                }
                canvas.drawText(k, 72f, curY + 28f, bodyPaint)
                canvas.drawText(v, (w * 0.42f), curY + 28f, bodyPaint)
                curY += 42f
            }
            curY += 22f
        }

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(config.createdAtMillis))
        drawKeyValueBox(
            "১. মৌজা ও জরিপ পরিচয়",
            listOf(
                "মালিক / প্রজেক্ট:" to config.ownerName.ifBlank { "অনির্ধারিত" },
                "জেলা / উপজেলা:" to "${config.district.ifBlank { "-" }} / ${config.upazila.ifBlank { "-" }}",
                "মৌজা / জে.এল.:" to "${config.mouza.ifBlank { "-" }} (JL: ${config.jlNumber.ifBlank { "-" }})",
                "খতিয়ান ও দাগ:" to config.khatianDagSummary.ifBlank { "-" },
                "তারিখ ও স্কেল:" to "$dateStr • ${config.scaleLabel}"
            )
        )

        drawKeyValueBox(
            "২. পরিমাপ ও ক্ষেত্রফলের সারসংক্ষেপ",
            listOf(
                "জমির পরিমাণ:" to config.areaSummaryText.ifBlank { "-" },
                "বাহুগুলোর দৈর্ঘ্য:" to config.sideLengthsText.ifBlank { "-" },
                "স্থানাঙ্ক (Coordinates):" to config.coordinatesText.ifBlank { "স্থানীয় জরিপ" }
            )
        )

        if (config.remarks.isNotBlank()) {
            canvas.drawText("৩. সার্ভেয়ার মন্তব্য ও নোট", 56f, curY, sectionTitle)
            curY += 34f
            config.remarks.lines().take(8).forEach { ln ->
                canvas.drawText(ln, 64f, curY, bodyPaint)
                curY += 32f
            }
            curY += 18f
        }

        // Deterministic verification QR matrix block
        if (config.includeQrCode) {
            val qrSize = 144
            val qrLeft = w - qrSize - 64f
            val qrTop = (h - 270f).coerceAtLeast(curY + 12f)
            val hash = sha256Hex("${config.id}|${config.mouza}|${config.areaSummaryText}")
            val cell = qrSize / 12f
            val qrPaint = Paint().apply { color = Color.BLACK }
            for (r in 0 until 12) {
                for (c in 0 until 12) {
                    val isFinder = (r < 3 && c < 3) || (r < 3 && c >= 9) || (r >= 9 && c < 3)
                    val bitIdx = (r * 12 + c) % hash.length
                    val dark = isFinder || (hash[bitIdx].code % 2 == 0)
                    if (dark) {
                        canvas.drawRect(
                            qrLeft + c * cell,
                            qrTop + r * cell,
                            qrLeft + (c + 1) * cell,
                            qrTop + (r + 1) * cell,
                            qrPaint
                        )
                    }
                }
            }
            val qrCap = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 16f }
            canvas.drawText("ID: ${hash.take(10).uppercase()}", qrLeft, qrTop + qrSize + 20f, qrCap)
        }

        if (config.includeSignatureBlock) {
            val sigY = h - 120f
            val lineP = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
            canvas.drawLine(64f, sigY, 380f, sigY, lineP)
            canvas.drawText("সার্ভেয়ার: ${profile.name} (${profile.phone})", 64f, sigY + 28f, bodyPaint)

            canvas.drawLine(460f, sigY, 760f, sigY, lineP)
            canvas.drawText("মালিক / পক্ষের স্বাক্ষর", 460f, sigY + 28f, bodyPaint)
        }

        val footP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; textSize = 18f }
        canvas.drawText(
            "অস্বীকৃতি: এটি ব্যক্তিগত ফিল্ড/ডিজিটাল সার্ভে প্রতিবেদন; সরকারি সার্টিফায়েড পর্চা বা নকশা হিসেবে প্রযোজ্য নয়। | Page 1 of 1",
            56f,
            h - 46f,
            footP
        )
        return bmp
    }

    // -------------------- Offline Bangla & English OCR Field Extractor --------------------

    fun bengaliDigitsToEnglish(input: String): String {
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val out = StringBuilder(input.length)
        for (ch in input) {
            val idx = bn.indexOf(ch)
            out.append(if (idx >= 0) ('0' + idx) else ch)
        }
        return out.toString()
    }

    fun extractKhatianFieldsFromText(rawText: String): List<OcrExtractedField> {
        val normalized = bengaliDigitsToEnglish(rawText)
        val out = mutableListOf<OcrExtractedField>()

        fun extract(regex: Regex, key: String, label: String, conf: Float) {
            val m = regex.find(normalized)
            val value = m?.groupValues?.getOrNull(1)?.trim().orEmpty()
            if (value.isNotBlank()) {
                out.add(OcrExtractedField(key, label, value, conf))
            }
        }

        extract(Regex("""(?:খতিয়ান|Khatian)\s*(?:নং|No\.?|নাম্বার)?\s*[:\-]?\s*([0-9০-৯/, -]+)""", RegexOption.IGNORE_CASE), "khatian", "খতিয়ান নং", 0.91f)
        extract(Regex("""(?:দাগ|Dag)\s*(?:নং|No\.?|নাম্বার)?\s*[:\-]?\s*([0-9০-৯/, -]+)""", RegexOption.IGNORE_CASE), "dag", "দাগ নং", 0.89f)
        extract(Regex("""(?:মৌজা|Mouza)\s*[:\-]?\s*([^\n,;|]+)""", RegexOption.IGNORE_CASE), "mouza", "মৌজা", 0.86f)
        extract(Regex("""(?:জে\.?এল\.?|JL)\s*(?:নং|No\.?)?\s*[:\-]?\s*([0-9০-৯]+)""", RegexOption.IGNORE_CASE), "jl", "জে.এল. নং", 0.88f)
        extract(Regex("""(?:মালিক|দখলদার|Owner|প্রজা)\s*(?:নাম|Name)?\s*[:\-]?\s*([^\n;|]+)""", RegexOption.IGNORE_CASE), "owner", "মালিকের নাম", 0.82f)
        extract(Regex("""(?:জমি|পরিমাণ|Area|শতাংশ|একর)\s*[:\-]?\s*([0-9]+(?:\.[0-9]+)?\s*(?:শতাংশ|একর|decimal|acre|শতক)?)""", RegexOption.IGNORE_CASE), "area", "জমির পরিমাণ", 0.84f)

        return out
    }

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // -------------------- AES-256-GCM Encrypted Vault Backup --------------------

    fun encryptVaultJson(plainJson: String, passphrase: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = deriveAesKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        return "BHUMI_VAULT_V1|" +
            Base64.encodeToString(salt, Base64.NO_WRAP) + "|" +
            Base64.encodeToString(iv, Base64.NO_WRAP) + "|" +
            Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
    }

    fun decryptVaultJson(encryptedPayload: String, passphrase: String): String? {
        return runCatching {
            val parts = encryptedPayload.trim().split("|")
            if (parts.size != 4 || parts[0] != "BHUMI_VAULT_V1") return null
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val iv = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[3], Base64.NO_WRAP)
            val key = deriveAesKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 24000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
