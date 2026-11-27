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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Engineering
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
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WorkstationRepository
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuFeatureCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface

private data class WorkstationModuleItem(
    val number: Int,
    val titleBn: String,
    val subtitle: String,
    val badge: String,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    var search by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    val modules = remember {
        listOf(
            WorkstationModuleItem(1, "মৌজা ম্যাপ স্টুডিও", "CS/SA/RS/BS/BRS PDF ও ম্যাপ পরিমাপ", "1000x HD", EmeraldPrimary, Icons.Rounded.PictureAsPdf, "mouza pdf map scale dag", "hub_mod_1", onOpenMouzaPdf),
            WorkstationModuleItem(2, "PDF ও CAD স্টুডিও", "PDF/JPG রূপান্তর, মার্চ/স্প্লিট, স্ন্যাপ CAD ও DXF", "CAD + DXF", Color(0xFF00796B), Icons.Rounded.AutoFixHigh, "pdf cad dxf image clean deskew", "hub_mod_2", onOpenPdfCad),
            WorkstationModuleItem(3, "প্যান্টোগ্রাফ ও নকশা তুলনা", "CS বনাম RS/BS ওভারলে, কন্ট্রোল পয়েন্ট ও TPS/Affine", "Pantograph", Color(0xFF5E35B1), Icons.Rounded.Compare, "pantograph cs sa rs bs affine tps overlay", "hub_mod_3", onOpenPantograph),
            WorkstationModuleItem(4, "স্যাটেলাইট ল্যান্ড সার্ভে", "HD Google/Esri/OSM ম্যাপ, পলিগন ও ওভারলে", "Satellite", TealSecondary, Icons.Rounded.Public, "satellite map overlay google esri", "hub_mod_4", onOpenRealMap),
            WorkstationModuleItem(5, "GPS ফিল্ড সার্ভে ও RTK", "ওয়েপয়েন্ট, NMEA/RTK, বিয়ারিং, আজিমুথ ও ট্র্যাক", "GPS / NMEA", Color(0xFF1A73E8), Icons.Rounded.GpsFixed, "gps field survey nmea rtk bearing", "hub_mod_5", onOpenGpsGis),
            WorkstationModuleItem(6, "জরিপ ও আঞ্চলিক একক ক্যালকুলেটর", "শতাংশ, কানি-গণ্ডা (চট্টগ্রাম/নোয়াখালী), হেরন, ট্র্যাভার্স", "Regional", SurveyGold, Icons.Rounded.Calculate, "calculator kani gonda katha traverse shoelace", "hub_mod_6") { onOpenTools("unit") },
            WorkstationModuleItem(7, "স্বয়ংক্রিয় জমি বিভাজন", "সমান/শতাংশ/রাস্তা রেখে পলিগন বিভাজন ও চিত্র সেভ", "Auto Split", Color(0xFFD81B60), Icons.Rounded.ContentCut, "divider split road percentage polygon", "hub_mod_7", onOpenPolygonDivider),
            WorkstationModuleItem(8, "খতিয়ান ও দাগ ম্যানেজার", "CS/SA/RS/BS/BRS খতিয়ান, হিস্যা ও PNG/PDF শিট", "Khatian", Color(0xFF0F6B54), Icons.Rounded.Description, "khatian cs sa rs bs brs dag hissa", "hub_mod_8", onOpenCustomKhatian),
            WorkstationModuleItem(9, "উত্তরাধিকার ও ফারায়েজ", "মুসলিম ফারায়েজ, হিন্দু দায়ভাগ, বৌদ্ধ ও খ্রিস্টান আইন", "4 Laws", Color(0xFF00897B), Icons.Rounded.Balance, "inheritance faraid hindu muslim christian buddhist", "hub_mod_9") { onOpenTools("faraiz") },
            WorkstationModuleItem(10, "দলিল ও আইনি খসড়া স্টুডিও", "সাফ-কবলা, বণ্টননামা, হেবা, বায়না — PDF ও DOCX", "PDF+DOCX", Color(0xFF8E24AA), Icons.Rounded.Gavel, "deed safkabala heba bayna docx pdf", "hub_mod_10", onOpenDeedStudio),
            WorkstationModuleItem(11, "ডকুমেন্ট স্ক্যানার ও OCR", "বাংলা/ইংরেজি খতিয়ান ও দাগ এক্সট্র্যাক্ট ও যাচাই", "Bangla OCR", Color(0xFFE65100), Icons.Rounded.DocumentScanner, "scanner ocr bangla extract table", "hub_mod_11", onOpenScannerOcr),
            WorkstationModuleItem(12, "পেশাদার সার্ভে রিপোর্ট স্টুডিও", "A4/A3 রিপোর্ট, উত্তর দিক, QR ও স্বাক্ষর ব্লক", "A4 / A3", Color(0xFF1565C0), Icons.Rounded.Summarize, "report memo demarcation a4 a3 qr", "hub_mod_12", onOpenReportStudio),
            WorkstationModuleItem(13, "ব্যক্তিগত ভূমি ডাটাবেজ ও ভল্ট", "মালিক/মৌজা ফোল্ডার, AES-256 ব্যাকআপ ও PIN লক", "Encrypted", EmeraldPrimary, Icons.Rounded.FolderShared, "database vault backup restore aes pin", "hub_mod_13", onOpenLandDatabase),
            WorkstationModuleItem(14, "আইনি ও সরকারি তথ্য লাইব্রেরি", "জরিপ বিধিমালা, পর্চা গাইড ও সিস্টেম আর্কিটেকচার", "Offline Guide", Color(0xFF3949AB), Icons.Rounded.MenuBook, "library law rules cs rs bs integration", "hub_mod_14", onOpenLegalLibrary),
            WorkstationModuleItem(15, "আর্থিক ও দলিল খরচ ক্যালকুলেটর", "মৌজা রেট, স্ট্যাম্প, খাজনা, নামজারি ও লাভ/ক্ষতি", "Tax & Fee", SurveyGold, Icons.Rounded.AccountBalance, "financial market stamp tax mutation profit", "hub_mod_15") { onOpenTools("tax") },
            WorkstationModuleItem(16, "GIS ও স্থানাঙ্ক রূপান্তর টুলস", "WGS84, UTM (45N/46N), DMS, KML/KMZ/GPX/GeoJSON/DXF", "WGS84/UTM", TealSecondary, Icons.Rounded.Language, "gis utm wgs84 dms kml kmz gpx geojson", "hub_mod_16", onOpenGpsGis),
            WorkstationModuleItem(17, "কাস্টম ফর্ম ও ফর্মুলা বিল্ডার", "নিজস্ব ফর্ম, ড্রপডাউন, ফর্মুলা ও JSON আমদানি/রপ্তানি", "Builder", Color(0xFF6D4C41), Icons.Rounded.Build, "custom template formula builder json", "hub_mod_17", onOpenCustomBuilder),
            WorkstationModuleItem(18, "প্রজেক্ট ও সরকারি পোর্টাল হাব", "সমন্বিত প্রজেক্ট ম্যানেজমেন্ট ও সরকারি পোর্টাল", "Projects", Color(0xFF2E7D32), Icons.Rounded.FolderShared, "project management dlrms portal", "hub_mod_18", onOpenServices),
            WorkstationModuleItem(19, "পুরনো মৌজা ম্যাপ ও দাগ ল্যাব (A01–09)", "ম্যাপ পরিষ্কার, স্কেল বিকৃতি, সাবেক-হাল দাগ ও A0–A4 প্রিন্ট", "Part A", Color(0xFF4E342E), Icons.Rounded.Layers, "old mouza clean distortion genealogy adjacency a0 a3 print", "hub_mod_19", onOpenOldMouzaLab),
            WorkstationModuleItem(20, "সার্ভে যন্ত্রপাতি ও ফিল্ড বুক (B01–08)", "Total Station, Theodolite, Level Book, EDM, Cut-Fill", "Part B", Color(0xFF00695C), Icons.Rounded.Engineering, "total station theodolite auto level edm gnss cut fill xlsx", "hub_mod_20", onOpenFieldInstruments),
            WorkstationModuleItem(21, "অনলাইন ভূমি সেবা কর্মক্ষেত্র (C01–08)", "নামজারি ৮ উৎস, খাজনা বকেয়া, খতিয়ান ও ৮ পোর্টাল হ্যান্ডঅফ", "Part C+I", Color(0xFF1565C0), Icons.Rounded.Public, "namjari mutation khajna dakhila dcr portal bd", "hub_mod_21", onOpenOnlineServiceWorkbench),
            WorkstationModuleItem(22, "রাজস্ব মামলা ও ডিউ ডিলিজেন্স (D01–05)", "মিস কেস, অধিগ্রহণ ক্ষতিপূরণ, খাস/লিজ ও ঝুঁকি রিপোর্ট", "Part D", Color(0xFFAD1457), Icons.Rounded.Rule, "revenue case acquisition khas lease dispute due diligence", "hub_mod_22", onOpenRevenueDueDiligence),
            WorkstationModuleItem(23, "দলিল ও তফসিল ল্যাব প্রো (E01–05)", "বহু-দাতা/গ্রহীতা, তফসিল জেনারেটর, ভুল যাচাই ও আইনি ধারা", "Part E", Color(0xFF6A1B9A), Icons.Rounded.HistoryEdu, "deed lab schedule tafsil mismatch legal clause docx pdf", "hub_mod_23", onOpenDeedLabPro),
            WorkstationModuleItem(24, "ক্ষেত্রফল তদন্ত ও সমন্বয় ল্যাব (F01–09)", "৬-উৎস তুলনা, রাস্তা/পুকুর বাদ নিট জমি ও ফ্রন্টেজ বিভাজন", "Part F", Color(0xFFEF6C00), Icons.Rounded.SquareFoot, "area reconciliation usable land road pond frontage valuation", "hub_mod_24", onOpenAreaInvestigation),
            WorkstationModuleItem(25, "বিশেষায়িত সার্ভে ও ইনভয়েস (G01–07)", "নদীভাঙন, ভবন কভারেজ/সেটব্যাক, যন্ত্রপাতি ও বিল PDF", "Part G", Color(0xFF00838F), Icons.Rounded.Build, "riverbank erosion building coverage calibration invoice certificate", "hub_mod_25", onOpenSpecializedTools),
            WorkstationModuleItem(26, "কাস্টম ভূমি সেবা অটোমেশন (Part H)", "নিজস্ব সেবা, পুনঃসাজানো ধাপ, শর্ত ও ফি ফর্মুলা ইঞ্জিন", "Part H", Color(0xFF283593), Icons.Rounded.SettingsSuggest, "custom service automation workflow steps formula json", "hub_mod_26", onOpenCustomServiceEngine),
            WorkstationModuleItem(27, "সর্বজনীন আর্কাইভ ও রপ্তানি (J01–04)", "১৪-ধরনের নথি, তুলনা ও XLSX / Shapefile / DXF / PDF রপ্তানি", "Part J", Color(0xFF1B5E20), Icons.Rounded.Inventory2, "archive search compare xlsx shapefile dxf kml kmz gpx pdf", "hub_mod_27", onOpenUniversalArchive)
        )
    }

    val filtered = remember(search) {
        if (search.isBlank()) modules
        else modules.filter {
            it.titleBn.contains(search, true) ||
                it.subtitle.contains(search, true) ||
                it.keywords.contains(search, true) ||
                it.number.toString() == search.trim()
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
                titleBn = "সার্ভেয়ার ওয়ার্কস্টেশন (২৭ মডিউল)",
                subtitleEn = "Complete Offline-First Bangladesh Digital Survey Suite (Parts A–K)",
                helpTitle = "২৭টি মডিউল ব্যবহার নির্দেশিকা",
                helpSteps = listOf(
                    "উপরের সার্চ বক্সে বাংলা বা ইংরেজিতে যেকোনো টুলের নাম লিখুন।",
                    "নতুন কাজ শুরু করতে 'নমুনা প্রকল্প লোড করুন' চাপলে পুবাইল মৌজার পূর্ণাঙ্গ নমুনা ডাটা যুক্ত হবে।",
                    "সকল হিসাব, দলিল খসড়া ও মোবাইল GPS ফলাফল ব্যক্তিগত ও প্রাথমিক রেফারেন্সের জন্য।"
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
                        placeholder = { Text("টুলস খুঁজুন (যেমন: প্যান্টোগ্রাফ, দলিল, OCR, বিভাজন, UTM)...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_search_input")
                    )
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
                                    text = "নমুনা প্রকল্প ও পূর্ণাঙ্গ ডাটা লোড",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "পুবাইল মৌজার নমুনা প্রকল্প, GPS ট্র্যাক, দলিল খসড়া ও রিপোর্ট যুক্ত করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repo.seedSampleBangladeshProject()
                                    toast = "নমুনা প্রজেক্ট, GPS ট্র্যাক, দলিল ও রিপোর্ট যুক্ত হয়েছে!"
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
