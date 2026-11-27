package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterCadRepository
import com.example.engine.CogoAndSurfaceEngine
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.FieldIoQualityAndDeedEngine
import com.example.engine.LandCalculators
import com.example.engine.SurveyMathProEngine
import com.example.engine.UniversalExportEngine
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface

@Composable
fun ScheduleQcAndCompatibilityScreen(
    initialMode: String = "schedule",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { MasterCadRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember(initialMode) {
        mutableIntStateOf(
            when (initialMode) {
                "qc" -> 1
                "files" -> 2
                else -> 0
            }
        )
    }
    var toast by remember { mutableStateOf<String?>(null) }

    val activePoly = remember(snap.entities) {
        snap.entities.firstOrNull { it.closed && it.points.size >= 3 }?.points ?: listOf(
            PolygonVertex(0.0, 0.0), PolygonVertex(120.0, 0.0), PolygonVertex(120.0, 90.0), PolygonVertex(0.0, 90.0)
        )
    }
    val measuredDecimal = remember(activePoly) {
        (SurveyMathProEngine.computeCoordinateGeometry(activePoly)?.sqFeet ?: 0.0) / LandCalculators.SQ_FT_PER_DECIMAL
    }

    // Tab 0: Automated Schedule (Tafsil)
    var mouza by remember { mutableStateOf("পুবাইল") }
    var jlNo by remember { mutableStateOf("৮৫") }
    var khatianNo by remember { mutableStateOf("৪১২") }
    var dagNo by remember { mutableStateOf("১০৪২") }
    var landClass by remember { mutableStateOf("নাল / ভিটা") }
    var nNeighbor by remember { mutableStateOf("দাগ ১০৪১ (রহিম উদ্দিন)") }
    var sNeighbor by remember { mutableStateOf("১২ ফুট রাস্তা") }
    var eNeighbor by remember { mutableStateOf("দাগ ১০৪৩ (আবুল হোসেন)") }
    var wNeighbor by remember { mutableStateOf("পুকুর পাড়") }
    var languageBn by remember { mutableStateOf(true) }

    val scheduleText = remember(mouza, jlNo, khatianNo, dagNo, landClass, activePoly, nNeighbor, sNeighbor, eNeighbor, wNeighbor, languageBn) {
        CogoAndSurfaceEngine.generateAutomatedLandSchedule(
            mouza, jlNo, khatianNo, dagNo, landClass, activePoly,
            nNeighbor, sNeighbor, eNeighbor, wNeighbor, languageBn
        )
    }

    // Tab 1: Survey Quality Control Audit
    var deedDecInput by remember { mutableStateOf("25.00") }
    var khatianDecInput by remember { mutableStateOf("24.80") }
    var travMisInput by remember { mutableStateOf("0.04") }
    var travPerimInput by remember { mutableStateOf("420.0") }
    var georefRmseInput by remember { mutableStateOf("1.8") }
    var gpsAccInput by remember { mutableStateOf("3.2") }

    val qcReport = remember(snap.entities, measuredDecimal, deedDecInput, khatianDecInput, travMisInput, travPerimInput, georefRmseInput, gpsAccInput) {
        FieldIoQualityAndDeedEngine.runSurveyQualityAudit(
            entities = snap.entities,
            measuredAreaDecimal = measuredDecimal,
            deedAreaDecimal = deedDecInput.toDoubleOrNull() ?: measuredDecimal,
            khatianAreaDecimal = khatianDecInput.toDoubleOrNull() ?: measuredDecimal,
            traverseMisclosureFt = travMisInput.toDoubleOrNull() ?: 0.0,
            traversePerimeterFt = travPerimInput.toDoubleOrNull() ?: 420.0,
            georefRmsePx = georefRmseInput.toDoubleOrNull() ?: 1.8,
            gpsAccuracyM = gpsAccInput.toDoubleOrNull() ?: 3.2
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1080.dp)
        ) {
            WorkstationTopBar(
                titleBn = "স্বয়ংক্রিয় তফসিল, মান নিয়ন্ত্রণ ও ফাইল হাব",
                subtitleEn = "Bangla/English Tafsil on Vintage Paper • Survey QC Audit • Master ZIP & Format Guide",
                helpTitle = "তফসিল, QC ও ফাইল হাব গাইড",
                helpSteps = listOf(
                    "তফসিল ট্যাবে সক্রিয় CAD প্লট পলিগন থেকে স্বয়ংক্রিয়ভাবে বাংলা ও ইংরেজি মেটস-অ্যান্ড-বাউন্ডস তফসিল তৈরি করে প্রাচীন কাগজে প্রিভিউ ও রপ্তানি করুন।",
                    "মান যাচাই (QC) ট্যাবে সীমানা ক্লোজার, ট্র্যাভার্স নির্ভুলতা, দলিল-খতিয়ান পার্থক্য ও GPS অনিশ্চয়তা অডিট দেখুন।",
                    "ফাইল হাব থেকে পুরো প্রজেক্টের DXF, SVG, CSV, XLSX, DOCX, GeoJSON, KML ও Shapefile এক ক্লিকে Master ZIP হিসেবে সেভ করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "sched_qc_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(0 to "তফসিল জেনারেটর", 1 to "সার্ভে মান যাচাই (QC)", 2 to "ফাইল ও Master ZIP")) { (idx, lbl) ->
                    NeuPillChip(text = lbl, selected = tab == idx, testTag = "sqf_tab_$idx", onClick = { tab = idx })
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    // ---------- Tab 0: Automated Tafsil on Vintage Stamp Paper ----------
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("স্বয়ংক্রিয় তফসিল ও চৌহদ্দি (CAD প্লট থেকে)", fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (languageBn) "বাংলা" else "English", style = MaterialTheme.typography.labelMedium)
                                        Switch(checked = languageBn, onCheckedChange = { languageBn = it })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = jlNo, onValueChange = { jlNo = it }, label = { Text("জে.এল.") }, singleLine = true, modifier = Modifier.weight(0.6f))
                                    OutlinedTextField(value = khatianNo, onValueChange = { khatianNo = it }, label = { Text("খতিয়ান") }, singleLine = true, modifier = Modifier.weight(0.8f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = dagNo, onValueChange = { dagNo = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = landClass, onValueChange = { landClass = it }, label = { Text("শ্রেণি") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = nNeighbor, onValueChange = { nNeighbor = it }, label = { Text("উত্তরে") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = sNeighbor, onValueChange = { sNeighbor = it }, label = { Text("দক্ষিণে") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = eNeighbor, onValueChange = { eNeighbor = it }, label = { Text("পূর্বে") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = wNeighbor, onValueChange = { wNeighbor = it }, label = { Text("পশ্চিমে") }, singleLine = true, modifier = Modifier.weight(1f))
                                }

                                VintageStampPaperCard(
                                    headerTitle = if (languageBn) "জমির তফসিল ও সীমানা বিবরণী (খসড়া)" else "AUTOMATED LEGAL LAND SCHEDULE",
                                    stampBadge = "প্লট তফসিল (প্রাচীন কাগজ শৈলী)",
                                    serialNumber = "দাগ-$dagNo"
                                ) {
                                    Text(scheduleText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Serif, color = Color(0xFF2B1C10))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                                title = "তফসিল ও চৌহদ্দি — মৌজা $mouza",
                                                stampValueLabel = "তফসিল খসড়া (১০০ টাকা শৈলী)",
                                                serialTag = "DAG-$dagNo",
                                                bodyText = scheduleText
                                            )
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Land_Schedule_${dagNo}")
                                            toast = "ভিন্টেজ PDF সেভ: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("sched_pdf_btn")
                                    ) { Text("PDF") }
                                    OutlinedButton(
                                        onClick = {
                                            val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                                title = "তফসিল ও চৌহদ্দি — মৌজা $mouza",
                                                stampValueLabel = "তফসিল খসড়া (১০০ টাকা শৈলী)",
                                                serialTag = "DAG-$dagNo",
                                                bodyText = scheduleText
                                            )
                                            val res = ExportStreamWriter.savePngBitmap(context, bmp, "Land_Schedule_${dagNo}")
                                            toast = "ভিন্টেজ PNG সেভ: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("PNG") }
                                    Button(
                                        onClick = {
                                            val docx = DocumentAndReportEngine.buildDocxBytes("Land Schedule $mouza Dag $dagNo", scheduleText)
                                            val res = DocumentAndReportEngine.saveBinaryDocument(context, "Land_Schedule_${dagNo}.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx)
                                            toast = "DOCX সেভ: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("DOCX") }
                                    OutlinedButton(
                                        onClick = {
                                            val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                                title = "তফসিল ও চৌহদ্দি — মৌজা $mouza",
                                                stampValueLabel = "তফসিল খসড়া",
                                                serialTag = "DAG-$dagNo",
                                                bodyText = scheduleText
                                            )
                                            val pCopy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                            bmp.recycle()
                                            UniversalExportEngine.printBitmapDocument(context, "Land_Schedule_$dagNo", pCopy)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Print") }
                                }
                            }
                        }
                    }

                    // ---------- Tab 1: Survey Quality Control Audit ----------
                    1 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("সার্ভে কোয়ালিটি কন্ট্রোল ও ত্রুটি অডিট (Module 16)", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = deedDecInput, onValueChange = { deedDecInput = it }, label = { Text("দলিলের জমি (শতাংশ)") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = khatianDecInput, onValueChange = { khatianDecInput = it }, label = { Text("খতিয়ান (শতাংশ)") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = travMisInput, onValueChange = { travMisInput = it }, label = { Text("মিসক্লোজার (ft)") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = georefRmseInput, onValueChange = { georefRmseInput = it }, label = { Text("Georef RMSE (px)") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = gpsAccInput, onValueChange = { gpsAccInput = it }, label = { Text("GPS ±m") }, singleLine = true, modifier = Modifier.weight(1f))
                                }

                                Surface(
                                    color = EmeraldPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("সামগ্রিক মান স্কোর: ${qcReport.overallScorePct}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        Text(qcReport.discrepancySummaryBn, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                qcReport.checks.forEach { chk ->
                                    val badgeCol = when (chk.status) {
                                        "PASS" -> EmeraldPrimary
                                        "WARN" -> Color(0xFFD97706)
                                        else -> Color(0xFFC62828)
                                    }
                                    Surface(color = badgeCol.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(chk.categoryBn, fontWeight = FontWeight.Bold)
                                                Text(chk.status, fontWeight = FontWeight.Bold, color = badgeCol)
                                            }
                                            Text("পরিমাপ: ${chk.measuredValue}", style = MaterialTheme.typography.bodySmall)
                                            Text("মানদণ্ড: ${chk.thresholdOrRef}", style = MaterialTheme.typography.labelSmall)
                                            Text("পরামর্শ: ${chk.recommendationBn}", style = MaterialTheme.typography.labelSmall, color = badgeCol)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val body = buildString {
                                            appendLine(qcReport.discrepancySummaryBn)
                                            appendLine()
                                            qcReport.checks.forEach { c ->
                                                appendLine("[${c.status}] ${c.categoryBn}")
                                                appendLine("  Measured: ${c.measuredValue} (Limit: ${c.thresholdOrRef})")
                                                appendLine("  Note: ${c.recommendationBn}")
                                            }
                                        }
                                        val bmp = UniversalExportEngine.renderDocumentReportBitmap("Survey Quality Control Audit", "Score: ${qcReport.overallScorePct}%", body)
                                        val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Survey_QC_Audit_${System.currentTimeMillis()}")
                                        toast = "QC অডিট PDF সেভ: ${res.displayPath}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("QC অডিট রিপোর্ট PDF ডাউনলোড") }
                            }
                        }
                    }

                    // ---------- Tab 2: Universal File Compatibility Matrix & Master Project ZIP ----------
                    2 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("সর্বজনীন ফাইল সামঞ্জস্য ও Master Project ZIP (Module 17)", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        val histText = snap.historicalDeeds.firstOrNull()?.let {
                                            FieldIoQualityAndDeedEngine.buildFaithfulHistoricalReproduction(it)
                                        } ?: scheduleText
                                        val zipBytes = FieldIoQualityAndDeedEngine.buildMasterProjectBundleZip(
                                            projectTitle = snap.projectTitle,
                                            entities = snap.entities,
                                            dimensions = snap.dimensions,
                                            layers = snap.layers,
                                            cogoPoints = snap.cogoPoints,
                                            deedDraftText = histText,
                                            scheduleText = scheduleText
                                        )
                                        val res = DocumentAndReportEngine.saveBinaryDocument(
                                            context = context,
                                            fileNameWithExt = "Bhumi_Master_Project_${System.currentTimeMillis()}.zip",
                                            mimeType = "application/zip",
                                            bytes = zipBytes
                                        )
                                        toast = "Master ZIP সেভ: ${res.displayPath}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("export_master_zip_btn")
                                ) {
                                    Text("সম্পূর্ণ প্রজেক্ট Master ZIP ডাউনলোড (DXF+SVG+CSV+XLSX+DOCX+SHP+KML)")
                                }
                            }
                        }

                        items(FieldIoQualityAndDeedEngine.FILE_COMPATIBILITY_MATRIX, key = { it.extension }) { fmt ->
                            val badgeCol = if (fmt.nativeInApp) EmeraldPrimary else Color(0xFFD97706)
                            Surface(color = LocalNeuPalette.current.surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${fmt.extension} — ${fmt.titleBn}", fontWeight = FontWeight.Bold)
                                        Text(fmt.statusLabelBn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = badgeCol)
                                    }
                                    Text(fmt.guidanceBn, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
