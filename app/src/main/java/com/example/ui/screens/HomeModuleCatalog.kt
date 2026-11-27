package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.AboutRoute
import com.example.model.AcLandOfficeRoute
import com.example.model.AccuracyReportRoute
import com.example.model.AiAssistantRoute
import com.example.model.AreaInvestigationRoute
import com.example.model.AutomatedScheduleRoute
import com.example.model.CadEngineRoute
import com.example.model.CogoEngineRoute
import com.example.model.CustomBuilderRoute
import com.example.model.CustomKhatianRoute
import com.example.model.CustomServiceEngineRoute
import com.example.model.DeedLabProRoute
import com.example.model.DeedStudioRoute
import com.example.model.DimensionSystemRoute
import com.example.model.DocumentArchiverRoute
import com.example.model.FieldInstrumentsRoute
import com.example.model.FieldToCadRoute
import com.example.model.FileCompatibilityRoute
import com.example.model.GpsGisToolsRoute
import com.example.model.HistoricalDeedRoute
import com.example.model.LandDatabaseRoute
import com.example.model.LegalLibraryRoute
import com.example.model.MouzaPdfRoute
import com.example.model.OldMouzaLabRoute
import com.example.model.OnlineLandServiceWorkbenchRoute
import com.example.model.PantographRoute
import com.example.model.PdfCadStudioRoute
import com.example.model.PolygonDividerRoute
import com.example.model.RealMapRoute
import com.example.model.ReportStudioRoute
import com.example.model.RevenueDueDiligenceRoute
import com.example.model.RoadAccessRoute
import com.example.model.ScannerOcrRoute
import com.example.model.ServicesRoute
import com.example.model.SpecializedSurveyToolsRoute
import com.example.model.SurfaceTopoRoute
import com.example.model.SurveyQualityRoute
import com.example.model.ToolsRoute
import com.example.model.UniversalArchiveExportRoute
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary

enum class HomeModuleCategory(val labelBn: String) {
    ALL("সকল টুলস"),
    MOUZA_MAP("মৌজা ও ম্যাপ"),
    CAD("CAD ও ডাইমেনশন"),
    COGO("COGO ও জরিপ"),
    DEED("দলিল (পুরাতন কাগজ)"),
    LAND_OFFICE("খতিয়ান ও ভূমি অফিস"),
    CALC_SPLIT("গণনা ও বিভাজন"),
    REPORTS_FILES("রিপোর্ট ও ফাইল")
}

data class HomeModuleSpec(
    val id: String,
    val number: Int,
    val titleBn: String,
    val subtitleBn: String,
    val badge: String,
    val category: HomeModuleCategory,
    val accent: Color,
    val icon: ImageVector,
    val keywords: String,
    val testTag: String,
    val route: Any
)

object HomeModuleCatalog {
    val allModules: List<HomeModuleSpec> = listOf(
        // Phase 1 Flagship Mapping & Measurement Modules
        HomeModuleSpec("mod_mouza_pdf", 1, "মৌজা নকশা ও PDF মাপ", "1000x HD জুম, ৫-পদ্ধতি স্কেল, অটো-ফিল, তফসিল ও Annotated PDF", "1000x HD", HomeModuleCategory.MOUZA_MAP, EmeraldPrimary, Icons.Rounded.PictureAsPdf, "mouza pdf map scale dag auto fill cleaner annotated csv", "card_mouza_pdf", MouzaPdfRoute()),
        HomeModuleSpec("mod_real_map", 2, "রিয়েল স্যাটেলাইট ও KMZ", "Google/Esri/OSM, Mouza Overlay, Split Cursor, Parcel Snap ও KMZ", "Satellite", HomeModuleCategory.MOUZA_MAP, TealSecondary, Icons.Rounded.Public, "satellite map overlay google esri kmz kml geojson snap", "card_real_map", RealMapRoute()),
        HomeModuleSpec("mod_pantograph", 3, "প্যান্টোগ্রাফ (CS↔RS/BS)", "Scale 1:X→1:Y, Affine/Homography/TPS, Outlier RMSE ও ওভারল্যাপ টেবিল", "Pantograph", HomeModuleCategory.MOUZA_MAP, Color(0xFF5E35B1), Icons.Rounded.Compare, "pantograph cs sa rs bs affine tps homography overlap", "card_pantograph", PantographRoute),
        HomeModuleSpec("mod_accuracy_report", 4, "নির্ভুলতা (±) ও ফিতা পলিগন", "স্কেল/জুম/GPS ত্রুটি প্রোপাগেশন, আস্থা স্তর, ফিতা পলিগন ও PDF", "± Error", HomeModuleCategory.CALC_SPLIT, Color(0xFF00695C), Icons.Rounded.Verified, "accuracy error confidence uncertainty tape polygon builder m01 m02", "card_accuracy_report", AccuracyReportRoute),
        HomeModuleSpec("mod_old_mouza_lab", 5, "ম্যাপ পরিষ্কার, বিকৃতি ও শিট জোড়া", "আগে-পরে ক্লিনার, X/Y সংকোচন, সাবেক-হাল দাগ ও শিট জয়েন্ট", "Sheet Join", HomeModuleCategory.MOUZA_MAP, Color(0xFF4E342E), Icons.Rounded.Layers, "old mouza clean distortion genealogy sheet join", "card_old_mouza_lab", OldMouzaLabRoute),

        // Separated Land Division & Calculators
        HomeModuleSpec("mod_polygon_divider", 6, "জমি বিভাজন (Auto Split)", "সমান/শতাংশ/রাস্তা রেখে পলিগন বণ্টন ও চিত্র সেভ", "Auto Split", HomeModuleCategory.CALC_SPLIT, Color(0xFFD81B60), Icons.Rounded.ContentCut, "divider split road percentage polygon", "card_polygon_divider", PolygonDividerRoute),
        HomeModuleSpec("mod_calc_unit", 7, "জরিপ ও আঞ্চলিক একক ক্যালকুলেটর", "শতাংশ, কানি-গণ্ডা, ছটাক, গুন্থা, রশি, হেরন ও ট্র্যাভার্স", "14 Units", HomeModuleCategory.CALC_SPLIT, SurveyGold, Icons.Rounded.Calculate, "calculator kani gonda katha bigha guntha chotak roshi traverse", "card_tools", ToolsRoute("unit")),
        HomeModuleSpec("mod_faraiz", 8, "উত্তরাধিকার ও ফারায়েজ", "মুসলিম ফারায়েজ, হিন্দু দায়ভাগ, বৌদ্ধ ও খ্রিস্টান আইন", "4 Laws", HomeModuleCategory.CALC_SPLIT, Color(0xFF00897B), Icons.Rounded.Balance, "inheritance faraid hindu muslim christian buddhist", "card_faraiz", ToolsRoute("faraiz")),
        HomeModuleSpec("mod_fin_tax", 9, "আর্থিক ও দলিল খরচ ক্যালকুলেটর", "মৌজা রেট, স্ট্যাম্প, রেজিস্ট্রেশন, খাজনা ও লাভ/ক্ষতি", "Tax & Fee", HomeModuleCategory.CALC_SPLIT, SurveyGold, Icons.Rounded.AccountBalance, "financial market stamp registration tax profit", "card_fin_tax", ToolsRoute("tax")),
        HomeModuleSpec("mod_area_investigate", 10, "ক্ষেত্রফল তদন্ত ও সমন্বয় ল্যাব", "৬-উৎস তুলনা, পুকুর/রাস্তা বাদ নিট জমি ও ফ্রন্টেজ বিভাজন", "Part F", HomeModuleCategory.CALC_SPLIT, Color(0xFFEF6C00), Icons.Rounded.SquareFoot, "area reconciliation usable land road pond frontage valuation", "card_area_investigate", AreaInvestigationRoute),

        // Separated Khatian & Land Office Modules
        HomeModuleSpec("mod_custom_khatian", 11, "কাস্টম খতিয়ান তৈরি (ভিন্টেজ)", "CS/SA/RS/BS খতিয়ান, ১৬ আনা হিস্যা ও পুরাতন কাগজে PNG/PDF", "Khatian", HomeModuleCategory.LAND_OFFICE, Color(0xFF00796B), Icons.Rounded.Description, "custom khatian cs sa rs bs brs hissa vintage", "card_custom_khatian", CustomKhatianRoute()),
        HomeModuleSpec("mod_ac_khatian", 12, "খতিয়ান তুলনা ও রেকর্ড কম্পিউটার", "CS/SA/RS/BS/BRS তুলনা ও নামজারি যাচাই", "Compare", HomeModuleCategory.LAND_OFFICE, Color(0xFF0F6B54), Icons.Rounded.Compare, "khatian cs sa rs bs brs compare mutation", "card_ac_khatian", AcLandOfficeRoute("khatian")),
        HomeModuleSpec("mod_ac_registers", 13, "১৫ ইউনিয়ন ভূমি অফিস রেজিস্টার", "রেজিস্টার I–XV (১–১৫) অফলাইন ডিজিটাল এন্ট্রি ও খোঁজ", "15 Reg", HomeModuleCategory.LAND_OFFICE, Color(0xFF1B5E20), Icons.Rounded.MenuBook, "union land office registers i xv mutation sairat hat bazar", "card_ac_registers", AcLandOfficeRoute("registers")),
        HomeModuleSpec("mod_ac_tax", 14, "তারিখভিত্তিক খাজনা (LD Tax)", "হিসাব বছর ও সুদ-বকেয়াসহ বাংলা সন খাজনা ক্যালকুলেটর", "LD Tax", HomeModuleCategory.LAND_OFFICE, SurveyGold, Icons.Rounded.AccountBalance, "ld tax khajna dakhila arrear interest date range", "card_ac_tax", AcLandOfficeRoute("tax")),
        HomeModuleSpec("mod_ac_reg", 15, "এসি ল্যান্ড কেস ও ১১ দলিল নির্দেশিকা", "ই-নামজারি ফ্লো, আপিল ও ১১ দলিল টেমপ্লেট রেজিস্ট্রি", "AC Land", HomeModuleCategory.LAND_OFFICE, Color(0xFF283593), Icons.Rounded.Gavel, "ac land sub registry 11 deed categories mutation", "card_ac_reg", AcLandOfficeRoute("registration")),
        HomeModuleSpec("mod_services", 16, "সকল সরকারি ভূমি পোর্টাল (২২+)", "নামজারি, খাজনা, DLRMS ও সরকারি পোর্টাল হাব", "22 Portals", HomeModuleCategory.LAND_OFFICE, Color(0xFF1A73E8), Icons.Rounded.Language, "services portals dlrms mutation khajna", "card_services", ServicesRoute),
        HomeModuleSpec("mod_online_workbench", 17, "অনলাইন ভূমি সেবা কর্মক্ষেত্র", "নামজারি ৮ উৎস, খাজনা বকেয়া, খতিয়ান ও ৮ পোর্টাল হ্যান্ডঅফ", "Part C+I", HomeModuleCategory.LAND_OFFICE, Color(0xFF1565C0), Icons.Rounded.Public, "namjari mutation khajna dakhila dcr portal bd", "card_online_workbench", OnlineLandServiceWorkbenchRoute),
        HomeModuleSpec("mod_revenue_dd", 18, "রাজস্ব মামলা ও ডিউ ডিলিজেন্স", "মিস কেস, অধিগ্রহণ ক্ষতিপূরণ, খাস/লিজ ও ঝুঁকি রিপোর্ট", "Part D", HomeModuleCategory.LAND_OFFICE, Color(0xFFAD1457), Icons.Rounded.Rule, "revenue case acquisition khas lease dispute due diligence", "card_revenue_dd", RevenueDueDiligenceRoute),
        HomeModuleSpec("mod_custom_service", 19, "কাস্টম ভূমি সেবা অটোমেশন", "নিজস্ব সেবা, পুনঃসাজানো ধাপ, শর্ত ও ফি ফর্মুলা ইঞ্জিন", "Part H", HomeModuleCategory.LAND_OFFICE, Color(0xFF283593), Icons.Rounded.SettingsSuggest, "custom service automation workflow steps formula json", "card_custom_service", CustomServiceEngineRoute),

        // Separated Historical Deed & Old Paper Modules
        HomeModuleSpec("mod_hist_deed", 20, "পুরাতন দলিল পুনর্গঠন ও বাংলা OCR", "ছেঁড়া/ঝাপসা স্ক্যান পরিষ্কার, পার্সার ও অরিজিনাল/আধুনিক খসড়া", "Vintage Deed", HomeModuleCategory.DEED, Color(0xFF6D4C41), Icons.Rounded.HistoryEdu, "historical deed cleanup ocr bangla vintage paper", "card_hist_deed", HistoricalDeedRoute),
        HomeModuleSpec("mod_doc_archiver", 21, "দলিল ও নথি সংরক্ষণাগার (Archiver)", "১২+ ঐতিহাসিক দলিলের টেমপ্লেট স্ক্রলযোগ্য পুরাতন কাগজে", "Archiver", HomeModuleCategory.DEED, Color(0xFF5D4037), Icons.Rounded.Inventory2, "document archiver parchment vintage templates deed", "card_doc_archiver", DocumentArchiverRoute),
        HomeModuleSpec("mod_auto_schedule", 22, "স্বয়ংক্রিয় তফসিল জেনারেটর", "চৌহদ্দি, সাবেক-হাল দাগ ও বাংলা/ইংরেজি তফসিল পুরাতন কাগজে", "Tafsil", HomeModuleCategory.DEED, Color(0xFF8D6E63), Icons.Rounded.Description, "automated schedule tafsil boundary vintage", "card_auto_schedule", AutomatedScheduleRoute),
        HomeModuleSpec("mod_deed_studio", 23, "দলিল ও আইনি খসড়া স্টুডিও", "সাফ-কবলা, বণ্টননামা, হেবা, বায়না — পুরাতন কাগজ PDF/DOCX", "PDF+DOCX", HomeModuleCategory.DEED, Color(0xFF8E24AA), Icons.Rounded.Gavel, "deed safkabala heba bayna docx pdf vintage", "card_deed_studio", DeedStudioRoute),
        HomeModuleSpec("mod_deed_lab_pro", 24, "দলিল ও তফসিল ল্যাব প্রো", "বহু-দাতা/গ্রহীতা, তফসিল ভুল যাচাই ও আইনি ধারা (ভিন্টেজ)", "Part E", HomeModuleCategory.DEED, Color(0xFF6A1B9A), Icons.Rounded.HistoryEdu, "deed lab schedule tafsil mismatch legal clause docx pdf", "card_deed_lab_pro", DeedLabProRoute),
        HomeModuleSpec("mod_scanner_ocr", 25, "ডকুমেন্ট স্ক্যানার ও বাংলা OCR", "বাংলা/ইংরেজি খতিয়ান ও দাগ এক্সট্র্যাক্ট ও যাচাই", "Bangla OCR", HomeModuleCategory.DEED, Color(0xFFE65100), Icons.Rounded.DocumentScanner, "scanner ocr bangla extract table", "card_scanner_ocr", ScannerOcrRoute),
        HomeModuleSpec("mod_legal_lib", 26, "আইনি ও সরকারি তথ্য লাইব্রেরি", "জরিপ বিধিমালা, পর্চা গাইড ও দলিল সংরক্ষণাগার", "Offline Guide", HomeModuleCategory.DEED, Color(0xFF3949AB), Icons.Rounded.MenuBook, "library law rules cs rs bs integration archiver", "card_legal_lib", LegalLibraryRoute),

        // Separated CAD & Dimensioning Modules
        HomeModuleSpec("mod_cad_draw", 27, "2D CAD ড্রয়িং ও OSNAP (১৫ মোড)", "Line, Polyline, Circle, Arc, Ellipse, Spline ও Snap", "CAD Draw", HomeModuleCategory.CAD, Color(0xFF00695C), Icons.Rounded.Architecture, "cad drawing osnap polyline circle spline hatch", "card_cad_draw", CadEngineRoute("draw")),
        HomeModuleSpec("mod_dim_linear", 28, "রৈখিক (Linear) CAD ডাইমেনশন", "Linear, Horizontal, Vertical, Aligned, Baseline, Continue", "Linear Dim", HomeModuleCategory.CAD, Color(0xFF0277BD), Icons.Rounded.Straighten, "linear horizontal vertical aligned baseline continue ordinate", "card_dim_linear", DimensionSystemRoute("linear")),
        HomeModuleSpec("mod_dim_angular", 29, "কৌণিক ও ব্যাসার্ধ (Angular) ডাইমেনশন", "Angular, 2/3-Pt, Radius, Diameter, Arc Length, Center", "Angular Dim", HomeModuleCategory.CAD, Color(0xFF283593), Icons.Rounded.Architecture, "angular radius diameter jogged arc length center mark", "card_dim_angular", DimensionSystemRoute("angular")),
        HomeModuleSpec("mod_dim_leader", 30, "লিডার ও অ্যানোটেশন (Leader)", "Leader, Multileader, Break, Spacing, Alignment, Override", "Leader Dim", HomeModuleCategory.CAD, Color(0xFF5E35B1), Icons.Rounded.Rule, "leader multileader break spacing align override edit", "card_dim_leader", DimensionSystemRoute("leader")),
        HomeModuleSpec("mod_cad_modify", 31, "CAD Modify, টপোলজি ও ১৬ লেয়ার", "Move, Rotate, Scale, Offset, Trim, Extend, Fillet ও Layer", "16 Layers", HomeModuleCategory.CAD, Color(0xFF00796B), Icons.Rounded.Layers, "cad modify trim extend offset mirror array topology 16 layers", "card_cad_modify", CadEngineRoute("modify")),
        HomeModuleSpec("mod_cad_layout", 32, "A0–A4 পেপার স্পেস ও DXF/SVG", "মৌজা টাইটেল ব্লক, স্কেল বার ও CAD শিট প্রিন্ট", "A0–A4 CAD", HomeModuleCategory.CAD, Color(0xFF1565C0), Icons.Rounded.PictureAsPdf, "paper space a0 a1 a2 a3 a4 dxf svg title block", "card_cad_layout", CadEngineRoute("layout")),
        HomeModuleSpec("mod_pdf_cad", 33, "PDF ও CAD স্টুডিও", "PDF/JPG রূপান্তর, মার্জ/স্প্লিট, স্ন্যাপ CAD ও DXF", "PDF + DXF", HomeModuleCategory.CAD, Color(0xFF00796B), Icons.Rounded.AutoFixHigh, "pdf cad dxf image clean deskew", "card_pdf_cad", PdfCadStudioRoute),

        // Separated COGO, Field & Engineering Survey Modules
        HomeModuleSpec("mod_cogo_point", 34, "COGO পয়েন্ট, ইনভার্স ও রেডিয়েশন", "Point Database, Inverse Bearing/Dist ও Radiation", "COGO Pt", HomeModuleCategory.COGO, EmeraldPrimary, Icons.Rounded.GpsFixed, "cogo point inverse radiation bearing azimuth", "card_cogo_point", CogoEngineRoute("point")),
        HomeModuleSpec("mod_cogo_intersect", 35, "COGO ইন্টারসেকশন ও রিসেকশন", "Bearing-Bearing, Dist-Dist, Perp Drop ও 3-Pt Resection", "Intersect", HomeModuleCategory.COGO, Color(0xFF00838F), Icons.Rounded.Explore, "cogo intersection bearing distance resection perpendicular", "card_cogo_intersect", CogoEngineRoute("intersect")),
        HomeModuleSpec("mod_cogo_traverse", 36, "COGO ট্র্যাভার্স (Bowditch / Transit)", "ক্লোজড/ওপেন ট্র্যাভার্স সমন্বয় ও নির্ভুলতা অনুপাত", "Traverse", HomeModuleCategory.COGO, Color(0xFF2E7D32), Icons.Rounded.Calculate, "cogo traverse bowditch transit least squares misclosure", "card_cogo_traverse", CogoEngineRoute("traverse")),
        HomeModuleSpec("mod_cogo_curve", 37, "COGO সার্কুলার কার্ভ (Curves)", "Radius, Tangent, Arc, Chord, Mid-Ordinate ও External", "Curves", HomeModuleCategory.COGO, Color(0xFF558B2F), Icons.Rounded.Architecture, "circular curve road radius tangent chord external", "card_cogo_curve", CogoEngineRoute("curve")),
        HomeModuleSpec("mod_cogo_subdivide", 38, "COGO হিঞ্জড ও স্লাইডিং বিভাজন", "Hinged Point, Parallel Sliding ও Area Target Split", "Hinge/Slide", HomeModuleCategory.COGO, Color(0xFFD81B60), Icons.Rounded.ContentCut, "hinged sliding parallel polygon subdivision target area", "card_cogo_subdivide", CogoEngineRoute("subdivide")),
        HomeModuleSpec("mod_field_cad", 39, "ফিল্ড-টু-CAD আমদানি (PNEZD)", "১১ ফরম্যাট পয়েন্ট ইম্পোর্ট, কোড-টু-লাইনওয়ার্ক ও Auto Layer", "Field-CAD", HomeModuleCategory.COGO, Color(0xFF00695C), Icons.Rounded.UploadFile, "field to cad pnezd csv txt dxf kml geojson gpx landxml nmea", "card_field_cad", FieldToCadRoute),
        HomeModuleSpec("mod_surface_tin", 40, "সার্ফেস TIN, কন্টুর ও কাটিং/ভরাট", "Spot Elevation, Delaunay TIN, কন্টুর ও Earthwork", "TIN / 3D", HomeModuleCategory.COGO, Color(0xFF4E342E), Icons.Rounded.Terrain, "tin contour cut fill earthwork elevation slope", "card_surface_tin", SurfaceTopoRoute),
        HomeModuleSpec("mod_road_access", 41, "রাস্তা অ্যালাইনমেন্ট ও প্রবেশপথ যাচাই", "স্টেশন/অফসেট, রোড ক্লিয়ারেন্স ও প্রবেশপথ চেক", "Road/Access", HomeModuleCategory.COGO, Color(0xFF37474F), Icons.Rounded.AddRoad, "road alignment station offset plot access corridor", "card_road_access", RoadAccessRoute),
        HomeModuleSpec("mod_qc_audit", 42, "সার্ভে কোয়ালিটি কন্ট্রোল (QC) অডিট", "অতিবিকৃত আকার, ক্ষুদ্র স্পাইক, বন্ধন ও এরিয়া অসামঞ্জস্য", "QC Audit", HomeModuleCategory.COGO, Color(0xFFC62828), Icons.Rounded.FactCheck, "quality control qc audit sliver spike misclosure tolerance", "card_qc_audit", SurveyQualityRoute),
        HomeModuleSpec("mod_gps_gis", 43, "GPS ফিল্ড সার্ভে, NMEA ও UTM", "WGS84, UTM (45N/46N), DMS, NMEA/RTK ও বিয়ারিং", "GPS/GIS", HomeModuleCategory.COGO, Color(0xFF1A73E8), Icons.Rounded.Language, "gps gis field survey nmea rtk utm wgs84 dms btm", "card_gps_gis", GpsGisToolsRoute),
        HomeModuleSpec("mod_field_instr", 44, "সার্ভে যন্ত্রপাতি ও ফিল্ড বুক", "Total Station, Theodolite, Level Book, EDM ও Cut-Fill", "Part B", HomeModuleCategory.COGO, Color(0xFF00695C), Icons.Rounded.Engineering, "total station theodolite auto level edm gnss cut fill xlsx", "card_field_instr", FieldInstrumentsRoute),

        // Separated Reports, Vault, Files, Search & About Modules
        HomeModuleSpec("mod_report_studio", 45, "পেশাদার সার্ভে রিপোর্ট স্টুডিও", "A4/A3 রিপোর্ট, উত্তর দিক, QR ও স্বাক্ষর ব্লক", "A4 / A3", HomeModuleCategory.REPORTS_FILES, Color(0xFF1565C0), Icons.Rounded.Summarize, "report memo demarcation a4 a3 qr", "card_report_studio", ReportStudioRoute),
        HomeModuleSpec("mod_land_db", 46, "ব্যক্তিগত ভূমি ডাটাবেজ ও ভল্ট", "মালিক/মৌজা ফোল্ডার, AES-256 ব্যাকআপ ও PIN লক", "Encrypted", HomeModuleCategory.REPORTS_FILES, EmeraldPrimary, Icons.Rounded.FolderShared, "database vault backup restore aes pin", "card_land_db", LandDatabaseRoute),
        HomeModuleSpec("mod_custom_builder", 47, "কাস্টম ফর্ম ও ফর্মুলা বিল্ডার", "নিজস্ব ফর্ম, ড্রপডাউন, ফর্মুলা ও JSON রপ্তানি", "Builder", HomeModuleCategory.REPORTS_FILES, Color(0xFF6D4C41), Icons.Rounded.Build, "custom template formula builder json", "card_custom_builder", CustomBuilderRoute),
        HomeModuleSpec("mod_spec_tools", 48, "বিশেষায়িত সার্ভে ও ইনভয়েস", "নদীভাঙন, ভবন কভারেজ/সেটব্যাক, যন্ত্রপাতি ও বিল PDF", "Part G", HomeModuleCategory.REPORTS_FILES, Color(0xFF00838F), Icons.Rounded.Build, "riverbank erosion building coverage calibration invoice", "card_spec_tools", SpecializedSurveyToolsRoute),
        HomeModuleSpec("mod_univ_archive", 49, "সর্বজনীন আর্কাইভ ও রপ্তানি", "১৪-ধরনের নথি, তুলনা ও XLSX/Shapefile/DXF/PDF", "Part J", HomeModuleCategory.REPORTS_FILES, Color(0xFF1B5E20), Icons.Rounded.Inventory2, "archive search compare xlsx shapefile dxf kml kmz gpx pdf", "card_univ_archive", UniversalArchiveExportRoute),
        HomeModuleSpec("mod_file_compat", 50, "ফাইল কম্প্যাটিবিলিটি ও Master ZIP", "DWG/DGN/GeoTIFF রূপান্তর গাইড ও Master Project ZIP", "ZIP + Matrix", HomeModuleCategory.REPORTS_FILES, Color(0xFF0D47A1), Icons.Rounded.Inventory2, "file compatibility dwg dgn geotiff master project zip export", "card_file_compat", FileCompatibilityRoute),
        HomeModuleSpec("mod_ai_search", 51, "গুগল ভূমি সার্চ", "বড় পেজে সরাসরি Google-এ যেকোনো বিষয় খুঁজুন", "Google", HomeModuleCategory.REPORTS_FILES, Color(0xFF1A73E8), Icons.Rounded.Search, "google search query help", "card_ai_assistant", AiAssistantRoute),
        HomeModuleSpec("mod_about", 52, "পরিচিতি, PC লিংক ও সংরক্ষণাগার", "ডেভেলপার প্রোফাইল, সাপোর্ট, PC সার্ভার ও Archiver", "About + PC", HomeModuleCategory.REPORTS_FILES, Color(0xFF00897B), Icons.Rounded.Person, "about profile contact pc server link", "card_about", AboutRoute)
    )
}
