package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddRoad
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MasterCadRepository
import com.example.data.WorkstationRepository
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuFeatureCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary

private enum class HubCategory(val labelBn: String) {
    ALL("সকল"),
    CAD("CAD ও ডাইমেনশন"),
    COGO("COGO ও জরিপ"),
    MOUZA("মৌজা ও খতিয়ান"),
    DEED("দলিল (পুরাতন কাগজ)"),
    LAND_OFFICE("ভূমি অফিস ও সেবা"),
    REPORTS_FILES("রিপোর্ট ও ফাইল")
}

private data class WorkstationModuleItem(
    val number: Int,
    val titleBn: String,
    val subtitle: String,
    val badge: String,
    val category: HubCategory,
    val accent: Color,
    val icon: ImageVector,
    val keywords: String,
    val testTag: String,
    val onOpen: () -> Unit
)

@Composable
fun WorkstationHubScreen(
    onBack: () -> Unit,
    onOpenMouzaPdf: () -> Unit,
    onOpenPdfCad: () -> Unit,
    onOpenPantograph: () -> Unit,
    onOpenRealMap: () -> Unit,
    onOpenGpsGis: () -> Unit,
    onOpenTools: (String) -> Unit,
    onOpenPolygonDivider: () -> Unit,
    onOpenCustomKhatian: () -> Unit,
    onOpenDeedStudio: () -> Unit,
    onOpenScannerOcr: () -> Unit,
    onOpenReportStudio: () -> Unit,
    onOpenLandDatabase: () -> Unit,
    onOpenLegalLibrary: () -> Unit,
    onOpenCustomBuilder: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenOldMouzaLab: () -> Unit = {},
    onOpenFieldInstruments: () -> Unit = {},
    onOpenOnlineServiceWorkbench: () -> Unit = {},
    onOpenRevenueDueDiligence: () -> Unit = {},
    onOpenDeedLabPro: () -> Unit = {},
    onOpenAreaInvestigation: () -> Unit = {},
    onOpenSpecializedTools: () -> Unit = {},
    onOpenCustomServiceEngine: () -> Unit = {},
    onOpenUniversalArchive: () -> Unit = {},
    onOpenCadEngine: (String) -> Unit = {},
    onOpenDimensionSystem: (String) -> Unit = {},
    onOpenCogoEngine: (String) -> Unit = {},
    onOpenHistoricalDeed: () -> Unit = {},
    onOpenDocumentArchiver: () -> Unit = {},
    onOpenAcLandOffice: (String) -> Unit = {},
    onOpenFieldToCad: () -> Unit = {},
    onOpenSurfaceTopo: () -> Unit = {},
    onOpenRoadAccess: () -> Unit = {},
    onOpenAutomatedSchedule: () -> Unit = {},
    onOpenSurveyQuality: () -> Unit = {},
    onOpenFileCompatibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    val masterRepo = remember(context) { MasterCadRepository.getInstance(context) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(HubCategory.ALL) }
    var toast by remember { mutableStateOf<String?>(null) }

    val modules = remember {
        listOf(
            // Separated CAD & Dimensioning Modules
            WorkstationModuleItem(1, "2D CAD ড্রয়িং ও OSNAP (15 মোড)", "Line, Polyline, Circle, Arc, Ellipse, Spline ও Snap", "CAD Engine", HubCategory.CAD, Color(0xFF00695C), Icons.Rounded.Architecture, "cad drawing osnap polyline circle spline hatch", "hub_mod_cad_draw") { onOpenCadEngine("draw") },
            WorkstationModuleItem(2, "রৈখিক (Linear) CAD ডাইমেনশন", "Linear, Horizontal, Vertical, Aligned, Baseline, Continue", "Linear Dim", HubCategory.CAD, Color(0xFF0277BD), Icons.Rounded.Straighten, "linear horizontal vertical aligned baseline continue ordinate", "hub_mod_dim_linear") { onOpenDimensionSystem("linear") },
            WorkstationModuleItem(3, "কৌণিক ও ব্যাসার্ধ (Angular) ডাইমেনশন", "Angular, 2/3-Pt, Radius, Diameter, Arc Length, Center", "Angular Dim", HubCategory.CAD, Color(0xFF283593), Icons.Rounded.Architecture, "angular radius diameter jogged arc length center mark", "hub_mod_dim_angular") { onOpenDimensionSystem("angular") },
            WorkstationModuleItem(4, "লিডার ও অ্যানোটেশন (Leader)", "Leader, Multileader, Break, Spacing, Alignment, Override", "Leader Dim", HubCategory.CAD, Color(0xFF5E35B1), Icons.Rounded.Rule, "leader multileader break spacing align override edit", "hub_mod_dim_leader") { onOpenDimensionSystem("leader") },
            WorkstationModuleItem(5, "CAD Modify, টপোলজি ও ১৬ লেয়ার", "Move, Rotate, Scale, Offset, Trim, Extend, Fillet ও Layer", "16 Layers", HubCategory.CAD, Color(0xFF00796B), Icons.Rounded.Layers, "cad modify trim extend offset mirror array topology 16 layers", "hub_mod_cad_modify") { onOpenCadEngine("modify") },
            WorkstationModuleItem(6, "A0–A4 পেপার স্পেস ও DXF/SVG", "মৌজা টাইটেল ব্লক, স্কেল বার ও CAD শিট প্রিন্ট", "A0–A4 CAD", HubCategory.CAD, Color(0xFF1565C0), Icons.Rounded.PictureAsPdf, "paper space a0 a1 a2 a3 a4 dxf svg title block", "hub_mod_cad_layout") { onOpenCadEngine("layout") },

            // Separated COGO & Survey Modules
            WorkstationModuleItem(7, "COGO পয়েন্ট, ইনভার্স ও রেডিয়েশন", "Point Database, Inverse Bearing/Dist ও Radiation", "COGO Pt", HubCategory.COGO, EmeraldPrimary, Icons.Rounded.GpsFixed, "cogo point inverse radiation bearing azimuth", "hub_mod_cogo_point") { onOpenCogoEngine("point") },
            WorkstationModuleItem(8, "COGO ইন্টারসেকশন ও রিসেকশন", "Bearing-Bearing, Dist-Dist, Perp Drop ও 3-Pt Resection", "Intersect", HubCategory.COGO, Color(0xFF00838F), Icons.Rounded.Explore, "cogo intersection bearing distance resection perpendicular", "hub_mod_cogo_intersect") { onOpenCogoEngine("intersect") },
            WorkstationModuleItem(9, "COGO ট্র্যাভার্স (Bowditch / Transit / LS)", "ক্লোজড/ওপেন ট্র্যাভার্স সমন্বয় ও নির্ভুলতা অনুপাত", "Traverse", HubCategory.COGO, Color(0xFF2E7D32), Icons.Rounded.Calculate, "cogo traverse bowditch transit least squares misclosure", "hub_mod_cogo_traverse") { onOpenCogoEngine("traverse") },
            WorkstationModuleItem(10, "COGO সার্কুলার কার্ভ (Circular Curves)", "Radius, Tangent, Arc, Chord, Mid-Ordinate ও External", "Curves", HubCategory.COGO, Color(0xFF558B2F), Icons.Rounded.Architecture, "circular curve road radius tangent chord external", "hub_mod_cogo_curve") { onOpenCogoEngine("curve") },
            WorkstationModuleItem(11, "COGO হিঞ্জড ও স্লাইডিং জমি বিভাজন", "Hinged Point, Parallel Sliding ও Area Target Split", "Hinge/Slide", HubCategory.COGO, Color(0xFFD81B60), Icons.Rounded.ContentCut, "hinged sliding parallel polygon subdivision target area", "hub_mod_cogo_subdivide") { onOpenCogoEngine("subdivide") },
            WorkstationModuleItem(12, "ফিল্ড-টু-CAD আমদানি (PNEZD/NMEA)", "১১ ফরম্যাট পয়েন্ট ইম্পোর্ট, কোড-টু-লাইনওয়ার্ক ও Auto Layer", "Field-CAD", HubCategory.COGO, Color(0xFF00695C), Icons.Rounded.UploadFile, "field to cad pnezd csv txt dxf kml geojson gpx landxml nmea", "hub_mod_field_cad", onOpenFieldToCad),
            WorkstationModuleItem(13, "সার্ফেস TIN, কন্টুর ও কাটিং/ভরাট", "Spot Elevation, Delaunay TIN, কন্টুর ও Earthwork", "TIN / 3D", HubCategory.COGO, Color(0xFF4E342E), Icons.Rounded.Terrain, "tin contour cut fill earthwork elevation slope", "hub_mod_surface_tin", onOpenSurfaceTopo),
            WorkstationModuleItem(14, "রাস্তা অ্যালাইনমেন্ট ও প্রবেশপথ যাচাই", "স্টেশন/অফসেট, রোড ক্লিয়ারেন্স ও সেতু/প্রবেশপথ চেক", "Road/Access", HubCategory.COGO, Color(0xFF37474F), Icons.Rounded.AddRoad, "road alignment station offset plot access corridor", "hub_mod_road_access", onOpenRoadAccess),
            WorkstationModuleItem(15, "সার্ভে কোয়ালিটি কন্ট্রোল (QC) অডিট", "অতিবিকৃত আকার, ক্ষুদ্র স্পাইক, বন্ধন ও এরিয়া অসামঞ্জস্য", "QC Audit", HubCategory.COGO, Color(0xFFC62828), Icons.Rounded.FactCheck, "quality control qc audit sliver spike misclosure tolerance", "hub_mod_qc_audit", onOpenSurveyQuality),

            // Separated Historical Deed & Vintage Paper Modules
            WorkstationModuleItem(16, "পুরাতন দলিল পুনর্গঠন ও বাংলা OCR", "ছেঁড়া/ঝাপসা স্ক্যান পরিষ্কার, পার্সার ও অরিজিনাল/আধুনিক খসড়া", "Vintage Deed", HubCategory.DEED, Color(0xFF6D4C41), Icons.Rounded.HistoryEdu, "historical deed cleanup ocr bangla vintage paper", "hub_mod_hist_deed", onOpenHistoricalDeed),
            WorkstationModuleItem(17, "দলিল ও নথি সংরক্ষণাগার (Archiver)", "১২+ ঐতিহাসিক দলিলের টেমপ্লেট স্ক্রলযোগ্য পুরাতন কাগজে", "Archiver", HubCategory.DEED, Color(0xFF5D4037), Icons.Rounded.Inventory2, "document archiver parchment vintage templates deed", "hub_mod_doc_archiver", onOpenDocumentArchiver),
            WorkstationModuleItem(18, "স্বয়ংক্রিয় তফসিল জেনারেটর (ভিন্টেজ)", "চৌহদ্দি, সাবেক-হাল দাগ ও বাংলা/ইংরেজি তফসিল পুরাতন কাগজে", "Tafsil", HubCategory.DEED, Color(0xFF8D6E63), Icons.Rounded.Description, "automated schedule tafsil boundary vintage", "hub_mod_auto_schedule", onOpenAutomatedSchedule),
            WorkstationModuleItem(19, "দলিল ও আইনি খসড়া স্টুডিও", "সাফ-কবলা, বণ্টননামা, হেবা, বায়না — পুরাতন কাগজ PDF ও DOCX", "PDF+DOCX", HubCategory.DEED, Color(0xFF8E24AA), Icons.Rounded.Gavel, "deed safkabala heba bayna docx pdf vintage", "hub_mod_10", onOpenDeedStudio),
            WorkstationModuleItem(20, "দলিল ও তফসিল ল্যাব প্রো (E01–05)", "বহু-দাতা/গ্রহীতা, তফসিল ভুল যাচাই ও আইনি ধারা (ভিন্টেজ)", "Part E", HubCategory.DEED, Color(0xFF6A1B9A), Icons.Rounded.HistoryEdu, "deed lab schedule tafsil mismatch legal clause docx pdf", "hub_mod_23", onOpenDeedLabPro),

            // Separated Land Office & Records Modules
            WorkstationModuleItem(21, "খতিয়ান তুলনা ও রেকর্ড কম্পিউটার", "CS/SA/RS/BS/BRS তুলনা ও নামজারি যাচাই", "Khatian Cmp", HubCategory.LAND_OFFICE, Color(0xFF0F6B54), Icons.Rounded.Compare, "khatian cs sa rs bs brs compare mutation", "hub_mod_ac_khatian") { onOpenAcLandOffice("khatian") },
            WorkstationModuleItem(22, "১৫ ইউনিয়ন ভূমি অফিস রেজিস্টার", "রেজিস্টার I–XV (১–১৫) অফলাইন ডিজিটাল এন্ট্রি ও খোঁজ", "15 Registers", HubCategory.LAND_OFFICE, Color(0xFF1B5E20), Icons.Rounded.MenuBook, "union land office registers i xv mutation sairat hat bazar", "hub_mod_ac_registers") { onOpenAcLandOffice("registers") },
            WorkstationModuleItem(23, "তারিখভিত্তিক খাজনা (LD Tax) ও বকেয়া", "হিসাব বছর ও সুদ-বকেয়াসহ বাংলা সন খাজনা ক্যালকুলেটর", "LD Tax", HubCategory.LAND_OFFICE, SurveyGold, Icons.Rounded.AccountBalance, "ld tax khajna dakhila arrear interest date range", "hub_mod_ac_tax") { onOpenAcLandOffice("tax") },
            WorkstationModuleItem(24, "এসি ল্যান্ড কেস ও ১১ দলিল নির্দেশিকা", "ই-নামজারি ফ্লো, আপিল ও ১১ দলিল টেমপ্লেট রেজিস্ট্রি", "AC Land", HubCategory.LAND_OFFICE, Color(0xFF283593), Icons.Rounded.Gavel, "ac land sub registry 11 deed categories mutation", "hub_mod_ac_reg") { onOpenAcLandOffice("registration") },

            // Core Mouza, Map & Existing 27 Modules
            WorkstationModuleItem(25, "মৌজা ম্যাপ স্টুডিও", "CS/SA/RS/BS/BRS PDF ও ম্যাপ পরিমাপ", "1000x HD", HubCategory.MOUZA, EmeraldPrimary, Icons.Rounded.PictureAsPdf, "mouza pdf map scale dag", "hub_mod_1", onOpenMouzaPdf),
            WorkstationModuleItem(26, "PDF ও CAD স্টুডিও", "PDF/JPG রূপান্তর, মার্জ/স্প্লিট, স্ন্যাপ CAD ও DXF", "CAD + DXF", HubCategory.CAD, Color(0xFF00796B), Icons.Rounded.AutoFixHigh, "pdf cad dxf image clean deskew", "hub_mod_2", onOpenPdfCad),
            WorkstationModuleItem(27, "প্যান্টোগ্রাফ ও নকশা তুলনা", "CS বনাম RS/BS ওভারলে, কন্ট্রোল পয়েন্ট ও TPS/Affine", "Pantograph", HubCategory.MOUZA, Color(0xFF5E35B1), Icons.Rounded.Compare, "pantograph cs sa rs bs affine tps overlay", "hub_mod_3", onOpenPantograph),
            WorkstationModuleItem(28, "স্যাটেলাইট ল্যান্ড সার্ভে", "HD Google/Esri/OSM ম্যাপ, পলিগন ও ওভারলে", "Satellite", HubCategory.MOUZA, TealSecondary, Icons.Rounded.Public, "satellite map overlay google esri", "hub_mod_4", onOpenRealMap),
            WorkstationModuleItem(29, "GPS ফিল্ড সার্ভে ও RTK", "ওয়েপয়েন্ট, NMEA/RTK, বিয়ারিং, আজিমুথ ও ট্র্যাক", "GPS / NMEA", HubCategory.COGO, Color(0xFF1A73E8), Icons.Rounded.GpsFixed, "gps field survey nmea rtk bearing", "hub_mod_5", onOpenGpsGis),
            WorkstationModuleItem(30, "জরিপ ও আঞ্চলিক একক ক্যালকুলেটর", "শতাংশ, কানি-গণ্ডা (চট্টগ্রাম/নোয়াখালী), হেরন, ট্র্যাভার্স", "Regional", HubCategory.COGO, SurveyGold, Icons.Rounded.Calculate, "calculator kani gonda katha traverse shoelace", "hub_mod_6") { onOpenTools("unit") },
            WorkstationModuleItem(31, "স্বয়ংক্রিয় জমি বিভাজন", "সমান/শতাংশ/রাস্তা রেখে পলিগন বিভাজন ও চিত্র সেভ", "Auto Split", HubCategory.COGO, Color(0xFFD81B60), Icons.Rounded.ContentCut, "divider split road percentage polygon", "hub_mod_7", onOpenPolygonDivider),
            WorkstationModuleItem(32, "খতিয়ান ও দাগ ম্যানেজার (ভিন্টেজ)", "CS/SA/RS/BS/BRS খতিয়ান, হিস্যা ও পুরাতন কাগজে PNG/PDF", "Khatian", HubCategory.MOUZA, Color(0xFF0F6B54), Icons.Rounded.Description, "khatian cs sa rs bs brs dag hissa vintage", "hub_mod_8", onOpenCustomKhatian),
            WorkstationModuleItem(33, "উত্তরাধিকার ও ফারায়েজ", "মুসলিম ফারায়েজ, হিন্দু দায়ভাগ, বৌদ্ধ ও খ্রিস্টান আইন", "4 Laws", HubCategory.DEED, Color(0xFF00897B), Icons.Rounded.Balance, "inheritance faraid hindu muslim christian buddhist", "hub_mod_9") { onOpenTools("faraiz") },
            WorkstationModuleItem(34, "ডকুমেন্ট স্ক্যানার ও OCR", "বাংলা/ইংরেজি খতিয়ান ও দাগ এক্সট্র্যাক্ট ও যাচাই", "Bangla OCR", HubCategory.DEED, Color(0xFFE65100), Icons.Rounded.DocumentScanner, "scanner ocr bangla extract table", "hub_mod_11", onOpenScannerOcr),
            WorkstationModuleItem(35, "পেশাদার সার্ভে রিপোর্ট স্টুডিও", "A4/A3 রিপোর্ট, উত্তর দিক, QR ও স্বাক্ষর ব্লক", "A4 / A3", HubCategory.REPORTS_FILES, Color(0xFF1565C0), Icons.Rounded.Summarize, "report memo demarcation a4 a3 qr", "hub_mod_12", onOpenReportStudio),
            WorkstationModuleItem(36, "ব্যক্তিগত ভূমি ডাটাবেজ ও ভল্ট", "মালিক/মৌজা ফোল্ডার, AES-256 ব্যাকআপ ও PIN লক", "Encrypted", HubCategory.REPORTS_FILES, EmeraldPrimary, Icons.Rounded.FolderShared, "database vault backup restore aes pin", "hub_mod_13", onOpenLandDatabase),
            WorkstationModuleItem(37, "আইনি ও সরকারি তথ্য লাইব্রেরি", "জরিপ বিধিমালা, পর্চা গাইড ও দলিল সংরক্ষণাগার", "Offline Guide", HubCategory.DEED, Color(0xFF3949AB), Icons.Rounded.MenuBook, "library law rules cs rs bs integration archiver", "hub_mod_14", onOpenLegalLibrary),
            WorkstationModuleItem(38, "আর্থিক ও দলিল খরচ ক্যালকুলেটর", "মৌজা রেট, স্ট্যাম্প, খাজনা, নামজারি ও লাভ/ক্ষতি", "Tax & Fee", HubCategory.LAND_OFFICE, SurveyGold, Icons.Rounded.AccountBalance, "financial market stamp tax mutation profit", "hub_mod_15") { onOpenTools("tax") },
            WorkstationModuleItem(39, "GIS ও স্থানাঙ্ক রূপান্তর টুলস", "WGS84, UTM (45N/46N), DMS, KML/KMZ/GPX/GeoJSON/DXF", "WGS84/UTM", HubCategory.COGO, TealSecondary, Icons.Rounded.Language, "gis utm wgs84 dms kml kmz gpx geojson", "hub_mod_16", onOpenGpsGis),
            WorkstationModuleItem(40, "কাস্টম ফর্ম ও ফর্মুলা বিল্ডার", "নিজস্ব ফর্ম, ড্রপডাউন, ফর্মুলা ও JSON আমদানি/রপ্তানি", "Builder", HubCategory.REPORTS_FILES, Color(0xFF6D4C41), Icons.Rounded.Build, "custom template formula builder json", "hub_mod_17", onOpenCustomBuilder),
            WorkstationModuleItem(41, "প্রজেক্ট ও সরকারি পোর্টাল হাব", "সমন্বিত প্রজেক্ট ম্যানেজমেন্ট ও সরকারি পোর্টাল", "Projects", HubCategory.LAND_OFFICE, Color(0xFF2E7D32), Icons.Rounded.FolderShared, "project management dlrms portal", "hub_mod_18", onOpenServices),
            WorkstationModuleItem(42, "পুরনো মৌজা ম্যাপ ও দাগ ল্যাব (A01–09)", "ম্যাপ পরিষ্কার, স্কেল বিকৃতি, সাবেক-হাল দাগ ও A0–A4 প্রিন্ট", "Part A", HubCategory.MOUZA, Color(0xFF4E342E), Icons.Rounded.Layers, "old mouza clean distortion genealogy adjacency a0 a3 print", "hub_mod_19", onOpenOldMouzaLab),
            WorkstationModuleItem(43, "সার্ভে যন্ত্রপাতি ও ফিল্ড বুক (B01–08)", "Total Station, Theodolite, Level Book, EDM, Cut-Fill", "Part B", HubCategory.COGO, Color(0xFF00695C), Icons.Rounded.Engineering, "total station theodolite auto level edm gnss cut fill xlsx", "hub_mod_20", onOpenFieldInstruments),
            WorkstationModuleItem(44, "অনলাইন ভূমি সেবা কর্মক্ষেত্র (C01–08)", "নামজারি ৮ উৎস, খাজনা বকেয়া, খতিয়ান ও ৮ পোর্টাল হ্যান্ডঅফ", "Part C+I", HubCategory.LAND_OFFICE, Color(0xFF1565C0), Icons.Rounded.Public, "namjari mutation khajna dakhila dcr portal bd", "hub_mod_21", onOpenOnlineServiceWorkbench),
            WorkstationModuleItem(45, "রাজস্ব মামলা ও ডিউ ডিলিজেন্স (D01–05)", "মিস কেস, অধিগ্রহণ ক্ষতিপূরণ, খাস/লিজ ও ঝুঁকি রিপোর্ট", "Part D", HubCategory.LAND_OFFICE, Color(0xFFAD1457), Icons.Rounded.Rule, "revenue case acquisition khas lease dispute due diligence", "hub_mod_22", onOpenRevenueDueDiligence),
            WorkstationModuleItem(46, "ক্ষেত্রফল তদন্ত ও সমন্বয় ল্যাব (F01–09)", "৬-উৎস তুলনা, রাস্তা/পুকুর বাদ নিট জমি ও ফ্রন্টেজ বিভাজন", "Part F", HubCategory.COGO, Color(0xFFEF6C00), Icons.Rounded.SquareFoot, "area reconciliation usable land road pond frontage valuation", "hub_mod_24", onOpenAreaInvestigation),
            WorkstationModuleItem(47, "বিশেষায়িত সার্ভে ও ইনভয়েস (G01–07)", "নদীভাঙন, ভবন কভারেজ/সেটব্যাক, যন্ত্রপাতি ও বিল PDF", "Part G", HubCategory.REPORTS_FILES, Color(0xFF00838F), Icons.Rounded.Build, "riverbank erosion building coverage calibration invoice certificate", "hub_mod_25", onOpenSpecializedTools),
            WorkstationModuleItem(48, "কাস্টম ভূমি সেবা অটোমেশন (Part H)", "নিজস্ব সেবা, পুনঃসাজানো ধাপ, শর্ত ও ফি ফর্মুলা ইঞ্জিন", "Part H", HubCategory.LAND_OFFICE, Color(0xFF283593), Icons.Rounded.SettingsSuggest, "custom service automation workflow steps formula json", "hub_mod_26", onOpenCustomServiceEngine),
            WorkstationModuleItem(49, "সর্বজনীন আর্কাইভ ও রপ্তানি (J01–04)", "১৪-ধরনের নথি, তুলনা ও XLSX / Shapefile / DXF / PDF রপ্তানি", "Part J", HubCategory.REPORTS_FILES, Color(0xFF1B5E20), Icons.Rounded.Inventory2, "archive search compare xlsx shapefile dxf kml kmz gpx pdf", "hub_mod_27", onOpenUniversalArchive),
            WorkstationModuleItem(50, "ফাইল কম্প্যাটিবিলিটি ও Master ZIP", "DWG/DGN/GeoTIFF রূপান্তর গাইড ও একক Master Project Bundle", "ZIP + Matrix", HubCategory.REPORTS_FILES, Color(0xFF0D47A1), Icons.Rounded.Inventory2, "file compatibility dwg dgn geotiff master project zip export", "hub_mod_file_hub", onOpenFileCompatibility)
        )
    }

    val filtered = remember(search, selectedCategory) {
        modules.filter { mod ->
            val catOk = selectedCategory == HubCategory.ALL || mod.category == selectedCategory
            val qOk = search.isBlank() ||
                mod.titleBn.contains(search, true) ||
                mod.subtitle.contains(search, true) ||
                mod.keywords.contains(search, true) ||
                mod.number.toString() == search.trim()
            catOk && qOk
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        val cols = if (maxWidth >= 760.dp) 3 else 2

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1100.dp)
        ) {
            WorkstationTopBar(
                titleBn = "সার্ভেয়ার ওয়ার্কস্টেশন (৫০টি আলাদা মডিউল)",
                subtitleEn = "Modular CAD + 30 Dimensions + COGO + Vintage Deeds + Land Office Suite",
                helpTitle = "আলাদা মডিউল ব্যবহার নির্দেশিকা",
                helpSteps = listOf(
                    "প্রতিটি টুলস (CAD ডাইমেনশন, COGO, দলিল সংরক্ষণাগার, খতিয়ান ও ভূমি অফিস) আলাদা আলাদা কার্ডে সাজানো আছে।",
                    "উপরের ক্যাটাগরি ফিল্টার বা সার্চ বক্স ব্যবহার করে দ্রুত যেকোনো মডিউল খুলুন।",
                    "নমুনা লোড চাপলে পুবাইল মৌজার CAD স্কেচ, COGO পয়েন্ট, ১৫ রেজিস্টার ও ঐতিহাসিক দলিল যুক্ত হবে।"
                ),
                onBack = onBack,
                backTestTag = "hub_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("মডিউল খুঁজুন (যেমন: Linear, Angular, COGO, দলিল, খতিয়ান, TIN)...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_search_input")
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(HubCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.labelBn, maxLines = 1) },
                                modifier = Modifier.testTag("hub_cat_${cat.name.lowercase()}")
                            )
                        }
                    }
                }

                item {
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "নমুনা প্রকল্প ও মাস্টার CAD ডাটা লোড",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "পুবাইল মৌজার CAD নকশা, COGO পয়েন্ট, রেজিস্টার ও ঐতিহাসিক দলিল লোড করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repo.seedSampleBangladeshProject()
                                    masterRepo.seedPubailMasterSample()
                                    toast = "মাস্টার নমুনা প্রজেক্ট, CAD, COGO ও দলিল যুক্ত হয়েছে!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("hub_seed_sample_button")
                            ) {
                                Text("নমুনা লোড", maxLines = 1)
                            }
                        }
                    }
                }

                items(filtered.chunked(cols), key = { row -> row.joinToString("_") { it.testTag } }) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { mod ->
                            NeuFeatureCard(
                                title = "${mod.number}. ${mod.titleBn}",
                                subtitle = mod.subtitle,
                                icon = mod.icon,
                                accentColor = mod.accent,
                                badge = mod.badge,
                                testTag = mod.testTag,
                                modifier = Modifier.weight(1f),
                                onClick = mod.onOpen
                            )
                        }
                        repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
