package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.LandCalculators
import com.example.engine.LandDivisionProEngine
import com.example.model.DivisionGeometryCutMode
import com.example.model.HoleCutoutPolygon
import com.example.model.PlotPlannerOptionConfig
import com.example.model.PolygonVertex
import com.example.model.ProDivisionReport
import com.example.model.QuadMasterSpec
import com.example.model.TafsilDagInput
import com.example.model.TafsilOwnerShareSpec
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale
import java.util.UUID
import kotlin.math.min

@Composable
fun QuadMasterTabContent(onToast: (String) -> Unit) {
    val context = LocalContext.current
    var northText by remember { mutableStateOf("80") }
    var southText by remember { mutableStateOf("92") }
    var eastText by remember { mutableStateOf("65") }
    var westText by remember { mutableStateOf("60") }
    var useDiagMode by remember { mutableStateOf(true) }
    var diagAcText by remember { mutableStateOf("108") }
    var cornerAngleText by remember { mutableStateOf("84") }
    var partsText by remember { mutableStateOf("2") }
    var cutAngleDeg by remember { mutableFloatStateOf(0f) }

    val spec = remember(northText, southText, eastText, westText, useDiagMode, diagAcText, cornerAngleText) {
        QuadMasterSpec(
            northFt = northText.toDoubleOrNull() ?: 0.0,
            southFt = southText.toDoubleOrNull() ?: 0.0,
            eastFt = eastText.toDoubleOrNull() ?: 0.0,
            westFt = westText.toDoubleOrNull() ?: 0.0,
            useDiagonalMode = useDiagMode,
            diagonalAcFt = diagAcText.toDoubleOrNull() ?: 0.0,
            cornerAngleSwDeg = cornerAngleText.toDoubleOrNull() ?: 90.0
        )
    }

    val quadResult = remember(spec, partsText, cutAngleDeg) {
        LandDivisionProEngine.solveAndSplitQuad(
            spec = spec,
            partsCount = (partsText.toIntOrNull() ?: 2).coerceIn(1, 12),
            parallelCutAngleDeg = cutAngleDeg.toDouble()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "QuadMaster — ৪ বাহু + কর্ণ/কোণ সমাধান ও বিভাজন",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = northText, onValueChange = { northText = it }, label = { Text("উত্তর (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = southText, onValueChange = { southText = it }, label = { Text("দক্ষিণ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = eastText, onValueChange = { eastText = it }, label = { Text("পূর্ব (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = westText, onValueChange = { westText = it }, label = { Text("পশ্চিম (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (useDiagMode) "মোড: কর্ণ (A-C) দিয়ে সঠিক আকৃতি" else "মোড: দক্ষিণ-পশ্চিম কোণ (°) দিয়ে")
                Switch(checked = useDiagMode, onCheckedChange = { useDiagMode = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (useDiagMode) {
                    OutlinedTextField(value = diagAcText, onValueChange = { diagAcText = it }, label = { Text("কর্ণ A–C (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                } else {
                    OutlinedTextField(value = cornerAngleText, onValueChange = { cornerAngleText = it }, label = { Text("কোণ SW (°)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = partsText, onValueChange = { partsText = it }, label = { Text("ভাগ সংখ্যা (১–১২)") }, singleLine = true, modifier = Modifier.weight(0.85f))
            }
            Text(String.format(Locale.US, "বিভাজন রেখার কোণ: %.0f°", cutAngleDeg), style = MaterialTheme.typography.labelMedium)
            Slider(value = cutAngleDeg, onValueChange = { cutAngleDeg = it }, valueRange = 0f..180f)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (quadResult == null) {
                Text(
                    "প্রদত্ত ৪ বাহু ও কর্ণ/কোণ দিয়ে বৈধ চতুর্ভুজ গঠন সম্ভব নয় (ত্রিভুজ অসমতা যাচাই করুন)।",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                ) {
                    val pts = quadResult.vertices
                    if (pts.size == 4) {
                        val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }
                        val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
                        val sc = min((size.width - 48f) / (maxX - minX).coerceAtLeast(1.0), (size.height - 48f) / (maxY - minY).coerceAtLeast(1.0))
                        fun toPt(v: PolygonVertex) = Offset((24f + (v.x - minX) * sc).toFloat(), (24f + (maxY - v.y) * sc).toFloat())

                        quadResult.splitParcels.forEach { sp ->
                            val path = Path().apply {
                                sp.vertices.forEachIndexed { idx, v ->
                                    val p = toPt(v)
                                    if (idx == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                }
                                close()
                            }
                            drawPath(path, Color(sp.colorArgb).copy(alpha = 0.28f), style = Fill)
                            drawPath(path, Color(0xFF1B4332), style = Stroke(3f))
                        }
                        // Diagonals AC and BD
                        drawLine(Color(0xFF1A73E8), toPt(pts[0]), toPt(pts[2]), strokeWidth = 2.5f)
                        drawLine(Color(0xFFE65100), toPt(pts[1]), toPt(pts[3]), strokeWidth = 2f)
                    }
                }

                Text(
                    text = String.format(
                        Locale.US,
                        "মোট জমি: %.3f শতক (%.3f কাঠা • %.1f ব.ফু.)",
                        quadResult.totalDecimal, quadResult.totalKatha, quadResult.totalSqFt
                    ),
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "কর্ণ AC: %.2f' | কর্ণ BD: %.2f' | ৪ কোণ: %s",
                        quadResult.diagonalAcFt,
                        quadResult.diagonalBdFt,
                        quadResult.interiorAnglesDeg.joinToString(", ") { String.format(Locale.US, "%.1f°", it) }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                quadResult.splitParcels.forEach { sp ->
                    Surface(
                        color = Color(sp.colorArgb).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${sp.index}. ${sp.ownerName} — ${String.format(Locale.US, "%.2f", sp.netDecimal)} শতক (${String.format(Locale.US, "%.2f", sp.netKatha)} কাঠা)", fontWeight = FontWeight.SemiBold)
                            Text("বাহুসমূহ: ${sp.edgeLengthsFt.joinToString(", ") { LandCalculators.formatFeetInches(it) }}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val reportForExport = ProDivisionReport(
                        id = "quad_exp_${System.currentTimeMillis()}",
                        title = "QuadMaster চতুর্ভুজ বিভাজন",
                        cutMode = DivisionGeometryCutMode.PARALLEL_LINE,
                        grossOuterAreaSqFt = quadResult.totalSqFt,
                        totalHoleExcludedSqFt = 0.0,
                        roadReservedSqFt = 0.0,
                        usableNetAreaSqFt = quadResult.totalSqFt,
                        allocatedNetAreaSqFt = quadResult.splitParcels.sumOf { it.netAreaSqFt },
                        remainingNetAreaSqFt = 0.0,
                        conservationErrorSqFt = 0.0,
                        isValidConservation = true,
                        totalRoadFrontageBeforeFt = 0.0,
                        totalRoadFrontageAfterFt = 0.0,
                        frontageReport = emptyList(),
                        parcels = quadResult.splitParcels
                    )
                    Button(
                        onClick = {
                            val r = LandDivisionProEngine.exportProDivisionReport(context, reportForExport, asPdf = false)
                            onToast("QuadMaster PNG: ${r.displayPath}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f).testTag("quad_export_png")
                    ) { Text("PNG সেভ") }
                    OutlinedButton(
                        onClick = {
                            val r = LandDivisionProEngine.exportProDivisionReport(context, reportForExport, asPdf = true)
                            onToast("QuadMaster PDF: ${r.displayPath}")
                        },
                        modifier = Modifier.weight(1f).testTag("quad_export_pdf")
                    ) { Text("PDF সেভ") }
                }
            }
        }
    }
}

@Composable
fun PlotPlannerTabContent(outerVertices: List<PolygonVertex>) {
    var kathaAText by remember { mutableStateOf("3.0") }
    var roadAText by remember { mutableStateOf("20") }
    var drainPctText by remember { mutableStateOf("8") }
    var kathaBText by remember { mutableStateOf("2.5") }
    var roadBText by remember { mutableStateOf("16") }
    var kathaCText by remember { mutableStateOf("4.0") }
    var roadCText by remember { mutableStateOf("12") }
    var selectedOptionIdx by remember { mutableIntStateOf(0) }

    val options = remember(kathaAText, roadAText, drainPctText, kathaBText, roadBText, kathaCText, roadCText) {
        val drain = drainPctText.toDoubleOrNull() ?: 8.0
        listOf(
            PlotPlannerOptionConfig("Option A", kathaAText.toDoubleOrNull() ?: 3.0, roadAText.toDoubleOrNull() ?: 20.0, drain),
            PlotPlannerOptionConfig("Option B", kathaBText.toDoubleOrNull() ?: 2.5, roadBText.toDoubleOrNull() ?: 16.0, (drain - 2.0).coerceAtLeast(0.0)),
            PlotPlannerOptionConfig("Option C", kathaCText.toDoubleOrNull() ?: 4.0, roadCText.toDoubleOrNull() ?: 12.0, (drain - 3.0).coerceAtLeast(0.0))
        )
    }

    val summaries = remember(outerVertices, options) {
        LandDivisionProEngine.comparePlotPlannerOptions(outerVertices, options)
    }
    val activeSummary = summaries.getOrNull(selectedOptionIdx)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("হাউজিং / প্লট প্ল্যানার (Option A / B / C তুলনা)", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = kathaAText, onValueChange = { kathaAText = it }, label = { Text("A কাঠা") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = roadAText, onValueChange = { roadAText = it }, label = { Text("A রাস্তা'") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = drainPctText, onValueChange = { drainPctText = it }, label = { Text("ড্রেন/খোলা %") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = kathaBText, onValueChange = { kathaBText = it }, label = { Text("B কাঠা") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = roadBText, onValueChange = { roadBText = it }, label = { Text("B রাস্তা'") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = kathaCText, onValueChange = { kathaCText = it }, label = { Text("C কাঠা") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = roadCText, onValueChange = { roadCText = it }, label = { Text("C রাস্তা'") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                summaries.forEachIndexed { idx, s ->
                    NeuPillChip(
                        text = "${s.optionLabel} (${s.plotCount} প্লট)",
                        selected = selectedOptionIdx == idx,
                        onClick = { selectedOptionIdx = idx }
                    )
                }
            }

            if (activeSummary != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                ) {
                    val allPts = outerVertices
                    if (allPts.size >= 3) {
                        val minX = allPts.minOf { it.x }; val maxX = allPts.maxOf { it.x }
                        val minY = allPts.minOf { it.y }; val maxY = allPts.maxOf { it.y }
                        val sc = min((size.width - 40f) / (maxX - minX).coerceAtLeast(1.0), (size.height - 40f) / (maxY - minY).coerceAtLeast(1.0))
                        fun toPt(v: PolygonVertex) = Offset((20f + (v.x - minX) * sc).toFloat(), (20f + (maxY - v.y) * sc).toFloat())

                        val outerPath = Path().apply {
                            allPts.forEachIndexed { idx, v ->
                                val p = toPt(v)
                                if (idx == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                            close()
                        }
                        drawPath(outerPath, Color(0xFFECEFF1), style = Fill)
                        drawPath(outerPath, Color(0xFF37474F), style = Stroke(3f))

                        activeSummary.roadCorridors.forEach { rc ->
                            val rp = Path().apply {
                                rc.forEachIndexed { i, v -> val p = toPt(v); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                                close()
                            }
                            drawPath(rp, Color(0xFF90A4AE).copy(alpha = 0.7f), style = Fill)
                        }
                        activeSummary.plotCells.forEach { cell ->
                            val cp = Path().apply {
                                cell.forEachIndexed { i, v -> val p = toPt(v); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                                close()
                            }
                            drawPath(cp, EmeraldPrimary.copy(alpha = 0.25f), style = Fill)
                            drawPath(cp, EmeraldPrimary, style = Stroke(2f))
                        }
                    }
                }
            }
        }

        summaries.forEachIndexed { idx, s ->
            Surface(
                color = if (selectedOptionIdx == idx) EmeraldPrimary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "${s.optionLabel}: মোট ${s.plotCount} টি প্লট (${String.format(Locale.US, "%.1f", s.targetPlotKatha)} কাঠা / ${String.format(Locale.US, "%.2f", s.targetPlotDecimal)} শতক)",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        String.format(
                            Locale.US,
                            "বিক্রয়যোগ্য জমি: %.2f শতক | রাস্তা+ড্রেন: %.2f শতক | দক্ষতা: %.1f%% | অব্যবহৃত: %.2f শতক",
                            s.salablePlotsAreaSqFt / 435.6,
                            s.roadAndDrainSqFt / 435.6,
                            s.efficiencyPct,
                            s.remainingWasteSqFt / 435.6
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        String.format(Locale.US, "প্রতি প্লটের মুখ: %.1f' × গভীরতা: %.1f'", s.roadFrontagePerPlotFt, s.plotDepthFt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun TafsilDivisionTabContent() {
    val dags = remember {
        mutableStateListOf(
            TafsilDagInput("td1", "আরএস ৪১২", "১০৪২", "নাল", 48.0, 0.5),
            TafsilDagInput("td2", "আরএস ৪১২", "১০৪৫", "ভিটা", 22.0, 1.0)
        )
    }
    val owners = remember {
        mutableStateListOf(
            TafsilOwnerShareSpec("আব্দুল করিম", 0.5),
            TafsilOwnerShareSpec("সখিনা বেগম", 0.3),
            TafsilOwnerShareSpec("রফিক উদ্দিন", 0.2)
        )
    }

    var khInput by remember { mutableStateOf("৪১২") }
    var dagInput by remember { mutableStateOf("") }
    var classInput by remember { mutableStateOf("নাল") }
    var decInput by remember { mutableStateOf("30") }
    var shareInput by remember { mutableStateOf("1.0") }

    var ownerNameInput by remember { mutableStateOf("") }
    var ownerShareInput by remember { mutableStateOf("0.25") }

    val report = remember(dags.toList(), owners.toList()) {
        LandDivisionProEngine.divideTafsilPlots(dags.toList(), owners.toList())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("১. তফসিলের দাগ ও খতিয়ান যুক্ত করুন", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = khInput, onValueChange = { khInput = it }, label = { Text("খতিয়ান") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = dagInput, onValueChange = { dagInput = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = classInput, onValueChange = { classInput = it }, label = { Text("শ্রেণী") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = decInput, onValueChange = { decInput = it }, label = { Text("মোট শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = shareInput, onValueChange = { shareInput = it }, label = { Text("অংশ (0–1)") }, singleLine = true, modifier = Modifier.weight(0.9f))
                Button(
                    onClick = {
                        if (dagInput.isNotBlank()) {
                            dags.add(
                                TafsilDagInput(
                                    id = "td_${UUID.randomUUID().toString().take(6)}",
                                    khatianNo = khInput.ifBlank { "-" },
                                    dagNo = dagInput.trim(),
                                    landClass = classInput.ifBlank { "নাল" },
                                    totalDecimal = decInput.toDoubleOrNull() ?: 0.0,
                                    ownerShareInDag = (shareInput.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0)
                                )
                            )
                            dagInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("দাগ +") }
            }
            dags.forEach { d ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "খতিয়ান ${d.khatianNo} | দাগ ${d.dagNo} (${d.landClass}): নিট ${String.format(Locale.US, "%.2f", d.effectiveDecimal)} শতক",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { dags.remove(d) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete dag", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("২. অংশীদার ও অনুপাত (মোট ১.০ = ১৬ আনা)", fontWeight = FontWeight.Bold, color = TealSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = ownerNameInput, onValueChange = { ownerNameInput = it }, label = { Text("অংশীদারের নাম") }, singleLine = true, modifier = Modifier.weight(1.3f))
                OutlinedTextField(value = ownerShareInput, onValueChange = { ownerShareInput = it }, label = { Text("অংশ (০–১)") }, singleLine = true, modifier = Modifier.weight(0.8f))
                Button(
                    onClick = {
                        if (ownerNameInput.isNotBlank()) {
                            owners.add(TafsilOwnerShareSpec(ownerNameInput.trim(), (ownerShareInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)))
                            ownerNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                ) { Text("যোগ +") }
            }
            owners.forEach { o ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${o.ownerName} — অংশ ${String.format(Locale.US, "%.3f", o.shareRatio)} (${String.format(Locale.US, "%.1f%%", o.shareRatio * 100.0)})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { owners.remove(o) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete owner", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (report.isShareSumBalanced) "✓ ১৬ আনা অনুপাত সুষম (মোট: ${String.format(Locale.US, "%.2f", report.totalEffectiveDecimal)} শতক)"
                else "⚠ অংশের যোগফল ১.০০০ নয় — অনুপাত অনুযায়ী স্বয়ংক্রিয় সমন্বয়কৃত (${String.format(Locale.US, "%.2f", report.totalEffectiveDecimal)} শতক)",
                fontWeight = FontWeight.Bold,
                color = if (report.isShareSumBalanced) EmeraldPrimary else Color(0xFFD97706)
            )
            report.ownerTotalsDecimal.forEach { (owner, tot) ->
                Text("• $owner মোট প্রাপ্য: ${String.format(Locale.US, "%.3f", tot)} শতক (${String.format(Locale.US, "%.2f", tot * 0.605)} কাঠা)", fontWeight = FontWeight.SemiBold)
            }
            report.rows.forEach { r ->
                Text(
                    String.format(Locale.US, "  - %s | দাগ %s (%s): %.3f শতক (%.1f%%)", r.ownerName, r.dagNo, r.landClass, r.allocatedDecimal, r.sharePct),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun HoleCutoutDialog(
    onDismiss: () -> Unit,
    onAddHole: (HoleCutoutPolygon) -> Unit
) {
    var label by remember { mutableStateOf("পুকুর / ভিটা (বাদ)") }
    var minX by remember { mutableStateOf("20") }
    var minY by remember { mutableStateOf("20") }
    var maxX by remember { mutableStateOf("45") }
    var maxY by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ভেতরের বাদযোগ্য পুকুর/ঘর (Hole) যোগ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("বিবরণ (যেমন পুকুর)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minX, onValueChange = { minX = it }, label = { Text("X₁ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minY, onValueChange = { minY = it }, label = { Text("Y₁ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = maxX, onValueChange = { maxX = it }, label = { Text("X₂ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxY, onValueChange = { maxY = it }, label = { Text("Y₂ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val x1 = minX.toDoubleOrNull() ?: 10.0
                val y1 = minY.toDoubleOrNull() ?: 10.0
                val x2 = (maxX.toDoubleOrNull() ?: 30.0).coerceAtLeast(x1 + 2.0)
                val y2 = (maxY.toDoubleOrNull() ?: 30.0).coerceAtLeast(y1 + 2.0)
                onAddHole(
                    HoleCutoutPolygon(
                        id = "hole_${UUID.randomUUID().toString().take(6)}",
                        label = label.ifBlank { "বাদযোগ্য অংশ" },
                        vertices = listOf(
                            PolygonVertex(x1, y1),
                            PolygonVertex(x2, y1),
                            PolygonVertex(x2, y2),
                            PolygonVertex(x1, y2)
                        )
                    )
                )
            }) { Text("যুক্ত করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
