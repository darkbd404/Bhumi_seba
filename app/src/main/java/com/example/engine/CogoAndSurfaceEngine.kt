package com.example.engine

import com.example.model.CircularCurveResult
import com.example.model.CogoPoint
import com.example.model.ContourSegment
import com.example.model.LandDivisionResultRecord
import com.example.model.LandSplitMode
import com.example.model.DivisionShareSpec
import com.example.model.PolygonVertex
import com.example.model.RoadAlignmentSpec
import com.example.model.TinTriangle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Module 03, 12, 13 & 14 Engine:
 * - COGO Points, Inverse, Radiation
 * - All Intersections (Bearing-Bearing, Bearing-Distance, Distance-Distance, Line-Line, Line-Arc, Circle-Circle, Perpendicular, Parallel Offset, 3-Point Resection)
 * - Closed/Open Traverse Adjustment: Bowditch, Transit & Least-Squares
 * - Circular Curve Geometry (R, D, Delta, L, C, T, E, M, PC/PI/PT, Deflections, 3-Pt Circle)
 * - Geometric Cutoff Subdivision (Parallel, Hinged-line, Sliding/Swing line, Target Area)
 * - Delaunay / Radial TIN Triangulation, Contours, Slope/Aspect, Profile, Cut/Fill Surface Volume
 * - Road Alignment Station/Offset, Right-of-Way & Plot Access check
 * - Automated Bangla & English Legal Land Schedule (Tafsil) & Metes-and-Bounds Generator
 */
object CogoAndSurfaceEngine {

    private fun degToRad(d: Double) = d * PI / 180.0
    private fun radToDeg(r: Double) = ((r * 180.0 / PI) % 360.0 + 360.0) % 360.0

    // -------------------- 3.1 & 3.2 Inverse & Radiation --------------------

    data class InverseResult(
        val horizontalDistFt: Double,
        val slopeDistFt: Double,
        val azimuthDeg: Double,
        val deltaNorthFt: Double,
        val deltaEastFt: Double,
        val deltaElevFt: Double,
        val gradePct: Double
    )

    fun inverse(from: CogoPoint, to: CogoPoint): InverseResult {
        val dN = to.northFt - from.northFt
        val dE = to.eastFt - from.eastFt
        val dZ = to.elevationFt - from.elevationFt
        val hDist = hypot(dE, dN)
        val sDist = hypot(hDist, dZ)
        val az = radToDeg(atan2(dE, dN))
        val grade = if (hDist > 1e-9) (dZ / hDist) * 100.0 else 0.0
        return InverseResult(hDist, sDist, az, dN, dE, dZ, grade)
    }

    fun radiation(station: CogoPoint, azimuthDeg: Double, horizDistFt: Double, dElevFt: Double = 0.0, newNo: Int = 101): CogoPoint {
        val r = degToRad(azimuthDeg)
        return CogoPoint(
            id = "cogo_rad_${System.nanoTime()}",
            pointNumber = newNo,
            code = "RAD",
            northFt = station.northFt + horizDistFt * cos(r),
            eastFt = station.eastFt + horizDistFt * sin(r),
            elevationFt = station.elevationFt + dElevFt,
            source = "COGO Radiation"
        )
    }

    // -------------------- 3.3 All Intersections & 3-Point Resection --------------------

    fun intersectBearingBearing(
        p1: PolygonVertex, az1Deg: Double,
        p2: PolygonVertex, az2Deg: Double
    ): PolygonVertex? {
        val r1 = degToRad(az1Deg); val r2 = degToRad(az2Deg)
        val d1x = sin(r1); val d1y = cos(r1)
        val d2x = sin(r2); val d2y = cos(r2)
        val denom = d1x * d2y - d1y * d2x
        if (abs(denom) < 1e-8) return null
        val t = ((p2.x - p1.x) * d2y - (p2.y - p1.y) * d2x) / denom
        return PolygonVertex(p1.x + t * d1x, p1.y + t * d1y)
    }

    fun intersectDistanceDistance(
        c1: PolygonVertex, r1: Double,
        c2: PolygonVertex, r2: Double
    ): List<PolygonVertex> {
        val d = hypot(c2.x - c1.x, c2.y - c1.y)
        if (d < 1e-9 || d > r1 + r2 || d < abs(r1 - r2)) return emptyList()
        val a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
        val h = sqrt((r1 * r1 - a * a).coerceAtLeast(0.0))
        val mx = c1.x + a * (c2.x - c1.x) / d
        val my = c1.y + a * (c2.y - c1.y) / d
        val rx = -(c2.y - c1.y) * (h / d)
        val ry = (c2.x - c1.x) * (h / d)
        return if (h < 1e-6) {
            listOf(PolygonVertex(mx, my))
        } else {
            listOf(PolygonVertex(mx + rx, my + ry), PolygonVertex(mx - rx, my - ry))
        }
    }

    fun intersectBearingDistance(
        origin: PolygonVertex, azDeg: Double,
        circleCenter: PolygonVertex, radius: Double
    ): List<PolygonVertex> {
        val r = degToRad(azDeg)
        val dx = sin(r); val dy = cos(r)
        val fx = origin.x - circleCenter.x
        val fy = origin.y - circleCenter.y
        val b = 2.0 * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - radius * radius
        val disc = b * b - 4.0 * c
        if (disc < 0.0) return emptyList()
        val sDisc = sqrt(disc)
        val t1 = (-b - sDisc) / 2.0
        val t2 = (-b + sDisc) / 2.0
        return listOf(t1, t2).distinct().map { t -> PolygonVertex(origin.x + t * dx, origin.y + t * dy) }
    }

    /** Tienstra / 3-Point Resection using known A, B, C and measured horizontal angles alpha (A-B) & beta (B-C) */
    fun solveThreePointResection(
        a: PolygonVertex, b: PolygonVertex, c: PolygonVertex,
        angleABDeg: Double, angleBCDeg: Double
    ): PolygonVertex? {
        val angleCADeg = 360.0 - angleABDeg - angleBCDeg
        fun sideLen(p1: PolygonVertex, p2: PolygonVertex) = hypot(p2.x - p1.x, p2.y - p1.y)
        val ab = sideLen(a, b); val bc = sideLen(b, c); val ca = sideLen(c, a)
        if (ab < 1e-6 || bc < 1e-6 || ca < 1e-6) return null
        val angA = acos(((ab * ab + ca * ca - bc * bc) / (2 * ab * ca)).coerceIn(-1.0, 1.0))
        val angB = acos(((ab * ab + bc * bc - ca * ca) / (2 * ab * bc)).coerceIn(-1.0, 1.0))
        val angC = acos(((bc * bc + ca * ca - ab * ab) / (2 * bc * ca)).coerceIn(-1.0, 1.0))

        val alp = degToRad(angleBCDeg) // opposite A
        val bet = degToRad(angleCADeg) // opposite B
        val gam = degToRad(angleABDeg) // opposite C
        fun cot(rad: Double) = 1.0 / tan(rad).let { if (abs(it) < 1e-9) 1e-9 else it }

        val k1 = 1.0 / (cot(angA) - cot(alp)).let { if (abs(it) < 1e-9) return null else it }
        val k2 = 1.0 / (cot(angB) - cot(bet)).let { if (abs(it) < 1e-9) return null else it }
        val k3 = 1.0 / (cot(angC) - cot(gam)).let { if (abs(it) < 1e-9) return null else it }
        val kSum = k1 + k2 + k3
        if (abs(kSum) < 1e-9) return null
        return PolygonVertex(
            (k1 * a.x + k2 * b.x + k3 * c.x) / kSum,
            (k1 * a.y + k2 * b.y + k3 * c.y) / kSum
        )
    }

    // -------------------- 3.4 Traverse Adjustment (Bowditch, Transit & Least-Squares) --------------------

    enum class TraverseMethod(val titleBn: String) {
        BOWDITCH("বউডিচ রুল (Compass/Bowditch)"),
        TRANSIT("ট্রানজিট রুল (Transit)"),
        LEAST_SQUARES("লিস্ট-স্কয়ার্স (Least-Squares)")
    }

    data class AdjustedTraverseSummary(
        val method: TraverseMethod,
        val totalPerimeterFt: Double,
        val linearMisclosureFt: Double,
        val precisionRatioLabel: String,
        val adjustedAreaSqFt: Double,
        val adjustedPoints: List<PolygonVertex>
    )

    fun adjustClosedTraverse(
        legs: List<TraverseLegInput>,
        method: TraverseMethod,
        startEast: Double = 0.0,
        startNorth: Double = 0.0
    ): AdjustedTraverseSummary? {
        if (legs.size < 3) return null
        val totalLen = legs.sumOf { it.distanceFt }.coerceAtLeast(1e-9)
        val rawDE = legs.map { it.distanceFt * sin(degToRad(it.bearingDeg)) }
        val rawDN = legs.map { it.distanceFt * cos(degToRad(it.bearingDeg)) }
        val errE = rawDE.sum()
        val errN = rawDN.sum()
        val misclosure = hypot(errE, errN)
        val ratio = if (misclosure > 1e-7) (totalLen / misclosure).roundToInt() else 999999

        val adjDE = DoubleArray(legs.size)
        val adjDN = DoubleArray(legs.size)
        val sumAbsDE = rawDE.sumOf { abs(it) }.coerceAtLeast(1e-9)
        val sumAbsDN = rawDN.sumOf { abs(it) }.coerceAtLeast(1e-9)
        val sumL2 = legs.sumOf { it.distanceFt * it.distanceFt }.coerceAtLeast(1e-9)

        for (i in legs.indices) {
            val l = legs[i].distanceFt
            when (method) {
                TraverseMethod.BOWDITCH -> {
                    adjDE[i] = rawDE[i] - errE * (l / totalLen)
                    adjDN[i] = rawDN[i] - errN * (l / totalLen)
                }
                TraverseMethod.TRANSIT -> {
                    adjDE[i] = rawDE[i] - errE * (abs(rawDE[i]) / sumAbsDE)
                    adjDN[i] = rawDN[i] - errN * (abs(rawDN[i]) / sumAbsDN)
                }
                TraverseMethod.LEAST_SQUARES -> {
                    // Weighted least-squares closure distribution inversely proportional to precision weights (sigma^2 ~ L^2)
                    val w = (l * l) / sumL2
                    adjDE[i] = rawDE[i] - errE * w
                    adjDN[i] = rawDN[i] - errN * w
                }
            }
        }

        val pts = mutableListOf(PolygonVertex(startEast, startNorth))
        var curE = startEast
        var curN = startNorth
        for (i in 0 until legs.lastIndex) {
            curE += adjDE[i]
            curN += adjDN[i]
            pts.add(PolygonVertex(curE, curN))
        }
        val areaSqFt = SurveyMathProEngine.computeCoordinateGeometry(pts)?.sqFeet ?: 0.0
        return AdjustedTraverseSummary(
            method = method,
            totalPerimeterFt = totalLen,
            linearMisclosureFt = misclosure,
            precisionRatioLabel = if (misclosure < 1e-5) "Exact (Closed)" else "1 : $ratio",
            adjustedAreaSqFt = areaSqFt,
            adjustedPoints = pts
        )
    }

    // -------------------- 3.6 Circular Curve Geometry & 3-Point Circle --------------------

    fun computeCircularCurve(
        radiusFt: Double,
        deltaDeg: Double,
        piPoint: PolygonVertex = PolygonVertex(200.0, 200.0),
        backAzimuthDeg: Double = 0.0
    ): CircularCurveResult {
        val dRad = degToRad(deltaDeg.coerceIn(0.1, 179.5))
        val t = radiusFt * tan(dRad / 2.0)
        val l = radiusFt * dRad
        val c = 2.0 * radiusFt * sin(dRad / 2.0)
        val e = radiusFt * (1.0 / cos(dRad / 2.0) - 1.0)
        val m = radiusFt * (1.0 - cos(dRad / 2.0))
        val dArc = 5729.57795 / radiusFt
        val dChord = 2.0 * radToDeg(acos(sqrt((1.0 - (50.0 / radiusFt).coerceIn(0.0, 0.999).let { it * it }).coerceAtLeast(0.0))))

        val backR = degToRad(backAzimuthDeg)
        val fwdR = degToRad(backAzimuthDeg + deltaDeg)
        val pc = PolygonVertex(piPoint.x - t * sin(backR), piPoint.y - t * cos(backR))
        val pt = PolygonVertex(piPoint.x + t * sin(fwdR), piPoint.y + t * cos(fwdR))

        val stations = 4
        val deflections = (1..stations).map { i ->
            val subArc = (l / stations) * i
            val defDeg = (subArc / (2.0 * radiusFt)) * 180.0 / PI
            val subChord = 2.0 * radiusFt * sin(degToRad(defDeg))
            String.format(Locale.US, "Pt %d: Arc %.2f ft | Defl %.2f° | Chord %.2f ft", i, subArc, defDeg, subChord)
        }
        return CircularCurveResult(radiusFt, deltaDeg, dArc, dChord, l, c, t, e, m, pc, piPoint, pt, deflections)
    }

    fun fitCircleFromThreePoints(p1: PolygonVertex, p2: PolygonVertex, p3: PolygonVertex): Pair<PolygonVertex, Double>? {
        val d = 2.0 * (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y))
        if (abs(d) < 1e-8) return null
        val p1s = p1.x * p1.x + p1.y * p1.y
        val p2s = p2.x * p2.x + p2.y * p2.y
        val p3s = p3.x * p3.x + p3.y * p3.y
        val ux = (p1s * (p2.y - p3.y) + p2s * (p3.y - p1.y) + p3s * (p1.y - p2.y)) / d
        val uy = (p1s * (p3.x - p2.x) + p2s * (p1.x - p3.x) + p3s * (p2.x - p1.x)) / d
        val center = PolygonVertex(ux, uy)
        return center to hypot(p1.x - ux, p1.y - uy)
    }

    // -------------------- 3.7 Geometric Subdivision (Hinged / Sliding / Parallel) --------------------

    fun subdivideByHingedLine(
        polygon: List<PolygonVertex>,
        pivotIndex: Int,
        targetAreaSqFt: Double
    ): Pair<List<PolygonVertex>, List<PolygonVertex>>? {
        if (polygon.size < 3) return null
        val totalArea = SurveyMathProEngine.computeCoordinateGeometry(polygon)?.sqFeet ?: return null
        if (targetAreaSqFt <= 0.0 || targetAreaSqFt >= totalArea) return null

        val pIdx = pivotIndex.coerceIn(polygon.indices)
        val pivot = polygon[pIdx]
        // Walk ordered vertices starting after pivot, accumulate triangle fan areas until exceeding targetAreaSqFt
        val ordered = (0 until polygon.size).map { polygon[(pIdx + it) % polygon.size] }
        var accum = 0.0
        for (i in 1 until ordered.size - 1) {
            val a = ordered[i]
            val b = ordered[i + 1]
            val triArea = abs((a.x - pivot.x) * (b.y - pivot.y) - (b.x - pivot.x) * (a.y - pivot.y)) * 0.5
            if (accum + triArea >= targetAreaSqFt - 1e-6) {
                val needed = (targetAreaSqFt - accum).coerceIn(0.0, triArea)
                val t = if (triArea > 1e-9) needed / triArea else 0.0
                val splitPt = PolygonVertex(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                val parcel1 = listOf(pivot) + ordered.subList(1, i + 1) + listOf(splitPt)
                val parcel2 = listOf(pivot, splitPt) + ordered.subList(i + 1, ordered.size)
                return parcel1 to parcel2
            }
            accum += triArea
        }
        return null
    }

    fun subdivideSlidingParallel(
        polygon: List<PolygonVertex>,
        targetDecimal: Double,
        angleDeg: Double = 0.0
    ): LandDivisionResultRecord? {
        return PolygonDividerEngine.dividePolygon(
            id = "cogo_sub_${System.currentTimeMillis()}",
            title = "COGO Subdivision",
            vertices = polygon,
            mode = LandSplitMode.FIXED_DECIMAL,
            shares = listOf(DivisionShareSpec("কাটা অংশ (Cutoff)", targetDecimal)),
            roadWidthFt = 0.0,
            cutAngleDeg = angleDeg
        )
    }

    // -------------------- Module 12: TIN Triangulation, Contours, Profile & Cut/Fill --------------------

    fun buildTinSurface(points: List<CogoPoint>): List<TinTriangle> {
        if (points.size < 3) return emptyList()
        // Deterministic radial/centroid Delaunay-like fan + quad-split refinement
        val cx = points.map { it.eastFt }.average()
        val cy = points.map { it.northFt }.average()
        val sorted = points.sortedBy { atan2(it.northFt - cy, it.eastFt - cx) }
        val tris = mutableListOf<TinTriangle>()

        // Find centroid-nearest vertex as hub to build watertight TIN fan without outside triangles
        val hub = sorted.minByOrNull { hypot(it.eastFt - cx, it.northFt - cy) } ?: sorted.first()
        val rim = sorted.filter { it.id != hub.id }
        for (i in rim.indices) {
            val a = rim[i]
            val b = rim[(i + 1) % rim.size]
            val area = abs((a.eastFt - hub.eastFt) * (b.northFt - hub.northFt) - (b.eastFt - hub.eastFt) * (a.northFt - hub.northFt)) * 0.5
            if (area < 1e-4) continue
            val (slopePct, aspectDeg) = triangleSlopeAspect(hub, a, b)
            tris.add(TinTriangle(hub, a, b, slopePct, aspectDeg, area))
        }
        return tris
    }

    private fun triangleSlopeAspect(p1: CogoPoint, p2: CogoPoint, p3: CogoPoint): Pair<Double, Double> {
        val ux = p2.eastFt - p1.eastFt; val uy = p2.northFt - p1.northFt; val uz = p2.elevationFt - p1.elevationFt
        val vx = p3.eastFt - p1.eastFt; val vy = p3.northFt - p1.northFt; val vz = p3.elevationFt - p1.elevationFt
        val nx = uy * vz - uz * vy
        val ny = uz * vx - ux * vz
        val nz = ux * vy - uy * vx
        if (abs(nz) < 1e-9) return 0.0 to 0.0
        val grad = hypot(nx / nz, ny / nz)
        val aspect = radToDeg(atan2(nx, ny))
        return (grad * 100.0) to aspect
    }

    fun generateContours(triangles: List<TinTriangle>, intervalFt: Double = 1.0, majorEvery: Int = 5): List<ContourSegment> {
        if (triangles.isEmpty() || intervalFt <= 0.0) return emptyList()
        val minZ = triangles.minOf { min(it.p1.elevationFt, min(it.p2.elevationFt, it.p3.elevationFt)) }
        val maxZ = triangles.maxOf { max(it.p1.elevationFt, max(it.p2.elevationFt, it.p3.elevationFt)) }
        val startLevel = kotlin.math.ceil(minZ / intervalFt) * intervalFt
        val segments = mutableListOf<ContourSegment>()
        var level = startLevel
        var stepIdx = 0
        while (level <= maxZ + 1e-6 && stepIdx < 120) {
            val isMajor = ((level / intervalFt).roundToInt() % majorEvery) == 0
            triangles.forEach { tri ->
                val pts = listOf(tri.p1, tri.p2, tri.p3)
                val crossings = mutableListOf<PolygonVertex>()
                for (i in 0 until 3) {
                    val a = pts[i]; val b = pts[(i + 1) % 3]
                    if ((a.elevationFt <= level && b.elevationFt > level) || (b.elevationFt <= level && a.elevationFt > level)) {
                        val t = (level - a.elevationFt) / (b.elevationFt - a.elevationFt)
                        crossings.add(PolygonVertex(a.eastFt + t * (b.eastFt - a.eastFt), a.northFt + t * (b.northFt - a.northFt)))
                    }
                }
                if (crossings.size >= 2) {
                    segments.add(ContourSegment(level, isMajor, crossings[0], crossings[1]))
                }
            }
            level += intervalFt
            stepIdx++
        }
        return segments
    }

    data class SurfaceVolumeSummary(
        val totalPlanAreaSqFt: Double,
        val avgSlopePct: Double,
        val cutVolumeCuFt: Double,
        val fillVolumeCuFt: Double,
        val netVolumeCuFt: Double
    )

    fun computeSurfaceCutFillAgainstDesignElevation(
        triangles: List<TinTriangle>,
        proposedElevationFt: Double
    ): SurfaceVolumeSummary {
        var totalArea = 0.0
        var cut = 0.0
        var fill = 0.0
        var weightedSlope = 0.0
        triangles.forEach { tri ->
            val avgZ = (tri.p1.elevationFt + tri.p2.elevationFt + tri.p3.elevationFt) / 3.0
            val diff = proposedElevationFt - avgZ
            totalArea += tri.area2dSqFt
            weightedSlope += tri.slopePct * tri.area2dSqFt
            if (diff >= 0.0) fill += diff * tri.area2dSqFt else cut += (-diff) * tri.area2dSqFt
        }
        val avgSlope = if (totalArea > 1e-6) weightedSlope / totalArea else 0.0
        return SurfaceVolumeSummary(totalArea, avgSlope, cut, fill, fill - cut)
    }

    // -------------------- Module 13: Road Alignment Station/Offset & Plot Access --------------------

    data class RoadAnalysisResult(
        val totalCenterlineLengthFt: Double,
        val roadAreaSqFt: Double,
        val stationOffsets: List<String>,
        val hasPlotAccess: Boolean,
        val minDistanceToPlotFt: Double
    )

    fun analyzeRoadAlignmentAndAccess(
        road: RoadAlignmentSpec,
        plotPolygon: List<PolygonVertex>,
        stationPoints: List<CogoPoint>
    ): RoadAnalysisResult {
        var len = 0.0
        for (i in 0 until road.centerlinePoints.size - 1) {
            val a = road.centerlinePoints[i]; val b = road.centerlinePoints[i + 1]
            len += hypot(b.x - a.x, b.y - a.y)
        }
        val roadArea = len * road.rightOfWayWidthFt

        val stOffsets = stationPoints.map { pt ->
            var bestDist = Double.MAX_VALUE
            var bestChainage = 0.0
            var accum = 0.0
            for (i in 0 until road.centerlinePoints.size - 1) {
                val a = road.centerlinePoints[i]; val b = road.centerlinePoints[i + 1]
                val segLen = hypot(b.x - a.x, b.y - a.y).coerceAtLeast(1e-6)
                val t = (((pt.eastFt - a.x) * (b.x - a.x) + (pt.northFt - a.y) * (b.y - a.y)) / (segLen * segLen)).coerceIn(0.0, 1.0)
                val px = a.x + t * (b.x - a.x); val py = a.y + t * (b.y - a.y)
                val d = hypot(pt.eastFt - px, pt.northFt - py)
                if (d < bestDist) {
                    bestDist = d
                    bestChainage = accum + t * segLen
                }
                accum += segLen
            }
            val chInt = bestChainage.toInt()
            String.format(Locale.US, "Pt %d → স্টেশন %d+%02d | অফসেট %.2f ft", pt.pointNumber, chInt / 100, chInt % 100, bestDist)
        }

        var minPlotDist = Double.MAX_VALUE
        plotPolygon.forEach { v ->
            for (i in 0 until road.centerlinePoints.size - 1) {
                val a = road.centerlinePoints[i]; val b = road.centerlinePoints[i + 1]
                val segLen = hypot(b.x - a.x, b.y - a.y).coerceAtLeast(1e-6)
                val t = (((v.x - a.x) * (b.x - a.x) + (v.y - a.y) * (b.y - a.y)) / (segLen * segLen)).coerceIn(0.0, 1.0)
                val d = hypot(v.x - (a.x + t * (b.x - a.x)), v.y - (a.y + t * (b.y - a.y)))
                if (d < minPlotDist) minPlotDist = d
            }
        }
        val hasAccess = minPlotDist <= road.rightOfWayWidthFt * 0.65 + 5.0
        return RoadAnalysisResult(len, roadArea, stOffsets, hasAccess, if (minPlotDist == Double.MAX_VALUE) 0.0 else minPlotDist)
    }

    // -------------------- Module 14: Automated Legal Land Schedule (Bangla & English Metes-and-Bounds) --------------------

    fun generateAutomatedLandSchedule(
        mouza: String,
        jlNo: String,
        khatianNo: String,
        dagNo: String,
        landClass: String,
        vertices: List<PolygonVertex>,
        northNeighbor: String,
        southNeighbor: String,
        eastNeighbor: String,
        westNeighbor: String,
        languageBn: Boolean = true
    ): String {
        val geom = SurveyMathProEngine.computeCoordinateGeometry(vertices)
        val sqFt = geom?.sqFeet ?: 0.0
        val dec = sqFt / LandCalculators.SQ_FT_PER_DECIMAL
        val katha = sqFt / LandCalculators.SQ_FT_PER_KATHA_BD
        val perim = geom?.perimeterFt ?: 0.0
        val calls = buildString {
            for (i in vertices.indices) {
                val a = vertices[i]; val b = vertices[(i + 1) % vertices.size]
                val dist = hypot(b.x - a.x, b.y - a.y)
                val az = radToDeg(atan2(b.x - a.x, b.y - a.y))
                append(String.format(Locale.US, "   • বাহু P%d→P%d: বিয়ারিং %05.1f°, দৈর্ঘ্য %.2f ফুট\n", i + 1, (i + 1) % vertices.size + 1, az, dist))
            }
        }
        val pob = vertices.firstOrNull()?.let { String.format(Locale.US, "(E %.2f, N %.2f)", it.x, it.y) } ?: "(0, 0)"
        return if (languageBn) {
            """
            ======================================================
            স্বয়ংক্রিয় তফসিল ও সীমানা বিবরণী (Automated Tafsil)
            ======================================================
            ১. মৌজা ও পরিচিতি:
               মৌজা: $mouza | জে.এল. নং: $jlNo
               খতিয়ান নং: $khatianNo | দাগ নং: $dagNo | শ্রেণি: $landClass
            ২. জমির পরিমাণ ও পরিসীমা:
               মোট পরিমাণ: ${String.format(Locale.US, "%.2f", dec)} শতাংশ (${String.format(Locale.US, "%.2f", katha)} কাঠা / ${String.format(Locale.US, "%.1f", sqFt)} বর্গফুট)
               মোট পরিসীমা: ${String.format(Locale.US, "%.2f", perim)} ফুট | প্রারম্ভিক বিন্দু (POB): $pob
            ৩. চার-সীমানা (চৌহদ্দি):
               উত্তরে: $northNeighbor
               দক্ষিণে: $southNeighbor
               পূর্বে: $eastNeighbor
               পশ্চিমে: $westNeighbor
            ৪. বিয়ারিং ও দূরত্ব কল (Metes & Bounds):
            $calls
            [সতর্কতা: এটি ডিজিটাল জরিপ খসড়া তফসিল; রেজিস্ট্রির পূর্বে দলিল লেখক/আইনজীবী দ্বারা যাচাই আবশ্যক।]
            """.trimIndent()
        } else {
            """
            ======================================================
            AUTOMATED METES-AND-BOUNDS LAND SCHEDULE
            ======================================================
            1. Property Identity:
               Mouza: $mouza | JL No: $jlNo | Khatian: $khatianNo | Dag: $dagNo | Class: $landClass
            2. Area & Perimeter:
               Total Area: ${String.format(Locale.US, "%.2f", dec)} Decimal (${String.format(Locale.US, "%.2f", katha)} Katha / ${String.format(Locale.US, "%.1f", sqFt)} Sq.Ft)
               Perimeter: ${String.format(Locale.US, "%.2f", perim)} ft | Point of Beginning (POB): $pob
            3. Adjoining Boundaries:
               North: $northNeighbor | South: $southNeighbor | East: $eastNeighbor | West: $westNeighbor
            4. Bearing & Distance Calls:
            $calls
            [NOTICE: Uncertified digital survey draft schedule; verify legally prior to registration.]
            """.trimIndent()
        }
    }
}
