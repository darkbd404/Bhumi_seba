package com.example.ui.screens

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppPreferences
import com.example.data.ExpansionRepository
import com.example.data.MasterCadRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.KhatianProEngine
import com.example.engine.LandCalculators
import com.example.engine.UniversalExportEngine
import com.example.model.DagIndexCrossRefItem
import com.example.model.EncroachmentOverlapReport
import com.example.model.PolygonVertex
import com.example.model.TaxRateSchedule
import com.example.model.ThreeWayAreaCompareResult
import com.example.model.UnionRegisterEntry
import com.example.model.UnionRegisterKind
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale
import java.util.UUID

@Composable
fun AcLandOfficeWorkbenchScreen(
    initialSection: String = "registers",
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val masterRepo = remember(context) { MasterCadRepository.getInstance(context) }
    val expRepo = remember(context) { ExpansionRepository.getInstance(context) }
    val appPrefs = remember(context) { AppPreferences(context) }
    val masterSnap by masterRepo.snapshot.collectAsStateWithLifecycle()
    val expSnap by expRepo.snapshot.collectAsStateWithLifecycle()
    val customKhatians by appPrefs.customKhatiansFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var tab by remember(initialSection) {
        mutableIntStateOf(
            when (initialSection) {
                "khatian" -> 0
                "tax" -> 2
                "cases" -> 3
                "registration" -> 4
                else -> 1
            }
        )
    }
    var toast by remember { mutableStateOf<String?>(null) }

    // Tab 0: Khatian Comparison Checker
    var khFormerType by remember { mutableStateOf("RS খতিয়ান (সাবেক)") }
    var khFormerDag by remember { mutableStateOf("১০৪২") }
    var khFormerDec by remember { mutableStateOf("25.00") }
    var khCurrentType by remember { mutableStateOf("BS খতিয়ান (হাল)") }
    var khCurrentDag by remember { mutableStateOf("২১২৫") }
    var khCurrentDec by remember { mutableStateOf("24.79") }
    var khShareRatio by remember { mutableStateOf("0.50") }

    // Tab 0 Extensions: Dag Index, 3-Way Compare & Encroachment
    var dagIndexQuery by remember { mutableStateOf("") }
    var threeWayMapDec by remember { mutableStateOf("25.00") }
    var threeWayKhatianDec by remember { mutableStateOf("24.79") }
    var threeWayFieldDec by remember { mutableStateOf("24.85") }
    var threeWayTolPct by remember { mutableStateOf("2.0") }
    var formerPolyWidth by remember { mutableStateOf("100") }
    var formerPolyHeight by remember { mutableStateOf("100") }
    var overlapShiftX by remember { mutableStateOf("15") }
    var overlapShiftY by remember { mutableStateOf("10") }

    // Tab 1: 15 Union Land Registers
    var selectedRegKind by remember { mutableStateOf<UnionRegisterKind?>(null) }
    var regKindForNew by remember { mutableStateOf(UnionRegisterKind.HOLDING_REG) }
    var regRefNo by remember { mutableStateOf("হোল্ডিং-৩০৫") }
    var regMouza by remember { mutableStateOf("পুবাইল") }
    var regKhatianDag by remember { mutableStateOf("খঃ ৪১২ | দাগ ১০৪২") }
    var regPerson by remember { mutableStateOf("মোঃ রফিকুল ইসলাম") }
    var regAmountOrArea by remember { mutableStateOf("২৪.৭৯ শতক") }
    var regDate by remember { mutableStateOf("২০২৬-১১-২৭") }
    var regStatus by remember { mutableStateOf("হালনাগাদ") }

    // Tab 2: Dated Tax Schedule & LD Tax Calculator
    var taxSchedule by remember(masterSnap.taxRateSchedule) { mutableStateOf(masterSnap.taxRateSchedule) }
    var taxLandCategory by remember { mutableStateOf(0) } // 0: Agri <=25Bigha, 1: Agri>25B, 2: Non-Agri Res, 3: Comm, 4: Ind
    var taxAreaDec by remember { mutableStateOf("24.79") }
    var taxArrearYears by remember { mutableStateOf("3") }
    var dakhilaRefNo by remember { mutableStateOf("") }
    var dakhilaVerified by remember { mutableStateOf(false) }

    // Tab 4: Sub-Registry 11 Deed Fee Calculator
    val deedCategories = listOf(
        "সাফ-কবলা (বিক্রয় দলিল)", "হেবা (রক্তসম্পর্কের দান)", "হেবা-বিল-এওয়াজ", "দানপত্র (বাইরে)",
        "বণ্টননামা (Partition)", "বায়নাপত্র (Bayna)", "এওয়াজ / বিনিময়", "বন্ধক / মর্টগেজ",
        "লিজ / পত্তনি", "আমমোক্তারনামা", "ওসীয়তনামা / উইল"
    )
    var selectedDeedCatIdx by remember { mutableIntStateOf(0) }
    var deedPropValue by remember { mutableStateOf("2500000") }
    var isMunicipalArea by remember { mutableStateOf(false) }

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
                titleBn = "খতিয়ান, এসি ল্যান্ড ও রেজিস্ট্রি ওয়ার্কবেঞ্চ",
                subtitleEn = "Khatian Compare • 15 Union Registers • Dated LD Tax • Case & 11 Sub-Registry Deeds",
                helpTitle = "ভূমি অফিস ও রেজিস্ট্রি ওয়ার্কবেঞ্চ গাইড",
                helpSteps = listOf(
                    "খতিয়ান ট্যাবে CS/SA/RS/BS/BRS/BDS সাবেক বনাম হাল দাগ ও জমির পরিমাণ তুলনা করুন এবং প্রাচীন কাগজ শৈলীতে প্রিভিউ দেখুন।",
                    "১৫ রেজিস্টার ট্যাবে মৌজা, হোল্ডিং, দাখিলা, বকেয়া, নামজারিসহ ১৫টি ইউনিয়ন ভূমি অফিস রেজিস্টার ব্যবস্থাপনা ও XLSX/PDF রপ্তানি করুন।",
                    "খাজনা ও রেজিস্ট্রি ট্যাবে তারিখভিত্তিক খাজনা হার, ৬.২৫% বকেয়া সুদ এবং ১১ ধরনের দলিলের স্ট্যাম্প ও ফি হিসাব করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "ac_land_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    0 to "খতিয়ান তুলনা",
                    1 to "১৫ ইউনিয়ন রেজিস্টার",
                    2 to "খাজনা ও হার সূচি",
                    3 to "কেস ও পোর্টাল",
                    4 to "সাব-রেজিস্ট্রি (১১ দলিল)"
                )
                items(tabs) { (idx, label) ->
                    NeuPillChip(
                        text = label,
                        selected = tab == idx,
                        testTag = "ac_tab_$idx",
                        onClick = { tab = idx }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    // ---------- Tab 0: Khatian & Land Record Computer Workstation ----------
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("১. সাবেক বনাম হাল খতিয়ান ও দাগ তুলনা (CS/SA/RS/BS/BRS/BDS)", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = khFormerType, onValueChange = { khFormerType = it }, label = { Text("সাবেক জরিপ") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = khFormerDag, onValueChange = { khFormerDag = it }, label = { Text("সাবেক দাগ") }, singleLine = true, modifier = Modifier.weight(0.7f))
                                    OutlinedTextField(value = khFormerDec, onValueChange = { khFormerDec = it }, label = { Text("শতাংশ") }, singleLine = true, modifier = Modifier.weight(0.8f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = khCurrentType, onValueChange = { khCurrentType = it }, label = { Text("হাল জরিপ") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = khCurrentDag, onValueChange = { khCurrentDag = it }, label = { Text("হাল দাগ") }, singleLine = true, modifier = Modifier.weight(0.7f))
                                    OutlinedTextField(value = khCurrentDec, onValueChange = { khCurrentDec = it }, label = { Text("শতাংশ") }, singleLine = true, modifier = Modifier.weight(0.8f))
                                }
                                OutlinedTextField(value = khShareRatio, onValueChange = { khShareRatio = it }, label = { Text("খতিয়ানে অংশ / হিস্যা (যেমন 0.50 বা 0.25)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                val fDec = khFormerDec.toDoubleOrNull() ?: 0.0
                                val cDec = khCurrentDec.toDoubleOrNull() ?: 0.0
                                val ratio = (khShareRatio.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0)
                                val diff = cDec - fDec
                                val shareDec = cDec * ratio
                                val shareSqFt = shareDec * LandCalculators.SQ_FT_PER_DECIMAL
                                val summaryText = """
                                    সাবেক রেকর্ড: $khFormerType | দাগ $khFormerDag | মোট ${String.format(Locale.US, "%.2f", fDec)} শতক
                                    হাল রেকর্ড: $khCurrentType | দাগ $khCurrentDag | মোট ${String.format(Locale.US, "%.2f", cDec)} শতক
                                    ক্ষেত্রফল পার্থক্য (হাল - সাবেক): ${String.format(Locale.US, "%+.2f", diff)} শতক (${if (kotlin.math.abs(diff) > 0.25) "তদন্তযোগ্য পার্থক্য" else "গ্রহণযোগ্য সীমা"})
                                    প্রযোজ্য অংশ ($ratio): ${String.format(Locale.US, "%.3f", shareDec)} শতক (${String.format(Locale.US, "%.1f", shareSqFt)} বর্গফুট)
                                    সংরক্ষিত কাস্টম খতিয়ান সংখ্যা: ${customKhatians.size}টি
                                """.trimIndent()

                                VintageStampPaperCard(
                                    headerTitle = "খতিয়ান ও রেকর্ড যাচাই সারসংক্ষেপ (খসড়া)",
                                    stampBadge = "খতিয়ান তুলনা ওয়ার্কশিট",
                                    serialNumber = "KH-CMP-2026"
                                ) {
                                    Text(summaryText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Serif, color = Color(0xFF2B1C10))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                                title = "খতিয়ান ও দাগ তুলনা প্রতিবেদন",
                                                stampValueLabel = "রেকর্ড যাচাই সারসংক্ষেপ",
                                                serialTag = "KH-${khCurrentDag}",
                                                bodyText = summaryText
                                            )
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Khatian_Compare_${System.currentTimeMillis()}")
                                            toast = "PDF সেভ: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ভিন্টেজ PDF") }
                                    Button(
                                        onClick = {
                                            val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                                title = "খতিয়ান ও দাগ তুলনা প্রতিবেদন",
                                                stampValueLabel = "রেকর্ড যাচাই সারসংক্ষেপ",
                                                serialTag = "KH-${khCurrentDag}",
                                                bodyText = summaryText
                                            )
                                            val res = ExportStreamWriter.savePngBitmap(context, bmp, "Khatian_Compare_${System.currentTimeMillis()}")
                                            toast = "PNG সেভ: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ভিন্টেজ PNG") }
                                }
                            }
                        }

                        // Item 2: Dag Index Cross-Reference Search across Custom Khatians
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("২. দাগ সূচি (Dag Index) ও ক্রস-রেফারেন্স সার্চ", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                Text(
                                    text = "সংরক্ষিত কাস্টম খতিয়ানসমূহ থেকে দাগ নম্বর, খতিয়ান নং, মৌজা বা মালিকের নাম দিয়ে নিমেষে খুঁজে বের করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = dagIndexQuery,
                                    onValueChange = { dagIndexQuery = it },
                                    label = { Text("দাগ, খতিয়ান, মৌজা বা মালিক খুঁজুন...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val indexResults = remember(customKhatians, dagIndexQuery) {
                                    KhatianProEngine.buildDagCrossReferenceIndex(customKhatians, dagIndexQuery)
                                }

                                if (indexResults.isEmpty()) {
                                    Text(
                                        text = if (customKhatians.isEmpty()) "এখনো কোনো কাস্টম খতিয়ান সংরক্ষিত নেই।" else "কোনো দাগের মিল পাওয়া যায়নি।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text("প্রাপ্ত দাগ ফলাফল: ${indexResults.size}টি", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    indexResults.take(6).forEach { item ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "দাগ নং: ${item.dagNumber} • ${item.khatianType} নং ${item.khatianNumber}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "মৌজা: ${item.mouzaName} | শ্রেণী: ${item.landClass} | অংশ শতক: ${String.format(Locale.US, "%.2f", item.thisKhatianDecimal)} (দাগে: ${String.format(Locale.US, "%.2f", item.totalDagDecimal)})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "মালিকগণ: ${item.ownersSummary}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TealSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Item 3: 3-Way Area Comparison (Map vs Khatian vs Field)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("৩. ৩-মুখী এলাকা তুলনা (নকশা বনাম খতিয়ান বনাম মাঠ)", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                Text(
                                    text = "নকশার পরিমাপ, খতিয়ানের রেকর্ড এবং মাঠপর্যায়ের সরেজমিন পরিমাপের ত্রিমুখী যাচাই ও সহনশীল সীমা (Tolerance %) পরীক্ষা।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = threeWayMapDec,
                                        onValueChange = { threeWayMapDec = it },
                                        label = { Text("নকশার জমি (শতক)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = threeWayKhatianDec,
                                        onValueChange = { threeWayKhatianDec = it },
                                        label = { Text("খতিয়ান (শতক)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = threeWayFieldDec,
                                        onValueChange = { threeWayFieldDec = it },
                                        label = { Text("মাঠপর্যায়ে (শতক)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = threeWayTolPct,
                                        onValueChange = { threeWayTolPct = it },
                                        label = { Text("সহনশীল সীমা (%)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val mDec = threeWayMapDec.toDoubleOrNull() ?: 0.0
                                val kDec = threeWayKhatianDec.toDoubleOrNull() ?: 0.0
                                val fDec = threeWayFieldDec.toDoubleOrNull() ?: 0.0
                                val tolPct = threeWayTolPct.toDoubleOrNull() ?: 2.0
                                val threeWay = remember(mDec, kDec, fDec, tolPct) {
                                    KhatianProEngine.compareThreeWayAreas(mDec, kDec, fDec, tolPct)
                                }

                                Surface(
                                    color = if (threeWay.withinTolerance) EmeraldPrimary.copy(alpha = 0.08f) else Color(0xFFD97706).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (threeWay.withinTolerance) "✓ সহনশীল সীমার ভেতরে মিল আছে" else "⚠ সর্বোচ্চ গরমিল লক্ষ্য করা গেছে",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (threeWay.withinTolerance) EmeraldPrimary else Color(0xFFB45309)
                                        )
                                        Text(
                                            text = "নকশা ↔ খতিয়ান: ${String.format(Locale.US, "%+.2f", threeWay.mapVsKhatianDiffDec)} শতক (${String.format(Locale.US, "%.2f", threeWay.mapVsKhatianDiffPct)}%) | মাঠ ↔ খতিয়ান: ${String.format(Locale.US, "%+.2f", threeWay.fieldVsKhatianDiffDec)} শতক (${String.format(Locale.US, "%.2f", threeWay.fieldVsKhatianDiffPct)}%)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = threeWay.verdictBn,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Item 4: Dag Overlap & Encroachment Demarcation Analyzer
                        item {
                            val fW = formerPolyWidth.toDoubleOrNull() ?: 100.0
                            val fH = formerPolyHeight.toDoubleOrNull() ?: 100.0
                            val sX = overlapShiftX.toDoubleOrNull() ?: 15.0
                            val sY = overlapShiftY.toDoubleOrNull() ?: 10.0

                            val formerPoly = remember(fW, fH) {
                                listOf(
                                    PolygonVertex(0.0, 0.0),
                                    PolygonVertex(fW, 0.0),
                                    PolygonVertex(fW, fH),
                                    PolygonVertex(0.0, fH)
                                )
                            }
                            val currentPoly = remember(fW, fH, sX, sY) {
                                listOf(
                                    PolygonVertex(sX, sY),
                                    PolygonVertex(sX + fW, sY),
                                    PolygonVertex(sX + fW, sY + fH),
                                    PolygonVertex(sX, sY + fH)
                                )
                            }
                            val overlapReport = remember(khFormerDag, khCurrentDag, formerPoly, currentPoly) {
                                KhatianProEngine.analyzeDagOverlapAndEncroachment(
                                    formerLabel = "সাবেক দাগ $khFormerDag",
                                    currentLabel = "হাল দাগ $khCurrentDag",
                                    formerPolygon = formerPoly,
                                    currentPolygon = currentPoly
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("৪. দাগ ম্যাচিং, ওভারল্যাপ ও Encroachment বিশ্লেষণ", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                Text(
                                    text = "সাবেক ও হাল দাগের জ্যামিতিক ওভারল্যাপ, পার্শ্ববর্তী দাগে অনুপ্রবেশ/দখল এবং সীমানা ডিমার্কেশন সনদ তৈরি।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = overlapShiftX,
                                        onValueChange = { overlapShiftX = it },
                                        label = { Text("দখল/স্থানান্তর X (ফুট)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = overlapShiftY,
                                        onValueChange = { overlapShiftY = it },
                                        label = { Text("দখল/স্থানান্তর Y (ফুট)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "ওভারল্যাপ বিশ্লেষণ সারসংক্ষেপ:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = overlapReport.summaryNoteBn,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "সাধারণ এলাকা: ${String.format(Locale.US, "%.2f", overlapReport.overlapAreaSqFt / 435.6)} শতক • দখল/বাহির: ${String.format(Locale.US, "%.2f", overlapReport.encroachmentIntoCurrentSqFt / 435.6)} শতক",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealSecondary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val bmp = KhatianProEngine.renderDemarcationCompareBitmap(overlapReport)
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Demarcation_Report_${System.currentTimeMillis()}")
                                            toast = "ডিমার্কেশন সনদ PDF: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ডিমার্কেশন PDF") }
                                    Button(
                                        onClick = {
                                            val bmp = KhatianProEngine.renderDemarcationCompareBitmap(overlapReport)
                                            val res = ExportStreamWriter.savePngBitmap(context, bmp, "Demarcation_Report_${System.currentTimeMillis()}")
                                            toast = "ডিমার্কেশন সনদ PNG: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ডিমার্কেশন PNG") }
                                }
                            }
                        }
                    }

                    // ---------- Tab 1: All 15 Editable Union Land Office Registers ----------
                    1 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("১৫টি ইউনিয়ন ভূমি অফিস রেজিস্টার (এসি ল্যান্ড ওয়ার্কবেঞ্চ)", fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedRegKind == null,
                                        onClick = { selectedRegKind = null },
                                        label = { Text("সকল (${masterSnap.unionRegisters.size})") }
                                    )
                                    UnionRegisterKind.entries.forEach { k ->
                                        FilterChip(
                                            selected = selectedRegKind == k,
                                            onClick = {
                                                selectedRegKind = k
                                                regKindForNew = k
                                            },
                                            label = { Text(k.titleBn, maxLines = 1) }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = regRefNo, onValueChange = { regRefNo = it }, label = { Text("রেফারেন্স / নং") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = regMouza, onValueChange = { regMouza = it }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = regKhatianDag, onValueChange = { regKhatianDag = it }, label = { Text("খতিয়ান ও দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = regPerson, onValueChange = { regPerson = it }, label = { Text("মালিক / পক্ষ") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = regAmountOrArea, onValueChange = { regAmountOrArea = it }, label = { Text("জমি / টাকা") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = regStatus, onValueChange = { regStatus = it }, label = { Text("অবস্থা") }, singleLine = true, modifier = Modifier.weight(1f))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            masterRepo.saveUnionRegisterEntry(
                                                UnionRegisterEntry(
                                                    id = "ureg_${UUID.randomUUID().toString().take(8)}",
                                                    kind = regKindForNew,
                                                    refNo = regRefNo,
                                                    mouza = regMouza,
                                                    khatianDag = regKhatianDag,
                                                    personName = regPerson,
                                                    amountOrArea = regAmountOrArea,
                                                    dateStr = regDate,
                                                    status = regStatus
                                                )
                                            )
                                            toast = "${regKindForNew.titleBn}-এ এন্ট্রি যুক্ত হয়েছে!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("add_union_reg_btn")
                                    ) { Text("রেজিস্টারে যোগ করুন") }
                                    OutlinedButton(
                                        onClick = {
                                            val rows = masterSnap.unionRegisters.map {
                                                listOf(it.kind.titleBn, it.refNo, it.mouza, it.khatianDag, it.personName, it.amountOrArea, it.status)
                                            }
                                            val xlsx = UniversalExportEngine.buildXlsxBytes("UnionRegisters", listOf("Register", "Ref", "Mouza", "KhatianDag", "Person", "AmountArea", "Status"), rows)
                                            val res = DocumentAndReportEngine.saveBinaryDocument(context, "Union_Registers_${System.currentTimeMillis()}.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx)
                                            toast = "XLSX সেভ: ${res.displayPath}"
                                        },
                                        modifier = Modifier.weight(0.75f)
                                    ) { Text("XLSX") }
                                }
                            }
                        }

                        val shownRegs = if (selectedRegKind == null) masterSnap.unionRegisters else masterSnap.unionRegisters.filter { it.kind == selectedRegKind }
                        items(shownRegs, key = { it.id }) { item ->
                            Surface(
                                color = LocalNeuPalette.current.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${item.kind.titleBn} • ${item.refNo}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    Text("${item.mouza} | ${item.khatianDag} | ${item.personName}", style = MaterialTheme.typography.bodySmall)
                                    Text("${item.amountOrArea} • তারিখ: ${item.dateStr} • অবস্থা: ${item.status}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // ---------- Tab 2: Dated Tax Rate Schedule & LD Tax Calculator ----------
                    2 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("তারিখভিত্তিক ভূমি উন্নয়ন কর হার সূচি (সম্পাদনাযোগ্য)", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = taxSchedule.effectiveYear, onValueChange = { taxSchedule = taxSchedule.copy(effectiveYear = it) }, label = { Text("কার্যকর অর্থবছর") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = taxSchedule.annualLateInterestPct.toString(), onValueChange = { it.toDoubleOrNull()?.let { v -> taxSchedule = taxSchedule.copy(annualLateInterestPct = v) } }, label = { Text("বার্ষিক সুদ %") }, singleLine = true, modifier = Modifier.weight(0.8f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = taxSchedule.agriPerDecimalRateBdt.toString(), onValueChange = { it.toDoubleOrNull()?.let { v -> taxSchedule = taxSchedule.copy(agriPerDecimalRateBdt = v) } }, label = { Text("কৃষি/শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = taxSchedule.nonAgriResidentialPerDecBdt.toString(), onValueChange = { it.toDoubleOrNull()?.let { v -> taxSchedule = taxSchedule.copy(nonAgriResidentialPerDecBdt = v) } }, label = { Text("আবাসিক/শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = taxSchedule.commercialPerDecBdt.toString(), onValueChange = { it.toDoubleOrNull()?.let { v -> taxSchedule = taxSchedule.copy(commercialPerDecBdt = v) } }, label = { Text("বাণিজ্যিক/শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Button(
                                    onClick = {
                                        masterRepo.updateTaxRateSchedule(taxSchedule)
                                        toast = "তারিখভিত্তিক খাজনা রেট সূচি সেভ হয়েছে!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("ট্যাক্স রেট সূচি সংরক্ষণ করুন") }

                                Text("খাজনা ও বকেয়া ক্যালকুলেটর", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                val catLabels = listOf("কৃষি (<=২৫ বিঘা মওকুফ)", "কৃষি (>২৫ বিঘা)", "আবাসিক অকৃষি", "বাণিজ্যিক", "শিল্প")
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    catLabels.forEachIndexed { i, lbl ->
                                        FilterChip(selected = taxLandCategory == i, onClick = { taxLandCategory = i }, label = { Text(lbl) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = taxAreaDec, onValueChange = { taxAreaDec = it }, label = { Text("জমি (শতাংশ)") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = taxArrearYears, onValueChange = { taxArrearYears = it }, label = { Text("বকেয়া বছর") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = dakhilaRefNo, onValueChange = { dakhilaRefNo = it }, label = { Text("দাখিলা নং (থাকলে লিখুন)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = dakhilaVerified, onCheckedChange = { dakhilaVerified = it })
                                    Text("আমি ldtax.gov.bd পোর্টালে QR কোড যাচাই করেছি", style = MaterialTheme.typography.bodySmall)
                                }

                                val dec = taxAreaDec.toDoubleOrNull() ?: 0.0
                                val yrs = (taxArrearYears.toIntOrNull() ?: 1).coerceAtLeast(1)
                                val annualTax = when (taxLandCategory) {
                                    0 -> taxSchedule.agriBelow25BighaFeeBdt
                                    1 -> dec * taxSchedule.agriPerDecimalRateBdt
                                    2 -> dec * taxSchedule.nonAgriResidentialPerDecBdt
                                    3 -> dec * taxSchedule.commercialPerDecBdt
                                    else -> dec * taxSchedule.industrialPerDecBdt
                                }
                                val arrearPrincipal = annualTax * yrs
                                val lateInterest = if (yrs > 1) arrearPrincipal * (taxSchedule.annualLateInterestPct / 100.0) * (yrs - 1) else 0.0
                                val totalDue = arrearPrincipal + lateInterest

                                Surface(color = EmeraldPrimary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("বার্ষিক কর: ${String.format(Locale.US, "%.2f", annualTax)} টাকা", fontWeight = FontWeight.Bold)
                                        Text("$yrs বছরের মূল বকেয়া: ${String.format(Locale.US, "%.2f", arrearPrincipal)} টাকা")
                                        Text("বিলম্ব সুদ (${taxSchedule.annualLateInterestPct}%): ${String.format(Locale.US, "%.2f", lateInterest)} টাকা")
                                        Text("মোট প্রদেয়: ${String.format(Locale.US, "%.2f", totalDue)} টাকা", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                                        Text("সতর্কতা: এটি আনুমানিক হিসাব; প্রকৃত দাখিলা কেবল ldtax.gov.bd পোর্টালে ইস্যু হয়।", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // ---------- Tab 3: AC Land Case Workflow & Official Portals ----------
                    3 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("এসি ল্যান্ড মামলা ও সরকারি পোর্টাল সংযোগ", fontWeight = FontWeight.Bold)
                                val portals = listOf(
                                    "ই-নামজারি (mutation.land.gov.bd)" to "https://mutation.land.gov.bd/",
                                    "অনলাইন খাজনা / দাখিলা (ldtax.gov.bd)" to "https://ldtax.gov.bd/",
                                    "ই-পর্চা ও মৌজা ম্যাপ (eporcha.gov.bd)" to "https://eporcha.gov.bd/",
                                    "মামলা ও শুনানি (case.gov.bd)" to "https://case.gov.bd/"
                                )
                                portals.forEach { (title, url) ->
                                    OutlinedButton(
                                        onClick = { onOpenUrlInApp(url, title) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(title) }
                                }
                                Text("সক্রিয় আবেদন ও কেস তালিকা (${expSnap.serviceCases.size}):", fontWeight = FontWeight.SemiBold)
                                expSnap.serviceCases.forEach { c ->
                                    Surface(color = Color(0xFF1565C0).copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("${c.title} (${c.serviceKind.titleBn})", fontWeight = FontWeight.Bold)
                                            Text("মৌজা: ${c.mouza} | দাগ: ${c.prevDag} | অবস্থা: ${c.status}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------- Tab 4: Sub-Registry 11 Deeds Calculator & Checklist ----------
                    4 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(18.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("সাব-রেজিস্ট্রি ও ১১ ধরনের দলিল খরচ ক্যালকুলেটর", fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    deedCategories.forEachIndexed { idx, name ->
                                        FilterChip(selected = selectedDeedCatIdx == idx, onClick = { selectedDeedCatIdx = idx }, label = { Text(name) })
                                    }
                                }
                                OutlinedTextField(value = deedPropValue, onValueChange = { deedPropValue = it }, label = { Text("দলিলের মূল্য / মৌজা মূল্য (টাকা)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isMunicipalArea, onCheckedChange = { isMunicipalArea = it })
                                    Text("পৌরসভা / সিটি কর্পোরেশন এলাকা (উৎস কর হার বৃদ্ধি)", style = MaterialTheme.typography.bodySmall)
                                }

                                val price = deedPropValue.toDoubleOrNull() ?: 0.0
                                val isHebaFamily = selectedDeedCatIdx == 1
                                val stampFee = if (isHebaFamily) 1000.0 else price * 0.015
                                val regFee = if (isHebaFamily) 100.0 else price * 0.01
                                val localTax = if (isHebaFamily) 0.0 else price * (if (isMunicipalArea) 0.02 else 0.03)
                                val sourceTax = if (isHebaFamily) 0.0 else price * (if (isMunicipalArea) 0.04 else 0.03)
                                val eFeeAndMisc = 1500.0
                                val grandTotal = stampFee + regFee + localTax + sourceTax + eFeeAndMisc

                                val regReportText = """
                                    দলিলের ধরন: ${deedCategories[selectedDeedCatIdx]}
                                    মৌজা / ঘোষিত মূল্য: ${String.format(Locale.US, "%,.0f", price)} টাকা
                                    ১. স্ট্যাম্প শুল্ক: ${String.format(Locale.US, "%,.0f", stampFee)} টাকা
                                    ২. রেজিস্ট্রেশন ফি: ${String.format(Locale.US, "%,.0f", regFee)} টাকা
                                    ৩. স্থানীয় সরকার কর: ${String.format(Locale.US, "%,.0f", localTax)} টাকা
                                    ৪. উৎস কর (৫৩এইচ): ${String.format(Locale.US, "%,.0f", sourceTax)} টাকা
                                    ৫. ই-ফি, নকল ও এন-ফর্ম খরচ (আনুমানিক): ${String.format(Locale.US, "%,.0f", eFeeAndMisc)} টাকা
                                    মোট আনুমানিক সরকারি ফি: ${String.format(Locale.US, "%,.0f", grandTotal)} টাকা
                                    প্রয়োজনীয় কাগজপত্র চেকলিস্ট:
                                    • মূল বায়া দলিল ও হাল বিএস/সিএস/আরএস খতিয়ান
                                    • হালনাগাদ খাজনার দাখিলা ও ডিসিআর (নামজারি)
                                    • দাতা ও গ্রহীতার এনআইডি, ছবি ও টিআইএন (প্রযোজ্য ক্ষেত্রে)
                                    • ওয়ারিশ সনদ / পর্চা ও নকশা (বণ্টন/উত্তরাধিকার ক্ষেত্রে)
                                """.trimIndent()

                                VintageStampPaperCard(
                                    headerTitle = "সাব-রেজিস্ট্রি দলিল ফি ও চেকলিস্ট সারসংক্ষেপ",
                                    stampBadge = deedCategories[selectedDeedCatIdx],
                                    serialNumber = "REG-CALC-2026"
                                ) {
                                    Text(regReportText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Serif, color = Color(0xFF2B1C10))
                                }

                                OutlinedButton(
                                    onClick = {
                                        val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                            title = "সাব-রেজিস্ট্রি ফি ও দলিল চেকলিস্ট",
                                            stampValueLabel = deedCategories[selectedDeedCatIdx],
                                            serialTag = "SUB-REG-2026",
                                            bodyText = regReportText
                                        )
                                        val res = ExportStreamWriter.savePdfBitmap(context, bmp, "SubRegistry_Checklist_${System.currentTimeMillis()}")
                                        toast = "PDF সেভ: ${res.displayPath}"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("সাব-রেজিস্ট্রি ফি চেকলিস্ট PDF সেভ করুন") }
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
