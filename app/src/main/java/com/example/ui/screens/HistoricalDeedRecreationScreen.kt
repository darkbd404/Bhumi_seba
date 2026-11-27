package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterCadRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.FieldIoQualityAndDeedEngine
import com.example.engine.UniversalExportEngine
import com.example.model.DeedTemplateType
import com.example.model.HistoricalDeedRecord
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.UUID

@Composable
fun HistoricalDeedRecreationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { MasterCadRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var record by remember(snap.historicalDeeds.firstOrNull()?.id) {
        mutableStateOf(
            snap.historicalDeeds.firstOrNull() ?: HistoricalDeedRecord(id = UUID.randomUUID().toString())
        )
    }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var deskewDeg by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1.35f) }
    var stainCleanupBw by remember { mutableStateOf(true) }
    var sideBySideView by remember { mutableStateOf(true) }
    var outputTab by remember { mutableIntStateOf(0) } // 0 = Faithful Historical, 1 = Modern Editable Draft
    var toast by remember { mutableStateOf<String?>(null) }

    val enhancedBitmap = remember(sourceBitmap, deskewDeg, contrast, stainCleanupBw) {
        sourceBitmap?.let { src ->
            FieldIoQualityAndDeedEngine.enhanceOldDeedBitmap(src, deskewDeg, contrast, stainCleanupBw)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val raw = BitmapFactory.decodeStream(stream)
                    if (raw != null) {
                        val maxSide = 1400f
                        val scale = (maxSide / maxOf(raw.width, raw.height)).coerceAtMost(1f)
                        sourceBitmap = if (scale < 1f) {
                            Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true)
                        } else raw
                        record = record.copy(sourceImageUri = uri.toString())
                        toast = "পুরাতন দলিলের ছবি লোড হয়েছে — পরিষ্কার ও OCR যাচাই করুন।"
                    }
                }
            }.onFailure {
                toast = "ছবি লোড ব্যর্থ: ${it.message}"
            }
        }
    }

    val faithfulText = remember(record) {
        FieldIoQualityAndDeedEngine.buildFaithfulHistoricalReproduction(record)
    }
    val modernText = remember(record) {
        FieldIoQualityAndDeedEngine.buildModernEditableLegalDraft(record)
    }
    val activeOutputText = if (outputTab == 0) faithfulText else modernText

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
                titleBn = "পুরাতন দলিল পুনর্গঠন ও ভিন্টেজ স্ট্যাম্প",
                subtitleEn = "Old Deed Photo Cleanup • Bangla/English OCR • Faithful Copy vs Modern Legal Draft",
                helpTitle = "পুরাতন দলিল পুনর্গঠন গাইড",
                helpSteps = listOf(
                    "১. Photo Picker দিয়ে পুরাতন পাতা-ফোঁটা দলিলের ছবি নিন এবং Deskew ও Stain Cleanup স্লাইডার দিয়ে স্পষ্ট করুন।",
                    "২. OCR বা পঠিত লেখা বক্সে পেস্ট করে 'অটো-ফিল' চাপলে দাতা, গ্রহীতা, মৌজা, খতিয়ান, দাগ ও চৌহদ্দি পূরণ হবে।",
                    "৩. নিচে ঐতিহাসিক অনুলিপি অথবা আধুনিক খসড়া—দুটিই আসল পুরাতন বাংলাদেশি স্ট্যাম্প কাগজের শৈলীতে PNG/PDF/DOCX রপ্তানি করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "hist_deed_back_btn"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        text = "অস্বীকৃতি: স্ক্যান করা পুরাতন দলিল থেকে পুনর্গঠিত অনুলিপি বা খসড়া সরকারি সার্টিফায়েড নকল নয়; কেবল রেফারেন্স ও খসড়া প্রস্তুতির জন্য।"
                    )
                }

                // 1. Scan & Image Cleanup Panel
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 18.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("১. পুরাতন দলিল স্ক্যান ও পরিষ্কার (Deskew & Stain Cleanup)", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("pick_old_deed_btn")
                            ) {
                                Text("গ্যালারি থেকে দলিল ছবি নিন")
                            }
                            OutlinedButton(
                                onClick = { sideBySideView = !sideBySideView },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (sideBySideView) "একক ভিউ" else "পাশাপাশি তুলনা")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("হলদে দাগ দূর (High-Contrast B&W)", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = stainCleanupBw, onCheckedChange = { stainCleanupBw = it })
                        }
                        Text("Deskew কোণ: ${String.format("%.1f°", deskewDeg)}", style = MaterialTheme.typography.labelSmall)
                        Slider(value = deskewDeg, onValueChange = { deskewDeg = it }, valueRange = -15f..15f)
                        Text("কনট্রাস্ট ও কালি গাঢ়ত্ব: ${String.format("%.2fx", contrast)}", style = MaterialTheme.typography.labelSmall)
                        Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.8f..2.2f)

                        if (enhancedBitmap != null) {
                            if (sideBySideView) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        bitmap = enhancedBitmap.asImageBitmap(),
                                        contentDescription = "Enhanced Old Deed",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Surface(
                                        color = Color(0xFFFDF7E8),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(220.dp)
                                    ) {
                                        Text(
                                            text = record.ocrRawText.ifBlank { "ডান পাশে OCR পাঠ বা পঠিত লেখা লিখুন..." },
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            } else {
                                Image(
                                    bitmap = enhancedBitmap.asImageBitmap(),
                                    contentDescription = "Enhanced Old Deed",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                }

                // 2. OCR Parse & Structured Editor
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 18.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("২. OCR পাঠ বিশ্লেষণ ও তথ্য সম্পাদনা", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = record.ocrRawText,
                            onValueChange = { record = record.copy(ocrRawText = it) },
                            label = { Text("মূল দলিলের OCR / টাইপ করা পাঠ") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("hist_ocr_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    record = FieldIoQualityAndDeedEngine.parseHistoricalDeedText(record.ocrRawText, record)
                                    toast = "OCR থেকে দলিলের তথ্য অটো-ফিল সম্পন্ন!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier.weight(1f).testTag("hist_ocr_parse_btn")
                            ) {
                                Text("OCR পার্স ও অটো-ফিল")
                            }
                            OutlinedButton(
                                onClick = {
                                    val sample = """
                                        দাতা: মৃত হাজী ছমির উদ্দিন
                                        গ্রহীতা: মোঃ রফিকুল ইসলাম
                                        দলিল নং: ৪৫২১/১৯৮৪  তারিখ: ১২/০৪/১৯৮৪
                                        সাব-রেজিস্ট্রি: গাজীপুর সদর
                                        মৌজা: পুবাইল  জে.এল.: ৮৫
                                        খতিয়ান: ৪১২  দাগ: ১০৪২  পরিমাণ: 24.79 শতাংশ
                                        উত্তরে: দাগ ১০৪১  দক্ষিণে: ১২ ফুট রাস্তা
                                        পূর্বে: দাগ ১০৪৩  পশ্চিমে: পুকুর পাড়
                                        পূর্ব দলিল: দলিল নং ১১০৫/১৯৬২
                                    """.trimIndent()
                                    record = FieldIoQualityAndDeedEngine.parseHistoricalDeedText(sample, record)
                                    toast = "নমুনা পুরাতন দলিলের পাঠ লোড ও পার্স হয়েছে!"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("নমুনা OCR লোড")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = record.deedNumber, onValueChange = { record = record.copy(deedNumber = it) }, label = { Text("মূল দলিল নং") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.registrationDate, onValueChange = { record = record.copy(registrationDate = it) }, label = { Text("রেজিস্ট্রি তারিখ") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = record.registrationOffice, onValueChange = { record = record.copy(registrationOffice = it) }, label = { Text("সাব-রেজিস্ট্রি অফিস") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = record.sellerName, onValueChange = { record = record.copy(sellerName = it) }, label = { Text("দাতা / বিক্রেতা") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.buyerName, onValueChange = { record = record.copy(buyerName = it) }, label = { Text("গ্রহীতা / ক্রেতা") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = record.mouza, onValueChange = { record = record.copy(mouza = it) }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.jlNumber, onValueChange = { record = record.copy(jlNumber = it) }, label = { Text("জে.এল.") }, singleLine = true, modifier = Modifier.weight(0.6f))
                            OutlinedTextField(value = record.khatianNumber, onValueChange = { record = record.copy(khatianNumber = it) }, label = { Text("খতিয়ান") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = record.dagNumber, onValueChange = { record = record.copy(dagNumber = it) }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.areaDecimal, onValueChange = { record = record.copy(areaDecimal = it) }, label = { Text("পরিমাণ (শতাংশ)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.stampValueLabel, onValueChange = { record = record.copy(stampValueLabel = it) }, label = { Text("স্ট্যাম্প মূল্য") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = record.boundaryNorth, onValueChange = { record = record.copy(boundaryNorth = it) }, label = { Text("উত্তরে") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.boundarySouth, onValueChange = { record = record.copy(boundarySouth = it) }, label = { Text("দক্ষিণে") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = record.boundaryEast, onValueChange = { record = record.copy(boundaryEast = it) }, label = { Text("পূর্বে") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = record.boundaryWest, onValueChange = { record = record.copy(boundaryWest = it) }, label = { Text("পশ্চিমে") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = record.customArchivalTerms,
                            onValueChange = { record = record.copy(customArchivalTerms = it) },
                            label = { Text("অতিরিক্ত / সংশোধিত দলিলের বক্তব্য (ঐচ্ছিক)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 3. Dual Mode Output on Authentic Vintage Stamp Paper
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 18.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("৩. পুরাতন কাগজে চূড়ান্ত প্রিভিউ ও রপ্তানি", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NeuPillChip(
                                text = "ঐতিহাসিক পুনর্গঠন",
                                selected = outputTab == 0,
                                onClick = { outputTab = 0 }
                            )
                            NeuPillChip(
                                text = "আধুনিক খসড়া",
                                selected = outputTab == 1,
                                onClick = { outputTab = 1 }
                            )
                        }

                        VintageStampPaperCard(
                            headerTitle = if (outputTab == 0) record.title else "আধুনিক পুনঃলিখিত খসড়া — ${record.mouza}",
                            stampBadge = record.stampValueLabel.ifBlank { "১০০ টাকা (প্রাচীন কাগজ)" },
                            serialNumber = "দলিল: ${record.deedNumber.ifBlank { "DRAFT" }}",
                            disclaimerText = DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN
                        ) {
                            Text(
                                text = activeOutputText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF2B1C10)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    repo.saveHistoricalDeed(record.copy(updatedAtMillis = System.currentTimeMillis()))
                                    toast = "পুনর্গঠিত দলিল ভল্টে সেভ হয়েছে!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("hist_save_btn")
                            ) {
                                Text("সেভ", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                        title = record.title,
                                        stampValueLabel = record.stampValueLabel,
                                        serialTag = "দলিল-${record.deedNumber.ifBlank { "DRAFT" }}",
                                        bodyText = activeOutputText
                                    )
                                    val res = ExportStreamWriter.savePngBitmap(context, bmp, "Vintage_Deed_${System.currentTimeMillis()}")
                                    toast = "ভিন্টেজ PNG সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(1f).testTag("hist_png_btn")
                            ) {
                                Text("PNG", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                        title = record.title,
                                        stampValueLabel = record.stampValueLabel,
                                        serialTag = "দলিল-${record.deedNumber.ifBlank { "DRAFT" }}",
                                        bodyText = activeOutputText
                                    )
                                    val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Vintage_Deed_${System.currentTimeMillis()}")
                                    toast = "ভিন্টেজ PDF সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(1f).testTag("hist_pdf_btn")
                            ) {
                                Text("PDF", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    val bytes = DocumentAndReportEngine.buildDocxBytes(record.title, activeOutputText)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(
                                        context,
                                        "Historical_Deed_${System.currentTimeMillis()}.docx",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        bytes
                                    )
                                    toast = "DOCX সেভ: ${res.displayPath}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier.weight(1f).testTag("hist_docx_btn")
                            ) {
                                Text("DOCX", maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                    title = record.title,
                                    stampValueLabel = record.stampValueLabel,
                                    serialTag = "দলিল-${record.deedNumber.ifBlank { "DRAFT" }}",
                                    bodyText = activeOutputText
                                )
                                val printCopy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                bmp.recycle()
                                UniversalExportEngine.printBitmapDocument(context, "Historical_Deed_Vintage", printCopy)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("প্রিন্ট ডায়ালগ খুলুন (Print Vintage Stamp Paper)")
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
