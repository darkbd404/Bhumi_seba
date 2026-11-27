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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.engine.ExportStreamWriter
import com.example.engine.FormulaExpressionEvaluator
import com.example.model.CustomServiceDefinition
import com.example.model.CustomWorkflowStep
import com.example.model.WorkflowStepKind
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Composable
fun CustomServiceEngineScreen(
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var selectedId by remember { mutableStateOf(snap.customServices.firstOrNull()?.id ?: "") }
    val activeService = snap.customServices.find { it.id == selectedId } ?: snap.customServices.firstOrNull()

    // New Custom Service Form
    var newSvcName by remember { mutableStateOf("") }
    var newSvcCat by remember { mutableStateOf("ব্যক্তিগত সেবা") }
    var newPortalUrl by remember { mutableStateOf("https://land.gov.bd/") }
    var newReqDocs by remember { mutableStateOf("NID, খতিয়ান, দলিল") }
    var newFormula by remember { mutableStateOf("base_fee + (area_dec * per_dec_fee)") }
    var newStepTitle by remember { mutableStateOf("") }
    var newStepKind by remember { mutableStateOf(WorkflowStepKind.INFO_COLLECTION) }

    // Live Fee Evaluator
    var evalAreaDec by remember { mutableStateOf("10.0") }
    var trackingRef by remember { mutableStateOf(activeService?.trackingRef ?: "") }
    var reminderDate by remember { mutableStateOf(activeService?.reminderDate ?: "2026-12-05") }

    var toast by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "কাস্টম ভূমি সেবা অটোমেশন ইঞ্জিন (Part H)",
                subtitleEn = "User-Defined Service Templates, Reorderable Workflow Steps & Fee Formula",
                helpTitle = "কাস্টম সেবা ইঞ্জিন গাইড",
                helpSteps = listOf(
                    "নিজের মতো যেকোনো ভূমি সেবার নাম, প্রয়োজনীয় ডকুমেন্ট ও ফি সূত্র যোগ করুন।",
                    "ধাপগুলো Move Up/Down করে সাজান এবং শর্ত (Yes/No) যুক্ত করুন।",
                    "JSON হিসেবে পুরো ওয়ার্কফ্লো রপ্তানি বা সংরক্ষণ করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "custom_svc_engine_back_btn"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snap.customServices, key = { it.id }) { svc ->
                            NeuPillChip(svc.serviceName, activeService?.id == svc.id, onClick = {
                                selectedId = svc.id
                                trackingRef = svc.trackingRef
                                reminderDate = svc.reminderDate
                            })
                        }
                    }
                }

                if (activeService != null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(activeService.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("বিভাগ: ${activeService.category} | পোর্টাল: ${activeService.officialPortalUrl}", style = MaterialTheme.typography.bodySmall)
                            Text("আবশ্যক কাগজপত্র: ${activeService.requiredDocsCsv}", style = MaterialTheme.typography.bodySmall)
                            Text("যোগ্যতার শর্ত: ${activeService.eligibilityRule}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B7B8C))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(evalAreaDec, { evalAreaDec = it }, label = { Text("জমি (area_dec)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(trackingRef, { trackingRef = it }, label = { Text("ট্র্যাকিং রেফ") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(reminderDate, { reminderDate = it }, label = { Text("রিমাইন্ডার") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            val feeVal = FormulaExpressionEvaluator.evaluate(
                                activeService.feeFormulaExpr,
                                mapOf(
                                    "base_fee" to activeService.defaultBaseFee,
                                    "per_dec_fee" to activeService.defaultPerDecFee,
                                    "area_dec" to (evalAreaDec.toDoubleOrNull() ?: 0.0)
                                )
                            ) ?: 0.0
                            Text("সূত্র (${activeService.feeFormulaExpr}) ফলাফল: ৳${String.format(Locale.US, "%,.0f", feeVal)}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onOpenUrlInApp(activeService.officialPortalUrl, activeService.serviceName) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("পোর্টাল খুলুন ↗") }

                                OutlinedButton(
                                    onClick = {
                                        val jsonStr = Json { prettyPrint = true }.encodeToString(activeService)
                                        val p = ExportStreamWriter.saveBinaryFileToDownloads(context, jsonStr.toByteArray(), "custom_service_${activeService.id}.json", "application/json")
                                        toast = "ওয়ার্কফ্লো JSON সেভ: $p"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("JSON এক্সপোর্ট") }
                            }

                            Text("ধাপসমূহ (Reorderable Steps):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            activeService.steps.forEachIndexed { idx, step ->
                                Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Checkbox(checked = step.completed, onCheckedChange = { chk ->
                                            val updated = activeService.steps.mapIndexed { i, s -> if (i == idx) s.copy(completed = chk) else s }
                                            repo.saveCustomService(activeService.copy(steps = updated, trackingRef = trackingRef, reminderDate = reminderDate))
                                        })
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${idx + 1}. ${step.titleBn}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            Text(step.kind.labelBn, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                        }
                                        if (idx > 0) {
                                            OutlinedButton(onClick = {
                                                val list = activeService.steps.toMutableList()
                                                val cur = list[idx]
                                                list[idx] = list[idx - 1]
                                                list[idx - 1] = cur
                                                repo.saveCustomService(activeService.copy(steps = list))
                                            }) { Text("↑") }
                                        }
                                        if (idx < activeService.steps.lastIndex) {
                                            OutlinedButton(onClick = {
                                                val list = activeService.steps.toMutableList()
                                                val cur = list[idx]
                                                list[idx] = list[idx + 1]
                                                list[idx + 1] = cur
                                                repo.saveCustomService(activeService.copy(steps = list))
                                            }) { Text("↓") }
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(newStepTitle, { newStepTitle = it }, label = { Text("নতুন ধাপ যুক্ত করুন") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedButton(onClick = {
                                    if (newStepTitle.isNotBlank()) {
                                        val next = activeService.steps + CustomWorkflowStep("st_${System.currentTimeMillis()}", newStepTitle, newStepKind)
                                        repo.saveCustomService(activeService.copy(steps = next))
                                        newStepTitle = ""
                                    }
                                }) { Text("+ ধাপ") }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(WorkflowStepKind.entries) { k ->
                                    NeuPillChip(k.labelBn, newStepKind == k, onClick = { newStepKind = k })
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("নতুন কাস্টম ভূমি সেবা তৈরি করুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(newSvcName, { newSvcName = it }, label = { Text("সেবার নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(newSvcCat, { newSvcCat = it }, label = { Text("বিভাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(newPortalUrl, { newPortalUrl = it }, label = { Text("পোর্টাল লিংক") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        OutlinedTextField(newReqDocs, { newReqDocs = it }, label = { Text("প্রয়োজনীয় ডকুমেন্ট") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(newFormula, { newFormula = it }, label = { Text("ফি সূত্র (Formula)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(
                            onClick = {
                                if (newSvcName.isNotBlank()) {
                                    val id = "cs_${System.currentTimeMillis()}"
                                    repo.saveCustomService(
                                        CustomServiceDefinition(
                                            id = id,
                                            serviceName = newSvcName,
                                            category = newSvcCat,
                                            officialPortalUrl = newPortalUrl,
                                            requiredDocsCsv = newReqDocs,
                                            feeFormulaExpr = newFormula,
                                            steps = listOf(
                                                CustomWorkflowStep("s1", "আবেদন তথ্য সংগ্রহ", WorkflowStepKind.INFO_COLLECTION),
                                                CustomWorkflowStep("s2", "কাগজপত্র ও শর্ত যাচাই", WorkflowStepKind.DOC_VERIFICATION),
                                                CustomWorkflowStep("s3", "চূড়ান্ত সংরক্ষণ", WorkflowStepKind.FINAL_ARCHIVE)
                                            )
                                        )
                                    )
                                    selectedId = id
                                    newSvcName = ""
                                    toast = "নতুন কাস্টম সেবা ওয়ার্কফ্লো তৈরি হয়েছে"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("create_custom_service_btn")
                        ) { Text("কাস্টম সেবা সেভ করুন") }
                    }
                }
            }
        }
        BottomSwipeToastBanner(toast, { toast = null }, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}
