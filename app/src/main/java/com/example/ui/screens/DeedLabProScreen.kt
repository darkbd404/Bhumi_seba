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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.engine.UniversalExportEngine
import com.example.model.EditableLegalClause
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun DeedLabProScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // E01 & E02 Multi-party & Multi-Dag Schedule Builder
    var deedTitle by remember { mutableStateOf("সাফ-কবলা দলিল ও তফসিল (খসড়া)") }
    val sellers = remember { mutableStateListOf("মোঃ রফিকুল ইসলাম (হিস্যা ১.০০)") }
    val buyers = remember { mutableStateListOf("মোঃ করিম উদ্দিন") }
    var newSeller by remember { mutableStateOf("") }
    var newBuyer by remember { mutableStateOf("") }

    var mouzaSchedule by remember { mutableStateOf("পুবাইল, জে.এল. নং ৮৫") }
    var formerKhatian by remember { mutableStateOf("সাবেক ২১০ (আরএস)") }
    var currentKhatian by remember { mutableStateOf("হাল ৪১২ (বিএস)") }
    var scheduleRowsText by remember { mutableStateOf("দাগ ১০৪২ | নাল | ৩৩.৫০ শতক | উঃ রাস্তা, দঃ ১০৪৪, পূঃ ১০৪৩, পঃ ১০৩৯") }
    var selectedClausesBody by remember { mutableStateOf("") }

    // E03 Deed Verification & Mismatch Assistant
    var verPartyOk by remember { mutableStateOf("33.50") }
    var verMeasuredArea by remember { mutableStateOf("33.20") }
    var verBoundaryMatch by remember { mutableStateOf(true) }
    var verPrevKhatianMatch by remember { mutableStateOf(true) }
    var verWitnessNames by remember { mutableStateOf("১. মোঃ হাসান, ২. মোঃ জামাল") }

    // E04 Legal Clause Search & Add
    var clauseQuery by remember { mutableStateOf("") }
    var newClauseCat by remember { mutableStateOf("বিক্রয়") }
    var newClauseTitle by remember { mutableStateOf("") }
    var newClauseBodyBn by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "দলিল ও তফসিল ল্যাব প্রো (E01–E05)",
                subtitleEn = "Unlimited Multi-Party Deed Composer, Tafsil Schedule, Mismatch Check & Clauses",
                helpTitle = "দলিল ল্যাব প্রো গাইড",
                helpSteps = listOf(
                    "বহু দাতা/গ্রহীতা এবং একাধিক সাবেক-হাল দাগ দিয়ে সম্পূর্ণ দলিল তফসিল তৈরি করুন।",
                    "ধারা লাইব্রেরি থেকে যেকোনো বাংলা/ইংরেজি শর্ত এক ট্যাপে দলিলের খসড়ায় যুক্ত করুন।",
                    "সব দলিল কেবল আনসার্টিফায়েড খসড়া — রেজিস্ট্রির আগে সাব-রেজিস্ট্রি ও আইনজীবীর মাধ্যমে যাচাই করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "deed_lab_pro_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("E01/E02 বহু-পক্ষ দলিল ও তফসিল", tab == 0, testTag = "dl_tab_0") { tab = 0 } }
                item { NeuPillChip("E03 দলিল ভুল ও অমিল যাচাই", tab == 1, testTag = "dl_tab_1") { tab = 1 } }
                item { NeuPillChip("E04/E05 আইনি ধারা লাইব্রেরি", tab == 2, testTag = "dl_tab_2") { tab = 2 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN)
                }

                when (tab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("E01 & E02. বহু-দাতা/গ্রহীতা দলিল ও মৌজা/দাগ তফসিল", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(deedTitle, { deedTitle = it }, label = { Text("দলিলের শিরোনাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(newSeller, { newSeller = it }, label = { Text("নতুন দাতা/বিক্রেতা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedButton(onClick = { if (newSeller.isNotBlank()) { sellers.add(newSeller); newSeller = "" } }) { Text("+ দাতা") }
                                }
                                Text("দাতাগণ (${sellers.size}): ${sellers.joinToString(" | ")}", style = MaterialTheme.typography.bodySmall)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(newBuyer, { newBuyer = it }, label = { Text("নতুন গ্রহীতা/ক্রেতা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedButton(onClick = { if (newBuyer.isNotBlank()) { buyers.add(newBuyer); newBuyer = "" } }) { Text("+ গ্রহীতা") }
                                }
                                Text("গ্রহীতাগণ (${buyers.size}): ${buyers.joinToString(" | ")}", style = MaterialTheme.typography.bodySmall)

                                OutlinedTextField(mouzaSchedule, { mouzaSchedule = it }, label = { Text("মৌজা ও জে.এল. নং") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(formerKhatian, { formerKhatian = it }, label = { Text("সাবেক খতিয়ান") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(currentKhatian, { currentKhatian = it }, label = { Text("হাল খতিয়ান") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(scheduleRowsText, { scheduleRowsText = it }, label = { Text("তফসিল দাগ ও ৪-সীমানা (প্রতি লাইনে ১টি দাগ)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(selectedClausesBody, { selectedClausesBody = it }, label = { Text("শর্তাবলি ও ধারা (ধারা লাইব্রেরি থেকে যুক্ত)") }, modifier = Modifier.fillMaxWidth())

                                val fullDeedDraft = buildString {
                                    append("=== $deedTitle ===\n")
                                    append("[${DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN}]\n\n")
                                    append("১. প্রথম পক্ষ (দাতাগণ):\n")
                                    sellers.forEachIndexed { idx, s -> append("   ${idx + 1}. $s\n") }
                                    append("\n২. দ্বিতীয় পক্ষ (গ্রহীতাগণ):\n")
                                    buyers.forEachIndexed { idx, b -> append("   ${idx + 1}. $b\n") }
                                    append("\n৩. জমির তফসিল ($mouzaSchedule):\n")
                                    append("   সাবেক খতিয়ান: $formerKhatian | হাল খতিয়ান: $currentKhatian\n")
                                    scheduleRowsText.lines().forEach { append("   • $it\n") }
                                    append("\n৪. শর্তাবলি:\n${selectedClausesBody.ifBlank { "নিষ্কণ্টক দখল ও হস্তান্তর।" }}\n")
                                    append("\n৫. সাক্ষীগণ: $verWitnessNames\n")
                                    append("\n৬. স্বাক্ষর: দাতা ________________   গ্রহীতা ________________")
                                }

                                VintageStampPaperCard(
                                    headerTitle = deedTitle,
                                    stampBadge = "১০০ টাকা দলিল কাগজ (খসড়া)",
                                    serialNumber = "PRO-${mouzaSchedule.take(10)}",
                                    disclaimerText = DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN
                                ) {
                                    Text(
                                        text = fullDeedDraft,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                        color = Color(0xFF2B1C10)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val docxBytes = DocumentAndReportEngine.buildDocxBytes(deedTitle, fullDeedDraft)
                                            val res = DocumentAndReportEngine.saveBinaryDocument(
                                                context,
                                                "deed_pro_${System.currentTimeMillis()}.docx",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                docxBytes
                                            )
                                            toast = "DOCX সংরক্ষিত: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_deed_pro_docx_btn")
                                    ) { Text("DOCX") }

                                    OutlinedButton(
                                        onClick = {
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap(
                                                title = deedTitle,
                                                subtitle = mouzaSchedule,
                                                bodyText = fullDeedDraft,
                                                disclaimer = DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN,
                                                useVintagePaper = true
                                            )
                                            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "deed_pro_${System.currentTimeMillis()}")
                                            UniversalExportEngine.printBitmapDocument(context, "DeedPro", copy)
                                            toast = "PDF সংরক্ষিত ও প্রিন্ট প্রস্তুত: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f).testTag("export_deed_pro_pdf_btn")
                                    ) { Text("PDF ও প্রিন্ট") }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("E03. দলিল প্রুফরিডিং ও অমিল (Mismatch) শনাক্তকরণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(verPartyOk, { verPartyOk = it }, label = { Text("দলিলের জমি (শতাংশ)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(verMeasuredArea, { verMeasuredArea = it }, label = { Text("খতিয়ান/মাপ (শতাংশ)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(verWitnessNames, { verWitnessNames = it }, label = { Text("সাক্ষীগণের নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                val d = (verPartyOk.toDoubleOrNull() ?: 0.0) - (verMeasuredArea.toDoubleOrNull() ?: 0.0)
                                val alerts = buildList {
                                    if (sellers.isEmpty() || buyers.isEmpty()) add("দাতা বা গ্রহীতার নাম অনুপস্থিত")
                                    if (kotlin.math.abs(d) > 0.05) add("জমির পরিমাণ অমিল: ${String.format(Locale.US, "%+.3f শতক", d)}")
                                    if (verWitnessNames.isBlank()) add("সাক্ষীগণের নাম লেখা হয়নি")
                                    if (!verBoundaryMatch) add("৪-সীমানা/চৌহদ্দিতে অমিল")
                                    if (!verPrevKhatianMatch) add("পূর্ব-খতিয়ান ধারাবাহিকতা অমিল")
                                }
                                Surface(
                                    color = if (alerts.isEmpty()) EmeraldPrimary.copy(alpha = 0.09f) else Color(0xFFC62828).copy(alpha = 0.09f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (alerts.isEmpty()) "✓ খসড়া তথ্যসমূহে কোনো বড় অমিল পাওয়া যায়নি (সাব-রেজিস্ট্রি প্রুফরিডিং আবশ্যক)।"
                                        else alerts.joinToString("\n") { "⚠ $it" },
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("E04. সম্পাদনাযোগ্য আইনি ধারা লাইব্রেরি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(clauseQuery, { clauseQuery = it }, label = { Text("ধারা খুঁজুন (বিক্রয়, হেবা, বণ্টন, দখল...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(newClauseCat, { newClauseCat = it }, label = { Text("বিভাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(newClauseTitle, { newClauseTitle = it }, label = { Text("ধারার শিরোনাম") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(newClauseBodyBn, { newClauseBodyBn = it }, label = { Text("ধারার বাংলা বক্তব্য") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        if (newClauseTitle.isNotBlank() && newClauseBodyBn.isNotBlank()) {
                                            repo.saveLegalClause(
                                                EditableLegalClause("cl_${System.currentTimeMillis()}", newClauseCat, newClauseTitle, newClauseBodyBn, "Custom Legal Clause")
                                            )
                                            newClauseTitle = ""; newClauseBodyBn = ""
                                            toast = "নতুন ধারা লাইব্রেরিতে যুক্ত হয়েছে"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("নতুন ধারা সেভ করুন") }
                            }
                        }

                        val filtered = snap.legalClauses.filter {
                            clauseQuery.isBlank() || it.category.contains(clauseQuery, true) || it.titleBn.contains(clauseQuery, true) || it.bodyBn.contains(clauseQuery, true)
                        }
                        items(filtered, key = { it.id }) { cl ->
                            Surface(color = Color.White.copy(alpha = 0.78f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("[${cl.category}] ${cl.titleBn}", fontWeight = FontWeight.Bold)
                                    Text(cl.bodyBn, style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick = {
                                        selectedClausesBody = (selectedClausesBody + "\n• " + cl.bodyBn).trim()
                                        toast = "'${cl.titleBn}' দলিল খসড়ায় যুক্ত হয়েছে"
                                    }) { Text("+ দলিল খসড়ায় যোগ করুন") }
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
