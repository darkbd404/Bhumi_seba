package com.example.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.model.AccuracyUncertaintyResult
import com.example.model.ConfidenceLevel
import com.example.model.GlobalSurveySettings
import com.example.model.MeasureBetweenResult
import com.example.model.MouzaPlot
import com.example.model.ParcelTableRow
import com.example.model.ShoelaceStepRow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class TapePolygonBuildResult(
    val verticesFt: List<Pair<Double, Double>>,
    val areaSqFeet: Double,
    val perimeterFeet: Double,
    val closureErrorFeet: Double,
    val closureRatioDenom: Double
)

object GeometryAndAccuracyEngine {

    /**
     * Step-by-step Shoelace calculation in feet. Returns total area SqFt and itemized rows.
     */
    fun computeShoelaceWithSteps(verticesFt: List<Pair<Double, Double>>): Pair<Double, List<ShoelaceStepRow>> {
        if (verticesFt.size < 3) return 0.0 to emptyList()
        val rows = ArrayList<ShoelaceStepRow>(verticesFt.size)
        var sum = 0.0
        for (i in verticesFt.indices) {
            val a = verticesFt[i]
            val b = verticesFt[(i + 1) % verticesFt.size]
            val p1 = a.first * b.second
            val p2 = b.first * a.second
            val net = p1 - p2
            sum += net
            rows.add(
                ShoelaceStepRow(
                    vertexIndex = i + 1,
                    xFt = a.first,
                    yFt = a.second,
                    xNextFt = b.first,
                    yNextFt = b.second,
                    prodXY = p1,
                    prodYX = p2,
                    netTerm = net
                )
            )
        }
        return (abs(sum) * 0.5) to rows
    }

    fun computePolygonPerimeterFt(verticesFt: List<Pair<Double, Double>>): Double {
        if (verticesFt.size < 2) return 0.0
        var p = 0.0
        val count = if (verticesFt.size >= 3) verticesFt.size else verticesFt.size - 1
        for (i in 0 until count) {
            val a = verticesFt[i]
            val b = verticesFt[(i + 1) % verticesFt.size]
            p += hypot(b.first - a.first, b.second - a.second)
        }
        return p
    }

    /**
     * Subtracts enclosed hole/cut-out polygons (e.g., pond or excluded structure) from outer boundary.
     */
    fun computeNetAreaWithHolesSqFt(
        outerVerticesFt: List<Pair<Double, Double>>,
        holesVerticesFt: List<List<Pair<Double, Double>>>
    ): Double {
        val outerArea = computeShoelaceWithSteps(outerVerticesFt).first
        val holeArea = holesVerticesFt.sumOf { computeShoelaceWithSteps(it).first }
        return (outerArea - holeArea).coerceAtLeast(0.0)
    }

    /**
     * Interior angles (degrees) at each vertex of a closed polygon.
     */
    fun computeInteriorAnglesDeg(verticesFt: List<Pair<Double, Double>>): List<Double> {
        val n = verticesFt.size
        if (n < 3) return emptyList()
        return List(n) { i ->
            val prev = verticesFt[(i - 1 + n) % n]
            val curr = verticesFt[i]
            val next = verticesFt[(i + 1) % n]
            val v1x = prev.first - curr.first
            val v1y = prev.second - curr.second
            val v2x = next.first - curr.first
            val v2y = next.second - curr.second
            val m1 = hypot(v1x, v1y).coerceAtLeast(1e-9)
            val m2 = hypot(v2x, v2y).coerceAtLeast(1e-9)
            val dot = ((v1x * v2x + v1y * v2y) / (m1 * m2)).coerceIn(-1.0, 1.0)
            Math.toDegrees(acos(dot))
        }
    }

    /**
     * Lock Side Length solver: adjusts vertex (edgeIndex + 1) % n along edge direction so that
     * edge i -> i+1 equals targetFeet in real-world feet while keeping all other corners stable.
     */
    fun adjustNormalizedPolygonEdgeLength(
        polyNorm: List<Pair<Float, Float>>,
        edgeIndex: Int,
        targetFeet: Double,
        pageWidthFt: Double,
        pageHeightFt: Double
    ): List<Pair<Float, Float>> {
        if (polyNorm.size < 3 || targetFeet <= 0.0) return polyNorm
        val idxA = edgeIndex.coerceIn(0, polyNorm.lastIndex)
        val idxB = (idxA + 1) % polyNorm.size
        val aFtX = polyNorm[idxA].first * pageWidthFt
        val aFtY = polyNorm[idxA].second * pageHeightFt
        val bFtX = polyNorm[idxB].first * pageWidthFt
        val bFtY = polyNorm[idxB].second * pageHeightFt
        val dx = bFtX - aFtX
        val dy = bFtY - aFtY
        val curLen = hypot(dx, dy).coerceAtLeast(1e-6)
        val ratio = targetFeet / curLen
        val newBFtX = aFtX + dx * ratio
        val newBFtY = aFtY + dy * ratio
        val newNormX = (newBFtX / pageWidthFt.coerceAtLeast(1e-6)).toFloat().coerceIn(0.002f, 0.998f)
        val newNormY = (newBFtY / pageHeightFt.coerceAtLeast(1e-6)).toFloat().coerceIn(0.002f, 0.998f)
        return polyNorm.mapIndexed { idx, pt -> if (idx == idxB) (newNormX to newNormY) else pt }
    }

    /**
     * Builds a closed polygon from sequential tape measurements (feet) and azimuth/bearings (degrees).
     */
    fun buildPolygonFromTapeLegs(
        legDistancesFeet: List<Double>,
        legBearingsDeg: List<Double>
    ): TapePolygonBuildResult {
        if (legDistancesFeet.isEmpty() || legDistancesFeet.size != legBearingsDeg.size) {
            return TapePolygonBuildResult(emptyList(), 0.0, 0.0, 0.0, 0.0)
        }
        val pts = mutableListOf(0.0 to 0.0)
        var x = 0.0
        var y = 0.0
        var perim = 0.0
        for (i in legDistancesFeet.indices) {
            val d = legDistancesFeet[i].coerceAtLeast(0.0)
            val rad = Math.toRadians(legBearingsDeg[i])
            // Azimuth 0 deg = North (+Y), 90 deg = East (+X)
            x += d * sin(rad)
            y += d * cos(rad)
            perim += d
            pts.add(x to y)
        }
        val closureErr = hypot(x, y)
        val ratioDenom = if (closureErr > 1e-6) perim / closureErr else 999999.0
        val polyClosed = if (pts.size > 3) pts.dropLast(1) else pts
        val area = computeShoelaceWithSteps(polyClosed).first
        return TapePolygonBuildResult(
            verticesFt = polyClosed,
            areaSqFeet = area,
            perimeterFeet = perim,
            closureErrorFeet = closureErr,
            closureRatioDenom = ratioDenom
        )
    }

    /**
     * Distance from point P to segment A-B in feet + foot-of-perpendicular point in normalized coords.
     */
    fun pointToSegmentPerpendicularFt(
        pointNorm: Pair<Float, Float>,
        segANorm: Pair<Float, Float>,
        segBNorm: Pair<Float, Float>,
        pageWidthFt: Double,
        pageHeightFt: Double
    ): Pair<Double, Pair<Float, Float>> {
        val px = pointNorm.first * pageWidthFt
        val py = pointNorm.second * pageHeightFt
        val ax = segANorm.first * pageWidthFt
        val ay = segANorm.second * pageHeightFt
        val bx = segBNorm.first * pageWidthFt
        val by = segBNorm.second * pageHeightFt
        val abx = bx - ax
        val aby = by - ay
        val abLenSq = abx * abx + aby * aby
        val t = if (abLenSq <= 1e-9) 0.0 else (((px - ax) * abx + (py - ay) * aby) / abLenSq).coerceIn(0.0, 1.0)
        val projX = ax + t * abx
        val projY = ay + t * aby
        val distFt = hypot(px - projX, py - projY)
        val projNorm = ((projX / pageWidthFt.coerceAtLeast(1e-6)).toFloat() to (projY / pageHeightFt.coerceAtLeast(1e-6)).toFloat())
        return distFt to projNorm
    }

    /**
     * Minimum gap distance between two polygons (Plot <-> Plot) in feet, plus closest endpoints.
     */
    fun measureBetweenPolygonsFt(
        polyA: List<Pair<Float, Float>>,
        polyB: List<Pair<Float, Float>>,
        pageWidthFt: Double,
        pageHeightFt: Double
    ): MeasureBetweenResult {
        if (polyA.isEmpty() || polyB.isEmpty()) {
            return MeasureBetweenResult("প্লট ↔ প্লট দূরত্ব", 0.0, "0'0\"", "দুটি প্লট নির্বাচন করুন")
        }
        var bestDist = Double.MAX_VALUE
        var bestP1: Pair<Float, Float>? = null
        var bestP2: Pair<Float, Float>? = null

        for (pa in polyA) {
            for (i in polyB.indices) {
                val b1 = polyB[i]
                val b2 = polyB[(i + 1) % polyB.size]
                val (d, proj) = pointToSegmentPerpendicularFt(pa, b1, b2, pageWidthFt, pageHeightFt)
                if (d < bestDist) {
                    bestDist = d
                    bestP1 = pa
                    bestP2 = proj
                }
            }
        }
        for (pb in polyB) {
            for (i in polyA.indices) {
                val a1 = polyA[i]
                val a2 = polyA[(i + 1) % polyA.size]
                val (d, proj) = pointToSegmentPerpendicularFt(pb, a1, a2, pageWidthFt, pageHeightFt)
                if (d < bestDist) {
                    bestDist = d
                    bestP1 = proj
                    bestP2 = pb
                }
            }
        }
        if (bestDist == Double.MAX_VALUE) bestDist = 0.0
        val fmt = LandCalculators.formatFeetInchesSuta(bestDist)
        return MeasureBetweenResult(
            modeTitleBn = "প্লট ↔ প্লট সর্বনিম্ন দূরত্ব",
            distanceFt = bestDist,
            distanceFormatted = fmt,
            detailsBn = String.format(Locale.US, "সর্বনিম্ন ব্যবধান: %.2f ফুট (%s)", bestDist, fmt),
            p1Norm = bestP1,
            p2Norm = bestP2
        )
    }

    /**
     * Computes frontage length of a plot along a user-drawn road line segment (closest adjacent edges).
     */
    fun computePlotRoadFrontageFt(
        polyNorm: List<Pair<Float, Float>>,
        roadANorm: Pair<Float, Float>,
        roadBNorm: Pair<Float, Float>,
        pageWidthFt: Double,
        pageHeightFt: Double
    ): MeasureBetweenResult {
        if (polyNorm.size < 2) {
            return MeasureBetweenResult("রাস্তার দিকের মুখ (Frontage)", 0.0, "0'0\"", "অবৈধ প্লট")
        }
        val rAx = roadANorm.first * pageWidthFt
        val rAy = roadANorm.second * pageHeightFt
        val rBx = roadBNorm.first * pageWidthFt
        val rBy = roadBNorm.second * pageHeightFt
        val rDx = rBx - rAx
        val rDy = rBy - rAy
        val rLen = hypot(rDx, rDy).coerceAtLeast(1e-6)
        val ux = rDx / rLen
        val uy = rDy / rLen

        // Find the polygon edge whose midpoint is closest to the road line and roughly aligned
        var bestEdgeLen = 0.0
        var bestScore = Double.MAX_VALUE
        var bestMidDist = 0.0
        var bestP1: Pair<Float, Float>? = null
        var bestP2: Pair<Float, Float>? = null

        for (i in polyNorm.indices) {
            val a = polyNorm[i]
            val b = polyNorm[(i + 1) % polyNorm.size]
            val mid = ((a.first + b.first) * 0.5f) to ((a.second + b.second) * 0.5f)
            val (distToRoad, _) = pointToSegmentPerpendicularFt(mid, roadANorm, roadBNorm, pageWidthFt, pageHeightFt)
            val ex = (b.first - a.first) * pageWidthFt
            val ey = (b.second - a.second) * pageHeightFt
            val edgeLen = hypot(ex, ey).coerceAtLeast(1e-6)
            val cosAlign = abs((ex * ux + ey * uy) / edgeLen)
            val score = distToRoad + (1.0 - cosAlign) * 15.0
            if (score < bestScore) {
                bestScore = score
                bestEdgeLen = edgeLen
                bestMidDist = distToRoad
                bestP1 = a
                bestP2 = b
            }
        }
        val fmt = LandCalculators.formatFeetInchesSuta(bestEdgeLen)
        return MeasureBetweenResult(
            modeTitleBn = "রাস্তার দিকের মুখ (Frontage)",
            distanceFt = bestEdgeLen,
            distanceFormatted = fmt,
            detailsBn = String.format(
                Locale.US,
                "মুখের দৈর্ঘ্য: %.2f ফুট (%s) • রাস্তা থেকে সেটব্যাক: %.1f ফুট",
                bestEdgeLen, fmt, bestMidDist
            ),
            p1Norm = bestP1,
            p2Norm = bestP2
        )
    }

    /**
     * M02: Analytic Error Propagation for Area & Edges combining:
     * - Calibration uncertainty (%)
     * - Image pixel resolution (ft / pixel)
     * - Zoom placement jitter (baseJitterPx / zoom)
     * - GPS accuracy radius (meters, when GPS source used)
     */
    fun computeMeasurementUncertainty(
        areaSqFeet: Double,
        perimeterFeet: Double,
        edgeLengthsFt: List<Double>,
        calibrationErrPct: Double = 0.6,
        ftPerPixel: Double = 0.35,
        zoomScale: Float = 1f,
        gpsAccuracyM: Double = 0.0,
        pointPlacementJitterPx: Double = 1.6,
        settings: GlobalSurveySettings = GlobalSurveySettings(),
        includeDisclaimer: Boolean = true
    ): AccuracyUncertaintyResult {
        val safeArea = areaSqFeet.coerceAtLeast(0.0)
        val safePerim = perimeterFeet.coerceAtLeast(0.0)
        val effectiveZoom = zoomScale.coerceAtLeast(0.5f).toDouble()

        val pointJitterFt = (pointPlacementJitterPx / effectiveZoom) * ftPerPixel
        val gpsJitterFt = gpsAccuracyM * 3.280839895 * 0.45
        val sigmaPointFt = hypot(pointJitterFt, gpsJitterFt)

        // Edge uncertainty: linear calibration contribution + sqrt(2) * vertex jitter
        val calFrac = (calibrationErrPct.coerceAtLeast(0.0)) / 100.0
        val edgeErrors = edgeLengthsFt.map { len ->
            hypot(len * calFrac, 1.4142 * sigmaPointFt)
        }

        // Analytic area error:
        // dA_cal = A * (2 * calFrac) [since scale enters squared in area]
        // dA_vertex ≈ 0.5 * Perimeter * sigmaPointFt / sqrt(max(1, n/4))
        val dACal = safeArea * (2.0 * calFrac)
        val nEdges = max(edgeLengthsFt.size, 3)
        val dAVertex = 0.5 * safePerim * sigmaPointFt / sqrt(max(1.0, nEdges / 3.0))
        val areaErrSqFt = hypot(dACal, dAVertex)

        val perimErrFt = if (edgeErrors.isNotEmpty()) {
            sqrt(edgeErrors.sumOf { it * it })
        } else {
            hypot(safePerim * calFrac, 2.0 * sigmaPointFt)
        }

        val preset = settings.activePreset()
        val areaObj = LandCalculators.fromSqFeet(safeArea, preset)
        val errObj = LandCalculators.fromSqFeet(areaErrSqFt, preset)
        val relPct = if (safeArea > 1e-6) (areaErrSqFt / safeArea) * 100.0 else 0.0

        val confidence = when {
            relPct <= settings.confidenceHighMaxErrPct -> ConfidenceLevel.HIGH
            relPct <= settings.confidenceMedMaxErrPct -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val sourceSummary = buildString {
            append("স্কেল ত্রুটি ±${String.format(Locale.US, "%.2f", calibrationErrPct)}%")
            if (ftPerPixel > 0) {
                append(" • রেজোলিউশন ${String.format(Locale.US, "%.2f", ftPerPixel)} ft/px (Zoom ${String.format(Locale.US, "%.1f", effectiveZoom)}x)")
            }
            if (gpsAccuracyM > 0.0) {
                append(" • GPS ±${String.format(Locale.US, "%.1f", gpsAccuracyM)} মি.")
            }
        }

        return AccuracyUncertaintyResult(
            areaSqFt = areaObj.sqFeet,
            areaErrSqFt = areaErrSqFt,
            decimal = areaObj.decimal,
            decimalErr = errObj.decimal,
            katha = areaObj.katha,
            kathaErr = errObj.katha,
            perimeterFt = safePerim,
            perimeterErrFt = perimErrFt,
            relativeErrPct = relPct,
            edgeErrorsFt = edgeErrors,
            confidence = confidence,
            includeDisclaimer = includeDisclaimer,
            sourceSummaryBn = sourceSummary
        )
    }

    fun buildParcelTableRow(
        plot: MouzaPlot,
        pageWidthPts: Int,
        pageHeightPts: Int,
        scale: com.example.model.MouzaScale,
        calibrationX: Double,
        calibrationY: Double,
        calibrationErrPct: Double,
        settings: GlobalSurveySettings
    ): ParcelTableRow {
        val pageFtW = (pageWidthPts / 72.0) * scale.feetPerInch * calibrationX
        val pageFtH = (pageHeightPts / 72.0) * scale.feetPerInch * calibrationY
        val poly = plot.effectivePolygon()
        val areaSqFt: Double
        val perimFt: Double
        val edgesFt: List<Double>
        if (poly != null && poly.size >= 3) {
            val outerFt = poly.map { (it.first * pageFtW) to (it.second * pageFtH) }
            val holesFt = plot.holePolygonsNormalized.map { h ->
                h.map { (it.first * pageFtW) to (it.second * pageFtH) }
            }
            areaSqFt = computeNetAreaWithHolesSqFt(outerFt, holesFt)
            perimFt = computePolygonPerimeterFt(outerFt)
            edgesFt = outerFt.indices.map { i ->
                val a = outerFt[i]
                val b = outerFt[(i + 1) % outerFt.size]
                hypot(b.first - a.first, b.second - a.second)
            }
        } else {
            val avgCal = (calibrationX + calibrationY) * 0.5
            areaSqFt = plot.exactAreaSqFeet ?: (plot.pixelCount * MouzaPdfEngine.computeSqFeetPerGridPixel(pageWidthPts, plot.gridWidth, scale, avgCal * avgCal))
            perimFt = sqrt(areaSqFt) * 4.0
            edgesFt = emptyList()
        }

        val preset = settings.activePreset()
        val res = LandCalculators.fromSqFeet(areaSqFt, preset)
        val err = computeMeasurementUncertainty(
            areaSqFeet = areaSqFt,
            perimeterFeet = perimFt,
            edgeLengthsFt = edgesFt,
            calibrationErrPct = calibrationErrPct,
            ftPerPixel = pageFtW / 1200.0,
            zoomScale = 2.0f,
            settings = settings
        )
        return ParcelTableRow(
            plotId = plot.id,
            plotIndex = plot.plotIndex,
            dagNumber = plot.dagNumber.ifBlank { "#${plot.plotIndex}" },
            khatianNumber = plot.khatianNumber.ifBlank { "-" },
            ownerName = plot.ownerName.ifBlank { "-" },
            isRoad = plot.isRoad,
            decimal = res.decimal,
            katha = res.katha,
            bigha = res.bigha,
            sqFeet = res.sqFeet,
            acre = res.acre,
            perimeterFt = perimFt,
            errorDecimal = err.decimalErr,
            remarks = when {
                plot.isRoad -> "রাস্তা (মোট জমিতে বাদ)"
                plot.holePolygonsNormalized.isNotEmpty() -> "হোল বাদ (${plot.holePolygonsNormalized.size}টি) ${plot.remarks}".trim()
                else -> plot.remarks
            }
        )
    }

    fun exportParcelTableCsv(rows: List<ParcelTableRow>, projectTitle: String): String {
        val sb = StringBuilder()
        sb.append("# $projectTitle\n")
        sb.append("Dag No,Khatian No,Owner,Is Road,Decimal (Shotok),Error (±Dec),Katha,Bigha,SqFeet,Acre,Perimeter (ft),Remarks\n")
        rows.forEach { r ->
            sb.append(
                String.format(
                    Locale.US,
                    "\"%s\",\"%s\",\"%s\",%s,%.3f,%.3f,%.3f,%.4f,%.1f,%.4f,%.2f,\"%s\"\n",
                    r.dagNumber.replace("\"", "'"),
                    r.khatianNumber.replace("\"", "'"),
                    r.ownerName.replace("\"", "'"),
                    if (r.isRoad) "Yes" else "No",
                    r.decimal,
                    r.errorDecimal,
                    r.katha,
                    r.bigha,
                    r.sqFeet,
                    r.acre,
                    r.perimeterFt,
                    r.remarks.replace("\"", "'")
                )
            )
        }
        val usableRows = rows.filterNot { it.isRoad }
        sb.append(
            String.format(
                Locale.US,
                "\"TOTAL (Usable %d Plots)\",,,No,%.3f,%.3f,%.3f,%.4f,%.1f,%.4f,%.2f,\"Excludes roads\"\n",
                usableRows.size,
                usableRows.sumOf { it.decimal },
                sqrt(usableRows.sumOf { it.errorDecimal * it.errorDecimal }),
                usableRows.sumOf { it.katha },
                usableRows.sumOf { it.bigha },
                usableRows.sumOf { it.sqFeet },
                usableRows.sumOf { it.acre },
                usableRows.sumOf { it.perimeterFt }
            )
        )
        return sb.toString()
    }

    fun exportAccuracyReportPdf(
        context: Context,
        title: String,
        uncertainty: AccuracyUncertaintyResult,
        parcelRows: List<ParcelTableRow> = emptyList(),
        outFile: File? = null
    ): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "AccuracyReports").apply { mkdirs() }
        val file = outFile ?: File(dir, "Accuracy_Report_${System.currentTimeMillis()}.pdf")

        fun drawAccuracyPage(c: Canvas) {
            val bg = Paint().apply { color = Color.parseColor("#FAF6EC") }
            c.drawRect(0f, 0f, 595f, 842f, bg)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F5B48")
                textSize = 18f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1D2A26")
                textSize = 11.5f
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#546E66")
                textSize = 9.5f
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#B8C9C2")
                strokeWidth = 1.2f
            }

            var y = 48f
            c.drawText("ডিজিটাল জরিপ নির্ভুলতা ও ত্রুটি (±) রিপোর্ট", 40f, y, titlePaint)
            y += 20f
            c.drawText("প্রজেক্ট: $title  |  তারিখ: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}", 40f, y, mutedPaint)
            y += 14f
            c.drawLine(40f, y, 555f, y, linePaint)
            y += 26f

            c.drawText("আস্থার স্তর (Confidence): ${uncertainty.confidence.labelBn} (${uncertainty.confidence.badgeEn})  |  আপেক্ষিক ত্রুটি: ±${String.format(Locale.US, "%.2f", uncertainty.relativeErrPct)}%", 40f, y, titlePaint.apply { textSize = 13f })
            y += 22f
            c.drawText(String.format(Locale.US, "• ক্ষেত্রফল (শতাংশ): %.3f ± %.3f শতক", uncertainty.decimal, uncertainty.decimalErr), 48f, y, bodyPaint)
            y += 18f
            c.drawText(String.format(Locale.US, "• ক্ষেত্রফল (কাঠা): %.3f ± %.3f কাঠা", uncertainty.katha, uncertainty.kathaErr), 48f, y, bodyPaint)
            y += 18f
            c.drawText(String.format(Locale.US, "• ক্ষেত্রফল (বর্গফুট): %.1f ± %.1f বর্গফুট", uncertainty.areaSqFt, uncertainty.areaErrSqFt), 48f, y, bodyPaint)
            y += 18f
            c.drawText(String.format(Locale.US, "• পরিসীমা: %.2f ± %.2f ফুট", uncertainty.perimeterFt, uncertainty.perimeterErrFt), 48f, y, bodyPaint)
            y += 22f
            c.drawText("ত্রুটির উৎস: ${uncertainty.sourceSummaryBn}", 40f, y, mutedPaint)
            y += 20f

            if (parcelRows.isNotEmpty()) {
                c.drawLine(40f, y, 555f, y, linePaint)
                y += 20f
                c.drawText("দাগভিত্তিক পরিমাপ ও ± ত্রুটি সারণি (${parcelRows.size}টি):", 40f, y, titlePaint.apply { textSize = 12f })
                y += 18f
                parcelRows.take(24).forEach { r ->
                    c.drawText(
                        String.format(
                            Locale.US,
                            "দাগ %-8s | %.2f ± %.2f শতক | %.2f কাঠা | %.1f ft² | পরিসীমা %.1f ft %s",
                            r.dagNumber, r.decimal, r.errorDecimal, r.katha, r.sqFeet, r.perimeterFt,
                            if (r.isRoad) "[রাস্তা]" else ""
                        ),
                        42f,
                        y,
                        bodyPaint.apply { textSize = 10f }
                    )
                    y += 16f
                    if (y > 780f) return@forEach
                }
            }

            if (uncertainty.includeDisclaimer) {
                c.drawLine(40f, 800f, 555f, 800f, linePaint)
                c.drawText(
                    "দ্রষ্টব্য: এটি সার্ভে হিসাব ও প্রাথমিক যাচাইয়ের সহায়ক — সরকারি বা আইনগত জরিপের চূড়ান্ত বিকল্প নয়।",
                    40f,
                    818f,
                    mutedPaint
                )
            }
        }

        val nativeOk = runCatching {
            val doc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = doc.startPage(pageInfo)
                drawAccuracyPage(page.canvas)
                doc.finishPage(page)
                FileOutputStream(file).use { doc.writeTo(it) }
                file.exists() && file.length() > 128
            } finally {
                runCatching { doc.close() }
            }
        }.getOrDefault(false)

        if (!nativeOk) {
            val bmp = android.graphics.Bitmap.createBitmap(595 * 2, 842 * 2, android.graphics.Bitmap.Config.ARGB_8888)
            try {
                Canvas(bmp).apply { scale(2f, 2f); drawAccuracyPage(this) }
                val bytes = ExportStreamWriter.encodeBitmapsAsPortablePdfBytes(listOf(bmp))
                FileOutputStream(file).use { it.write(bytes) }
            } finally {
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
        return file
    }
}
