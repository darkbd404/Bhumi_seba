package com.example.engine

import com.example.model.AdjacentDagNeighbour
import com.example.model.KhajnaHoldingRecord
import com.example.model.LevelReadingEntry
import com.example.model.PolygonVertex
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// -------------------- Part A Math --------------------

data class OldMapCalibrationReport(
    val pixelsPerFoot: Double,
    val mapAreaSqFt: Double,
    val mapAreaDecimal: Double,
    val scaleDistortionPct: Double,
    val uncertaintySqFt: Double,
    val correctedPolygonAreaSqFt: Double
)

// -------------------- Part B Math --------------------

data class TotalStationShotResult(
    val horizontalDistFt: Double,
    val verticalDiffFt: Double,
    val targetNorthFt: Double,
    val targetEastFt: Double,
    val targetElevationFt: Double
)

data class MissingLineResult(
    val slopeDistFt: Double,
    val horizontalDistFt: Double,
    val verticalDiffFt: Double,
    val bearingDeg: Double,
    val gradePct: Double
)

data class LevelBookComputedRow(
    val station: String,
    val chainageFt: Double,
    val bs: Double?,
    val isReading: Double?,
    val fs: Double?,
    val hi: Double?,
    val rise: Double?,
    val fall: Double?,
    val rl: Double,
    val remarks: String
)

data class LevelBookSummary(
    val rows: List<LevelBookComputedRow>,
    val sumBs: Double,
    val sumFs: Double,
    val sumRise: Double,
    val sumFall: Double,
    val rlDifference: Double,
    val closingError: Double,
    val arithmeticBalanced: Boolean
)

data class TransitTraverseResult(
    val misclosureFt: Double,
    val precisionRatio: String,
    val bowditchAreaSqFt: Double,
    val transitAreaSqFt: Double
)

// -------------------- Part F Math --------------------

data class AreaReconciliationReport(
    val netRecordDecimal: Double,
    val physicalMeasuredDecimal: Double,
    val differenceDecimal: Double,
    val statusLabel: String,
    val discrepancyPct: Double
)

data class UsableLandReport(
    val grossAreaSqFt: Double,
    val roadDeductionSqFt: Double,
    val obstructionDeductionSqFt: Double,
    val netUsableSqFt: Double,
    val netUsableDecimal: Double,
    val netUsableKatha: Double
)

data class EarthworkCutFillResult(
    val lengthFt: Double,
    val existingAreaSqFt: Double,
    val proposedAreaSqFt: Double,
    val netVolumeCuFt: Double,
    val isFill: Boolean,
    val slopePct: Double
)

object ExpansionMathAndSurveyEngine {

    /** Part A02: Calibrates old map pixels into real feet + detects scale/scan distortion & uncertainty */
    fun analyzeOldMapMeasurement(
        pixelSpanMeasured: Double,
        knownGroundDistFt: Double,
        secondaryPixelSpan: Double? = null,
        secondaryKnownDistFt: Double? = null,
        polygonPixelArea: Double,
        tiltCorrectionCosFactor: Double = 1.0,
        scannerResolutionDpi: Double = 300.0
    ): OldMapCalibrationReport {
        val safePx = pixelSpanMeasured.coerceAtLeast(1.0)
        val safeFt = knownGroundDistFt.coerceAtLeast(0.1)
        val ppf1 = safePx / safeFt
        val ppf2 = if (secondaryPixelSpan != null && secondaryKnownDistFt != null && secondaryKnownDistFt > 0.1) {
            secondaryPixelSpan / secondaryKnownDistFt
        } else ppf1
        val avgPpf = (ppf1 + ppf2) * 0.5
        val distortionPct = if (avgPpf > 0.0) (abs(ppf1 - ppf2) / avgPpf) * 100.0 else 0.0

        val rawSqFt = polygonPixelArea.coerceAtLeast(0.0) / (avgPpf * avgPpf)
        val correctedSqFt = rawSqFt / tiltCorrectionCosFactor.coerceIn(0.5, 1.0)
        val pixelUncertaintyFt = (1.5 / avgPpf).coerceAtLeast(0.15) * (300.0 / scannerResolutionDpi.coerceIn(72.0, 2400.0))
        val approxPerim = 4.0 * sqrt(correctedSqFt.coerceAtLeast(0.0))
        val uncertaintySqFt = approxPerim * pixelUncertaintyFt * 0.5 + (correctedSqFt * (distortionPct / 100.0))

        return OldMapCalibrationReport(
            pixelsPerFoot = avgPpf,
            mapAreaSqFt = rawSqFt,
            mapAreaDecimal = correctedSqFt / LandCalculators.SQ_FT_PER_DECIMAL,
            scaleDistortionPct = distortionPct,
            uncertaintySqFt = uncertaintySqFt,
            correctedPolygonAreaSqFt = correctedSqFt
        )
    }

    /** Part A05: Verify 4-side descriptions against digitized neighbours */
    fun summarizeAdjacency(neighbours: List<AdjacentDagNeighbour>): Pair<Double, List<String>> {
        val totalShared = neighbours.sumOf { it.sharedLengthFt.coerceAtLeast(0.0) }
        val warnings = mutableListOf<String>()
        listOf("উত্তর", "দক্ষিণ", "পূর্ব", "পশ্চিম").forEach { dir ->
            val match = neighbours.find { it.direction.contains(dir, true) }
            if (match == null || match.neighbourDagNo.isBlank()) {
                warnings.add("$dir দিকের প্রতিবেশী দাগ নম্বর উল্লেখ নেই")
            } else if (!match.existsInDigitizedMap) {
                warnings.add("$dir দিকের দাগ ${match.neighbourDagNo} ডিজিটাইজড ম্যাপে পাওয়া যায়নি")
            }
        }
        return totalShared to warnings
    }

    /** Part B01: Total Station 3D coordinate & elevation reduction */
    fun computeTotalStationShot(
        stationN: Double,
        stationE: Double,
        stationRl: Double,
        instrumentHeight: Double,
        prismHeight: Double,
        horizontalAzimuthDeg: Double,
        verticalAngleFromHorizDeg: Double,
        slopeDistFt: Double,
        prismConstantFt: Double = 0.0
    ): TotalStationShotResult {
        val corrSlope = (slopeDistFt + prismConstantFt).coerceAtLeast(0.0)
        val vRad = Math.toRadians(verticalAngleFromHorizDeg)
        val hDist = corrSlope * cos(vRad)
        val vComp = corrSlope * sin(vRad)
        val dZ = vComp + instrumentHeight - prismHeight
        val azRad = Math.toRadians(horizontalAzimuthDeg)
        val targetN = stationN + hDist * cos(azRad)
        val targetE = stationE + hDist * sin(azRad)
        val targetRl = stationRl + dZ
        return TotalStationShotResult(hDist, dZ, targetN, targetE, targetRl)
    }

    /** Part B01 & B04: Missing Line Measurement (MLM) between two observed coordinates */
    fun computeMissingLine(
        n1: Double, e1: Double, rl1: Double,
        n2: Double, e2: Double, rl2: Double
    ): MissingLineResult {
        val dN = n2 - n1
        val dE = e2 - e1
        val dZ = rl2 - rl1
        val hDist = hypot(dN, dE)
        val sDist = sqrt(hDist * hDist + dZ * dZ)
        val bearing = (Math.toDegrees(atan2(dE, dN)) + 360.0) % 360.0
        val grade = if (hDist > 1e-6) (dZ / hDist) * 100.0 else 0.0
        return MissingLineResult(sDist, hDist, dZ, bearing, grade)
    }

    /** Part B01: Bowditch & Transit Rule Traverse Comparison */
    fun solveBowditchAndTransit(legs: List<TraverseLegInput>): TransitTraverseResult? {
        val bowditch = SurveyMathProEngine.solveClosedTraverse(legs) ?: return null
        val deltas = legs.map { leg ->
            val rad = Math.toRadians(leg.bearingDeg)
            (leg.distanceFt * cos(rad)) to (leg.distanceFt * sin(rad))
        }
        val errN = deltas.sumOf { it.first }
        val errE = deltas.sumOf { it.second }
        val sumAbsN = deltas.sumOf { abs(it.first) }.coerceAtLeast(1e-6)
        val sumAbsE = deltas.sumOf { abs(it.second) }.coerceAtLeast(1e-6)

        var curN = 0.0
        var curE = 0.0
        val transitVerts = mutableListOf(PolygonVertex(0.0, 0.0))
        deltas.forEachIndexed { idx, (dN, dE) ->
            curN += dN - errN * (abs(dN) / sumAbsN)
            curE += dE - errE * (abs(dE) / sumAbsE)
            if (idx < deltas.lastIndex) transitVerts.add(PolygonVertex(curE, curN))
        }
        val transitGeom = SurveyMathProEngine.computeCoordinateGeometry(transitVerts)
        return TransitTraverseResult(
            misclosureFt = bowditch.linearMisclosureFt,
            precisionRatio = bowditch.precisionRatio,
            bowditchAreaSqFt = bowditch.adjustedAreaSqFt,
            transitAreaSqFt = transitGeom?.sqFeet ?: bowditch.adjustedAreaSqFt
        )
    }

    /** Part B02: Theoretical interior angle sum check & per-station angular correction */
    fun checkPolygonAngularMisclosure(interiorAnglesDeg: List<Double>): Triple<Double, Double, Double> {
        val n = interiorAnglesDeg.size
        if (n < 3) return Triple(0.0, 0.0, 0.0)
        val theoretical = (n - 2) * 180.0
        val observed = interiorAnglesDeg.sum()
        val misclosure = observed - theoretical
        val perAngleCorr = -misclosure / n
        return Triple(theoretical, misclosure, perAngleCorr)
    }

    /** Part B03: Auto Level Book reduction supporting Height of Instrument (HI) & Rise/Fall */
    fun computeLevelBook(
        benchmarkRl: Double,
        entries: List<LevelReadingEntry>,
        knownClosingBenchmarkRl: Double? = null
    ): LevelBookSummary {
        if (entries.isEmpty()) {
            return LevelBookSummary(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true)
        }
        val rows = mutableListOf<LevelBookComputedRow>()
        var currentRl = benchmarkRl
        var currentHi: Double? = null
        var prevStaff: Double? = null
        var sumBs = 0.0
        var sumFs = 0.0
        var sumRise = 0.0
        var sumFall = 0.0

        entries.forEachIndexed { idx, e ->
            var rise: Double? = null
            var fall: Double? = null

            val targetReading = e.fs ?: e.intermediateSight
            if (idx == 0 && e.bs != null) {
                currentRl = benchmarkRl
                currentHi = benchmarkRl + e.bs
                prevStaff = e.bs
                sumBs += e.bs
            } else {
                if (targetReading != null && currentHi != null) {
                    currentRl = currentHi!! - targetReading
                    if (prevStaff != null) {
                        val diff = prevStaff!! - targetReading
                        if (diff >= 0) {
                            rise = diff; sumRise += diff
                        } else {
                            fall = -diff; sumFall += (-diff)
                        }
                    }
                    prevStaff = targetReading
                }
                if (e.fs != null) sumFs += e.fs
                if (e.bs != null) {
                    // Change point: reset HI using current RL + new BS
                    currentHi = currentRl + e.bs
                    prevStaff = e.bs
                    sumBs += e.bs
                }
            }

            rows.add(
                LevelBookComputedRow(
                    station = e.station,
                    chainageFt = e.chainageFt,
                    bs = e.bs,
                    isReading = e.intermediateSight,
                    fs = e.fs,
                    hi = currentHi,
                    rise = rise,
                    fall = fall,
                    rl = currentRl,
                    remarks = e.remarks
                )
            )
        }

        val rlDiff = currentRl - benchmarkRl
        val bsMinusFs = sumBs - sumFs
        val riseMinusFall = sumRise - sumFall
        val balanced = abs(bsMinusFs - rlDiff) < 0.005 && abs(riseMinusFall - rlDiff) < 0.005
        val closingError = if (knownClosingBenchmarkRl != null) currentRl - knownClosingBenchmarkRl else 0.0

        return LevelBookSummary(
            rows = rows,
            sumBs = sumBs,
            sumFs = sumFs,
            sumRise = sumRise,
            sumFall = sumFall,
            rlDifference = rlDiff,
            closingError = closingError,
            arithmeticBalanced = balanced
        )
    }

    /** Part B07: Cut & Fill Earthwork Volume & Slope Gradient */
    fun computeEarthworkCutFill(
        sectionLengthFt: Double,
        widthFt: Double,
        existingElevStartFt: Double,
        existingElevEndFt: Double,
        proposedElevStartFt: Double,
        proposedElevEndFt: Double
    ): EarthworkCutFillResult {
        val len = sectionLengthFt.coerceAtLeast(0.1)
        val wid = widthFt.coerceAtLeast(0.1)
        val existAvg = (existingElevStartFt + existingElevEndFt) * 0.5
        val propAvg = (proposedElevStartFt + proposedElevEndFt) * 0.5
        val a1 = (proposedElevStartFt - existingElevStartFt) * wid
        val a2 = (proposedElevEndFt - existingElevEndFt) * wid
        val vol = len * (a1 + a2) * 0.5
        val slopePct = ((existingElevEndFt - existingElevStartFt) / len) * 100.0
        return EarthworkCutFillResult(
            lengthFt = len,
            existingAreaSqFt = existAvg * wid,
            proposedAreaSqFt = propAvg * wid,
            netVolumeCuFt = abs(vol),
            isFill = vol >= 0.0,
            slopePct = slopePct
        )
    }

    /** Part C05: Khajna Holding Arrears + Duplicate Holding Check */
    fun computeHoldingKhajna(h: KhajnaHoldingRecord): Triple<Double, Double, Double> {
        val annual = h.areaDecimal.coerceAtLeast(0.0) * h.annualRatePerDecimalBdt.coerceAtLeast(0.0)
        val arrears = annual * h.arrearsYears.coerceAtLeast(0)
        val total = annual + arrears
        return Triple(annual, arrears, total)
    }

    fun detectDuplicateHoldings(holdings: List<KhajnaHoldingRecord>): List<String> {
        val seen = mutableMapOf<String, Int>()
        val dupes = mutableListOf<String>()
        holdings.forEach { h ->
            val key = "${h.mouza.trim().lowercase()}|${h.khatianNo.trim()}|${h.dagNo.trim()}"
            val c = (seen[key] ?: 0) + 1
            seen[key] = c
            if (c == 2) dupes.add("ডুপ্লিকেট খাজনা হোল্ডিং সতর্কতা: মৌজা ${h.mouza}, খতিয়ান ${h.khatianNo}, দাগ ${h.dagNo}")
        }
        return dupes
    }

    /** Part F01 & F02: Old Record vs New Survey / Inherited / Sold Reconciliation */
    fun reconcileLandRecords(
        recordedKhatianDecimal: Double,
        inheritedOrAcquiredDecimal: Double,
        soldOrTransferredDecimal: Double,
        physicallyMeasuredDecimal: Double
    ): AreaReconciliationReport {
        val base = if (inheritedOrAcquiredDecimal > 0.0) inheritedOrAcquiredDecimal else recordedKhatianDecimal
        val expectedRemaining = (base - soldOrTransferredDecimal).coerceAtLeast(0.0)
        val diff = physicallyMeasuredDecimal - expectedRemaining
        val pct = if (expectedRemaining > 1e-6) (abs(diff) / expectedRemaining) * 100.0 else 0.0
        val label = when {
            abs(diff) < 0.05 -> "সঠিক ও সামঞ্জস্যপূর্ণ (Balanced)"
            diff > 0 -> String.format(Locale.US, "সরেজমিন বাড়তি (+%.3f শতক Excess)", diff)
            else -> String.format(Locale.US, "সরেজমিন ঘাটতি (%.3f শতক Shortage)", diff)
        }
        return AreaReconciliationReport(
            netRecordDecimal = expectedRemaining,
            physicalMeasuredDecimal = physicallyMeasuredDecimal,
            differenceDecimal = diff,
            statusLabel = label,
            discrepancyPct = pct
        )
    }

    /** Part F05 & F06: Road Access + Waterbody/Building Obstruction Net Usable Area */
    fun computeNetUsableLand(
        grossSqFt: Double,
        roadLengthFt: Double,
        roadWidthFt: Double,
        pondSqFt: Double,
        canalSqFt: Double,
        buildingSqFt: Double,
        otherRestrictionSqFt: Double,
        kathaSqFt: Double = 720.0
    ): UsableLandReport {
        val roadDed = (roadLengthFt.coerceAtLeast(0.0) * roadWidthFt.coerceAtLeast(0.0)).coerceAtMost(grossSqFt)
        val obsDed = (pondSqFt + canalSqFt + buildingSqFt + otherRestrictionSqFt).coerceAtLeast(0.0)
        val net = (grossSqFt - roadDed - obsDed).coerceAtLeast(0.0)
        return UsableLandReport(
            grossAreaSqFt = grossSqFt,
            roadDeductionSqFt = roadDed,
            obstructionDeductionSqFt = obsDed,
            netUsableSqFt = net,
            netUsableDecimal = net / LandCalculators.SQ_FT_PER_DECIMAL,
            netUsableKatha = net / kathaSqFt.coerceAtLeast(100.0)
        )
    }
}
