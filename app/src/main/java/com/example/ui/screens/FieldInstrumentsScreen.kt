package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpansionRepository
import com.example.engine.ExpansionMathAndSurveyEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.TraverseLegInput
import com.example.engine.UniversalExportEngine
import com.example.model.FieldBookEntry
import com.example.model.LevelReadingEntry
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun FieldInstrumentsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // B01 Total Station + EDM
    var stN by remember { mutableStateOf("1000.0") }
    var stE by remember { mutableStateOf("1000.0") }
    var stRl by remember { mutableStateOf("15.50") }
    var hi by remember { mutableStateOf("4.85") }
    var prismH by remember { mutableStateOf("5.00") }
    var azDeg by remember { mutableStateOf("62.5") }
    var vAngle by remember { mutableStateOf("2.4") }
    var slopeDist by remember { mutableStateOf("145.0") }
    var prismConst by remember { mutableStateOf("0.0") }
    var tsOutput by remember { mutableStateOf("") }

    // B02 Theodolite + Bowditch/Transit
    var anglesInput by remember { mutableStateOf("89.5, 90.2, 90.1, 90.0") }
    var legsInput by remember { mutableStateOf("AB:0:100; BC:90:80; CD:180:100.3; DA:270.2:79.9") }
    var theoOutput by remember { mutableStateOf("") }

    // B03 Auto Level Book
    var benchmarkRl by remember { mutableStateOf("10.000") }
    var closingBmRl by remember { mutableStateOf("10.150") }
    val levelEntries = remember {
        mutableStateListOf(
            LevelReadingEntry("l1", "BM-1", 0.0, bs = 1.450, remarks = "শুরু বেঞ্চমার্ক"),
            LevelReadingEntry("l2", "IS-1", 50.0, intermediateSight = 1.320, remarks = "রাস্তার কেন্দ্র"),
            LevelReadingEntry("l3", "CP-1", 100.0, bs = 1.620, fs = 1.180, remarks = "চেঞ্জ পয়েন্ট"),
            LevelReadingEntry("l4", "BM-Close", 150.0, fs = 1.740, remarks = "শেষ বেঞ্চমার্ক")
        )
    }
    var newSt by remember { mutableStateOf("TP-2") }
    var newBs by remember { mutableStateOf("") }
    var newIs by remember { mutableStateOf("") }
    var newFs by remember { mutableStateOf("") }
    var levelSummaryText by remember { mutableStateOf("") }

    // B05-07 GNSS Averaging & Cut-Fill Earthwork
    var gnssCoords by remember { mutableStateOf("23.81040,90.41250; 23.81042,90.41252; 23.81039,90.41249") }
    var gnssAvgOut by remember { mutableStateOf("") }
    var secLen by remember { mutableStateOf("100") }
    var secWid by remember { mutableStateOf("30") }
    var exStart by remember { mutableStateOf("10.2") }
    var exEnd by remember { mutableStateOf("11.4") }
    var prStart by remember { mutableStateOf("12.0") }
    var prEnd by remember { mutableStateOf("12.0") }
    var earthOut by remember { mutableStateOf("") }

    val tabs = listOf(
        "B01/B04 টোটাল স্টেশন ও EDM",
        "B02 থিওডোলাইট ও ট্র্যাভার্স",
        "B03 অটো লেভেল বুক",
        "B05-08 GNSS, মাটি ও ফিল্ডবুক"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "সার্ভেয়ার যন্ত্রপাতি ও ফিল্ড বুক (B01–B08)",
                subtitleEn = "Total Station, Theodolite, Auto Level HI/Rise-Fall, EDM, Cut-Fill & Field Book",
                helpTitle = "ফিল্ড যন্ত্রপাতি ক্যালকুলেশন নির্দেশিকা",
                helpSteps = listOf(
                    "টোটাল স্টেশন ট্যাবে স্টেশন স্থানাঙ্ক, HI, প্রিজম উচ্চতা ও ঢালু দূরত্ব দিলে আনুভূমিক দূরত্ব ও RL পাওয়া যাবে।",
                    "অটো লেভেল বুকে Sum BS - Sum FS == Sum Rise - Sum Fall == Last RL - First RL অটো যাচাই হয়।",
                    "ফিল্ড বুকের যেকোনো রিডিং Excel (XLSX), CSV বা PDF হিসেবে রপ্তানি করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "field_instruments_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.indices.toList()) { i ->
                    NeuPillChip(tabs[i], activeTab == i, testTag = "inst_tab_$i", onClick = { activeTab = i })
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("B01 & B04. টোটাল স্টেশন শট ও EDM রিডাকশন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(stN, { stN = it }, label = { Text("Station N") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(stE, { stE = it }, label = { Text("Station E") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(stRl, { stRl = it }, label = { Text("RL (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(hi, { hi = it }, label = { Text("যন্ত্রের উচ্চতা HI") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(prismH, { prismH = it }, label = { Text("প্রিজম উচ্চতা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(prismConst, { prismConst = it }, label = { Text("Prism Const") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(azDeg, { azDeg = it }, label = { Text("অনুভূমিক কোণ (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(vAngle, { vAngle = it }, label = { Text("উল্লম্ব কোণ (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(slopeDist, { slopeDist = it }, label = { Text("ঢালু দূরত্ব (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val r = ExpansionMathAndSurveyEngine.computeTotalStationShot(
                                                stN.toDoubleOrNull() ?: 0.0,
                                                stE.toDoubleOrNull() ?: 0.0,
                                                stRl.toDoubleOrNull() ?: 0.0,
                                                hi.toDoubleOrNull() ?: 0.0,
                                                prismH.toDoubleOrNull() ?: 0.0,
                                                azDeg.toDoubleOrNull() ?: 0.0,
                                                vAngle.toDoubleOrNull() ?: 0.0,
                                                slopeDist.toDoubleOrNull() ?: 0.0,
                                                prismConst.toDoubleOrNull() ?: 0.0
                                            )
                                            val mlm = ExpansionMathAndSurveyEngine.computeMissingLine(
                                                stN.toDoubleOrNull() ?: 0.0, stE.toDoubleOrNull() ?: 0.0, stRl.toDoubleOrNull() ?: 0.0,
                                                r.targetNorthFt, r.targetEastFt, r.targetElevationFt
                                            )
                                            tsOutput = String.format(
                                                Locale.US,
                                                "• আনুভূমিক দূরত্ব (HD): %.3f ft\n• উচ্চতা পার্থক্য (ΔZ): %+.3f ft\n• টার্গেট N: %.3f | E: %.3f | RL: %.3f ft\n• MLM ঢাল গ্রেড: %.2f%% (Bearing: %.2f°)",
                                                r.horizontalDistFt, r.verticalDiffFt, r.targetNorthFt, r.targetEastFt, r.targetElevationFt, mlm.gradePct, mlm.bearingDeg
                                            )
                                            repo.saveFieldBookEntry(
                                                FieldBookEntry(
                                                    id = "fb_${System.currentTimeMillis()}",
                                                    stationNo = "ST-1",
                                                    pointNo = "P-${snap.fieldBookEntries.size + 1}",
                                                    instrument = "Total Station / EDM",
                                                    bearingDeg = azDeg.toDoubleOrNull() ?: 0.0,
                                                    distanceFt = r.horizontalDistFt,
                                                    verticalAngleDeg = vAngle.toDoubleOrNull() ?: 0.0,
                                                    elevationM = r.targetElevationFt * 0.3048,
                                                    remarks = "RL ${String.format(Locale.US, "%.2f", r.targetElevationFt)} ft"
                                                )
                                            )
                                            repo.logCalculation("Part B01", "Total Station শট", "HD=${String.format(Locale.US, "%.2f", r.horizontalDistFt)}ft, RL=${String.format(Locale.US, "%.2f", r.targetElevationFt)}ft")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("calc_ts_shot_btn")
                                    ) { Text("হিসাব ও ফিল্ডবুকে সেভ") }
                                }
                                if (tsOutput.isNotBlank()) {
                                    Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(tsOutput, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("B02. ডিজিটাল থিওডোলাইট ও Bowditch বনাম Transit ট্র্যাভার্স", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(anglesInput, { anglesInput = it }, label = { Text("অভ্যন্তরীণ কোণসমূহ (° কমা দিয়ে)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(legsInput, { legsInput = it }, label = { Text("বাহু:বিয়ারিং:দূরত্ব (; সেমিকোলন দিয়ে)") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        val angs = anglesInput.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                                        val (theo, mis, corr) = ExpansionMathAndSurveyEngine.checkPolygonAngularMisclosure(angs)
                                        val legs = legsInput.split(";").mapNotNull { seg ->
                                            val p = seg.trim().split(":")
                                            if (p.size == 3) TraverseLegInput(p[0].trim(), p[1].trim().toDoubleOrNull() ?: 0.0, p[2].trim().toDoubleOrNull() ?: 0.0) else null
                                        }
                                        val tr = ExpansionMathAndSurveyEngine.solveBowditchAndTransit(legs)
                                        theoOutput = buildString {
                                            append("• কোণ চেক: তাত্ত্বিক=${String.format(Locale.US, "%.2f", theo)}°, ত্রুটি=${String.format(Locale.US, "%+.3f", mis)}°, প্রতি কোণ সংশোধন=${String.format(Locale.US, "%+.3f", corr)}°\n")
                                            if (tr != null) {
                                                append("• ট্র্যাভার্স ক্লোজিং ত্রুটি: ${String.format(Locale.US, "%.3f ft", tr.misclosureFt)} (${tr.precisionRatio})\n")
                                                append("• Bowditch এলাকা: ${String.format(Locale.US, "%.2f ব.ফু", tr.bowditchAreaSqFt)}\n")
                                                append("• Transit Rule এলাকা: ${String.format(Locale.US, "%.2f ব.ফু", tr.transitAreaSqFt)}")
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("calc_theo_traverse_btn")
                                ) { Text("কৌণিক ত্রুটি ও Bowditch/Transit সমাধান") }

                                if (theoOutput.isNotBlank()) {
                                    Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(theoOutput, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("B03. অটো/ডিজিটাল লেভেল বুক (HI ও Rise/Fall যাচাই)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(benchmarkRl, { benchmarkRl = it }, label = { Text("Start BM RL") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(closingBmRl, { closingBmRl = it }, label = { Text("Known Close BM") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(newSt, { newSt = it }, label = { Text("St") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(newBs, { newBs = it }, label = { Text("BS") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(newIs, { newIs = it }, label = { Text("IS") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(newFs, { newFs = it }, label = { Text("FS") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        levelEntries.add(
                                            LevelReadingEntry(
                                                "l_${System.currentTimeMillis()}", newSt, levelEntries.size * 50.0,
                                                bs = newBs.toDoubleOrNull(), intermediateSight = newIs.toDoubleOrNull(), fs = newFs.toDoubleOrNull()
                                            )
                                        )
                                        newBs = ""; newIs = ""; newFs = ""
                                    }, modifier = Modifier.weight(1f)) { Text("+ রিডিং যোগ") }
                                    Button(
                                        onClick = {
                                            val sum = ExpansionMathAndSurveyEngine.computeLevelBook(
                                                benchmarkRl.toDoubleOrNull() ?: 10.0,
                                                levelEntries,
                                                closingBmRl.toDoubleOrNull()
                                            )
                                            levelSummaryText = buildString {
                                                sum.rows.forEach { r ->
                                                    append("${r.station} | Ch:${r.chainageFt}ft | HI:${r.hi?.let { String.format(Locale.US, "%.3f", it) } ?: "-"} | RL:${String.format(Locale.US, "%.3f", r.rl)}\n")
                                                }
                                                append("• Sum BS (${String.format(Locale.US, "%.3f", sum.sumBs)}) - Sum FS (${String.format(Locale.US, "%.3f", sum.sumFs)}) = ${String.format(Locale.US, "%+.3f", sum.sumBs - sum.sumFs)}\n")
                                                append("• Sum Rise (${String.format(Locale.US, "%.3f", sum.sumRise)}) - Sum Fall (${String.format(Locale.US, "%.3f", sum.sumFall)}) = ${String.format(Locale.US, "%+.3f", sum.sumRise - sum.sumFall)}\n")
                                                append("• Arithmetic Check: ${if (sum.arithmeticBalanced) "সঠিক (PASSED ✓)" else "অমিল ⚠"} | Closing Error: ${String.format(Locale.US, "%+.4f", sum.closingError)}")
                                            }
                                            repo.logCalculation("Part B03", "লেভেল বুক", "Closing Err: ${String.format(Locale.US, "%.4f", sum.closingError)}")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("compute_level_book_btn")
                                    ) { Text("হিসাব ও চেক") }
                                }
                                if (levelSummaryText.isNotBlank()) {
                                    Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(levelSummaryText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("B05 & B07. GNSS পয়েন্ট এভারেজিং এবং মাটি কাটা/ভরাট (Cut & Fill)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(gnssCoords, { gnssCoords = it }, label = { Text("GNSS পর্যবেক্ষণ (lat,lon; ...)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedButton(onClick = {
                                    val pts = gnssCoords.split(";").mapNotNull { p ->
                                        val xy = p.trim().split(",")
                                        if (xy.size == 2) (xy[0].trim().toDoubleOrNull() to xy[1].trim().toDoubleOrNull()) else null
                                    }.filter { it.first != null && it.second != null }
                                    if (pts.isNotEmpty()) {
                                        val avgLat = pts.map { it.first!! }.average()
                                        val avgLon = pts.map { it.second!! }.average()
                                        gnssAvgOut = "গড় স্থানাঙ্ক (${pts.size} শট): ${String.format(Locale.US, "%.6f, %.6f", avgLat, avgLon)}"
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("GNSS Base-Rover পয়েন্ট গড় বের করুন") }
                                if (gnssAvgOut.isNotBlank()) Text(gnssAvgOut, style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(secLen, { secLen = it }, label = { Text("দৈর্ঘ্য (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(secWid, { secWid = it }, label = { Text("প্রস্থ (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(exStart, { exStart = it }, label = { Text("বর্তমান শুরু RL") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(exEnd, { exEnd = it }, label = { Text("বর্তমান শেষ RL") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(prStart, { prStart = it }, label = { Text("প্রস্তাবিত শুরু RL") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(prEnd, { prEnd = it }, label = { Text("প্রস্তাবিত শেষ RL") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        val ew = ExpansionMathAndSurveyEngine.computeEarthworkCutFill(
                                            secLen.toDoubleOrNull() ?: 100.0,
                                            secWid.toDoubleOrNull() ?: 30.0,
                                            exStart.toDoubleOrNull() ?: 10.0,
                                            exEnd.toDoubleOrNull() ?: 11.0,
                                            prStart.toDoubleOrNull() ?: 12.0,
                                            prEnd.toDoubleOrNull() ?: 12.0
                                        )
                                        earthOut = "• কাজ: ${if (ew.isFill) "মাটি ভরাট (Fill)" else "মাটি কাটা (Cut)"}\n• আয়তন: ${String.format(Locale.US, "%.1f ঘনফুট (%.2f সিএফটি ট্রাক প্রায়)", ew.netVolumeCuFt, ew.netVolumeCuFt / 100.0)}\n• ঢাল: ${String.format(Locale.US, "%.2f%%", ew.slopePct)}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("calc_earthwork_btn")
                                ) { Text("মাটি কাটা/ভরাট (Cut & Fill) হিসাব") }
                                if (earthOut.isNotBlank()) Text(earthOut, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("B08. ডিজিটাল ফিল্ড বুক (${snap.fieldBookEntries.size} এন্ট্রি)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val headers = listOf("Station", "Point", "Instrument", "Bearing", "HD_ft", "Elev_m", "Remarks")
                                            val rows = snap.fieldBookEntries.map {
                                                listOf(it.stationNo, it.pointNo, it.instrument, it.bearingDeg.toString(), String.format(Locale.US, "%.2f", it.distanceFt), String.format(Locale.US, "%.2f", it.elevationM), it.remarks)
                                            }
                                            val xlsx = UniversalExportEngine.buildXlsxBytes("FieldBook", headers, rows)
                                            val path = ExportStreamWriter.saveBinaryFileToDownloads(context, xlsx, "field_book_${System.currentTimeMillis()}.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                            toast = "Excel XLSX সংরক্ষিত: $path"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("XLSX রপ্তানি") }

                                    OutlinedButton(
                                        onClick = {
                                            val body = snap.fieldBookEntries.joinToString("\n") {
                                                "${it.stationNo} -> ${it.pointNo} | ${it.instrument} | Az:${it.bearingDeg}° | HD:${String.format(Locale.US, "%.2f", it.distanceFt)}ft | ${it.remarks}"
                                            }.ifBlank { "কোনো ফিল্ড রিডিং নেই" }
                                            val bmp = UniversalExportEngine.renderDocumentReportBitmap("ডিজিটাল ফিল্ড বুক", "Total Station / Level / GNSS Register", body)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "field_book_${System.currentTimeMillis()}")
                                            toast = "PDF সংরক্ষিত: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("PDF সেভ") }
                                }
                                snap.fieldBookEntries.take(8).forEach { e ->
                                    Surface(color = Color.White.copy(alpha = 0.7f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("${e.stationNo} → ${e.pointNo} • ${e.instrument} • ${String.format(Locale.US, "%.2f", e.distanceFt)}ft • ${e.remarks}", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                                    }
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
