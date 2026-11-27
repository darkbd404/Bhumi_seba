package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.model.OcrExtractedField
import com.example.model.OcrScannedDocument
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.UUID

@Composable
fun ScannerOcrScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    val snapshot by repo.snapshot.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("খতিয়ান / পর্চা স্ক্যান ০১") }
    var rawText by remember {
        mutableStateOf(
            "মৌজা: পুবাইল, জে.এল. নং: ৮৫\nখতিয়ান নং: ৪১২\nদাগ নং: ১০৪২\nমালিকের নাম: মোঃ রফিকুল ইসলাম\nজমির পরিমাণ: ৩৩.৫০ শতাংশ"
        )
    }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var highContrastFilter by remember { mutableStateOf(true) }
    var extractedFields by remember {
        mutableStateOf(DocumentAndReportEngine.extractKhatianFieldsFromText(rawText))
    }
    var userVerified by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val docHash = remember(rawText) { DocumentAndReportEngine.sha256Hex(rawText.trim()) }
    val isDuplicate = remember(docHash, snapshot.ocrDocuments) {
        snapshot.ocrDocuments.any { it.docHash == docHash }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.let { bmp ->
                scannedBitmap = bmp
                toast = "নথির ছবি লোড ও কনট্রাস্ট ক্লিন সম্পন্ন!"
            }
        }
    }

    val displayBitmap = remember(scannedBitmap, highContrastFilter) {
        val src = scannedBitmap ?: return@remember null
        if (!highContrastFilter) return@remember src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val cm = ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.45f, 0f, 0f, 0f, -45f,
                        0f, 1.45f, 0f, 0f, -45f,
                        0f, 0f, 1.45f, 0f, -45f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        c.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        out
    }

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
                titleBn = "ডকুমেন্ট স্ক্যানার ও বাংলা OCR",
                subtitleEn = "Document Cleanup • Field Extraction • Duplicate Hash Check",
                helpTitle = "ডকুমেন্ট স্ক্যানার ও OCR গাইড",
                helpSteps = listOf(
                    "Android Photo Picker থেকে পর্চা বা দলিলের ছবি নিন এবং B&W কনট্রাস্ট দিয়ে লেখা স্পষ্ট করুন।",
                    "বাংলা বা ইংরেজি পর্চার লেখা পেস্ট/সম্পাদনা করে 'ক্ষেত্র বিশ্লেষণ (Extract Fields)' চাপলে খতিয়ান, দাগ, মৌজা, জে.এল. ও পরিমাণ অটো-পিক হবে।",
                    "নথির ডুপ্লিকেট এড়াতে SHA-256 হ্যাশ চেক করা হয় এবং সব ফিল্ড ম্যানুয়ালি যাচাই করা যায়।"
                ),
                onBack = onNavigateBack,
                backTestTag = "ocr_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        text = "অস্বীকৃতি: OCR এবং অটো-এক্সট্র্যাক্টকৃত তথ্য ভুল পড়তে পারে (বিশেষত হাতের লেখা ও পুরনো বাংলা খতিয়ান)। সংরক্ষণের আগে প্রতিটি সংখ্যা মূল কাগজের সাথে মিলিয়ে নিশ্চিত করুন।"
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
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("স্ক্যান নথির শিরোনাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("ocr_pick_photo")
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("নথির ছবি নিন")
                            }
                            if (displayBitmap != null) {
                                OutlinedButton(
                                    onClick = {
                                        val res = ExportStreamWriter.savePdfBitmap(context, displayBitmap, "Scanned_Doc_${System.currentTimeMillis()}")
                                        toast = "স্ক্যানকৃত PDF সেভ: ${res.displayPath}"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("PDF সেভ")
                                }
                            }
                        }

                        if (displayBitmap != null) {
                            Image(
                                bitmap = displayBitmap.asImageBitmap(),
                                contentDescription = "Scanned Document",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ডকুমেন্ট ক্লিন ও হাই-কনট্রাস্ট B&W")
                                Switch(checked = highContrastFilter, onCheckedChange = { highContrastFilter = it })
                            }
                        }
                    }
                }

                // Bangla/English Text Extraction Engine
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("বাংলা / ইংরেজি টেক্সট ও ফিল্ড এক্সট্র্যাক্টর", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            label = { Text("স্ক্যান বা কপিকৃত খতিয়ান/দলিল লেখা") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth().testTag("ocr_raw_text_input")
                        )

                        if (isDuplicate) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "সতর্কতা: একই লেখার একটি ডকুমেন্ট ইতোমধ্যে ভল্টে সংরক্ষিত রয়েছে (Hash Match)!",
                                    color = Color(0xFFE65100),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                extractedFields = DocumentAndReportEngine.extractKhatianFieldsFromText(rawText)
                                toast = "${extractedFields.size}টি খতিয়ান/জরিপ ফিল্ড চিহ্নিত হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            modifier = Modifier.fillMaxWidth().testTag("ocr_extract_button")
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("স্বয়ংক্রিয় ফিল্ড এক্সট্র্যাক্ট (Bangla & English)")
                        }
                    }
                }

                // Editable Verified Fields
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("শনাক্তকৃত ও সম্পাদনাযোগ্য তথ্য", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        if (extractedFields.isEmpty()) {
                            Text("উপরের লেখা থেকে খতিয়ান, দাগ, মৌজা বা পরিমাণ শনাক্ত করতে 'স্বয়ংক্রিয় ফিল্ড এক্সট্র্যাক্ট' চাপুন।", style = MaterialTheme.typography.bodySmall)
                        } else {
                            extractedFields.forEachIndexed { idx, field ->
                                OutlinedTextField(
                                    value = field.value,
                                    onValueChange = { nextVal ->
                                        extractedFields = extractedFields.mapIndexed { i, f ->
                                            if (i == idx) f.copy(value = nextVal) else f
                                        }
                                    },
                                    label = {
                                        Text("${field.labelBn} (Confidence ${(field.confidence * 100).toInt()}%)")
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("আমি মূল কাগজের সাথে মিলিয়ে যাচাই করেছি")
                            Switch(checked = userVerified, onCheckedChange = { userVerified = it })
                        }

                        Button(
                            onClick = {
                                repo.saveOcrDocument(
                                    OcrScannedDocument(
                                        id = UUID.randomUUID().toString(),
                                        title = title,
                                        rawText = rawText,
                                        extractedFields = extractedFields,
                                        docHash = docHash,
                                        verifiedByUser = userVerified
                                    )
                                )
                                toast = "OCR স্ক্যান ডকুমেন্ট ডাটাবেজে সংরক্ষিত হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("ocr_save_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("যাচাইকৃত তথ্য ভল্টে সংরক্ষণ করুন")
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
