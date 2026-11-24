package com.example.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

// Type-safe Navigation Compose Routes
@Serializable
data object HomeRoute

@Serializable
data object MouzaPdfRoute

@Serializable
data object RealMapRoute

@Serializable
data object ServicesRoute

@Serializable
data object ToolsRoute

@Serializable
data object AiAssistantRoute

@Serializable
data class WebBrowserRoute(
    val url: String,
    val title: String
)

@Serializable
data object AboutRoute

enum class MouzaScale(
    val labelBn: String,
    val labelEn: String,
    val ratioLabel: String,
    val feetPerInch: Double
) {
    SCALE_16("১৬\" = ১ মাইল (স্ট্যান্ডার্ড)", "16\" = 1 Mile (CS/SA/RS/BS)", "1:3,960", 330.0),
    SCALE_32("৩২\" = ১ মাইল (ঘনবসতি)", "32\" = 1 Mile (Urban/Dense)", "1:1,980", 165.0),
    SCALE_64("৬৪\" = ১ মাইল (সিটি/বড়)", "64\" = 1 Mile (City/Large)", "1:990", 82.5),
    SCALE_80("৮০\" = ১ মাইল (১ চেইন)", "80\" = 1 Mile (1 Chain/in)", "1:792", 66.0)
}

enum class MouzaSelectionTool(val labelBn: String) {
    AUTO_TAP("অটো দাগ সিলেক্ট"),
    POLYGON_DRAW("ম্যানুয়াল পলিগন")
}

data class MouzaPlot(
    val id: Int,
    val plotIndex: Int,
    val dagNumber: String = "",
    val khatianNumber: String = "",
    val pixelCount: Int,
    val maskIndices: IntArray, // Indices in detection grid (width * height)
    val gridWidth: Int,
    val gridHeight: Int,
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
    val color: Color,
    val manualPolygonNormalized: List<Pair<Float, Float>>? = null // Used when drawn manually
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MouzaPlot) return false
        return id == other.id
    }

    override fun hashCode(): Int = id
}

enum class MapLayerOption(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val tileUrl: String,
    val attribution: String,
    val maxZoom: Int = 19
) {
    ESRI_SATELLITE(
        id = "esri_satellite",
        titleBn = "Esri স্যাটেলাইট",
        titleEn = "Esri Satellite",
        tileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        attribution = "Tiles © Esri — World Imagery",
        maxZoom = 19
    ),
    OPEN_STREET_MAP(
        id = "osm",
        titleBn = "OpenStreetMap",
        titleEn = "OpenStreetMap",
        tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        attribution = "© OpenStreetMap contributors",
        maxZoom = 19
    ),
    ESRI_STREET(
        id = "esri_street",
        titleBn = "Esri স্ট্রিট",
        titleEn = "Esri Street",
        tileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}",
        attribution = "Tiles © Esri — World Street Map",
        maxZoom = 19
    ),
    ESRI_TOPO(
        id = "esri_topo",
        titleBn = "Esri টপোগ্রাফিক",
        titleEn = "Esri Topo",
        tileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}",
        attribution = "Tiles © Esri — World Topo Map",
        maxZoom = 19
    ),
    OPEN_TOPO_MAP(
        id = "open_topo",
        titleBn = "OpenTopoMap",
        titleEn = "OpenTopoMap",
        tileUrl = "https://tile.opentopomap.org/{z}/{x}/{y}.png",
        attribution = "© OpenTopoMap (CC-BY-SA)",
        maxZoom = 17
    )
}

enum class MapMeasureCategory(
    val id: String,
    val titleBn: String,
    val subtitleBn: String
) {
    AREA("area", "জমি এরিয়া", "বহুভুজ পিন করে জমির শতাংশ/কাঠা মাপুন"),
    DISTANCE("distance", "ফিতা / স্কেল", "দুই বা ততোধিক বিন্দুর দূরত্ব মাপুন"),
    GPS_WALK("gps_walk", "জিপিএস হেঁটে মাপ", "জমির আইল ধরে হেঁটে সরাসরি পিন যোগ করুন"),
    CIRCLE("circle", "বৃত্তাকার রেডিয়াস", "কেন্দ্র থেকে চারপাশের ব্যাসার্ধ ও এরিয়া"),
    SPLIT("split", "জমি বণ্টন গাইড", "জমিটি সমান ভাগে ভাগ করার স্কেল হিসাব")
}

enum class LandServiceCategory(val titleBn: String) {
    OWN_PORTALS("গুরুত্বপূর্ণ ও নিজস্ব পোর্টাল"),
    GOVT_LAND("সরকারি ভূমি ও খতিয়ান সেবা"),
    TAX_DEED("কর, নামজারি ও দলিল সেবা"),
    SURVEY_HELP("নকশা, আইন ও সহায়তা")
}

data class LandServicePortal(
    val id: String,
    val titleBn: String,
    val subtitleBn: String,
    val url: String,
    val category: LandServiceCategory,
    val badgeText: String,
    val isFeatured: Boolean = false
)

@Serializable
data class SavedMeasurementRecord(
    val id: String,
    val title: String,
    val source: String, // "Mouza PDF" or "Real Map"
    val decimalArea: Double,
    val kathaArea: Double,
    val sqFeetArea: Double,
    val timestampMillis: Long
)
