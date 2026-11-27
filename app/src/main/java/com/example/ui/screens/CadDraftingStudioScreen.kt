package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterCadRepository
import com.example.engine.CadAndDimensionEngine
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.SurveyMathProEngine
import com.example.engine.UniversalExportEngine
import com.example.model.CadDimensionEntity
import com.example.model.CadDimensionType
import com.example.model.CadEntity
import com.example.model.CadEntityType
import com.example.model.CadOsnapMode
import com.example.model.MeasurementDisplayUnit
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CadDraftingStudioScreen(
    initialMode: String = "draw",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { MasterCadRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember(initialMode) {
        mutableIntStateOf(
            when (initialMode) {
                "dimensions" -> 1
                "modify" -> 2
                "layers" -> 3
                "layout" -> 4
                else -> 0
            }
        )
    }
    var toast by remember { mutableStateOf<String?>(null) }

    // Drafting state
    var activeDrawType by remember { mutableStateOf(CadEntityType.POLYLINE) }
    var draftPoints by remember { mutableStateOf<List<PolygonVertex>>(emptyList()) }
    var coordXInput by remember { mutableStateOf("140") }
    var coordYInput by remember { mutableStateOf("60") }
    var bearDegInput by remember { mutableStateOf("90") }
    var bearDistInput by remember { mutableStateOf("50") }
    var textLabelInput by remember { mutableStateOf("দাগ সীমানা") }
    var circleRadiusInput by remember { mutableStateOf("25") }

    // Dimension state
    var selectedDimType by remember { mutableStateOf(CadDimensionType.ALIGNED) }
    var dimPrefix by remember { mutableStateOf("") }
    var dimSuffix by remember { mutableStateOf("") }
    var dimOverride by remember { mutableStateOf("") }
    var dimScaleLabel by remember { mutableStateOf("16\" = 1 Mile (1:3,960)") }
    var paperSize by remember { mutableStateOf("A3") }

    // Modify state
    var offsetFtInput by remember { mutableStateOf("5.0") }
    var rotateDegInput by remember { mutableStateOf("15.0") }
    var scaleFactorInput by remember { mutableStateOf("1.10") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1100.dp)) {
            WorkstationTopBar(
                titleBn = "প্রফেশনাল 2D CAD ও ডাইমেনশন ইঞ্জিন",
                subtitleEn = "30 Dimension Types • 15 OSNAP • 16 Layers • Modify/Repair • A0–A4 DXF/PDF",
                helpTitle = "প্রফেশনাল CAD স্টুডিও গাইড",
                helpSteps = listOf(
                    "ক্যানভাসে ট্যাপ করে বা X/Y বা বিয়ারিং-দূরত্ব দিয়ে নির্ভুল লাইন, পলিগন, বৃত্ত ও টেক্সট আঁকুন।",
                    "৩০ ধরনের অ্যাসোসিয়েটিভ ডাইমেনশন এবং ৮ ধরনের মাপের একক (ফুট-ইঞ্চি, দশমিক ফুট, মিটার, সেমি) ব্যবহার করুন।",
                    "১৬টি CAD লেয়ার নিয়ন্ত্রণ করে A0–A4 পেপার স্পেসে DXF, SVG ও PDF প্রিন্ট রপ্তানি করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "cad_engine_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("০১. ড্রয়িং ও OSNAP", tab == 0, testTag = "cad_tab_draw") { tab = 0 } }
                item { NeuPillChip("০২. ৩০টি ডাইমেনশন ও একক", tab == 1, testTag = "cad_tab_dim") { tab = 1 } }
                item { NeuPillChip("০৩. Modify ও মেরামত", tab == 2, testTag = "cad_tab_mod") { tab = 2 } }
                item { NeuPillChip("০৪. ১৬ লেয়ার কন্ট্রোল", tab == 3, testTag = "cad_tab_layers") { tab = 3 } }
                item { NeuPillChip("০৫. A0–A4 লেআউট ও DXF", tab == 4, testTag = "cad_tab_layout") { tab = 4 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive CAD Viewport Canvas visible on Drawing/Dim/Modify tabs
                if (tab <= 2) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ইন্টারেক্টিভ 2D CAD ক্যানভাস (ট্যাপ করে পয়েন্ট যোগ করুন)", fontWeight = FontWeight.Bold)
                                Text("${snap.entities.size} অবজেক্ট • ${snap.dimensions.size} মাপ", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(Color(0xFFFAFDFC), RoundedCornerShape(14.dp))
                                    .pointerInput(activeDrawType, snap.entities, snap.styleSettings) {
                                        detectTapGestures { offset ->
                                            val worldX = ((offset.x / size.width) * 200.0) - 20.0
                                            val worldY = (((size.height - offset.y) / size.height) * 160.0) - 20.0
                                            val match = CadAndDimensionEngine.resolveSnap(
                                                raw = PolygonVertex(worldX, worldY),
                                                lastAnchor = draftPoints.lastOrNull(),
                                                entities = snap.entities,
                                                settings = snap.styleSettings
                                            )
                                            draftPoints = draftPoints + match.snapped
                                            toast = match.activeMode?.let { "স্ন্যাপ: ${it.shortEn} (${String.format(Locale.US, "%.1f, %.1f", match.snapped.x, match.snapped.y)})" }
                                                ?: "পয়েন্ট যুক্ত (${String.format(Locale.US, "%.1f, %.1f", match.snapped.x, match.snapped.y)})"
                                        }
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width; val h = size.height
                                    fun sx(x: Double) = (((x + 20.0) / 200.0) * w).toFloat()
                                    fun sy(y: Double) = (h - (((y + 20.0) / 160.0) * h)).toFloat()

                                    // Grid lines
                                    val step = snap.styleSettings.gridSpacingFt.coerceAtLeast(5.0)
                                    var gx = -20.0
                                    while (gx <= 180.0) {
                                        drawLine(Color(0xFFE0ECE9), Offset(sx(gx), 0f), Offset(sx(gx), h), 1f)
                                        gx += step
                                    }
                                    var gy = -20.0
                                    while (gy <= 140.0) {
                                        drawLine(Color(0xFFE0ECE9), Offset(0f, sy(gy)), Offset(w, sy(gy)), 1f)
                                        gy += step
                                    }

                                    val layerMap = snap.layers.associateBy { it.id }
                                    snap.entities.forEach { ent ->
                                        val layer = layerMap[ent.layerId]
                                        if (layer?.visible == false || layer?.frozen == true) return@forEach
                                        val col = Color(layer?.colorArgb ?: 0xFF0F6B54)
                                        when (ent.type) {
                                            CadEntityType.CIRCLE -> {
                                                val c = ent.points.firstOrNull() ?: return@forEach
                                                val rPx = ((ent.radius / 200.0) * w).toFloat()
                                                drawCircle(col, rPx, Offset(sx(c.x), sy(c.y)), style = Stroke(3f))
                                            }
                                            else -> if (ent.points.size >= 2) {
                                                val path = Path()
                                                ent.points.forEachIndexed { i, pt ->
                                                    if (i == 0) path.moveTo(sx(pt.x), sy(pt.y)) else path.lineTo(sx(pt.x), sy(pt.y))
                                                }
                                                if (ent.closed) {
                                                    path.close()
                                                    drawPath(path, col.copy(alpha = 0.12f))
                                                }
                                                drawPath(path, col, style = Stroke(3.5f))
                                            }
                                        }
                                    }

                                    // Draw live draft polyline
                                    if (draftPoints.isNotEmpty()) {
                                        val dPath = Path()
                                        draftPoints.forEachIndexed { idx, pt ->
                                            val px = sx(pt.x); val py = sy(pt.y)
                                            if (idx == 0) dPath.moveTo(px, py) else dPath.lineTo(px, py)
                                            drawCircle(Color(0xFFD81B60), 6f, Offset(px, py))
                                        }
                                        drawPath(dPath, Color(0xFFD81B60), style = Stroke(3f))
                                    }

                                    // Draw associative dimension lines
                                    snap.dimensions.forEach { dim ->
                                        val ev = CadAndDimensionEngine.evaluateDimension(dim, snap.entities)
                                        drawLine(
                                            Color(0xFFE65100),
                                            Offset(sx(ev.dimLineP1.x), sy(ev.dimLineP1.y)),
                                            Offset(sx(ev.dimLineP2.x), sy(ev.dimLineP2.y)),
                                            2f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (tab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("CAD ড্রয়িং টুলস ও স্থানাঙ্ক ইনপুট", fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        CadEntityType.LINE, CadEntityType.POLYLINE, CadEntityType.POLYGON,
                                        CadEntityType.RECTANGLE, CadEntityType.CIRCLE, CadEntityType.ARC,
                                        CadEntityType.TEXT
                                    ).forEach { et ->
                                        FilterChip(selected = activeDrawType == et, onClick = { activeDrawType = et }, label = { Text(et.titleBn) })
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(coordXInput, { coordXInput = it }, label = { Text("East / X (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(coordYInput, { coordYInput = it }, label = { Text("North / Y (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedButton(onClick = {
                                        val x = coordXInput.toDoubleOrNull() ?: 0.0
                                        val y = coordYInput.toDoubleOrNull() ?: 0.0
                                        draftPoints = draftPoints + PolygonVertex(x, y)
                                    }) { Text("+ X/Y") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(bearDegInput, { bearDegInput = it }, label = { Text("বিয়ারিং (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(bearDistInput, { bearDistInput = it }, label = { Text("দূরত্ব (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedButton(onClick = {
                                        val base = draftPoints.lastOrNull() ?: PolygonVertex(0.0, 0.0)
                                        val az = (bearDegInput.toDoubleOrNull() ?: 0.0) * Math.PI / 180.0
                                        val dist = bearDistInput.toDoubleOrNull() ?: 20.0
                                        draftPoints = draftPoints + PolygonVertex(base.x + dist * sin(az), base.y + dist * cos(az))
                                    }) { Text("+ Bearing") }
                                }

                                if (activeDrawType == CadEntityType.CIRCLE || activeDrawType == CadEntityType.TEXT) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(circleRadiusInput, { circleRadiusInput = it }, label = { Text("ব্যাসার্ধ (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                        OutlinedTextField(textLabelInput, { textLabelInput = it }, label = { Text("টেক্সট / লেবেল") }, modifier = Modifier.weight(1f), singleLine = true)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            if (draftPoints.isEmpty()) {
                                                toast = "প্রথমে ক্যানভাস বা X/Y দিয়ে পয়েন্ট দিন"
                                                return@Button
                                            }
                                            val newEnt = CadEntity(
                                                id = "ent_${UUID.randomUUID().toString().take(6)}",
                                                type = activeDrawType,
                                                points = draftPoints,
                                                radius = circleRadiusInput.toDoubleOrNull() ?: 20.0,
                                                closed = activeDrawType == CadEntityType.POLYGON || activeDrawType == CadEntityType.RECTANGLE,
                                                text = textLabelInput
                                            )
                                            repo.updateCadWorkspace(entities = snap.entities + newEnt)
                                            draftPoints = emptyList()
                                            toast = "${activeDrawType.titleBn} CAD-এ যুক্ত হয়েছে!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("cad_commit_entity_btn")
                                    ) { Text("অবজেক্ট যুক্ত করুন") }

                                    OutlinedButton(onClick = { draftPoints = emptyList() }) { Text("ড্রাফট পরিষ্কার") }
                                }
                            }
                        }

                        // 15 OSNAP & Precision Modes
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("১৫টি OSNAP ও প্রিসিশন মোড", fontWeight = FontWeight.Bold)
                                CadOsnapMode.entries.chunked(3).forEach { rowModes ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowModes.forEach { mode ->
                                            val on = mode in snap.styleSettings.enabledOsnaps
                                            FilterChip(
                                                selected = on,
                                                onClick = {
                                                    val next = if (on) snap.styleSettings.enabledOsnaps - mode else snap.styleSettings.enabledOsnaps + mode
                                                    repo.updateCadWorkspace(styleSettings = snap.styleSettings.copy(enabledOsnaps = next))
                                                },
                                                label = { Text(mode.shortEn, maxLines = 1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(3 - rowModes.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Module 01.1 & Module 02: 30 Dimensions + 8 Measurement Units + Auto Plot Dimensioning
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("পরিমাপ একক ও ফরম্যাট সিস্টেম (Module 02)", fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MeasurementDisplayUnit.entries.forEach { u ->
                                        FilterChip(
                                            selected = snap.styleSettings.activeUnit == u,
                                            onClick = {
                                                val updatedDims = snap.dimensions.map { it.copy(unit = u) }
                                                repo.updateCadWorkspace(dimensions = updatedDims, styleSettings = snap.styleSettings.copy(activeUnit = u))
                                                toast = "সকল ডাইমেনশন ${u.labelBn}-এ আপডেট হয়েছে"
                                            },
                                            label = { Text(u.labelBn) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val mainPoly = snap.entities.firstOrNull { it.points.size >= 3 }?.points ?: return@Button
                                        val autoDims = CadAndDimensionEngine.generateAutoPlotDimensions(
                                            vertices = mainPoly,
                                            unit = snap.styleSettings.activeUnit,
                                            includeDiagonals = true,
                                            includeAngles = true
                                        )
                                        repo.updateCadWorkspace(dimensions = autoDims)
                                        toast = "প্লটের সকল বাহু, কর্ণ ও কোণের ${autoDims.size}টি অটো-ডাইমেনশন তৈরি হয়েছে!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("cad_auto_dim_btn")
                                ) { Text("অটো প্লট ডাইমেনশনিং (বাহু + কর্ণ + কোণ)") }

                                Text("৩০ ধরনের অ্যাসোসিয়েটিভ ডাইমেনশন টুলস (Module 01.1)", fontWeight = FontWeight.Bold)
                                CadDimensionType.entries.chunked(3).forEach { rowDims ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowDims.forEach { dt ->
                                            FilterChip(
                                                selected = selectedDimType == dt,
                                                onClick = { selectedDimType = dt },
                                                label = { Text(dt.codeEn, maxLines = 1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(3 - rowDims.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(dimPrefix, { dimPrefix = it }, label = { Text("Prefix") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(dimSuffix, { dimSuffix = it }, label = { Text("Suffix") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(dimOverride, { dimOverride = it }, label = { Text("Override Text") }, modifier = Modifier.weight(1f), singleLine = true)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val pts = snap.entities.firstOrNull { it.points.size >= 2 }?.points
                                            ?: listOf(PolygonVertex(0.0, 0.0), PolygonVertex(100.0, 0.0), PolygonVertex(100.0, 80.0))
                                        val newDim = CadDimensionEntity(
                                            id = "dim_${UUID.randomUUID().toString().take(6)}",
                                            dimType = selectedDimType,
                                            p1 = pts[0],
                                            p2 = pts[1],
                                            p3 = pts.getOrNull(2),
                                            prefix = dimPrefix,
                                            suffix = dimSuffix,
                                            overrideText = dimOverride,
                                            unit = snap.styleSettings.activeUnit
                                        )
                                        repo.updateCadWorkspace(dimensions = snap.dimensions + newDim)
                                        toast = "${selectedDimType.titleBn} ডাইমেনশন যুক্ত হয়েছে"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("+ নির্বাচিত ডাইমেনশন যুক্ত করুন (${selectedDimType.titleBn})") }
                            }
                        }

                        items(snap.dimensions, key = { it.id }) { d ->
                            val ev = CadAndDimensionEngine.evaluateDimension(d, snap.entities)
                            Surface(color = Color.White.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(d.dimType.titleBn, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        Text("মান: ${ev.formattedLabel}", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(onClick = { repo.updateCadWorkspace(dimensions = snap.dimensions.filterNot { it.id == d.id }) }) {
                                        Text("মুছুন")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Module 01.3 & 01.4 Modify & Topology Repair
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("CAD Modify ও জ্যামিতি মেরামত টুলস", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(offsetFtInput, { offsetFtInput = it }, label = { Text("Offset (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(rotateDegInput, { rotateDegInput = it }, label = { Text("Rotate (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(scaleFactorInput, { scaleFactorInput = it }, label = { Text("Scale (x)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = {
                                        val first = snap.entities.firstOrNull { it.points.size >= 3 } ?: return@OutlinedButton
                                        val offPts = CadAndDimensionEngine.offsetPolygon(first.points, offsetFtInput.toDoubleOrNull() ?: 5.0)
                                        repo.updateCadWorkspace(entities = snap.entities + first.copy(id = "off_${System.nanoTime()}", layerId = "layer_proposed_bdy", points = offPts))
                                        toast = "Offset পলিগন তৈরি হয়েছে"
                                    }, modifier = Modifier.weight(1f)) { Text("Offset") }

                                    OutlinedButton(onClick = {
                                        val first = snap.entities.firstOrNull { it.points.size >= 2 } ?: return@OutlinedButton
                                        val rotPts = CadAndDimensionEngine.rotateVertices(first.points, rotateDegInput.toDoubleOrNull() ?: 15.0)
                                        repo.updateCadWorkspace(entities = snap.entities.map { if (it.id == first.id) it.copy(points = rotPts) else it })
                                        toast = "অবজেক্ট Rotate হয়েছে"
                                    }, modifier = Modifier.weight(1f)) { Text("Rotate") }

                                    OutlinedButton(onClick = {
                                        val first = snap.entities.firstOrNull { it.points.size >= 2 } ?: return@OutlinedButton
                                        val scPts = CadAndDimensionEngine.scaleVertices(first.points, scaleFactorInput.toDoubleOrNull() ?: 1.1)
                                        repo.updateCadWorkspace(entities = snap.entities.map { if (it.id == first.id) it.copy(points = scPts) else it })
                                        toast = "অবজেক্ট Scale হয়েছে"
                                    }, modifier = Modifier.weight(1f)) { Text("Scale") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = {
                                        val first = snap.entities.firstOrNull { it.points.size >= 2 } ?: return@OutlinedButton
                                        val mirPts = CadAndDimensionEngine.mirrorVertices(first.points, horizontalAxis = false)
                                        repo.updateCadWorkspace(entities = snap.entities + first.copy(id = "mir_${System.nanoTime()}", points = mirPts))
                                        toast = "Mirror কপি তৈরি হয়েছে"
                                    }, modifier = Modifier.weight(1f)) { Text("Mirror") }

                                    Button(
                                        onClick = {
                                            val rep = CadAndDimensionEngine.analyzeAndCleanTopology(snap.entities)
                                            repo.updateCadWorkspace(entities = rep.cleanedEntities)
                                            toast = "টপোলজি যাচাই: ডুপ্লিকেট ${rep.duplicateEntityCount}, সেলফ-ইন্টারসেকশন ${rep.selfIntersections}, গ্যাপ ${rep.openGapsCount}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                        modifier = Modifier.weight(1f).testTag("cad_repair_topology_btn")
                                    ) { Text("টপোলজি ক্লিনআপ ও যাচাই") }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Module 04: 16-Layer Manager
                        items(snap.layers, key = { it.id }) { layer ->
                            Surface(color = Color.White.copy(alpha = 0.85f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.width(18.dp).height(18.dp).background(Color(layer.colorArgb), RoundedCornerShape(4.dp)))
                                        Column {
                                            Text(layer.nameBn, fontWeight = FontWeight.Bold)
                                            Text("DXF: ${layer.dxfName}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("দৃশ্যমান", style = MaterialTheme.typography.labelSmall)
                                        Checkbox(checked = layer.visible, onCheckedChange = { v ->
                                            repo.updateCadWorkspace(layers = snap.layers.map { if (it.id == layer.id) it.copy(visible = v) else it })
                                        })
                                        Text("লক", style = MaterialTheme.typography.labelSmall)
                                        Switch(checked = layer.locked, onCheckedChange = { l ->
                                            repo.updateCadWorkspace(layers = snap.layers.map { if (it.id == layer.id) it.copy(locked = l) else it })
                                        })
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // Module 04 Paper-Space A0–A4 Title Block + DXF/SVG/PDF Export
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Paper Space A0–A4 টাইটেল ব্লক ও রপ্তানি", fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("A4", "A3", "A2", "A1", "A0").forEach { sz ->
                                        FilterChip(selected = paperSize == sz, onClick = { paperSize = sz }, label = { Text(sz) })
                                    }
                                }
                                OutlinedTextField(dimScaleLabel, { dimScaleLabel = it }, label = { Text("স্কেল লেবেল") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val bmp = CadAndDimensionEngine.renderCadLayoutSheetBitmap(
                                                title = snap.projectTitle,
                                                mouzaInfo = snap.mouzaInfo,
                                                paperSize = paperSize,
                                                scaleLabel = dimScaleLabel,
                                                entities = snap.entities,
                                                dimensions = snap.dimensions,
                                                layers = snap.layers,
                                                unit = snap.styleSettings.activeUnit
                                            )
                                            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "CAD_${paperSize}_${System.currentTimeMillis()}")
                                            UniversalExportEngine.printBitmapDocument(context, "CAD_Layout_$paperSize", copy)
                                            toast = "$paperSize PDF ও প্রিন্ট প্রস্তুত: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("cad_export_pdf_btn")
                                    ) { Text("PDF ও প্রিন্ট") }

                                    OutlinedButton(
                                        onClick = {
                                            val dxf = CadAndDimensionEngine.exportCadToDxf(snap.projectTitle, snap.entities, snap.dimensions, snap.layers)
                                            val res = DocumentAndReportEngine.saveBinaryDocument(context, "bhumi_cad_${System.currentTimeMillis()}.dxf", "application/dxf", dxf.toByteArray(Charsets.UTF_8))
                                            toast = "AutoCAD DXF সেভ: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f).testTag("cad_export_dxf_btn")
                                    ) { Text(".DXF") }

                                    OutlinedButton(
                                        onClick = {
                                            val svg = CadAndDimensionEngine.exportCadToSvg(snap.projectTitle, snap.entities, snap.dimensions, snap.layers)
                                            val res = DocumentAndReportEngine.saveBinaryDocument(context, "bhumi_cad_${System.currentTimeMillis()}.svg", "image/svg+xml", svg.toByteArray(Charsets.UTF_8))
                                            toast = "SVG ভেক্টর সেভ: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f).testTag("cad_export_svg_btn")
                                    ) { Text(".SVG") }
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
