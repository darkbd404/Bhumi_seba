package com.example.model

import kotlinx.serialization.Serializable

// Additional Type-Safe Navigation Routes for the 18-Module Digital Surveyor Workstation
@Serializable
data object WorkstationHubRoute

@Serializable
data object PolygonDividerRoute

@Serializable
data object PantographRoute

@Serializable
data object PdfCadStudioRoute

@Serializable
data object GpsGisToolsRoute

@Serializable
data object DeedStudioRoute

@Serializable
data object ScannerOcrRoute

@Serializable
data object ReportStudioRoute

@Serializable
data object LandDatabaseRoute

@Serializable
data object CustomBuilderRoute

@Serializable
data object LegalLibraryRoute

// -------------------- 1. Configurable Regional Land Units --------------------

@Serializable
data class RegionalUnitProfile(
    val id: String,
    val regionNameBn: String,
    val regionNameEn: String,
    val kathaSqFt: Double = 720.0,
    val bighaSqFt: Double = 14400.0,
    val kaniSqFt: Double = 17424.0,     // e.g. Chattogram 40 Dec = 17424, Noakhali 120 Dec = 52272
    val gondaSqFt: Double = 871.2,      // 1/20 Kani
    val koraSqFt: Double = 217.8,       // 1/4 Gonda
    val krantiSqFt: Double = 72.6,      // 1/3 Kora
    val tilSqFt: Double = 3.63,         // 1/20 Kranti
    val customUnitName: String = "স্থানীয় একক",
    val customUnitSqFt: Double = 435.6,
    val isDefault: Boolean = false
)

// -------------------- 2. Automatic Land Division --------------------

@Serializable
enum class LandSplitMode(val titleBn: String) {
    EQUAL("সমান ভাগ"),
    PERCENTAGE("শতাংশ (%) ভিত্তিক"),
    FIXED_DECIMAL("নির্দিষ্ট শতাংশ"),
    FIXED_KATHA("নির্দিষ্ট কাঠা")
}

@Serializable
data class PolygonVertex(
    val x: Double,
    val y: Double
)

@Serializable
data class DivisionShareSpec(
    val ownerName: String,
    val value: Double // Equal weight, percentage, decimal, or katha
)

@Serializable
data class DividedSubParcel(
    val index: Int,
    val ownerName: String,
    val areaSqFt: Double,
    val areaDecimal: Double,
    val areaKatha: Double,
    val perimeterFt: Double,
    val edgeLengthsFt: List<Double>,
    val vertices: List<PolygonVertex>,
    val colorArgb: Long
)

@Serializable
data class LandDivisionResultRecord(
    val id: String,
    val title: String,
    val originalAreaSqFt: Double,
    val roadReservedSqFt: Double,
    val usableAreaSqFt: Double,
    val allocatedAreaSqFt: Double,
    val remainingAreaSqFt: Double,
    val conservationErrorSqFt: Double,
    val isValidConservation: Boolean,
    val roadWidthFt: Double,
    val cutAngleDeg: Double,
    val parcels: List<DividedSubParcel>,
    val roadPolygon: List<PolygonVertex> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

// -------------------- 3. Pantograph & Map Comparison --------------------

@Serializable
enum class CadastralSurveyStage(val label: String) {
    CS("সিএস (CS)"),
    SA("এসএ (SA)"),
    RS("আরএস (RS)"),
    BS("বিএস (BS)"),
    BRS("বিআরএস (BRS)")
}

@Serializable
enum class TransformModelType(val titleBn: String) {
    SIMILARITY("সিমিলারিটি (Scale + Rotate + Shift)"),
    AFFINE("অ্যাফাইন (Affine 6-Param)"),
    PROJECTIVE("প্রজেক্টিভ (Homography)"),
    TPS("থিন প্লেট স্প্লাইন (TPS / Rubber Sheet)")
}

@Serializable
data class ControlPointPair(
    val id: String,
    val label: String,
    val baseX: Double,
    val baseY: Double,
    val overlayX: Double,
    val overlayY: Double,
    val residualErrorPx: Double = 0.0
)

@Serializable
data class PantographReportSummary(
    val baseStage: CadastralSurveyStage = CadastralSurveyStage.CS,
    val overlayStage: CadastralSurveyStage = CadastralSurveyStage.RS,
    val transformType: TransformModelType = TransformModelType.AFFINE,
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
    val rotationDeg: Double = 0.0,
    val shiftX: Double = 0.0,
    val shiftY: Double = 0.0,
    val rmseErrorPx: Double = 0.0,
    val maxResidualPx: Double = 0.0,
    val controlPoints: List<ControlPointPair> = emptyList(),
    val dagMatches: List<String> = emptyList()
)

// -------------------- 4. GPS Field Survey & GIS Coordinates --------------------

@Serializable
data class GeoWaypoint(
    val id: String,
    val label: String,
    val lat: Double,
    val lon: Double,
    val altitudeM: Double = 0.0,
    val accuracyM: Double = 5.0,
    val hdop: Double = 1.2,
    val vdop: Double = 1.8,
    val fixType: String = "Phone GPS",
    val bearingDeg: Double = 0.0,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Serializable
data class FieldSurveyTrack(
    val id: String,
    val title: String,
    val mode: String = "Walking", // Walking / Vehicle / NMEA Import
    val waypoints: List<GeoWaypoint> = emptyList(),
    val areaSqFt: Double = 0.0,
    val perimeterFt: Double = 0.0,
    val sourceNotice: String = "মোবাইল GPS আনুমানিক পরিমাপ — সরকারি সার্টিফিকেশন নয়",
    val timestampMillis: Long = System.currentTimeMillis()
)

// -------------------- 5. Multi-Law Inheritance --------------------

@Serializable
enum class InheritanceLawType(val titleBn: String, val lawRefEn: String) {
    MUSLIM_SUNNI("মুসলিম (সুন্নি হানাফি ফারায়েজ)", "Muslim Personal Law (Shariat)"),
    HINDU_DAYABHAGA("হিন্দু (বাংলাদেশ দায়ভাগ আইন)", "Hindu Law — Dayabhaga School"),
    BUDDHIST_BD("বৌদ্ধ উত্তরাধিকার (বাংলাদেশ)", "Buddhist Customary / Dayabhaga Ref"),
    CHRISTIAN_BD("খ্রিস্টান (Succession Act 1925)", "Succession Act 1925 (Bangladesh)")
}

@Serializable
data class UniversalInheritanceShare(
    val relationBn: String,
    val totalFraction: Double,
    val perPersonFraction: Double,
    val count: Int,
    val totalDecimal: Double,
    val perPersonDecimal: Double,
    val statusNote: String = ""
)

@Serializable
data class InheritanceCalculationReport(
    val lawType: InheritanceLawType,
    val lawVersion: String,
    val totalDecimal: Double,
    val shares: List<UniversalInheritanceShare>,
    val unallocatedFraction: Double = 0.0,
    val requiresLegalReview: Boolean = false,
    val warnings: List<String> = emptyList()
)

// -------------------- 6. Deed & Legal Document Studio --------------------

@Serializable
enum class DeedTemplateType(val titleBn: String, val titleEn: String) {
    SAFKABALA_SALE("সাফ-কবলা দলিল (খসড়া)", "Sale Deed Draft"),
    BONTANNAMA_PARTITION("বণ্টননামা দলিল (খসড়া)", "Partition Deed Draft"),
    HEBA_GIFT("হেবা / দানপত্র দলিল (খসড়া)", "Heba / Gift Deed Draft"),
    BAYNA_AGREEMENT("বায়না চুক্তিপত্র (খসড়া)", "Bayna Agreement Draft"),
    EXCHANGE_DEED("এওয়াজ / বিনিময় দলিল (খসড়া)", "Exchange Deed Draft"),
    AFFIDAVIT("হলফনামা (Affidavit Draft)", "Affidavit Draft"),
    POWER_OF_ATTORNEY("আমমোক্তারনামা (খসড়া)", "Power of Attorney Draft"),
    POSSESSION_MEMO("দখলনামা স্মারক (খসড়া)", "Possession Handover Memo"),
    SURVEY_CONTRACT("জরিপ / আমিন চুক্তিপত্র", "Surveyor Appointment Contract"),
    LAND_SCHEDULE("জমির তফসিল ও চৌহদ্দি", "Land Schedule & Boundaries"),
    INHERITANCE_STATEMENT("ওয়ারিশ সম্পত্তি বণ্টন বিবরণী", "Inheritance Distribution Statement"),
    SETTLEMENT_AGREEMENT("আপস-মীমাংসা চুক্তিপত্র", "Settlement Agreement Draft")
}

@Serializable
data class DeedDraftDocument(
    val id: String,
    val templateType: DeedTemplateType = DeedTemplateType.SAFKABALA_SALE,
    val languageBn: Boolean = true,
    val title: String = "",
    val firstPartyName: String = "",
    val firstPartyFather: String = "",
    val firstPartyNid: String = "",
    val firstPartyAddress: String = "",
    val secondPartyName: String = "",
    val secondPartyFather: String = "",
    val secondPartyNid: String = "",
    val secondPartyAddress: String = "",
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val khatianType: String = "বিএস (BS)",
    val khatianNumber: String = "",
    val dagNumber: String = "",
    val landClass: String = "নাল / ভিটা",
    val areaDecimal: String = "",
    val boundaryNorth: String = "",
    val boundarySouth: String = "",
    val boundaryEast: String = "",
    val boundaryWest: String = "",
    val transactionValueBdt: String = "",
    val termsAndConditions: String = "",
    val witness1: String = "",
    val witness2: String = "",
    val deedDate: String = "",
    val customNotes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// -------------------- 7. Document Scanner & OCR --------------------

@Serializable
data class OcrExtractedField(
    val key: String,
    val labelBn: String,
    val value: String,
    val confidence: Float
)

@Serializable
data class OcrScannedDocument(
    val id: String,
    val title: String,
    val imagePath: String? = null,
    val rawText: String,
    val extractedFields: List<OcrExtractedField> = emptyList(),
    val docHash: String = "",
    val verifiedByUser: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// -------------------- 8. Report Studio --------------------

@Serializable
enum class SurveyReportType(val titleBn: String) {
    SURVEY_MEMO("জরিপ স্মারকলিপি (Survey Memo)"),
    MEASUREMENT_REPORT("জমি পরিমাপ প্রতিবেদন"),
    DEMARCATION_REPORT("সীমানা চিহ্নিতকরণ (Demarcation)"),
    GPS_SURVEY_REPORT("জিপিএস ফিল্ড সার্ভে রিপোর্ট"),
    PANTOGRAPH_REPORT("প্যান্টোগ্রাফ ও নকশা তুলনা"),
    DIVISION_REPORT("স্বয়ংক্রিয় জমি বিভাজন রিপোর্ট"),
    KHATIAN_REPORT("খতিয়ান ও মালিকানা বিশ্লেষণ"),
    INHERITANCE_REPORT("উত্তরাধিকার ও ফারায়েজ রিপোর্ট")
}

@Serializable
data class SurveyReportConfig(
    val id: String,
    val reportType: SurveyReportType = SurveyReportType.MEASUREMENT_REPORT,
    val title: String = "ডিজিটাল ভূমি জরিপ প্রতিবেদন",
    val ownerName: String = "",
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val khatianDagSummary: String = "",
    val scaleLabel: String = "16\" = 1 Mile (1:3,960)",
    val pageSize: String = "A4", // A4 or A3
    val orientation: String = "Portrait", // Portrait or Landscape
    val areaSummaryText: String = "",
    val sideLengthsText: String = "",
    val coordinatesText: String = "",
    val remarks: String = "",
    val includeQrCode: Boolean = true,
    val includeNorthArrow: Boolean = true,
    val includeSignatureBlock: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// -------------------- 9. Custom Form & Formula Builder --------------------

@Serializable
enum class CustomFieldType(val labelBn: String) {
    NUMBER("সংখ্যা (Number)"),
    TEXT("টেক্সট (Text)"),
    DROPDOWN("ড্রপডাউন (Options)"),
    CHECKBOX("চেকবক্স (Yes/No)"),
    DATE("তারিখ (Date)"),
    FORMULA("অটো ফর্মুলা (Calculated)")
}

@Serializable
data class CustomFormFieldDef(
    val id: String,
    val key: String, // Variable name used in formulas e.g. "len", "wid"
    val label: String,
    val type: CustomFieldType,
    val defaultValue: String = "",
    val optionsCsv: String = "", // For DROPDOWN
    val formulaExpr: String = "" // e.g. "(len * wid) / 435.6"
)

@Serializable
data class CustomFormTemplate(
    val id: String,
    val title: String,
    val description: String = "",
    val fields: List<CustomFormFieldDef> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// -------------------- 10. Unified Project & Personal Land Database --------------------

@Serializable
data class UnifiedSurveyProject(
    val id: String,
    val projectName: String,
    val ownerName: String = "",
    val ownerPhone: String = "",
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val khatianNumbers: String = "",
    val dagNumbers: String = "",
    val plotCount: Int = 1,
    val totalDecimalArea: Double = 0.0,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
data class FinancialRateConfig(
    val id: String,
    val locationLabel: String,
    val effectiveDate: String,
    val stampDutyPct: Double = 1.5,
    val registrationFeePct: Double = 1.0,
    val localGovtTaxPct: Double = 3.0,
    val sourceGainTaxPct: Double = 4.0,
    val mutationFeeBdt: Double = 1170.0,
    val landTaxPerDecimalBdt: Double = 10.0,
    val mouzaRatePerDecimalBdt: Double = 250000.0
)

@Serializable
data class LandVaultBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val regionalUnits: List<RegionalUnitProfile> = emptyList(),
    val unifiedProjects: List<UnifiedSurveyProject> = emptyList(),
    val deedDrafts: List<DeedDraftDocument> = emptyList(),
    val ocrDocuments: List<OcrScannedDocument> = emptyList(),
    val fieldTracks: List<FieldSurveyTrack> = emptyList(),
    val divisionRecords: List<LandDivisionResultRecord> = emptyList(),
    val reportConfigs: List<SurveyReportConfig> = emptyList(),
    val customTemplates: List<CustomFormTemplate> = emptyList(),
    val customKhatians: List<CustomKhatianDocument> = emptyList(),
    val savedProjects: List<SavedSurveyProject> = emptyList()
)
