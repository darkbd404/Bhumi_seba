package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.engine.DagPolygonInput
import com.example.engine.PantographEngine
import com.example.model.CadastralSurveyStage
import com.example.model.ControlPointPair
import com.example.model.PantographTransformVersion
import com.example.model.TransformModelType
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PantographScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseStage by remember { mutableStateOf(CadastralSurveyStage.CS) }
    var overlayStage by remember { mutableStateOf(CadastralSurveyStage.RS) }
    var transformType by remember { mutableStateOf(TransformModelType.AFFINE) }
    var compareMode by remember { mutableStateOf(0) } // 0=Overlay, 1=SideBySide, 2=Swipe
    var swipeSplitRatio by remember { mutableFloatStateOf(0.5f) }
    var overlayOpacity by remember { mutableFloatStateOf(0.52f) }
    var overlayScale by remember { mutableFloatStateOf(1.0f) }
    var overlayRot by remember { mutableFloatStateOf(0f) }
    var overlayShiftX by remember { mutableFloatStateOf(0f) }
    var blinkMode by remember { mutableStateOf(false) }
    var blinkShowOverlay by remember { mutableStateOf(true) }
    var lockControlPoints by remember { mutableStateOf(false) }
    var tapTargetMode by remember { mutableStateOf(0) } // 0=Off, 1=Pick Base, 2=Pick Overlay
    var pendingBaseTap by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var baseBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var overlayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // 2.1 Scale Pantograph inputs (e.g., 1:3960 -> 1:1980)
    var srcScaleDenom by remember { mutableStateOf("3960") }
    var tgtScaleDenom by remember { mutableStateOf("1980") }

    var controlPointsInput by remember {
        mutableStateOf(
            "CP1: 100,100 -> 104,98\nCP2: 500,100 -> 512,102\nCP3: 500,450 -> 510,460\nCP4: 100,450 -> 102,458\nCP5: 300,275 -> 307,279"
        )
    }

    // 2.2 Dag Polygon Transfer & Overlap Table inputs (Dag: x1,y1;x2,y2;...)
    var oldDagPolyInput by remember {
        mutableStateOf("450: 150,150; 360,150; 360,340; 150,340\n451: 360,150; 480,150; 480,340; 360,340")
    }
    var newDagPolyInput by remember {
        mutableStateOf("912: 140,145; 300,145; 300,345; 140,345\n913: 300,145; 495,145; 495,345; 300,345")
    }
    var sqFtPerUnitSqInput by remember { mutableStateOf("1.5") }

    var transformHistory by remember { mutableStateOf<List<PantographTransformVersion>>(emptyList()) }
    var toast by remember { mutableStateOf<String?>(null) }

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

    val oldDags = remember(oldDagPolyInput) { parseDagPolygons(oldDagPolyInput) }
    val newDags = remember(newDagPolyInput) { parseDagPolygons(newDagPolyInput) }

    val solvedModel = remember(baseStage, overlayStage, transformType, parsedPairs) {
        PantographEngine.solveTransformModel(
            baseStage = baseStage,
            overlayStage = overlayStage,
            transformType = transformType,
            pairs = parsedPairs,
            dagMatches = emptyList()
        )
    }
    val transformReport = solvedModel.summary

    val scaleResult = remember(srcScaleDenom, tgtScaleDenom) {
        PantographEngine.computeScalePantograph(
            sourceDenom = srcScaleDenom.toDoubleOrNull() ?: 3960.0,
            targetDenom = tgtScaleDenom.toDoubleOrNull() ?: 1980.0
        )
    }

    val overlapRows = remember(oldDags, newDags, solvedModel, sqFtPerUnitSqInput) {
        val unitSqFt = (sqFtPerUnitSqInput.toDoubleOrNull() ?: 1.5).coerceAtLeast(0.01)
        PantographEngine.computeDagOverlapTable(
            oldDags = oldDags,
            newDags = newDags,
            overlayToBaseTransform = solvedModel.forwardTransform,
            sqFeetPerMapUnitSq = unitSqFt
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
                titleBn = "প্যান্টোগ্রাফ ও নকশা তুলনা (Full Suite)",
                subtitleEn = "Scale 1:X→1:Y • Similarity / Affine / Homography / TPS • Dag Overlap Table",
                helpTitle = "প্যান্টোগ্রাফ পূর্ণ ব্যবহার নির্দেশিকা",
                helpSteps = listOf(
                    "বেস (যেমন CS) ও ওভারলে (যেমন RS/BS) ম্যাপ বাছুন — সাইড-বাই-সাইড, সোয়াইপ বা ব্লিঙ্ক দিয়ে তুলনা করুন।",
                    "ম্যাপে সরাসরি ট্যাপ করে কন্ট্রোল পয়েন্ট যোগ করা যাবে অথবা অটোলিস্ট এডিট করুন।",
                    "ত্রুটিপূর্ণ পয়েন্ট (Outlier) থাকলে 'বাদ দিলে RMSE' পরামর্শ দেখাবে ও দাগ ওভারল্যাপ (শতক) সারণি তৈরি করবে।"
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
                        "প্যান্টোগ্রাফ রূপান্তর, স্কেল এনলার্জ ও দাগ ওভারল্যাপ আনুমানিক বিশ্লেষণের জন্য; মূল সরকারি ম্যাপ অপরিবর্তিত থাকে।"
                    )
                }

                // 2.1 Scale Pantograph (1:X -> 1:Y)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("স্কেল প্যান্টোগ্রাফ (১:X → ১:Y বড়/ছোট)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = srcScaleDenom,
                                onValueChange = { srcScaleDenom = it },
                                label = { Text("উৎস স্কেল ১:") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tgtScaleDenom,
                                onValueChange = { tgtScaleDenom = it },
                                label = { Text("লক্ষ্য স্কেল ১:") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = scaleResult.formulaSummaryBn,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NeuPillChip("16\"→32\" (2x)", srcScaleDenom == "3960" && tgtScaleDenom == "1980") {
                                srcScaleDenom = "3960"; tgtScaleDenom = "1980"
                            }
                            NeuPillChip("16\"→64\" (4x)", srcScaleDenom == "3960" && tgtScaleDenom == "990") {
                                srcScaleDenom = "3960"; tgtScaleDenom = "990"
                            }
                            NeuPillChip("32\"→16\" (0.5x)", srcScaleDenom == "1980" && tgtScaleDenom == "3960") {
                                srcScaleDenom = "1980"; tgtScaleDenom = "3960"
                            }
                        }
                    }
                }

                // Stage & Gallery Selector + Interactive Tap CP Mode
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("তুলনামূলক জরিপ স্তর", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { lockControlPoints = !lockControlPoints },
                                modifier = Modifier.testTag("panto_lock_cp_btn")
                            ) {
                                Icon(
                                    imageVector = if (lockControlPoints) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = "Lock Control Points",
                                    tint = if (lockControlPoints) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                NeuPillChip("বেস: ${st.name}", baseStage == st) { baseStage = st }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CadastralSurveyStage.entries.forEach { st ->
                                NeuPillChip("ওভারলে: ${st.name}", overlayStage == st) { overlayStage = st }
                            }
                        }
                    }
                }

                // Visual Overlay / Side-by-Side / Opacity-Swipe Viewport + Tap CP Picker
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeuPillChip("ওভারলে", compareMode == 0) { compareMode = 0 }
                            NeuPillChip("পাশাপাশি", compareMode == 1) { compareMode = 1 }
                            NeuPillChip("সোয়াইপ (Swipe)", compareMode == 2) { compareMode = 2 }
                            Spacer(Modifier.width(6.dp))
                            Text("ব্লিঙ্ক", style = MaterialTheme.typography.labelMedium)
                            Switch(checked = blinkMode, onCheckedChange = { blinkMode = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NeuPillChip("ম্যাপ প্যান", tapTargetMode == 0) { tapTargetMode = 0 }
                            NeuPillChip("+ বেসে CP ট্যাপ", tapTargetMode == 1 && !lockControlPoints) {
                                if (!lockControlPoints) tapTargetMode = 1 else toast = "CP লক করা আছে"
                            }
                            NeuPillChip("+ ওভারলেতে CP ট্যাপ", tapTargetMode == 2 && !lockControlPoints) {
                                if (!lockControlPoints) tapTargetMode = 2 else toast = "CP লক করা আছে"
                            }
                        }

                        if (compareMode == 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(210.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MapComparePane(baseBitmap, "${baseStage.label} (বেস)", Color(0xFF0F6B54), Modifier.weight(1f))
                                MapComparePane(overlayBitmap, "${overlayStage.label} (নতুন)", Color(0xFF1A73E8), Modifier.weight(1f))
                            }
                        } else {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(235.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF3F6F4))
                                    .pointerInput(tapTargetMode, lockControlPoints, pendingBaseTap) {
                                        detectTapGestures { offset ->
                                            if (lockControlPoints || tapTargetMode == 0) return@detectTapGestures
                                            val rx = (offset.x / size.width.coerceAtLeast(1)) * 600.0
                                            val ry = (offset.y / size.height.coerceAtLeast(1)) * 500.0
                                            if (tapTargetMode == 1) {
                                                pendingBaseTap = rx to ry
                                                tapTargetMode = 2
                                                toast = "বেস পয়েন্ট নেওয়া হয়েছে (${rx.toInt()},${ry.toInt()}) — এখন ওভারলেতে ট্যাপ করুন"
                                            } else if (tapTargetMode == 2) {
                                                val bp = pendingBaseTap ?: (rx to ry)
                                                val nextNum = parsedPairs.size + 1
                                                val line = String.format(
                                                    Locale.US,
                                                    "CP%d: %.1f,%.1f -> %.1f,%.1f",
                                                    nextNum, bp.first, bp.second, rx, ry
                                                )
                                                controlPointsInput = (controlPointsInput.trim() + "\n" + line).trim()
                                                pendingBaseTap = null
                                                tapTargetMode = 1
                                                toast = "CP$nextNum যুক্ত হয়েছে!"
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MapComparePane(baseBitmap, "${baseStage.label} বেস", Color(0xFF0F6B54), Modifier.fillMaxSize())
                                val ovAlpha = if (blinkMode) (if (blinkShowOverlay) overlayOpacity else 0f) else overlayOpacity
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (compareMode == 2) swipeSplitRatio else 1f)
                                        .align(Alignment.CenterStart)
                                        .clip(RoundedCornerShape(0.dp))
                                        .alpha(if (compareMode == 2) 0.92f else ovAlpha)
                                        .graphicsLayer {
                                            scaleX = overlayScale
                                            scaleY = overlayScale
                                            rotationZ = overlayRot
                                            translationX = overlayShiftX
                                        }
                                ) {
                                    MapComparePane(overlayBitmap, "${overlayStage.label} ওভারলে", Color(0xFF1A73E8), Modifier.fillMaxSize())
                                }

                                // Draw transformed Old Dags (green dashed) & New Dags (blue) + Control Points
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sx = size.width / 600f
                                    val sy = size.height / 500f

                                    newDags.forEach { d ->
                                        if (d.vertices.size >= 3) {
                                            val p = Path()
                                            p.moveTo(d.vertices[0].first.toFloat() * sx, d.vertices[0].second.toFloat() * sy)
                                            d.vertices.drop(1).forEach { v -> p.lineTo(v.first.toFloat() * sx, v.second.toFloat() * sy) }
                                            p.close()
                                            drawPath(p, Color(0xFF1565C0), style = Stroke(width = 3f))
                                        }
                                    }

                                    oldDags.forEach { d ->
                                        val tr = d.vertices.map { solvedModel.forwardTransform(it.first, it.second) }
                                        if (tr.size >= 3) {
                                            val p = Path()
                                            p.moveTo(tr[0].first.toFloat() * sx, tr[0].second.toFloat() * sy)
                                            tr.drop(1).forEach { v -> p.lineTo(v.first.toFloat() * sx, v.second.toFloat() * sy) }
                                            p.close()
                                            drawPath(
                                                p,
                                                Color(0xFF1B5E20),
                                                style = Stroke(width = 3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                                            )
                                        }
                                    }

                                    transformReport.controlPoints.forEach { cp ->
                                        val cx = (cp.baseX.toFloat() * sx).coerceIn(6f, size.width - 6f)
                                        val cy = (cp.baseY.toFloat() * sy).coerceIn(6f, size.height - 6f)
                                        val isOutlier = solvedModel.outlierSuggestions.any { it.pointId == cp.id }
                                        drawCircle(
                                            color = if (isOutlier) Color(0xFFD32F2F) else Color(0xFF00897B),
                                            radius = 7f,
                                            center = Offset(cx, cy)
                                        )
                                    }
                                }
                            }
                        }

                        if (compareMode == 2) {
                            Text(String.format(Locale.US, "সোয়াইপ বিভাজন: %.0f%%", swipeSplitRatio * 100f), style = MaterialTheme.typography.labelSmall)
                            Slider(value = swipeSplitRatio, onValueChange = { swipeSplitRatio = it }, valueRange = 0.05f..0.95f)
                        }

                        Text(
                            String.format(Locale.US, "স্বচ্ছতা: %.0f%% | স্কেল: %.2fx | ঘূর্ণন: %.1f°", overlayOpacity * 100f, overlayScale, overlayRot),
                            style = MaterialTheme.typography.labelSmall
                        )
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

                // Control Point Solver + Outlier Detection & What-If RMSE
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("রূপান্তর মডেল, Outlier ও RMSE বিশ্লেষণ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TransformModelType.entries.forEach { t ->
                                NeuPillChip(text = t.name, selected = transformType == t) { transformType = t }
                            }
                        }
                        OutlinedTextField(
                            value = controlPointsInput,
                            onValueChange = { if (!lockControlPoints) controlPointsInput = it },
                            enabled = !lockControlPoints,
                            label = { Text("কমন কন্ট্রোল পয়েন্ট (Label: baseX,baseY -> overX,overY)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Scale X/Y: %.4f / %.4f | Rotation: %.2f° | RMSE: %.3f px (Max: %.3f px)",
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

                        // Outlier Warnings & 1-Tap Removal
                        solvedModel.outlierSuggestions.forEach { out ->
                            Surface(
                                color = Color(0xFFD32F2F).copy(alpha = 0.10f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFD32F2F))
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "Outlier %s (%.2f px) বাদ দিলে RMSE %.2f → %.2f px হবে",
                                                out.label, out.residualPx, out.rmseBefore, out.rmseIfRemoved
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB71C1C)
                                        )
                                    }
                                    if (!lockControlPoints) {
                                        IconButton(
                                            onClick = {
                                                val kept = controlPointsInput.lines().filterNot {
                                                    it.trim().startsWith("${out.label}:")
                                                }
                                                controlPointsInput = kept.joinToString("\n")
                                                toast = "${out.label} বাদ দেওয়া হয়েছে"
                                            }
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove Outlier", tint = Color(0xFFD32F2F))
                                        }
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val ver = PantographTransformVersion(
                                        id = "ver_${System.currentTimeMillis()}",
                                        label = "Ver ${transformHistory.size + 1} (${transformType.name}, ${parsedPairs.size} CP)",
                                        timestampMillis = System.currentTimeMillis(),
                                        transformType = transformType,
                                        pairs = transformReport.controlPoints,
                                        rmseErrorPx = transformReport.rmseErrorPx,
                                        scaleX = transformReport.scaleX,
                                        scaleY = transformReport.scaleY,
                                        rotationDeg = transformReport.rotationDeg
                                    )
                                    transformHistory = listOf(ver) + transformHistory
                                    toast = "Transform ভার্সন সেভ হয়েছে"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("ভার্সন সেভ")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val pdf = PantographEngine.exportPantographVintageReportPdf(
                                            context = context,
                                            summary = transformReport,
                                            scaleResult = scaleResult,
                                            overlapRows = overlapRows,
                                            oldDags = oldDags,
                                            newDags = newDags,
                                            overlayToBase = solvedModel.forwardTransform
                                        )
                                        toast = "পুরাতন কাগজে প্যান্টোগ্রাফ রিপোর্ট সেভ: ${pdf.name}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("panto_export_pdf_btn")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("রিপোর্ট PDF")
                            }
                        }

                        if (transformHistory.isNotEmpty()) {
                            Text("ভার্সন ইতিহাস:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            transformHistory.take(4).forEach { ver ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ver.label} • RMSE ${String.format(Locale.US, "%.2f", ver.rmseErrorPx)} px",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            transformType = ver.transformType
                                            controlPointsInput = ver.pairs.joinToString("\n") {
                                                "${it.label}: ${it.baseX},${it.baseY} -> ${it.overlayX},${it.overlayY}"
                                            }
                                            toast = "রিস্টোর হয়েছে: ${ver.label}"
                                        }
                                    ) { Text("ফিরুন") }
                                }
                            }
                        }
                    }
                }

                // 2.2 Polygon Dag Transfer & Overlap Intersection Table
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("দাগ স্থানান্তর ও ছেদ-ক্ষেত্রফল (Overlap Table)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = oldDagPolyInput,
                            onValueChange = { oldDagPolyInput = it },
                            label = { Text("সাবেক (${overlayStage.name}) দাগ পলিগন (Dag: x,y;x,y;...)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newDagPolyInput,
                            onValueChange = { newDagPolyInput = it },
                            label = { Text("হাল (${baseStage.name}) দাগ পলিগন (Dag: x,y;x,y;...)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sqFtPerUnitSqInput,
                            onValueChange = { sqFtPerUnitSqInput = it },
                            label = { Text("প্রতি বর্গ-ইউনিট = কত বর্গফুট (যেমন 1.5)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (overlapRows.isEmpty()) {
                            Text("• কোনো দাগ ওভারল্যাপ পাওয়া যায়নি।", style = MaterialTheme.typography.bodySmall)
                        } else {
                            overlapRows.forEach { row ->
                                Surface(
                                    color = EmeraldPrimary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "সাবেক %s → নতুন %s  |  %.3f শতক  |  সাবেকের %.1f%% | নতুনের %.1f%%",
                                            row.oldDag, row.newDag, row.overlapDecimal, row.overlapPctOfOld, row.overlapPctOfNew
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

private fun parseDagPolygons(raw: String): List<DagPolygonInput> {
    return raw.lines().mapNotNull { line ->
        val parts = line.split(":")
        if (parts.size != 2) return@mapNotNull null
        val dag = parts[0].trim()
        val verts = parts[1].split(";").mapNotNull { pt ->
            val xy = pt.split(",").mapNotNull { it.trim().toDoubleOrNull() }
            if (xy.size >= 2) xy[0] to xy[1] else null
        }
        if (dag.isNotEmpty() && verts.size >= 3) DagPolygonInput(dag, verts) else null
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
