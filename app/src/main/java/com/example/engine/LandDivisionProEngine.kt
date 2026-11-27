package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.example.model.CompassAllocationDirection
import com.example.model.DivisionGeometryCutMode
import com.example.model.DivisionShareSpec
import com.example.model.HoleCutoutPolygon
import com.example.model.ParcelFourBoundaries
import com.example.model.PlotPlannerOptionConfig
import com.example.model.PlotPlannerOptionSummary
import com.example.model.PolygonVertex
import com.example.model.ProDividedSubParcel
import com.example.model.ProDivisionReport
import com.example.model.QuadMasterResult
import com.example.model.QuadMasterSpec
import com.example.model.RegionalUnitProfile
import com.example.model.RoadFrontageComparisonItem
import com.example.model.TafsilAllocationRow
import com.example.model.TafsilDagInput
import com.example.model.TafsilDivisionReport
import com.example.model.TafsilOwnerShareSpec
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object LandDivisionProEngine {

    private val PARCEL_COLORS = listOf(
        0xFF0F6B54L,
        0xFF1A73E8L,
        0xFFD49A17L,
        0xFF8E24AAL,
        0xFFD81B60L,
        0xFF00897BL,
        0xFF5E35B1L,
        0xFFE65100L
    )

    // ==================== 1. EXACT POLYGON + HOLES GEOMETRY ====================

    fun signedArea(vertices: List<PolygonVertex>): Double {
        if (vertices.size < 3) return 0.0
        var sum = 0.0
        for (i in vertices.indices) {
            val a = vertices[i]
            val b = vertices[(i + 1) % vertices.size]
            sum += (a.x * b.y) - (b.x * a.y)
        }
        return sum * 0.5
    }

    fun ensureCounterClockwise(vertices: List<PolygonVertex>): List<PolygonVertex> {
        if (vertices.size < 3) return vertices
        return if (signedArea(vertices) < 0.0) vertices.reversed() else vertices
    }

    fun polygonCentroid(vertices: List<PolygonVertex>): PolygonVertex {
        if (vertices.isEmpty()) return PolygonVertex(0.0, 0.0)
        var signed = 0.0
        var cx = 0.0
        var cy = 0.0
        for (i in vertices.indices) {
            val p1 = vertices[i]
            val p2 = vertices[(i + 1) % vertices.size]
            val cross = (p1.x * p2.y) - (p2.x * p1.y)
            signed += cross
            cx += (p1.x + p2.x) * cross
            cy += (p1.y + p2.y) * cross
        }
        signed *= 0.5
        return if (abs(signed) > 1e-6) {
            PolygonVertex(cx / (6.0 * signed), cy / (6.0 * signed))
        } else {
            PolygonVertex(vertices.map { it.x }.average(), vertices.map { it.y }.average())
        }
    }

    fun pointInPolygon(pt: PolygonVertex, poly: List<PolygonVertex>): Boolean {
        if (poly.size < 3) return false
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val pi = poly[i]
            val pj = poly[j]
            val intersect = ((pi.y > pt.y) != (pj.y > pt.y)) &&
                (pt.x < (pj.x - pi.x) * (pt.y - pi.y) / ((pj.y - pi.y).takeIf { abs(it) > 1e-12 } ?: 1e-12) + pi.x)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * Triangulates a simple polygon (convex or concave) via ear clipping.
     */
    fun triangulatePolygon(vertices: List<PolygonVertex>): List<List<PolygonVertex>> {
        val pts = ensureCounterClockwise(vertices).toMutableList()
        if (pts.size < 3) return emptyList()
        if (pts.size == 3) return listOf(pts.toList())

        fun cross(a: PolygonVertex, b: PolygonVertex, c: PolygonVertex): Double =
            (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

        fun pointInTriangle(p: PolygonVertex, a: PolygonVertex, b: PolygonVertex, c: PolygonVertex): Boolean {
            val c1 = cross(a, b, p)
            val c2 = cross(b, c, p)
            val c3 = cross(c, a, p)
            return c1 >= -1e-9 && c2 >= -1e-9 && c3 >= -1e-9
        }

        val triangles = mutableListOf<List<PolygonVertex>>()
        var guard = 0
        while (pts.size > 3 && guard < pts.size * pts.size + 10) {
            guard++
            var earFound = false
            val n = pts.size
            for (i in 0 until n) {
                val prev = pts[(i + n - 1) % n]
                val curr = pts[i]
                val next = pts[(i + 1) % n]
                if (cross(prev, curr, next) <= 1e-9) continue
                var containsOther = false
                for (k in 0 until n) {
                    if (k == (i + n - 1) % n || k == i || k == (i + 1) % n) continue
                    if (pointInTriangle(pts[k], prev, curr, next)) {
                        containsOther = true
                        break
                    }
                }
                if (!containsOther) {
                    triangles.add(listOf(prev, curr, next))
                    pts.removeAt(i)
                    earFound = true
                    break
                }
            }
            if (!earFound) break
        }
        if (pts.size == 3) triangles.add(pts.toList())
        return triangles
    }

    /**
     * Exact analytical intersection of two convex CCW polygons via Sutherland-Hodgman edge clipping.
     */
    private fun intersectConvexPolygons(subject: List<PolygonVertex>, clipper: List<PolygonVertex>): List<PolygonVertex> {
        var output = ensureCounterClockwise(subject)
        val clipCCW = ensureCounterClockwise(clipper)
        if (output.size < 3 || clipCCW.size < 3) return emptyList()

        for (i in clipCCW.indices) {
            val a = clipCCW[i]
            val b = clipCCW[(i + 1) % clipCCW.size]
            val ex = b.x - a.x
            val ey = b.y - a.y
            val len = hypot(ex, ey)
            if (len < 1e-12) continue
            // Inward left-hand normal of CCW edge (a -> b): (-ey/len, ex/len)
            val nx = -ey / len
            val ny = ex / len
            val limit = a.x * nx + a.y * ny
            output = PolygonDividerEngine.clipPolygonByHalfPlane(output, nx, ny, limit, keepGreater = true)
            if (output.size < 3) return emptyList()
        }
        return output
    }

    /**
     * Exact analytical intersection area between any two simple polygons (convex or concave).
     */
    fun exactIntersectionAreaSqFt(polyA: List<PolygonVertex>, polyB: List<PolygonVertex>): Double {
        if (polyA.size < 3 || polyB.size < 3) return 0.0
        val minAx = polyA.minOf { it.x }; val maxAx = polyA.maxOf { it.x }
        val minAy = polyA.minOf { it.y }; val maxAy = polyA.maxOf { it.y }
        val minBx = polyB.minOf { it.x }; val maxBx = polyB.maxOf { it.x }
        val minBy = polyB.minOf { it.y }; val maxBy = polyB.maxOf { it.y }
        if (maxAx <= minBx || maxBx <= minAx || maxAy <= minBy || maxBy <= minAy) return 0.0

        val trisA = triangulatePolygon(polyA)
        val trisB = triangulatePolygon(polyB)
        var sum = 0.0
        for (ta in trisA) {
            for (tb in trisB) {
                val inter = intersectConvexPolygons(ta, tb)
                if (inter.size >= 3) {
                    sum += PolygonDividerEngine.polygonAreaSqFt(inter)
                }
            }
        }
        return sum
    }

    fun netAreaWithHolesSqFt(outer: List<PolygonVertex>, holes: List<HoleCutoutPolygon>): Pair<Double, Double> {
        val gross = PolygonDividerEngine.polygonAreaSqFt(outer)
        if (gross <= 1e-6 || holes.isEmpty()) return gross to 0.0
        val excluded = holes.sumOf { exactIntersectionAreaSqFt(outer, it.vertices) }.coerceAtMost(gross)
        return (gross - excluded).coerceAtLeast(0.0) to excluded
    }

    // ==================== 2. HALF-PLANE, RADIAL, THROUGH-POINT & DRAG LINE SPLITS ====================

    private fun bisectNetAreaParallel(
        poly: List<PolygonVertex>,
        holes: List<HoleCutoutPolygon>,
        targetNetAreaSqFt: Double,
        cutAngleDeg: Double
    ): Pair<List<PolygonVertex>, List<PolygonVertex>> {
        val (totalNet, _) = netAreaWithHolesSqFt(poly, holes)
        if (poly.size < 3 || totalNet <= 1e-6) return emptyList<PolygonVertex>() to emptyList()
        if (targetNetAreaSqFt <= 1e-6) return emptyList<PolygonVertex>() to poly
        if (targetNetAreaSqFt >= totalNet - 1e-6) return poly to emptyList()

        val rad = Math.toRadians(cutAngleDeg)
        val nx = cos(rad)
        val ny = sin(rad)
        val projs = poly.map { it.x * nx + it.y * ny }
        var lo = projs.min()
        var hi = projs.max()
        repeat(56) {
            val mid = (lo + hi) * 0.5
            val piece = PolygonDividerEngine.clipPolygonByHalfPlane(poly, nx, ny, mid, keepGreater = false)
            val (pieceNet, _) = netAreaWithHolesSqFt(piece, holes)
            if (pieceNet < targetNetAreaSqFt) lo = mid else hi = mid
        }
        val splitLimit = (lo + hi) * 0.5
        val left = PolygonDividerEngine.clipPolygonByHalfPlane(poly, nx, ny, splitLimit, keepGreater = false)
        val right = PolygonDividerEngine.clipPolygonByHalfPlane(poly, nx, ny, splitLimit, keepGreater = true)
        return left to right
    }

    /**
     * Cuts a polygon radially from a pivot point `pivot` by sweeping cut line angle `theta`.
     */
    fun bisectPolygonThroughPoint(
        poly: List<PolygonVertex>,
        holes: List<HoleCutoutPolygon>,
        pivot: PolygonVertex,
        targetNetAreaSqFt: Double
    ): Pair<List<PolygonVertex>, List<PolygonVertex>> {
        val (totalNet, _) = netAreaWithHolesSqFt(poly, holes)
        if (poly.size < 3 || totalNet <= 1e-6) return emptyList<PolygonVertex>() to emptyList()
        if (targetNetAreaSqFt <= 1e-6) return emptyList<PolygonVertex>() to poly
        if (targetNetAreaSqFt >= totalNet - 1e-6) return poly to emptyList()

        fun cutAtAngle(deg: Double): Pair<List<PolygonVertex>, List<PolygonVertex>> {
            val rad = Math.toRadians(deg)
            val nx = cos(rad)
            val ny = sin(rad)
            val limit = pivot.x * nx + pivot.y * ny
            val a = PolygonDividerEngine.clipPolygonByHalfPlane(poly, nx, ny, limit, keepGreater = false)
            val b = PolygonDividerEngine.clipPolygonByHalfPlane(poly, nx, ny, limit, keepGreater = true)
            return a to b
        }

        // Sample 72 angles (every 5 degrees) to locate bracket crossing targetNetAreaSqFt
        var bestLo = 0.0
        var bestHi = 180.0
        var bestErr = Double.MAX_VALUE
        var prevA = 0.0
        var prevArea = netAreaWithHolesSqFt(cutAtAngle(0.0).first, holes).first
        var foundBracket = false

        for (step in 1..72) {
            val ang = step * 5.0
            val (pA, _) = cutAtAngle(ang)
            val areaA = netAreaWithHolesSqFt(pA, holes).first
            val err = abs(areaA - targetNetAreaSqFt)
            if (err < bestErr) {
                bestErr = err
                bestLo = ang
                bestHi = ang
            }
            if ((prevArea - targetNetAreaSqFt) * (areaA - targetNetAreaSqFt) <= 0.0) {
                bestLo = prevA
                bestHi = ang
                foundBracket = true
                break
            }
            prevA = ang
            prevArea = areaA
        }

        if (!foundBracket) {
            return cutAtAngle(bestLo)
        }

        val startIncreasing = netAreaWithHolesSqFt(cutAtAngle(bestLo).first, holes).first <
            netAreaWithHolesSqFt(cutAtAngle(bestHi).first, holes).first
        var lo = bestLo
        var hi = bestHi
        repeat(46) {
            val mid = (lo + hi) * 0.5
            val midArea = netAreaWithHolesSqFt(cutAtAngle(mid).first, holes).first
            if (startIncreasing) {
                if (midArea < targetNetAreaSqFt) lo = mid else hi = mid
            } else {
                if (midArea > targetNetAreaSqFt) lo = mid else hi = mid
            }
        }
        return cutAtAngle((lo + hi) * 0.5)
    }

    /**
     * Live Interactive Drag-Line Split: divides `vertices` directly along the user's line segment (p1 -> p2).
     */
    fun splitPolygonByLineSegment(
        vertices: List<PolygonVertex>,
        holes: List<HoleCutoutPolygon> = emptyList(),
        lineStart: PolygonVertex,
        lineEnd: PolygonVertex,
        labelA: String = "অংশ ক (Side A)",
        labelB: String = "অংশ খ (Side B)",
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): List<ProDividedSubParcel> {
        if (vertices.size < 3 || PolygonDividerEngine.hasSelfIntersection(vertices)) return emptyList()
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val len = hypot(dx, dy)
        if (len < 1e-6) return emptyList()
        val nx = -dy / len
        val ny = dx / len
        val limit = lineStart.x * nx + lineStart.y * ny

        val partA = PolygonDividerEngine.clipPolygonByHalfPlane(vertices, nx, ny, limit, keepGreater = false)
        val partB = PolygonDividerEngine.clipPolygonByHalfPlane(vertices, nx, ny, limit, keepGreater = true)

        val out = mutableListOf<ProDividedSubParcel>()
        listOf(labelA to partA, labelB to partB).forEachIndexed { idx, (name, poly) ->
            if (poly.size >= 3) {
                val (net, excl) = netAreaWithHolesSqFt(poly, holes)
                val gross = PolygonDividerEngine.polygonAreaSqFt(poly)
                val edges = PolygonDividerEngine.edgeLengths(poly)
                out.add(
                    ProDividedSubParcel(
                        index = idx + 1,
                        ownerName = name,
                        grossAreaSqFt = gross,
                        excludedHoleSqFt = excl,
                        netAreaSqFt = net,
                        netDecimal = net / LandCalculators.SQ_FT_PER_DECIMAL,
                        netKatha = net / unitProfile.kathaSqFt,
                        perimeterFt = edges.sum(),
                        edgeLengthsFt = edges,
                        roadFrontageFt = 0.0,
                        vertices = poly,
                        colorArgb = PARCEL_COLORS[idx % PARCEL_COLORS.size]
                    )
                )
            }
        }
        return attachAutoFourBoundaries(out, hasRoadStrip = false)
    }

    // ==================== 3. FULL PRO DIVISION SUITE (PARALLEL / RADIAL / FRONTAGE / VALUE / HOLES) ====================

    fun dividePolygonPro(
        id: String,
        title: String,
        vertices: List<PolygonVertex>,
        holes: List<HoleCutoutPolygon> = emptyList(),
        cutMode: DivisionGeometryCutMode = DivisionGeometryCutMode.PARALLEL_LINE,
        splitMode: com.example.model.LandSplitMode = com.example.model.LandSplitMode.EQUAL,
        shares: List<DivisionShareSpec>,
        directions: List<CompassAllocationDirection> = emptyList(),
        valueWeights: List<Double> = emptyList(),
        frontRowValueMultiplier: Double = 1.35,
        roadWidthFt: Double = 0.0,
        roadSideAngleDeg: Double = 90.0,
        cutAngleDeg: Double = 0.0,
        pivotCornerIdx: Int = 0,
        customPivot: PolygonVertex? = null,
        fixedFrontagesFt: List<Double> = emptyList(),
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): ProDivisionReport? {
        if (vertices.size < 3 || PolygonDividerEngine.hasSelfIntersection(vertices) || shares.isEmpty()) return null
        val (grossOuterNet, totalHolesInOuter) = netAreaWithHolesSqFt(vertices, holes)
        val grossOuterArea = PolygonDividerEngine.polygonAreaSqFt(vertices)
        if (grossOuterNet <= 1e-3) return null

        // 1) Carve Road Corridor
        val rRad = Math.toRadians(roadSideAngleDeg)
        val rnx = cos(rRad)
        val rny = sin(rRad)
        var usablePoly = vertices
        var roadPoly: List<PolygonVertex> = emptyList()
        if (roadWidthFt > 0.0) {
            val minProj = vertices.minOf { it.x * rnx + it.y * rny }
            val maxProj = vertices.maxOf { it.x * rnx + it.y * rny }
            if (maxProj - minProj > roadWidthFt + 1.0) {
                val limit = minProj + roadWidthFt
                roadPoly = PolygonDividerEngine.clipPolygonByHalfPlane(vertices, rnx, rny, limit, keepGreater = false)
                usablePoly = PolygonDividerEngine.clipPolygonByHalfPlane(vertices, rnx, rny, limit, keepGreater = true)
            }
        }

        val (usableNetArea, _) = netAreaWithHolesSqFt(usablePoly, holes)
        if (usableNetArea <= 1e-3) return null
        val roadArea = PolygonDividerEngine.polygonAreaSqFt(roadPoly)

        // Road contact edge projection line for frontage reporting
        val roadContactLimit = usablePoly.minOf { it.x * rnx + it.y * rny }
        val frontageBefore = computeRoadContactLength(usablePoly, rnx, rny, roadContactLimit)

        // Effective cut angle (respects directional allocation if requested)
        val effectiveCutAngle = when {
            cutMode == DivisionGeometryCutMode.CORNER_FIXED_AREA -> 45.0
            directions.any { it == CompassAllocationDirection.NORTH || it == CompassAllocationDirection.SOUTH } -> 90.0
            else -> cutAngleDeg
        }

        // Order shares by direction preference if set
        val indexedOrder = shares.indices.sortedBy { idx ->
            when (directions.getOrNull(idx) ?: CompassAllocationDirection.AUTO) {
                CompassAllocationDirection.WEST, CompassAllocationDirection.SOUTH, CompassAllocationDirection.ROAD_FRONT -> 0
                CompassAllocationDirection.AUTO -> idx + 1
                CompassAllocationDirection.EAST, CompassAllocationDirection.NORTH -> 100 + idx
            }
        }

        // Compute target SqFt per share (incorporating Value-Weighted multiplier if active)
        val rawTargets: List<Double> = when (splitMode) {
            com.example.model.LandSplitMode.EQUAL -> {
                val weights = shares.indices.map { i ->
                    val w = shares[i].value.coerceAtLeast(1.0)
                    if (cutMode == DivisionGeometryCutMode.VALUE_WEIGHTED) {
                        val vw = valueWeights.getOrNull(i)?.coerceAtLeast(0.2) ?: (if (i == 0) frontRowValueMultiplier.coerceAtLeast(0.5) else 1.0)
                        w / vw
                    } else w
                }
                val sumW = weights.sum().coerceAtLeast(1e-6)
                weights.map { usableNetArea * (it / sumW) }
            }
            com.example.model.LandSplitMode.PERCENTAGE -> {
                val sumPct = shares.sumOf { it.value.coerceAtLeast(0.0) }
                val scale = if (sumPct > 100.0) 100.0 / sumPct else 1.0
                shares.map { usableNetArea * ((it.value.coerceAtLeast(0.0) * scale) / 100.0) }
            }
            com.example.model.LandSplitMode.FIXED_DECIMAL -> {
                shares.map { it.value.coerceAtLeast(0.0) * LandCalculators.SQ_FT_PER_DECIMAL }
            }
            com.example.model.LandSplitMode.FIXED_KATHA -> {
                shares.map { it.value.coerceAtLeast(0.0) * unitProfile.kathaSqFt }
            }
        }

        val parcels = mutableListOf<ProDividedSubParcel>()
        var remainingPoly = usablePoly
        var allocatedNetTotal = 0.0

        // Special Case: Fixed-Frontage Cuts along perpendicular to road normal
        if (cutMode == DivisionGeometryCutMode.FIXED_FRONTAGE && fixedFrontagesFt.isNotEmpty()) {
            // Tangent vector along road edge = (-rny, rnx)
            val tx = -rny
            val ty = rnx
            var currentStart = usablePoly.minOf { it.x * tx + it.y * ty }
            for ((stepIdx, origIdx) in indexedOrder.withIndex()) {
                val frontWidth = fixedFrontagesFt.getOrElse(origIdx) { fixedFrontagesFt.last() }.coerceAtLeast(1.0)
                val (remNet, _) = netAreaWithHolesSqFt(remainingPoly, holes)
                if (remNet <= 1e-4) break
                val isLast = stepIdx == indexedOrder.lastIndex
                val cutLimit = currentStart + frontWidth
                val (piece, nextRem) = if (isLast) {
                    remainingPoly to emptyList()
                } else {
                    val p = PolygonDividerEngine.clipPolygonByHalfPlane(remainingPoly, tx, ty, cutLimit, keepGreater = false)
                    val r = PolygonDividerEngine.clipPolygonByHalfPlane(remainingPoly, tx, ty, cutLimit, keepGreater = true)
                    p to r
                }
                currentStart = cutLimit
                if (piece.size >= 3) {
                    val (net, excl) = netAreaWithHolesSqFt(piece, holes)
                    if (net > 1e-4) {
                        val edges = PolygonDividerEngine.edgeLengths(piece)
                        val frontage = computeRoadContactLength(piece, rnx, rny, roadContactLimit)
                        allocatedNetTotal += net
                        parcels.add(
                            ProDividedSubParcel(
                                index = origIdx + 1,
                                ownerName = shares[origIdx].ownerName.ifBlank { "অংশ ${origIdx + 1}" },
                                grossAreaSqFt = PolygonDividerEngine.polygonAreaSqFt(piece),
                                excludedHoleSqFt = excl,
                                netAreaSqFt = net,
                                netDecimal = net / LandCalculators.SQ_FT_PER_DECIMAL,
                                netKatha = net / unitProfile.kathaSqFt,
                                perimeterFt = edges.sum(),
                                edgeLengthsFt = edges,
                                roadFrontageFt = frontage,
                                valueWeight = valueWeights.getOrNull(origIdx) ?: 1.0,
                                weightedScore = net * (valueWeights.getOrNull(origIdx) ?: 1.0),
                                allocatedDirection = directions.getOrNull(origIdx) ?: CompassAllocationDirection.AUTO,
                                vertices = piece,
                                colorArgb = PARCEL_COLORS[origIdx % PARCEL_COLORS.size]
                            )
                        )
                    }
                }
                remainingPoly = nextRem
            }
        } else {
            val pivot = when (cutMode) {
                DivisionGeometryCutMode.RADIAL_CORNER -> vertices[pivotCornerIdx.coerceIn(0, vertices.lastIndex)]
                DivisionGeometryCutMode.THROUGH_POINT -> customPivot ?: polygonCentroid(usablePoly)
                else -> polygonCentroid(usablePoly)
            }

            for ((stepIdx, origIdx) in indexedOrder.withIndex()) {
                val (remNet, _) = netAreaWithHolesSqFt(remainingPoly, holes)
                if (remNet <= 1e-4) break
                val desired = min(rawTargets[origIdx], remNet)
                if (desired <= 1e-4) continue

                val isExactLast = (stepIdx == indexedOrder.lastIndex) &&
                    (splitMode == com.example.model.LandSplitMode.EQUAL || abs(desired - remNet) < 0.05)

                val (piece, nextRem) = when {
                    isExactLast -> remainingPoly to emptyList()
                    cutMode == DivisionGeometryCutMode.RADIAL_CORNER || cutMode == DivisionGeometryCutMode.THROUGH_POINT ->
                        bisectPolygonThroughPoint(remainingPoly, holes, pivot, desired)
                    else -> bisectNetAreaParallel(remainingPoly, holes, desired, effectiveCutAngle)
                }

                if (piece.size >= 3) {
                    val (net, excl) = netAreaWithHolesSqFt(piece, holes)
                    if (net > 1e-4) {
                        val edges = PolygonDividerEngine.edgeLengths(piece)
                        val frontage = computeRoadContactLength(piece, rnx, rny, roadContactLimit)
                        val vw = valueWeights.getOrNull(origIdx)
                            ?: if (cutMode == DivisionGeometryCutMode.VALUE_WEIGHTED && frontage > 1.0) frontRowValueMultiplier else 1.0
                        allocatedNetTotal += net
                        parcels.add(
                            ProDividedSubParcel(
                                index = origIdx + 1,
                                ownerName = shares[origIdx].ownerName.ifBlank { "অংশ ${origIdx + 1}" },
                                grossAreaSqFt = PolygonDividerEngine.polygonAreaSqFt(piece),
                                excludedHoleSqFt = excl,
                                netAreaSqFt = net,
                                netDecimal = net / LandCalculators.SQ_FT_PER_DECIMAL,
                                netKatha = net / unitProfile.kathaSqFt,
                                perimeterFt = edges.sum(),
                                edgeLengthsFt = edges,
                                roadFrontageFt = frontage,
                                valueWeight = vw,
                                weightedScore = net * vw,
                                allocatedDirection = directions.getOrNull(origIdx) ?: CompassAllocationDirection.AUTO,
                                vertices = piece,
                                colorArgb = PARCEL_COLORS[origIdx % PARCEL_COLORS.size]
                            )
                        )
                    }
                }
                remainingPoly = nextRem
            }
        }

        val withBoundaries = attachAutoFourBoundaries(parcels, hasRoadStrip = roadWidthFt > 0.0)
        val (remNet, _) = netAreaWithHolesSqFt(remainingPoly, holes)
        val totalCheck = allocatedNetTotal + remNet
        val conservationErr = abs(totalCheck - usableNetArea)
        val tol = max(1.0, usableNetArea * 0.002)

        val totalFrontAfter = withBoundaries.sumOf { it.roadFrontageFt }.coerceAtLeast(1e-6)
        val frontageReport = withBoundaries.map { p ->
            RoadFrontageComparisonItem(
                parcelIndex = p.index,
                ownerName = p.ownerName,
                frontageFt = p.roadFrontageFt,
                frontageRatioPct = (p.roadFrontageFt / totalFrontAfter) * 100.0
            )
        }

        return ProDivisionReport(
            id = id,
            title = title.ifBlank { "পলিগন ভাগ-বণ্টন প্রতিবেদন" },
            cutMode = cutMode,
            grossOuterAreaSqFt = grossOuterArea,
            totalHoleExcludedSqFt = totalHolesInOuter,
            roadReservedSqFt = roadArea,
            usableNetAreaSqFt = usableNetArea,
            allocatedNetAreaSqFt = allocatedNetTotal,
            remainingNetAreaSqFt = remNet,
            conservationErrorSqFt = conservationErr,
            isValidConservation = conservationErr <= tol,
            totalRoadFrontageBeforeFt = frontageBefore,
            totalRoadFrontageAfterFt = withBoundaries.sumOf { it.roadFrontageFt },
            frontageReport = frontageReport,
            parcels = withBoundaries,
            roadPolygon = roadPoly,
            holes = holes
        )
    }

    private fun computeRoadContactLength(
        poly: List<PolygonVertex>,
        rnx: Double,
        rny: Double,
        roadLineProj: Double
    ): Double {
        if (poly.size < 2) return 0.0
        var total = 0.0
        val tol = 0.35 // within 0.35 ft of road boundary line
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val da = abs((a.x * rnx + a.y * rny) - roadLineProj)
            val db = abs((b.x * rnx + b.y * rny) - roadLineProj)
            if (da <= tol && db <= tol) {
                total += hypot(b.x - a.x, b.y - a.y)
            }
        }
        return total
    }

    fun attachAutoFourBoundaries(
        parcels: List<ProDividedSubParcel>,
        hasRoadStrip: Boolean
    ): List<ProDividedSubParcel> {
        if (parcels.isEmpty()) return parcels
        val centroids = parcels.map { polygonCentroid(it.vertices) }
        return parcels.mapIndexed { idx, p ->
            val c = centroids[idx]
            var north = "পার্শ্ববর্তী দাগ (উত্তর)"
            var south = if (hasRoadStrip && p.roadFrontageFt > 0.5) "নতুন প্রস্তাবিত রাস্তা" else "পার্শ্ববর্তী দাগ (দক্ষিণ)"
            var east = "পার্শ্ববর্তী দাগ (পূর্ব)"
            var west = "পার্শ্ববর্তী দাগ (পশ্চিম)"

            var bestN = Double.MAX_VALUE
            var bestS = Double.MAX_VALUE
            var bestE = Double.MAX_VALUE
            var bestW = Double.MAX_VALUE

            parcels.forEachIndexed { oIdx, other ->
                if (oIdx == idx) return@forEachIndexed
                val oc = centroids[oIdx]
                val dx = oc.x - c.x
                val dy = oc.y - c.y
                val dist = hypot(dx, dy)
                if (abs(dy) >= abs(dx)) {
                    if (dy > 0 && dist < bestN) {
                        bestN = dist
                        north = "${other.ownerName} (অংশ ${other.index})"
                    } else if (dy < 0 && dist < bestS) {
                        bestS = dist
                        south = "${other.ownerName} (অংশ ${other.index})"
                    }
                } else {
                    if (dx > 0 && dist < bestE) {
                        bestE = dist
                        east = "${other.ownerName} (অংশ ${other.index})"
                    } else if (dx < 0 && dist < bestW) {
                        bestW = dist
                        west = "${other.ownerName} (অংশ ${other.index})"
                    }
                }
            }
            p.copy(boundaries = ParcelFourBoundaries(north, south, east, west))
        }
    }

    // ==================== 4. QUADMASTER (4 SIDES + DIAGONAL / ANGLE SOLVER & SPLIT) ====================

    fun solveAndSplitQuad(
        spec: QuadMasterSpec,
        partsCount: Int = 2,
        parallelCutAngleDeg: Double = 0.0,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): QuadMasterResult? {
        val n = spec.northFt.coerceAtLeast(0.0)
        val s = spec.southFt.coerceAtLeast(0.0)
        val e = spec.eastFt.coerceAtLeast(0.0)
        val w = spec.westFt.coerceAtLeast(0.0)
        if (n <= 0.0 || s <= 0.0 || e <= 0.0 || w <= 0.0) return null

        val a = PolygonVertex(0.0, 0.0)
        val b = PolygonVertex(s, 0.0)
        val c: PolygonVertex
        val d: PolygonVertex

        if (spec.useDiagonalMode) {
            val ac = spec.diagonalAcFt
            if (ac <= 0.0 || s + e <= ac || s + ac <= e || e + ac <= s) return null
            if (ac + n <= w || ac + w <= n || n + w <= ac) return null

            // Angle BAC via Law of Cosines
            val cosBac = ((s * s + ac * ac - e * e) / (2.0 * s * ac)).coerceIn(-1.0, 1.0)
            val angBac = acos(cosBac)
            c = PolygonVertex(ac * cos(angBac), ac * sin(angBac))

            // Angle CAD via Law of Cosines above diagonal AC
            val cosCad = ((ac * ac + w * w - n * n) / (2.0 * ac * w)).coerceIn(-1.0, 1.0)
            val angCad = acos(cosCad)
            val angAd = angBac + angCad
            d = PolygonVertex(w * cos(angAd), w * sin(angAd))
        } else {
            val angRad = Math.toRadians(spec.cornerAngleSwDeg.coerceIn(15.0, 165.0))
            d = PolygonVertex(w * cos(angRad), w * sin(angRad))
            // Circle intersection of (B, radius=e) and (D, radius=n)
            val dx = d.x - b.x
            val dy = d.y - b.y
            val distBd = hypot(dx, dy)
            if (distBd <= 1e-6 || distBd > e + n || distBd < abs(e - n)) return null
            val aDist = (e * e - n * n + distBd * distBd) / (2.0 * distBd)
            val h = sqrt((e * e - aDist * aDist).coerceAtLeast(0.0))
            val px = b.x + aDist * dx / distBd
            val py = b.y + aDist * dy / distBd
            val c1 = PolygonVertex(px + h * (-dy) / distBd, py + h * dx / distBd)
            val c2 = PolygonVertex(px - h * (-dy) / distBd, py - h * dx / distBd)
            // Pick whichever keeps CCW simple polygon with positive signed area
            val poly1 = listOf(a, b, c1, d)
            val poly2 = listOf(a, b, c2, d)
            c = if (!PolygonDividerEngine.hasSelfIntersection(poly1) && signedArea(poly1) >= signedArea(poly2)) c1 else c2
        }

        val quadVerts = ensureCounterClockwise(listOf(a, b, c, d))
        if (PolygonDividerEngine.hasSelfIntersection(quadVerts)) return null
        val totalSqFt = PolygonDividerEngine.polygonAreaSqFt(quadVerts)
        val diagAc = hypot(c.x - a.x, c.y - a.y)
        val diagBd = hypot(d.x - b.x, d.y - b.y)
        val edges = PolygonDividerEngine.edgeLengths(quadVerts)

        val angles = quadVerts.indices.map { i ->
            val prev = quadVerts[(i + 3) % 4]
            val cur = quadVerts[i]
            val next = quadVerts[(i + 1) % 4]
            val v1x = prev.x - cur.x; val v1y = prev.y - cur.y
            val v2x = next.x - cur.x; val v2y = next.y - cur.y
            val dot = v1x * v2x + v1y * v2y
            val mags = (hypot(v1x, v1y) * hypot(v2x, v2y)).coerceAtLeast(1e-9)
            Math.toDegrees(acos((dot / mags).coerceIn(-1.0, 1.0)))
        }

        val count = partsCount.coerceIn(1, 12)
        val shares = (1..count).map { DivisionShareSpec("অংশ $it", 1.0) }
        val splitRes = dividePolygonPro(
            id = "quad_${System.currentTimeMillis()}",
            title = "QuadMaster চতুর্ভুজ বিভাজন",
            vertices = quadVerts,
            cutMode = DivisionGeometryCutMode.PARALLEL_LINE,
            shares = shares,
            cutAngleDeg = parallelCutAngleDeg,
            unitProfile = unitProfile
        )

        return QuadMasterResult(
            vertices = quadVerts,
            totalSqFt = totalSqFt,
            totalDecimal = totalSqFt / LandCalculators.SQ_FT_PER_DECIMAL,
            totalKatha = totalSqFt / unitProfile.kathaSqFt,
            diagonalAcFt = diagAc,
            diagonalBdFt = diagBd,
            interiorAnglesDeg = angles,
            edgeLengthsFt = edges,
            splitParcels = splitRes?.parcels ?: emptyList()
        )
    }

    // ==================== 5. PLOT PLANNER (OPTION A / B / C GRID COMPARISON) ====================

    fun evaluatePlotPlannerOption(
        outerVertices: List<PolygonVertex>,
        config: PlotPlannerOptionConfig,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): PlotPlannerOptionSummary {
        val grossSqFt = PolygonDividerEngine.polygonAreaSqFt(outerVertices)
        val targetPlotSqFt = (config.targetPlotKatha.coerceAtLeast(0.5) * unitProfile.kathaSqFt)
        val targetPlotDecimal = targetPlotSqFt / LandCalculators.SQ_FT_PER_DECIMAL

        val minX = outerVertices.minOfOrNull { it.x } ?: 0.0
        val maxX = outerVertices.maxOfOrNull { it.x } ?: 0.0
        val minY = outerVertices.minOfOrNull { it.y } ?: 0.0
        val maxY = outerVertices.maxOfOrNull { it.y } ?: 0.0

        val ratio = config.aspectRatioWidthToDepth.coerceIn(0.35, 1.5)
        val plotW = sqrt(targetPlotSqFt * ratio)
        val plotD = targetPlotSqFt / plotW.coerceAtLeast(1.0)
        val roadW = config.roadWidthFt.coerceAtLeast(0.0)

        val cells = mutableListOf<List<PolygonVertex>>()
        val roadStrips = mutableListOf<List<PolygonVertex>>()

        // Double-loaded residential block layout along Y rows separated by roads
        var y = minY
        var rowIdx = 0
        while (y + plotD <= maxY + 1e-6) {
            var x = minX
            while (x + plotW <= maxX + 1e-6) {
                val rect = listOf(
                    PolygonVertex(x, y),
                    PolygonVertex(x + plotW, y),
                    PolygonVertex(x + plotW, y + plotD),
                    PolygonVertex(x, y + plotD)
                )
                val center = PolygonVertex(x + plotW * 0.5, y + plotD * 0.5)
                if (rect.all { pointInPolygon(it, outerVertices) } && pointInPolygon(center, outerVertices)) {
                    cells.add(rect)
                }
                x += plotW
            }
            y += plotD
            rowIdx++
            // Place access road between every second back-to-back row or single rows if road > 0
            if (roadW > 0.0 && rowIdx % 2 == 1 && y + roadW <= maxY) {
                roadStrips.add(
                    listOf(
                        PolygonVertex(minX, y),
                        PolygonVertex(maxX, y),
                        PolygonVertex(maxX, y + roadW),
                        PolygonVertex(minX, y + roadW)
                    )
                )
                y += roadW
            }
        }

        val drainOpenPct = config.drainOpenSpacePct.coerceIn(0.0, 35.0)
        val drainOpenSqFt = grossSqFt * (drainOpenPct / 100.0)
        val rawRoadSqFt = roadStrips.sumOf { exactIntersectionAreaSqFt(outerVertices, it) }
        val totalInfraSqFt = (rawRoadSqFt + drainOpenSqFt).coerceAtMost(grossSqFt * 0.75)

        val plotCount = if (cells.isNotEmpty()) {
            cells.size
        } else {
            // Fallback analytical yield when outer parcel is narrow/irregular
            ((grossSqFt - totalInfraSqFt).coerceAtLeast(0.0) / targetPlotSqFt).toInt()
        }

        val salablePlotsSqFt = (plotCount * targetPlotSqFt).coerceAtMost((grossSqFt - totalInfraSqFt).coerceAtLeast(0.0))
        val netSalableSqFt = (grossSqFt - totalInfraSqFt).coerceAtLeast(0.0)
        val remainingWasteSqFt = (netSalableSqFt - salablePlotsSqFt).coerceAtLeast(0.0)
        val effPct = if (grossSqFt > 0.0) (salablePlotsSqFt / grossSqFt) * 100.0 else 0.0

        return PlotPlannerOptionSummary(
            optionLabel = config.optionLabel,
            targetPlotKatha = config.targetPlotKatha,
            targetPlotDecimal = targetPlotDecimal,
            grossAreaSqFt = grossSqFt,
            roadAndDrainSqFt = totalInfraSqFt,
            netSalableSqFt = netSalableSqFt,
            plotCount = plotCount,
            salablePlotsAreaSqFt = salablePlotsSqFt,
            remainingWasteSqFt = remainingWasteSqFt,
            roadFrontagePerPlotFt = plotW,
            plotDepthFt = plotD,
            efficiencyPct = effPct,
            plotCells = cells,
            roadCorridors = roadStrips
        )
    }

    fun comparePlotPlannerOptions(
        outerVertices: List<PolygonVertex>,
        options: List<PlotPlannerOptionConfig> = listOf(
            PlotPlannerOptionConfig("Option A (৩ কাঠা • ২০' রাস্তা)", 3.0, 20.0, 8.0),
            PlotPlannerOptionConfig("Option B (২.৫ কাঠা • ১৬' রাস্তা)", 2.5, 16.0, 6.0),
            PlotPlannerOptionConfig("Option C (৪ কাঠা • ১২' রাস্তা)", 4.0, 12.0, 5.0)
        ),
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): List<PlotPlannerOptionSummary> = options.map { evaluatePlotPlannerOption(outerVertices, it, unitProfile) }

    // ==================== 6. TAFSIL DIVISION (MULTI-DAG / MULTI-KHATIAN PARTITION) ====================

    fun divideTafsilPlots(
        dags: List<TafsilDagInput>,
        owners: List<TafsilOwnerShareSpec>,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): TafsilDivisionReport {
        val totalEff = dags.sumOf { it.effectiveDecimal }
        val sumShares = owners.sumOf { it.shareRatio.coerceAtLeast(0.0) }
        val balanced = abs(sumShares - 1.0) <= 0.002
        val normFactor = if (sumShares > 1e-6) 1.0 / sumShares else 1.0

        val rows = mutableListOf<TafsilAllocationRow>()
        val ownerTotals = linkedMapOf<String, Double>()
        val kathaPerDecimal = LandCalculators.SQ_FT_PER_DECIMAL / unitProfile.kathaSqFt

        for (dag in dags) {
            val dagEff = dag.effectiveDecimal
            for (owner in owners) {
                val normalizedRatio = owner.shareRatio.coerceAtLeast(0.0) * normFactor
                val allocDec = dagEff * normalizedRatio
                rows.add(
                    TafsilAllocationRow(
                        ownerName = owner.ownerName.ifBlank { "অংশীদার" },
                        khatianNo = dag.khatianNo,
                        dagNo = dag.dagNo,
                        landClass = dag.landClass,
                        allocatedDecimal = allocDec,
                        allocatedKatha = allocDec * kathaPerDecimal,
                        sharePct = normalizedRatio * 100.0
                    )
                )
                ownerTotals[owner.ownerName] = (ownerTotals[owner.ownerName] ?: 0.0) + allocDec
            }
        }

        return TafsilDivisionReport(
            totalEffectiveDecimal = totalEff,
            totalAllocatedDecimal = rows.sumOf { it.allocatedDecimal },
            ownerTotalsDecimal = ownerTotals,
            rows = rows,
            isShareSumBalanced = balanced
        )
    }

    // ==================== 7. PRO REPORT PNG / PDF EXPORT ====================

    fun exportProDivisionReport(
        context: Context?,
        report: ProDivisionReport,
        asPdf: Boolean,
        desktopFallbackDir: File? = null
    ): ExportedFileResult {
        val width = 1850
        val height = 1550
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            textSize = 40f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 24f
        }
        canvas.drawText("${report.title} (${report.cutMode.titleBn})", 55f, 65f, titlePaint)
        canvas.drawText(
            String.format(
                Locale.US,
                "মোট: %.2f শতক | হোল/পুকুর বাদ: %.2f শতক | রাস্তা: %.2f শতক | নিট বণ্টন: %.2f শতক | রাস্তার মুখ: %.1f' → %.1f'",
                report.grossOuterAreaSqFt / 435.6,
                report.totalHoleExcludedSqFt / 435.6,
                report.roadReservedSqFt / 435.6,
                report.allocatedNetAreaSqFt / 435.6,
                report.totalRoadFrontageBeforeFt,
                report.totalRoadFrontageAfterFt
            ),
            55f,
            106f,
            subPaint
        )

        val allPts = report.parcels.flatMap { it.vertices } + report.roadPolygon + report.holes.flatMap { it.vertices }
        if (allPts.isNotEmpty()) {
            val minX = allPts.minOf { it.x }
            val maxX = allPts.maxOf { it.x }
            val minY = allPts.minOf { it.y }
            val maxY = allPts.maxOf { it.y }
            val spanX = (maxX - minX).coerceAtLeast(1.0)
            val spanY = (maxY - minY).coerceAtLeast(1.0)
            val drawLeft = 75f
            val drawTop = 140f
            val drawW = width - 150f
            val drawH = 820f
            val scale = min(drawW / spanX, drawH / spanY)

            fun toScreen(v: PolygonVertex) =
                (drawLeft + (v.x - minX) * scale).toFloat() to (drawTop + (maxY - v.y) * scale).toFloat()

            if (report.roadPolygon.size >= 3) {
                val rp = Path()
                report.roadPolygon.forEachIndexed { idx, v ->
                    val (sx, sy) = toScreen(v)
                    if (idx == 0) rp.moveTo(sx, sy) else rp.lineTo(sx, sy)
                }
                rp.close()
                canvas.drawPath(rp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CFD8DC"); style = Paint.Style.FILL })
                canvas.drawPath(rp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#455A64"); style = Paint.Style.STROKE; strokeWidth = 3f })
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3.5f }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }

            report.parcels.forEach { p ->
                val path = Path()
                var sumX = 0f; var sumY = 0f
                p.vertices.forEachIndexed { idx, v ->
                    val (sx, sy) = toScreen(v)
                    sumX += sx; sumY += sy
                    if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                }
                path.close()
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.colorArgb.toInt(); alpha = 70; style = Paint.Style.FILL })
                canvas.drawPath(path, borderPaint)
                val cx = sumX / p.vertices.size
                val cy = sumY / p.vertices.size
                canvas.drawText("${p.index}. ${p.ownerName}", cx, cy - 10f, labelPaint)
                canvas.drawText(String.format(Locale.US, "%.2f শতক (মুখ %.1f')", p.netDecimal, p.roadFrontageFt), cx, cy + 20f, labelPaint)
            }

            // Draw excluded holes (e.g. pond/house)
            val holeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B3E5FC"); alpha = 190; style = Paint.Style.FILL }
            val holeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0277BD")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }
            report.holes.forEach { hole ->
                if (hole.vertices.size >= 3) {
                    val hp = Path()
                    var hx = 0f; var hy = 0f
                    hole.vertices.forEachIndexed { idx, v ->
                        val (sx, sy) = toScreen(v)
                        hx += sx; hy += sy
                        if (idx == 0) hp.moveTo(sx, sy) else hp.lineTo(sx, sy)
                    }
                    hp.close()
                    canvas.drawPath(hp, holeFill)
                    canvas.drawPath(hp, holeStroke)
                    canvas.drawText(hole.label, hx / hole.vertices.size, hy / hole.vertices.size, labelPaint)
                }
            }
        }

        // Table of parcels + 4-side boundaries (Chouhoddi)
        var yRow = 1010f
        val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B4D3E"); textSize = 24f; isFakeBoldText = true }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 21f }
        canvas.drawText("বণ্টন ও চৌহদ্দি (৪-পাশের সীমানা) তালিকা:", 55f, yRow, hdrPaint)
        yRow += 36f
        report.parcels.take(10).forEach { p ->
            val sidesFtIn = p.edgeLengthsFt.joinToString(", ") { LandCalculators.formatFeetInches(it) }
            canvas.drawText(
                String.format(
                    Locale.US,
                    "%d. %s — নিট %.2f শতক (%.2f কাঠা) | মুখ: %.1f' | বাহু: %s",
                    p.index, p.ownerName, p.netDecimal, p.netKatha, p.roadFrontageFt, sidesFtIn
                ),
                55f,
                yRow,
                rowPaint
            )
            yRow += 28f
            canvas.drawText(
                "   চৌহদ্দি: উ: ${p.boundaries.north} | দ: ${p.boundaries.south} | পূ: ${p.boundaries.east} | প: ${p.boundaries.west}",
                55f,
                yRow,
                subPaint
            )
            yRow += 34f
        }

        canvas.drawText(
            "বিঃদ্রঃ এটি জ্যামিতিক বিভাজন খসড়া — চূড়ান্ত সীমানা নির্ধারণে সরেজমিন জরিপ ও আমিন যাচাই আবশ্যক।",
            55f,
            height - 40f,
            subPaint
        )

        val fileName = "Land_Division_Pro_${System.currentTimeMillis()}"
        val res = if (asPdf) {
            ExportStreamWriter.savePdfBitmap(context, bmp, fileName, desktopFallbackDir = desktopFallbackDir)
        } else {
            ExportStreamWriter.savePngBitmap(context, bmp, fileName, desktopFallbackDir = desktopFallbackDir)
        }
        return ExportedFileResult(res.uri, res.displayPath)
    }
}
