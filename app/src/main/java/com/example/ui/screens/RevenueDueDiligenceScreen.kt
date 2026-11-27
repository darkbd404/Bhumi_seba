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
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.model.RevenueCaseRecord
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun RevenueDueDiligenceScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // D01 & D03 Case / Khas Land Manager
    var caseNo by remember { mutableStateOf("") }
    var caseCategory by remember { mutableStateOf("মিস কেস (Misc Case)") }
    var applicant by remember { mutableStateOf("") }
    var oppositeParty by remember { mutableStateOf("") }
    var mouzaDag by remember { mutableStateOf("") }
    var nextHearing by remember { mutableStateOf("") }
    var caseNotes by remember { mutableStateOf("") }

    // D02 Land Acquisition Compensation Estimator
    var acqDec by remember { mutableStateOf("15.0") }
    var acqMouzaRate by remember { mutableStateOf("250000") }
    var acqMultiplier by remember { mutableStateOf("3.0") } // 200% premium = 3x total under 2017 BD Act
    var acqStructureBdt by remember { mutableStateOf("150000") }
    var acqOut by remember { mutableStateOf("") }

    // D04 Physical Possession Dispute
    var recordAreaDec by remember { mutableStateOf("25.0") }
    var fenceAreaDec by remember { mutableStateOf("22.8") }
    var setbackDisputeFt by remember { mutableStateOf("2.5") }

    // D05 Due Diligence 5-Tier Status (Never auto-declaring Clear Title without records)
    val checklistItems = remember {
        listOf(
            "সিএস ও এসএ রেকর্ড ধারাবাহিকতা",
            "আরএস / বিএস / বিআরএস খতিয়ান মিল",
            "ভায়া দলিল চেইন (Chain of Title)",
            "হাল নামজারি ও ডিসিআর",
            "হাল সনের খাজনা / দাখিলা",
            "বণ্টননামা / ওয়ারিশ অংশ যাচাই",
            "মৌজা ম্যাপের সাথে সরেজমিন দখল",
            "সরকারি খাস / অর্পিত / ওয়াকফ / নদী এলাকা চেক",
            "সার্ভে বা দেওয়ানি মামলা অনুসন্ধান",
            "ব্যাংক বন্ধক / বায়না দায়মুক্তি (NEC)"
        )
    }
    val statuses = listOf("যাচাইকৃত", "ডকুমেন্ট প্রাপ্ত", "অযাচাইকৃত", "অমিল/বিরোধ", "আইনি পর্যালোচনা প্রয়োজন")
    val ddState = remember { mutableStateMapOf<Int, String>() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "ভূমি মামলা ও মালিকানা যাচাই (D01–D05)",
                subtitleEn = "Revenue Cases, Acquisition Compensation, Khas/Lease, Dispute & Due Diligence",
                helpTitle = "ভূমি মামলা ও ডিউ ডিলিজেন্স নির্দেশিকা",
                helpSteps = listOf(
                    "মামলা ম্যানেজারে মিস কেস, রিভিশন, আপিল ও খাস/লিজ মামলার পরবর্তী শুনানির তারিখ সংরক্ষণ করুন।",
                    "ডিউ ডিলিজেন্স রিপোর্টে কোনো কাগজ না থাকলে কখনোই 'নিষ্কণ্টক' বলবেন না — সর্বদা 'অযাচাইকৃত' বা 'আইনি পর্যালোচনা প্রয়োজন' রাখুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "revenue_dd_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("D01/D03 রাজস্ব মামলা ও খাস/লিজ", tab == 0, testTag = "dd_tab_0") { tab = 0 } }
                item { NeuPillChip("D02/D04 অধিগ্রহণ ও দখল বিরোধ", tab == 1, testTag = "dd_tab_1") { tab = 1 } }
                item { NeuPillChip("D05 মালিকানা ডিউ ডিলিজেন্স রিপোর্ট", tab == 2, testTag = "dd_tab_2") { tab = 2 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        "আইনি সতর্কতা: অনুপস্থিত বা অযাচাইকৃত সরকারি রেকর্ড ছাড়া এই অ্যাপ কখনোই জমির স্বত্ব নিষ্কণ্টক (Clear Title) ঘোষণা করে না। ঝুঁকি প্রতিবেদনে প্রতিটি ধাপ প্রমাণসাপেক্ষ।"
                    )
                }

                when (tab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("D01 & D03. রাজস্ব মামলা, সার্টিফিকেট, খাস/অর্পিত/লিজ রেজিস্টার", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("মিস কেস (Misc Case)", "রিভিশন / আপিল", "সার্টিফিকেট মামলা", "খাস জমি বন্দোবস্ত", "সরকারি লিজ / ভিপি")) { c ->
                                        NeuPillChip(c, caseCategory == c, onClick = { caseCategory = c })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(caseNo, { caseNo = it }, label = { Text("মামলা / নথি নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(nextHearing, { nextHearing = it }, label = { Text("শুনানি / নবায়ন তারিখ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(applicant, { applicant = it }, label = { Text("প্রথম পক্ষ / আবেদনকারী") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(oppositeParty, { oppositeParty = it }, label = { Text("প্রতিপক্ষ / দপ্তর") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(mouzaDag, { mouzaDag = it }, label = { Text("মৌজা, খতিয়ান ও দাগ বিবরণ") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(caseNotes, { caseNotes = it }, label = { Text("আদেশ ও নোট") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        repo.saveRevenueCase(
                                            RevenueCaseRecord(
                                                id = "rev_${System.currentTimeMillis()}",
                                                caseNo = caseNo.ifBlank { "CASE-${snap.revenueCases.size + 1}" },
                                                category = caseCategory,
                                                applicant = applicant.ifBlank { "মালিক" },
                                                oppositeParty = oppositeParty,
                                                mouzaDagSummary = mouzaDag,
                                                nextHearingDate = nextHearing,
                                                ordersAndNotes = caseNotes
                                            )
                                        )
                                        toast = "মামলা/নথি সংরক্ষিত হয়েছে"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("save_revenue_case_btn")
                                ) { Text("কেস ও শুনানি তালিকাভুক্ত করুন") }
                            }
                        }
                        items(snap.revenueCases, key = { it.id }) { rc ->
                            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${rc.caseNo} • ${rc.category}", fontWeight = FontWeight.Bold)
                                    Text("পক্ষ: ${rc.applicant} বনাম ${rc.oppositeParty.ifBlank { "সংশ্লিষ্ট দপ্তর" }}", style = MaterialTheme.typography.bodySmall)
                                    Text("তফসিল: ${rc.mouzaDagSummary} | শুনানি/নবায়ন: ${rc.nextHearingDate}", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
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
                                Text("D02. জমি অধিগ্রহণ ও ক্ষতিপূরণ প্রাক্কলন (Estimated)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(acqDec, { acqDec = it }, label = { Text("অধিগ্রহণকৃত (শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(acqMouzaRate, { acqMouzaRate = it }, label = { Text("মৌজা দর (৳/শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(acqMultiplier, { acqMultiplier = it }, label = { Text("গুণক (যেমন 3.0 = ২০০% অতিরিক্ত)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(acqStructureBdt, { acqStructureBdt = it }, label = { Text("স্থাপনা/গাছ ক্ষতিপূরণ (৳)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        val d = acqDec.toDoubleOrNull() ?: 0.0
                                        val r = acqMouzaRate.toDoubleOrNull() ?: 0.0
                                        val m = acqMultiplier.toDoubleOrNull() ?: 3.0
                                        val s = acqStructureBdt.toDoubleOrNull() ?: 0.0
                                        val landComp = d * r * m
                                        val total = landComp + s
                                        acqOut = String.format(Locale.US, "• জমির আনুমানিক ক্ষতিপূরণ: ৳%,.0f\n• অবকাঠামোসহ মোট প্রাক্কলিত ক্ষতিপূরণ: ৳%,.0f\n(চূড়ান্ত মূল্য জেলা প্রশাসকের LA শাখার অ্যাওয়ার্ড অনুযায়ী নির্ধারিত হয়)", landComp, total)
                                        repo.logCalculation("Part D02", "অধিগ্রহণ ক্ষতিপূরণ", String.format(Locale.US, "৳%,.0f (%.2f শতক)", total, d))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("calc_acq_btn")
                                ) { Text("ক্ষতিপূরণ হিসাব করুন") }
                                if (acqOut.isNotBlank()) Text(acqOut, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)

                                Text("D04. দখল ও বেদখল (Encroachment) বিশ্লেষণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(recordAreaDec, { recordAreaDec = it }, label = { Text("রেকর্ড জমি (শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(fenceAreaDec, { fenceAreaDec = it }, label = { Text("বেড়া/দখল মাপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(setbackDisputeFt, { setbackDisputeFt = it }, label = { Text("সীমানা বিচ্যুতি (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val diff = (fenceAreaDec.toDoubleOrNull() ?: 0.0) - (recordAreaDec.toDoubleOrNull() ?: 0.0)
                                Text(
                                    text = String.format(Locale.US, "দখল বনাম রেকর্ড পার্থক্য: %+.3f শতক | অনুপ্রবেশ দূরত্ব: %s ft", diff, setbackDisputeFt),
                                    fontWeight = FontWeight.Bold,
                                    color = if (kotlin.math.abs(diff) > 0.1) Color(0xFFC62828) else EmeraldPrimary
                                )
                            }
                        }
                    }

                    2 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("D05. সমন্বিত মালিকানা ডিউ ডিলিজেন্স ও ঝুঁকি মূল্যায়ন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                checklistItems.forEachIndexed { idx, label ->
                                    val cur = ddState[idx] ?: "অযাচাইকৃত"
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${idx + 1}. $label", style = MaterialTheme.typography.labelLarge)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(statuses) { st ->
                                                NeuPillChip(st, cur == st, onClick = { ddState[idx] = st })
                                            }
                                        }
                                    }
                                }

                                val verifiedCount = (checklistItems.indices).count { ddState[it] == "যাচাইকৃত" }
                                val conflictCount = (checklistItems.indices).count { ddState[it] == "অমিল/বিরোধ" }
                                val unverifiedCount = checklistItems.size - verifiedCount - conflictCount
                                val summaryRisk = when {
                                    conflictCount > 0 -> "উচ্চ ঝুঁকি (High Risk — $conflictCount টি অমিল শনাক্ত)"
                                    verifiedCount < checklistItems.size -> "অসম্পূর্ণ / পর্যালোচনা প্রয়োজন ($unverifiedCount টি অযাচাইকৃত/প্রমাণসাপেক্ষ)"
                                    else -> "ডকুমেন্ট অনুযায়ী সম্পূর্ণ (চূড়ান্ত আইনি যাচাই সাপেক্ষে)"
                                }
                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("সামগ্রিক ঝুঁকি অবস্থান: $summaryRisk", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val reportBody = buildString {
                                                append("সামগ্রিক মূল্যায়ন: $summaryRisk\n\n")
                                                checklistItems.forEachIndexed { idx, lbl ->
                                                    append("${idx + 1}. $lbl : ${ddState[idx] ?: "অযাচাইকৃত"}\n")
                                                }
                                            }
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap(
                                                "মালিকানা ডিউ ডিলিজেন্স ও ঝুঁকি প্রতিবেদন",
                                                "Ownership Due Diligence Risk Report",
                                                reportBody,
                                                "সতর্কতা: অনুপস্থিত রেকর্ড বা আদালতের তথ্য ছাড়া এটি নিষ্কণ্টক স্বত্বের সরকারি সনদ নয়।"
                                            )
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "due_diligence_${System.currentTimeMillis()}")
                                            toast = "ডিউ ডিলিজেন্স PDF সংরক্ষিত: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_due_diligence_pdf_btn")
                                    ) { Text("PDF রিপোর্ট") }

                                    OutlinedButton(
                                        onClick = {
                                            val reportBody = checklistItems.mapIndexed { idx, lbl ->
                                                "${idx + 1}. $lbl : ${ddState[idx] ?: "অযাচাইকৃত"}"
                                            }.joinToString("\n")
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap("ডিউ ডিলিজেন্স প্রতিবেদন", summaryRisk, reportBody)
                                            UniversalExportEngine.printBitmapDocument(context, "DueDiligence", bmp)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("প্রিন্ট") }
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
