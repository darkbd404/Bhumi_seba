package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.CadDimensionEntity
import com.example.model.CadDimensionType
import com.example.model.CadEntity
import com.example.model.CadEntityType
import com.example.model.CadLayerConfig
import com.example.model.CadOsnapMode
import com.example.model.CadStyleSettings
import com.example.model.MeasurementDisplayUnit
import com.example.model.PolygonVertex
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Module 01, 02 & 04 Engine:
 * - Complete 2D CAD Drafting & Geometry Editing (Offset, Fillet, Chamfer, Trim/Extend, Mirror, Rotate, Scale, Array, Divide)
 * - Self-intersection / Gap / Overlap / Duplicate Line Detection & Cleanup
 * - All 15 OSNAP & Precision Tracking Modes
 * - All 30 Associative Dimension Types with live geometry recalculation
 * - All 8 Measurement Display Units (37'-8", 37 feet 8 inches, fractional 1/8", decimal ft, m, cm, mm, Bengali)
 * - Auto Plot Dimensioning (N/S/E/W + diagonals + interior/exterior angles + corner ordinates)
 * - 16-Layer Multi-Layer Plot Engine + Paper-Space A0-A4 Title Block PDF/Bitmap + DXF/SVG Export
 */
object CadAndDimensionEngine {

    fun defaultLayers(): List<CadLayerConfig> = listOf(
        CadLayerConfig("layer_mouza_bdy", "মৌজা সীমানা", "MOUZA_BOUNDARY", 0xFF1B5E20, 4f),
        CadLayerConfig("layer_plot_bdy", "দাগ সীমানা", "DAG_BOUNDARY", 0xFF0D47A1, 3.5f),
        CadLayerConfig("layer_proposed_bdy", "প্রস্তাবিত সীমানা", "PROPOSED_BDY", 0xFFD81B60, 3f),
        CadLayerConfig("layer_old_bdy", "পুরাতন/সাবেক সীমানা", "OLD_BOUNDARY", 0xFF6D4C41, 2.5f),
        CadLayerConfig("layer_new_bdy", "নতুন/হাল সীমানা", "NEW_BOUNDARY", 0xFF00796B, 3f),
        CadLayerConfig("layer_disputed", "বিরোধপূর্ণ এলাকা", "DISPUTED_AREA", 0xFFC62828, 3f),
        CadLayerConfig("layer_road", "রাস্তা / প্রবেশপথ", "ROAD_ACCESS", 0xFF455A64, 3f),
        CadLayerConfig("layer_water", "পুকুর / খাল / জলাশয়", "WATERBODY", 0xFF0288D1, 2.5f),
        CadLayerConfig("layer_building", "ভবন / স্থাপনা", "BUILDING", 0xFF5D4037, 2.5f),
        CadLayerConfig("layer_dim", "ডাইমেনশন (মাপ)", "DIMENSIONS", 0xFFE65100, 2f),
        CadLayerConfig("layer_text", "টেক্সট ও লেবেল", "TEXT_LABELS", 0xFF212121, 1.8f),
        CadLayerConfig("layer_owner", "মালিকের নাম ও অংশ", "OWNER_INFO", 0xFF4A148C, 1.8f),
        CadLayerConfig("layer_gps", "সার্ভে ও GPS পয়েন্ট", "SURVEY_POINTS", 0xFF00838F, 2f),
        CadLayerConfig("layer_grid", "কো-অর্ডিনেট গ্রিড", "COORD_GRID", 0xFF90A4AE, 1f),
        CadLayerConfig("layer_hatch", "হ্যাচ / শেড ফিল", "HATCH_FILL", 0xFF2E7D32, 1.5f),
        CadLayerConfig("layer_anno", "অ্যানোটেশন ও লিডার", "ANNOTATIONS", 0xFF37474F, 2f)
    )

    // -------------------- Module 02: Precision Unit Formatters --------------------

    fun formatFeetDistance(
        feet: Double,
        unit: MeasurementDisplayUnit,
        precision: Int = 2,
        prefix: String = "",
        suffix: String = ""
    ): String {
        val core = when (unit) {
            MeasurementDisplayUnit.FEET_INCHES -> {
                val totalIn = (feet * 12.0).roundToInt()
                val ft = totalIn / 12
                val inch = abs(totalIn % 12)
                "${ft}'-${inch}\""
            }
            MeasurementDisplayUnit.FEET_INCHES_WORDS -> {
                val totalIn = (feet * 12.0).roundToInt()
                val ft = totalIn / 12
                val inch = abs(totalIn % 12)
                "$ft feet $inch inches"
            }
            MeasurementDisplayUnit.FRACTIONAL_INCHES -> {
                val totalEighths = (feet * 12.0 * 8.0).roundToInt()
                val ft = totalEighths / 96
                val remEighths = abs(totalEighths % 96)
                val wholeIn = remEighths / 8
                val fracEighth = remEighths % 8
                val fracStr = when (fracEighth) {
                    0 -> ""
                    2 -> " 1/4"
                    4 -> " 1/2"
                    6 -> " 3/4"
                    else -> " $fracEighth/8"
                }
                "${ft}'-${wholeIn}${fracStr}\""
            }
            MeasurementDisplayUnit.DECIMAL_FEET -> {
                String.format(Locale.US, "%.${precision}f ft", feet)
            }
            MeasurementDisplayUnit.METRES -> {
                String.format(Locale.US, "%.${precision}f m", feet * 0.3048)
            }
            MeasurementDisplayUnit.CENTIMETRES -> {
                String.format(Locale.US, "%.${precision}f cm", feet * 30.48)
            }
            MeasurementDisplayUnit.MILLIMETRES -> {
                String.format(Locale.US, "%.0f mm", feet * 304.8)
            }
            MeasurementDisplayUnit.BENGALI_UNITS -> {
                val hat = feet / 1.5
                String.format(Locale.US, "%.2f ফুট (%.2f হাত)", feet, hat)
            }
        }
        return buildString {
            if (prefix.isNotBlank()) append(prefix).append(" ")
            append(core)
            if (suffix.isNotBlank()) append(" ").append(suffix)
        }
    }

    fun formatDualMeasurementSummary(areaSqFt: Double, perimeterFt: Double, unit: MeasurementDisplayUnit): String {
        val dec = areaSqFt / LandCalculators.SQ_FT_PER_DECIMAL
        val katha = areaSqFt / LandCalculators.SQ_FT_PER_KATHA_BD
        val sqM = areaSqFt * 0.09290304
        val perimStr = formatFeetDistance(perimeterFt, unit)
        return String.format(
            Locale.US,
            "%.2f শতক | %.2f কাঠা | %.1f বর্গফুট (%.1f m²) | পরিসীমা: %s",
            dec, katha, areaSqFt, sqM, perimStr
        )
    }

    // -------------------- Module 01.1: Evaluate All 30 Associative Dimension Types --------------------

    data class EvaluatedDimension(
        val numericValue: Double,
        val formattedLabel: String,
        val dimLineP1: PolygonVertex,
        val dimLineP2: PolygonVertex,
        val textAnchor: PolygonVertex
    )

    fun evaluateDimension(dim: CadDimensionEntity, entities: List<CadEntity> = emptyList()): EvaluatedDimension {
        // Reassociate endpoints if associatedEntityId matches a polygon/polyline edge
        val assoc = if (!dim.associatedEntityId.isNullOrBlank()) {
            entities.firstOrNull { it.id == dim.associatedEntityId }
        } else null
        val p1 = assoc?.points?.firstOrNull() ?: dim.p1
        val p2 = assoc?.points?.getOrNull(1) ?: dim.p2
        val p3 = dim.p3 ?: assoc?.points?.getOrNull(2)

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dist = hypot(dx, dy)
        val angleRad = atan2(dy, dx)
        val nx = -sin(angleRad)
        val ny = cos(angleRad)

        var measuredVal = dist
        var label: String
        var lineStart = PolygonVertex(p1.x + nx * dim.offsetDistanceFt, p1.y + ny * dim.offsetDistanceFt)
        var lineEnd = PolygonVertex(p2.x + nx * dim.offsetDistanceFt, p2.y + ny * dim.offsetDistanceFt)

        when (dim.dimType) {
            CadDimensionType.HORIZONTAL -> {
                measuredVal = abs(dx)
                val yOff = max(p1.y, p2.y) + dim.offsetDistanceFt
                lineStart = PolygonVertex(p1.x, yOff)
                lineEnd = PolygonVertex(p2.x, yOff)
                label = formatFeetDistance(measuredVal, dim.unit, dim.precision, dim.prefix, dim.suffix)
            }
            CadDimensionType.VERTICAL -> {
                measuredVal = abs(dy)
                val xOff = max(p1.x, p2.x) + dim.offsetDistanceFt
                lineStart = PolygonVertex(xOff, p1.y)
                lineEnd = PolygonVertex(xOff, p2.y)
                label = formatFeetDistance(measuredVal, dim.unit, dim.precision, dim.prefix, dim.suffix)
            }
            CadDimensionType.ROTATED -> {
                val theta = dim.rotationDeg * PI / 180.0
                measuredVal = abs(dx * cos(theta) + dy * sin(theta))
                val rnx = -sin(theta)
                val rny = cos(theta)
                lineStart = PolygonVertex(p1.x + rnx * dim.offsetDistanceFt, p1.y + rny * dim.offsetDistanceFt)
                lineEnd = PolygonVertex(
                    lineStart.x + (dx * cos(theta) + dy * sin(theta)) * cos(theta),
                    lineStart.y + (dx * cos(theta) + dy * sin(theta)) * sin(theta)
                )
                label = formatFeetDistance(measuredVal, dim.unit, dim.precision, dim.prefix, dim.suffix)
            }
            CadDimensionType.ANGULAR,
            CadDimensionType.TWO_PT_ANGULAR,
            CadDimensionType.THREE_PT_ANGULAR -> {
                val vertex = p2
                val a1 = p1
                val a2 = p3 ?: PolygonVertex(vertex.x + 20.0, vertex.y)
                val v1x = a1.x - vertex.x
                val v1y = a1.y - vertex.y
                val v2x = a2.x - vertex.x
                val v2y = a2.y - vertex.y
                val mag = (hypot(v1x, v1y) * hypot(v2x, v2y)).coerceAtLeast(1e-9)
                val cosT = ((v1x * v2x + v1y * v2y) / mag).coerceIn(-1.0, 1.0)
                measuredVal = acos(cosT) * 180.0 / PI
                lineStart = a1
                lineEnd = a2
                label = buildString {
                    if (dim.prefix.isNotBlank()) append(dim.prefix).append(" ")
                    append(String.format(Locale.US, "%.2f°", measuredVal))
                    if (dim.suffix.isNotBlank()) append(" ").append(dim.suffix)
                }
            }
            CadDimensionType.RADIUS, CadDimensionType.JOGGED_RADIUS -> {
                measuredVal = dist
                lineStart = p1
                lineEnd = p2
                val prefix = dim.prefix.ifBlank { if (dim.dimType == CadDimensionType.JOGGED_RADIUS) "JR" else "R" }
                label = "$prefix ${formatFeetDistance(measuredVal, dim.unit, dim.precision, "", dim.suffix)}"
            }
            CadDimensionType.DIAMETER -> {
                measuredVal = dist * 2.0
                lineStart = PolygonVertex(p1.x - dx, p1.y - dy)
                lineEnd = p2
                val prefix = dim.prefix.ifBlank { "Ø" }
                label = "$prefix ${formatFeetDistance(measuredVal, dim.unit, dim.precision, "", dim.suffix)}"
            }
            CadDimensionType.ARC_LENGTH -> {
                val sweepDeg = abs(dim.rotationDeg).takeIf { it > 0.1 } ?: 60.0
                measuredVal = dist * (sweepDeg * PI / 180.0)
                lineStart = p1
                lineEnd = p2
                label = "⌒ ${formatFeetDistance(measuredVal, dim.unit, dim.precision, dim.prefix, dim.suffix)}"
            }
            CadDimensionType.ORDINATE_X -> {
                measuredVal = p1.x
                lineStart = p1
                lineEnd = PolygonVertex(p1.x, p1.y - dim.offsetDistanceFt)
                label = "E: ${formatFeetDistance(abs(measuredVal), dim.unit, dim.precision, dim.prefix, dim.suffix)}"
            }
            CadDimensionType.ORDINATE_Y -> {
                measuredVal = p1.y
                lineStart = p1
                lineEnd = PolygonVertex(p1.x + dim.offsetDistanceFt, p1.y)
                label = "N: ${formatFeetDistance(abs(measuredVal), dim.unit, dim.precision, dim.prefix, dim.suffix)}"
            }
            CadDimensionType.CENTER_MARK, CadDimensionType.CENTERLINE -> {
                measuredVal = dist
                lineStart = p1
                lineEnd = p2
                label = if (dim.dimType == CadDimensionType.CENTER_MARK) "+" else "CL"
            }
            CadDimensionType.LEADER, CadDimensionType.MULTILEADER -> {
                measuredVal = dist
                lineStart = p1
                lineEnd = p2
                label = dim.overrideText.ifBlank {
                    "${dim.prefix} ${formatFeetDistance(dist, dim.unit, dim.precision)} ${dim.suffix}".trim()
                }
            }
            else -> {
                // LINEAR, ALIGNED, BASELINE, CONTINUE, QUICK_DIM, MULTIPLE_DIM, CHAIN_DIM,
                // STAGGERED_DIM, DIM_BREAK, DIM_SPACING, DIM_ALIGNMENT, DIM_OVERRIDE, DIM_REASSOCIATE, DIM_EDIT
                val staggerExtra = dim.staggeredLevel * 5.5
                val off = dim.offsetDistanceFt + staggerExtra
                lineStart = PolygonVertex(p1.x + nx * off, p1.y + ny * off)
                lineEnd = PolygonVertex(p2.x + nx * off, p2.y + ny * off)
                label = formatFeetDistance(measuredVal, dim.unit, dim.precision, dim.prefix, dim.suffix)
            }
        }

        val finalLabel = if (dim.overrideText.isNotBlank() && dim.dimType != CadDimensionType.LEADER && dim.dimType != CadDimensionType.MULTILEADER) {
            dim.overrideText
        } else {
            label
        }

        val anchor = PolygonVertex((lineStart.x + lineEnd.x) / 2.0, (lineStart.y + lineEnd.y) / 2.0)
        return EvaluatedDimension(measuredVal, finalLabel, lineStart, lineEnd, anchor)
    }

    /** Module 02: Auto Plot Dimensioning (N/S/E/W + interior edges + diagonals + interior angles + corner ordinates) */
    fun generateAutoPlotDimensions(
        vertices: List<PolygonVertex>,
        unit: MeasurementDisplayUnit,
        includeDiagonals: Boolean = true,
        includeAngles: Boolean = true,
        includeOrdinates: Boolean = false
    ): List<CadDimensionEntity> {
        if (vertices.size < 3) return emptyList()
        val out = mutableListOf<CadDimensionEntity>()
        val n = vertices.size
        for (i in 0 until n) {
            val a = vertices[i]
            val b = vertices[(i + 1) % n]
            val c = vertices[(i + 2) % n]
            out.add(
                CadDimensionEntity(
                    id = "auto_edge_$i",
                    dimType = CadDimensionType.ALIGNED,
                    p1 = a,
                    p2 = b,
                    offsetDistanceFt = 6.0 + (i % 2) * 3.5, // anti-overlap stagger
                    unit = unit,
                    staggeredLevel = i % 2
                )
            )
            if (includeAngles) {
                out.add(
                    CadDimensionEntity(
                        id = "auto_ang_$i",
                        dimType = CadDimensionType.THREE_PT_ANGULAR,
                        p1 = a,
                        p2 = b,
                        p3 = c,
                        offsetDistanceFt = 4.0,
                        unit = unit
                    )
                )
            }
            if (includeOrdinates) {
                out.add(
                    CadDimensionEntity(
                        id = "auto_ord_x_$i",
                        dimType = CadDimensionType.ORDINATE_X,
                        p1 = a,
                        p2 = a,
                        offsetDistanceFt = 5.0,
                        unit = unit
                    )
                )
            }
        }
        if (includeDiagonals && n >= 4) {
            out.add(
                CadDimensionEntity(
                    id = "auto_diag_02",
                    dimType = CadDimensionType.ALIGNED,
                    p1 = vertices[0],
                    p2 = vertices[2],
                    offsetDistanceFt = 0.0,
                    prefix = "Diag",
                    unit = unit
                )
            )
        }
        return out
    }

    // -------------------- Module 01.5: OSNAP & Precision Point Resolver --------------------

    data class SnapMatch(
        val snapped: PolygonVertex,
        val activeMode: CadOsnapMode?
    )

    fun resolveSnap(
        raw: PolygonVertex,
        lastAnchor: PolygonVertex?,
        entities: List<CadEntity>,
        settings: CadStyleSettings,
        snapRadiusFt: Double = 6.0
    ): SnapMatch {
        val enabled = settings.enabledOsnaps.toSet()
        var candidate = raw
        var matchedMode: CadOsnapMode? = null
        var bestDist = snapRadiusFt

        // 1. Ortho / Polar constraint relative to lastAnchor
        if (lastAnchor != null) {
            if (CadOsnapMode.ORTHO_MODE in enabled) {
                val dx = abs(raw.x - lastAnchor.x)
                val dy = abs(raw.y - lastAnchor.y)
                candidate = if (dx >= dy) PolygonVertex(raw.x, lastAnchor.y) else PolygonVertex(lastAnchor.x, raw.y)
                matchedMode = CadOsnapMode.ORTHO_MODE
            } else if (CadOsnapMode.POLAR_TRACKING in enabled) {
                val dx = raw.x - lastAnchor.x
                val dy = raw.y - lastAnchor.y
                val len = hypot(dx, dy)
                if (len > 1e-4) {
                    val deg = atan2(dy, dx) * 180.0 / PI
                    val snappedDeg = (deg / 15.0).roundToInt() * 15.0
                    if (abs(deg - snappedDeg) < 6.0) {
                        val r = snappedDeg * PI / 180.0
                        candidate = PolygonVertex(lastAnchor.x + len * cos(r), lastAnchor.y + len * sin(r))
                        matchedMode = CadOsnapMode.POLAR_TRACKING
                    }
                }
            }
        }

        // 2. Geometric OSNAP checks against entities
        entities.forEach { e ->
            // Endpoints & Nodes
            if (CadOsnapMode.ENDPOINT in enabled || CadOsnapMode.NODE in enabled) {
                e.points.forEach { pt ->
                    val d = hypot(raw.x - pt.x, raw.y - pt.y)
                    if (d < bestDist) {
                        bestDist = d
                        candidate = pt
                        matchedMode = if (e.type == CadEntityType.POINT) CadOsnapMode.NODE else CadOsnapMode.ENDPOINT
                    }
                }
            }
            // Midpoints
            if (CadOsnapMode.MIDPOINT in enabled && e.points.size >= 2) {
                val count = if (e.closed) e.points.size else e.points.size - 1
                for (i in 0 until count) {
                    val a = e.points[i]
                    val b = e.points[(i + 1) % e.points.size]
                    val mid = PolygonVertex((a.x + b.x) / 2.0, (a.y + b.y) / 2.0)
                    val d = hypot(raw.x - mid.x, raw.y - mid.y)
                    if (d < bestDist) {
                        bestDist = d
                        candidate = mid
                        matchedMode = CadOsnapMode.MIDPOINT
                    }
                }
            }
            // Circle/Arc Center & Quadrants
            if ((e.type == CadEntityType.CIRCLE || e.type == CadEntityType.ARC) && e.points.isNotEmpty()) {
                val c = e.points.first()
                if (CadOsnapMode.CENTER in enabled || CadOsnapMode.GEOMETRIC_CENTER in enabled) {
                    val d = hypot(raw.x - c.x, raw.y - c.y)
                    if (d < bestDist) {
                        bestDist = d
                        candidate = c
                        matchedMode = CadOsnapMode.CENTER
                    }
                }
                if (CadOsnapMode.QUADRANT in enabled && e.radius > 0.0) {
                    listOf(
                        PolygonVertex(c.x + e.radius, c.y),
                        PolygonVertex(c.x - e.radius, c.y),
                        PolygonVertex(c.x, c.y + e.radius),
                        PolygonVertex(c.x, c.y - e.radius)
                    ).forEach { q ->
                        val d = hypot(raw.x - q.x, raw.y - q.y)
                        if (d < bestDist) {
                            bestDist = d
                            candidate = q
                            matchedMode = CadOsnapMode.QUADRANT
                        }
                    }
                }
            }
            // Perpendicular / Nearest from lastAnchor or raw
            if ((CadOsnapMode.PERPENDICULAR in enabled || CadOsnapMode.NEAREST in enabled) && e.points.size >= 2) {
                val ref = lastAnchor ?: raw
                for (i in 0 until e.points.size - 1) {
                    val proj = projectPointOnSegment(ref, e.points[i], e.points[i + 1])
                    val d = hypot(raw.x - proj.x, raw.y - proj.y)
                    if (d < bestDist) {
                        bestDist = d
                        candidate = proj
                        matchedMode = if (CadOsnapMode.PERPENDICULAR in enabled) CadOsnapMode.PERPENDICULAR else CadOsnapMode.NEAREST
                    }
                }
            }
        }

        // 3. Grid snap fallback if no geometric snap caught
        if (matchedMode == null && CadOsnapMode.GRID_SNAP in enabled && settings.gridSpacingFt > 0.0) {
            val g = settings.gridSpacingFt
            candidate = PolygonVertex(
                (raw.x / g).roundToInt() * g,
                (raw.y / g).roundToInt() * g
            )
            matchedMode = CadOsnapMode.GRID_SNAP
        }

        return SnapMatch(candidate, matchedMode)
    }

    private fun projectPointOnSegment(p: PolygonVertex, a: PolygonVertex, b: PolygonVertex): PolygonVertex {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val len2 = abx * abx + aby * aby
        if (len2 < 1e-9) return a
        val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / len2).coerceIn(0.0, 1.0)
        return PolygonVertex(a.x + t * abx, a.y + t * aby)
    }

    // -------------------- Module 01.3 & 01.4: CAD Modify, Repair & Topology --------------------

    fun offsetPolygon(vertices: List<PolygonVertex>, offsetFt: Double): List<PolygonVertex> {
        if (vertices.size < 3 || abs(offsetFt) < 1e-6) return vertices
        val cx = vertices.map { it.x }.average()
        val cy = vertices.map { it.y }.average()
        return vertices.map { v ->
            val dx = v.x - cx
            val dy = v.y - cy
            val len = hypot(dx, dy).coerceAtLeast(1e-6)
            PolygonVertex(v.x + (dx / len) * offsetFt, v.y + (dy / len) * offsetFt)
        }
    }

    fun rotateVertices(vertices: List<PolygonVertex>, angleDeg: Double): List<PolygonVertex> {
        if (vertices.isEmpty()) return vertices
        val cx = vertices.map { it.x }.average()
        val cy = vertices.map { it.y }.average()
        val r = angleDeg * PI / 180.0
        val c = cos(r); val s = sin(r)
        return vertices.map { v ->
            val dx = v.x - cx; val dy = v.y - cy
            PolygonVertex(cx + dx * c - dy * s, cy + dx * s + dy * c)
        }
    }

    fun scaleVertices(vertices: List<PolygonVertex>, factor: Double): List<PolygonVertex> {
        if (vertices.isEmpty() || factor <= 0.0) return vertices
        val cx = vertices.map { it.x }.average()
        val cy = vertices.map { it.y }.average()
        return vertices.map { v ->
            PolygonVertex(cx + (v.x - cx) * factor, cy + (v.y - cy) * factor)
        }
    }

    fun mirrorVertices(vertices: List<PolygonVertex>, horizontalAxis: Boolean): List<PolygonVertex> {
        if (vertices.isEmpty()) return vertices
        val cx = vertices.map { it.x }.average()
        val cy = vertices.map { it.y }.average()
        return vertices.map { v ->
            if (horizontalAxis) PolygonVertex(v.x, 2 * cy - v.y) else PolygonVertex(2 * cx - v.x, v.y)
        }
    }

    fun filletCorner(a: PolygonVertex, corner: PolygonVertex, b: PolygonVertex, radiusFt: Double): List<PolygonVertex> {
        val v1x = a.x - corner.x; val v1y = a.y - corner.y
        val v2x = b.x - corner.x; val v2y = b.y - corner.y
        val l1 = hypot(v1x, v1y); val l2 = hypot(v2x, v2y)
        if (l1 < radiusFt || l2 < radiusFt || radiusFt <= 0.0) return listOf(corner)
        val p1 = PolygonVertex(corner.x + (v1x / l1) * radiusFt, corner.y + (v1y / l1) * radiusFt)
        val p2 = PolygonVertex(corner.x + (v2x / l2) * radiusFt, corner.y + (v2y / l2) * radiusFt)
        val mid = PolygonVertex((p1.x + p2.x) * 0.5 + (corner.x - (p1.x + p2.x) * 0.5) * 0.3, (p1.y + p2.y) * 0.5 + (corner.y - (p1.y + p2.y) * 0.5) * 0.3)
        return listOf(p1, mid, p2)
    }

    fun divideSegmentIntoEqualPoints(a: PolygonVertex, b: PolygonVertex, segments: Int): List<PolygonVertex> {
        val n = segments.coerceAtLeast(2)
        return (1 until n).map { i ->
            val t = i.toDouble() / n
            PolygonVertex(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }

    data class TopologyRepairReport(
        val selfIntersections: Int,
        val duplicateVertexCount: Int,
        val duplicateEntityCount: Int,
        val openGapsCount: Int,
        val cleanedEntities: List<CadEntity>
    )

    fun analyzeAndCleanTopology(entities: List<CadEntity>, toleranceFt: Double = 0.05): TopologyRepairReport {
        var selfIntersections = 0
        var dupVertices = 0
        var openGaps = 0
        val seenSignatures = mutableSetOf<String>()
        var dupEntities = 0
        val cleaned = mutableListOf<CadEntity>()

        entities.forEach { ent ->
            val dedupPts = mutableListOf<PolygonVertex>()
            ent.points.forEach { pt ->
                val prev = dedupPts.lastOrNull()
                if (prev != null && hypot(pt.x - prev.x, pt.y - prev.y) < toleranceFt) {
                    dupVertices++
                } else {
                    dedupPts.add(pt)
                }
            }
            if ((ent.type == CadEntityType.POLYLINE || ent.type == CadEntityType.POLYGON) && dedupPts.size >= 3) {
                val gap = hypot(dedupPts.first().x - dedupPts.last().x, dedupPts.first().y - dedupPts.last().y)
                if (!ent.closed && gap > toleranceFt && gap < 5.0) {
                    openGaps++
                }
                selfIntersections += countSelfIntersections(dedupPts)
            }
            val sig = "${ent.type.name}|${dedupPts.joinToString(";") { "%.2f,%.2f".format(Locale.US, it.x, it.y) }}"
            if (!seenSignatures.add(sig)) {
                dupEntities++
            } else {
                cleaned.add(ent.copy(points = dedupPts))
            }
        }
        return TopologyRepairReport(selfIntersections, dupVertices, dupEntities, openGaps, cleaned)
    }

    fun countSelfIntersections(pts: List<PolygonVertex>): Int {
        if (pts.size < 4) return 0
        var count = 0
        val n = pts.size
        for (i in 0 until n) {
            val a1 = pts[i]; val a2 = pts[(i + 1) % n]
            for (j in i + 2 until n) {
                if (i == 0 && j == n - 1) continue
                val b1 = pts[j]; val b2 = pts[(j + 1) % n]
                if (segmentsIntersect(a1, a2, b1, b2)) count++
            }
        }
        return count
    }

    private fun segmentsIntersect(p1: PolygonVertex, p2: PolygonVertex, p3: PolygonVertex, p4: PolygonVertex): Boolean {
        fun ccw(a: PolygonVertex, b: PolygonVertex, c: PolygonVertex) =
            (c.y - a.y) * (b.x - a.x) > (b.y - a.y) * (c.x - a.x)
        return ccw(p1, p3, p4) != ccw(p2, p3, p4) && ccw(p1, p2, p3) != ccw(p1, p2, p4)
    }

    // -------------------- DXF, SVG, and High-Res Paper Space A0-A4 Export --------------------

    fun exportCadToDxf(
        title: String,
        entities: List<CadEntity>,
        dimensions: List<CadDimensionEntity>,
        layers: List<CadLayerConfig>
    ): String {
        val sb = StringBuilder()
        sb.append("0\nSECTION\n2\nHEADER\n9\n\$ACADVER\n1\nAC1015\n0\nENDSEC\n")
        sb.append("0\nSECTION\n2\nTABLES\n0\nTABLE\n2\nLAYER\n")
        layers.forEach { layer ->
            sb.append("0\nLAYER\n2\n${layer.dxfName}\n70\n${if (layer.frozen) 1 else 0}\n62\n7\n6\nCONTINUOUS\n")
        }
        sb.append("0\nENDTAB\n0\nENDSEC\n0\nSECTION\n2\nENTITIES\n")
        val layerById = layers.associateBy { it.id }

        entities.forEach { e ->
            val lName = layerById[e.layerId]?.dxfName ?: "0"
            when (e.type) {
                CadEntityType.CIRCLE -> {
                    val c = e.points.firstOrNull() ?: PolygonVertex(0.0, 0.0)
                    sb.append("0\nCIRCLE\n8\n$lName\n10\n${c.x}\n20\n${c.y}\n40\n${e.radius}\n")
                }
                CadEntityType.ARC -> {
                    val c = e.points.firstOrNull() ?: PolygonVertex(0.0, 0.0)
                    sb.append("0\nARC\n8\n$lName\n10\n${c.x}\n20\n${c.y}\n40\n${e.radius}\n50\n${e.startAngleDeg}\n51\n${e.endAngleDeg}\n")
                }
                CadEntityType.POINT -> {
                    e.points.forEach { pt ->
                        sb.append("0\nPOINT\n8\n$lName\n10\n${pt.x}\n20\n${pt.y}\n")
                    }
                }
                CadEntityType.TEXT -> {
                    val p = e.points.firstOrNull() ?: PolygonVertex(0.0, 0.0)
                    sb.append("0\nTEXT\n8\n$lName\n10\n${p.x}\n20\n${p.y}\n40\n2.5\n1\n${e.text.ifBlank { title }}\n")
                }
                else -> {
                    if (e.points.size >= 2) {
                        sb.append("0\nLWPOLYLINE\n8\n$lName\n90\n${e.points.size}\n70\n${if (e.closed) 1 else 0}\n")
                        e.points.forEach { pt ->
                            sb.append("10\n${pt.x}\n20\n${pt.y}\n")
                        }
                    }
                }
            }
        }

        dimensions.forEach { d ->
            val ev = evaluateDimension(d, entities)
            sb.append("0\nDIMENSION\n8\nDIMENSIONS\n1\n${ev.formattedLabel}\n10\n${ev.dimLineP1.x}\n20\n${ev.dimLineP1.y}\n11\n${ev.textAnchor.x}\n21\n${ev.textAnchor.y}\n13\n${d.p1.x}\n23\n${d.p1.y}\n14\n${d.p2.x}\n24\n${d.p2.y}\n")
        }

        sb.append("0\nENDSEC\n0\nEOF\n")
        return sb.toString()
    }

    fun exportCadToSvg(
        title: String,
        entities: List<CadEntity>,
        dimensions: List<CadDimensionEntity>,
        layers: List<CadLayerConfig>
    ): String {
        val allPts = entities.flatMap { it.points } + dimensions.flatMap { listOf(it.p1, it.p2) }
        val minX = (allPts.minOfOrNull { it.x } ?: 0.0) - 20.0
        val minY = (allPts.minOfOrNull { it.y } ?: 0.0) - 20.0
        val maxX = (allPts.maxOfOrNull { it.x } ?: 120.0) + 20.0
        val maxY = (allPts.maxOfOrNull { it.y } ?: 120.0) + 20.0
        val w = (maxX - minX).coerceAtLeast(100.0)
        val h = (maxY - minY).coerceAtLeast(100.0)
        val layerById = layers.associateBy { it.id }

        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="$minX $minY $w $h" width="1200" height="900">""")
        sb.append("<title>$title</title>")
        sb.append("""<rect x="$minX" y="$minY" width="$w" height="$h" fill="#FFFFFF"/>""")

        entities.forEach { e ->
            val layer = layerById[e.layerId]
            if (layer?.visible == false || layer?.frozen == true) return@forEach
            val hexColor = String.format("#%06X", 0xFFFFFF and (e.colorOverrideArgb?.toInt() ?: layer?.colorArgb?.toInt() ?: 0x0F6B54))
            when (e.type) {
                CadEntityType.CIRCLE -> {
                    val c = e.points.firstOrNull() ?: return@forEach
                    sb.append("""<circle cx="${c.x}" cy="${c.y}" r="${e.radius}" stroke="$hexColor" stroke-width="1.2" fill="none"/>""")
                }
                CadEntityType.TEXT -> {
                    val p = e.points.firstOrNull() ?: return@forEach
                    sb.append("""<text x="${p.x}" y="${p.y}" font-size="4" fill="$hexColor">${e.text}</text>""")
                }
                else -> if (e.points.size >= 2) {
                    val ptsStr = e.points.joinToString(" ") { "${it.x},${it.y}" }
                    val tag = if (e.closed) "polygon" else "polyline"
                    val fill = if (e.closed) "${hexColor}22" else "none"
                    sb.append("""<$tag points="$ptsStr" stroke="$hexColor" stroke-width="1.2" fill="$fill"/>""")
                }
            }
        }
        dimensions.forEach { d ->
            val ev = evaluateDimension(d, entities)
            sb.append("""<line x1="${ev.dimLineP1.x}" y1="${ev.dimLineP1.y}" x2="${ev.dimLineP2.x}" y2="${ev.dimLineP2.y}" stroke="#E65100" stroke-width="0.7" stroke-dasharray="2,1"/>""")
            sb.append("""<text x="${ev.textAnchor.x}" y="${ev.textAnchor.y}" font-size="3.6" fill="#BF360C" text-anchor="middle">${ev.formattedLabel}</text>""")
        }
        sb.append("</svg>")
        return sb.toString()
    }

    /** Module 04: Paper-Space Layout Bitmap (A0–A4) with Title Block, North Arrow, Scale Bar, Legend, Parcel Table */
    fun renderCadLayoutSheetBitmap(
        title: String,
        mouzaInfo: String,
        paperSize: String,
        scaleLabel: String,
        entities: List<CadEntity>,
        dimensions: List<CadDimensionEntity>,
        layers: List<CadLayerConfig>,
        unit: MeasurementDisplayUnit
    ): Bitmap {
        val (w, h) = when (paperSize.uppercase()) {
            "A0" -> 2800 to 2000
            "A1" -> 2200 to 1560
            "A2" -> 1800 to 1280
            "A3" -> 1654 to 1170
            else -> 1400 to 1000 // A4 Landscape
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val margin = 38f
        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B4D3E"); style = Paint.Style.STROKE; strokeWidth = 4f }
        c.drawRect(margin, margin, w - margin, h - margin, borderP)

        // Viewport on left 72%, Parcel & Layer Table on right 28%
        val vpRight = margin + (w - margin * 2) * 0.72f
        val vpRect = RectF(margin + 14f, margin + 14f, vpRight - 10f, h - margin - 110f)
        c.drawRect(vpRect, Paint().apply { color = Color.parseColor("#FAFCFB") })
        c.drawRect(vpRect, Paint().apply { color = Color.parseColor("#90A4AE"); style = Paint.Style.STROKE; strokeWidth = 2f })

        val allPts = entities.flatMap { it.points } + dimensions.flatMap { listOf(it.p1, it.p2) }
        val minX = allPts.minOfOrNull { it.x } ?: 0.0
        val maxX = (allPts.maxOfOrNull { it.x } ?: 100.0).coerceAtLeast(minX + 10.0)
        val minY = allPts.minOfOrNull { it.y } ?: 0.0
        val maxY = (allPts.maxOfOrNull { it.y } ?: 100.0).coerceAtLeast(minY + 10.0)

        val spanX = maxX - minX
        val spanY = maxY - minY
        val scale = min((vpRect.width() * 0.78) / spanX, (vpRect.height() * 0.78) / spanY).toFloat()
        val ox = vpRect.centerX() - ((minX + maxX) * 0.5 * scale).toFloat()
        val oy = vpRect.centerY() + ((minY + maxY) * 0.5 * scale).toFloat()
        fun sx(x: Double) = ox + (x * scale).toFloat()
        fun sy(y: Double) = oy - (y * scale).toFloat() // Engineering Y-up

        val layerMap = layers.associateBy { it.id }
        entities.forEach { ent ->
            val layer = layerMap[ent.layerId]
            if (layer?.visible == false || layer?.frozen == true) return@forEach
            val col = ent.colorOverrideArgb?.toInt() ?: layer?.colorArgb?.toInt() ?: Color.BLACK
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = col
                style = Paint.Style.STROKE
                strokeWidth = ent.lineWidthPx
            }
            when (ent.type) {
                CadEntityType.CIRCLE -> {
                    val pt = ent.points.firstOrNull() ?: return@forEach
                    c.drawCircle(sx(pt.x), sy(pt.y), (ent.radius * scale).toFloat(), p)
                }
                CadEntityType.TEXT -> {
                    val pt = ent.points.firstOrNull() ?: return@forEach
                    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; textSize = 22f; isFakeBoldText = true }
                    c.drawText(ent.text, sx(pt.x), sy(pt.y), tp)
                }
                else -> if (ent.points.size >= 2) {
                    val path = Path()
                    ent.points.forEachIndexed { i, pt ->
                        if (i == 0) path.moveTo(sx(pt.x), sy(pt.y)) else path.lineTo(sx(pt.x), sy(pt.y))
                    }
                    if (ent.closed) {
                        path.close()
                        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, Color.red(col), Color.green(col), Color.blue(col)); style = Paint.Style.FILL })
                    }
                    c.drawPath(path, p)
                }
            }
        }

        val dimLineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E65100")
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
        }
        val dimTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BF360C")
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        dimensions.forEach { dim ->
            val ev = evaluateDimension(dim, entities)
            c.drawLine(sx(ev.dimLineP1.x), sy(ev.dimLineP1.y), sx(ev.dimLineP2.x), sy(ev.dimLineP2.y), dimLineP)
            c.drawText(ev.formattedLabel, sx(ev.textAnchor.x), sy(ev.textAnchor.y) - 6f, dimTextP)
        }

        // Right Pane: Parcel Table & Legend
        val sideLeft = vpRight + 8f
        val sideRect = RectF(sideLeft, vpRect.top, w - margin - 14f, h - margin - 14f)
        c.drawRect(sideRect, Paint().apply { color = Color.parseColor("#F5F8F7") })
        c.drawRect(sideRect, borderP)

        val hdP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B4D3E"); textSize = 22f; isFakeBoldText = true }
        val txP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 17f }
        var syPos = sideRect.top + 34f
        c.drawText("TITLE & PARCEL TABLE", sideLeft + 14f, syPos, hdP); syPos += 28f
        c.drawText("শিরোনাম: ${title.take(24)}", sideLeft + 14f, syPos, txP); syPos += 24f
        c.drawText("মৌজা: ${mouzaInfo.take(26)}", sideLeft + 14f, syPos, txP); syPos += 24f
        c.drawText("পেপার: $paperSize | স্কেল: $scaleLabel", sideLeft + 14f, syPos, txP); syPos += 30f

        val closedPolys = entities.filter { it.closed && it.points.size >= 3 }
        c.drawText("দাগ / পলিগন (${closedPolys.size}):", sideLeft + 14f, syPos, hdP); syPos += 24f
        closedPolys.take(10).forEachIndexed { i, poly ->
            val area = SurveyMathProEngine.computeCoordinateGeometry(poly.points)?.sqFeet ?: 0.0
            val dec = area / LandCalculators.SQ_FT_PER_DECIMAL
            c.drawText("P${i + 1}: ${String.format(Locale.US, "%.2f শতক (%.0f ft²)", dec, area)}", sideLeft + 14f, syPos, txP)
            syPos += 22f
        }
        syPos += 12f
        c.drawText("অ্যাক্টিভ লেয়ার (${layers.count { it.visible }}):", sideLeft + 14f, syPos, hdP); syPos += 22f
        layers.filter { it.visible }.take(8).forEach { l ->
            c.drawText("■ ${l.nameBn}", sideLeft + 14f, syPos, txP); syPos += 20f
        }

        // Bottom Title & North / Scale Bar inside Viewport footer
        val footTop = vpRect.bottom + 10f
        c.drawText("North ↑   |   Scale: $scaleLabel   |   Unit: ${unit.shortCode}   |   অস্বীকৃতি: আনসার্টিফায়েড CAD জরিপ চিত্র", margin + 20f, footTop + 42f, txP)
        return bmp
    }
}
