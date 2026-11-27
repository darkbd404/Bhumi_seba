package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import com.example.model.CadastralSurveyStage
import com.example.model.ControlPointPair
import com.example.model.GeoWaypoint
import com.example.model.PantographOverlapRow
import com.example.model.PantographReportSummary
import com.example.model.TransformModelType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

data class ScalePantographResult(
    val sourceRatioDenom: Double,
    val targetRatioDenom: Double,
    val magnificationFactor: Double,
    val percentChange: Double,
    val formulaSummaryBn: String
)

data class OutlierSuggestion(
    val pointId: String,
    val label: String,
    val residualPx: Double,
    val rmseBefore: Double,
    val rmseIfRemoved: Double
)

data class DagPolygonInput(
    val dagNumber: String,
    val vertices: List<Pair<Double, Double>>
)

data class SolvedPantographModel(
    val summary: PantographReportSummary,
    val forwardTransform: (Double, Double) -> Pair<Double, Double>, // (overlayX, overlayY) -> (baseX, baseY)
    val outlierSuggestions: List<OutlierSuggestion>
)

object PantographEngine {

    /**
     * 2.1 Scale Pantograph: 1:X -> 1:Y enlargement or reduction math.
     */
    fun computeScalePantograph(sourceDenom: Double, targetDenom: Double): ScalePantographResult {
        val src = sourceDenom.coerceAtLeast(1.0)
        val tgt = targetDenom.coerceAtLeast(1.0)
        // Larger ratio denominator means smaller map scale, so enlarging from 1:3960 to 1:1980 gives factor = 2.0x
        val factor = src / tgt
        val pct = (factor - 1.0) * 100.0
        val actionBn = when {
            factor > 1.001 -> "বড় করা (Enlarge)"
            factor < 0.999 -> "ছোট করা (Reduce)"
            else -> "সমান স্কেল"
        }
        val formula = String.format(
            Locale.US,
            "সূত্র: স্কেল অনুপাত = (১/%.0f) ÷ (১/%.0f) = %.0f ÷ %.0f = %.4fx (%s, %+.1f%%)",
            tgt, src, src, tgt, factor, actionBn, pct
        )
        return ScalePantographResult(
            sourceRatioDenom = src,
            targetRatioDenom = tgt,
            magnificationFactor = factor,
            percentChange = pct,
            formulaSummaryBn = formula
        )
    }

    /**
     * Solves full 2D Similarity, Affine, Projective Homography (4+ pts 8x8 DLT),
     * or True Thin-Plate Spline (TPS RBF: U(r) = r^2 * ln(r^2)).
     * Maps Overlay(x, y) -> Base(x, y) and computes outlier leave-one-out RMSE drops.
     */
    fun solveTransformModel(
        baseStage: CadastralSurveyStage,
        overlayStage: CadastralSurveyStage,
        transformType: TransformModelType,
        pairs: List<ControlPointPair>,
        dagMatches: List<String> = emptyList(),
        computeOutliers: Boolean = true
    ): SolvedPantographModel {
        if (pairs.size < 2) {
            val emptySummary = PantographReportSummary(
                baseStage = baseStage,
                overlayStage = overlayStage,
                transformType = transformType,
                controlPoints = pairs,
                dagMatches = dagMatches
            )
            return SolvedPantographModel(emptySummary, { x, y -> x to y }, emptyList())
        }

        // Base affine fit (for scale/rotation summary and initial fallback)
        val affineReport = GisCoordinateEngine.computePantographTransform(
            baseStage = baseStage,
            overlayStage = overlayStage,
            transformType = if (transformType == TransformModelType.SIMILARITY) TransformModelType.SIMILARITY else TransformModelType.AFFINE,
            pairs = pairs,
            dagMatches = dagMatches
        )

        val transformFn: (Double, Double) -> Pair<Double, Double> = when {
            transformType == TransformModelType.PROJECTIVE && pairs.size >= 4 -> {
                solveHomography(pairs) ?: buildAffineFunction(pairs)
            }
            transformType == TransformModelType.TPS && pairs.size >= 4 -> {
                solveThinPlateSpline(pairs) ?: buildAffineFunction(pairs)
            }
            transformType == TransformModelType.SIMILARITY || pairs.size < 3 -> {
                buildSimilarityFunction(pairs)
            }
            else -> buildAffineFunction(pairs)
        }

        val updatedPairs = pairs.mapIndexed { idx, p ->
            val err = if (transformType == TransformModelType.TPS && pairs.size >= 5) {
                // TPS fits training points near-exactly; measure true leave-one-out cross-validation residual!
                val subset = pairs.filterIndexed { i, _ -> i != idx }
                val looFn = solveThinPlateSpline(subset) ?: buildAffineFunction(subset)
                val (px, py) = looFn(p.overlayX, p.overlayY)
                hypot(px - p.baseX, py - p.baseY)
            } else {
                val (px, py) = transformFn(p.overlayX, p.overlayY)
                hypot(px - p.baseX, py - p.baseY)
            }
            p.copy(residualErrorPx = err)
        }

        val rmse = sqrt(updatedPairs.map { it.residualErrorPx * it.residualErrorPx }.average())
        val maxErr = updatedPairs.maxOfOrNull { it.residualErrorPx } ?: 0.0

        val outliers = if (computeOutliers && pairs.size >= 4) {
            val median = updatedPairs.map { it.residualErrorPx }.sorted()[updatedPairs.size / 2]
            updatedPairs.mapIndexedNotNull { idx, cp ->
                if (cp.residualErrorPx > maxOf(median * 1.8, 2.5)) {
                    val remaining = pairs.filterIndexed { i, _ -> i != idx }
                    val sub = solveTransformModel(
                        baseStage = baseStage,
                        overlayStage = overlayStage,
                        transformType = transformType,
                        pairs = remaining,
                        dagMatches = dagMatches,
                        computeOutliers = false
                    )
                    OutlierSuggestion(
                        pointId = cp.id,
                        label = cp.label,
                        residualPx = cp.residualErrorPx,
                        rmseBefore = rmse,
                        rmseIfRemoved = sub.summary.rmseErrorPx
                    )
                } else null
            }.sortedByDescending { it.residualPx }
        } else emptyList()

        val finalSummary = affineReport.copy(
            transformType = transformType,
            rmseErrorPx = rmse,
            maxResidualPx = maxErr,
            controlPoints = updatedPairs
        )
        return SolvedPantographModel(finalSummary, transformFn, outliers)
    }

    private fun buildSimilarityFunction(pairs: List<ControlPointPair>): (Double, Double) -> Pair<Double, Double> {
        val p1 = pairs[0]
        val p2 = pairs[1]
        val dBase = hypot(p2.baseX - p1.baseX, p2.baseY - p1.baseY).coerceAtLeast(1e-6)
        val dOver = hypot(p2.overlayX - p1.overlayX, p2.overlayY - p1.overlayY).coerceAtLeast(1e-6)
        val s = dBase / dOver
        val theta = atan2(p2.baseY - p1.baseY, p2.baseX - p1.baseX) - atan2(p2.overlayY - p1.overlayY, p2.overlayX - p1.overlayX)
        val cosT = cos(theta) * s
        val sinT = sin(theta) * s
        val shiftX = p1.baseX - (p1.overlayX * cosT - p1.overlayY * sinT)
        val shiftY = p1.baseY - (p1.overlayX * sinT + p1.overlayY * cosT)
        return { ox, oy ->
            (shiftX + ox * cosT - oy * sinT) to (shiftY + ox * sinT + oy * cosT)
        }
    }

    private fun buildAffineFunction(pairs: List<ControlPointPair>): (Double, Double) -> Pair<Double, Double> {
        val n = pairs.size
        val rows = pairs.map { doubleArrayOf(1.0, it.overlayX, it.overlayY) }
        val ax = solveNormalEquations(rows, pairs.map { it.baseX }, 3)
        val ay = solveNormalEquations(rows, pairs.map { it.baseY }, 3)
        if (ax == null || ay == null || n < 3) return buildSimilarityFunction(pairs)
        return { ox, oy ->
            (ax[0] + ax[1] * ox + ax[2] * oy) to (ay[0] + ay[1] * ox + ay[2] * oy)
        }
    }

    /**
     * 8-DOF Projective Homography:
     * baseX = (h0*x + h1*y + h2) / (h6*x + h7*y + 1)
     * baseY = (h3*x + h4*y + h5) / (h6*x + h7*y + 1)
     */
    private fun solveHomography(pairs: List<ControlPointPair>): ((Double, Double) -> Pair<Double, Double>)? {
        if (pairs.size < 4) return null
        val designRows = ArrayList<DoubleArray>(pairs.size * 2)
        val targets = ArrayList<Double>(pairs.size * 2)
        for (p in pairs) {
            val ox = p.overlayX
            val oy = p.overlayY
            val bx = p.baseX
            val by = p.baseY
            designRows.add(doubleArrayOf(ox, oy, 1.0, 0.0, 0.0, 0.0, -bx * ox, -bx * oy))
            targets.add(bx)
            designRows.add(doubleArrayOf(0.0, 0.0, 0.0, ox, oy, 1.0, -by * ox, -by * oy))
            targets.add(by)
        }
        val h = solveNormalEquations(designRows, targets, 8) ?: return null
        return { ox, oy ->
            val denom = (h[6] * ox + h[7] * oy + 1.0).let { if (abs(it) < 1e-8) 1e-8 else it }
            val bx = (h[0] * ox + h[1] * oy + h[2]) / denom
            val by = (h[3] * ox + h[4] * oy + h[5]) / denom
            bx to by
        }
    }

    /**
     * True 2D Thin-Plate Spline (TPS) interpolation:
     * f(x,y) = a0 + a1*x + a2*y + sum_i w_i * U(|P_i - (x,y)|), where U(r) = r^2 * ln(r^2)
     */
    private fun solveThinPlateSpline(pairs: List<ControlPointPair>): ((Double, Double) -> Pair<Double, Double>)? {
        val n = pairs.size
        if (n < 3) return null
        val dim = n + 3
        val matX = Array(dim) { DoubleArray(dim) }
        val rhsX = DoubleArray(dim)
        val rhsY = DoubleArray(dim)

        // Small regularization lambda helps prevent ill-conditioning on collinear control points
        val reg = 1e-4

        for (i in 0 until n) {
            for (j in 0 until n) {
                matX[i][j] = if (i == j) reg else tpsKernel(
                    pairs[i].overlayX - pairs[j].overlayX,
                    pairs[i].overlayY - pairs[j].overlayY
                )
            }
            matX[i][n] = 1.0
            matX[i][n + 1] = pairs[i].overlayX
            matX[i][n + 2] = pairs[i].overlayY

            matX[n][i] = 1.0
            matX[n + 1][i] = pairs[i].overlayX
            matX[n + 2][i] = pairs[i].overlayY

            rhsX[i] = pairs[i].baseX
            rhsY[i] = pairs[i].baseY
        }

        val matCopy = Array(dim) { r -> matX[r].clone() }
        val solX = solveSquareLinearSystem(matX, rhsX) ?: return null
        val solY = solveSquareLinearSystem(matCopy, rhsY) ?: return null

        val pts = pairs.map { it.overlayX to it.overlayY }
        return { ox, oy ->
            var px = solX[n] + solX[n + 1] * ox + solX[n + 2] * oy
            var py = solY[n] + solY[n + 1] * ox + solY[n + 2] * oy
            for (i in 0 until n) {
                val u = tpsKernel(ox - pts[i].first, oy - pts[i].second)
                px += solX[i] * u
                py += solY[i] * u
            }
            px to py
        }
    }

    private fun tpsKernel(dx: Double, dy: Double): Double {
        val r2 = dx * dx + dy * dy
        return if (r2 <= 1e-12) 0.0 else r2 * ln(r2)
    }

    /**
     * Clips subject polygon against a convex/simple clip polygon and computes intersection area.
     * Uses Sutherland-Hodgman when clip is convex and falls back cleanly to grid sampling if concave.
     */
    fun computePolygonOverlapAreaUnits(
        polyA: List<Pair<Double, Double>>,
        polyB: List<Pair<Double, Double>>
    ): Double {
        if (polyA.size < 3 || polyB.size < 3) return 0.0
        if (isConvex(polyB)) {
            val clipped = sutherlandHodgmanClip(polyA, polyB)
            return shoelaceArea(clipped)
        }
        if (isConvex(polyA)) {
            val clipped = sutherlandHodgmanClip(polyB, polyA)
            return shoelaceArea(clipped)
        }
        // Fallback adaptive 40x40 grid integration for concave-concave overlap
        val minX = maxOf(polyA.minOf { it.first }, polyB.minOf { it.first })
        val maxX = minOf(polyA.maxOf { it.first }, polyB.maxOf { it.first })
        val minY = maxOf(polyA.minOf { it.second }, polyB.minOf { it.second })
        val maxY = minOf(polyA.maxOf { it.second }, polyB.maxOf { it.second })
        if (maxX <= minX || maxY <= minY) return 0.0
        val steps = 40
        val dx = (maxX - minX) / steps
        val dy = (maxY - minY) / steps
        var hits = 0
        for (ix in 0 until steps) {
            val cx = minX + (ix + 0.5) * dx
            for (iy in 0 until steps) {
                val cy = minY + (iy + 0.5) * dy
                if (pointInPoly(cx, cy, polyA) && pointInPoly(cx, cy, polyB)) {
                    hits++
                }
            }
        }
        return hits * dx * dy
    }

    /**
     * Transforms old-map Dag polygons onto the base/new map and builds the Overlap Intersection Table.
     */
    fun computeDagOverlapTable(
        oldDags: List<DagPolygonInput>,
        newDags: List<DagPolygonInput>,
        overlayToBaseTransform: (Double, Double) -> Pair<Double, Double>,
        sqFeetPerMapUnitSq: Double = 1.0
    ): List<PantographOverlapRow> {
        val rows = mutableListOf<PantographOverlapRow>()
        for (old in oldDags) {
            val transformed = old.vertices.map { (x, y) -> overlayToBaseTransform(x, y) }
            val oldAreaSqFt = shoelaceArea(transformed) * sqFeetPerMapUnitSq
            if (oldAreaSqFt <= 1e-6) continue
            for (newDag in newDags) {
                val newAreaSqFt = shoelaceArea(newDag.vertices) * sqFeetPerMapUnitSq
                if (newAreaSqFt <= 1e-6) continue
                val overlapSqFt = computePolygonOverlapAreaUnits(transformed, newDag.vertices) * sqFeetPerMapUnitSq
                val overlapDec = overlapSqFt / LandCalculators.SQ_FT_PER_DECIMAL
                if (overlapDec > 0.005) {
                    rows.add(
                        PantographOverlapRow(
                            oldDag = old.dagNumber,
                            newDag = newDag.dagNumber,
                            overlapDecimal = overlapDec,
                            overlapPctOfOld = (overlapSqFt / oldAreaSqFt * 100.0).coerceIn(0.0, 100.0),
                            overlapPctOfNew = (overlapSqFt / newAreaSqFt * 100.0).coerceIn(0.0, 100.0)
                        )
                    )
                }
            }
        }
        return rows.sortedByDescending { it.overlapDecimal }
    }

    fun exportPantographVintageReportPdf(
        context: Context,
        summary: PantographReportSummary,
        scaleResult: ScalePantographResult,
        overlapRows: List<PantographOverlapRow>,
        oldDags: List<DagPolygonInput>,
        newDags: List<DagPolygonInput>,
        overlayToBase: (Double, Double) -> Pair<Double, Double>
    ): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "PantographReports").apply { mkdirs() }
        val file = File(dir, "Pantograph_${summary.baseStage.name}_vs_${summary.overlayStage.name}_${System.currentTimeMillis()}.pdf")

        fun drawPantographPage(c: Canvas) {
            c.drawColor(Color.parseColor("#F5ECD8"))
            val borderPaint = Paint().apply {
                color = Color.parseColor("#5D4037")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            c.drawRect(22f, 22f, 573f, 820f, borderPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3E2723")
                textSize = 16.5f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2B1D16")
                textSize = 10.5f
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6D4C41")
                textSize = 9f
            }

            var y = 48f
            c.drawText("ডিজিটাল প্যান্টোগ্রাফ ও দাগ স্থানান্তর প্রতিবেদন", 36f, y, titlePaint)
            y += 18f
            c.drawText(
                "ম্যাপ তুলনা: ${summary.baseStage.label} (বেস) ↔ ${summary.overlayStage.label} (ওভারলে)  |  মডেল: ${summary.transformType.name}  |  তারিখ: ${SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())}",
                36f,
                y,
                mutedPaint
            )
            y += 16f
            c.drawText(scaleResult.formulaSummaryBn, 36f, y, bodyPaint)
            y += 16f
            c.drawText(
                String.format(
                    Locale.US,
                    "Scale X/Y: %.4f / %.4f  |  Rotation: %.2f°  |  RMSE: %.3f px  |  Max Residual: %.3f px",
                    summary.scaleX, summary.scaleY, summary.rotationDeg, summary.rmseErrorPx, summary.maxResidualPx
                ),
                36f,
                y,
                bodyPaint
            )
            y += 16f

            val boxTop = y
            val boxBottom = y + 210f
            c.drawRect(36f, boxTop, 559f, boxBottom, borderPaint.apply { strokeWidth = 1f })
            val allPts = (newDags.flatMap { it.vertices } + oldDags.flatMap { d -> d.vertices.map { overlayToBase(it.first, it.second) } })
            if (allPts.isNotEmpty()) {
                val minX = allPts.minOf { it.first }
                val maxX = allPts.maxOf { it.first }.coerceAtLeast(minX + 1.0)
                val minY = allPts.minOf { it.second }
                val maxY = allPts.maxOf { it.second }.coerceAtLeast(minY + 1.0)
                val sx = 490f / (maxX - minX).toFloat()
                val sy = 175f / (maxY - minY).toFloat()
                val s = minOf(sx, sy)
                fun mapPt(pt: Pair<Double, Double>): Pair<Float, Float> {
                    val px = 52f + ((pt.first - minX) * s).toFloat()
                    val py = (boxTop + 18f) + ((pt.second - minY) * s).toFloat()
                    return px to py
                }

                val newPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1565C0")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                newDags.forEach { d ->
                    if (d.vertices.size >= 3) {
                        val path = Path()
                        val first = mapPt(d.vertices.first())
                        path.moveTo(first.first, first.second)
                        d.vertices.drop(1).forEach { v -> val p = mapPt(v); path.lineTo(p.first, p.second) }
                        path.close()
                        c.drawPath(path, newPaint)
                        c.drawText("নতুন ${d.dagNumber}", first.first + 4f, first.second + 14f, bodyPaint)
                    }
                }

                val oldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1B5E20")
                    style = Paint.Style.STROKE
                    strokeWidth = 2.4f
                    pathEffect = DashPathEffect(floatArrayOf(8f, 5f), 0f)
                }
                oldDags.forEach { d ->
                    val tr = d.vertices.map { overlayToBase(it.first, it.second) }
                    if (tr.size >= 3) {
                        val path = Path()
                        val first = mapPt(tr.first())
                        path.moveTo(first.first, first.second)
                        tr.drop(1).forEach { v -> val p = mapPt(v); path.lineTo(p.first, p.second) }
                        path.close()
                        c.drawPath(path, oldPaint)
                        c.drawText("সাবেক ${d.dagNumber}", first.first + 6f, first.second + 28f, mutedPaint)
                    }
                }
            }
            y = boxBottom + 22f

            c.drawText("সাবেক দাগ → নতুন দাগ ছেদ-ক্ষেত্রফল সারণি (Overlap Table):", 36f, y, titlePaint.apply { textSize = 12f })
            y += 18f
            if (overlapRows.isEmpty()) {
                c.drawText("• কোনো দাগ ওভারল্যাপ সনাক্ত হয়নি — দাগ বহুভুজ ও কন্ট্রোল পয়েন্ট যাচাই করুন।", 40f, y, bodyPaint)
                y += 18f
            } else {
                overlapRows.take(12).forEach { r ->
                    c.drawText(
                        String.format(
                            Locale.US,
                            "সাবেক দাগ %-6s → নতুন দাগ %-6s  |  ছেদ: %.3f শতক  |  সাবেকের %.1f%%  |  নতুনের %.1f%%",
                            r.oldDag, r.newDag, r.overlapDecimal, r.overlapPctOfOld, r.overlapPctOfNew
                        ),
                        40f,
                        y,
                        bodyPaint
                    )
                    y += 16f
                }
            }

            y += 8f
            c.drawText("কন্ট্রোল পয়েন্ট ও রেসিডুয়াল (${summary.controlPoints.size}টি):", 36f, y, titlePaint.apply { textSize = 11.5f })
            y += 16f
            summary.controlPoints.take(8).forEach { cp ->
                c.drawText(
                    String.format(
                        Locale.US,
                        "• %-5s Base(%.1f, %.1f) ↔ Over(%.1f, %.1f) | Residual: %.3f px",
                        cp.label, cp.baseX, cp.baseY, cp.overlayX, cp.overlayY, cp.residualErrorPx
                    ),
                    40f,
                    y,
                    mutedPaint
                )
                y += 14f
            }

            c.drawText(
                "ব্যক্তিগত ব্যবহারের কপি — সরকারি রেজিস্টার/রেকর্ড নয়; নিবন্ধিত দলিল/সরকারি পর্চাই আইনত গ্রাহ্য।",
                36f,
                806f,
                mutedPaint
            )
        }

        val nativeOk = runCatching {
            val doc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = doc.startPage(pageInfo)
                drawPantographPage(page.canvas)
                doc.finishPage(page)
                FileOutputStream(file).use { doc.writeTo(it) }
                file.exists() && file.length() > 128
            } finally {
                runCatching { doc.close() }
            }
        }.getOrDefault(false)

        if (!nativeOk) {
            val bmp = Bitmap.createBitmap(595 * 2, 842 * 2, Bitmap.Config.ARGB_8888)
            try {
                Canvas(bmp).apply { scale(2f, 2f); drawPantographPage(this) }
                val bytes = ExportStreamWriter.encodeBitmapsAsPortablePdfBytes(listOf(bmp))
                FileOutputStream(file).use { it.write(bytes) }
            } finally {
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
        return file
    }

    // -------------------- Linear Algebra & Clipping Helpers --------------------

    private fun isConvex(poly: List<Pair<Double, Double>>): Boolean {
        if (poly.size < 3) return false
        var sign = 0
        for (i in poly.indices) {
            val p0 = poly[i]
            val p1 = poly[(i + 1) % poly.size]
            val p2 = poly[(i + 2) % poly.size]
            val cross = (p1.first - p0.first) * (p2.second - p1.second) - (p1.second - p0.second) * (p2.first - p1.first)
            if (abs(cross) > 1e-9) {
                val curSign = if (cross > 0) 1 else -1
                if (sign == 0) sign = curSign else if (sign != curSign) return false
            }
        }
        return true
    }

    private fun sutherlandHodgmanClip(
        subject: List<Pair<Double, Double>>,
        clip: List<Pair<Double, Double>>
    ): List<Pair<Double, Double>> {
        // Ensure clip is CCW
        val clipOrdered = if (signedArea(clip) < 0) clip.reversed() else clip
        var output = subject
        for (i in clipOrdered.indices) {
            val a = clipOrdered[i]
            val b = clipOrdered[(i + 1) % clipOrdered.size]
            val input = output
            output = mutableListOf()
            if (input.isEmpty()) break
            var s = input.last()
            for (e in input) {
                val eInside = cross(a, b, e) >= -1e-9
                val sInside = cross(a, b, s) >= -1e-9
                if (eInside) {
                    if (!sInside) {
                        intersectLines(s, e, a, b)?.let { output.add(it) }
                    }
                    output.add(e)
                } else if (sInside) {
                    intersectLines(s, e, a, b)?.let { output.add(it) }
                }
                s = e
            }
        }
        return output
    }

    private fun cross(a: Pair<Double, Double>, b: Pair<Double, Double>, p: Pair<Double, Double>): Double {
        return (b.first - a.first) * (p.second - a.second) - (b.second - a.second) * (p.first - a.first)
    }

    private fun intersectLines(
        p1: Pair<Double, Double>,
        p2: Pair<Double, Double>,
        p3: Pair<Double, Double>,
        p4: Pair<Double, Double>
    ): Pair<Double, Double>? {
        val a1 = p2.second - p1.second
        val b1 = p1.first - p2.first
        val c1 = a1 * p1.first + b1 * p1.second
        val a2 = p4.second - p3.second
        val b2 = p3.first - p4.first
        val c2 = a2 * p3.first + b2 * p3.second
        val det = a1 * b2 - a2 * b1
        if (abs(det) < 1e-12) return null
        return ((b2 * c1 - b1 * c2) / det) to ((a1 * c2 - a2 * c1) / det)
    }

    private fun signedArea(poly: List<Pair<Double, Double>>): Double {
        if (poly.size < 3) return 0.0
        var s = 0.0
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            s += a.first * b.second - b.first * a.second
        }
        return s * 0.5
    }

    fun shoelaceArea(poly: List<Pair<Double, Double>>): Double = abs(signedArea(poly))

    private fun pointInPoly(x: Double, y: Double, poly: List<Pair<Double, Double>>): Boolean {
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val xi = poly[i].first
            val yi = poly[i].second
            val xj = poly[j].first
            val yj = poly[j].second
            val inter = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (abs(it) < 1e-9) 1e-9 else it }) + xi)
            if (inter) inside = !inside
            j = i
        }
        return inside
    }

    private fun solveNormalEquations(rows: List<DoubleArray>, targets: List<Double>, dim: Int): DoubleArray? {
        val ata = Array(dim) { DoubleArray(dim) }
        val atb = DoubleArray(dim)
        for (rIdx in rows.indices) {
            val r = rows[rIdx]
            val y = targets[rIdx]
            for (i in 0 until dim) {
                atb[i] += r[i] * y
                for (j in 0 until dim) {
                    ata[i][j] += r[i] * r[j]
                }
            }
        }
        return solveSquareLinearSystem(ata, atb)
    }

    private fun solveSquareLinearSystem(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        for (col in 0 until n) {
            var pivot = col
            for (row in col + 1 until n) {
                if (abs(a[row][col]) > abs(a[pivot][col])) pivot = row
            }
            val tmpR = a[col]; a[col] = a[pivot]; a[pivot] = tmpR
            val tmpB = b[col]; b[col] = b[pivot]; b[pivot] = tmpB
            if (abs(a[col][col]) < 1e-11) return null
            for (row in col + 1 until n) {
                val f = a[row][col] / a[col][col]
                b[row] -= f * b[col]
                for (j in col until n) a[row][j] -= f * a[col][j]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = b[i]
            for (j in i + 1 until n) sum -= a[i][j] * x[j]
            x[i] = sum / a[i][i]
        }
        return x
    }
}
