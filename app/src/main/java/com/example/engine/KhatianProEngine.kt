package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.example.model.CustomKhatianDocument
import com.example.model.DagIndexCrossRefItem
import com.example.model.EncroachmentOverlapReport
import com.example.model.KhatianOneClickAnalysis
import com.example.model.KhatianOneClickOwnerResult
import com.example.model.PolygonVertex
import com.example.model.RegionalUnitProfile
import com.example.model.ThreeWayAreaCompareResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object KhatianProEngine {

    /**
     * M08: One-click complete owner share & 16-ana validation for any CustomKhatianDocument.
     */
    fun analyzeKhatianOneClick(
        doc: CustomKhatianDocument,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): KhatianOneClickAnalysis {
        val totalDec = doc.dags.sumOf { it.thisKhatianDecimal.coerceAtLeast(0.0) }
        val sqFtPerDec = LandCalculators.SQ_FT_PER_DECIMAL
        val kathaDiv = unitProfile.kathaSqFt.coerceAtLeast(100.0)
        val bighaDiv = unitProfile.bighaSqFt.coerceAtLeast(100.0)

        val ownerResults = doc.owners.map { owner ->
            val ratio = owner.shareRatio.coerceAtLeast(0.0)
            val dec = totalDec * ratio
            val sq = dec * sqFtPerDec
            KhatianOneClickOwnerResult(
                name = owner.name.ifBlank { "মালিক" },
                shareText = owner.shareText.ifBlank { String.format(Locale.US, "%.4f", ratio) },
                ratio = ratio,
                decimal = dec,
                katha = sq / kathaDiv,
                bigha = sq / bighaDiv,
                sqFt = sq
            )
        }
        val totalRatio = doc.owners.sumOf { it.shareRatio.coerceAtLeast(0.0) }
        val diff = totalRatio - 1.0
        val exact = doc.owners.isNotEmpty() && abs(diff) <= 0.002
        val note = when {
            doc.owners.isEmpty() -> "খতিয়ানে এখনো কোনো মালিক যোগ করা হয়নি।"
            exact -> "✓ সকল মালিকের অংশের যোগফল ১৬ আনা (১.০০০) নির্ভুলভাবে মিলেছে।"
            diff < 0.0 -> String.format(
                Locale.US,
                "⚠ মালিকদের মোট অংশ %.4f (১৬ আনা হতে %.4f কম বা %.2f আনা ঘাটতি)।",
                totalRatio, -diff, -diff * 16.0
            )
            else -> String.format(
                Locale.US,
                "⚠ সতর্কতা: মালিকদের মোট অংশ %.4f (১৬ আনার চেয়ে %.4f বেশি বা %.2f আনা অতিরিক্ত)!",
                totalRatio, diff, diff * 16.0
            )
        }
        return KhatianOneClickAnalysis(
            khatianNumber = doc.khatianNumber.ifBlank { "-" },
            totalDagDecimal = totalDec,
            owners = ownerResults,
            totalOwnerRatio = totalRatio,
            isSixteenAnaExact = exact,
            ratioDiscrepancy = diff,
            discrepancyNote = note
        )
    }

    /**
     * M08: Dag Index (Dag <-> Khatian <-> Owner cross-reference and search).
     */
    fun buildDagCrossReferenceIndex(
        khatians: List<CustomKhatianDocument>,
        query: String = ""
    ): List<DagIndexCrossRefItem> {
        val q = query.trim().lowercase()
        val all = khatians.flatMap { kh ->
            val ownersSummary = if (kh.owners.isEmpty()) "মালিক অলিখিত" else kh.owners.joinToString(", ") { it.name }
            kh.dags.map { dag ->
                DagIndexCrossRefItem(
                    dagNumber = dag.dagNumber,
                    khatianId = kh.id,
                    khatianType = kh.khatianType,
                    khatianNumber = kh.khatianNumber.ifBlank { "-" },
                    mouzaName = kh.mouzaName.ifBlank { "-" },
                    ownersSummary = ownersSummary,
                    landClass = dag.landClass,
                    totalDagDecimal = dag.totalDecimal,
                    thisKhatianDecimal = dag.thisKhatianDecimal
                )
            }
        }
        if (q.isBlank()) return all
        return all.filter { item ->
            item.dagNumber.lowercase().contains(q) ||
                item.khatianNumber.lowercase().contains(q) ||
                item.mouzaName.lowercase().contains(q) ||
                item.ownersSummary.lowercase().contains(q)
        }
    }

    /**
     * M08: Map vs Khatian vs Field 3-Way Comparison with customizable tolerance %.
     */
    fun compareThreeWayAreas(
        mapDecimal: Double,
        khatianDecimal: Double,
        fieldDecimal: Double,
        tolerancePct: Double = 2.0
    ): ThreeWayAreaCompareResult {
        val m = mapDecimal.coerceAtLeast(0.0)
        val k = khatianDecimal.coerceAtLeast(0.0)
        val f = fieldDecimal.coerceAtLeast(0.0)

        fun pctDiff(diff: Double, base: Double): Double =
            if (base > 1e-6) (abs(diff) / base) * 100.0 else 0.0

        val mkDiff = m - k
        val mkPct = pctDiff(mkDiff, k)
        val fkDiff = f - k
        val fkPct = pctDiff(fkDiff, k)
        val mfDiff = m - f
        val mfPct = pctDiff(mfDiff, m.coerceAtLeast(f))

        val maxVal = max(m, max(k, f))
        val minVal = min(m, min(k, f))
        val maxSpreadDec = maxVal - minVal
        val maxSpreadPct = max(mkPct, max(fkPct, mfPct))
        val tol = tolerancePct.coerceIn(0.1, 25.0)
        val within = maxSpreadPct <= tol
        val verdict = if (within) {
            String.format(Locale.US, "✓ নকশা, খতিয়ান ও মাঠ ±%.1f%% সহনশীল সীমার ভেতর সামঞ্জস্যপূর্ণ (সর্বোচ্চ পার্থক্য %.2f%%)।", tol, maxSpreadPct)
        } else {
            String.format(Locale.US, "⚠ সতর্কতা: সর্বোচ্চ পার্থক্য %.2f%% যা সহনশীল সীমা (%.1f%%) অতিক্রম করেছে — সরেজমিন দখল/নকশা স্কেল যাচাই করুন।", maxSpreadPct, tol)
        }
        return ThreeWayAreaCompareResult(
            mapDecimal = m,
            khatianDecimal = k,
            fieldDecimal = f,
            mapVsKhatianDiffDec = mkDiff,
            mapVsKhatianDiffPct = mkPct,
            fieldVsKhatianDiffDec = fkDiff,
            fieldVsKhatianDiffPct = fkPct,
            mapVsFieldDiffDec = mfDiff,
            mapVsFieldDiffPct = mfPct,
            maxSpreadDec = maxSpreadDec,
            maxSpreadPct = maxSpreadPct,
            tolerancePct = tol,
            withinTolerance = within,
            verdictBn = verdict
        )
    }

    /**
     * M08: Exact Analytical CS/SA vs RS/BS Dag Overlap & Encroachment Detection.
     */
    fun analyzeDagOverlapAndEncroachment(
        formerLabel: String,
        currentLabel: String,
        formerPolygon: List<PolygonVertex>,
        currentPolygon: List<PolygonVertex>
    ): EncroachmentOverlapReport {
        val fArea = PolygonDividerEngine.polygonAreaSqFt(formerPolygon)
        val cArea = PolygonDividerEngine.polygonAreaSqFt(currentPolygon)
        val overlap = LandDivisionProEngine.exactIntersectionAreaSqFt(formerPolygon, currentPolygon)
        val outsideFormerIntoCurrent = (cArea - overlap).coerceAtLeast(0.0)
        val shortfallOfFormer = (fArea - overlap).coerceAtLeast(0.0)
        val ratioPct = if (cArea > 1e-6) (overlap / cArea) * 100.0 else 0.0
        val note = String.format(
            Locale.US,
            "%s (%.2f শতক) বনাম %s (%.2f শতক): ওভারল্যাপ %.2f শতক (%.1f%%) | বাড়তি/দখল %.2f শতক | ঘাটতি %.2f শতক",
            formerLabel, fArea / 435.6,
            currentLabel, cArea / 435.6,
            overlap / 435.6, ratioPct,
            outsideFormerIntoCurrent / 435.6,
            shortfallOfFormer / 435.6
        )
        return EncroachmentOverlapReport(
            formerDagLabel = formerLabel,
            currentDagLabel = currentLabel,
            formerAreaSqFt = fArea,
            currentAreaSqFt = cArea,
            overlapAreaSqFt = overlap,
            encroachmentIntoCurrentSqFt = outsideFormerIntoCurrent,
            shortfallGapSqFt = shortfallOfFormer,
            overlapRatioPct = ratioPct,
            formerPolygon = formerPolygon,
            currentPolygon = currentPolygon,
            summaryNoteBn = note
        )
    }

    /**
     * M08: Digital Demarcation & Encroachment Overlay Report Canvas Bitmap (for PNG/PDF Export).
     */
    fun renderDemarcationCompareBitmap(
        report: EncroachmentOverlapReport,
        threeWay: ThreeWayAreaCompareResult? = null
    ): Bitmap {
        val w = 1650
        val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        com.example.ui.components.VintagePaperRenderer.drawAgedStampPaperBackground(
            canvas = canvas,
            w = w,
            h = h,
            headerLabelBn = "ডিজিটাল ডিমার্কেশন ও দাগ ম্যাচিং প্রতিবেদন",
            stampValueLabel = "${report.formerDagLabel} ↔ ${report.currentDagLabel}",
            serialTag = "DEMARCATION-M08",
            showStampCrest = true
        )

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#174E3D")
            textSize = 30f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B1C10")
            textSize = 22f
        }
        canvas.drawText(report.summaryNoteBn, 65f, 225f, titlePaint)

        val allPts = report.formerPolygon + report.currentPolygon
        if (allPts.isNotEmpty()) {
            val minX = allPts.minOf { it.x }; val maxX = allPts.maxOf { it.x }
            val minY = allPts.minOf { it.y }; val maxY = allPts.maxOf { it.y }
            val spanX = (maxX - minX).coerceAtLeast(1.0)
            val spanY = (maxY - minY).coerceAtLeast(1.0)
            val left = 90f; val top = 265f; val boxW = w - 180f; val boxH = 660f
            val sc = min(boxW / spanX, boxH / spanY)

            fun toPt(v: PolygonVertex) =
                (left + (v.x - minX) * sc).toFloat() to (top + (maxY - v.y) * sc).toFloat()

            // Former dashed blue/brown outline
            if (report.formerPolygon.size >= 3) {
                val p1 = Path()
                report.formerPolygon.forEachIndexed { idx, v ->
                    val (sx, sy) = toPt(v)
                    if (idx == 0) p1.moveTo(sx, sy) else p1.lineTo(sx, sy)
                }
                p1.close()
                canvas.drawPath(p1, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(42, 26, 115, 232); style = Paint.Style.FILL })
                canvas.drawPath(p1, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1A73E8")
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
                })
            }

            // Current solid green outline + side lengths
            if (report.currentPolygon.size >= 3) {
                val p2 = Path()
                report.currentPolygon.forEachIndexed { idx, v ->
                    val (sx, sy) = toPt(v)
                    if (idx == 0) p2.moveTo(sx, sy) else p2.lineTo(sx, sy)
                }
                p2.close()
                canvas.drawPath(p2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(54, 15, 107, 84); style = Paint.Style.FILL })
                canvas.drawPath(p2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F6B54")
                    style = Paint.Style.STROKE
                    strokeWidth = 4.5f
                })
                for (i in report.currentPolygon.indices) {
                    val a = report.currentPolygon[i]
                    val b = report.currentPolygon[(i + 1) % report.currentPolygon.size]
                    val (ax, ay) = toPt(a); val (bx, by) = toPt(b)
                    val lenFt = kotlin.math.hypot(b.x - a.x, b.y - a.y)
                    canvas.drawText(
                        LandCalculators.formatFeetInches(lenFt),
                        (ax + bx) * 0.5f,
                        (ay + by) * 0.5f - 8f,
                        bodyPaint
                    )
                }
            }
        }

        var ty = 975f
        canvas.drawText(
            String.format(
                Locale.US,
                "সাবেক (%s): %.3f শতক  |  হাল (%s): %.3f শতক  |  মিলিত ওভারল্যাপ: %.3f শতক (%.1f%%)",
                report.formerDagLabel, report.formerAreaSqFt / 435.6,
                report.currentDagLabel, report.currentAreaSqFt / 435.6,
                report.overlapAreaSqFt / 435.6, report.overlapRatioPct
            ),
            65f, ty, bodyPaint
        )
        ty += 38f
        canvas.drawText(
            String.format(
                Locale.US,
                "দখল/বহির্ভূত অংশ: %.3f শতক (%.0f ব.ফু.)  |  ঘাটতি/ফাঁক: %.3f শতক (%.0f ব.ফু.)",
                report.encroachmentIntoCurrentSqFt / 435.6, report.encroachmentIntoCurrentSqFt,
                report.shortfallGapSqFt / 435.6, report.shortfallGapSqFt
            ),
            65f, ty, bodyPaint
        )
        if (threeWay != null) {
            ty += 42f
            canvas.drawText(threeWay.verdictBn, 65f, ty, titlePaint)
        }
        canvas.drawText(
            "ব্যক্তিগত ব্যবহারের কপি — সরকারি রেজিস্টার/রেকর্ড নয়; নিবন্ধিত দলিল/সরকারি পর্চাই আইনত গ্রাহ্য",
            65f,
            h - 54f,
            Paint(bodyPaint).apply { color = Color.parseColor("#8C1D18"); textSize = 19f }
        )
        return bmp
    }
}
