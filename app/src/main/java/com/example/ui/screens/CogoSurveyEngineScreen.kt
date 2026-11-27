package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterCadRepository
import com.example.engine.CogoAndSurfaceEngine
import com.example.engine.LandCalculators
import com.example.engine.TraverseLegInput
import com.example.model.CogoPoint
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun CogoSurveyEngineScreen(
    initialMode: String = "point",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { MasterCadRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var tab by remember(initialMode) {
        mutableIntStateOf(
            when (initialMode) {
                "intersect" -> 1
                "traverse" -> 2
                "curve" -> 3
                "subdivide" -> 4
                else -> 0
            }
        )
    }
    var toast by remember { mutableStateOf<String?>(null) }

    // 1. Radiation & Inverse
    var stN by remember { mutableStateOf("1000") }
    var stE by remember { mutableStateOf("1000") }
    var radAz by remember { mutableStateOf("60") }
    var radDist by remember { mutableStateOf("120") }
    var radSummary by remember { mutableStateOf("") }

    // 2. Intersections & 3-Point Resection
    var ax by remember { mutableStateOf("0") }; var ay by remember { mutableStateOf("0") }
    var bx by remember { mutableStateOf("100") }; var by by remember { mutableStateOf("0") }
    var r1 by remember { mutableStateOf("80") }; var r2 by remember { mutableStateOf("80") }
    var az1 by remember { mutableStateOf("45") }; var az2 by remember { mutableStateOf("315") }
    var interResult by remember { mutableStateOf("") }

    // 3. Traverse Adjustment
    var travMethod by remember { mutableStateOf(CogoAndSurfaceEngine.TraverseMethod.BOWDITCH) }
    var travSummary by remember { mutableStateOf("") }

    // 4. Circular Curve
    var curveR by remember { mutableStateOf("250") }
    var curveDelta by remember { mutableStateOf("42") }
    var curveSummary by remember { mutableStateOf("") }

    // 5. Geometric Cutoff Subdivision
    var targetCutDec by remember { mutableStateOf("10.0") }
    var cutModeHinged by remember { mutableStateOf(true) }
    var subSummary by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "প্রফেশনাল COGO সার্ভে ইঞ্জিন (Module 03)",
                subtitleEn = "Inverse/Radiation • Intersections & Resection • Bowditch/Transit/Least-Squares • Curves • Hinged Cutoff",
                helpTitle = "COGO জরিপ ক্যালকুলেটর গাইড",
                helpSteps = listOf(
                    "Radiation/Inverse ট্যাবে যেকোনো স্টেশন থেকে বিয়ারিং-দূরত্ব দিয়ে নতুন কো-অর্ডিনেট বের করুন।",
                    "Intersections-এ Distance-Distance, Bearing-Bearing এবং ৩-পয়েন্ট Resection সমাধান করুন।",
                    "Traverse-এ Bowditch, Transit এবং Least-Squares সমন্বয় এবং Hinged/Sliding কাটঅফ বিভাজন চালান।"
                ),
                onBack = onNavigateBack,
                backTestTag = "cogo_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("৩.১ পয়েন্ট/ইনভার্স/রেডিয়েশন", tab == 0, testTag = "cogo_tab_0") { tab = 0 } }
                item { NeuPillChip("৩.২ ইন্টারসেকশন ও রিসেকশন", tab == 1, testTag = "cogo_tab_1") { tab = 1 } }
                item { NeuPillChip("৩.৩ ট্র্যাভার্স (Bowditch/LS)", tab == 2, testTag = "cogo_tab_2") { tab = 2 } }
                item { NeuPillChip("৩.৪ সার্কুলার কার্ভ (Curve)", tab == 3, testTag = "cogo_tab_3") { tab = 3 } }
                item { NeuPillChip("৩.৫ হিঞ্জড/স্লাইডিং বিভাজন", tab == 4, testTag = "cogo_tab_4") { tab = 4 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    0 -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("COGO Inverse & Radiation (${snap.cogoPoints.size} সংরক্ষিত পয়েন্ট)", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(stN, { stN = it }, label = { Text("Station North (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(stE, { stE = it }, label = { Text("Station East (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(radAz, { radAz = it }, label = { Text("Bearing/Azimuth (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(radDist, { radDist = it }, label = { Text("Distance (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val base = CogoPoint("base", 100, "STN", stN.toDoubleOrNull() ?: 0.0, stE.toDoubleOrNull() ?: 0.0)
                                        val resPt = CogoAndSurfaceEngine.radiation(base, radAz.toDoubleOrNull() ?: 0.0, radDist.toDoubleOrNull() ?: 50.0, newNo = snap.cogoPoints.size + 1)
                                        val inv = CogoAndSurfaceEngine.inverse(base, resPt)
                                        repo.updateCogoPoints(snap.cogoPoints + resPt)
                                        radSummary = String.format(Locale.US, "নতুন পয়েন্ট P%d → N: %.3f ft, E: %.3f ft\nযাচাই Inverse: %.3f ft @ %.2f°", resPt.pointNumber, resPt.northFt, resPt.eastFt, inv.horizontalDistFt, inv.azimuthDeg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("cogo_radiation_btn")
                                ) { Text("Radiation পয়েন্ট তৈরি") }
                            }
                            if (radSummary.isNotBlank()) {
                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(radSummary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    1 -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("COGO Intersections & 3-Point Resection", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(ax, { ax = it }, label = { Text("Pt A East") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(ay, { ay = it }, label = { Text("Pt A North") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(bx, { bx = it }, label = { Text("Pt B East") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(by, { by = it }, label = { Text("Pt B North") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(r1, { r1 = it }, label = { Text("R1 (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(r2, { r2 = it }, label = { Text("R2 (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(az1, { az1 = it }, label = { Text("Az 1 (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(az2, { az2 = it }, label = { Text("Az 2 (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val pa = PolygonVertex(ax.toDoubleOrNull() ?: 0.0, ay.toDoubleOrNull() ?: 0.0)
                                        val pb = PolygonVertex(bx.toDoubleOrNull() ?: 100.0, by.toDoubleOrNull() ?: 0.0)
                                        val dd = CogoAndSurfaceEngine.intersectDistanceDistance(pa, r1.toDoubleOrNull() ?: 80.0, pb, r2.toDoubleOrNull() ?: 80.0)
                                        val bb = CogoAndSurfaceEngine.intersectBearingBearing(pa, az1.toDoubleOrNull() ?: 45.0, pb, az2.toDoubleOrNull() ?: 315.0)
                                        interResult = buildString {
                                            append("• Distance–Distance: ")
                                            if (dd.isEmpty()) append("ছেদ পাওয়া যায়নি\n") else dd.forEachIndexed { idx, p -> append("P${idx + 1}(E=${String.format(Locale.US, "%.2f", p.x)}, N=${String.format(Locale.US, "%.2f", p.y)}) ") }.also { append("\n") }
                                            append("• Bearing–Bearing: ")
                                            append(if (bb == null) "সমান্তরাল" else String.format(Locale.US, "E=%.2f, N=%.2f", bb.x, bb.y))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("cogo_intersect_btn")
                                ) { Text("Intersection সমাধান") }

                                OutlinedButton(onClick = {
                                    val pa = PolygonVertex(0.0, 0.0); val pb = PolygonVertex(100.0, 0.0); val pc = PolygonVertex(50.0, 100.0)
                                    val res = CogoAndSurfaceEngine.solveThreePointResection(pa, pb, pc, 120.0, 120.0)
                                    interResult = if (res != null) String.format(Locale.US, "৩-পয়েন্ট Resection স্টেশন: E=%.3f ft, N=%.3f ft", res.x, res.y) else "রিসেকশন অসমাধানযোগ্য"
                                }) { Text("3-Pt Resection") }
                            }
                            if (interResult.isNotBlank()) {
                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(interResult, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    2 -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Closed Traverse সমন্বয় (Bowditch / Transit / Least-Squares)", fontWeight = FontWeight.Bold)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CogoAndSurfaceEngine.TraverseMethod.entries.forEach { m ->
                                    FilterChip(selected = travMethod == m, onClick = { travMethod = m }, label = { Text(m.titleBn) })
                                }
                            }
                            Button(
                                onClick = {
                                    val legs = listOf(
                                        TraverseLegInput("AB", 0.0, 100.0),
                                        TraverseLegInput("BC", 90.0, 120.0),
                                        TraverseLegInput("CD", 180.2, 100.1),
                                        TraverseLegInput("DA", 270.0, 119.7)
                                    )
                                    val res = CogoAndSurfaceEngine.adjustClosedTraverse(legs, travMethod)
                                    if (res != null) {
                                        val dec = res.adjustedAreaSqFt / LandCalculators.SQ_FT_PER_DECIMAL
                                        travSummary = String.format(
                                            Locale.US,
                                            "পদ্ধতি: %s\nমোট পরিসীমা: %.2f ft | মিসক্লোজার: %.3f ft (%s)\nসমন্বিত এরিয়া: %.2f sq.ft (%.2f শতক)",
                                            res.method.titleBn, res.totalPerimeterFt, res.linearMisclosureFt, res.precisionRatioLabel, res.adjustedAreaSqFt, dec
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("cogo_traverse_btn")
                            ) { Text("ট্র্যাভার্স সমন্বয় চালান (নমুনা ৪-বাহু)") }
                            if (travSummary.isNotBlank()) {
                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(travSummary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    3 -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("সার্কুলার কার্ভ (R, D, Δ, L, C, T, E, M ও স্টেকআউট)", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(curveR, { curveR = it }, label = { Text("ব্যাসার্ধ R (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(curveDelta, { curveDelta = it }, label = { Text("কোণ Δ (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Button(
                                onClick = {
                                    val res = CogoAndSurfaceEngine.computeCircularCurve(curveR.toDoubleOrNull() ?: 250.0, curveDelta.toDoubleOrNull() ?: 42.0)
                                    curveSummary = String.format(
                                        Locale.US,
                                        "Arc L = %.2f ft | Chord C = %.2f ft | Tangent T = %.2f ft\nExternal E = %.2f ft | Mid-Ord M = %.2f ft | D = %.2f°\n%s",
                                        res.arcLengthFt, res.chordLengthFt, res.tangentLengthFt, res.externalDistanceFt, res.midOrdinateFt, res.degreeOfCurveArc,
                                        res.chordDeflections.joinToString("\n")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("cogo_curve_btn")
                            ) { Text("কার্ভ হিসাব করুন") }
                            if (curveSummary.isNotBlank()) {
                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(curveSummary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    4 -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("জ্যামিতিক কাটঅফ জমি বিভাজন (Hinged / Sliding Line)", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = cutModeHinged, onClick = { cutModeHinged = true }, label = { Text("Hinged Line (কোণায় পিভট)") })
                                FilterChip(selected = !cutModeHinged, onClick = { cutModeHinged = false }, label = { Text("Sliding Parallel Line") })
                            }
                            OutlinedTextField(targetCutDec, { targetCutDec = it }, label = { Text("কাটার লক্ষ্যমাত্রা (শতাংশ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Button(
                                onClick = {
                                    val poly = snap.entities.firstOrNull { it.points.size >= 3 }?.points
                                        ?: listOf(PolygonVertex(0.0, 0.0), PolygonVertex(120.0, 0.0), PolygonVertex(120.0, 90.0), PolygonVertex(0.0, 90.0))
                                    val dec = targetCutDec.toDoubleOrNull() ?: 10.0
                                    if (cutModeHinged) {
                                        val split = CogoAndSurfaceEngine.subdivideByHingedLine(poly, 0, dec * LandCalculators.SQ_FT_PER_DECIMAL)
                                        subSummary = if (split == null) "লক্ষ্যমাত্রা মোট জমির চেয়ে বেশি/অবৈধ" else {
                                            val a1 = (com.example.engine.SurveyMathProEngine.computeCoordinateGeometry(split.first)?.sqFeet ?: 0.0) / LandCalculators.SQ_FT_PER_DECIMAL
                                            val a2 = (com.example.engine.SurveyMathProEngine.computeCoordinateGeometry(split.second)?.sqFeet ?: 0.0) / LandCalculators.SQ_FT_PER_DECIMAL
                                            String.format(Locale.US, "Hinged কাটঅফ সফল!\nঅংশ ১: %.2f শতক (%d কোণা)\nঅবশিষ্ট অংশ ২: %.2f শতক (%d কোণা)", a1, split.first.size, a2, split.second.size)
                                        }
                                    } else {
                                        val res = CogoAndSurfaceEngine.subdivideSlidingParallel(poly, dec, 0.0)
                                        subSummary = if (res == null) "বিভাজন অসফল" else String.format(Locale.US, "Sliding কাটঅফ সফল: অংশ %.2f শতক | অবশিষ্ট %.2f শতক", res.allocatedAreaSqFt / 435.6, res.remainingAreaSqFt / 435.6)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("cogo_subdivide_btn")
                            ) { Text("প্লট কাটঅফ বিভাজন চালান") }
                            if (subSummary.isNotBlank()) {
                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(subSummary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
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
