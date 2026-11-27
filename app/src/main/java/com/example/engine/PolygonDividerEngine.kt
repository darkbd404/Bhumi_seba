package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.model.DividedSubParcel
import com.example.model.DivisionShareSpec
import com.example.model.LandDivisionResultRecord
import com.example.model.LandSplitMode
import com.example.model.PolygonVertex
import com.example.model.RegionalUnitProfile
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object PolygonDividerEngine {

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

    val SAMPLE_POLYGONS: List<Pair<String, List<PolygonVertex>>> = listOf(
        "আয়তাকার / সামান্য বাঁকা প্লট (১২০'×৭৫')" to listOf(
            PolygonVertex(0.0, 0.0),
            PolygonVertex(120.0, 4.0),
            PolygonVertex(116.0, 75.0),
            PolygonVertex(-4.0, 72.0)
        ),
        "এল-আকৃতির (L-Shape) জমি" to listOf(
            PolygonVertex(0.0, 0.0),
            PolygonVertex(100.0, 0.0),
            PolygonVertex(100.0, 40.0),
            PolygonVertex(55.0, 40.0),
            PolygonVertex(55.0, 90.0),
            PolygonVertex(0.0, 90.0)
        ),
        "ট্রাপিজিয়াম / অনিয়মিত ৫-বাহু জমি" to listOf(
            PolygonVertex(10.0, 0.0),
            PolygonVertex(110.0, 12.0),
            PolygonVertex(128.0, 68.0),
            PolygonVertex(64.0, 96.0),
            PolygonVertex(0.0, 58.0)
        )
    )

    fun polygonAreaSqFt(vertices: List<PolygonVertex>): Double {
        if (vertices.size < 3) return 0.0
        var sum = 0.0
        for (i in vertices.indices) {
            val a = vertices[i]
            val b = vertices[(i + 1) % vertices.size]
            sum += (a.x * b.y) - (b.x * a.y)
        }
        return abs(sum) * 0.5
    }

    fun edgeLengths(vertices: List<PolygonVertex>): List<Double> {
        if (vertices.size < 2) return emptyList()
        return vertices.indices.map { i ->
            val a = vertices[i]
            val b = vertices[(i + 1) % vertices.size]
            hypot(b.x - a.x, b.y - a.y)
        }
    }

    fun hasSelfIntersection(vertices: List<PolygonVertex>): Boolean {
        val n = vertices.size
        if (n < 4) return false
        for (i in 0 until n) {
            val a1 = vertices[i]
            val a2 = vertices[(i + 1) % n]
            for (j in i + 2 until n) {
                if (i == 0 && j == n - 1) continue
                val b1 = vertices[j]
                val b2 = vertices[(j + 1) % n]
                if (segmentsIntersect(a1, a2, b1, b2)) return true
            }
        }
        return false
    }

    private fun segmentsIntersect(p1: PolygonVertex, p2: PolygonVertex, q1: PolygonVertex, q2: PolygonVertex): Boolean {
        fun orient(a: PolygonVertex, b: PolygonVertex, c: PolygonVertex): Double =
            (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        val o1 = orient(p1, p2, q1)
        val o2 = orient(p1, p2, q2)
        val o3 = orient(q1, q2, p1)
        val o4 = orient(q1, q2, p2)
        return (o1 * o2 < -1e-7) && (o3 * o4 < -1e-7)
    }

    /**
     * Clips polygon against half-plane: dot((x,y), (nx, ny)) <= limit (or >= limit if keepGreater=true).
     */
    fun clipPolygonByHalfPlane(
        poly: List<PolygonVertex>,
        nx: Double,
        ny: Double,
        limit: Double,
        keepGreater: Boolean
    ): List<PolygonVertex> {
        if (poly.size < 3) return emptyList()
        fun inside(p: PolygonVertex): Boolean {
            val d = p.x * nx + p.y * ny
            return if (keepGreater) d >= limit - 1e-9 else d <= limit + 1e-9
        }
        fun intersect(a: PolygonVertex, b: PolygonVertex): PolygonVertex {
            val da = (a.x * nx + a.y * ny) - limit
            val db = (b.x * nx + b.y * ny) - limit
            val denom = da - db
            val t = if (abs(denom) < 1e-12) 0.5 else (da / denom).coerceIn(0.0, 1.0)
            return PolygonVertex(
                x = a.x + t * (b.x - a.x),
                y = a.y + t * (b.y - a.y)
            )
        }

        val out = ArrayList<PolygonVertex>(poly.size + 2)
        var prev = poly.last()
        var prevIn = inside(prev)
        for (curr in poly) {
            val currIn = inside(curr)
            if (currIn) {
                if (!prevIn) out.add(intersect(prev, curr))
                out.add(curr)
            } else if (prevIn) {
                out.add(intersect(prev, curr))
            }
            prev = curr
            prevIn = currIn
        }
        return out.filterIndexed { idx, v ->
            val next = out[(idx + 1) % out.size]
            out.size <= 3 || hypot(v.x - next.x, v.y - next.y) > 1e-6
        }
    }

    /**
     * Splits a polygon cleanly into (leftPiece with targetAreaSqFt, rightPiece with remaining area)
     * along unit normal (cos(angle), sin(angle)).
     */
    fun bisectPolygonByArea(
        poly: List<PolygonVertex>,
        targetAreaSqFt: Double,
        cutAngleDeg: Double
    ): Pair<List<PolygonVertex>, List<PolygonVertex>> {
        val total = polygonAreaSqFt(poly)
        if (poly.size < 3 || total <= 1e-6) return emptyList<PolygonVertex>() to emptyList()
        if (targetAreaSqFt <= 1e-6) return emptyList<PolygonVertex>() to poly
        if (targetAreaSqFt >= total - 1e-6) return poly to emptyList()

        val rad = Math.toRadians(cutAngleDeg)
        val nx = cos(rad)
        val ny = sin(rad)
        val projs = poly.map { it.x * nx + it.y * ny }
        var lo = projs.min()
        var hi = projs.max()

        repeat(52) {
            val mid = (lo + hi) * 0.5
            val piece = clipPolygonByHalfPlane(poly, nx, ny, mid, keepGreater = false)
            val area = polygonAreaSqFt(piece)
            if (area < targetAreaSqFt) {
                lo = mid
            } else {
                hi = mid
            }
        }
        val splitLimit = (lo + hi) * 0.5
        val firstPiece = clipPolygonByHalfPlane(poly, nx, ny, splitLimit, keepGreater = false)
        val secondPiece = clipPolygonByHalfPlane(poly, nx, ny, splitLimit, keepGreater = true)
        return firstPiece to secondPiece
    }

    /**
     * Divides an arbitrary polygon into N parts according to Equal, Percentage, Fixed Decimal,
     * or Fixed Katha rules, optionally carving out a road corridor of exact `roadWidthFt`.
     */
    fun dividePolygon(
        id: String,
        title: String,
        vertices: List<PolygonVertex>,
        mode: LandSplitMode,
        shares: List<DivisionShareSpec>,
        roadWidthFt: Double = 0.0,
        roadSideAngleDeg: Double = 90.0, // 90° carves road along bottom/south edge
        cutAngleDeg: Double = 0.0,       // 0° divides West -> East
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): LandDivisionResultRecord? {
        if (vertices.size < 3 || hasSelfIntersection(vertices)) return null
        val originalArea = polygonAreaSqFt(vertices)
        if (originalArea <= 1e-3 || shares.isEmpty()) return null

        // Carve road corridor from the outer boundary projection if roadWidthFt > 0
        var usablePoly = vertices
        var roadPoly: List<PolygonVertex> = emptyList()
        if (roadWidthFt > 0.0) {
            val rRad = Math.toRadians(roadSideAngleDeg)
            val rnx = cos(rRad)
            val rny = sin(rRad)
            val minProj = vertices.minOf { it.x * rnx + it.y * rny }
            val maxProj = vertices.maxOf { it.x * rnx + it.y * rny }
            if (maxProj - minProj > roadWidthFt + 1.0) {
                val limit = minProj + roadWidthFt
                roadPoly = clipPolygonByHalfPlane(vertices, rnx, rny, limit, keepGreater = false)
                usablePoly = clipPolygonByHalfPlane(vertices, rnx, rny, limit, keepGreater = true)
            }
        }

        val usableArea = polygonAreaSqFt(usablePoly)
        if (usableArea <= 1e-3) return null
        val roadArea = (originalArea - usableArea).coerceAtLeast(0.0)

        // Compute target SqFt per owner
        val targetSqFts: List<Double> = when (mode) {
            LandSplitMode.EQUAL -> {
                val totalWeight = shares.sumOf { it.value.coerceAtLeast(1.0) }
                shares.map { usableArea * (it.value.coerceAtLeast(1.0) / totalWeight) }
            }
            LandSplitMode.PERCENTAGE -> {
                val sumPct = shares.sumOf { it.value.coerceAtLeast(0.0) }
                val scale = if (sumPct > 100.0) 100.0 / sumPct else 1.0
                shares.map { usableArea * ((it.value.coerceAtLeast(0.0) * scale) / 100.0) }
            }
            LandSplitMode.FIXED_DECIMAL -> {
                shares.map { it.value.coerceAtLeast(0.0) * LandCalculators.SQ_FT_PER_DECIMAL }
            }
            LandSplitMode.FIXED_KATHA -> {
                shares.map { it.value.coerceAtLeast(0.0) * unitProfile.kathaSqFt }
            }
        }

        val parcels = mutableListOf<DividedSubParcel>()
        var remainingPoly = usablePoly
        var allocatedTotal = 0.0

        for (i in shares.indices) {
            val remArea = polygonAreaSqFt(remainingPoly)
            if (remArea <= 1e-4) break
            val desired = min(targetSqFts[i], remArea)
            if (desired <= 1e-4) continue

            val isExactFullExhaustion = (i == shares.lastIndex) &&
                (mode == LandSplitMode.EQUAL || abs(desired - remArea) < 0.05)

            val (piecePoly, nextRem) = if (isExactFullExhaustion) {
                remainingPoly to emptyList()
            } else {
                bisectPolygonByArea(remainingPoly, desired, cutAngleDeg)
            }

            val pieceArea = polygonAreaSqFt(piecePoly)
            if (pieceArea > 1e-4 && piecePoly.size >= 3) {
                val edges = edgeLengths(piecePoly)
                allocatedTotal += pieceArea
                parcels.add(
                    DividedSubParcel(
                        index = i + 1,
                        ownerName = shares[i].ownerName.ifBlank { "অংশ ${i + 1}" },
                        areaSqFt = pieceArea,
                        areaDecimal = pieceArea / LandCalculators.SQ_FT_PER_DECIMAL,
                        areaKatha = pieceArea / unitProfile.kathaSqFt,
                        perimeterFt = edges.sum(),
                        edgeLengthsFt = edges,
                        vertices = piecePoly,
                        colorArgb = PARCEL_COLORS[i % PARCEL_COLORS.size]
                    )
                )
            }
            remainingPoly = nextRem
        }

        val remainingArea = polygonAreaSqFt(remainingPoly)
        val totalCheck = allocatedTotal + remainingArea
        val conservationErr = abs(totalCheck - usableArea)
        val toleranceSqFt = max(0.5, usableArea * 0.0005) // 0.05% or 0.5 sq.ft tolerance

        return LandDivisionResultRecord(
            id = id,
            title = title.ifBlank { "স্বয়ংক্রিয় জমি বিভাজন" },
            originalAreaSqFt = originalArea,
            roadReservedSqFt = roadArea,
            usableAreaSqFt = usableArea,
            allocatedAreaSqFt = allocatedTotal,
            remainingAreaSqFt = remainingArea,
            conservationErrorSqFt = conservationErr,
            isValidConservation = conservationErr <= toleranceSqFt,
            roadWidthFt = roadWidthFt,
            cutAngleDeg = cutAngleDeg,
            parcels = parcels,
            roadPolygon = roadPoly
        )
    }

    fun exportDivisionDiagram(
        context: Context?,
        record: LandDivisionResultRecord,
        asPdf: Boolean,
        desktopFallbackDir: File? = null
    ): ExportedFileResult {
        val width = 1800
        val height = 1350
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6B54")
            textSize = 42f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 26f
        }
        canvas.drawText(record.title, 60f, 70f, titlePaint)
        canvas.drawText(
            String.format(
                Locale.US,
                "মোট: %.2f শতক | রাস্তা: %.2f শতক | নিট বণ্টন: %.2f শতক (ত্রুটি: %.3f ব.ফু)",
                record.originalAreaSqFt / 435.6,
                record.roadReservedSqFt / 435.6,
                record.allocatedAreaSqFt / 435.6,
                record.conservationErrorSqFt
            ),
            60f,
            115f,
            subPaint
        )

        val allPts = (record.parcels.flatMap { it.vertices } + record.roadPolygon)
        if (allPts.isNotEmpty()) {
            val minX = allPts.minOf { it.x }
            val maxX = allPts.maxOf { it.x }
            val minY = allPts.minOf { it.y }
            val maxY = allPts.maxOf { it.y }
            val spanX = (maxX - minX).coerceAtLeast(1.0)
            val spanY = (maxY - minY).coerceAtLeast(1.0)
            val drawLeft = 80f
            val drawTop = 160f
            val drawW = width - 160f
            val drawH = height - 420f
            val scale = min(drawW / spanX, drawH / spanY)

            fun toScreen(v: PolygonVertex): Pair<Float, Float> {
                val sx = (drawLeft + (v.x - minX) * scale).toFloat()
                val sy = (drawTop + (maxY - v.y) * scale).toFloat()
                return sx to sy
            }

            if (record.roadPolygon.size >= 3) {
                val roadFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#CFD8DC")
                    style = Paint.Style.FILL
                }
                val roadStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#546E7A")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val path = Path()
                record.roadPolygon.forEachIndexed { idx, v ->
                    val (sx, sy) = toScreen(v)
                    if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                }
                path.close()
                canvas.drawPath(path, roadFill)
                canvas.drawPath(path, roadStroke)
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 26f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            record.parcels.forEach { parcel ->
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = parcel.colorArgb.toInt()
                    alpha = 72
                    style = Paint.Style.FILL
                }
                val path = Path()
                var sumX = 0f
                var sumY = 0f
                parcel.vertices.forEachIndexed { idx, v ->
                    val (sx, sy) = toScreen(v)
                    sumX += sx
                    sumY += sy
                    if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                }
                path.close()
                canvas.drawPath(path, fill)
                canvas.drawPath(path, borderPaint)

                val cx = sumX / parcel.vertices.size
                val cy = sumY / parcel.vertices.size
                canvas.drawText(parcel.ownerName, cx, cy - 10f, labelPaint)
                canvas.drawText(
                    String.format(Locale.US, "%.2f শতক", parcel.areaDecimal),
                    cx,
                    cy + 24f,
                    labelPaint
                )
            }
        }

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 22f
        }
        canvas.drawText(
            "বিঃদ্রঃ এটি আনুমানিক নকশা বিভাজন — চূড়ান্ত সীমানা নির্ধারণে সরেজমিন জরিপ ও আমিন যাচাই আবশ্যক।",
            60f,
            height - 60f,
            footPaint
        )

        val fileName = "Land_Division_${System.currentTimeMillis()}"
        val res = if (asPdf) {
            ExportStreamWriter.savePdfBitmap(context, bmp, fileName, desktopFallbackDir = desktopFallbackDir)
        } else {
            ExportStreamWriter.savePngBitmap(context, bmp, fileName, desktopFallbackDir = desktopFallbackDir)
        }
        return ExportedFileResult(res.uri, res.displayPath)
    }
}
