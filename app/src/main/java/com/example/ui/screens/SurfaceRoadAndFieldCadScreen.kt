package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.engine.CogoAndSurfaceEngine
import com.example.engine.DocumentAndReportEngine
import com.example.engine.FieldIoQualityAndDeedEngine
import com.example.engine.UniversalExportEngine
import com.example.model.CadEntityType
import com.example.model.CogoPoint
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun SurfaceRoadAndFieldCadScreen(
    initialMode: String = "field",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { MasterCadRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember(initialMode) {
        mutableIntStateOf(
            when (initialMode) {
                "surface" -> 1
                "road" -> 2
                else -> 0
            }
        )
    }
    var toast by remember { mutableStateOf<String?>(null) }

    // Tab 0: Field-to-CAD Importer
    var selectedFormat by remember { mutableStateOf<FieldIoQualityAndDeedEngine.PointFileFormat?>(null) }
    var importRawText by remember {
        mutableStateOf(
            "1,0.0,0.0,12.4,BDY\n2,0.0,120.0,12.8,BDY\n3,90.0,120.0,13.9,BDY\n4,90.0,0.0,13.1,BDY\n5,45.0,60.0,13.6,SPOT"
        )
    }
    var importResult by remember { mutableStateOf<FieldIoQualityAndDeedEngine.FieldImportResult?>(null) }

    val fileOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.onSuccess { text ->
                if (!text.isNullOrBlank()) {
                    importRawText = text
                    importResult = FieldIoQualityAndDeedEngine.importFieldOrVectorText(text, selectedFormat)
                    toast = importResult?.summaryNote ?: "ফাইল লোড সম্পন্ন"
                }
            }.onFailure {
                toast = "ফাইল পড়া যায়নি: ${it.message}"
            }
        }
    }

    // Tab 1: Surface TIN & Contours
    var contourInterval by remember { mutableStateOf(0.5) }
    var designElevationInput by remember { mutableStateOf("13.2") }

    // Tab 2: Road Alignment
    var roadSpec by remember(snap.roadSpec) { mutableStateOf(snap.roadSpec) }
    var roadRowInput by remember(snap.roadSpec) { mutableStateOf(snap.roadSpec.rightOfWayWidthFt.toString()) }

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
                titleBn = "ফিল্ড-টু-CAD, কন্টুর TIN ও রাস্তা জরিপ",
                subtitleEn = "Field PNEZD/LandXML/NMEA/DXF/GIS • Surface TIN Contours Cut/Fill • Road Station & Plot Access",
                helpTitle = "ফিল্ড সার্ভে, কন্টুর ও রাস্তা ইঞ্জিন গাইড",
                helpSteps = listOf(
                    "ফিল্ড-টু-CAD ট্যাবে CSV, PNEZD, PENZD, GPX, NMEA, LandXML, DXF, SVG, GeoJSON বা KML ফাইল আমদানি করুন—কোড অনুযায়ী অটো CAD লাইন তৈরি হবে।",
                    "কন্টুর ও TIN ট্যাবে স্পট হাইট পয়েন্ট থেকে ত্রিভুজায়ন (TIN), কন্টুর রেখা, ঢাল % এবং ডিজাইন লেভেল অনুযায়ী কাটিং/ভরাট ঘনফুট হিসাব করুন।",
                    "রাস্তা ও প্রবেশপথ ট্যাবে প্রস্তাবিত রাস্তার স্টেশন+অফসেট এবং প্লটের সাথে সংযোগ যাচাই করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "surface_road_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(0 to "ফিল্ড-টু-CAD আমদানি", 1 to "TIN কন্টুর ও কাটিং/ভরাট", 2 to "রাস্তা অ্যালাইনমেন্ট ও প্রবেশপথ")) { (idx, lbl) ->
                    NeuPillChip(text = lbl, selected = tab == idx, testTag = "srf_tab_$idx", onClick = { tab = idx })
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    // ---------- Tab 0: Field-to-CAD ----------
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("১১টি ফরম্যাট থেকে অটো ফিল্ড-টু-CAD রূপান্তর", fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(selected = selectedFormat == null, onClick = { selectedFormat = null }, label = { Text("Auto Detect") })
                                    FieldIoQualityAndDeedEngine.PointFileFormat.entries.forEach { fmt ->
                                        FilterChip(selected = selectedFormat == fmt, onClick = { selectedFormat = fmt }, label = { Text(fmt.name) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { fileOpenLauncher.launch(arrayOf("*/*")) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ডিভাইস থেকে ফাইল খুলুন") }
                                    OutlinedButton(
                                        onClick = {
                                            importRawText = "<LandXML><CgPoints><CgPoint name=\"BDY1\">0 0 12.1</CgPoint><CgPoint name=\"BDY2\">0 100 12.5</CgPoint><CgPoint name=\"BDY3\">80 100 13.0</CgPoint></CgPoints></LandXML>"
                                            selectedFormat = FieldIoQualityAndDeedEngine.PointFileFormat.LANDXML
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("নমুনা LandXML") }
                                }
                                OutlinedTextField(
                                    value = importRawText,
                                    onValueChange = { importRawText = it },
                                    label = { Text("ফিল্ড ডাটা (CSV / PNEZD / GPX / NMEA / LandXML / DXF)") },
                                    minLines = 4,
                                    modifier = Modifier.fillMaxWidth().testTag("field_import_input")
                                )
                                Button(
                                    onClick = {
                                        val res = FieldIoQualityAndDeedEngine.importFieldOrVectorText(importRawText, selectedFormat)
                                        importResult = res
                                        toast = res.summaryNote
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("run_field_import_btn")
                                ) { Text("পার্স ও অটো লাইনওয়ার্ক তৈরি করুন") }

                                importResult?.let { res ->
                                    Surface(color = EmeraldPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(res.summaryNote, fontWeight = FontWeight.Bold)
                                            res.points.take(6).forEach { p ->
                                                Text("Pt ${p.pointNumber} [${p.code}]: N=${String.format(Locale.US, "%.2f", p.northFt)}, E=${String.format(Locale.US, "%.2f", p.eastFt)}, Z=${String.format(Locale.US, "%.2f", p.elevationFt)}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        val mergedPts = snap.cogoPoints + res.points
                                                        val mergedEnts = snap.entities + res.entities
                                                        repo.updateCogoPoints(mergedPts)
                                                        repo.updateCadWorkspace(entities = mergedEnts)
                                                        toast = "${res.points.size}টি পয়েন্ট ও ${res.entities.size}টি CAD এন্টিটি মাস্টার প্রজেক্টে যুক্ত!"
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("মাস্টার CAD-এ পাঠান") }
                                                OutlinedButton(
                                                    onClick = {
                                                        val rows = res.points.map { listOf(it.pointNumber.toString(), it.northFt.toString(), it.eastFt.toString(), it.elevationFt.toString(), it.code) }
                                                        val xlsx = UniversalExportEngine.buildXlsxBytes("ImportedPoints", listOf("Pt", "North", "East", "Elev", "Code"), rows)
                                                        val saved = DocumentAndReportEngine.saveBinaryDocument(context, "FieldPoints_${System.currentTimeMillis()}.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx)
                                                        toast = "XLSX সেভ: ${saved.displayPath}"
                                                    },
                                                    modifier = Modifier.weight(0.75f)
                                                ) { Text("XLSX") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------- Tab 1: Surface TIN, Contours & Cut/Fill ----------
                    1 -> {
                        item {
                            val tin = remember(snap.cogoPoints) { CogoAndSurfaceEngine.buildTinSurface(snap.cogoPoints) }
                            val contours = remember(tin, contourInterval) { CogoAndSurfaceEngine.generateContours(tin, contourInterval, majorEvery = 2) }
                            val designEl = designElevationInput.toDoubleOrNull() ?: 13.0
                            val volSummary = remember(tin, designEl) {
                                CogoAndSurfaceEngine.computeSurfaceCutFillAgainstDesignElevation(tin, designEl)
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("সার্ফেস TIN ত্রিভুজায়ন ও কন্টুর ম্যাপ (${snap.cogoPoints.size} পয়েন্ট)", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0.5, 1.0, 2.0, 5.0).forEach { iv ->
                                        FilterChip(selected = contourInterval == iv, onClick = { contourInterval = iv }, label = { Text("${iv} ft") })
                                    }
                                }

                                Surface(
                                    color = Color(0xFFFAF8F2),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(260.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                        val pts = snap.cogoPoints
                                        if (pts.size < 3) return@Canvas
                                        val minE = pts.minOf { it.eastFt } - 10.0
                                        val maxE = (pts.maxOf { it.eastFt } + 10.0).coerceAtLeast(minE + 10.0)
                                        val minN = pts.minOf { it.northFt } - 10.0
                                        val maxN = (pts.maxOf { it.northFt } + 10.0).coerceAtLeast(minN + 10.0)
                                        val scale = minOf(size.width / (maxE - minE), size.height / (maxN - minN)).toFloat()
                                        fun sx(e: Double) = ((e - minE) * scale).toFloat()
                                        fun sy(n: Double) = size.height - ((n - minN) * scale).toFloat()

                                        tin.forEach { t ->
                                            val path = Path().apply {
                                                moveTo(sx(t.p1.eastFt), sy(t.p1.northFt))
                                                lineTo(sx(t.p2.eastFt), sy(t.p2.northFt))
                                                lineTo(sx(t.p3.eastFt), sy(t.p3.northFt))
                                                close()
                                            }
                                            drawPath(path, Color(0x4490A4AE), style = Stroke(width = 1.5f))
                                        }
                                        contours.forEach { seg ->
                                            drawLine(
                                                color = if (seg.isMajor) Color(0xFF8D3B14) else Color(0xFFD49A6A),
                                                start = Offset(sx(seg.start.x), sy(seg.start.y)),
                                                end = Offset(sx(seg.end.x), sy(seg.end.y)),
                                                strokeWidth = if (seg.isMajor) 3f else 1.5f
                                            )
                                        }
                                        pts.forEach { p ->
                                            drawCircle(EmeraldPrimary, radius = 6f, center = Offset(sx(p.eastFt), sy(p.northFt)))
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = designElevationInput,
                                    onValueChange = { designElevationInput = it },
                                    label = { Text("প্রস্তাবিত ডিজাইন উচ্চতা / RL (ফুট)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Surface(color = TealSecondary.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("TIN ত্রিভুজ: ${tin.size}টি | কন্টুর সেগমেন্ট: ${contours.size}টি", fontWeight = FontWeight.Bold)
                                        Text("গড় ঢাল (Avg Slope): ${String.format(Locale.US, "%.2f%%", volSummary.avgSlopePct)}")
                                        Text("মোট কাটিং (Cut Volume): ${String.format(Locale.US, "%.1f", volSummary.cutVolumeCuFt)} ঘনফুট")
                                        Text("মোট ভরাট (Fill Volume): ${String.format(Locale.US, "%.1f", volSummary.fillVolumeCuFt)} ঘনফুট")
                                        Text("নিট মাটি (Net +Fill/-Cut): ${String.format(Locale.US, "%+.1f", volSummary.netVolumeCuFt)} ঘনফুট", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // ---------- Tab 2: Road Alignment, Station/Offset & Plot Access ----------
                    2 -> {
                        item {
                            val activePoly = snap.entities.firstOrNull { it.closed && it.points.size >= 3 }?.points ?: listOf(
                                PolygonVertex(0.0, 0.0), PolygonVertex(120.0, 0.0), PolygonVertex(120.0, 90.0), PolygonVertex(0.0, 90.0)
                            )
                            val roadAnalysis = remember(roadSpec, activePoly, snap.cogoPoints) {
                                CogoAndSurfaceEngine.analyzeRoadAlignmentAndAccess(roadSpec, activePoly, snap.cogoPoints)
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("রাস্তা অ্যালাইনমেন্ট, স্টেশন-অফসেট ও প্রবেশপথ যাচাই", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = roadSpec.name,
                                    onValueChange = { roadSpec = roadSpec.copy(name = it) },
                                    label = { Text("রাস্তার নাম") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = roadRowInput,
                                    onValueChange = {
                                        roadRowInput = it
                                        it.toDoubleOrNull()?.let { w -> roadSpec = roadSpec.copy(rightOfWayWidthFt = w.coerceAtLeast(4.0)) }
                                    },
                                    label = { Text("রাস্তার প্রস্থ / Right-of-Way (ফুট)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        repo.updateRoadSpec(roadSpec)
                                        toast = "রাস্তা অ্যালাইনমেন্ট সেভ হয়েছে!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("অ্যালাইনমেন্ট সংরক্ষণ করুন") }

                                Surface(
                                    color = (if (roadAnalysis.hasPlotAccess) EmeraldPrimary else Color(0xFFC62828)).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (roadAnalysis.hasPlotAccess) "✅ প্রবেশপথ সংযুক্ত (Valid Plot Access)" else "⚠️ ভূমি-আবদ্ধ সতর্কতা (রাস্তা থেকে দূরত্ব > RoW)",
                                            fontWeight = FontWeight.Bold,
                                            color = if (roadAnalysis.hasPlotAccess) EmeraldPrimary else Color(0xFFC62828)
                                        )
                                        Text("রাস্তার দৈর্ঘ্য: ${String.format(Locale.US, "%.2f", roadAnalysis.totalCenterlineLengthFt)} ফুট | জমির হিস্যা/রাস্তা এলাকা: ${String.format(Locale.US, "%.1f", roadAnalysis.roadAreaSqFt)} বর্গফুট")
                                        Text("প্লট থেকে রাস্তার ন্যূনতম দূরত্ব: ${String.format(Locale.US, "%.2f", roadAnalysis.minDistanceToPlotFt)} ফুট")
                                        Spacer(Modifier.height(4.dp))
                                        Text("স্টেশন ও অফসেট তালিকা:", fontWeight = FontWeight.SemiBold)
                                        roadAnalysis.stationOffsets.forEach { st ->
                                            Text("• $st", style = MaterialTheme.typography.bodySmall)
                                        }
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
