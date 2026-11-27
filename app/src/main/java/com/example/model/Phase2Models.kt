package com.example.model

import kotlinx.serialization.Serializable

// ==================== M06: PRO LAND DIVISION MODELS ====================

@Serializable
enum class DivisionGeometryCutMode(val titleBn: String) {
    PARALLEL_LINE("সমান্তরাল রেখা (Parallel)"),
    RADIAL_CORNER("কৌণিক বিকিরণ (Radial Corner)"),
    THROUGH_POINT("নির্দিষ্ট বিন্দুগামী (Through Point)"),
    FIXED_FRONTAGE("নির্দিষ্ট রাস্তার মুখ (Frontage)"),
    CORNER_FIXED_AREA("কোণ থেকে নির্দিষ্ট শতক"),
    VALUE_WEIGHTED("মূল্য ওজন ভিত্তিক (Value Weighted)")
}

@Serializable
enum class CompassAllocationDirection(val titleBn: String) {
    AUTO("স্বয়ংক্রিয় ক্রম"),
    NORTH("উত্তর অংশ"),
    SOUTH("দক্ষিণ অংশ"),
    EAST("পূর্ব অংশ"),
    WEST("পশ্চিম অংশ"),
    ROAD_FRONT("রাস্তার সম্মুখ অংশ")
}

@Serializable
data class HoleCutoutPolygon(
    val id: String,
    val label: String = "পুকুর / ঘর (বাদ)",
    val vertices: List<PolygonVertex>
)

@Serializable
data class ParcelFourBoundaries(
    val north: String = "",
    val south: String = "",
    val east: String = "",
    val west: String = ""
)

@Serializable
data class RoadFrontageComparisonItem(
    val parcelIndex: Int,
    val ownerName: String,
    val frontageFt: Double,
    val frontageRatioPct: Double
)

@Serializable
data class ProDividedSubParcel(
    val index: Int,
    val ownerName: String,
    val grossAreaSqFt: Double,
    val excludedHoleSqFt: Double,
    val netAreaSqFt: Double,
    val netDecimal: Double,
    val netKatha: Double,
    val perimeterFt: Double,
    val edgeLengthsFt: List<Double>,
    val roadFrontageFt: Double,
    val valueWeight: Double = 1.0,
    val weightedScore: Double = 0.0,
    val allocatedDirection: CompassAllocationDirection = CompassAllocationDirection.AUTO,
    val boundaries: ParcelFourBoundaries = ParcelFourBoundaries(),
    val vertices: List<PolygonVertex>,
    val colorArgb: Long
)

@Serializable
data class ProDivisionReport(
    val id: String,
    val title: String,
    val cutMode: DivisionGeometryCutMode,
    val grossOuterAreaSqFt: Double,
    val totalHoleExcludedSqFt: Double,
    val roadReservedSqFt: Double,
    val usableNetAreaSqFt: Double,
    val allocatedNetAreaSqFt: Double,
    val remainingNetAreaSqFt: Double,
    val conservationErrorSqFt: Double,
    val isValidConservation: Boolean,
    val totalRoadFrontageBeforeFt: Double,
    val totalRoadFrontageAfterFt: Double,
    val frontageReport: List<RoadFrontageComparisonItem>,
    val parcels: List<ProDividedSubParcel>,
    val roadPolygon: List<PolygonVertex> = emptyList(),
    val holes: List<HoleCutoutPolygon> = emptyList()
)

@Serializable
data class QuadMasterSpec(
    val northFt: Double,
    val southFt: Double,
    val eastFt: Double,
    val westFt: Double,
    val useDiagonalMode: Boolean = true,
    val diagonalAcFt: Double = 0.0,
    val cornerAngleSwDeg: Double = 90.0
)

@Serializable
data class QuadMasterResult(
    val vertices: List<PolygonVertex>,
    val totalSqFt: Double,
    val totalDecimal: Double,
    val totalKatha: Double,
    val diagonalAcFt: Double,
    val diagonalBdFt: Double,
    val interiorAnglesDeg: List<Double>,
    val edgeLengthsFt: List<Double>,
    val splitParcels: List<ProDividedSubParcel>
)

@Serializable
data class PlotPlannerOptionConfig(
    val optionLabel: String,
    val targetPlotKatha: Double,
    val roadWidthFt: Double,
    val drainOpenSpacePct: Double,
    val aspectRatioWidthToDepth: Double = 0.65
)

@Serializable
data class PlotPlannerOptionSummary(
    val optionLabel: String,
    val targetPlotKatha: Double,
    val targetPlotDecimal: Double,
    val grossAreaSqFt: Double,
    val roadAndDrainSqFt: Double,
    val netSalableSqFt: Double,
    val plotCount: Int,
    val salablePlotsAreaSqFt: Double,
    val remainingWasteSqFt: Double,
    val roadFrontagePerPlotFt: Double,
    val plotDepthFt: Double,
    val efficiencyPct: Double,
    val plotCells: List<List<PolygonVertex>> = emptyList(),
    val roadCorridors: List<List<PolygonVertex>> = emptyList()
)

@Serializable
data class TafsilDagInput(
    val id: String,
    val khatianNo: String,
    val dagNo: String,
    val landClass: String,
    val totalDecimal: Double,
    val ownerShareInDag: Double = 1.0
) {
    val effectiveDecimal: Double get() = (totalDecimal * ownerShareInDag).coerceAtLeast(0.0)
}

@Serializable
data class TafsilOwnerShareSpec(
    val ownerName: String,
    val shareRatio: Double
)

@Serializable
data class TafsilAllocationRow(
    val ownerName: String,
    val khatianNo: String,
    val dagNo: String,
    val landClass: String,
    val allocatedDecimal: Double,
    val allocatedKatha: Double,
    val sharePct: Double
)

@Serializable
data class TafsilDivisionReport(
    val totalEffectiveDecimal: Double,
    val totalAllocatedDecimal: Double,
    val ownerTotalsDecimal: Map<String, Double>,
    val rows: List<TafsilAllocationRow>,
    val isShareSumBalanced: Boolean
)

// ==================== M07: INHERITANCE, HISSA & FORMULA MODELS ====================

@Serializable
data class AnaGondaBreakdown(
    val ratio: Double,
    val percentage: Double,
    val ana: Int,
    val gonda: Int,
    val kora: Int,
    val kranti: Int,
    val til: Int
) {
    fun formattedBn(): String = "$ana আনা $gonda গণ্ডা $kora কড়া $kranti ক্রান্তি $til তিল"
}

@Serializable
data class HanafiHeirInput(
    val totalDecimal: Double,
    val deceasedIsMale: Boolean = true,
    val spouseCount: Int = 1, // 0..4 wives or 0..1 husband
    val sons: Int = 0,
    val daughters: Int = 0,
    val sonSons: Int = 0,          // নাতি (পুত্রের পুত্র)
    val sonDaughters: Int = 0,     // নাতনি (পুত্রের কন্যা)
    val hasFather: Boolean = false,
    val hasMother: Boolean = false,
    val hasPaternalGrandfather: Boolean = false, // দাদা
    val hasPaternalGrandmother: Boolean = false, // দাদি
    val hasMaternalGrandmother: Boolean = false, // নানি
    val fullBrothers: Int = 0,
    val fullSisters: Int = 0,
    val paternalBrothers: Int = 0,
    val paternalSisters: Int = 0,
    val customFractionOverrides: Map<String, Double> = emptyMap()
)

@Serializable
data class HanafiShareLine(
    val key: String,
    val relationBn: String,
    val count: Int,
    val totalFraction: Double,
    val perPersonFraction: Double,
    val totalDecimal: Double,
    val perPersonDecimal: Double,
    val totalKatha: Double,
    val totalBigha: Double,
    val anaGonda: AnaGondaBreakdown,
    val perPersonAnaGonda: AnaGondaBreakdown,
    val shareRuleNote: String,
    val isBlocked: Boolean = false
)

@Serializable
data class HanafiFaraizReport(
    val totalDecimal: Double,
    val shares: List<HanafiShareLine>,
    val blockedHeirsNotes: List<String>,
    val awlApplied: Boolean,
    val raddApplied: Boolean,
    val customOverrideApplied: Boolean,
    val awlOrRaddSummary: String,
    val stepByStepNotes: List<String>,
    val totalAllocatedFraction: Double
)

@Serializable
data class DeceasedHeirRedistributionReport(
    val deceasedHeirLabel: String,
    val inheritedDecimal: Double,
    val stage2Report: HanafiFaraizReport,
    val combinedLivingShares: List<HanafiShareLine>
)

@Serializable
enum class HinduSchool(val titleBn: String) {
    DAYABHAGA("দায়ভাগ (Dayabhaga — বাংলাদেশ প্রচলিত)"),
    MITAKSHARA("মিতাক্ষরা (Mitakshara — সহদায়াদ প্রথা)")
}

@Serializable
data class HinduInheritanceRules(
    val school: HinduSchool = HinduSchool.DAYABHAGA,
    val widowGetsEqualSonShare: Boolean = true,
    val daughtersEqualInMitakshara: Boolean = true,
    val motherGetsEqualOnPartition: Boolean = true
)

@Serializable
data class HinduInheritanceReport(
    val rules: HinduInheritanceRules,
    val totalDecimal: Double,
    val shares: List<HanafiShareLine>,
    val stepByStepExplanation: List<String>,
    val requiresLegalReview: Boolean = true
)

@Serializable
data class HissaOwnerEntry(
    val id: String,
    val name: String,
    val ana: Int = 0,
    val gonda: Int = 0,
    val kora: Int = 0,
    val kranti: Int = 0,
    val til: Int = 0,
    val explicitRatio: Double? = null
) {
    fun ratio(): Double = explicitRatio ?: (
        ((ana * 20.0 * 4.0 * 3.0 * 20.0) +
            (gonda * 4.0 * 3.0 * 20.0) +
            (kora * 3.0 * 20.0) +
            (kranti * 20.0) + til) / 76800.0
    ).coerceIn(0.0, 2.0)
}

@Serializable
data class HissaOwnerComputed(
    val id: String,
    val name: String,
    val ratio: Double,
    val decimal: Double,
    val katha: Double,
    val sqFt: Double,
    val anaGondaText: String
)

@Serializable
data class HissaAnalysisReport(
    val dagTotalDecimal: Double,
    val owners: List<HissaOwnerComputed>,
    val totalRatio: Double,
    val totalAnaEquivalent: Double,
    val isExact16Ana: Boolean,
    val mismatchAna: Double,
    val statusNote: String
)

@Serializable
data class CrossKhatianHolding(
    val id: String,
    val khatianNo: String,
    val dagNo: String,
    val dagTotalDecimal: Double,
    val ownerRatio: Double
)

@Serializable
enum class UnknownSideShape(val titleBn: String) {
    RECTANGLE("আয়তক্ষেত্র (ক্ষেত্রফল + ১ বাহু)"),
    TRAPEZIUM_HEIGHT("ট্র্যাপিজিয়াম (ক্ষেত্রফল + ২ সমান্তরাল → উচ্চতা)"),
    TRAPEZIUM_SIDE("ট্র্যাপিজিয়াম (ক্ষেত্রফল + উচ্চতা + ১ বাহু)"),
    TRIANGLE_BASE_HEIGHT("ত্রিভুজ (ক্ষেত্রফল + ভূমি → উচ্চতা)"),
    TRIANGLE_SAS("ত্রিভুজ SAS (ক্ষেত্রফল + ১ বাহু + কোণ → ২য় ও ৩য় বাহু)"),
    PARALLELOGRAM("সামান্তরিক (ক্ষেত্রফল + ভূমি → উচ্চতা)"),
    QUAD_WITH_DIAGONAL("চতুর্ভুজ কর্ণসহ (ক্ষেত্রফল + কর্ণ + ৩ বাহু → ৪র্থ বাহু)")
}

@Serializable
data class UnknownSideSolveResult(
    val solvedValue: Double,
    val secondaryValue: Double? = null,
    val labelBn: String,
    val formulaUsed: String,
    val stepByStep: String,
    val isValid: Boolean,
    val errorMsg: String? = null
)

@Serializable
data class CircleSectorResult(
    val radiusFt: Double,
    val centralAngleDeg: Double,
    val fullCircleAreaSqFt: Double,
    val circumferenceFt: Double,
    val arcLengthFt: Double,
    val chordLengthFt: Double,
    val sectorAreaSqFt: Double,
    val segmentAreaSqFt: Double,
    val sectorDecimal: Double
)

@Serializable
data class FormulaCheatSheetEntry(
    val id: String,
    val titleBn: String,
    val formulaText: String,
    val workedExampleBn: String,
    val verificationNote: String
)

// ==================== M08: KHATIAN ANALYSIS & RECORD MANAGER MODELS ====================

@Serializable
data class KhatianOneClickOwnerResult(
    val name: String,
    val shareText: String,
    val ratio: Double,
    val decimal: Double,
    val katha: Double,
    val bigha: Double,
    val sqFt: Double
)

@Serializable
data class KhatianOneClickAnalysis(
    val khatianNumber: String,
    val totalDagDecimal: Double,
    val owners: List<KhatianOneClickOwnerResult>,
    val totalOwnerRatio: Double,
    val isSixteenAnaExact: Boolean,
    val ratioDiscrepancy: Double,
    val discrepancyNote: String
)

@Serializable
data class DagIndexCrossRefItem(
    val dagNumber: String,
    val khatianId: String,
    val khatianType: String,
    val khatianNumber: String,
    val mouzaName: String,
    val ownersSummary: String,
    val landClass: String,
    val totalDagDecimal: Double,
    val thisKhatianDecimal: Double
)

@Serializable
data class ThreeWayAreaCompareResult(
    val mapDecimal: Double,
    val khatianDecimal: Double,
    val fieldDecimal: Double,
    val mapVsKhatianDiffDec: Double,
    val mapVsKhatianDiffPct: Double,
    val fieldVsKhatianDiffDec: Double,
    val fieldVsKhatianDiffPct: Double,
    val mapVsFieldDiffDec: Double,
    val mapVsFieldDiffPct: Double,
    val maxSpreadDec: Double,
    val maxSpreadPct: Double,
    val tolerancePct: Double,
    val withinTolerance: Boolean,
    val verdictBn: String
)

@Serializable
data class EncroachmentOverlapReport(
    val formerDagLabel: String,
    val currentDagLabel: String,
    val formerAreaSqFt: Double,
    val currentAreaSqFt: Double,
    val overlapAreaSqFt: Double,
    val encroachmentIntoCurrentSqFt: Double,
    val shortfallGapSqFt: Double,
    val overlapRatioPct: Double,
    val formerPolygon: List<PolygonVertex>,
    val currentPolygon: List<PolygonVertex>,
    val summaryNoteBn: String
)
