package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WorkstationRepository
import com.example.engine.LandAreaResult
import com.example.engine.LandCalculators
import com.example.engine.LengthUnit
import com.example.engine.SurveyMathProEngine
import com.example.engine.TraverseLegInput
import com.example.model.InheritanceLawType
import com.example.model.PolygonVertex
import com.example.model.RegionalUnitProfile
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import java.util.Locale
import java.util.UUID

private enum class SurveyToolTab(val key: String, val titleBn: String) {
    UNIT_CONVERTER("unit", "জমির একক"),
    LENGTH_CONVERTER("len", "দৈর্ঘ্য মাপ"),
    SHAPE_SURVEY("shape", "আকৃতি মাপ"),
    COORDINATE_TRAVERSE("traverse", "ট্র্যাভার্স ও কোণ"),
    KHATIAN_HISSA("hissa", "খতিয়ান হিস্যা"),
    FARAIZ("faraiz", "ওয়ারিশ/ফারায়েজ"),
    DEED_TAX("tax", "দলিল ও আর্থিক"),
    CHAIN_SCALE("scale", "স্কেল ও চেইন"),
    FORMULA_SOLVER("formula", "অজানা বাহু ও সূত্র")
}

@Composable
fun ToolsScreen(
    initialTabKey: String = "unit",
    onNavigateBack: () -> Unit,
    onOpenCogoEngine: (String) -> Unit = {},
    onOpenCadEngine: (String) -> Unit = {},
    onOpenDimensionSystem: (String) -> Unit = {},
    onOpenWorkstationHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeTab by remember(initialTabKey) {
        mutableStateOf(
            SurveyToolTab.entries.find { it.key == initialTabKey } ?: SurveyToolTab.UNIT_CONVERTER
        )
    }

    LaunchedEffect(initialTabKey) {
        SurveyToolTab.entries.find { it.key == initialTabKey }?.let { activeTab = it }
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
                .widthIn(max = 960.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .testTag("tools_back_button")
                    .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "ভূমি জরিপ ও পরিমাপ টুলস",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "সবগুলো অফলাইন ক্যালকুলেটর চালু রয়েছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SurveyToolTab.entries.forEach { tab ->
                val icon = when (tab) {
                    SurveyToolTab.UNIT_CONVERTER -> Icons.Rounded.SwapHoriz
                    SurveyToolTab.LENGTH_CONVERTER -> Icons.Rounded.LinearScale
                    SurveyToolTab.SHAPE_SURVEY -> Icons.Rounded.CropSquare
                    SurveyToolTab.COORDINATE_TRAVERSE -> Icons.Rounded.Explore
                    SurveyToolTab.KHATIAN_HISSA -> Icons.Rounded.Percent
                    SurveyToolTab.FARAIZ -> Icons.Rounded.Balance
                    SurveyToolTab.DEED_TAX -> Icons.Rounded.AccountBalance
                    SurveyToolTab.CHAIN_SCALE -> Icons.Rounded.Straighten
                    SurveyToolTab.FORMULA_SOLVER -> Icons.Rounded.Calculate
                }
                NeuPillChip(
                    text = tab.titleBn,
                    selected = activeTab == tab,
                    icon = icon,
                    testTag = "tool_tab_${tab.name.lowercase()}",
                    onClick = { activeTab = tab }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "বিশেষায়িত সার্ভে ও CAD টুলস মেনু (Specialized Tools)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenCogoEngine("traverse") },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("tools_open_cogo_btn")
                        ) {
                            Text("COGO ট্র্যাভার্স ও ইন্টারসেকশন", maxLines = 1)
                        }
                        Button(
                            onClick = { onOpenCadEngine("draw") },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("tools_open_cad_btn")
                        ) {
                            Text("2D CAD স্টুডিও", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onOpenDimensionSystem("linear") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("tools_open_dim_btn")
                        ) {
                            Text("CAD ডাইমেনশন (Linear/Angular/Leader)", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenWorkstationHub,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("tools_open_hub_btn")
                        ) {
                            Text("সকল আলাদা মডিউল", maxLines = 1)
                        }
                    }
                }
            }
            item {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 18 })
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "tools_tab_transition"
                ) { tab ->
                    when (tab) {
                        SurveyToolTab.UNIT_CONVERTER -> UnitConverterPanel()
                        SurveyToolTab.LENGTH_CONVERTER -> LengthConverterPanel()
                        SurveyToolTab.SHAPE_SURVEY -> ShapeSurveyPanel()
                        SurveyToolTab.COORDINATE_TRAVERSE -> TraverseCoordPanel()
                        SurveyToolTab.KHATIAN_HISSA -> KhatianHissaProPanel()
                        SurveyToolTab.FARAIZ -> FaraizProPanel()
                        SurveyToolTab.DEED_TAX -> DeedTaxPanel()
                        SurveyToolTab.CHAIN_SCALE -> ChainScalePanel()
                        SurveyToolTab.FORMULA_SOLVER -> FormulaAndUnknownSidePanel()
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        }
    }
}

@Composable
private fun LengthConverterPanel() {
    var inputValue by remember { mutableStateOf("66") }
    var selectedUnit by remember { mutableStateOf(LengthUnit.FEET) }

    val v = inputValue.toDoubleOrNull() ?: 0.0
    val converted = LandCalculators.convertLength(v, selectedUnit)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("দৈর্ঘ্য একক কনভার্টার", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("মান (${selectedUnit.labelBn})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LengthUnit.entries.forEach { u ->
                    NeuPillChip(
                        text = u.labelBn.substringBefore(" "),
                        selected = selectedUnit == u,
                        onClick = { selectedUnit = u }
                    )
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
            Text("রূপান্তরিত ফলাফল", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
            LengthUnit.entries.forEach { u ->
                val valNum = converted[u] ?: 0.0
                StatRow(
                    label = u.labelBn,
                    value = String.format(Locale.US, "%.4f", valNum),
                    highlight = u == LengthUnit.FEET || u == LengthUnit.LINK || u == LengthUnit.METER
                )
            }
        }
    }
}

@Composable
private fun UnitConverterPanel() {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()
    val profiles = snap.regionalUnits
    var selectedProfileId by remember(profiles) { mutableStateOf(profiles.firstOrNull()?.id ?: "bd_standard") }
    val activeProfile = profiles.find { it.id == selectedProfileId } ?: SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()

    var inputValue by remember { mutableStateOf("10") }
    var inputUnit by remember { mutableIntStateOf(0) }
    val units = listOf("শতাংশ", "কাঠা (আঞ্চলিক)", "বিঘা (আঞ্চলিক)", "একর", "বর্গফুট", "বর্গমিটার", "কানি (আঞ্চলিক)", "গণ্ডা (আঞ্চলিক)")

    var showCustomDialog by remember { mutableStateOf(false) }
    var customRegionName by remember { mutableStateOf("নিজস্ব আঞ্চলিক একক") }
    var customKathaSqFt by remember { mutableStateOf("720") }
    var customKaniDecimal by remember { mutableStateOf("40") }

    val v = inputValue.toDoubleOrNull() ?: 0.0
    val sqFeet = when (inputUnit) {
        0 -> v * LandCalculators.SQ_FT_PER_DECIMAL
        1 -> v * activeProfile.kathaSqFt
        2 -> v * activeProfile.bighaSqFt
        3 -> v * LandCalculators.SQ_FT_PER_ACRE
        4 -> v
        5 -> v * LandCalculators.SQ_FT_PER_SQ_METER
        6 -> v * activeProfile.kaniSqFt
        else -> v * activeProfile.gondaSqFt
    }
    val regRes = SurveyMathProEngine.convertSqFeetWithProfile(sqFeet, activeProfile)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("আঞ্চলিক প্রোফাইল (Configurable Regional Units)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { showCustomDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("নতুন প্রোফাইল", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { p ->
                    FilterChip(
                        selected = p.id == activeProfile.id,
                        onClick = { selectedProfileId = p.id },
                        label = { Text(p.regionNameBn, maxLines = 1) }
                    )
                }
            }

            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("মান (${units[inputUnit]})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                units.forEachIndexed { idx, label ->
                    NeuPillChip(
                        text = label,
                        selected = inputUnit == idx,
                        onClick = { inputUnit = idx }
                    )
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
            Text("ফলাফল (${activeProfile.regionNameBn})", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            StatRow("শতাংশ (Decimal)", String.format(Locale.US, "%.4f", regRes.decimal), highlight = true)
            StatRow("অযুতাংশ", String.format(Locale.US, "%.2f", regRes.ajutangsho))
            StatRow("কাঠা (${activeProfile.kathaSqFt.toInt()} ব.ফু)", String.format(Locale.US, "%.4f", regRes.katha), highlight = true)
            StatRow("বিঘা (${activeProfile.bighaSqFt.toInt()} ব.ফু)", String.format(Locale.US, "%.4f", regRes.bigha))
            StatRow("একর (১০০ শতাংশ)", String.format(Locale.US, "%.4f", regRes.acre))
            StatRow("কানি (${String.format(Locale.US, "%.1f", activeProfile.kaniSqFt / 435.6)} শতক)", String.format(Locale.US, "%.4f", regRes.kani), highlight = true)
            StatRow("গণ্ডা (১/২০ কানি)", String.format(Locale.US, "%.4f", regRes.gonda))
            StatRow("কড়া (১/৪ গণ্ডা)", String.format(Locale.US, "%.4f", regRes.kora))
            StatRow("ক্রান্তি ও তিল", String.format(Locale.US, "%.3f ক্রান্তি / %.1f তিল", regRes.kranti, regRes.til))
            StatRow("বর্গফুট / বর্গমিটার", String.format(Locale.US, "%.2f ব.ফু / %.2f ব.মি", regRes.sqFeet, regRes.sqMeters))
            StatRow(activeProfile.customUnitName, String.format(Locale.US, "%.4f", regRes.customUnitValue))
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("নতুন আঞ্চলিক একক কনফিগার করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = customRegionName, onValueChange = { customRegionName = it }, label = { Text("অঞ্চলের নাম") }, singleLine = true)
                    NumField("১ কাঠায় কত বর্গফুট? (যেমন 720 বা 1620)", customKathaSqFt, { customKathaSqFt = it }, Modifier.fillMaxWidth())
                    NumField("১ কানিতে কত শতাংশ? (যেমন 40 বা 120)", customKaniDecimal, { customKaniDecimal = it }, Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val kSq = customKathaSqFt.toDoubleOrNull()?.coerceAtLeast(100.0) ?: 720.0
                    val kaniDec = customKaniDecimal.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 40.0
                    val kaniSq = kaniDec * LandCalculators.SQ_FT_PER_DECIMAL
                    val newProf = RegionalUnitProfile(
                        id = "custom_${UUID.randomUUID().toString().take(6)}",
                        regionNameBn = customRegionName.ifBlank { "কাস্টম এলাকা" },
                        regionNameEn = "Custom Regional Unit",
                        kathaSqFt = kSq,
                        bighaSqFt = kSq * 20.0,
                        kaniSqFt = kaniSq,
                        gondaSqFt = kaniSq / 20.0,
                        koraSqFt = kaniSq / 80.0,
                        krantiSqFt = kaniSq / 240.0,
                        tilSqFt = kaniSq / 4800.0,
                        customUnitName = "কাস্টম কানি (${kaniDec.toInt()} শতক)",
                        customUnitSqFt = kaniSq
                    )
                    repo.saveRegionalUnitProfile(newProf)
                    selectedProfileId = newProf.id
                    showCustomDialog = false
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun TraverseCoordPanel() {
    var subMode by remember { mutableIntStateOf(0) } // 0=Cosine/Parallel, 1=Shoelace XY, 2=Bowditch Closed Traverse
    var sideA by remember { mutableStateOf("60") }
    var sideB by remember { mutableStateOf("50") }
    var angleC by remember { mutableStateOf("75") }
    var xyText by remember { mutableStateOf("0,0\n100,0\n105,80\n5,85") }
    var traverseText by remember { mutableStateOf("AB,90,100\nBC,180,80\nCD,270,100.2\nDA,0,79.9") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("অজানা বাহু ও সামান্তরিক", "কো-অর্ডিনেট (Shoelace)", "বদ্ধ ট্র্যাভার্স (Bowditch)").forEachIndexed { idx, t ->
                FilterChip(selected = subMode == idx, onClick = { subMode = idx }, label = { Text(t) })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (subMode) {
                0 -> {
                    Text("কোসাইন সূত্র: অজানা ৩য় বাহু এবং ক্ষেত্রফল", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("বাহু ১ / ভূমি", sideA, { sideA = it }, Modifier.weight(1f))
                        NumField("বাহু ২ / উচ্চতা", sideB, { sideB = it }, Modifier.weight(1f))
                        NumField("অন্তর্ভুক্ত কোণ (°)", angleC, { angleC = it }, Modifier.weight(1f))
                    }
                    val tri = SurveyMathProEngine.solveTriangleSideAndArea(
                        sideA.toDoubleOrNull() ?: 0.0,
                        sideB.toDoubleOrNull() ?: 0.0,
                        angleC.toDoubleOrNull() ?: 90.0
                    )
                    val para = SurveyMathProEngine.parallelogramSqFeet(sideA.toDoubleOrNull() ?: 0.0, sideB.toDoubleOrNull() ?: 0.0)
                    if (tri != null) {
                        StatRow("অজানা ৩য় বাহুর দৈর্ঘ্য", String.format(Locale.US, "%.3f ফু", tri.first), highlight = true)
                        StatRow("ত্রিভুজ ক্ষেত্রফল", String.format(Locale.US, "%.2f ব.ফু (%.3f শতক)", tri.second, tri.second / LandCalculators.SQ_FT_PER_DECIMAL))
                        StatRow("সামান্তরিক ক্ষেত্রফল (ভূমি×উচ্চতা)", String.format(Locale.US, "%.2f ব.ফু (%.3f শতক)", para, para / LandCalculators.SQ_FT_PER_DECIMAL))
                    }
                }
                1 -> {
                    Text("কো-অর্ডিনেট জ্যামিতি (প্রতি লাইনে X,Y লিখুন)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = xyText,
                        onValueChange = { xyText = it },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val pts = xyText.lines().mapNotNull { ln ->
                        val p = ln.split(",")
                        val x = p.getOrNull(0)?.trim()?.toDoubleOrNull()
                        val y = p.getOrNull(1)?.trim()?.toDoubleOrNull()
                        if (x != null && y != null) PolygonVertex(x, y) else null
                    }
                    val res = SurveyMathProEngine.computeCoordinateGeometry(pts)
                    if (res != null) {
                        StatRow("শুলেস ক্ষেত্রফল", String.format(Locale.US, "%.2f ব.ফু (%.4f শতক)", res.sqFeet, res.decimal), highlight = true)
                        StatRow("মোট পরিসীমা", String.format(Locale.US, "%.2f ফু", res.perimeterFt))
                        StatRow("ভরকেন্দ্র (Centroid)", String.format(Locale.US, "(%.2f, %.2f)", res.centroid.x, res.centroid.y))
                    }
                }
                else -> {
                    Text("Bowditch বদ্ধ ট্র্যাভার্স (Label,Bearing°,DistFt)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = traverseText,
                        onValueChange = { traverseText = it },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val legs = traverseText.lines().mapNotNull { ln ->
                        val p = ln.split(",")
                        val lbl = p.getOrNull(0)?.trim().orEmpty()
                        val brg = p.getOrNull(1)?.trim()?.toDoubleOrNull()
                        val dst = p.getOrNull(2)?.trim()?.toDoubleOrNull()
                        if (lbl.isNotBlank() && brg != null && dst != null) TraverseLegInput(lbl, brg, dst) else null
                    }
                    val res = SurveyMathProEngine.solveClosedTraverse(legs)
                    if (res != null) {
                        StatRow("সমন্বিত ক্ষেত্রফল", String.format(Locale.US, "%.2f ব.ফু (%.4f শতক)", res.adjustedAreaSqFt, res.adjustedDecimal), highlight = true)
                        StatRow("রৈখিক বন্ধন ত্রুটি (Misclosure)", String.format(Locale.US, "%.3f ফু (%s)", res.linearMisclosureFt, res.precisionRatio))
                        StatRow("মোট পরিসীমা", String.format(Locale.US, "%.2f ফু", res.totalPerimeterFt))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShapeSurveyPanel() {
    var shapeIdx by remember { mutableIntStateOf(0) }
    val shapes = listOf("৪ বাহু / অনিয়মিত", "ত্রিভুজ (হেরন)", "বৃত্ত / গোল", "ট্রাপিজিয়াম")

    var a by remember { mutableStateOf("60") }
    var b by remember { mutableStateOf("58") }
    var c by remember { mutableStateOf("40") }
    var d by remember { mutableStateOf("42") }
    var diag by remember { mutableStateOf("") }

    val sqFt = when (shapeIdx) {
        0 -> LandCalculators.fourSideSurveySqFeet(
            north = a.toDoubleOrNull() ?: 0.0,
            south = b.toDoubleOrNull() ?: 0.0,
            east = c.toDoubleOrNull() ?: 0.0,
            west = d.toDoubleOrNull() ?: 0.0,
            diagonal = diag.toDoubleOrNull()
        ).first
        1 -> LandCalculators.triangleHeronSqFeet(
            a.toDoubleOrNull() ?: 0.0,
            b.toDoubleOrNull() ?: 0.0,
            c.toDoubleOrNull() ?: 0.0
        ) ?: 0.0
        2 -> LandCalculators.circleAreaSqFeet(a.toDoubleOrNull() ?: 0.0)
        else -> LandCalculators.trapezoidAreaSqFeet(
            a.toDoubleOrNull() ?: 0.0,
            b.toDoubleOrNull() ?: 0.0,
            c.toDoubleOrNull() ?: 0.0
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            shapes.forEachIndexed { i, title ->
                NeuPillChip(text = title, selected = shapeIdx == i, onClick = { shapeIdx = i })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (shapeIdx) {
                0 -> {
                    Text("বাহুগুলোর মাপ (ফুটে লিখুন)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("উত্তর বাহু", a, { a = it }, Modifier.weight(1f))
                        NumField("দক্ষিণ বাহু", b, { b = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("পূর্ব বাহু", c, { c = it }, Modifier.weight(1f))
                        NumField("পশ্চিম বাহু", d, { d = it }, Modifier.weight(1f))
                    }
                    NumField("কর্ণ / ডায়াগোনাল (ঐচ্ছিক - নিখুঁত মাপের জন্য)", diag, { diag = it }, Modifier.fillMaxWidth())
                }
                1 -> {
                    Text("ত্রিভুজের ৩ বাহু (ফুট)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("বাহু ১", a, { a = it }, Modifier.weight(1f))
                        NumField("বাহু ২", b, { b = it }, Modifier.weight(1f))
                        NumField("বাহু ৩", c, { c = it }, Modifier.weight(1f))
                    }
                }
                2 -> {
                    Text("বৃত্তের ব্যাসার্ধ (ফুট)", style = MaterialTheme.typography.titleSmall)
                    NumField("ব্যাসার্ধ (Radius)", a, { a = it }, Modifier.fillMaxWidth())
                }
                else -> {
                    Text("সমান্তরাল ২ বাহু ও লম্ব দূরত্ব (ফুট)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("সমান্তরাল ১", a, { a = it }, Modifier.weight(1f))
                        NumField("সমান্তরাল ২", b, { b = it }, Modifier.weight(1f))
                    }
                    NumField("লম্ব উচ্চতা (Height)", c, { c = it }, Modifier.fillMaxWidth())
                }
            }
        }

        AreaBreakdownCard("জমির ক্ষেত্রফল", LandCalculators.fromSqFeet(sqFt))
    }
}

@Composable
private fun KhatianHissaPanel() {
    var totalDecimal by remember { mutableStateOf("100") }
    var ana by remember { mutableStateOf("4") }
    var gonda by remember { mutableStateOf("10") }
    var kora by remember { mutableStateOf("2") }
    var kranti by remember { mutableStateOf("0") }
    var til by remember { mutableStateOf("0") }

    val ratio = LandCalculators.hissaToRatio(
        ana = (ana.toIntOrNull() ?: 0).coerceIn(0, 16),
        gonda = (gonda.toIntOrNull() ?: 0).coerceIn(0, 19),
        kora = (kora.toIntOrNull() ?: 0).coerceIn(0, 3),
        kranti = (kranti.toIntOrNull() ?: 0).coerceIn(0, 2),
        til = (til.toIntOrNull() ?: 0).coerceIn(0, 19)
    )
    val shareDecimal = (totalDecimal.toDoubleOrNull() ?: 0.0) * ratio
    val res = LandCalculators.fromDecimal(shareDecimal)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("খতিয়ানের আনা-গণ্ডা হিস্যা হিসাব", style = MaterialTheme.typography.titleMedium)
            NumField("দাগের মোট জমি (শতাংশ)", totalDecimal, { totalDecimal = it }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField("আনা", ana, { ana = it }, Modifier.weight(1f))
                NumField("গণ্ডা", gonda, { gonda = it }, Modifier.weight(1f))
                NumField("কড়া", kora, { kora = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField("ক্রান্তি", kranti, { kranti = it }, Modifier.weight(1f))
                NumField("তিল", til, { til = it }, Modifier.weight(1f))
            }
            Text(
                text = String.format(Locale.US, "অংশের হার: %.4f%% (%.6f)", ratio * 100.0, ratio),
                style = MaterialTheme.typography.titleSmall,
                color = EmeraldPrimary
            )
        }
        AreaBreakdownCard("উক্ত হিস্যায় প্রাপ্য জমি", res)
    }
}

@Composable
private fun FaraizPanel() {
    var lawType by remember { mutableStateOf(InheritanceLawType.MUSLIM_SUNNI) }
    var totalDec by remember { mutableStateOf("50") }
    var deceasedMale by remember { mutableStateOf(true) }
    var hasSpouse by remember { mutableStateOf(true) }
    var sons by remember { mutableStateOf("2") }
    var daughters by remember { mutableStateOf("1") }
    var hasFather by remember { mutableStateOf(false) }
    var hasMother by remember { mutableStateOf(true) }
    var brothers by remember { mutableStateOf("0") }
    var sisters by remember { mutableStateOf("0") }

    val report = SurveyMathProEngine.calculateUniversalInheritance(
        lawType = lawType,
        totalDecimal = totalDec.toDoubleOrNull() ?: 0.0,
        deceasedIsMale = deceasedMale,
        hasSpouse = hasSpouse,
        sons = (sons.toIntOrNull() ?: 0).coerceAtLeast(0),
        daughters = (daughters.toIntOrNull() ?: 0).coerceAtLeast(0),
        hasFather = hasFather,
        hasMother = hasMother,
        brothers = (brothers.toIntOrNull() ?: 0).coerceAtLeast(0),
        sisters = (sisters.toIntOrNull() ?: 0).coerceAtLeast(0)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("উত্তরাধিকার আইন নির্বাচন (Rule-Set Engine)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InheritanceLawType.entries.forEach { law ->
                    FilterChip(
                        selected = lawType == law,
                        onClick = { lawType = law },
                        label = { Text(law.titleBn, maxLines = 1) }
                    )
                }
            }
            NumField("মোট জমি (শতাংশ)", totalDec, { totalDec = it }, Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("মৃত ব্যক্তি পুরুষ? (${if (deceasedMale) "পুরুষ" else "মহিলা"})")
                Switch(checked = deceasedMale, onCheckedChange = { deceasedMale = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (deceasedMale) "স্ত্রী জীবিত?" else "স্বামী জীবিত?")
                Switch(checked = hasSpouse, onCheckedChange = { hasSpouse = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("পুত্র সংখ্যা", sons, { sons = it }, Modifier.weight(1f))
                NumField("কন্যা সংখ্যা", daughters, { daughters = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("ভাই সংখ্যা", brothers, { brothers = it }, Modifier.weight(1f))
                NumField("বোন সংখ্যা", sisters, { sisters = it }, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("পিতা জীবিত?")
                Switch(checked = hasFather, onCheckedChange = { hasFather = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("মাতা জীবিত?")
                Switch(checked = hasMother, onCheckedChange = { hasMother = it })
            }
        }

        if (report.warnings.isNotEmpty() || report.requiresLegalReview) {
            LegalPrecisionNoticeBanner(
                text = (report.warnings + "আইনি সতর্কতা: ওয়ারিশ সংক্রান্ত চূড়ান্ত বণ্টনের আগে বিজ্ঞ আইনজীবী/মুফতির পর্যালোচনা নিন (${report.lawVersion})।")
                    .joinToString("\n• ")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("ওয়ারিশদের প্রাপ্য অংশ (${lawType.titleBn})", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            if (report.shares.isEmpty()) {
                Text("জমির পরিমাণ ও ওয়ারিশ নির্বাচন করুন।", style = MaterialTheme.typography.bodySmall)
            } else {
                report.shares.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.relationBn, style = MaterialTheme.typography.titleSmall)
                            if (item.count > 1) {
                                Text(
                                    text = String.format(Locale.US, "প্রত্যেকে: %.3f শতক", item.perPersonDecimal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.statusNote.isNotBlank()) {
                                Text(item.statusNote, style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%.3f শতাংশ", item.totalDecimal),
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f%%", item.totalFraction * 100.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeedTaxPanel() {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()
    var rateCfg by remember(snap.financialRate) { mutableStateOf(snap.financialRate) }

    var areaDecimalText by remember { mutableStateOf("10") }
    var customSalePriceText by remember { mutableStateOf("3200000") }
    var otherExpensesText by remember { mutableStateOf("25000") }
    var ownerSplitText by remember { mutableStateOf("2") }

    val fin = SurveyMathProEngine.calculateFinancialBreakdown(
        areaDecimal = areaDecimalText.toDoubleOrNull() ?: 0.0,
        customSalePriceBdt = customSalePriceText.toDoubleOrNull(),
        otherExpensesBdt = otherExpensesText.toDoubleOrNull() ?: 0.0,
        ownerShareCount = (ownerSplitText.toIntOrNull() ?: 1).coerceAtLeast(1),
        rates = rateCfg
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("আর্থিক মূল্যায়ন, মৌজা রেট, কর ও মালিকানা খরচ ভাগ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("জমির পরিমাণ (শতাংশ)", areaDecimalText, { areaDecimalText = it }, Modifier.weight(1f))
                NumField("মৌজা রেট / শতক", rateCfg.mouzaRatePerDecimalBdt.toLong().toString(), {
                    rateCfg = rateCfg.copy(mouzaRatePerDecimalBdt = it.toDoubleOrNull() ?: 0.0)
                }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("বিক্রয় / বাজার মূল্য (টাকা)", customSalePriceText, { customSalePriceText = it }, Modifier.weight(1f))
                NumField("অন্যান্য/আইনি খরচ", otherExpensesText, { otherExpensesText = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("অংশীদার সংখ্যা", ownerSplitText, { ownerSplitText = it }, Modifier.weight(0.7f))
                OutlinedTextField(
                    value = rateCfg.effectiveDate,
                    onValueChange = { rateCfg = rateCfg.copy(effectiveDate = it) },
                    label = { Text("রেট কার্যকরের তারিখ") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField("স্ট্যাম্প %", rateCfg.stampDutyPct.toString(), { rateCfg = rateCfg.copy(stampDutyPct = it.toDoubleOrNull() ?: 1.5) }, Modifier.weight(1f))
                NumField("রেজিঃ %", rateCfg.registrationFeePct.toString(), { rateCfg = rateCfg.copy(registrationFeePct = it.toDoubleOrNull() ?: 1.0) }, Modifier.weight(1f))
                NumField("স্থানীয় কর %", rateCfg.localGovtTaxPct.toString(), { rateCfg = rateCfg.copy(localGovtTaxPct = it.toDoubleOrNull() ?: 3.0) }, Modifier.weight(1f))
                NumField("উৎস কর %", rateCfg.sourceGainTaxPct.toString(), { rateCfg = rateCfg.copy(sourceGainTaxPct = it.toDoubleOrNull() ?: 4.0) }, Modifier.weight(1f))
            }
            Button(
                onClick = { repo.saveFinancialRate(rateCfg) },
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("এই এলাকার রেট প্রোফাইল সংরক্ষণ করুন")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("আর্থিক বিশ্লেষণ ফলাফল", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
            StatRow("মৌজা মূল্যায়ন (Mouza Value)", "${fin.mouzaTotalValueBdt.toLong()} টাকা")
            StatRow("স্ট্যাম্প + রেজিঃ + স্থানীয় + গেইন কর", "${(fin.stampDutyBdt + fin.registrationFeeBdt + fin.localGovtTaxBdt + fin.gainTaxBdt).toLong()} টাকা")
            StatRow("ই-নামজারি (Mutation) ফি", "${fin.mutationCostBdt.toLong()} টাকা")
            StatRow("বার্ষিক ভূমি উন্নয়ন কর (খাজনা)", "${fin.annualLandTaxBdt.toLong()} টাকা")
            StatRow("সর্বমোট সরকারি ও প্রক্রিয়া খরচ", "${fin.totalGovtAndProcessCostBdt.toLong()} টাকা", highlight = true)
            StatRow("মোট ক্রয়/অধিগ্রহণ ব্যয়", "${fin.totalAcquisitionCostBdt.toLong()} টাকা", highlight = true)
            StatRow("জনপ্রতি / অংশীদার প্রতি খরচ", "${fin.perShareCostBdt.toLong()} টাকা", highlight = true)
            StatRow(
                "আনুমানিক নিট লাভ / পার্থক্য",
                "${fin.netProfitOrLossBdt.toLong()} টাকা",
                highlight = fin.netProfitOrLossBdt >= 0
            )
        }
    }
}

@Composable
private fun ChainScalePanel() {
    var feetText by remember { mutableStateOf("66") }
    val feet = feetText.toDoubleOrNull() ?: 0.0
    val meters = feet * 0.3048
    val links = feet / 0.66
    val chains = feet / 66.0
    val hat = feet / 1.5
    val yards = feet / 3.0
    val inchesOn16Scale = feet / 330.0
    val inchesOn32Scale = feet / 165.0
    val inchesOn64Scale = feet / 82.5

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("গান্টার চেইন, লিংক ও মৌজা স্কেল", style = MaterialTheme.typography.titleMedium)
            NumField("দৈর্ঘ্য (ফুটে লিখুন)", feetText, { feetText = it }, Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatRow("মিটার (Meter)", String.format(Locale.US, "%.3f মি.", meters))
            StatRow("গান্টার লিংক (১ লিংক = ০.৬৬ ফু)", String.format(Locale.US, "%.2f লিংক", links))
            StatRow("গান্টার চেইন (১ চেইন = ৬৬ ফু)", String.format(Locale.US, "%.3f চেইন", chains))
            StatRow("হাত (১ হাত = ১৮ ইঞ্চি)", String.format(Locale.US, "%.2f হাত", hat))
            StatRow("গজ (Yard)", String.format(Locale.US, "%.2f গজ", yards))
            StatRow("১৬\"=১ মাইল নকশায় দূরত্ব", String.format(Locale.US, "%.4f ইঞ্চি", inchesOn16Scale), highlight = true)
            StatRow("৩২\"=১ মাইল নকশায় দূরত্ব", String.format(Locale.US, "%.4f ইঞ্চি", inchesOn32Scale), highlight = true)
            StatRow("৬৪\"=১ মাইল নকশায় দূরত্ব", String.format(Locale.US, "%.4f ইঞ্চি", inchesOn64Scale), highlight = true)
        }
    }
}

@Composable
private fun AreaBreakdownCard(title: String, res: LandAreaResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
        StatRow("শতাংশ (Decimal)", String.format(Locale.US, "%.4f", res.decimal), highlight = true)
        StatRow("কাঠা (৭২০ বর্গফুট)", String.format(Locale.US, "%.4f", res.katha), highlight = true)
        StatRow("বিঘা (২০ কাঠা)", String.format(Locale.US, "%.4f", res.bigha))
        StatRow("একর (১০০ শতাংশ)", String.format(Locale.US, "%.4f", res.acre))
        StatRow("অযুতাংশ", String.format(Locale.US, "%.2f", res.ajutangsho))
        StatRow("বর্গফুট (Sq.Ft)", String.format(Locale.US, "%.2f", res.sqFeet))
        StatRow("বর্গমিটার (Sq.M)", String.format(Locale.US, "%.2f", res.sqMeters))
        StatRow("বর্গহাত", String.format(Locale.US, "%.2f", res.sqHat))
        StatRow("হেক্টর (Hectare)", String.format(Locale.US, "%.5f", res.hectare))
    }
}

@Composable
private fun StatRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun NumField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}
