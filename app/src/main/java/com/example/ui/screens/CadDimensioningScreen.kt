package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterCadRepository
import com.example.engine.CadAndDimensionEngine
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.model.CadDimensionEntity
import com.example.model.CadDimensionType
import com.example.model.MeasurementDisplayUnit
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.UUID

/**
 * Dedicated Professional CAD Dimensioning Engine Screen:
 * - Linear Dimensioning Tools (Linear, Horizontal, Vertical, Aligned, Rotated, Baseline, Continue, Chain, Staggered, Ordinate X/Y)
 * - Angular & Arc Dimensioning Tools (Angular, 2-Pt, 3-Pt, Radius, Diameter, Jogged Radius, Arc Length, Center Mark/Line)
 * - Leader & Annotation Tools (Leader, Multileader, Dim Break/Spacing/Alignment/Override/Reassociate/Edit)
 * - Precision Unit Formats & Auto Plot Dimensioning
 */
@Composable
fun CadDimensioningScreen(
    initialCategory: String = "all",
    onNavigateBack: () -> Unit,
    onOpenFullCadCanvas: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { MasterCadRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember(initialCategory) {
        mutableIntStateOf(
            when (initialCategory) {
                "linear" -> 0
                "angular" -> 1
                "leader" -> 2
                else -> 0
            }
        )
    }

    val linearTypes = remember {
        listOf(
            CadDimensionType.LINEAR, CadDimensionType.HORIZONTAL, CadDimensionType.VERTICAL,
            CadDimensionType.ALIGNED, CadDimensionType.ROTATED, CadDimensionType.BASELINE,
            CadDimensionType.CONTINUE, CadDimensionType.QUICK_DIM, CadDimensionType.MULTIPLE_DIM,
            CadDimensionType.CHAIN_DIM, CadDimensionType.STAGGERED_DIM,
            CadDimensionType.ORDINATE_X, CadDimensionType.ORDINATE_Y
        )
    }
    val angularTypes = remember {
        listOf(
            CadDimensionType.ANGULAR, CadDimensionType.TWO_PT_ANGULAR, CadDimensionType.THREE_PT_ANGULAR,
            CadDimensionType.RADIUS, CadDimensionType.DIAMETER, CadDimensionType.JOGGED_RADIUS,
            CadDimensionType.ARC_LENGTH, CadDimensionType.CENTER_MARK, CadDimensionType.CENTERLINE
        )
    }
    val leaderTypes = remember {
        listOf(
            CadDimensionType.LEADER, CadDimensionType.MULTILEADER,
            CadDimensionType.DIM_BREAK, CadDimensionType.DIM_SPACING, CadDimensionType.DIM_ALIGNMENT,
            CadDimensionType.DIM_OVERRIDE, CadDimensionType.DIM_REASSOCIATE, CadDimensionType.DIM_EDIT
        )
    }

    val activePool = when (tab) {
        0 -> linearTypes
        1 -> angularTypes
        2 -> leaderTypes
        else -> CadDimensionType.entries
    }

    var selectedType by remember(tab) { mutableStateOf(activePool.first()) }
    var p1x by remember { mutableStateOf("0") }; var p1y by remember { mutableStateOf("0") }
    var p2x by remember { mutableStateOf("120") }; var p2y by remember { mutableStateOf("0") }
    var p3x by remember { mutableStateOf("120") }; var p3y by remember { mutableStateOf("90") }
    var offsetFt by remember { mutableStateOf("8.0") }
    var rotDeg by remember { mutableStateOf("0.0") }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var leaderNote by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

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
                titleBn = "CAD ডাইমেনশন ইঞ্জিন (Linear / Angular / Leader)",
                subtitleEn = "30 Associative Dimensions • 8 Unit Formats • Auto Plot Dimensioning • DXF & PDF",
                helpTitle = "CAD ডাইমেনশন টুলস গাইড",
                helpSteps = listOf(
                    "Linear ট্যাবে রৈখিক, অনুভূমিক, উল্লম্ব, অ্যালাইনড, বেজলাইন, চেইন ও অর্ডিনেট মাপ যুক্ত করুন।",
                    "Angular ট্যাবে ২-পয়েন্ট/৩-পয়েন্ট কোণ, ব্যাসার্ধ, ব্যাস ও বৃত্তচাপের দৈর্ঘ্য নির্ণয় করুন।",
                    "Leader ট্যাবে লিডার, মাল্টি-লিডার, ডাইমেনশন ব্রেক, স্পেসিং ও ওভাররাইড সম্পাদনা করুন এবং DXF বা PDF সেভ করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "cad_dim_screen_back"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        0 to "রৈখিক (Linear)",
                        1 to "কৌণিক ও ব্যাসার্ধ (Angular)",
                        2 to "লিডার ও এডিট (Leader)",
                        3 to "সকল ৩০টি ও একক (8 Units)"
                    )
                ) { (idx, lbl) ->
                    NeuPillChip(text = lbl, selected = tab == idx, testTag = "dim_cat_tab_$idx", onClick = { tab = idx })
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Visual CAD Plot + Dimensions Viewport
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("লাইভ অ্যাসোসিয়েটিভ ডাইমেনশন ক্যানভাস", fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = onOpenFullCadCanvas) {
                                Text("ড্রয়িং ক্যানভাসে যান")
                            }
                        }
                        Surface(
                            color = Color(0xFFFAFDFC),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(210.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                val allPts = snap.entities.flatMap { it.points } + snap.dimensions.flatMap { listOf(it.p1, it.p2) }
                                val minX = (allPts.minOfOrNull { it.x } ?: 0.0) - 18.0
                                val maxX = (allPts.maxOfOrNull { it.x } ?: 120.0).coerceAtLeast(minX + 20.0) + 18.0
                                val minY = (allPts.minOfOrNull { it.y } ?: 0.0) - 18.0
                                val maxY = (allPts.maxOfOrNull { it.y } ?: 90.0).coerceAtLeast(minY + 20.0) + 18.0
                                val scale = minOf(size.width / (maxX - minX), size.height / (maxY - minY)).toFloat()
                                fun sx(x: Double) = ((x - minX) * scale).toFloat()
                                fun sy(y: Double) = size.height - ((y - minY) * scale).toFloat()

                                snap.entities.forEach { ent ->
                                    if (ent.points.size >= 2) {
                                        val p = Path()
                                        ent.points.forEachIndexed { idx, pt ->
                                            if (idx == 0) p.moveTo(sx(pt.x), sy(pt.y)) else p.lineTo(sx(pt.x), sy(pt.y))
                                        }
                                        if (ent.closed) {
                                            p.close()
                                            drawPath(p, EmeraldPrimary.copy(alpha = 0.12f))
                                        }
                                        drawPath(p, EmeraldPrimary, style = Stroke(3.5f))
                                    }
                                }
                                snap.dimensions.forEach { d ->
                                    val ev = CadAndDimensionEngine.evaluateDimension(d, snap.entities)
                                    val c = when {
                                        d.dimType in angularTypes -> Color(0xFF5E35B1)
                                        d.dimType in leaderTypes -> Color(0xFF1565C0)
                                        else -> Color(0xFFE65100)
                                    }
                                    drawLine(c, Offset(sx(ev.dimLineP1.x), sy(ev.dimLineP1.y)), Offset(sx(ev.dimLineP2.x), sy(ev.dimLineP2.y)), 2.5f)
                                    drawCircle(c, 5f, Offset(sx(ev.textAnchor.x), sy(ev.textAnchor.y)))
                                }
                            }
                        }
                    }
                }

                // Unit switcher & Auto Plot Dim
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("মাপের একক (৮টি ফরম্যাট) ও অটো প্লট ডাইমেনশনিং", fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MeasurementDisplayUnit.entries.forEach { u ->
                                FilterChip(
                                    selected = snap.styleSettings.activeUnit == u,
                                    onClick = {
                                        val updated = snap.dimensions.map { it.copy(unit = u) }
                                        repo.updateCadWorkspace(dimensions = updated, styleSettings = snap.styleSettings.copy(activeUnit = u))
                                        toast = "একক ${u.labelBn}-এ রূপান্তর হয়েছে!"
                                    },
                                    label = { Text(u.labelBn) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val poly = snap.entities.firstOrNull { it.closed && it.points.size >= 3 }?.points
                                        ?: listOf(PolygonVertex(0.0, 0.0), PolygonVertex(120.0, 0.0), PolygonVertex(120.0, 90.0), PolygonVertex(0.0, 90.0))
                                    val autoDims = CadAndDimensionEngine.generateAutoPlotDimensions(
                                        vertices = poly,
                                        unit = snap.styleSettings.activeUnit,
                                        includeDiagonals = true,
                                        includeAngles = true,
                                        includeOrdinates = true
                                    )
                                    repo.updateCadWorkspace(dimensions = autoDims)
                                    toast = "প্লটের সকল বাহু, কর্ণ, কোণ ও অর্ডিনেটের ${autoDims.size}টি ডাইমেনশন তৈরি হয়েছে!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("dim_auto_plot_btn")
                            ) { Text("অটো প্লট ডাইমেনশন") }
                            OutlinedButton(
                                onClick = {
                                    val dxf = CadAndDimensionEngine.exportCadToDxf(snap.projectTitle, snap.entities, snap.dimensions, snap.layers)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "Dimensions_${System.currentTimeMillis()}.dxf", "application/dxf", dxf.toByteArray(Charsets.UTF_8))
                                    toast = "DXF সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(0.6f)
                            ) { Text("DXF") }
                            OutlinedButton(
                                onClick = {
                                    val bmp = CadAndDimensionEngine.renderCadLayoutSheetBitmap(
                                        snap.projectTitle, snap.mouzaInfo, "A4", "16\" = 1 Mile",
                                        snap.entities, snap.dimensions, snap.layers, snap.styleSettings.activeUnit
                                    )
                                    val res = ExportStreamWriter.savePdfBitmap(context, bmp, "CAD_Dimensions_${System.currentTimeMillis()}")
                                    toast = "PDF সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(0.6f)
                            ) { Text("PDF") }
                        }
                    }
                }

                // Active Dimension Type Tool Builder
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (tab) {
                                0 -> "রৈখিক (Linear / Aligned / Ordinate) ডাইমেনশন টুলস"
                                1 -> "কৌণিক, ব্যাসার্ধ ও বৃত্তচাপ (Angular & Curve) টুলস"
                                2 -> "লিডার, মাল্টি-লিডার ও ডাইমেনশন এডিট টুলস"
                                else -> "সকল ৩০টি ডাইমেনশন টুলস"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        activePool.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { dt ->
                                    FilterChip(
                                        selected = selectedType == dt,
                                        onClick = { selectedType = dt },
                                        label = { Text(dt.codeEn, maxLines = 1) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(p1x, { p1x = it }, label = { Text("P1 X") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(p1y, { p1y = it }, label = { Text("P1 Y") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(p2x, { p2x = it }, label = { Text("P2 X") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(p2y, { p2y = it }, label = { Text("P2 Y") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        if (selectedType in angularTypes || selectedType in leaderTypes) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(p3x, { p3x = it }, label = { Text("P3 / Elbow X") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(p3y, { p3y = it }, label = { Text("P3 / Elbow Y") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(offsetFt, { offsetFt = it }, label = { Text("Offset (ft)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(rotDeg, { rotDeg = it }, label = { Text("Rotation (°)") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(prefix, { prefix = it }, label = { Text("Prefix") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(suffix, { suffix = it }, label = { Text("Suffix") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(leaderNote, { leaderNote = it }, label = { Text("Leader / Override") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        }

                        Button(
                            onClick = {
                                val dim = CadDimensionEntity(
                                    id = "dim_${UUID.randomUUID().toString().take(6)}",
                                    dimType = selectedType,
                                    p1 = PolygonVertex(p1x.toDoubleOrNull() ?: 0.0, p1y.toDoubleOrNull() ?: 0.0),
                                    p2 = PolygonVertex(p2x.toDoubleOrNull() ?: 100.0, p2y.toDoubleOrNull() ?: 0.0),
                                    p3 = PolygonVertex(p3x.toDoubleOrNull() ?: 100.0, p3y.toDoubleOrNull() ?: 80.0),
                                    offsetDistanceFt = offsetFt.toDoubleOrNull() ?: 8.0,
                                    rotationDeg = rotDeg.toDoubleOrNull() ?: 0.0,
                                    prefix = prefix,
                                    suffix = suffix,
                                    overrideText = leaderNote,
                                    unit = snap.styleSettings.activeUnit
                                )
                                repo.updateCadWorkspace(dimensions = snap.dimensions + dim)
                                val ev = CadAndDimensionEngine.evaluateDimension(dim, snap.entities)
                                toast = "${selectedType.titleBn} যুক্ত: ${ev.formattedLabel}"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            modifier = Modifier.fillMaxWidth().testTag("dim_add_custom_btn")
                        ) { Text("+ ${selectedType.titleBn} যুক্ত করুন") }
                    }
                }

                items(snap.dimensions, key = { it.id }) { d ->
                    val ev = CadAndDimensionEngine.evaluateDimension(d, snap.entities)
                    Surface(color = LocalNeuPalette.current.surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.dimType.titleBn, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("পরিমাপ: ${ev.formattedLabel}", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(onClick = { repo.updateCadWorkspace(dimensions = snap.dimensions.filterNot { it.id == d.id }) }) {
                                Text("মুছুন")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        BottomSwipeToastBanner(toast, { toast = null }, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}
