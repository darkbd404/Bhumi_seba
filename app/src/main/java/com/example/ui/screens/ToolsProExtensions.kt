package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.engine.InheritanceAndHissaProEngine
import com.example.engine.LandCalculators
import com.example.model.CrossKhatianHolding
import com.example.model.DeceasedHeirRedistributionReport
import com.example.model.HanafiHeirInput
import com.example.model.HinduInheritanceRules
import com.example.model.HinduSchool
import com.example.model.HissaOwnerEntry
import com.example.model.InheritanceLawType
import com.example.model.UnknownSideShape
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale
import java.util.UUID

@Composable
fun KhatianHissaProPanel() {
    var subTab by remember { mutableIntStateOf(0) } // 0=১৬-আনা বহুদাগ খতিয়ান, 1=আনা-গণ্ডা রূপান্তরকারী, 2=ক্রস-খতিয়ান মোট হিস্যা
    var dagTotalDecText by remember { mutableStateOf("100") }
    var soldDeductPct by remember { mutableFloatStateOf(0f) }

    val owners = remember {
        mutableStateListOf(
            HissaOwnerEntry("o1", "আব্দুল করিম", ana = 8, gonda = 0, kora = 0, kranti = 0, til = 0),
            HissaOwnerEntry("o2", "সখিনা বেগম", ana = 5, gonda = 6, kora = 2, kranti = 2, til = 0),
            HissaOwnerEntry("o3", "রফিক উদ্দিন", ana = 2, gonda = 13, kora = 1, kranti = 0, til = 14)
        )
    }

    // Bidirectional converter state
    var convDecInput by remember { mutableStateOf("25") }
    var convAna by remember { mutableStateOf("4") }
    var convGonda by remember { mutableStateOf("0") }
    var convKora by remember { mutableStateOf("0") }
    var convKranti by remember { mutableStateOf("0") }
    var convTil by remember { mutableStateOf("0") }

    // Cross-khatian holdings list
    val holdings = remember {
        mutableStateListOf(
            CrossKhatianHolding("h1", "আরএস ৪১২", "১০৪২", 48.0, 0.5),
            CrossKhatianHolding("h2", "আরএস ৫২০", "৩১৪", 22.5, 0.25),
            CrossKhatianHolding("h3", "বিএস ১৮", "১০৯", 16.0, 1.0)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("১৬-আনা বহু-মালিক হিস্যা", "আনা-গণ্ডা ↔ শতাংশ কনভার্টার", "ক্রস-খতিয়ান সামগ্রিক অংশ").forEachIndexed { idx, title ->
                FilterChip(
                    selected = subTab == idx,
                    onClick = { subTab = idx },
                    label = { Text(title) }
                )
            }
        }

        when (subTab) {
            0 -> {
                val safeDec = dagTotalDecText.toDoubleOrNull() ?: 100.0
                val hissaReport = remember(safeDec, owners.toList(), soldDeductPct) {
                    InheritanceAndHissaProEngine.analyzeHissaOwners(
                        dagTotalDecimal = safeDec,
                        owners = owners.toList(),
                        soldOrDeductedRatio = (soldDeductPct / 100f).toDouble()
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("খতিয়ানের ১৬-আনা বহু-মালিক হিস্যা ও কর্তন হিসাব", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    OutlinedTextField(
                        value = dagTotalDecText,
                        onValueChange = { dagTotalDecText = it },
                        label = { Text("দাগের মোট জমি (শতাংশ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hissa_total_dec_input")
                    )

                    Text(
                        String.format(Locale.US, "বিক্রয় / কর্তন হার: %.1f%%", soldDeductPct),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = soldDeductPct,
                        onValueChange = { soldDeductPct = it },
                        valueRange = 0f..100f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মালিক তালিকা (${owners.size} জন)", fontWeight = FontWeight.SemiBold)
                        Button(
                            onClick = {
                                owners.add(
                                    HissaOwnerEntry(
                                        id = "own_${UUID.randomUUID().toString().take(6)}",
                                        name = "মালিক ${owners.size + 1}",
                                        ana = 1
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) { Text("+ মালিক যোগ") }
                    }

                    owners.forEachIndexed { idx, owner ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = owner.name,
                                        onValueChange = { newName ->
                                            owners[idx] = owner.copy(name = newName)
                                        },
                                        label = { Text("নাম") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { owners.removeAt(idx) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = owner.ana.toString(),
                                        onValueChange = { v -> owners[idx] = owner.copy(ana = (v.toIntOrNull() ?: 0).coerceIn(0, 16)) },
                                        label = { Text("আনা") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = owner.gonda.toString(),
                                        onValueChange = { v -> owners[idx] = owner.copy(gonda = (v.toIntOrNull() ?: 0).coerceIn(0, 19)) },
                                        label = { Text("গণ্ডা") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = owner.kora.toString(),
                                        onValueChange = { v -> owners[idx] = owner.copy(kora = (v.toIntOrNull() ?: 0).coerceIn(0, 3)) },
                                        label = { Text("কড়া") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = owner.kranti.toString(),
                                        onValueChange = { v -> owners[idx] = owner.copy(kranti = (v.toIntOrNull() ?: 0).coerceIn(0, 2)) },
                                        label = { Text("ক্রান্তি") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = owner.til.toString(),
                                        onValueChange = { v -> owners[idx] = owner.copy(til = (v.toIntOrNull() ?: 0).coerceIn(0, 19)) },
                                        label = { Text("তিল") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = hissaReport.statusNote,
                        fontWeight = FontWeight.Bold,
                        color = if (hissaReport.isExact16Ana) EmeraldPrimary else Color(0xFFD97706)
                    )
                    Text(
                        String.format(
                            Locale.US,
                            "মোট অংশ: %.4f (১৬ আনার শতকরা %.2f%%) | দাগে মোট: %.2f শতক",
                            hissaReport.totalRatio,
                            (hissaReport.totalAnaEquivalent / 16.0) * 100.0,
                            hissaReport.dagTotalDecimal
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    hissaReport.owners.forEach { o ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(o.name, fontWeight = FontWeight.SemiBold)
                                Text("${o.anaGondaText} (${String.format(Locale.US, "%.4f", o.ratio)})", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    String.format(Locale.US, "%.3f শতক", o.decimal),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    String.format(Locale.US, "%.2f কাঠা • %.0f ব.ফু.", o.katha, o.sqFt),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("দ্বিমুখী আনা-গণ্ডা ↔ দশমিক/শতক রূপান্তর", fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                    // Direction A: Ana-Gonda -> Decimal
                    Text("ক) আনা-গণ্ডা-কড়া-ক্রান্তি-তিল লিখুন:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = convAna, onValueChange = { convAna = it }, label = { Text("আনা") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = convGonda, onValueChange = { convGonda = it }, label = { Text("গণ্ডা") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = convKora, onValueChange = { convKora = it }, label = { Text("কড়া") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = convKranti, onValueChange = { convKranti = it }, label = { Text("ক্রান্তি") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = convTil, onValueChange = { convTil = it }, label = { Text("তিল") }, modifier = Modifier.weight(1f))
                    }
                    val convertedRatio = InheritanceAndHissaProEngine.anaGondaToRatio(
                        ana = convAna.toIntOrNull() ?: 0,
                        gonda = convGonda.toIntOrNull() ?: 0,
                        kora = convKora.toIntOrNull() ?: 0,
                        kranti = convKranti.toIntOrNull() ?: 0,
                        til = convTil.toIntOrNull() ?: 0
                    )
                    Text(
                        String.format(Locale.US, "অংশের হার: %.6f (%.4f%%)", convertedRatio, convertedRatio * 100.0),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    // Direction B: Decimal ratio -> Ana-Gonda breakdown
                    Text("খ) দশমিক অনুপাত বা শতক থেকে আনা-গণ্ডা রূপান্তর:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = convDecInput,
                        onValueChange = { convDecInput = it },
                        label = { Text("অনুপাত (০.০–১.০) অথবা শতকে হিস্যা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val rawNum = convDecInput.toDoubleOrNull() ?: 0.0
                    val ratioToBreakdown = if (rawNum > 1.0) (rawNum / 100.0).coerceIn(0.0, 1.0) else rawNum.coerceIn(0.0, 1.0)
                    val breakdown = InheritanceAndHissaProEngine.ratioToAnaGondaBreakdown(ratioToBreakdown)
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("আনা-গণ্ডা রূপান্তর ফলাফল:", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            Text(breakdown.formattedBn(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "অনুপাত: %.6f | শতকরা: %.4f%%", breakdown.ratio, breakdown.percentage), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            else -> {
                // Cross-Khatian owner holding
                var khNo by remember { mutableStateOf("আরএস ৪১২") }
                var dagNo by remember { mutableStateOf("১০৪২") }
                var dagDec by remember { mutableStateOf("48") }
                var ownerRatio by remember { mutableStateOf("0.5") }

                val (aggDec, aggKatha, aggSqFt) = remember(holdings.toList()) {
                    InheritanceAndHissaProEngine.aggregateCrossKhatianOwnerShare(holdings.toList())
                }

                Column(
                    modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ক্রস-খতিয়ান সামগ্রিক মালিকানা একত্রিতকরণ", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = khNo, onValueChange = { khNo = it }, label = { Text("খতিয়ান") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dagNo, onValueChange = { dagNo = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = dagDec, onValueChange = { dagDec = it }, label = { Text("দাগের জমি (শতক)") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        OutlinedTextField(value = ownerRatio, onValueChange = { ownerRatio = it }, label = { Text("অংশ (০–১)") }, singleLine = true, modifier = Modifier.weight(0.9f))
                        Button(
                            onClick = {
                                if (dagNo.isNotBlank()) {
                                    holdings.add(
                                        CrossKhatianHolding(
                                            id = "ck_${UUID.randomUUID().toString().take(6)}",
                                            khatianNo = khNo.ifBlank { "-" },
                                            dagNo = dagNo.trim(),
                                            dagTotalDecimal = dagDec.toDoubleOrNull() ?: 0.0,
                                            ownerRatio = (ownerRatio.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0)
                                        )
                                    )
                                    dagNo = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) { Text("+ যোগ") }
                    }

                    holdings.forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "খতিয়ান ${h.khatianNo} • দাগ ${h.dagNo} (${h.dagTotalDecimal} শতক × ${h.ownerRatio}) = ${String.format(Locale.US, "%.3f", h.dagTotalDecimal * h.ownerRatio)} শতক",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { holdings.remove(h) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                String.format(Locale.US, "সকল খতিয়ান মিলিয়ে মালিকের মোট প্রাপ্য: %.3f শতাংশ", aggDec),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                String.format(Locale.US, "সমান: %.2f কাঠা  |  %.1f বর্গফুট (মোট %dটি হোল্ডিং)", aggKatha, aggSqFt, holdings.size),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaraizProPanel() {
    var isMuslimSunni by remember { mutableStateOf(true) }
    var totalDecText by remember { mutableStateOf("50") }
    var deceasedMale by remember { mutableStateOf(true) }
    var spouseCount by remember { mutableIntStateOf(1) }
    var sonsText by remember { mutableStateOf("2") }
    var daughtersText by remember { mutableStateOf("1") }
    var sonSonsText by remember { mutableStateOf("0") }
    var sonDaughtersText by remember { mutableStateOf("0") }
    var hasFather by remember { mutableStateOf(false) }
    var hasMother by remember { mutableStateOf(true) }
    var hasPaternalGrandfather by remember { mutableStateOf(false) }
    var hasPaternalGrandmother by remember { mutableStateOf(false) }
    var hasMaternalGrandmother by remember { mutableStateOf(false) }
    var fullBrothersText by remember { mutableStateOf("0") }
    var fullSistersText by remember { mutableStateOf("0") }
    var paternalBrothersText by remember { mutableStateOf("0") }
    var paternalSistersText by remember { mutableStateOf("0") }

    // Hindu Law specifics
    var hinduSchool by remember { mutableStateOf(HinduSchool.DAYABHAGA) }
    var widowGetsEqual by remember { mutableStateOf(true) }
    var daughtersEqualMitakshara by remember { mutableStateOf(true) }
    var motherGetsEqual by remember { mutableStateOf(true) }

    // Manasikha (Redistribution of Deceased Heir) Dialog State
    var showManasikhaDialog by remember { mutableStateOf(false) }
    var manasikhaReport by remember { mutableStateOf<DeceasedHeirRedistributionReport?>(null) }
    var targetDeceasedHeirLabel by remember { mutableStateOf("পুত্র ১") }
    var targetDeceasedDecimal by remember { mutableDoubleStateOf(15.0) }

    val hanafiInput = remember(
        totalDecText, deceasedMale, spouseCount, sonsText, daughtersText,
        sonSonsText, sonDaughtersText, hasFather, hasMother, hasPaternalGrandfather,
        hasPaternalGrandmother, hasMaternalGrandmother, fullBrothersText, fullSistersText,
        paternalBrothersText, paternalSistersText
    ) {
        HanafiHeirInput(
            totalDecimal = totalDecText.toDoubleOrNull() ?: 50.0,
            deceasedIsMale = deceasedMale,
            spouseCount = spouseCount,
            sons = (sonsText.toIntOrNull() ?: 0).coerceAtLeast(0),
            daughters = (daughtersText.toIntOrNull() ?: 0).coerceAtLeast(0),
            sonSons = (sonSonsText.toIntOrNull() ?: 0).coerceAtLeast(0),
            sonDaughters = (sonDaughtersText.toIntOrNull() ?: 0).coerceAtLeast(0),
            hasFather = hasFather,
            hasMother = hasMother,
            hasPaternalGrandfather = hasPaternalGrandfather,
            hasPaternalGrandmother = hasPaternalGrandmother,
            hasMaternalGrandmother = hasMaternalGrandmother,
            fullBrothers = (fullBrothersText.toIntOrNull() ?: 0).coerceAtLeast(0),
            fullSisters = (fullSistersText.toIntOrNull() ?: 0).coerceAtLeast(0),
            paternalBrothers = (paternalBrothersText.toIntOrNull() ?: 0).coerceAtLeast(0),
            paternalSisters = (paternalSistersText.toIntOrNull() ?: 0).coerceAtLeast(0)
        )
    }

    val hanafiReport = remember(hanafiInput) {
        InheritanceAndHissaProEngine.calculateHanafiFaraiz(hanafiInput)
    }

    val hinduReport = remember(
        totalDecText, deceasedMale, spouseCount, sonsText, daughtersText, hasFather, hasMother,
        hinduSchool, widowGetsEqual, daughtersEqualMitakshara, motherGetsEqual
    ) {
        InheritanceAndHissaProEngine.calculateHinduInheritance(
            totalDecimal = totalDecText.toDoubleOrNull() ?: 50.0,
            deceasedIsMale = deceasedMale,
            hasSpouse = spouseCount > 0,
            sons = (sonsText.toIntOrNull() ?: 0).coerceAtLeast(0),
            daughters = (daughtersText.toIntOrNull() ?: 0).coerceAtLeast(0),
            hasFather = hasFather,
            hasMother = hasMother,
            rules = HinduInheritanceRules(
                school = hinduSchool,
                widowGetsEqualSonShare = widowGetsEqual,
                daughtersEqualInMitakshara = daughtersEqualMitakshara,
                motherGetsEqualOnPartition = motherGetsEqual
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("উত্তরাধিকার ও ফারায়েজ প্রো ইঞ্জিন", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMuslimSunni,
                    onClick = { isMuslimSunni = true },
                    label = { Text("মুসলিম (হানাফী ফারায়েজ)") }
                )
                FilterChip(
                    selected = !isMuslimSunni,
                    onClick = { isMuslimSunni = false },
                    label = { Text("হিন্দু উত্তরাধিকার (দায়ভাগ/মিতাক্ষরা)") }
                )
            }
            OutlinedTextField(
                value = totalDecText,
                onValueChange = { totalDecText = it },
                label = { Text("বণ্টনযোগ্য মোট জমি (শতাংশ)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("faraiz_total_dec_input")
            )
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
                Text(if (deceasedMale) "স্ত্রী সংখ্যা: $spouseCount জন" else "স্বামী জীবিত? (${if (spouseCount > 0) "হ্যাঁ" else "না"})")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (deceasedMale) {
                        (0..4).forEach { count ->
                            FilterChip(
                                selected = spouseCount == count,
                                onClick = { spouseCount = count },
                                label = { Text("$count") }
                            )
                        }
                    } else {
                        Switch(checked = spouseCount > 0, onCheckedChange = { spouseCount = if (it) 1 else 0 })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = sonsText, onValueChange = { sonsText = it }, label = { Text("পুত্র") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = daughtersText, onValueChange = { daughtersText = it }, label = { Text("কন্যা") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            if (isMuslimSunni) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sonSonsText, onValueChange = { sonSonsText = it }, label = { Text("নাতি (পুত্রের পুত্র)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sonDaughtersText, onValueChange = { sonDaughtersText = it }, label = { Text("নাতনি (পুত্রের কন্যা)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
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

            if (isMuslimSunni) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = hasPaternalGrandfather, onClick = { hasPaternalGrandfather = !hasPaternalGrandfather }, label = { Text("দাদা (পিতার পিতা)") })
                    FilterChip(selected = hasPaternalGrandmother, onClick = { hasPaternalGrandmother = !hasPaternalGrandmother }, label = { Text("দাদি (পিতার মাতা)") })
                    FilterChip(selected = hasMaternalGrandmother, onClick = { hasMaternalGrandmother = !hasMaternalGrandmother }, label = { Text("নানি (মাতার মাতা)") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = fullBrothersText, onValueChange = { fullBrothersText = it }, label = { Text("আপন ভাই") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fullSistersText, onValueChange = { fullSistersText = it }, label = { Text("আপন বোন") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = paternalBrothersText, onValueChange = { paternalBrothersText = it }, label = { Text("সৎ ভাই") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = paternalSistersText, onValueChange = { paternalSistersText = it }, label = { Text("সৎ বোন") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = hinduSchool == HinduSchool.DAYABHAGA, onClick = { hinduSchool = HinduSchool.DAYABHAGA }, label = { Text("দায়ভাগ (বাংলাদেশ)") })
                    FilterChip(selected = hinduSchool == HinduSchool.MITAKSHARA, onClick = { hinduSchool = HinduSchool.MITAKSHARA }, label = { Text("মিতাক্ষরা (সহদায়াদ)") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("বিধবা স্ত্রীর পুত্রের সমান অংশ?")
                    Switch(checked = widowGetsEqual, onCheckedChange = { widowGetsEqual = it })
                }
                if (hinduSchool == HinduSchool.MITAKSHARA) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("কন্যার সহদায়াদ সমান অধিকার?")
                        Switch(checked = daughtersEqualMitakshara, onCheckedChange = { daughtersEqualMitakshara = it })
                    }
                }
            }
        }

        // Results Section
        val activeShares = if (isMuslimSunni) hanafiReport.shares else hinduReport.shares

        Column(
            modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isMuslimSunni) "ওয়ারিশদের প্রাপ্য অংশ (হানাফী ফারায়েজ)" else "ওয়ারিশদের অংশ (${hinduSchool.titleBn})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                if (isMuslimSunni && activeShares.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val firstShare = activeShares.firstOrNull { it.totalDecimal > 0.0 }
                            targetDeceasedHeirLabel = firstShare?.relationBn ?: "মৃত ওয়ারিশ"
                            targetDeceasedDecimal = firstShare?.totalDecimal ?: 10.0
                            showManasikhaDialog = true
                        }
                    ) {
                        Text("মানাসিখা (২য় ধাপ)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (isMuslimSunni) {
                Surface(
                    color = (if (hanafiReport.awlApplied || hanafiReport.raddApplied) Color(0xFFE8F5E9) else EmeraldPrimary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(hanafiReport.awlOrRaddSummary, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        if (hanafiReport.blockedHeirsNotes.isNotEmpty()) {
                            Text(
                                "হজব/বঞ্চিত ওয়ারিশ: ${hanafiReport.blockedHeirsNotes.joinToString(" | ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            activeShares.forEach { sh ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sh.relationBn, fontWeight = FontWeight.Bold)
                            Text(sh.shareRuleNote, style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                            Text(
                                "আনা-গণ্ডা: ${sh.anaGonda.formattedBn()} (${String.format(Locale.US, "%.4f%%", sh.totalFraction * 100.0)})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                String.format(Locale.US, "%.3f শতক", sh.totalDecimal),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                String.format(Locale.US, "%.2f কাঠা • %.2f বিঘা", sh.totalKatha, sh.totalBigha),
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (sh.count > 1) {
                                Text(
                                    String.format(Locale.US, "জনপ্রতি: %.3f শতক", sh.perPersonDecimal),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF558B2F)
                                )
                            }
                        }
                    }
                }
            }
        }

        manasikhaReport?.let { mRep ->
            Column(
                modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "মানাসিখা ফলাফল: ${mRep.deceasedHeirLabel}-এর প্রাপ্য ${String.format(Locale.US, "%.3f", mRep.inheritedDecimal)} শতকের ২য় ধাপ বণ্টন",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                mRep.combinedLivingShares.forEach { cs ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cs.relationBn, fontWeight = FontWeight.SemiBold)
                        Text(String.format(Locale.US, "%.3f শতক (%.2f%%)", cs.totalDecimal, cs.totalFraction * 100.0), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showManasikhaDialog) {
        var mSons by remember { mutableStateOf("1") }
        var mDaughters by remember { mutableStateOf("1") }
        var mSpouse by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showManasikhaDialog = false },
            title = { Text("মানাসিখা (Manasikha) — ২য় ধাপের ওয়ারিশ বণ্টন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("মৃত ওয়ারিশ: $targetDeceasedHeirLabel (${String.format(Locale.US, "%.3f", targetDeceasedDecimal)} শতক)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("উক্ত মৃত ব্যক্তির স্ত্রী/স্বামী জীবিত?")
                        Switch(checked = mSpouse, onCheckedChange = { mSpouse = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = mSons, onValueChange = { mSons = it }, label = { Text("পুত্র") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = mDaughters, onValueChange = { mDaughters = it }, label = { Text("কন্যা") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val s2Input = HanafiHeirInput(
                        totalDecimal = targetDeceasedDecimal,
                        deceasedIsMale = true,
                        spouseCount = if (mSpouse) 1 else 0,
                        sons = (mSons.toIntOrNull() ?: 0).coerceAtLeast(0),
                        daughters = (mDaughters.toIntOrNull() ?: 0).coerceAtLeast(0)
                    )
                    manasikhaReport = InheritanceAndHissaProEngine.redistributeDeceasedHeirShare(
                        deceasedHeirLabel = targetDeceasedHeirLabel,
                        inheritedDecimal = targetDeceasedDecimal,
                        stage2Input = s2Input
                    )
                    showManasikhaDialog = false
                }) { Text("পুনর্বণ্টন হিসাব") }
            },
            dismissButton = { TextButton(onClick = { showManasikhaDialog = false }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun FormulaAndUnknownSidePanel() {
    var mode by remember { mutableIntStateOf(0) } // 0=অজানা বাহু, 1=বৃত্ত ও সেক্টর, 2=চিটশিট
    var selectedShape by remember { mutableStateOf(UnknownSideShape.RECTANGLE) }

    var targetAreaSqFtText by remember { mutableStateOf("1000") }
    var v1Text by remember { mutableStateOf("40") }
    var v2Text by remember { mutableStateOf("30") }
    var v3Text by remember { mutableStateOf("35") }
    var v4Text by remember { mutableStateOf("45") }

    // Circle/Sector state
    var radiusText by remember { mutableStateOf("25") }
    var angleText by remember { mutableStateOf("90") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("অজানা বাহু সলভার (৭ আকৃতি)", "বৃত্তচাপ, বৃত্তাংশ ও সেক্টর", "অফলাইন ফর্মুলা চিট-শিট").forEachIndexed { idx, title ->
                FilterChip(selected = mode == idx, onClick = { mode = idx }, label = { Text(title) })
            }
        }

        when (mode) {
            0 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("অজানা বাহু ও উচ্চতা বের করার সলভার", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        UnknownSideShape.entries.forEach { sh ->
                            FilterChip(
                                selected = selectedShape == sh,
                                onClick = { selectedShape = sh },
                                label = { Text(sh.titleBn.substringBefore(" ")) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = targetAreaSqFtText,
                        onValueChange = { targetAreaSqFtText = it },
                        label = { Text("লক্ষ্য ক্ষেত্রফল (বর্গফুট)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (selectedShape) {
                        UnknownSideShape.RECTANGLE -> {
                            OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("জানা বাহু / দৈর্ঘ্য (ফুট)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        UnknownSideShape.TRAPEZIUM_HEIGHT -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("সমান্তরাল ১ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = v2Text, onValueChange = { v2Text = it }, label = { Text("সমান্তরাল ২ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                        UnknownSideShape.TRAPEZIUM_SIDE -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("উচ্চতা h (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = v2Text, onValueChange = { v2Text = it }, label = { Text("জানা সমান্তরাল বাহু (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                        UnknownSideShape.TRIANGLE_BASE_HEIGHT -> {
                            OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("ভূমি (ফুট)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        UnknownSideShape.TRIANGLE_SAS -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("বাহু a (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = v2Text, onValueChange = { v2Text = it }, label = { Text("অন্তর্ভুক্ত কোণ C (°)") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                        UnknownSideShape.PARALLELOGRAM -> {
                            OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("ভূমি (ফুট)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        UnknownSideShape.QUAD_WITH_DIAGONAL -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = v1Text, onValueChange = { v1Text = it }, label = { Text("কর্ণ d (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = v2Text, onValueChange = { v2Text = it }, label = { Text("বাহু ১ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = v3Text, onValueChange = { v3Text = it }, label = { Text("বাহু ২ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = v4Text, onValueChange = { v4Text = it }, label = { Text("বাহু ৩ (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    val solveRes = remember(selectedShape, targetAreaSqFtText, v1Text, v2Text, v3Text, v4Text) {
                        InheritanceAndHissaProEngine.solveUnknownSide(
                            shape = selectedShape,
                            areaSqFt = targetAreaSqFtText.toDoubleOrNull() ?: 1000.0,
                            v1 = v1Text.toDoubleOrNull() ?: 40.0,
                            v2 = v2Text.toDoubleOrNull() ?: 30.0,
                            v3 = v3Text.toDoubleOrNull() ?: 35.0,
                            v4 = v4Text.toDoubleOrNull() ?: 45.0
                        )
                    }

                    Surface(
                        color = (if (solveRes.isValid) EmeraldPrimary else MaterialTheme.colorScheme.error).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (solveRes.isValid) {
                                Text("${solveRes.labelBn}:", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    String.format(Locale.US, "%.3f ফুট (%s)", solveRes.solvedValue, LandCalculators.formatFeetInchesSuta(solveRes.solvedValue)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                solveRes.secondaryValue?.let { sec ->
                                    Text(String.format(Locale.US, "২য় মান: %.3f ফুট (%s)", sec, LandCalculators.formatFeetInches(sec)), fontWeight = FontWeight.SemiBold)
                                }
                                Text("সূত্র: ${solveRes.formulaUsed}", style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                                Text("ধাপ: ${solveRes.stepByStep}", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text(solveRes.errorMsg ?: "গণনায় ত্রুটি", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().neuSurface(cornerRadius = 20.dp, elevation = 5.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("বৃত্ত, বৃত্তচাপ, জ্যা ও সেক্টর সলভার", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = radiusText, onValueChange = { radiusText = it }, label = { Text("ব্যাসার্ধ r (ফুট)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = angleText, onValueChange = { angleText = it }, label = { Text("কেন্দ্রীয় কোণ θ (°)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    val secRes = remember(radiusText, angleText) {
                        InheritanceAndHissaProEngine.solveCircleSector(
                            radiusFt = radiusText.toDoubleOrNull() ?: 25.0,
                            centralAngleDeg = angleText.toDoubleOrNull() ?: 90.0
                        )
                    }
                    Surface(color = EmeraldPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(String.format(Locale.US, "সেক্টর ক্ষেত্রফল: %.2f ব.ফু (%.3f শতক)", secRes.sectorAreaSqFt, secRes.sectorDecimal), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            Text(String.format(Locale.US, "বৃত্তচাপের দৈর্ঘ্য (Arc): %.2f ফুট (%s)", secRes.arcLengthFt, LandCalculators.formatFeetInches(secRes.arcLengthFt)))
                            Text(String.format(Locale.US, "জ্যা-এর দৈর্ঘ্য (Chord): %.2f ফুট", secRes.chordLengthFt))
                            Text(String.format(Locale.US, "সেগমেন্ট ক্ষেত্রফল (চাপ ও জ্যা এর মাঝের অংশ): %.2f ব.ফু", secRes.segmentAreaSqFt))
                            Text(String.format(Locale.US, "সম্পূর্ণ বৃত্তের ক্ষেত্রফল: %.2f ব.ফু (পরিধি: %.2f')", secRes.fullCircleAreaSqFt, secRes.circumferenceFt))
                        }
                    }
                }
            }

            else -> {
                InheritanceAndHissaProEngine.OFFLINE_FORMULA_CHEAT_SHEET.forEach { entry ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.titleBn, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            Text("সূত্র: ${entry.formulaText}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text("উদাহরণ: ${entry.workedExampleBn}", style = MaterialTheme.typography.bodySmall)
                            Text("আমিন টিপস: ${entry.verificationNote}", style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                        }
                    }
                }
            }
        }
    }
}
