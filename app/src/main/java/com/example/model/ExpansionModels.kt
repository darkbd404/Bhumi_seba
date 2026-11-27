package com.example.model

import kotlinx.serialization.Serializable

// ==================== New Type-Safe Routes (Expansion Pack 02: Parts A–J) ====================

@Serializable data object OldMouzaLabRoute
@Serializable data object FieldInstrumentsRoute
@Serializable data object OnlineLandServiceWorkbenchRoute
@Serializable data object RevenueDueDiligenceRoute
@Serializable data object DeedLabProRoute
@Serializable data object AreaInvestigationRoute
@Serializable data object SpecializedSurveyToolsRoute
@Serializable data object CustomServiceEngineRoute
@Serializable data object UniversalArchiveExportRoute

// ==================== Part A: Old Mouza Map & Dag Relationship ====================

@Serializable
data class DagGenealogyRecord(
    val id: String,
    val mouza: String,
    val jlNumber: String = "",
    val relationType: String = "বিভাজন (Split)", // Split / Merge / Partial / Renumber
    val formerSurvey: String = "সিএস / আরএস",
    val formerDagNumbers: String,
    val currentSurvey: String = "বিএস / বিআরএস",
    val currentDagNumbers: String,
    val khatianLink: String = "",
    val ownerLink: String = "",
    val transferDecimal: Double = 0.0,
    val verifiedManually: Boolean = false,
    val notes: String = ""
)

@Serializable
data class AdjacentDagNeighbour(
    val id: String,
    val direction: String, // উত্তর / দক্ষিণ / পূর্ব / পশ্চিম
    val neighbourDagNo: String,
    val ownerName: String = "",
    val sharedLengthFt: Double = 0.0,
    val existsInDigitizedMap: Boolean = true
)

// ==================== Part B: Field Instrument Leveling & Field Book ====================

@Serializable
data class LevelReadingEntry(
    val id: String,
    val station: String,
    val chainageFt: Double = 0.0,
    val bs: Double? = null,
    val intermediateSight: Double? = null,
    val fs: Double? = null,
    val remarks: String = ""
)

@Serializable
data class FieldBookEntry(
    val id: String,
    val stationNo: String,
    val pointNo: String,
    val instrument: String = "Total Station",
    val bearingDeg: Double = 0.0,
    val distanceFt: Double = 0.0,
    val verticalAngleDeg: Double = 0.0,
    val elevationM: Double = 0.0,
    val remarks: String = "",
    val correctionNote: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

// ==================== Part C & I: Online Land Service Workbench ====================

@Serializable
enum class LandServiceWorkflowKind(val titleBn: String, val portalUrl: String) {
    ONLINE_KHATIAN("অনলাইন খতিয়ান / সার্টিফাইড কপি", "https://eporcha.gov.bd/"),
    MOUZA_MAP_ORDER("সরকারি মৌজা ম্যাপ সংগ্রহ", "https://map.settlement.gov.bd/"),
    E_MUTATION("ই-নামজারি (৮ ধরনের উৎস)", "https://mutation.land.gov.bd/"),
    PARTITION_RECORD("জমাভাগ / জমা একত্রীকরণ", "https://mutation.land.gov.bd/"),
    KHAJNA_TAX("ভূমি উন্নয়ন কর / খাজনা", "https://ldtax.gov.bd/"),
    SUB_REGISTRY("সাব-রেজিস্ট্রি ও দলিল সহায়তা", "https://dolil.gov.bd"),
    RECORD_CORRECTION("অনলাইন রেকর্ড সংশোধন", "https://eporcha.gov.bd/"),
    SURVEY_OBJECTION("জরিপ আপত্তি ও আপিল (৩০/৩১ ধারা)", "https://dlrs.gov.bd/")
}

@Serializable
data class ServiceApplicationCase(
    val id: String,
    val serviceKind: LandServiceWorkflowKind,
    val subWorkflow: String = "",
    val title: String,
    val applicantName: String = "",
    val applicantFather: String = "",
    val applicantPhone: String = "",
    val applicantNid: String = "",
    val previousOwner: String = "",
    val prevKhatian: String = "",
    val prevDag: String = "",
    val deedNumber: String = "",
    val deedDate: String = "",
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val appliedDecimal: Double = 0.0,
    val officialFeeBdt: Double = 0.0,
    val trackingNumber: String = "",
    val paymentRef: String = "",
    val hearingDate: String = "",
    val status: String = "খসড়া প্রস্তুতি (Draft)",
    val checklistDoneCsv: String = "",
    val receiptVerifiedByUser: Boolean = false,
    val objectionNotes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
data class KhajnaHoldingRecord(
    val id: String,
    val holdingNo: String,
    val ownerName: String,
    val mouza: String,
    val jlNo: String = "",
    val khatianNo: String,
    val dagNo: String,
    val landClass: String = "কৃষি / আবাসিক",
    val areaDecimal: Double,
    val annualRatePerDecimalBdt: Double = 10.0,
    val arrearsYears: Int = 0,
    val dakhilaRef: String = "",
    val receiptVerified: Boolean = false
)

// ==================== Part D: Revenue, Acquisition, Lease, Dispute, Due Diligence ====================

@Serializable
data class RevenueCaseRecord(
    val id: String,
    val caseNo: String,
    val category: String, // মিস কেস / রিভিশন / আপিল / সার্টিফিকেট / খাস জমি / অধিগ্রহণ / দখল বিরোধ
    val applicant: String,
    val oppositeParty: String = "",
    val mouzaDagSummary: String = "",
    val areaDecimal: Double = 0.0,
    val estimatedAmountBdt: Double = 0.0,
    val nextHearingDate: String = "",
    val status: String = "চলমান",
    val ordersAndNotes: String = ""
)

// ==================== Part E: Legal Clauses ====================

@Serializable
data class EditableLegalClause(
    val id: String,
    val category: String, // বিক্রয় / বণ্টন / হেবা / দখল / চৌহদ্দি / পণ / সাক্ষী / বিরোধ
    val titleBn: String,
    val bodyBn: String,
    val bodyEn: String
)

// ==================== Part G: Specialized Survey, Equipment & Quotations ====================

@Serializable
data class SurveyInstrumentItem(
    val id: String,
    val name: String,
    val category: String, // Total Station / GNSS RTK / Auto Level / Chain & Tape
    val serialNo: String,
    val lastCalibrated: String,
    val assignedProject: String = "",
    val conditionNote: String = "সচল"
)

@Serializable
data class SurveyQuotationRecord(
    val id: String,
    val clientName: String,
    val mouzaDag: String,
    val siteVisitFee: Double = 2000.0,
    val digitizationFee: Double = 1500.0,
    val perDagFee: Double = 1000.0,
    val dagCount: Int = 1,
    val instrumentFee: Double = 3000.0,
    val reportFee: Double = 1500.0,
    val travelFee: Double = 800.0,
    val paidAmount: Double = 0.0,
    val status: String = "কোটেশন (Quotation)"
)

// ==================== Part H: Custom Land Service Automation Engine ====================

@Serializable
enum class WorkflowStepKind(val labelBn: String) {
    INFO_COLLECTION("তথ্য সংগ্রহ"),
    DOC_VERIFICATION("কাগজপত্র যাচাই"),
    DECISION_YES_NO("শর্ত / হ্যাঁ-না সিদ্ধান্ত"),
    PAYMENT_STAGE("ফি / পেমেন্ট ধাপ"),
    HEARING_STAGE("শুনানি ধাপ"),
    APPROVAL_STAGE("অনুমোদন স্তর"),
    CORRECTION_REAPPLY("সংশোধন / পুনঃআবেদন"),
    FINAL_ARCHIVE("চূড়ান্ত সনদ / সংরক্ষণ")
}

@Serializable
data class CustomWorkflowStep(
    val id: String,
    val titleBn: String,
    val kind: WorkflowStepKind,
    val requiredCondition: String = "",
    val completed: Boolean = false
)

@Serializable
data class CustomServiceDefinition(
    val id: String,
    val serviceName: String,
    val category: String,
    val description: String = "",
    val officialPortalUrl: String = "https://land.gov.bd/",
    val requiredDocsCsv: String = "NID, খতিয়ান, দলিল, খাজনা রশিদ",
    val optionalDocsCsv: String = "ওয়ারিশ সনদ, মৌজা ম্যাপ",
    val eligibilityRule: String = "বৈধ মালিকানা বা আবেদনকারীর স্বার্থ থাকতে হবে",
    val feeFormulaExpr: String = "base_fee + (area_dec * per_dec_fee)",
    val defaultBaseFee: Double = 500.0,
    val defaultPerDecFee: Double = 50.0,
    val steps: List<CustomWorkflowStep> = emptyList(),
    val trackingRef: String = "",
    val reminderDate: String = "",
    val status: String = "সক্রিয়"
)

// ==================== Part J: Universal Document Archive & Calculation History ====================

@Serializable
data class UniversalArchiveDocument(
    val id: String,
    val docType: String, // দলিল / খতিয়ান / পর্চা / দাখিলা / নামজারি আদেশ / DCR / মৌজা ম্যাপ / জরিপ রিপোর্ট / কোর্ট অর্ডার / নোটিশ / চুক্তি / ওয়ারিশ সনদ
    val title: String,
    val ownerName: String = "",
    val nidRef: String = "",
    val mouza: String = "",
    val dagNo: String = "",
    val khatianNo: String = "",
    val deedOrAppNo: String = "",
    val docDate: String = "",
    val projectRef: String = "",
    val verificationStatus: String = "ব্যবহারকারীর সরবরাহকৃত (অযাচাইকৃত)",
    val summaryContent: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Serializable
data class ToolCalculationHistoryEntry(
    val id: String,
    val moduleTag: String,
    val summaryTitle: String,
    val resultSummary: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
