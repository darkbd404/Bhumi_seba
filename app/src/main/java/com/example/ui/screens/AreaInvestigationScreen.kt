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
import com.example.data.ExpansionRepository
import com.example.engine.ExpansionMathAndSurveyEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.LandCalculators
import com.example.engine.UniversalExportEngine
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale
import kotlin.math.abs

@Composable
fun AreaInvestigationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }

    var tab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // F01 & F02 Reconciliation
    var recKhatianDec by remember { mutableStateOf("40.0") }
    var recInheritedDec by remember { mutableStateOf("20.0") }
    var recSoldDec by remember { mutableStateOf("5.0") }
    var recFieldDec by remember { mutableStateOf("14.6") }
    var reconcileText by remember { mutableStateOf("") }

    // F03 6-Source Comparison & F04 Boundary Diagonal Check
    var srcDeed by remember { mutableStateOf("15.0") }
    var srcKhatian by remember { mutableStateOf("15.0") }
    var srcMap by remember { mutableStateOf("14.8") }
    var srcSat by remember { mutableStateOf("14.4") }
    var srcGps by remember { mutableStateOf("14.5") }
    var srcTs by remember { mutableStateOf("14.62") }
    var sideN by remember { mutableStateOf("80") }
    var sideS by remember { mutableStateOf("80") }
    var sideE by remember { mutableStateOf("79") }
    var sideW by remember { mutableStateOf("79") }
    var diag1 by remember { mutableStateOf("112.4") }

    // F05 & F06 Road + Obstruction Deduction -> Net Usable Land
    var grossDec by remember { mutableStateOf("25.0") }
    var roadLenFt by remember { mutableStateOf("90") }
    var roadWidFt by remember { mutableStateOf("10") }
    var pondSqFt by remember { mutableStateOf("1200") }
    var canalSqFt by remember { mutableStateOf("0") }
    var buildingSqFt by remember { mutableStateOf("1400") }
    var usableText by remember { mutableStateOf("") }

    // F07-09 Frontage Division Feasibility & Valuation Comparison
    var totalFrontageFt by remember { mutableStateOf("72") }
    var lotDepthFt by remember { mutableStateOf("95") }
    var numShares by remember { mutableStateOf("3") }
    var minLegalFrontageFt by remember { mutableStateOf("20") }
    var rateCommercial by remember { mutableStateOf("550000") }
    var rateResidential by remember { mutableStateOf("320000") }
    var rateAgri by remember { mutableStateOf("95000") }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "জমি ও ক্ষেত্রফল তদন্ত ল্যাব (F01–F09)",
                subtitleEn = "Reconciliation, 6-Source Diff, Road/Water Exclusion, Frontage & Valuation",
                helpTitle = "ক্ষেত্রফল তদন্ত ল্যাব নির্দেশিকা",
                helpSteps = listOf(
                    "রেকর্ড বনাম ওয়ারিশ/বিক্রি/সরেজমিন মাপের ঘাটতি বা বাড়তি সহজেই মিলিয়ে দেখুন।",
                    "রাস্তা, পুকুর, খাল ও ভবন বাদ দিয়ে নিট ব্যবহারযোগ্য জমি বের করুন।",
                    "মুখ-চওড়া (Frontage) ও ন্যূনতম সম্মুখ দৈর্ঘ্য অনুযায়ী বিভাজনযোগ্যতা যাচাই করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "area_investigation_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NeuPillChip("F01-04 রেকর্ড ও ৬-উৎস তুলনা", tab == 0, testTag = "area_inv_tab_0") { tab = 0 } }
                item { NeuPillChip("F05-06 রাস্তা ও বাধামুক্ত নিট জমি", tab == 1, testTag = "area_inv_tab_1") { tab = 1 } }
                item { NeuPillChip("F07-09 ফ্রন্টেজ বিভাজন ও মূল্য", tab == 2, testTag = "area_inv_tab_2") { tab = 2 } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("F01 & F02. পূর্ব রেকর্ড, অংশ ও সরেজমিন সমন্বয়", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(recKhatianDec, { recKhatianDec = it }, label = { Text("মূল খতিয়ান (শতক)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(recInheritedDec, { recInheritedDec = it }, label = { Text("প্রাপ্য/কেনা অংশ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(recSoldDec, { recSoldDec = it }, label = { Text("পূর্ব বিক্রি/বাদ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(recFieldDec, { recFieldDec = it }, label = { Text("সরেজমিন মাপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        val r = ExpansionMathAndSurveyEngine.reconcileLandRecords(
                                            recKhatianDec.toDoubleOrNull() ?: 0.0,
                                            recInheritedDec.toDoubleOrNull() ?: 0.0,
                                            recSoldDec.toDoubleOrNull() ?: 0.0,
                                            recFieldDec.toDoubleOrNull() ?: 0.0
                                        )
                                        reconcileText = "• কাগজপত্র মোতাবেক নিট প্রাপ্য: ${String.format(Locale.US, "%.3f", r.netRecordDecimal)} শতক\n• অবস্থান: ${r.statusLabel} (অমিল: ${String.format(Locale.US, "%.2f%%", r.discrepancyPct)})"
                                        repo.logCalculation("Part F01", "ক্ষেত্রফল সমন্বয়", "${r.statusLabel} (${String.format(Locale.US, "%.2f%%", r.discrepancyPct)})")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("calc_reconcile_btn")
                                ) { Text("সমন্বয় হিসাব করুন") }
                                if (reconcileText.isNotBlank()) Text(reconcileText, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)

                                Text("F03 & F04. ৬-উৎস তুলনা ও কর্ণ সামঞ্জস্য", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(srcDeed, { srcDeed = it }, label = { Text("দলিল") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(srcKhatian, { srcKhatian = it }, label = { Text("খতিয়ান") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(srcMap, { srcMap = it }, label = { Text("ম্যাপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(srcSat, { srcSat = it }, label = { Text("স্যাটেলাইট") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(srcGps, { srcGps = it }, label = { Text("GPS") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(srcTs, { srcTs = it }, label = { Text("TotalSt") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val vals = listOfNotNull(
                                    srcDeed.toDoubleOrNull(), srcKhatian.toDoubleOrNull(), srcMap.toDoubleOrNull(),
                                    srcSat.toDoubleOrNull(), srcGps.toDoubleOrNull(), srcTs.toDoubleOrNull()
                                )
                                if (vals.size >= 2) {
                                    val spread = vals.max() - vals.min()
                                    Text("৬-উৎসের সর্বোচ্চ-সর্বনিম্ন ব্যবধান: ${String.format(Locale.US, "%.3f শতক", spread)}", fontWeight = FontWeight.Bold, color = if (spread > 0.5) Color(0xFFD97706) else EmeraldPrimary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(sideN, { sideN = it }, label = { Text("N ft") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(sideS, { sideS = it }, label = { Text("S ft") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(sideE, { sideE = it }, label = { Text("E ft") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(sideW, { sideW = it }, label = { Text("W ft") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(diag1, { diag1 = it }, label = { Text("কর্ণ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val dN = sideN.toDoubleOrNull() ?: 0.0
                                val dE = sideE.toDoubleOrNull() ?: 0.0
                                val dS = sideS.toDoubleOrNull() ?: 0.0
                                val dW = sideW.toDoubleOrNull() ?: 0.0
                                val dg = diag1.toDoubleOrNull() ?: 0.0
                                val t1 = LandCalculators.triangleHeronSqFeet(dN, dE, dg)
                                val t2 = LandCalculators.triangleHeronSqFeet(dS, dW, dg)
                                val possible = t1 != null && t2 != null
                                val heronSqFt = if (possible) (t1!! + t2!!) else 0.0
                                Text(
                                    if (possible) String.format(Locale.US, "কর্ণ দ্বারা প্রকৃত ক্ষেত্রফল: %.2f ব.ফু (%.3f শতক) ✓", heronSqFt, heronSqFt / 435.6)
                                    else "⚠ ত্রিভুজ শর্ত ভঙ্গ হয়েছে — কর্ণের মাপ পুনরায় দেখুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (possible) EmeraldPrimary else Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    1 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("F05 & F06. রাস্তা, পুকুর, খাল ও ভবন বাদে নিট জমি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(grossDec, { grossDec = it }, label = { Text("মোট দাগ/প্লটের জমি (শতাংশ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(roadLenFt, { roadLenFt = it }, label = { Text("রাস্তার দৈর্ঘ্য (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(roadWidFt, { roadWidFt = it }, label = { Text("রাস্তার প্রস্থ (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(pondSqFt, { pondSqFt = it }, label = { Text("পুকুর/ডোবা (ব.ফু)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(canalSqFt, { canalSqFt = it }, label = { Text("খাল/নালা (ব.ফু)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(buildingSqFt, { buildingSqFt = it }, label = { Text("স্থাপনা/গাছ (ব.ফু)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        val grossSq = (grossDec.toDoubleOrNull() ?: 0.0) * LandCalculators.SQ_FT_PER_DECIMAL
                                        val u = ExpansionMathAndSurveyEngine.computeNetUsableLand(
                                            grossSqFt = grossSq,
                                            roadLengthFt = roadLenFt.toDoubleOrNull() ?: 0.0,
                                            roadWidthFt = roadWidFt.toDoubleOrNull() ?: 0.0,
                                            pondSqFt = pondSqFt.toDoubleOrNull() ?: 0.0,
                                            canalSqFt = canalSqFt.toDoubleOrNull() ?: 0.0,
                                            buildingSqFt = buildingSqFt.toDoubleOrNull() ?: 0.0,
                                            otherRestrictionSqFt = 0.0
                                        )
                                        usableText = String.format(
                                            Locale.US,
                                            "• মোট জমি: %.1f ব.ফু\n• রাস্তা বাদ: %.1f ব.ফু | বাধা/জলাশয় বাদ: %.1f ব.ফু\n• নিট ব্যবহারযোগ্য জমি: %.1f ব.ফু = %.3f শতক (%.3f কাঠা)",
                                            u.grossAreaSqFt, u.roadDeductionSqFt, u.obstructionDeductionSqFt, u.netUsableSqFt, u.netUsableDecimal, u.netUsableKatha
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("calc_net_usable_btn")
                                ) { Text("নিট ব্যবহারযোগ্য জমি বের করুন") }
                                if (usableText.isNotBlank()) Text(usableText, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                            }
                        }
                    }

                    2 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("F07–F09. মুখ-চওড়া (Frontage) বিভাজন সম্ভাব্যতা ও মূল্য তুলনা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(totalFrontageFt, { totalFrontageFt = it }, label = { Text("মোট রাস্তার মুখ (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(lotDepthFt, { lotDepthFt = it }, label = { Text("গভীরতা (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(numShares, { numShares = it }, label = { Text("ভাগ সংখ্যা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(minLegalFrontageFt, { minLegalFrontageFt = it }, label = { Text("ন্যূনতম প্রয়োজনীয় মুখ (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val f = totalFrontageFt.toDoubleOrNull() ?: 0.0
                                val n = (numShares.toIntOrNull() ?: 1).coerceAtLeast(1)
                                val minF = minLegalFrontageFt.toDoubleOrNull() ?: 15.0
                                val eachF = f / n
                                val feasible = eachF >= minF
                                Surface(
                                    color = if (feasible) EmeraldPrimary.copy(alpha = 0.09f) else Color(0xFFC62828).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        String.format(
                                            Locale.US,
                                            "প্রতি ভাগে রাস্তার মুখ: %.2f ft (গভীরতা %.1f ft)\nসিদ্ধান্ত: %s",
                                            eachF,
                                            lotDepthFt.toDoubleOrNull() ?: 0.0,
                                            if (feasible) "বিভাজনযোগ্য (নূন্যতম %.1f ft শর্ত পূরণ ✓)".format(minF)
                                            else "সতর্কতা: ন্যূনতম %.1f ft মুখের চেয়ে কম! ভিতরে যাতায়াতের জন্য করিডোর/রাস্তা রাখতে হবে।".format(minF)
                                        ),
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text("F09. তুলনামূলক জমি মূল্য প্রাক্কলন (Estimated Valuation)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                val decVal = (f * (lotDepthFt.toDoubleOrNull() ?: 0.0)) / 435.6
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(rateCommercial, { rateCommercial = it }, label = { Text("বাণিজ্যিক ৳/শতক") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(rateResidential, { rateResidential = it }, label = { Text("আবাসিক ৳/শতক") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(rateAgri, { rateAgri = it }, label = { Text("কৃষি ৳/শতক") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val cVal = decVal * (rateCommercial.toDoubleOrNull() ?: 0.0)
                                val rVal = decVal * (rateResidential.toDoubleOrNull() ?: 0.0)
                                val aVal = decVal * (rateAgri.toDoubleOrNull() ?: 0.0)
                                Text(
                                    String.format(Locale.US, "আনুমানিক মূল্য (%.2f শতক):\n• বাণিজ্যিক: ৳%,.0f  • আবাসিক: ৳%,.0f  • কৃষি: ৳%,.0f", decVal, cVal, rVal, aVal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary
                                )
                                OutlinedButton(
                                    onClick = {
                                        val body = "$reconcileText\n$usableText\nপ্রতি ভাগের মুখ: ${String.format(Locale.US, "%.2f ft", eachF)}\nবাণিজ্যিক প্রাক্কলন: ৳${String.format(Locale.US, "%,.0f", cVal)}"
                                        val bmp = UniversalExportEngine.renderDocumentReportBitmap("ক্ষেত্রফল তদন্ত প্রতিবেদন", "F01-F09 Area Investigation", body)
                                        val res = ExportStreamWriter.savePdfBitmap(context, bmp, "area_investigation_${System.currentTimeMillis()}")
                                        toast = "PDF সংরক্ষিত: ${res.displayPath}"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("তদন্ত প্রতিবেদন PDF সেভ করুন") }
                            }
                        }
                    }
                }
            }
        }
        BottomSwipeToastBanner(toast, { toast = null }, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}
