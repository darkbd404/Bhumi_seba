package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpansionRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.GisCoordinateEngine
import com.example.engine.UniversalExportEngine
import com.example.model.GeoWaypoint
import com.example.model.UniversalArchiveDocument
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun UniversalArchiveExportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // J01 & J02 Archive Form + Multi-facet search
    var searchQuery by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("খতিয়ান") }
    var docTitle by remember { mutableStateOf("") }
    var docOwner by remember { mutableStateOf("") }
    var docMouza by remember { mutableStateOf("") }
    var docDag by remember { mutableStateOf("") }
    var docKhatian by remember { mutableStateOf("") }
    var docDeedNo by remember { mutableStateOf("") }
    var docSummary by remember { mutableStateOf("") }

    // J03 Comparison
    var leftId by remember { mutableStateOf(snap.archiveDocs.firstOrNull()?.id ?: "") }
    var rightId by remember { mutableStateOf(snap.archiveDocs.getOrNull(1)?.id ?: snap.archiveDocs.firstOrNull()?.id ?: "") }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "সর্বজনীন আর্কাইভ ও রপ্তানি হাব (J01–J04)",
                subtitleEn = "14-Type Document Archive, Multi-Facet Search, Compare & PDF/DOCX/XLSX/SHP/GIS Export",
                helpTitle = "আর্কাইভ ও রপ্তানি হাব নির্দেশিকা",
                helpSteps = listOf(
                    "১৪ ধরনের ভূমি ডকুমেন্ট (দলিল, খতিয়ান, পর্চা, দাখিলা, DCR, ম্যাপ) এক জায়গায় সংরক্ষণ ও খুঁজুন।",
                    "যেকোনো ২টি নথি পাশাপাশি তুলনা করে অমিল শনাক্ত করুন।",
                    "এক ক্লিকে PDF, DOCX, Excel XLSX, CSV, JSON, GeoJSON, KML, KMZ, DXF, GPX ও ESRI Shapefile ZIP রপ্তানি করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "universal_archive_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("J01-02 নথি আর্কাইভ ও সার্চ", tab == 0, testTag = "arch_tab_0") { tab = 0 } }
                item { NeuPillChip("J03 পাশাপাশি তুলনা ও হিস্ট্রি", tab == 1, testTag = "arch_tab_1") { tab = 1 } }
                item { NeuPillChip("J04 সার্বজনীন এক্সপোর্ট (XLSX/SHP/PDF)", tab == 2, testTag = "arch_tab_2") { tab = 2 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("J01 & J02. ১৪-ধরনের ডকুমেন্ট আর্কাইভ ও বহু-মুখী অনুসন্ধান", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("খুঁজুন: মালিক, NID, মৌজা, দাগ, খতিয়ান, দলিল, তারিখ...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("দলিল", "খতিয়ান", "পর্চা", "দাখিলা", "নামজারি আদেশ", "DCR", "মৌজা ম্যাপ", "জরিপ রিপোর্ট", "কোর্ট অর্ডার", "ওয়ারিশ সনদ")) { t ->
                                        NeuPillChip(t, docType == t, onClick = { docType = t })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(docTitle, { docTitle = it }, label = { Text("শিরোনাম") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(docOwner, { docOwner = it }, label = { Text("মালিকের নাম") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(docMouza, { docMouza = it }, label = { Text("মৌজা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(docKhatian, { docKhatian = it }, label = { Text("খতিয়ান") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(docDag, { docDag = it }, label = { Text("দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(docDeedNo, { docDeedNo = it }, label = { Text("দলিল/আবেদন নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(docSummary, { docSummary = it }, label = { Text("সারসংক্ষেপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        if (docTitle.isNotBlank() || docOwner.isNotBlank()) {
                                            repo.saveArchiveDoc(
                                                UniversalArchiveDocument(
                                                    id = "arch_${System.currentTimeMillis()}",
                                                    docType = docType,
                                                    title = docTitle.ifBlank { "$docType - $docOwner" },
                                                    ownerName = docOwner,
                                                    mouza = docMouza,
                                                    dagNo = docDag,
                                                    khatianNo = docKhatian,
                                                    deedOrAppNo = docDeedNo,
                                                    summaryContent = docSummary
                                                )
                                            )
                                            docTitle = ""; docSummary = ""
                                            toast = "আর্কাইভে সংরক্ষিত হয়েছে"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("save_archive_doc_btn")
                                ) { Text("ডকুমেন্ট আর্কাইভ করুন") }
                            }
                        }

                        val filtered = snap.archiveDocs.filter { d ->
                            searchQuery.isBlank() ||
                                d.title.contains(searchQuery, true) ||
                                d.ownerName.contains(searchQuery, true) ||
                                d.mouza.contains(searchQuery, true) ||
                                d.dagNo.contains(searchQuery, true) ||
                                d.khatianNo.contains(searchQuery, true) ||
                                d.deedOrAppNo.contains(searchQuery, true) ||
                                d.docType.contains(searchQuery, true)
                        }
                        items(filtered, key = { it.id }) { doc ->
                            Surface(color = Color.White.copy(alpha = 0.78f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("[${doc.docType}] ${doc.title}", fontWeight = FontWeight.Bold)
                                    Text("মালিক: ${doc.ownerName.ifBlank { "-" }} | মৌজা: ${doc.mouza} | খতিয়ান: ${doc.khatianNo} | দাগ: ${doc.dagNo}", style = MaterialTheme.typography.bodySmall)
                                    Text("স্ট্যাটাস: ${doc.verificationStatus} | নং: ${doc.deedOrAppNo.ifBlank { "-" }}", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            val left = snap.archiveDocs.find { it.id == leftId }
                            val right = snap.archiveDocs.find { it.id == rightId }
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("J03. ২টি নথি পাশাপাশি তুলনা (Side-by-Side Diff)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("প্রথম নথি:", style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(snap.archiveDocs) { d -> NeuPillChip(d.title.take(18), leftId == d.id, onClick = { leftId = d.id }) }
                                }
                                Text("দ্বিতীয় নথি:", style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(snap.archiveDocs) { d -> NeuPillChip(d.title.take(18), rightId == d.id, onClick = { rightId = d.id }) }
                                }
                                if (left != null && right != null) {
                                    Surface(color = EmeraldPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        val diffReport = buildString {
                                            append("• মালিক তুলনা: ${left.ownerName} vs ${right.ownerName} [${if (left.ownerName == right.ownerName) "মিল ✓" else "অমিল ⚠"}]\n")
                                            append("• মৌজা তুলনা: ${left.mouza} vs ${right.mouza} [${if (left.mouza == right.mouza) "মিল ✓" else "অমিল ⚠"}]\n")
                                            append("• খতিয়ান তুলনা: ${left.khatianNo} vs ${right.khatianNo} [${if (left.khatianNo == right.khatianNo) "মিল ✓" else "অমিল ⚠"}]\n")
                                            append("• দাগ তুলনা: ${left.dagNo} vs ${right.dagNo} [${if (left.dagNo == right.dagNo) "মিল ✓" else "অমিল ⚠"}]")
                                        }
                                        Text(diffReport, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text("টুল হিসাবের ইতিহাস (${snap.calcHistory.size} এন্ট্রি)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                snap.calcHistory.take(10).forEach { h ->
                                    Text("• [${h.moduleTag}] ${h.summaryTitle}: ${h.resultSummary}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            val headers = listOf("DocType", "Title", "Owner", "Mouza", "Khatian", "Dag", "Number", "Status")
                            val rows = snap.archiveDocs.map {
                                listOf(it.docType, it.title, it.ownerName, it.mouza, it.khatianNo, it.dagNo, it.deedOrAppNo, it.verificationStatus)
                            }
                            val samplePts = listOf(
                                GeoWaypoint("1", "P1", 23.8100, 90.4120),
                                GeoWaypoint("2", "P2", 23.8100, 90.4130),
                                GeoWaypoint("3", "P3", 23.8110, 90.4130),
                                GeoWaypoint("4", "P4", 23.8110, 90.4120)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("J04. সর্বজনীন এক্সপোর্ট ইঞ্জিন (সকল ফরম্যাট)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val bytes = UniversalExportEngine.buildXlsxBytes("ArchiveDocs", headers, rows)
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, bytes, "bhumi_archive_${System.currentTimeMillis()}.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                            toast = "Excel XLSX সেভ: $p"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_all_xlsx_btn")
                                    ) { Text("Excel (.xlsx)") }

                                    OutlinedButton(
                                        onClick = {
                                            val csv = UniversalExportEngine.buildCsvText(headers, rows)
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, csv.toByteArray(), "bhumi_archive_${System.currentTimeMillis()}.csv", "text/csv")
                                            toast = "CSV সেভ: $p"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("CSV") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val shpZip = UniversalExportEngine.buildShapefileZipBytes("bhumi_parcel", samplePts)
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, shpZip, "bhumi_shapefile_${System.currentTimeMillis()}.zip", "application/zip")
                                            toast = "ESRI Shapefile ZIP (.shp/.shx/.dbf/.prj) সেভ: $p"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_all_shp_btn")
                                    ) { Text("Shapefile ZIP") }

                                    OutlinedButton(
                                        onClick = {
                                            val jsonBytes = Json { prettyPrint = true }.encodeToString(snap).toByteArray()
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, jsonBytes, "bhumi_expansion_backup_${System.currentTimeMillis()}.json", "application/json")
                                            toast = "JSON ব্যাকআপ সেভ: $p"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("JSON ব্যাকআপ") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val body = snap.archiveDocs.joinToString("\n") { "${it.docType}: ${it.title} (${it.mouza}, দাগ ${it.dagNo})" }
                                            val docx = DocumentAndReportEngine.buildDocxBytes("সর্বজনীন ভূমি আর্কাইভ", body)
                                            val res = DocumentAndReportEngine.saveBinaryDocument(context, "bhumi_archive_${System.currentTimeMillis()}.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx)
                                            toast = "DOCX সেভ: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("DOCX") }

                                    OutlinedButton(
                                        onClick = {
                                            val geo = GisCoordinateEngine.exportGeoJson("BhumiPlot", samplePts)
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, geo.toByteArray(), "bhumi_plot_${System.currentTimeMillis()}.geojson", "application/geo+json")
                                            toast = "GeoJSON সেভ: $p"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("GeoJSON") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val kmz = GisCoordinateEngine.exportKmzBytes("BhumiPlot", samplePts)
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, kmz, "bhumi_plot_${System.currentTimeMillis()}.kmz", "application/vnd.google-earth.kmz")
                                            toast = "KMZ সেভ: $p"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("KML / KMZ") }

                                    OutlinedButton(
                                        onClick = {
                                            val gpx = GisCoordinateEngine.exportGpx("BhumiPlot", samplePts)
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, gpx.toByteArray(), "bhumi_plot_${System.currentTimeMillis()}.gpx", "application/gpx+xml")
                                            toast = "GPX সেভ: $p"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("GPX") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val dxf = GisCoordinateEngine.exportPlanarDxf("BhumiPlot", listOf(0.0 to 0.0, 100.0 to 0.0, 100.0 to 80.0, 0.0 to 80.0))
                                            val p = ExportStreamWriter.saveBinaryFileToDownloads(context, dxf.toByteArray(), "bhumi_plot_${System.currentTimeMillis()}.dxf", "application/dxf")
                                            toast = "AutoCAD DXF সেভ: $p"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("AutoCAD DXF") }

                                    Button(
                                        onClick = {
                                            val body = snap.archiveDocs.joinToString("\n") { "• [${it.docType}] ${it.title} — ${it.mouza}, খতিয়ান ${it.khatianNo}, দাগ ${it.dagNo}" }
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap("সর্বজনীন ভূমি আর্কাইভ সারসংক্ষেপ", "Universal Archive PDF", body)
                                            val copy = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "universal_archive_${System.currentTimeMillis()}")
                                            UniversalExportEngine.printBitmapDocument(context, "UniversalArchive", copy)
                                            toast = "PDF ও প্রিন্ট ডায়ালগ প্রস্তুত: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_all_pdf_print_btn")
                                    ) { Text("PDF ও প্রিন্ট") }
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomSwipeToastBanner(toast, { toast = null }, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}
