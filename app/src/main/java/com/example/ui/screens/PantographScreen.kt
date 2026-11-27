package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.GisCoordinateEngine
import com.example.model.CadastralSurveyStage
import com.example.model.ControlPointPair
import com.example.model.TransformModelType
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PantographScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var baseStage by remember { mutableStateOf(CadastralSurveyStage.CS) }
    var overlayStage by remember { mutableStateOf(CadastralSurveyStage.RS) }
    var transformType by remember { mutableStateOf(TransformModelType.AFFINE) }
    var sideBySide by remember { mutableStateOf(false) }
    var overlayOpacity by remember { mutableFloatStateOf(0.5f) }
    var overlayScale by remember { mutableFloatStateOf(1.0f) }
    var overlayRot by remember { mutableFloatStateOf(0f) }
    var overlayShiftX by remember { mutableFloatStateOf(0f) }
    var blinkMode by remember { mutableStateOf(false) }
    var blinkShowOverlay by remember { mutableStateOf(true) }

    var baseBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var overlayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var controlPointsInput by remember {
        mutableStateOf("CP1: 100,100 -> 104,98\nCP2: 500,100 -> 512,102\nCP3: 500,450 -> 510,460\nCP4: 100,450 -> 102,458")
    }
    var dagMatchNotes by remember { mutableStateOf("সাবেক CS ৪৫০ -> হাল RS ৯১২/৯১৩ (+২.১% এলাকা পরিবর্তন)") }

    LaunchedEffect(blinkMode) {
        while (blinkMode) {
            delay(700L)
            blinkShowOverlay = !blinkShowOverlay
        }
    }

    val basePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()?.let { baseBitmap = it }
        }
    }
    val overlayPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()?.let { overlayBitmap = it }
        }
    }

    val parsedPairs = remember(controlPointsInput) {
        controlPointsInput.lines().mapIndexedNotNull { idx, ln ->
            val arrowParts = ln.split("->")
            if (arrowParts.size != 2) return@mapIndexedNotNull null
            val leftPart = arrowParts[0]
            val label = if (leftPart.contains(":")) leftPart.substringBefore(":").trim() else "CP${idx + 1}"
            val coords1 = leftPart.substringAfter(":").split(",").mapNotNull { it.trim().toDoubleOrNull() }
            val coords2 = arrowParts[1].split(",").mapNotNull { it.trim().toDoubleOrNull() }
            if (coords1.size >= 2 && coords2.size >= 2) {
                ControlPointPair("cp_$idx", label, coords1[0], coords1[1], coords2[0], coords2[1])
            } else null
        }
    }

    val transformReport = remember(baseStage, overlayStage, transformType, parsedPairs, dagMatchNotes) {
        GisCoordinateEngine.computePantographTransform(
            baseStage = baseStage,
            overlayStage = overlayStage,
            transformType = transformType,
            pairs = parsedPairs,
            dagMatches = dagMatchNotes.lines().filter { it.isNotBlank() }
        )
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
                .widthIn(max = 980.dp)
        ) {
            WorkstationTopBar(
                titleBn = "প্যান্টোগ্রাফ ও নকশা তুলনা",
                subtitleEn = "CS vs SA vs RS vs BS • Overlay • Affine & TPS Control Points",
                helpTitle = "প্যান্টোগ্রাফ ব্যবহার নির্দেশিকা",
                helpSteps = listOf(
                    "১ম ম্যাপ (যেমন CS) ও ২য় ম্যাপ (যেমন RS/BS) গ্যালারি থেকে সিলেক্ট করুন — মূল ফাইল কখনোই পরিবর্তিত হয় না।",
                    "স্বচ্ছতা স্লাইডার বা ব্লিঙ্ক তুলনা দিয়ে দুটি নকশার আইলের পার্থক্য দেখুন।",
                    "কন্ট্রোল পয়েন্ট বসালে অ্যাপ স্বয়ংক্রিয়ভাবে Affine/TPS স্কেল, ঘূর্ণন ও RMSE রেসিডুয়াল ত্রুটি রিপোর্ট দেয়।"
                ),
                onBack = onBack,
                backTestTag = "pantograph_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        "প্যান্টোগ্রাফ রূপান্তর ও ওভারলে আনুমানিক নকশা বিশ্লেষণের জন্য; মূল সরকারি ম্যাপ অপরিবর্তিত থাকে।"
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("তুলনামূলক জরিপ স্তর", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    basePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f).testTag("panto_pick_base")
                            ) { Text("বেস ম্যাপ (${baseStage.name})", maxLines = 1) }
                            OutlinedButton(
                                onClick = {
                                    overlayPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f).testTag("panto_pick_overlay")
                            ) { Text("ওভারলে (${overlayStage.name})", maxLines = 1) }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CadastralSurveyStage.entries.forEach { st ->
                                NeuPillChip(
                                    text = "বেস: ${st.name}",
                                    selected = baseStage == st,
                                    onClick = { baseStage = st }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CadastralSurveyStage.entries.forEach { st ->
                                NeuPillChip(
                                    text = "ওভারলে: ${st.name}",
                                    selected = overlayStage == st,
                                    onClick = { overlayStage = st }
                                )
                            }
                        }
                    }
                }

                // Visual Overlay / Side-by-Side Viewport
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("সাইড-বাই-সাইড", style = MaterialTheme.typography.labelLarge)
                            Switch(checked = sideBySide, onCheckedChange = { sideBySide = it })
                            Text("ব্লিঙ্ক তুলনা", style = MaterialTheme.typography.labelLarge)
                            Switch(checked = blinkMode, onCheckedChange = { blinkMode = it })
                        }

                        if (sideBySide) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MapComparePane(baseBitmap, "${baseStage.label} (বেস)", Color(0xFF0F6B54), Modifier.weight(1f))
                                MapComparePane(overlayBitmap, "${overlayStage.label} (নতুন)", Color(0xFF1A73E8), Modifier.weight(1f))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF3F6F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                MapComparePane(baseBitmap, "${baseStage.label} বেস", Color(0xFF0F6B54), Modifier.fillMaxSize())
                                val ovAlpha = if (blinkMode) (if (blinkShowOverlay) overlayOpacity else 0f) else overlayOpacity
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(ovAlpha)
                                        .graphicsLayer {
                                            scaleX = overlayScale
                                            scaleY = overlayScale
                                            rotationZ = overlayRot
                                            translationX = overlayShiftX
                                        }
                                ) {
                                    MapComparePane(overlayBitmap, "${overlayStage.label} ওভারলে", Color(0xFF1A73E8), Modifier.fillMaxSize())
                                }
                            }
                        }

                        Text(String.format(Locale.US, "ওভারলে স্বচ্ছতা: %.0f%% | স্কেল: %.2fx | ঘূর্ণন: %.1f°", overlayOpacity * 100f, overlayScale, overlayRot), style = MaterialTheme.typography.labelSmall)
                        Slider(value = overlayOpacity, onValueChange = { overlayOpacity = it }, valueRange = 0f..1f)
                        Slider(value = overlayScale, onValueChange = { overlayScale = it }, valueRange = 0.5f..2.0f)
                        Slider(value = overlayRot, onValueChange = { overlayRot = it }, valueRange = -45f..45f)
                        Slider(value = overlayShiftX, onValueChange = { overlayShiftX = it }, valueRange = -120f..120f)
                        Button(
                            onClick = {
                                overlayScale = transformReport.scaleX.toFloat().coerceIn(0.5f, 2.0f)
                                overlayRot = transformReport.rotationDeg.toFloat().coerceIn(-45f, 45f)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("panto_apply_fit_button")
                        ) { Text("কন্ট্রোল পয়েন্ট থেকে অটো ফিট করুন (${transformType.name})") }
                    }
                }

                // Control Point Solver & Accuracy Report
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("রূপান্তর মডেল ও RMSE ত্রুটি রিপোর্ট", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TransformModelType.entries.forEach { t ->
                                NeuPillChip(text = t.name, selected = transformType == t, onClick = { transformType = t })
                            }
                        }
                        OutlinedTextField(
                            value = controlPointsInput,
                            onValueChange = { controlPointsInput = it },
                            label = { Text("কমন কন্ট্রোল পয়েন্ট (Label: x1,y1 -> x2,y2)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Scale X/Y: %.4f / %.4f | Rotation: %.2f° | RMSE ত্রুটি: %.3f px (সর্বোচ্চ: %.3f px)",
                                transformReport.scaleX,
                                transformReport.scaleY,
                                transformReport.rotationDeg,
                                transformReport.rmseErrorPx,
                                transformReport.maxResidualPx
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        transformReport.controlPoints.forEach { cp ->
                            Text(
                                text = String.format(Locale.US, "• %s: Base(%.1f, %.1f) <-> Overlay(%.1f, %.1f) | Residual: %.3f px", cp.label, cp.baseX, cp.baseY, cp.overlayX, cp.overlayY, cp.residualErrorPx),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        OutlinedTextField(
                            value = dagMatchNotes,
                            onValueChange = { dagMatchNotes = it },
                            label = { Text("সাবেক ও হাল দাগ ম্যাচিং ও এলাকা পরিবর্তন নোট") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MapComparePane(
    bitmap: ImageBitmap?,
    placeholderLabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = placeholderLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "$placeholderLabel\n(গ্যালারি থেকে ম্যাপ বাছুন)",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
