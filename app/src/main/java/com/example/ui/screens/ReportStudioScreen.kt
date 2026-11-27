package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.model.SurveyReportConfig
import com.example.model.SurveyReportType
import com.example.model.SurveyorProfile
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.UUID

@Composable
fun ReportStudioScreen(
    surveyorProfile: SurveyorProfile = SurveyorProfile(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }

    var cfg by remember {
        mutableStateOf(
            SurveyReportConfig(
                id = UUID.randomUUID().toString(),
                reportType = SurveyReportType.MEASUREMENT_REPORT,
                title = "ডিজিটাল ভূমি পরিমাপ প্রতিবেদন",
                district = "গাজীপুর",
                upazila = "গাজীপুর সদর",
                mouza = "পুবাইল",
                jlNumber = "৮৫",
                khatianDagSummary = "বিএস খতিয়ান ৪১২ • দাগ ১০৪২",
                areaSummaryText = "২০.৫০ শতাংশ (১২.৪২ কাঠা)",
                sideLengthsText = "উ: ১০২' • দ: ১০০' • পূ: ৮৮' • প: ৯০'",
                coordinatesText = "UTM 46N: 242712E, 2648310N",
                remarks = "সরেজমিন ফিতা ও ১৬\"=১ মাইল নকশার সাথে মিলিয়ে পরিমাপ সম্পন্ন।"
            )
        )
    }
    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

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
                titleBn = "পেশাদার সার্ভে রিপোর্ট স্টুডিও",
                subtitleEn = "8 Report Types • A4 / A3 Portrait & Landscape • QR & North Arrow",
                helpTitle = "রিপোর্ট স্টুডিও ব্যবহার গাইড",
                helpSteps = listOf(
                    "৮টি প্রতিবেদনের ধরন (জরিপ মেমো, সীমানা চিহ্নিতকরণ, প্যান্টোগ্রাফ, বিভাজন, ফারায়েজ ইত্যাদি) থেকে নির্বাচন করুন।",
                    "কাগজের মাপ (A4 বা A3), Portrait/Landscape এবং উত্তর তীর ও ভেরিফিকেশন QR চালু করুন।",
                    "'রিপোর্ট প্রিভিউ' দেখে সরাসরি PNG বা PDF হিসেবে সেভ করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "report_studio_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("১. প্রতিবেদনের ধরন ও পেজ সেটআপ", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SurveyReportType.entries.forEach { rt ->
                                FilterChip(
                                    selected = cfg.reportType == rt,
                                    onClick = { cfg = cfg.copy(reportType = rt, title = rt.titleBn) },
                                    label = { Text(rt.titleBn, maxLines = 1) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("A4", "A3").forEach { ps ->
                                FilterChip(
                                    selected = cfg.pageSize == ps,
                                    onClick = { cfg = cfg.copy(pageSize = ps) },
                                    label = { Text(ps) }
                                )
                            }
                            listOf("Portrait", "Landscape").forEach { ori ->
                                FilterChip(
                                    selected = cfg.orientation == ori,
                                    onClick = { cfg = cfg.copy(orientation = ori) },
                                    label = { Text(ori) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("উত্তর দিক (North Arrow)")
                            Switch(checked = cfg.includeNorthArrow, onCheckedChange = { cfg = cfg.copy(includeNorthArrow = it) })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ভেরিফিকেশন QR ও স্বাক্ষর ব্লক")
                            Switch(
                                checked = cfg.includeQrCode && cfg.includeSignatureBlock,
                                onCheckedChange = { cfg = cfg.copy(includeQrCode = it, includeSignatureBlock = it) }
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("২. মৌজা, পরিমাপ ও মন্তব্য", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = cfg.title, onValueChange = { cfg = cfg.copy(title = it) }, label = { Text("প্রতিবেদনের শিরোনাম") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = cfg.ownerName, onValueChange = { cfg = cfg.copy(ownerName = it) }, label = { Text("মালিক / প্রজেক্ট") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cfg.scaleLabel, onValueChange = { cfg = cfg.copy(scaleLabel = it) }, label = { Text("স্কেল") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = cfg.district, onValueChange = { cfg = cfg.copy(district = it) }, label = { Text("জেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cfg.upazila, onValueChange = { cfg = cfg.copy(upazila = it) }, label = { Text("উপজেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cfg.mouza, onValueChange = { cfg = cfg.copy(mouza = it) }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = cfg.jlNumber, onValueChange = { cfg = cfg.copy(jlNumber = it) }, label = { Text("জে.এল.") }, singleLine = true, modifier = Modifier.weight(0.6f))
                            OutlinedTextField(value = cfg.khatianDagSummary, onValueChange = { cfg = cfg.copy(khatianDagSummary = it) }, label = { Text("খতিয়ান ও দাগ") }, singleLine = true, modifier = Modifier.weight(1.4f))
                        }
                        OutlinedTextField(value = cfg.areaSummaryText, onValueChange = { cfg = cfg.copy(areaSummaryText = it) }, label = { Text("জমির পরিমাণ ও ফলাফল") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = cfg.sideLengthsText, onValueChange = { cfg = cfg.copy(sideLengthsText = it) }, label = { Text("বাহুগুলোর দৈর্ঘ্য") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = cfg.coordinatesText, onValueChange = { cfg = cfg.copy(coordinatesText = it) }, label = { Text("স্থানাঙ্ক (Coordinates)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = cfg.remarks, onValueChange = { cfg = cfg.copy(remarks = it) }, label = { Text("সার্ভেয়ার মন্তব্য") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    previewBmp = DocumentAndReportEngine.renderSurveyReportBitmap(cfg, surveyorProfile)
                                    toast = "${cfg.pageSize} (${cfg.orientation}) রিপোর্ট প্রিভিউ প্রস্তুত!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier.weight(1f).testTag("render_report_preview")
                            ) {
                                Text("প্রিভিউ তৈরি")
                            }
                            Button(
                                onClick = {
                                    repo.saveReportConfig(cfg.copy(createdAtMillis = System.currentTimeMillis()))
                                    toast = "রিপোর্ট ডাটাবেজে সেভ হয়েছে!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("সেভ")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderSurveyReportBitmap(cfg, surveyorProfile)
                                    val res = ExportStreamWriter.savePngBitmap(context, bmp, "Report_${cfg.pageSize}_${System.currentTimeMillis()}")
                                    toast = "রিপোর্ট PNG সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(1f).testTag("export_report_png")
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PNG সেভ")
                            }
                            Button(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderSurveyReportBitmap(cfg, surveyorProfile)
                                    val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Report_${cfg.pageSize}_${System.currentTimeMillis()}")
                                    toast = "রিপোর্ট PDF সেভ: ${res.displayPath}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("export_report_pdf")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PDF সেভ")
                            }
                        }
                    }
                }

                if (previewBmp != null) {
                    item {
                        Image(
                            bitmap = previewBmp!!.asImageBitmap(),
                            contentDescription = "Generated Survey Report Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                        )
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
