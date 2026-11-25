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
import com.example.engine.LandAreaResult
import com.example.engine.LandCalculators
import com.example.engine.LengthUnit
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import java.util.Locale

private enum class SurveyToolTab(val key: String, val titleBn: String) {
    UNIT_CONVERTER("unit", "জমির একক"),
    LENGTH_CONVERTER("len", "দৈর্ঘ্য মাপ"),
    SHAPE_SURVEY("shape", "আকৃতি মাপ"),
    KHATIAN_HISSA("hissa", "খতিয়ান হিস্যা"),
    FARAIZ("faraiz", "ওয়ারিশ/ফারায়েজ"),
    DEED_TAX("tax", "দলিল ও দাম"),
    CHAIN_SCALE("scale", "স্কেল ও চেইন")
}

@Composable
fun ToolsScreen(
    initialTabKey: String = "unit",
    onNavigateBack: () -> Unit,
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
                    SurveyToolTab.KHATIAN_HISSA -> Icons.Rounded.Percent
                    SurveyToolTab.FARAIZ -> Icons.Rounded.Balance
                    SurveyToolTab.DEED_TAX -> Icons.Rounded.AccountBalance
                    SurveyToolTab.CHAIN_SCALE -> Icons.Rounded.Straighten
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
                        SurveyToolTab.KHATIAN_HISSA -> KhatianHissaPanel()
                        SurveyToolTab.FARAIZ -> FaraizPanel()
                        SurveyToolTab.DEED_TAX -> DeedTaxPanel()
                        SurveyToolTab.CHAIN_SCALE -> ChainScalePanel()
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
    var inputValue by remember { mutableStateOf("10") }
    var inputUnit by remember { mutableIntStateOf(0) }
    val units = listOf("শতাংশ", "কাঠা", "বিঘা", "একর", "বর্গফুট", "বর্গমিটার")

    val v = inputValue.toDoubleOrNull() ?: 0.0
    val res = when (inputUnit) {
        0 -> LandCalculators.fromDecimal(v)
        1 -> LandCalculators.fromKatha(v)
        2 -> LandCalculators.fromBigha(v)
        3 -> LandCalculators.fromAcre(v)
        4 -> LandCalculators.fromSqFeet(v)
        else -> LandCalculators.fromSqMeters(v)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("পরিমাণ লিখুন ও একক বাছুন", style = MaterialTheme.typography.titleMedium)
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

        AreaBreakdownCard("ফলাফল (বাংলাদেশ স্ট্যান্ডার্ড)", res)
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
    var totalDec by remember { mutableStateOf("50") }
    var deceasedMale by remember { mutableStateOf(true) }
    var hasSpouse by remember { mutableStateOf(true) }
    var sons by remember { mutableStateOf("2") }
    var daughters by remember { mutableStateOf("1") }
    var hasFather by remember { mutableStateOf(false) }
    var hasMother by remember { mutableStateOf(true) }

    val shares = LandCalculators.calculateFaraiz(
        totalDecimal = totalDec.toDoubleOrNull() ?: 0.0,
        deceasedIsMale = deceasedMale,
        hasSpouse = hasSpouse,
        sons = (sons.toIntOrNull() ?: 0).coerceAtLeast(0),
        daughters = (daughters.toIntOrNull() ?: 0).coerceAtLeast(0),
        hasFather = hasFather,
        hasMother = hasMother
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("মুসলিম উত্তরাধিকার (ফারায়েজ) জমি বণ্টন", style = MaterialTheme.typography.titleMedium)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("ওয়ারিশদের প্রাপ্য অংশ", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
            if (shares.isEmpty()) {
                Text("জমির পরিমাণ ও ওয়ারিশ নির্বাচন করুন।", style = MaterialTheme.typography.bodySmall)
            } else {
                shares.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.relationBn, style = MaterialTheme.typography.titleSmall)
                            if (item.count > 1) {
                                Text(
                                    text = String.format(Locale.US, "প্রত্যেকে: %.2f শতক", item.perPersonDecimal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%.2f শতাংশ", item.totalDecimal),
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
    var valueText by remember { mutableStateOf("1000000") }
    var isCity by remember { mutableStateOf(false) }
    var gainTaxPct by remember { mutableStateOf("4") }

    val breakdown = LandCalculators.estimateDeedCost(
        deedValueBdt = valueText.toDoubleOrNull() ?: 0.0,
        isCityArea = isCity,
        gainTaxPercent = gainTaxPct.toDoubleOrNull() ?: 4.0
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("সাফ-কবলা দলিল খরচ অনুমান (টাকায়)", style = MaterialTheme.typography.titleMedium)
            NumField("দলিলের মূল্য (টাকা)", valueText, { valueText = it }, Modifier.fillMaxWidth())
            NumField("উৎস / গেইন কর (%)", gainTaxPct, { gainTaxPct = it }, Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("সিটি কর্পোরেশন / ক্যান্টনমেন্ট এলাকা?")
                Switch(checked = isCity, onCheckedChange = { isCity = it })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("সরকারি ফি বিবরণ", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
            StatRow("স্ট্যাম্প শুল্ক (১.৫%)", "${breakdown.stampDuty.toLong()} টাকা")
            StatRow("রেজিস্ট্রেশন ফি (১.০%)", "${breakdown.registrationFee.toLong()} টাকা")
            StatRow("স্থানীয় সরকার কর (${if (isCity) "২%" else "৩%"})", "${breakdown.localGovtTax.toLong()} টাকা")
            StatRow("উৎস কর", "${breakdown.sourceGainTax.toLong()} টাকা")
            StatRow("সর্বমোট আনুমানিক সরকারি খরচ", "${breakdown.totalGovtCost.toLong()} টাকা", highlight = true)
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
