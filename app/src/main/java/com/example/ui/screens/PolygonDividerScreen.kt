package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WorkstationRepository
import com.example.engine.LandCalculators
import com.example.engine.LandDivisionProEngine
import com.example.engine.PolygonDividerEngine
import com.example.model.CompassAllocationDirection
import com.example.model.DivisionGeometryCutMode
import com.example.model.DivisionShareSpec
import com.example.model.HoleCutoutPolygon
import com.example.model.LandSplitMode
import com.example.model.ParcelFourBoundaries
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale
import kotlin.math.min

@Composable
fun PolygonDividerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }

    var activeTab by remember { mutableIntStateOf(0) } // 0=AnyShape, 1=QuadMaster, 2=PlotPlanner, 3=Tafsil
    var selectedPresetIdx by remember { mutableIntStateOf(0) }
    var verticesText by remember {
        mutableStateOf(
            PolygonDividerEngine.SAMPLE_POLYGONS[0].second.joinToString("\n") { "${it.x}, ${it.y}" }
        )
    }
    var splitMode by remember { mutableStateOf(LandSplitMode.EQUAL) }
    var cutMode by remember { mutableStateOf(DivisionGeometryCutMode.PARALLEL_LINE) }
    var compassDir by remember { mutableStateOf(CompassAllocationDirection.AUTO) }
    var partCountText by remember { mutableStateOf("3") }
    var customSharesText by remember { mutableStateOf("রহিম:40, করিম:35, সখিনা:25") }
    var fixedFrontageText by remember { mutableStateOf("30, 30, 40") }
    var valueWeightsText by remember { mutableStateOf("1.35, 1.0, 0.9") }
    var roadWidthText by remember { mutableStateOf("8") }
    var cutAngleDeg by remember { mutableFloatStateOf(0f) }
    var divisionTitle by remember { mutableStateOf("নমুনা মৌজা বণ্টন নকশা") }

    val holes = remember { mutableStateListOf<HoleCutoutPolygon>() }
    var showHoleDialog by remember { mutableStateOf(false) }

    // Live Drag-Line Split state
    var dragLineMode by remember { mutableStateOf(false) }
    var dragStartWorld by remember { mutableStateOf<PolygonVertex?>(null) }
    var dragEndWorld by remember { mutableStateOf<PolygonVertex?>(null) }

    // User-editable 4 boundaries per parcel index
    val customBoundaries = remember { mutableStateMapOf<Int, ParcelFourBoundaries>() }
    var toast by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showHoleDialog || activeTab != 0) {
        if (showHoleDialog) showHoleDialog = false else activeTab = 0
    }

    val parsedVertices = remember(verticesText) {
        verticesText.lines().mapNotNull { ln ->
            val parts = ln.split(",", " ", "\t").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                val x = parts[0].toDoubleOrNull()
                val y = parts[1].toDoubleOrNull()
                if (x != null && y != null) PolygonVertex(x, y) else null
            } else null
        }
    }

    val sharesSpec = remember(splitMode, partCountText, customSharesText) {
        if (splitMode == LandSplitMode.EQUAL) {
            val n = (partCountText.toIntOrNull() ?: 2).coerceIn(2, 12)
            (1..n).map { DivisionShareSpec("অংশীদার $it", 1.0) }
        } else {
            customSharesText.split(",").mapIndexedNotNull { idx, raw ->
                val seg = raw.trim()
                if (seg.isBlank()) null
                else if (seg.contains(":")) {
                    val name = seg.substringBefore(":").trim()
                    val num = seg.substringAfter(":").trim().toDoubleOrNull() ?: 0.0
                    DivisionShareSpec(name, num)
                } else {
                    DivisionShareSpec("অংশ ${idx + 1}", seg.toDoubleOrNull() ?: 0.0)
                }
            }
        }
    }

    val parsedFrontages = remember(fixedFrontageText) {
        fixedFrontageText.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    }
    val parsedValueWeights = remember(valueWeightsText) {
        valueWeightsText.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    }

    val legacyResult = remember(parsedVertices, splitMode, sharesSpec, roadWidthText, cutAngleDeg, divisionTitle) {
        PolygonDividerEngine.dividePolygon(
            id = "div_${System.currentTimeMillis()}",
            title = divisionTitle,
            vertices = parsedVertices,
            mode = splitMode,
            shares = sharesSpec,
            roadWidthFt = (roadWidthText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            cutAngleDeg = cutAngleDeg.toDouble()
        )
    }

    val proReport = remember(
        parsedVertices, holes.toList(), cutMode, splitMode, sharesSpec, compassDir,
        parsedValueWeights, roadWidthText, cutAngleDeg, parsedFrontages, divisionTitle
    ) {
        LandDivisionProEngine.dividePolygonPro(
            id = "div_pro_${System.currentTimeMillis()}",
            title = divisionTitle,
            vertices = parsedVertices,
            holes = holes.toList(),
            cutMode = cutMode,
            splitMode = splitMode,
            shares = sharesSpec,
            directions = List(sharesSpec.size) { compassDir },
            valueWeights = parsedValueWeights,
            roadWidthFt = (roadWidthText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            cutAngleDeg = cutAngleDeg.toDouble(),
            fixedFrontagesFt = parsedFrontages
        )
    }

    val dragParcels = remember(dragLineMode, dragStartWorld, dragEndWorld, parsedVertices, holes.toList()) {
        val s = dragStartWorld
        val e = dragEndWorld
        if (dragLineMode && s != null && e != null) {
            LandDivisionProEngine.splitPolygonByLineSegment(parsedVertices, holes.toList(), s, e)
        } else emptyList()
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
                titleBn = "জমি বিভাজন প্রো (Land Division Pro)",
                subtitleEn = "AnyShape & Drag Cut • QuadMaster • Plot Planner A/B/C • Tafsil Partition",
                helpTitle = "জমি বিভাজন প্রো ব্যবহার",
                helpSteps = listOf(
                    "AnyShape ট্যাবে যেকোনো বহুভুজ (ভেতরের পুকুর/ঘর বাদ দিয়ে), ৬টি কাটার পদ্ধতি, রাস্তার মুখ (Frontage) ও লাইভ ড্র্যাগ-লাইন দিয়ে ভাগ করুন।",
                    "QuadMaster ট্যাবে ৪ বাহু ও কর্ণ বা কোণ দিয়ে ঠিক করে সরাসরি সমভাগ ও কর্ণ যাচাই করুন।",
                    "Plot Planner A/B/C ও তফসিল ট্যাবে আবাসিক প্লট দক্ষতা তুলনা এবং বহু-দাগ অংশীদার বণ্টন করুন।"
                ),
                onBack = onBack,
                backTestTag = "divider_back_button"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "AnyShape ও ড্র্যাগ",
                    1 to "QuadMaster (৪ বাহু)",
                    2 to "Plot Planner (A/B/C)",
                    3 to "তফসিল বণ্টন"
                ).forEach { (idx, label) ->
                    NeuPillChip(
                        text = label,
                        selected = activeTab == idx,
                        testTag = "divider_tab_$idx",
                        onClick = { activeTab = idx }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        "জ্যামিতিক বিভাজন গণিত ও ক্ষেত্রফল সংরক্ষণ সূত্র অনুযায়ী করা হয়; সরেজমিন দখল ও পর্চা মিলিয়ে খুঁটি স্থাপন করুন।"
                    )
                }

                when (activeTab) {
                    1 -> item { QuadMasterTabContent(onToast = { toast = it }) }
                    2 -> item { PlotPlannerTabContent(outerVertices = parsedVertices) }
                    3 -> item { TafsilDivisionTabContent() }
                    else -> {
                        // Preset Polygons
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PolygonDividerEngine.SAMPLE_POLYGONS.forEachIndexed { idx, (label, pts) ->
                                    NeuPillChip(
                                        text = label.substringBefore("("),
                                        selected = selectedPresetIdx == idx,
                                        onClick = {
                                            selectedPresetIdx = idx
                                            verticesText = pts.joinToString("\n") { "${it.x}, ${it.y}" }
                                            dragStartWorld = null
                                            dragEndWorld = null
                                        }
                                    )
                                }
                            }
                        }

                        // Visual Canvas Preview (+ Interactive Drag-Line Cut)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neuSurface(cornerRadius = 20.dp, elevation = 6.dp)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (dragLineMode) "ক্যানভাসে আঙুল টেনে লাইভ কাটুন" else "পলিগন বিভাজন প্রিভিউ (পুকুর ও রাস্তা সহ)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("ড্র্যাগ স্প্লিট", style = MaterialTheme.typography.labelSmall)
                                        Switch(
                                            checked = dragLineMode,
                                            onCheckedChange = {
                                                dragLineMode = it
                                                if (!it) {
                                                    dragStartWorld = null
                                                    dragEndWorld = null
                                                }
                                            },
                                            modifier = Modifier.testTag("divider_drag_toggle")
                                        )
                                    }
                                }

                                val displayedParcels = if (dragLineMode && dragParcels.isNotEmpty()) {
                                    dragParcels
                                } else {
                                    proReport?.parcels ?: emptyList()
                                }

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(235.dp)
                                        .background(Color.White, RoundedCornerShape(14.dp))
                                        .pointerInput(dragLineMode, parsedVertices) {
                                            if (!dragLineMode || parsedVertices.size < 3) return@pointerInput
                                            val minX = parsedVertices.minOf { it.x }
                                            val maxX = parsedVertices.maxOf { it.x }
                                            val minY = parsedVertices.minOf { it.y }
                                            val maxY = parsedVertices.maxOf { it.y }
                                            val spanX = (maxX - minX).coerceAtLeast(1.0)
                                            val spanY = (maxY - minY).coerceAtLeast(1.0)
                                            val pad = 28f
                                            val sc = min(
                                                (size.width - pad * 2) / spanX,
                                                (size.height - pad * 2) / spanY
                                            )
                                            fun toWorld(pos: Offset) = PolygonVertex(
                                                minX + (pos.x - pad) / sc,
                                                maxY - (pos.y - pad) / sc
                                            )
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    dragStartWorld = toWorld(start)
                                                    dragEndWorld = toWorld(start)
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    dragEndWorld = toWorld(change.position)
                                                }
                                            )
                                        }
                                ) {
                                    val rep = proReport
                                    val allPts = parsedVertices + (rep?.roadPolygon ?: emptyList()) + holes.flatMap { it.vertices }
                                    if (allPts.size >= 3) {
                                        val minX = allPts.minOf { it.x }; val maxX = allPts.maxOf { it.x }
                                        val minY = allPts.minOf { it.y }; val maxY = allPts.maxOf { it.y }
                                        val spanX = (maxX - minX).coerceAtLeast(1.0)
                                        val spanY = (maxY - minY).coerceAtLeast(1.0)
                                        val pad = 28f
                                        val sc = min((size.width - pad * 2) / spanX, (size.height - pad * 2) / spanY)
                                        fun toPt(v: PolygonVertex) = Offset((pad + (v.x - minX) * sc).toFloat(), (pad + (maxY - v.y) * sc).toFloat())

                                        if (rep != null && rep.roadPolygon.size >= 3 && !dragLineMode) {
                                            val rp = Path().apply {
                                                rep.roadPolygon.forEachIndexed { i, v -> val p = toPt(v); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                                                close()
                                            }
                                            drawPath(rp, Color(0xFFB0BEC5), style = Fill)
                                            drawPath(rp, Color(0xFF455A64), style = Stroke(2f))
                                        }
                                        displayedParcels.forEach { parcel ->
                                            val path = Path().apply {
                                                parcel.vertices.forEachIndexed { i, v -> val p = toPt(v); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                                                close()
                                            }
                                            drawPath(path, Color(parcel.colorArgb).copy(alpha = 0.28f), style = Fill)
                                            drawPath(path, Color(0xFF1B4332), style = Stroke(3f))
                                        }
                                        holes.forEach { h ->
                                            if (h.vertices.size >= 3) {
                                                val hp = Path().apply {
                                                    h.vertices.forEachIndexed { i, v -> val p = toPt(v); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                                                    close()
                                                }
                                                drawPath(hp, Color(0xFF81D4FA).copy(alpha = 0.65f), style = Fill)
                                                drawPath(hp, Color(0xFF0277BD), style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
                                            }
                                        }
                                        if (dragLineMode && dragStartWorld != null && dragEndWorld != null) {
                                            drawLine(Color(0xFFD32F2F), toPt(dragStartWorld!!), toPt(dragEndWorld!!), strokeWidth = 4f)
                                        }
                                    }
                                }
                            }
                        }

                        // Controls
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = divisionTitle,
                                    onValueChange = { divisionTitle = it },
                                    label = { Text("প্রজেক্ট / নকশার শিরোনাম") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("জ্যামিতিক কাটার ধরন (Cut Mode):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DivisionGeometryCutMode.entries.forEach { cm ->
                                        FilterChip(
                                            selected = cutMode == cm,
                                            onClick = { cutMode = cm },
                                            label = { Text(cm.titleBn, maxLines = 1) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LandSplitMode.entries.forEach { mode ->
                                        NeuPillChip(
                                            text = mode.titleBn,
                                            selected = splitMode == mode,
                                            onClick = { splitMode = mode }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompassAllocationDirection.entries.forEach { dir ->
                                        FilterChip(
                                            selected = compassDir == dir,
                                            onClick = { compassDir = dir },
                                            label = { Text(dir.titleBn, maxLines = 1) }
                                        )
                                    }
                                }

                                if (splitMode == LandSplitMode.EQUAL) {
                                    OutlinedTextField(
                                        value = partCountText,
                                        onValueChange = { partCountText = it },
                                        label = { Text("কত ভাগ করবেন? (২ - ১২)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("divider_parts_input")
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = customSharesText,
                                        onValueChange = { customSharesText = it },
                                        label = { Text("মালিক ও মান (নাম:মান কমা দিয়ে)") },
                                        placeholder = { Text("রহিম:40, করিম:35, সখিনা:25") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (cutMode == DivisionGeometryCutMode.FIXED_FRONTAGE) {
                                    OutlinedTextField(
                                        value = fixedFrontageText,
                                        onValueChange = { fixedFrontageText = it },
                                        label = { Text("নির্দিষ্ট রাস্তার মুখ প্রস্থ (ফুট, কমা দিয়ে)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (cutMode == DivisionGeometryCutMode.VALUE_WEIGHTED) {
                                    OutlinedTextField(
                                        value = valueWeightsText,
                                        onValueChange = { valueWeightsText = it },
                                        label = { Text("মূল্য ওজন / সামনের জমির গুণিতক (কমা দিয়ে)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = roadWidthText,
                                        onValueChange = { roadWidthText = it },
                                        label = { Text("রাস্তা (ফুট - না থাকলে ০)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = { showHoleDialog = true }, modifier = Modifier.weight(0.9f)) {
                                        Text("+ পুকুর/ঘর (Hole)")
                                    }
                                }

                                holes.forEach { h ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "বাদযোগ্য: ${h.label} (${String.format(Locale.US, "%.2f", PolygonDividerEngine.polygonAreaSqFt(h.vertices) / 435.6)} শতক)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        IconButton(onClick = { holes.remove(h) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove hole", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Text(String.format(Locale.US, "বিভাজন রেখার কোণ: %.0f°", cutAngleDeg), style = MaterialTheme.typography.labelLarge)
                                Slider(value = cutAngleDeg, onValueChange = { cutAngleDeg = it }, valueRange = 0f..180f)

                                OutlinedTextField(
                                    value = verticesText,
                                    onValueChange = { verticesText = it },
                                    label = { Text("সীমানা স্থানাঙ্ক (X, Y ফুটে — প্রতি লাইনে ১টি কোণ)") },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Results, Frontage Comparison, Editable 4 Boundaries & Exports
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val activeParcels = if (dragLineMode && dragParcels.isNotEmpty()) dragParcels else (proReport?.parcels ?: emptyList())
                                if (proReport == null) {
                                    Text(
                                        text = "পলিগনের সীমানা নিজের ওপর দিয়ে ক্রস করেছে বা কমপক্ষে ৩টি বৈধ বিন্দু নেই।",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        text = if (proReport.isValidConservation) "ক্ষেত্রফল সংরক্ষণ যাচাই: সফল (Verified ±${String.format(Locale.US, "%.3f", proReport.conservationErrorSqFt)} ব.ফু.)"
                                        else "ক্ষেত্রফল যাচাই সতর্কতা",
                                        color = if (proReport.isValidConservation) EmeraldPrimary else Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "মোট: %.2f শতক | পুকুর/হোল বাদ: %.2f শতক | রাস্তা: %.2f শতক | নিট বণ্টন: %.2f শতক | অবশিষ্ট: %.2f শতক",
                                            proReport.grossOuterAreaSqFt / 435.6,
                                            proReport.totalHoleExcludedSqFt / 435.6,
                                            proReport.roadReservedSqFt / 435.6,
                                            proReport.allocatedNetAreaSqFt / 435.6,
                                            proReport.remainingNetAreaSqFt / 435.6
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "রাস্তার মুখ (Frontage) আগে: %.1f' → ভাগ পরবর্তী সম্মিলিত: %.1f'",
                                            proReport.totalRoadFrontageBeforeFt,
                                            proReport.totalRoadFrontageAfterFt
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TealSecondary
                                    )

                                    activeParcels.forEach { p ->
                                        val bnd = customBoundaries[p.index] ?: p.boundaries
                                        Surface(
                                            color = Color(p.colorArgb).copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("${p.index}. ${p.ownerName}", fontWeight = FontWeight.Bold)
                                                    Text(
                                                        String.format(Locale.US, "%.2f শতক (%.2f কাঠা)", p.netDecimal, p.netKatha),
                                                        color = EmeraldPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "বাহু: ${p.edgeLengthsFt.joinToString(", ") { LandCalculators.formatFeetInches(it) }} | মুখ: ${String.format(Locale.US, "%.1f'", p.roadFrontageFt)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    OutlinedTextField(
                                                        value = bnd.north,
                                                        onValueChange = { customBoundaries[p.index] = bnd.copy(north = it) },
                                                        label = { Text("উত্তর") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = bnd.south,
                                                        onValueChange = { customBoundaries[p.index] = bnd.copy(south = it) },
                                                        label = { Text("দক্ষিণ") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    OutlinedTextField(
                                                        value = bnd.east,
                                                        onValueChange = { customBoundaries[p.index] = bnd.copy(east = it) },
                                                        label = { Text("পূর্ব") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = bnd.west,
                                                        onValueChange = { customBoundaries[p.index] = bnd.copy(west = it) },
                                                        label = { Text("পশ্চিম") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val exportReport = proReport.copy(
                                            parcels = activeParcels.map { p ->
                                                p.copy(boundaries = customBoundaries[p.index] ?: p.boundaries)
                                            }
                                        )
                                        Button(
                                            onClick = {
                                                legacyResult?.let { repo.saveDivisionRecord(it) }
                                                val res = LandDivisionProEngine.exportProDivisionReport(context, exportReport, asPdf = false)
                                                toast = "প্রো বণ্টন নকশা PNG সেভ: ${res.displayPath}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier.weight(1f).testTag("divider_export_png")
                                        ) { Text("PNG ও সেভ", maxLines = 1) }

                                        OutlinedButton(
                                            onClick = {
                                                legacyResult?.let { repo.saveDivisionRecord(it) }
                                                val res = LandDivisionProEngine.exportProDivisionReport(context, exportReport, asPdf = true)
                                                toast = "প্রো বণ্টন PDF সেভ: ${res.displayPath}"
                                            },
                                            modifier = Modifier.weight(1f).testTag("divider_export_pdf")
                                        ) { Text("PDF সেভ", maxLines = 1) }
                                    }
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

    if (showHoleDialog) {
        HoleCutoutDialog(
            onDismiss = { showHoleDialog = false },
            onAddHole = {
                holes.add(it)
                showHoleDialog = false
            }
        )
    }
}
