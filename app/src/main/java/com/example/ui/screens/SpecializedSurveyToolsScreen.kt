package com.example.ui.screens

import android.graphics.Bitmap
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
import com.example.engine.ExportStreamWriter
import com.example.engine.UniversalExportEngine
import com.example.model.SurveyInstrumentItem
import com.example.model.SurveyQuotationRecord
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun SpecializedSurveyToolsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // G01 Riverbank Erosion/Accretion + G02 Classification + G03 Building Setback
    var preErosionDec by remember { mutableStateOf("45.0") }
    var postErosionDec by remember { mutableStateOf("31.5") }
    var landUseClass by remember { mutableStateOf("কৃষি (নাল)") }
    var plotSqFt by remember { mutableStateOf("4356") }
    var bldgLenFt by remember { mutableStateOf("45") }
    var bldgWidFt by remember { mutableStateOf("36") }
    var frontSetbackFt by remember { mutableStateOf("5.0") }

    // G04 Asset Inventory & G05 Calibration Register
    var assetName by remember { mutableStateOf("বৈদ্যুতিক খুঁটি (P-12)") }
    var assetCoord by remember { mutableStateOf("23.8105, 90.4126") }
    var instName by remember { mutableStateOf("") }
    var instSerial by remember { mutableStateOf("") }
    var instCalDate by remember { mutableStateOf("2026-11-26") }

    // G06 & G07 Quotation Invoice & Unofficial Surveyor Certificate
    var clientName by remember { mutableStateOf("মোঃ রফিকুল ইসলাম") }
    var mouzaDag by remember { mutableStateOf("পুবাইল মৌজা, দাগ ১০৪২") }
    var siteVisitFee by remember { mutableStateOf("2000") }
    var instFee by remember { mutableStateOf("3500") }
    var reportFee by remember { mutableStateOf("1500") }
    var paidFee by remember { mutableStateOf("3000") }
    var certMeasuredDec by remember { mutableStateOf("33.25") }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "বিশেষায়িত সার্ভে, যন্ত্র ও বিলিং (G01–G07)",
                subtitleEn = "River Erosion, Land Use, Building Coverage, Assets, Calibration, Invoice & Cert",
                helpTitle = "বিশেষায়িত সার্ভে টুলস নির্দেশিকা",
                helpSteps = listOf(
                    "নদীভাঙন বা পয়োস্তি জমির পূর্ব ও বর্তমান ক্ষেত্রফল তুলনা করুন।",
                    "ভবনের প্লট কভারেজ (%) ও সেটব্যাক হিসাব করুন।",
                    "কোটেশন/ইনভয়েস এবং জরিপ পরিমাপ সনদের খসড়া সরাসরি PDF ও প্রিন্ট করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "specialized_tools_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("G01-03 নদীভাঙন, শ্রেণি ও ভবন", tab == 0, testTag = "spec_tab_0") { tab = 0 } }
                item { NeuPillChip("G04-05 অ্যাসেট ও যন্ত্রপাতি রেজিস্টার", tab == 1, testTag = "spec_tab_1") { tab = 1 } }
                item { NeuPillChip("G06-07 কোটেশন, ইনভয়েস ও সনদ খসড়া", tab == 2, testTag = "spec_tab_2") { tab = 2 } }
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
                                Text("G01. নদীভাঙন / চর পয়োস্তি মনিটর ও G02. ভূমি শ্রেণি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(preErosionDec, { preErosionDec = it }, label = { Text("পূর্বের জমি (শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(postErosionDec, { postErosionDec = it }, label = { Text("বর্তমান জেগে থাকা জমি") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val diffEr = (postErosionDec.toDoubleOrNull() ?: 0.0) - (preErosionDec.toDoubleOrNull() ?: 0.0)
                                Text(
                                    text = if (diffEr < 0) String.format(Locale.US, "নদীগর্ভে বিলীন (Diluvion): %.3f শতক", -diffEr)
                                    else String.format(Locale.US, "পয়োস্তি/নতুন চর (Alluvion): +%.3f শতক", diffEr),
                                    fontWeight = FontWeight.Bold,
                                    color = if (diffEr < 0) Color(0xFFC62828) else EmeraldPrimary
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("কৃষি (নাল)", "বসতবাড়ি (ভিটা)", "বাণিজ্যিক", "শিল্প", "জলাশয়/পুকুর", "রাস্তা", "খাস/সংরক্ষিত", "মিশ্র")) { cls ->
                                        NeuPillChip(cls, landUseClass == cls, onClick = { landUseClass = cls })
                                    }
                                }

                                Text("G03. ভবন ফুটপ্রিন্ট ও প্লট কভারেজ (%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(plotSqFt, { plotSqFt = it }, label = { Text("প্লট (ব.ফু)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(bldgLenFt, { bldgLenFt = it }, label = { Text("ভবন দৈর্ঘ্য (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(bldgWidFt, { bldgWidFt = it }, label = { Text("ভবন প্রস্থ (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(frontSetbackFt, { frontSetbackFt = it }, label = { Text("সামনের সেটব্যাক দূরত্ব (ফুট)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                val pArea = (plotSqFt.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
                                val fArea = (bldgLenFt.toDoubleOrNull() ?: 0.0) * (bldgWidFt.toDoubleOrNull() ?: 0.0)
                                val covPct = (fArea / pArea) * 100.0
                                Text(
                                    String.format(Locale.US, "ভবন ফুটপ্রিন্ট: %.1f ব.ফু | প্লট কভারেজ: %.1f%% | সেটব্যাক: %s ft", fArea, covPct, frontSetbackFt),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }

                    1 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("G04 & G05. ইউটিলিটি অ্যাসেট এবং সার্ভে যন্ত্রপাতি রেজিস্টার", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(assetName, { assetName = it }, label = { Text("খুঁটি / কালভার্ট / ড্রেন") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(assetCoord, { assetCoord = it }, label = { Text("Lat, Lon") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val csv = UniversalExportEngine.buildCsvText(listOf("Asset", "Coordinate", "LandUse"), listOf(listOf(assetName, assetCoord, landUseClass)))
                                        val p = ExportStreamWriter.saveBinaryFileToDownloads(context, csv.toByteArray(), "utility_asset_${System.currentTimeMillis()}.csv", "text/csv")
                                        toast = "অ্যাসেট CSV সংরক্ষিত: $p"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("অ্যাসেট তালিকা CSV এক্সপোর্ট") }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(instName, { instName = it }, label = { Text("নতুন যন্ত্রের নাম") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(instSerial, { instSerial = it }, label = { Text("সিরিয়াল নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(instCalDate, { instCalDate = it }, label = { Text("ক্যালিব্রেশন") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        if (instName.isNotBlank()) {
                                            repo.saveInstrument(SurveyInstrumentItem("inst_${System.currentTimeMillis()}", instName, "Survey Gear", instSerial, instCalDate))
                                            instName = ""; instSerial = ""
                                            toast = "যন্ত্রপাতি রেজিস্টারে যুক্ত হয়েছে"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("save_instrument_btn")
                                ) { Text("যন্ত্রপাতি যোগ করুন") }
                            }
                        }
                        items(snap.instruments, key = { it.id }) { inst ->
                            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${inst.name} (${inst.category})", fontWeight = FontWeight.Bold)
                                    Text("সিরিয়াল: ${inst.serialNo} | সর্বশেষ ক্যালিব্রেশন: ${inst.lastCalibrated} | অবস্থা: ${inst.conditionNote}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            val totalQuote = (siteVisitFee.toDoubleOrNull() ?: 0.0) + (instFee.toDoubleOrNull() ?: 0.0) + (reportFee.toDoubleOrNull() ?: 0.0)
                            val due = (totalQuote - (paidFee.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("G06 & G07. সার্ভে কোটেশন, ইনভয়েস এবং পরিমাপ সনদ (খসড়া)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(clientName, { clientName = it }, label = { Text("ক্লায়েন্টের নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(mouzaDag, { mouzaDag = it }, label = { Text("মৌজা ও দাগ") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(siteVisitFee, { siteVisitFee = it }, label = { Text("সাইট ভিজিট (৳)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(instFee, { instFee = it }, label = { Text("যন্ত্রপাতি ফি (৳)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(reportFee, { reportFee = it }, label = { Text("রিপোর্ট ফি (৳)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(paidFee, { paidFee = it }, label = { Text("পরিশোধিত (৳)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(certMeasuredDec, { certMeasuredDec = it }, label = { Text("পরিমাপকৃত (শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Text(String.format(Locale.US, "মোট বিল: ৳%,.0f | বকেয়া: ৳%,.0f", totalQuote, due), fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            repo.saveQuotation(
                                                SurveyQuotationRecord(
                                                    id = "qt_${System.currentTimeMillis()}",
                                                    clientName = clientName, mouzaDag = mouzaDag,
                                                    siteVisitFee = siteVisitFee.toDoubleOrNull() ?: 0.0,
                                                    instrumentFee = instFee.toDoubleOrNull() ?: 0.0,
                                                    reportFee = reportFee.toDoubleOrNull() ?: 0.0,
                                                    paidAmount = paidFee.toDoubleOrNull() ?: 0.0
                                                )
                                            )
                                            val body = """
                                                ক্লায়েন্ট: $clientName
                                                তফসিল: $mouzaDag
                                                • সাইট ভিজিট: ৳$siteVisitFee
                                                • যন্ত্রপাতি ও ডিজিটাইজেশন: ৳$instFee
                                                • রিপোর্ট ও নকশা: ৳$reportFee
                                                • মোট বিল: ৳${String.format(Locale.US, "%,.0f", totalQuote)} | পরিশোধিত: ৳$paidFee | বকেয়া: ৳${String.format(Locale.US, "%,.0f", due)}
                                            """.trimIndent()
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap("সার্ভে কোটেশন ও ইনভয়েস", clientName, body)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "survey_invoice_${System.currentTimeMillis()}")
                                            toast = "ইনভয়েস PDF সংরক্ষিত: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_invoice_pdf_btn")
                                    ) { Text("ইনভয়েস PDF") }

                                    OutlinedButton(
                                        onClick = {
                                            val certBody = """
                                                [অস্বীকৃতি: এটি ব্যক্তিগত সার্ভেয়ার পরিমাপ প্রতিবেদন/খসড়া; স্বাক্ষর ও সিল ছাড়া সরকারি সনদ হিসেবে প্রযোজ্য নয়]
                                                
                                                ক্লায়েন্ট / মালিক: $clientName
                                                মৌজা ও দাগ: $mouzaDag
                                                জমির শ্রেণি: $landUseClass
                                                সরেজমিন পরিমাপকৃত ক্ষেত্রফল: $certMeasuredDec শতক
                                                
                                                সার্ভেয়ারের স্বাক্ষর ও সিল: ________________________
                                                তারিখ: $instCalDate
                                            """.trimIndent()
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap(
                                                "সার্ভেয়ার পরিমাপ সনদ (Unofficial Draft)",
                                                mouzaDag,
                                                certBody,
                                                "সতর্কতা: অনুমোদিত সার্ভেয়ারের স্বাক্ষর ব্যতীত এই খসড়া আইনগতভাবে কার্যকর নয়।"
                                            )
                                            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "survey_cert_draft_${System.currentTimeMillis()}")
                                            UniversalExportEngine.printBitmapDocument(context, "SurveyCertDraft", copy)
                                            toast = "সনদ খসড়া PDF সংরক্ষিত: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f).testTag("export_survey_cert_btn")
                                    ) { Text("সনদ খসড়া PDF") }
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
