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
import androidx.compose.material3.Checkbox
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
import com.example.engine.ExpansionMathAndSurveyEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.UniversalExportEngine
import com.example.model.KhajnaHoldingRecord
import com.example.model.LandServiceWorkflowKind
import com.example.model.ServiceApplicationCase
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun OnlineLandServiceWorkbenchScreen(
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var activeSection by remember { mutableIntStateOf(0) }
    var selectedKind by remember { mutableStateOf(LandServiceWorkflowKind.E_MUTATION) }
    var subWorkflow by remember { mutableStateOf("ক্রয়সূত্রে নামজারি (Purchase)") }

    var title by remember { mutableStateOf("ই-নামজারি প্রস্তুতি প্যাকেট") }
    var applicantName by remember { mutableStateOf("") }
    var applicantNid by remember { mutableStateOf("") }
    var prevOwner by remember { mutableStateOf("") }
    var mouza by remember { mutableStateOf("") }
    var khatianNo by remember { mutableStateOf("") }
    var dagNo by remember { mutableStateOf("") }
    var deedNo by remember { mutableStateOf("") }
    var appliedDec by remember { mutableStateOf("10.0") }
    var trackingNo by remember { mutableStateOf("") }
    var hearingDate by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("খসড়া প্রস্তুতি (Draft)") }
    var userConfirmedDcrReceipt by remember { mutableStateOf(false) }

    // C05 Khajna Workbench
    var holdNo by remember { mutableStateOf("") }
    var holdOwner by remember { mutableStateOf("") }
    var holdMouza by remember { mutableStateOf("পুবাইল") }
    var holdKhatian by remember { mutableStateOf("৪১২") }
    var holdDag by remember { mutableStateOf("১০৪২") }
    var holdDec by remember { mutableStateOf("20.0") }
    var holdRate by remember { mutableStateOf("10.0") }
    var holdYears by remember { mutableStateOf("2") }
    var dakhilaRef by remember { mutableStateOf("") }
    var receiptVerified by remember { mutableStateOf(false) }

    var toast by remember { mutableStateOf<String?>(null) }

    val mutationSources = listOf(
        "ক্রয়সূত্রে নামজারি (Purchase)",
        "উত্তরাধিকার / ওয়ারিশসূত্রে",
        "হেবা / দানপত্র (Gift)",
        "এওয়াজ / বিনিময় (Exchange)",
        "আদালতের ডিক্রি মূলে",
        "নিলাম খরিদ (Auction)",
        "সরকারি বন্দোবস্ত (Settlement)",
        "অন্যান্য বৈধ হস্তান্তর"
    )

    val officialPortals = listOf(
        Triple("জাতীয় ভূমি পোর্টাল (Land Portal)", "https://land.gov.bd/", "সমন্বিত নাগরিক সেবা ও আইন"),
        Triple("ই-নামজারি (E-Mutation)", "https://mutation.land.gov.bd/", "নামজারি আবেদন, স্ট্যাটাস ও ডিসিআর"),
        Triple("ই-পর্চা (সার্টিফায়েড খতিয়ান)", "https://eporcha.gov.bd/", "সিএস/এসএ/আরএস/বিএস/নামজারি খতিয়ান"),
        Triple("ভূমি উন্নয়ন কর (LD Tax)", "https://ldtax.gov.bd/", "অনলাইন খাজনা ও দাখিলা যাচাই"),
        Triple("ডিজিটাল মৌজা ম্যাপ (Settlement)", "https://map.settlement.gov.bd/", "মৌজা ম্যাপ ক্রয় ও সিট সংগ্রহ"),
        Triple("ভূমি রেকর্ড ও জরিপ অধিদপ্তর (DLRS)", "https://dlrs.gov.bd/", "জরিপ তফসিল ও প্রজ্ঞাপন"),
        Triple("বাংলাদেশ আইন (Laws of BD)", "https://bdlaws.minlaw.gov.bd/", "মূল আইন ও ধারা অনুসন্ধান"),
        Triple("মামলা ব্যবস্থাপনা (Case / Hearing)", "https://case.gov.bd/", "শুনানি ও রাজস্ব মামলার তথ্য")
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "বাংলাদেশ অনলাইন ভূমি সেবা কর্মক্ষেত্র (C01–C08 + পোর্টাল)",
                subtitleEn = "Guided Application Packet Prep, E-Mutation 8 Sources, Khajna & Official Portals",
                helpTitle = "অনলাইন ভূমি সেবা ওয়ার্কবেঞ্চ নির্দেশিকা",
                helpSteps = listOf(
                    "এখানে আবেদন ফর্মের সকল তথ্য ও ডকুমেন্ট তালিকা অফলাইনে প্রস্তুত ও ত্রুটি যাচাই করুন।",
                    "পোর্টাল বাটনে ট্যাপ করলে অফিসিয়াল ওয়েবসাইট অ্যাপের ভেতরেই খুলবে — CAPTCHA/OTP/পেমেন্ট ম্যানুয়ালি সম্পন্ন করবেন।",
                    "অ্যাপ কখনো আপনার পাসওয়ার্ড/OTP সংরক্ষণ করে না এবং রশিদ যাচাই ছাড়া আবেদন অনুমোদিত দেখায় না।"
                ),
                onBack = onNavigateBack,
                backTestTag = "online_service_workbench_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("C01–C08 আবেদন ও নামজারি প্রস্তুতি", activeSection == 0, testTag = "svc_sec_0") { activeSection = 0 } }
                item { NeuPillChip("C05 খাজনা ও হোল্ডিং যাচাই", activeSection == 1, testTag = "svc_sec_1") { activeSection = 1 } }
                item { NeuPillChip("Part I. অফিসিয়াল পোর্টাল ডিরেক্টরি (৮টি)", activeSection == 2, testTag = "svc_sec_2") { activeSection = 2 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        "নিরাপত্তা ও নীতিমালা: অফিসিয়াল পোর্টালের CAPTCHA, OTP বা অনলাইন পেমেন্ট কখনোই অটোমেট করা হয় না এবং পাসওয়ার্ড সংরক্ষণ করা হয় না। পোর্টালে জমা ও DCR/দাখিলা পাওয়ার পর কেবল ব্যবহারকারীর নিশ্চিতকরণেই স্ট্যাটাস আপডেট হয়।"
                    )
                }

                when (activeSection) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ধাপ ১: সেবার ধরন নির্বাচন করুন (C01–C08)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(LandServiceWorkflowKind.entries) { kind ->
                                        NeuPillChip(kind.titleBn, selectedKind == kind, onClick = {
                                            selectedKind = kind
                                            title = kind.titleBn
                                        })
                                    }
                                }

                                if (selectedKind == LandServiceWorkflowKind.E_MUTATION) {
                                    Text("নামজারির উৎস (৮ ধরন):", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(mutationSources) { src ->
                                            NeuPillChip(src, subWorkflow == src, onClick = { subWorkflow = src })
                                        }
                                    }
                                }

                                OutlinedTextField(title, { title = it }, label = { Text("ফাইল / আবেদনের শিরোনাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(applicantName, { applicantName = it }, label = { Text("আবেদনকারীর নাম") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(applicantNid, { applicantNid = it }, label = { Text("NID নম্বর") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(prevOwner, { prevOwner = it }, label = { Text("পূর্বের মালিক / রেকর্ডীয় প্রজা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(mouza, { mouza = it }, label = { Text("মৌজা ও জে.এল.") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(khatianNo, { khatianNo = it }, label = { Text("খতিয়ান নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(dagNo, { dagNo = it }, label = { Text("দাগ নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(deedNo, { deedNo = it }, label = { Text("দলিল নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(appliedDec, { appliedDec = it }, label = { Text("আবেদিত জমি (শতাংশ)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(trackingNo, { trackingNo = it }, label = { Text("পোর্টাল ট্র্যাকিং নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(hearingDate, { hearingDate = it }, label = { Text("শুনানির তারিখ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }

                                // Automatic Missing Document & Consistency Detector
                                val missing = buildList {
                                    if (applicantName.isBlank()) add("আবেদনকারীর নাম")
                                    if (applicantNid.isBlank()) add("জাতীয় পরিচয়পত্র (NID)")
                                    if (mouza.isBlank()) add("মৌজা ও জে.এল.")
                                    if (khatianNo.isBlank()) add("খতিয়ান নম্বর")
                                    if (dagNo.isBlank()) add("দাগ নম্বর")
                                    if (selectedKind == LandServiceWorkflowKind.E_MUTATION && subWorkflow.contains("ক্রয়") && deedNo.isBlank()) add("রেজিস্ট্রিকৃত দলিল নম্বর")
                                }
                                Surface(
                                    color = if (missing.isEmpty()) EmeraldPrimary.copy(alpha = 0.08f) else Color(0xFFD97706).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (missing.isEmpty()) "✓ প্রাথমিক প্রস্তুতি প্যাকেট সম্পূর্ণ — এখন সরকারি পোর্টালে ম্যানুয়ালি আবেদন করতে পারেন।"
                                        else "⚠ অসম্পূর্ণ তথ্য/কাগজ শনাক্ত: ${missing.joinToString(", ")}",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = userConfirmedDcrReceipt, onCheckedChange = {
                                        userConfirmedDcrReceipt = it
                                        statusText = if (it) "DCR/রশিদ ব্যবহারকারী কর্তৃক যাচাইকৃত" else "খসড়া প্রস্তুতি (Draft)"
                                    })
                                    Text("আমি অফিসিয়াল DCR / দাখিলা রশিদ হাতে পেয়ে যাচাই করেছি", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val dec = appliedDec.toDoubleOrNull() ?: 0.0
                                            val estFee = if (selectedKind == LandServiceWorkflowKind.E_MUTATION) 1170.0 else 100.0
                                            repo.saveServiceCase(
                                                ServiceApplicationCase(
                                                    id = "svc_${System.currentTimeMillis()}",
                                                    serviceKind = selectedKind,
                                                    subWorkflow = subWorkflow,
                                                    title = title,
                                                    applicantName = applicantName,
                                                    applicantNid = applicantNid,
                                                    previousOwner = prevOwner,
                                                    mouza = mouza,
                                                    prevKhatian = khatianNo,
                                                    prevDag = dagNo,
                                                    deedNumber = deedNo,
                                                    appliedDecimal = dec,
                                                    officialFeeBdt = estFee,
                                                    trackingNumber = trackingNo,
                                                    hearingDate = hearingDate,
                                                    status = statusText,
                                                    receiptVerifiedByUser = userConfirmedDcrReceipt
                                                )
                                            )
                                            toast = "সেবা আবেদন প্রস্তুতি প্যাকেট সংরক্ষিত হয়েছে"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("save_service_case_btn")
                                    ) { Text("আবেদন প্যাকেট সেভ") }

                                    OutlinedButton(
                                        onClick = { onOpenUrlInApp(selectedKind.portalUrl, selectedKind.titleBn) },
                                        modifier = Modifier.weight(1f).testTag("open_official_service_portal_btn")
                                    ) { Text("অফিসিয়াল পোর্টাল খুলুন ↗") }
                                }

                                OutlinedButton(
                                    onClick = {
                                        val body = """
                                            সেবা: ${selectedKind.titleBn} ($subWorkflow)
                                            আবেদনকারী: $applicantName | NID: $applicantNid
                                            পূর্ব মালিক: $prevOwner
                                            মৌজা: $mouza | খতিয়ান: $khatianNo | দাগ: $dagNo | দলিল: $deedNo
                                            জমি: $appliedDec শতাংশ | ট্র্যাকিং: $trackingNo | শুনানি: $hearingDate
                                            স্ট্যাটাস: $statusText
                                        """.trimIndent()
                                        val bmp = UniversalExportEngine.renderDocumentReportBitmap(
                                            title = "অনলাইন ভূমি সেবা প্রস্তুতি শিট",
                                            subtitle = selectedKind.titleBn,
                                            bodyText = body
                                        )
                                        val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                        val res = ExportStreamWriter.savePdfBitmap(context, bmp, "service_packet_${System.currentTimeMillis()}")
                                        UniversalExportEngine.printBitmapDocument(context, "ServicePacket", copy)
                                        toast = "PDF সংরক্ষিত ও প্রিন্ট প্রস্তুত: ${res.displayPath}"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("প্রস্তুতি প্যাকেট PDF ও প্রিন্ট করুন") }
                            }
                        }

                        items(snap.serviceCases, key = { it.id }) { sc ->
                            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("${sc.title} (${sc.serviceKind.titleBn})", fontWeight = FontWeight.Bold)
                                    Text("আবেদনকারী: ${sc.applicantName.ifBlank { "-" }} | মৌজা: ${sc.mouza} | খতিয়ান: ${sc.prevKhatian} | দাগ: ${sc.prevDag}", style = MaterialTheme.typography.bodySmall)
                                    Text("স্ট্যাটাস: ${sc.status} | ট্র্যাকিং: ${sc.trackingNumber.ifBlank { "অনির্ধারিত" }}", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            val dupes = ExpansionMathAndSurveyEngine.detectDuplicateHoldings(snap.khajnaHoldings)
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("C05. ভূমি উন্নয়ন কর / খাজনা হোল্ডিং ও বকেয়া ম্যানেজার", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(holdNo, { holdNo = it }, label = { Text("হোল্ডিং নং") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(holdOwner, { holdOwner = it }, label = { Text("মালিকের নাম") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(holdMouza, { holdMouza = it }, label = { Text("মৌজা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(holdKhatian, { holdKhatian = it }, label = { Text("খতিয়ান") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(holdDag, { holdDag = it }, label = { Text("দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(holdDec, { holdDec = it }, label = { Text("জমি (শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(holdRate, { holdRate = it }, label = { Text("বার্ষিক হার (৳/শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(holdYears, { holdYears = it }, label = { Text("বকেয়া বছর") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(dakhilaRef, { dakhilaRef = it }, label = { Text("দাখিলা নম্বর / রশিদ রেফারেন্স") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = receiptVerified, onCheckedChange = { receiptVerified = it })
                                    Text("অফিসিয়াল অনলাইন দাখিলা রশিদ হাতে পেয়েছি ও যাচাই করেছি", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = {
                                        val rec = KhajnaHoldingRecord(
                                            id = "kh_${System.currentTimeMillis()}",
                                            holdingNo = holdNo.ifBlank { "হোল্ডিং-${snap.khajnaHoldings.size + 1}" },
                                            ownerName = holdOwner.ifBlank { "মালিক" },
                                            mouza = holdMouza,
                                            khatianNo = holdKhatian,
                                            dagNo = holdDag,
                                            areaDecimal = holdDec.toDoubleOrNull() ?: 0.0,
                                            annualRatePerDecimalBdt = holdRate.toDoubleOrNull() ?: 10.0,
                                            arrearsYears = holdYears.toIntOrNull() ?: 0,
                                            dakhilaRef = dakhilaRef,
                                            receiptVerified = receiptVerified && dakhilaRef.isNotBlank()
                                        )
                                        repo.saveKhajnaHolding(rec)
                                        toast = "খাজনা হোল্ডিং সংরক্ষিত"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("save_khajna_holding_btn")
                                ) { Text("হোল্ডিং হিসাব ও সংরক্ষণ করুন") }

                                dupes.forEach { d ->
                                    Text("⚠ $d", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                                }
                            }
                        }
                        items(snap.khajnaHoldings, key = { it.id }) { h ->
                            val (annual, arrears, total) = ExpansionMathAndSurveyEngine.computeHoldingKhajna(h)
                            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${h.holdingNo} • ${h.ownerName}", fontWeight = FontWeight.Bold)
                                    Text("মৌজা: ${h.mouza} | খতিয়ান: ${h.khatianNo} | দাগ: ${h.dagNo} | ${h.areaDecimal} শতক", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        String.format(Locale.US, "বার্ষিক: ৳%.0f | বকেয়া (%d বছর): ৳%.0f | মোট প্রদেয় (আনুমানিক): ৳%.0f", annual, h.arrearsYears, arrears, total),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldPrimary
                                    )
                                    Text("দাখিলা: ${if (h.receiptVerified) "যাচাইকৃত (${h.dakhilaRef})" else "রশিদ সংযুক্ত/যাচাই হয়নি"}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    2 -> {
                        items(officialPortals) { (name, url, desc) ->
                            Surface(
                                color = Color.White.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(url, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                    }
                                    Button(
                                        onClick = { onOpenUrlInApp(url, name) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) { Text("খুলুন ↗") }
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
