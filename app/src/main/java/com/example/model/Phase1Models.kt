package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data object AccuracyReportRoute

@Serializable
enum class ConfidenceLevel(val labelBn: String, val badgeEn: String) {
    HIGH("ভরসাযোগ্য", "HIGH"),
    MEDIUM("মধ্যম", "MEDIUM"),
    LOW("কম (সতর্কতা)", "LOW")
}

@Serializable
data class CustomUnitPreset(
    val id: String,
    val nameBn: String,
    val decimalSqFt: Double = 435.6,
    val kathaSqFt: Double = 720.0,
    val bighaSqFt: Double = 14400.0,
    val kaniDecimals: Double = 40.0,
    val isBuiltIn: Boolean = false
)

@Serializable
data class ScaleCalibrationProfile(
    val id: String,
    val nameBn: String,
    val surveyStage: String = "RS",
    val feetPerInch: Double = 330.0,
    val scaleXMultiplier: Double = 1.0,
    val scaleYMultiplier: Double = 1.0,
    val paperWidthIn: Double = 0.0,
    val paperHeightIn: Double = 0.0,
    val notes: String = ""
)

@Serializable
data class GlobalSurveySettings(
    val useBanglaDigits: Boolean = true,
    val decimalPrecision: Int = 2,
    val preferredAreaUnit: String = "DECIMAL", // DECIMAL, KATHA, SQ_FT, BIGHA, KANI
    val defaultOldPaper: Boolean = true,
    val confidenceHighMaxErrPct: Double = 1.5,
    val confidenceMedMaxErrPct: Double = 4.0,
    val activeUnitPresetId: String = "preset_std_bd",
    val customUnitPresets: List<CustomUnitPreset> = defaultUnitPresets(),
    val savedCalibrationProfiles: List<ScaleCalibrationProfile> = defaultCalibrationProfiles(),
    val customTileApiKey: String = ""
) {
    fun activePreset(): CustomUnitPreset =
        customUnitPresets.firstOrNull { it.id == activeUnitPresetId }
            ?: customUnitPresets.firstOrNull()
            ?: defaultUnitPresets().first()

    companion object {
        fun defaultUnitPresets(): List<CustomUnitPreset> = listOf(
            CustomUnitPreset(
                id = "preset_std_bd",
                nameBn = "বাংলাদেশ স্ট্যান্ডার্ড (১ কাঠা=৭২০ ft², ১ শতক=৪৩৫.৬ ft²)",
                decimalSqFt = 435.6,
                kathaSqFt = 720.0,
                bighaSqFt = 14400.0,
                kaniDecimals = 40.0,
                isBuiltIn = true
            ),
            CustomUnitPreset(
                id = "preset_ctg_120",
                nameBn = "চট্টগ্রাম/কক্সবাজার কানি (১ কানি=৪০ বা ১২০ শতক)",
                decimalSqFt = 435.6,
                kathaSqFt = 720.0,
                bighaSqFt = 14400.0,
                kaniDecimals = 40.0,
                isBuiltIn = true
            ),
            CustomUnitPreset(
                id = "preset_noakhali_160",
                nameBn = "নোয়াখালী/কুমিল্লা (১ কানি=১৬০ শতক বা ৩০ শতক)",
                decimalSqFt = 435.6,
                kathaSqFt = 720.0,
                bighaSqFt = 14400.0,
                kaniDecimals = 160.0,
                isBuiltIn = true
            )
        )

        fun defaultCalibrationProfiles(): List<ScaleCalibrationProfile> = listOf(
            ScaleCalibrationProfile("cal_16", "১৬\" = ১ মাইল (CS/SA/RS/BS)", "RS", 330.0, 1.0, 1.0),
            ScaleCalibrationProfile("cal_32", "৩২\" = ১ মাইল (ঘনবসতি)", "RS", 165.0, 1.0, 1.0),
            ScaleCalibrationProfile("cal_64", "৬৪\" = ১ মাইল (সিটি জরিপ)", "BS", 82.5, 1.0, 1.0),
            ScaleCalibrationProfile("cal_80", "৮০\" = ১ মাইল (১ চেইন/ইঞ্চি)", "BS", 66.0, 1.0, 1.0)
        )
    }
}

data class AccuracyUncertaintyResult(
    val areaSqFt: Double,
    val areaErrSqFt: Double,
    val decimal: Double,
    val decimalErr: Double,
    val katha: Double,
    val kathaErr: Double,
    val perimeterFt: Double,
    val perimeterErrFt: Double,
    val relativeErrPct: Double,
    val edgeErrorsFt: List<Double>,
    val confidence: ConfidenceLevel,
    val includeDisclaimer: Boolean = true,
    val sourceSummaryBn: String
)

data class PdfInspectionReport(
    val isPdf: Boolean,
    val pageCount: Int,
    val isEncrypted: Boolean,
    val hasVectorPaths: Boolean,
    val hasTextObjects: Boolean,
    val vectorSegmentCount: Int,
    val extractedVectorPolygons: List<List<Pair<Float, Float>>> = emptyList(),
    val detectedScaleText: String? = null,
    val suggestedFeetPerInch: Double? = null,
    val pageWidthPts: Int = 1000,
    val pageHeightPts: Int = 1000,
    val disclosureBn: String = ""
)

data class CalibrationRefLine(
    val id: String,
    val label: String,
    val p1Norm: Pair<Float, Float>,
    val p2Norm: Pair<Float, Float>,
    val actualFeet: Double,
    val measuredFeetBefore: Double,
    val deviationPct: Double = 0.0
)

data class ShoelaceStepRow(
    val vertexIndex: Int,
    val xFt: Double,
    val yFt: Double,
    val xNextFt: Double,
    val yNextFt: Double,
    val prodXY: Double,
    val prodYX: Double,
    val netTerm: Double
)

data class ParcelTableRow(
    val plotId: Int,
    val plotIndex: Int,
    val dagNumber: String,
    val khatianNumber: String,
    val ownerName: String,
    val isRoad: Boolean,
    val decimal: Double,
    val katha: Double,
    val bigha: Double,
    val sqFeet: Double,
    val acre: Double,
    val perimeterFt: Double,
    val errorDecimal: Double,
    val remarks: String
)

data class MeasureBetweenResult(
    val modeTitleBn: String,
    val distanceFt: Double,
    val distanceFormatted: String,
    val detailsBn: String,
    val p1Norm: Pair<Float, Float>? = null,
    val p2Norm: Pair<Float, Float>? = null
)

data class PantographOverlapRow(
    val oldDag: String,
    val newDag: String,
    val overlapDecimal: Double,
    val overlapPctOfOld: Double,
    val overlapPctOfNew: Double
)

@Serializable
data class PantographTransformVersion(
    val id: String,
    val label: String,
    val timestampMillis: Long,
    val transformType: TransformModelType,
    val pairs: List<ControlPointPair>,
    val rmseErrorPx: Double,
    val scaleX: Double,
    val scaleY: Double,
    val rotationDeg: Double
)

data class ImportedSpatialParcel(
    val id: String,
    val name: String,
    val dagNumber: String,
    val ownerName: String,
    val pointsLatLon: List<Pair<Double, Double>>,
    val areaSqFt: Double,
    val decimal: Double,
    val sourceFormat: String
)
