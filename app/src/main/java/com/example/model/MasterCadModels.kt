package com.example.model

import kotlinx.serialization.Serializable

// ==================== Separated Individual Routes (Master Prompt 03) ====================

@Serializable data class CadEngineRoute(val initialMode: String = "draw")
@Serializable data class DimensionSystemRoute(val category: String = "all")
@Serializable data class CogoEngineRoute(val initialMode: String = "point")
@Serializable data object HistoricalDeedRoute
@Serializable data object DocumentArchiverRoute
@Serializable data object KhatianWorkstationRoute
@Serializable data class AcLandOfficeRoute(val initialSection: String = "registers")
@Serializable data object FieldToCadRoute
@Serializable data object SurfaceTopoRoute
@Serializable data object RoadAccessRoute
@Serializable data object AutomatedScheduleRoute
@Serializable data object SurveyQualityRoute
@Serializable data object FileCompatibilityRoute

// ==================== Module 01 & 02: 2D CAD Drawing + 30 Dimension Types ====================

@Serializable
enum class CadDimensionType(val titleBn: String, val codeEn: String) {
    LINEAR("রৈখিক (Linear)", "Linear"),
    HORIZONTAL("অনুভূমিক (Horizontal)", "Horizontal"),
    VERTICAL("উল্লম্ব (Vertical)", "Vertical"),
    ALIGNED("অ্যালাইনড (Aligned)", "Aligned"),
    ROTATED("ঘূর্ণিত (Rotated)", "Rotated"),
    BASELINE("বেজলাইন (Baseline)", "Baseline"),
    CONTINUE("কন্টিনিউ (Continue)", "Continue"),
    ANGULAR("কৌণিক (Angular)", "Angular"),
    TWO_PT_ANGULAR("২-পয়েন্ট কৌণিক", "2-Pt Angular"),
    THREE_PT_ANGULAR("৩-পয়েন্ট কৌণিক", "3-Pt Angular"),
    RADIUS("ব্যাসার্ধ (Radius)", "Radius"),
    DIAMETER("ব্যাস (Diameter)", "Diameter"),
    JOGGED_RADIUS("জগড ব্যাসার্ধ", "Jogged Radius"),
    ARC_LENGTH("বৃত্তচাপ দৈর্ঘ্য", "Arc Length"),
    ORDINATE_X("অর্ডিনেট X (East)", "Ordinate X"),
    ORDINATE_Y("অর্ডিনেট Y (North)", "Ordinate Y"),
    LEADER("লিডার (Leader)", "Leader"),
    MULTILEADER("মাল্টি-লিডার", "Multileader"),
    CENTER_MARK("সেন্টার মার্ক (+)", "Center Mark"),
    CENTERLINE("সেন্টারলাইন (CL)", "Centerline"),
    QUICK_DIM("কুইক ডাইমেনশন", "Quick Dim"),
    MULTIPLE_DIM("মাল্টিপল ডাইমেনশন", "Multiple Dim"),
    CHAIN_DIM("চেইন ডাইমেনশন", "Chain Dim"),
    STAGGERED_DIM("স্ট্যাগার্ড ডাইমেনশন", "Staggered"),
    DIM_BREAK("ডাইমেনশন ব্রেক", "Dim Break"),
    DIM_SPACING("ডাইমেনশন স্পেসিং", "Dim Spacing"),
    DIM_ALIGNMENT("ডাইমেনশন অ্যালাইনমেন্ট", "Dim Align"),
    DIM_OVERRIDE("ডাইমেনশন ওভাররাইড", "Dim Override"),
    DIM_REASSOCIATE("রি-অ্যাসোসিয়েট", "Reassociate"),
    DIM_EDIT("ডাইমেনশন এডিট", "Dim Edit")
}

@Serializable
enum class MeasurementDisplayUnit(val labelBn: String, val shortCode: String) {
    FEET_INCHES("ফুট-ইঞ্চি (37'-8\")", "ft-in"),
    FEET_INCHES_WORDS("ফুট ও ইঞ্চি শব্দে (37 ft 8 in)", "ft in"),
    FRACTIONAL_INCHES("ভগ্নাংশ ইঞ্চি (37'-8 1/2\")", "frac-in"),
    DECIMAL_FEET("দশমিক ফুট (37.67 ft)", "dec-ft"),
    METRES("মিটার (11.48 m)", "m"),
    CENTIMETRES("সেন্টিমিটার (cm)", "cm"),
    MILLIMETRES("মিলিমিটার (mm)", "mm"),
    BENGALI_UNITS("বাংলা একক (শতাংশ/কাঠা/হাত)", "bn")
}

@Serializable
enum class CadEntityType(val titleBn: String) {
    LINE("লাইন"),
    POLYLINE("পলিলাইন / সীমানা"),
    RECTANGLE("আয়তক্ষেত্র"),
    POLYGON("বহুভুজ"),
    CIRCLE("বৃত্ত"),
    ARC("বৃত্তচাপ"),
    ELLIPSE("উপবৃত্ত"),
    SPLINE("স্প্লাইন"),
    DONUT("ডোনাট"),
    POINT("পয়েন্ট"),
    RAY("রে (Ray)"),
    XLINE("কনস্ট্রাকশন লাইন"),
    HATCH("হ্যাচ / শেড"),
    TEXT("টেক্সট / লেবেল")
}

@Serializable
enum class CadOsnapMode(val labelBn: String, val shortEn: String) {
    ENDPOINT("Endpoint", "END"),
    MIDPOINT("Midpoint", "MID"),
    CENTER("Center", "CEN"),
    NODE("Node", "NOD"),
    QUADRANT("Quadrant", "QUA"),
    INTERSECTION("Intersection", "INT"),
    PERPENDICULAR("Perpendicular", "PER"),
    TANGENT("Tangent", "TAN"),
    NEAREST("Nearest", "NEA"),
    PARALLEL("Parallel", "PAR"),
    GEOMETRIC_CENTER("Geom Center", "GCE"),
    POLAR_TRACKING("Polar Track", "POL"),
    ORTHO_MODE("Ortho (90°)", "ORT"),
    DYNAMIC_INPUT("Dynamic Input", "DYN"),
    GRID_SNAP("Grid Snap", "GRD")
}

@Serializable
data class CadLayerConfig(
    val id: String,
    val nameBn: String,
    val dxfName: String,
    val colorArgb: Long,
    val lineWeightPx: Float = 2.5f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val frozen: Boolean = false
)

@Serializable
data class CadEntity(
    val id: String,
    val type: CadEntityType,
    val layerId: String = "layer_plot_bdy",
    val points: List<PolygonVertex> = emptyList(),
    val radius: Double = 0.0,
    val secondaryRadius: Double = 0.0,
    val startAngleDeg: Double = 0.0,
    val endAngleDeg: Double = 360.0,
    val closed: Boolean = false,
    val text: String = "",
    val lineWidthPx: Float = 3f,
    val hatchStyle: String = "NONE", // NONE, DIAGONAL, CROSS, SOLID_LIGHT
    val colorOverrideArgb: Long? = null
)

@Serializable
data class CadDimensionEntity(
    val id: String,
    val dimType: CadDimensionType,
    val layerId: String = "layer_dim",
    val p1: PolygonVertex,
    val p2: PolygonVertex,
    val p3: PolygonVertex? = null,
    val offsetDistanceFt: Double = 8.0,
    val rotationDeg: Double = 0.0,
    val associatedEntityId: String? = null,
    val prefix: String = "",
    val suffix: String = "",
    val overrideText: String = "",
    val unit: MeasurementDisplayUnit = MeasurementDisplayUnit.FEET_INCHES,
    val precision: Int = 2,
    val arrowStyle: String = "CLOSED_FILLED", // CLOSED_FILLED, ARCH_TICK, DOT
    val textHeightPx: Float = 22f,
    val styleName: String = "BD-Survey-ISO",
    val isBroken: Boolean = false,
    val staggeredLevel: Int = 0
)

@Serializable
data class CadStyleSettings(
    val activeStyleName: String = "BD-Survey-ISO",
    val activeUnit: MeasurementDisplayUnit = MeasurementDisplayUnit.FEET_INCHES,
    val precision: Int = 2,
    val textHeightPx: Float = 22f,
    val arrowStyle: String = "CLOSED_FILLED",
    val scaleFactor: Double = 1.0,
    val gridSpacingFt: Double = 10.0,
    val enabledOsnaps: List<CadOsnapMode> = listOf(
        CadOsnapMode.ENDPOINT,
        CadOsnapMode.MIDPOINT,
        CadOsnapMode.INTERSECTION,
        CadOsnapMode.PERPENDICULAR,
        CadOsnapMode.GRID_SNAP
    )
)

// ==================== Module 03: COGO Survey Engine ====================

@Serializable
data class CogoPoint(
    val id: String,
    val pointNumber: Int,
    val code: String = "PT",
    val northFt: Double,
    val eastFt: Double,
    val elevationFt: Double = 0.0,
    val description: String = "",
    val source: String = "Manual"
)

@Serializable
data class CircularCurveResult(
    val radiusFt: Double,
    val deltaDeg: Double,
    val degreeOfCurveArc: Double,
    val degreeOfCurveChord: Double,
    val arcLengthFt: Double,
    val chordLengthFt: Double,
    val tangentLengthFt: Double,
    val externalDistanceFt: Double,
    val midOrdinateFt: Double,
    val pcPoint: PolygonVertex,
    val piPoint: PolygonVertex,
    val ptPoint: PolygonVertex,
    val chordDeflections: List<String> = emptyList()
)

// ==================== Module 06: Historical Deed Recreation Engine ====================

@Serializable
data class HistoricalDeedRecord(
    val id: String,
    val title: String = "পুরাতন সাফ-কবলা দলিল (পুনর্গঠিত)",
    val templateType: DeedTemplateType = DeedTemplateType.SAFKABALA_SALE,
    val sourceImageUri: String? = null,
    val ocrRawText: String = "",
    val buyerName: String = "",
    val buyerFather: String = "",
    val buyerAddress: String = "",
    val sellerName: String = "",
    val sellerFather: String = "",
    val sellerAddress: String = "",
    val deedNumber: String = "",
    val registrationDate: String = "",
    val registrationOffice: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val khatianNumber: String = "",
    val dagNumber: String = "",
    val areaDecimal: String = "",
    val shareFraction: String = "১.০০০ (১৬ আনা)",
    val boundaryNorth: String = "",
    val boundarySouth: String = "",
    val boundaryEast: String = "",
    val boundaryWest: String = "",
    val previousDeedRef: String = "",
    val stampValueLabel: String = "১০০ টাকা (প্রাচীন নমুনা)",
    val paperSize: String = "Legal / A4",
    val customArchivalTerms: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// ==================== Module 08: AC Land & Union Land Office Registers ====================

@Serializable
enum class UnionRegisterKind(val titleBn: String) {
    MOUZA_REG("১. মৌজা রেজিস্টার"),
    HOLDING_REG("২. হোল্ডিং রেজিস্টার (রেঃ II)"),
    OWNER_REG("৩. জমির মালিক রেজিস্টার"),
    LAND_CLASS_REG("৪. জমির শ্রেণি রেজিস্টার"),
    DAG_REG("৫. দাগ রেজিস্টার"),
    KHATIAN_REG("৬. খতিয়ান রেজিস্টার"),
    TAX_ASSESSMENT("৭. কর নির্ধারণ রেজিস্টার"),
    DAKHILA_REG("৮. দাখিলা / রশিদ রেজিস্টার"),
    DEMAND_REG("৯. দাবি রেজিস্টার"),
    COLLECTION_REG("১০. আদায় রেজিস্টার"),
    ARREAR_REG("১১. বকেয়া রেজিস্টার"),
    MUTATION_REF("১২. নামজারি রেজিস্টার (রেঃ IX)"),
    FIELD_INSPECTION("১৩. মাঠ পরিদর্শন রেজিস্টার"),
    NOTICE_REG("১৪. নোটিশ ও সমন রেজিস্টার"),
    CASE_REG("১৫. রাজস্ব / মিস কেস রেজিস্টার")
}

@Serializable
data class UnionRegisterEntry(
    val id: String,
    val kind: UnionRegisterKind,
    val refNo: String,
    val mouza: String,
    val khatianDag: String,
    val personName: String,
    val amountOrArea: String,
    val dateStr: String,
    val status: String = "সক্রিয়",
    val notes: String = ""
)

@Serializable
data class TaxRateSchedule(
    val effectiveYear: String = "১৪৩৩ বঙ্গাব্দ / ২০২৬",
    val agriBelow25BighaFeeBdt: Double = 10.0,
    val agriPerDecimalRateBdt: Double = 2.0,
    val nonAgriResidentialPerDecBdt: Double = 60.0,
    val commercialPerDecBdt: Double = 250.0,
    val industrialPerDecBdt: Double = 150.0,
    val annualLateInterestPct: Double = 6.25
)

// ==================== Module 12 & 13: Surface TIN & Road Alignment ====================

@Serializable
data class TinTriangle(
    val p1: CogoPoint,
    val p2: CogoPoint,
    val p3: CogoPoint,
    val slopePct: Double,
    val aspectDeg: Double,
    val area2dSqFt: Double
)

@Serializable
data class ContourSegment(
    val elevationFt: Double,
    val isMajor: Boolean,
    val start: PolygonVertex,
    val end: PolygonVertex
)

@Serializable
data class RoadAlignmentSpec(
    val id: String = "road_1",
    val name: String = "প্রস্তাবিত সংযোগ সড়ক",
    val rightOfWayWidthFt: Double = 20.0,
    val centerlinePoints: List<PolygonVertex> = listOf(
        PolygonVertex(0.0, 0.0),
        PolygonVertex(120.0, 0.0),
        PolygonVertex(180.0, 60.0)
    )
)

// ==================== Module 16: Survey Quality Control Audit ====================

@Serializable
data class QualityControlCheckResult(
    val categoryBn: String,
    val status: String, // PASS, WARN, FAIL
    val measuredValue: String,
    val thresholdOrRef: String,
    val recommendationBn: String
)

@Serializable
data class QualityControlAuditReport(
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val overallScorePct: Int,
    val checks: List<QualityControlCheckResult>,
    val discrepancySummaryBn: String
)
