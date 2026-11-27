package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.model.DeedDraftDocument
import com.example.model.DeedTemplateType
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.UUID

@Composable
fun DeedStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    var doc by remember {
        mutableStateOf(
            DeedDraftDocument(
                id = UUID.randomUUID().toString(),
                templateType = DeedTemplateType.SAFKABALA_SALE,
                title = "সাফ-কবলা দলিল (খসড়া)",
                district = "গাজীপুর",
                upazila = "গাজীপুর সদর",
                mouza = "পুবাইল",
                jlNumber = "৮৫",
                khatianType = "বিএস (BS)",
                areaDecimal = "10.00"
            )
        )
    }
    var toast by remember { mutableStateOf<String?>(null) }
    val previewText = remember(doc) { DocumentAndReportEngine.buildDeedText(doc) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
        ) {
            WorkstationTopBar(
                titleBn = "দলিল ও আইনি খসড়া স্টুডিও",
                subtitleEn = "12 Editable BD Deed Templates • Bangla/English • PDF & .DOCX",
                helpTitle = "দলিল খসড়া স্টুডিও গাইড",
                helpSteps = listOf(
                    "উপরের ১২টি টেমপ্লেট থেকে সাফ-কবলা, বণ্টননামা, হেবা, বায়না, হলফনামা, দখলনামা বা জরিপ চুক্তি বাছুন।",
                    "পক্ষদ্বয়ের নাম, NID, জমির তফসিল ও ৪ দিকের চৌহদ্দি পূরণ করে নিচে লাইভ প্রিভিউ দেখুন।",
                    "Microsoft Word (.docx) বা PDF ফরম্যাটে সেভ করে অভিজ্ঞ আইনজীবী বা দলিল লেখকের সাথে চূড়ান্ত করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "deed_studio_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        text = DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("১. দলিলের ধরন (১২টি টেমপ্লেট)", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (doc.languageBn) "বাংলা" else "English", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(6.dp))
                                Switch(
                                    checked = doc.languageBn,
                                    onCheckedChange = {
                                        doc = doc.copy(
                                            languageBn = it,
                                            title = if (it) doc.templateType.titleBn else doc.templateType.titleEn
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DeedTemplateType.entries.forEach { t ->
                                FilterChip(
                                    selected = doc.templateType == t,
                                    onClick = {
                                        doc = doc.copy(
                                            templateType = t,
                                            title = if (doc.languageBn) t.titleBn else t.titleEn
                                        )
                                    },
                                    label = { Text(if (doc.languageBn) t.titleBn else t.titleEn, maxLines = 1) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = doc.title,
                            onValueChange = { doc = doc.copy(title = it) },
                            label = { Text("দলিলের শিরোনাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Party 1 & Party 2
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("২. ১ম ও ২য় পক্ষের বিবরণ", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = doc.firstPartyName,
                                onValueChange = { doc = doc.copy(firstPartyName = it) },
                                label = { Text("১ম পক্ষ (দাতা/বিক্রেতা)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = doc.firstPartyFather,
                                onValueChange = { doc = doc.copy(firstPartyFather = it) },
                                label = { Text("পিতা/স্বামী") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = doc.firstPartyNid,
                                onValueChange = { doc = doc.copy(firstPartyNid = it) },
                                label = { Text("১ম পক্ষের NID") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = doc.firstPartyAddress,
                                onValueChange = { doc = doc.copy(firstPartyAddress = it) },
                                label = { Text("ঠিকানা") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = doc.secondPartyName,
                                onValueChange = { doc = doc.copy(secondPartyName = it) },
                                label = { Text("২য় পক্ষ (গ্রহীতা/ক্রেতা)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = doc.secondPartyFather,
                                onValueChange = { doc = doc.copy(secondPartyFather = it) },
                                label = { Text("পিতা/স্বামী") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = doc.secondPartyNid,
                                onValueChange = { doc = doc.copy(secondPartyNid = it) },
                                label = { Text("২য় পক্ষের NID") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = doc.secondPartyAddress,
                                onValueChange = { doc = doc.copy(secondPartyAddress = it) },
                                label = { Text("ঠিকানা") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Schedule & Chouhoddi
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("৩. জমির তফসিল ও ৪-দিক চৌহদ্দি", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doc.district, onValueChange = { doc = doc.copy(district = it) }, label = { Text("জেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.upazila, onValueChange = { doc = doc.copy(upazila = it) }, label = { Text("উপজেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.mouza, onValueChange = { doc = doc.copy(mouza = it) }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doc.jlNumber, onValueChange = { doc = doc.copy(jlNumber = it) }, label = { Text("জে.এল.") }, singleLine = true, modifier = Modifier.weight(0.7f))
                            OutlinedTextField(value = doc.khatianNumber, onValueChange = { doc = doc.copy(khatianNumber = it) }, label = { Text("খতিয়ান") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.dagNumber, onValueChange = { doc = doc.copy(dagNumber = it) }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doc.areaDecimal, onValueChange = { doc = doc.copy(areaDecimal = it) }, label = { Text("পরিমাণ (শতাংশ)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.transactionValueBdt, onValueChange = { doc = doc.copy(transactionValueBdt = it) }, label = { Text("মূল্য (টাকা)") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doc.boundaryNorth, onValueChange = { doc = doc.copy(boundaryNorth = it) }, label = { Text("উত্তরে") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.boundarySouth, onValueChange = { doc = doc.copy(boundarySouth = it) }, label = { Text("দক্ষিণে") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doc.boundaryEast, onValueChange = { doc = doc.copy(boundaryEast = it) }, label = { Text("পূর্বে") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.boundaryWest, onValueChange = { doc = doc.copy(boundaryWest = it) }, label = { Text("পশ্চিমে") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = doc.termsAndConditions,
                            onValueChange = { doc = doc.copy(termsAndConditions = it) },
                            label = { Text("কাস্টম শর্তাবলি (ফাঁকা রাখলে ডিফল্ট শর্ত যুক্ত হবে)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doc.witness1, onValueChange = { doc = doc.copy(witness1 = it) }, label = { Text("১ম সাক্ষী") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = doc.witness2, onValueChange = { doc = doc.copy(witness2 = it) }, label = { Text("২য় সাক্ষী") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Live Preview & Export
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("প্রাচীন কাগজ প্রিভিউ ও রপ্তানি (PDF / PNG / DOCX)", fontWeight = FontWeight.Bold)
                        VintageStampPaperCard(
                            headerTitle = doc.title.ifBlank { doc.templateType.titleBn },
                            stampBadge = "১০০ টাকা স্ট্যাম্প শৈলী (খসড়া)",
                            serialNumber = "নং- ${doc.id.take(8).uppercase()}",
                            disclaimerText = if (doc.languageBn) DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN else DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_EN
                        ) {
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Serif,
                                color = androidx.compose.ui.graphics.Color(0xFF2B1C10)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    repo.saveDeedDraft(doc.copy(updatedAtMillis = System.currentTimeMillis()))
                                    toast = "দলিল খসড়া ভল্টে সেভ হয়েছে!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("save_deed_draft")
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("সেভ")
                            }
                            OutlinedButton(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderDeedDraftBitmap(doc)
                                    val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Deed_${doc.templateType.name}")
                                    toast = "PDF সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(1f).testTag("export_deed_pdf")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PDF")
                            }
                            Button(
                                onClick = {
                                    val bytes = DocumentAndReportEngine.buildDocxBytes(doc.title, previewText)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(
                                        context,
                                        "Deed_${doc.templateType.name}_${System.currentTimeMillis()}.docx",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        bytes
                                    )
                                    toast = "Word (.docx) সেভ: ${res.displayPath}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier.weight(1f).testTag("export_deed_docx")
                            ) {
                                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("DOCX")
                            }
                        }
                    }
                }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
