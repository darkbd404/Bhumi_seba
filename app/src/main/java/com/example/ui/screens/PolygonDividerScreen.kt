package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.WorkstationRepository
import com.example.engine.PolygonDividerEngine
import com.example.model.DivisionShareSpec
import com.example.model.LandSplitMode
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
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

    var selectedPresetIdx by remember { mutableStateOf(0) }
    var verticesText by remember {
        mutableStateOf(
            PolygonDividerEngine.SAMPLE_POLYGONS[0].second.joinToString("\n") { "${it.x}, ${it.y}" }
        )
    }
    var splitMode by remember { mutableStateOf(LandSplitMode.EQUAL) }
    var partCountText by remember { mutableStateOf("3") }
    var customSharesText by remember { mutableStateOf("রহিম:40, করিম:35, সখিনা:25") }
    var roadWidthText by remember { mutableStateOf("8") }
    var cutAngleDeg by remember { mutableFloatStateOf(0f) }
    var divisionTitle by remember { mutableStateOf("নমুনা মৌজা বণ্টন নকশা") }
    var toast by remember { mutableStateOf<String?>(null) }

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

    val result = remember(parsedVertices, splitMode, sharesSpec, roadWidthText, cutAngleDeg, divisionTitle) {
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
                titleBn = "স্বয়ংক্রিয় জমি বিভাজন ইঞ্জিন",
                subtitleEn = "Arbitrary Polygon Subdivision • Road Reservation • Conservation Check",
                helpTitle = "স্বয়ংক্রিয় জমি বিভাজন ব্যবহার",
                helpSteps = listOf(
                    "উপর থেকে নমুনা জমি বাছুন অথবা X, Y (ফুট) স্থানাঙ্ক লিখুন।",
                    "সমান ভাগ, শতাংশ (%) বা নির্দিষ্ট শতাংশ/কাঠা মোড নির্বাচন করুন।",
                    "রাস্তার প্রস্থ (যেমন ৮ বা ১২ ফুট) ও কাটার কোণ (০°–৩৬০°) ঠিক করলে সাথে সাথে ক্ষেত্রফল যাচাই ও নকশা PNG/PDF সেভ করা যাবে।"
                ),
                onBack = onBack,
                backTestTag = "divider_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        "জ্যামিতিক বিভাজন গণিতের মাধ্যমে করা হয়; সরেজমিন দখল ও পর্চা মিলিয়ে আমিন/সার্ভেয়ারের মাধ্যমে খুঁটি স্থাপন করুন।"
                    )
                }

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
                                }
                            )
                        }
                    }
                }

                // Visual Canvas Preview
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 6.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("পলিগন বিভাজন প্রিভিউ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color.White, RoundedCornerShape(14.dp))
                        ) {
                            val res = result
                            val allPts = if (res != null) (res.parcels.flatMap { it.vertices } + res.roadPolygon) else parsedVertices
                            if (allPts.size >= 3) {
                                val minX = allPts.minOf { it.x }
                                val maxX = allPts.maxOf { it.x }
                                val minY = allPts.minOf { it.y }
                                val maxY = allPts.maxOf { it.y }
                                val spanX = (maxX - minX).coerceAtLeast(1.0)
                                val spanY = (maxY - minY).coerceAtLeast(1.0)
                                val pad = 28f
                                val sc = min((size.width - pad * 2) / spanX, (size.height - pad * 2) / spanY)

                                fun toPt(v: PolygonVertex) = Offset(
                                    (pad + (v.x - minX) * sc).toFloat(),
                                    (pad + (maxY - v.y) * sc).toFloat()
                                )

                                if (res != null) {
                                    if (res.roadPolygon.size >= 3) {
                                        val rp = Path().apply {
                                            res.roadPolygon.forEachIndexed { i, v ->
                                                val p = toPt(v)
                                                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                            }
                                            close()
                                        }
                                        drawPath(rp, Color(0xFFB0BEC5), style = Fill)
                                        drawPath(rp, Color(0xFF455A64), style = Stroke(2f))
                                    }
                                    res.parcels.forEach { parcel ->
                                        val path = Path().apply {
                                            parcel.vertices.forEachIndexed { i, v ->
                                                val p = toPt(v)
                                                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                            }
                                            close()
                                        }
                                        drawPath(path, Color(parcel.colorArgb).copy(alpha = 0.28f), style = Fill)
                                        drawPath(path, Color(0xFF1B4332), style = Stroke(3f))
                                    }
                                } else {
                                    val path = Path().apply {
                                        parsedVertices.forEachIndexed { i, v ->
                                            val p = toPt(v)
                                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                        }
                                        close()
                                    }
                                    drawPath(path, Color.Red, style = Stroke(3f))
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
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

                        OutlinedTextField(
                            value = roadWidthText,
                            onValueChange = { roadWidthText = it },
                            label = { Text("রাস্তার জন্য বাদ (ফুট - না থাকলে ০)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = String.format(Locale.US, "বিভাজন রেখার কোণ: %.0f°", cutAngleDeg),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = cutAngleDeg,
                            onValueChange = { cutAngleDeg = it },
                            valueRange = 0f..180f
                        )

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

                // Results & Validation
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (result == null) {
                            Text(
                                text = "পলিগনের সীমানা নিজের ওপর দিয়ে ক্রস করেছে বা কমপক্ষে ৩টি বৈধ বিন্দু নেই।",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = if (result.isValidConservation) "ক্ষেত্রফল সংরক্ষণ যাচাই: সফল (Verified)" else "ক্ষেত্রফল যাচাই সতর্কতা",
                                color = if (result.isValidConservation) EmeraldPrimary else Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "মোট জমি: %.2f শতক | রাস্তা বাদ: %.2f শতক | নিট বণ্টনযোগ্য: %.2f শতক | অবশিষ্ট: %.2f শতক",
                                    result.originalAreaSqFt / 435.6,
                                    result.roadReservedSqFt / 435.6,
                                    result.usableAreaSqFt / 435.6,
                                    result.remainingAreaSqFt / 435.6
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )

                            result.parcels.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(p.colorArgb).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${p.index}. ${p.ownerName}", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "বাহু: ${p.edgeLengthsFt.joinToString(", ") { String.format(Locale.US, "%.1f'", it) }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%.2f শতক", p.areaDecimal),
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f কাঠা", p.areaKatha),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        repo.saveDivisionRecord(result)
                                        val res = PolygonDividerEngine.exportDivisionDiagram(context, result, asPdf = false)
                                        toast = "সেভ ও PNG ডাউনলোড: ${res.displayPath}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("divider_export_png")
                                ) { Text("PNG ও সেভ", maxLines = 1) }

                                OutlinedButton(
                                    onClick = {
                                        repo.saveDivisionRecord(result)
                                        val res = PolygonDividerEngine.exportDivisionDiagram(context, result, asPdf = true)
                                        toast = "PDF সেভ: ${res.displayPath}"
                                    },
                                    modifier = Modifier.weight(1f).testTag("divider_export_pdf")
                                ) { Text("PDF সেভ", maxLines = 1) }
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
