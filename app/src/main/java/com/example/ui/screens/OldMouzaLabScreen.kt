package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpansionRepository
import com.example.engine.ExpansionMathAndSurveyEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.UniversalExportEngine
import com.example.model.AdjacentDagNeighbour
import com.example.model.DagGenealogyRecord
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import java.util.Locale

@Composable
fun OldMouzaLabScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { ExpansionRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // A01 Image Restoration State
    var sourceBmp by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBmp by remember { mutableStateOf<Bitmap?>(null) }
    var contrastVal by remember { mutableFloatStateOf(1.6f) }
    var stainCleanup by remember { mutableFloatStateOf(18f) }
    var inkIsolate by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { bmp ->
                sourceBmp = bmp
                enhancedBmp = enhanceHistoricalMapBitmap(bmp, contrastVal, stainCleanup, inkIsolate)
                toast = "পুরনো ম্যাপ ইমেজ লোড হয়েছে"
            } ?: run { toast = "ইমেজ পড়া যায়নি" }
        }
    }

    // A02 Calibration & Distortion State
    var pxSpan1 by remember { mutableStateOf("420") }
    var ftSpan1 by remember { mutableStateOf("100") }
    var pxSpan2 by remember { mutableStateOf("408") }
    var ftSpan2 by remember { mutableStateOf("100") }
    var polyPxArea by remember { mutableStateOf("155000") }
    var tiltCos by remember { mutableStateOf("0.985") }
    var scanDpi by remember { mutableStateOf("300") }
    var calResultText by remember { mutableStateOf("") }

    // A03 & A06 Topology & Sheet Overlap State
    var sheetGapPx by remember { mutableStateOf("1.8") }
    var unclosedGapFt by remember { mutableStateOf("0.45") }
    var overlapSqFt by remember { mutableStateOf("18.5") }

    // A04 & A07 Genealogy + Search State
    var genMouza by remember { mutableStateOf("পুবাইল") }
    var genRelation by remember { mutableStateOf("বিভাজন (Split)") }
    var genFormerSurvey by remember { mutableStateOf("আরএস (RS)") }
    var genFormerDag by remember { mutableStateOf("৫১২") }
    var genCurrentSurvey by remember { mutableStateOf("বিএস (BS)") }
    var genCurrentDag by remember { mutableStateOf("১০৪২, ১০৪৩") }
    var genKhatian by remember { mutableStateOf("সাবেক ২১০ → হাল ৪১২") }
    var genDecimal by remember { mutableStateOf("33.5") }
    var genVerified by remember { mutableStateOf(false) }
    var searchDagQuery by remember { mutableStateOf("") }

    // A05 4-Side Adjacency + A08 Area Comparison
    var northDag by remember { mutableStateOf("১০৪১") }
    var northLen by remember { mutableStateOf("72") }
    var southDag by remember { mutableStateOf("রাস্তা / ১০৪৪") }
    var southLen by remember { mutableStateOf("71.5") }
    var eastDag by remember { mutableStateOf("১০৪৩") }
    var eastLen by remember { mutableStateOf("54") }
    var westDag by remember { mutableStateOf("১০৩৯") }
    var westLen by remember { mutableStateOf("55") }
    var csAreaComp by remember { mutableStateOf("33.0") }
    var rsAreaComp by remember { mutableStateOf("33.5") }
    var bsAreaComp by remember { mutableStateOf("32.8") }
    var fieldAreaComp by remember { mutableStateOf("32.65") }

    // A09 Cadastral Print
    var printPaper by remember { mutableStateOf("A3") }
    var printScale by remember { mutableStateOf("16\" = 1 Mile (1:3960)") }
    var printDagLabel by remember { mutableStateOf("মৌজা: পুবাইল | দাগ: ১০৪২ (৩৩.৫ শতক)") }
    var printGrid by remember { mutableStateOf(true) }

    val tabs = listOf(
        "A01-03 পুরনো ম্যাপ ও স্কেল",
        "A04-07 দাগ সম্পর্ক ও সার্চ",
        "A05-08 সীমানা ও তুলনা",
        "A09 ম্যাপ প্রিন্ট (A0-A4)"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
            WorkstationTopBar(
                titleBn = "পুরনো মৌজা ও দাগ ল্যাব (A01–A09)",
                subtitleEn = "Historical Map Restoration, Distortion, Genealogy & Cadastral Print",
                helpTitle = "পুরনো মৌজা ম্যাপ ও দাগ ল্যাব গাইড",
                helpSteps = listOf(
                    "ফটো পিকার দিয়ে পুরনো দাগযুক্ত মৌজা স্ক্যান বেছে কালি ও দাগ পৃথক করুন।",
                    "X/Y দুই দিকের পিক্সেল ও জানা দূরত্ব দিয়ে স্ক্যান-বিকৃতি (Distortion %) ও অনিশ্চয়তা বের করুন।",
                    "সাবেক-হাল দাগ নম্বর মিললেই স্বয়ংক্রিয়ভাবে এক জমি ধরবেন না — বিভাজন/একত্রীকরণ ম্যানুয়ালি যাচাই করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "old_mouza_back_btn"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.indices.toList()) { i ->
                    NeuPillChip(
                        text = tabs[i],
                        selected = activeTab == i,
                        testTag = "mouza_lab_tab_$i",
                        onClick = { activeTab = i }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        "গুরুত্বপূর্ণ নিয়ম: পুরনো নকশা ভাঁজ, আর্দ্রতা ও স্ক্যানিংয়ের কারণে প্রসারিত/সংকুচিত হতে পারে। সাবেক ও হাল দাগ নম্বর একই হলেই তা একই জমি নয় — মৌজা সিট ও খতিয়ান মিলিয়ে যাচাই আবশ্যক।"
                    )
                }

                when (activeTab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("A01. পুরনো ম্যাপ পরিষ্কার ও কালি পৃথকীকরণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("pick_old_map_btn")
                                ) { Text("গ্যালারি থেকে পুরনো ম্যাপ নির্বাচন করুন (Photo Picker)") }

                                Text("কনট্রাস্ট (Ink Contrast): ${String.format(Locale.US, "%.1fx", contrastVal)}", style = MaterialTheme.typography.labelMedium)
                                Slider(value = contrastVal, onValueChange = {
                                    contrastVal = it
                                    sourceBmp?.let { s -> enhancedBmp = enhanceHistoricalMapBitmap(s, contrastVal, stainCleanup, inkIsolate) }
                                }, valueRange = 1f..3f)

                                Text("দাগ/হলদেটে ভাব কমানো (Stain Suppression): ${stainCleanup.toInt()}%", style = MaterialTheme.typography.labelMedium)
                                Slider(value = stainCleanup, onValueChange = {
                                    stainCleanup = it
                                    sourceBmp?.let { s -> enhancedBmp = enhanceHistoricalMapBitmap(s, contrastVal, stainCleanup, inkIsolate) }
                                }, valueRange = 0f..55f)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = inkIsolate, onCheckedChange = {
                                        inkIsolate = it
                                        sourceBmp?.let { s -> enhancedBmp = enhanceHistoricalMapBitmap(s, contrastVal, stainCleanup, inkIsolate) }
                                    })
                                    Text("কেবল সীমানা কালি পৃথক করুন (Grayscale Threshold)", style = MaterialTheme.typography.bodySmall)
                                }

                                enhancedBmp?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Enhanced Historical Mouza Map",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                                            val res = ExportStreamWriter.savePngBitmap(context, copy, "clean_mouza_${System.currentTimeMillis()}")
                                            toast = "পরিষ্কার ম্যাপ সংরক্ষিত: ${res.displayPath}"
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("পরিষ্কার ম্যাপ PNG হিসেবে সেভ করুন") }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("A02 & A03. স্কেল ক্যালিব্রেশন, বিকৃতি ও টপোলজি চেক", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(pxSpan1, { pxSpan1 = it }, label = { Text("X পিক্সেল") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(ftSpan1, { ftSpan1 = it }, label = { Text("মাঠের ফুট (X)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(pxSpan2, { pxSpan2 = it }, label = { Text("Y পিক্সেল") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(ftSpan2, { ftSpan2 = it }, label = { Text("মাঠের ফুট (Y)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(polyPxArea, { polyPxArea = it }, label = { Text("পলিগন পিক্সেল এলাকা") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(tiltCos, { tiltCos = it }, label = { Text("Tilt Factor (0.5–1)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(scanDpi, { scanDpi = it }, label = { Text("Scan DPI") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(unclosedGapFt, { unclosedGapFt = it }, label = { Text("Unclosed Gap (ft)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(sheetGapPx, { sheetGapPx = it }, label = { Text("সিট জয়েন্ট গ্যাপ (px)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(overlapSqFt, { overlapSqFt = it }, label = { Text("ওভারল্যাপ (ব.ফু)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Button(
                                    onClick = {
                                        val report = ExpansionMathAndSurveyEngine.analyzeOldMapMeasurement(
                                            pixelSpanMeasured = pxSpan1.toDoubleOrNull() ?: 400.0,
                                            knownGroundDistFt = ftSpan1.toDoubleOrNull() ?: 100.0,
                                            secondaryPixelSpan = pxSpan2.toDoubleOrNull(),
                                            secondaryKnownDistFt = ftSpan2.toDoubleOrNull(),
                                            polygonPixelArea = polyPxArea.toDoubleOrNull() ?: 100000.0,
                                            tiltCorrectionCosFactor = tiltCos.toDoubleOrNull() ?: 1.0,
                                            scannerResolutionDpi = scanDpi.toDoubleOrNull() ?: 300.0
                                        )
                                        val gap = unclosedGapFt.toDoubleOrNull() ?: 0.0
                                        val ovl = overlapSqFt.toDoubleOrNull() ?: 0.0
                                        val topoWarn = buildString {
                                            if (gap > 0.2) append("• Unclosed boundary gap: ${String.format(Locale.US, "%.2f", gap)} ft — স্ন্যাপ/জয়েন প্রয়োজন\n")
                                            if (ovl > 1.0) append("• সিট বা দাগ ওভারল্যাপ: ${String.format(Locale.US, "%.2f", ovl)} ব.ফু — পুনরায় ট্রেস করুন\n")
                                            if (gap <= 0.2 && ovl <= 1.0) append("• টপোলজি ও সিট জয়েন গ্রহণযোগ্য সীমায় আছে")
                                        }
                                        val out = """
                                            প্রতি ফুট পিক্সেল: ${String.format(Locale.US, "%.3f px/ft", report.pixelsPerFoot)}
                                            সংশোধিত ক্ষেত্রফল: ${String.format(Locale.US, "%.2f ব.ফু (%.3f শতাংশ)", report.correctedPolygonAreaSqFt, report.mapAreaDecimal)}
                                            স্ক্যানিং বিকৃতি (Distortion): ${String.format(Locale.US, "%.2f%%", report.scaleDistortionPct)}
                                            পরিমাপ অনিশ্চয়তা (±): ±${String.format(Locale.US, "%.2f ব.ফু", report.uncertaintySqFt)}
                                            $topoWarn
                                        """.trimIndent()
                                        calResultText = out
                                        repo.logCalculation("Part A02", "পুরনো ম্যাপ ক্যালিব্রেশন", "${String.format(Locale.US, "%.3f", report.mapAreaDecimal)} শতক (±${String.format(Locale.US, "%.1f", report.uncertaintySqFt)} ব.ফু)")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("calc_old_map_distortion_btn")
                                ) { Text("বিকৃতি, ক্ষেত্রফল ও টপোলজি যাচাই করুন") }

                                if (calResultText.isNotBlank()) {
                                    Surface(color = EmeraldPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(calResultText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
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
                                Text("A04 & A07. সাবেক বনাম হাল দাগ সম্পর্ক ম্যানেজার", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(genMouza, { genMouza = it }, label = { Text("মৌজা") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(genFormerSurvey, { genFormerSurvey = it }, label = { Text("সাবেক জরিপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(genFormerDag, { genFormerDag = it }, label = { Text("সাবেক দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(genCurrentSurvey, { genCurrentSurvey = it }, label = { Text("হাল জরিপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(genCurrentDag, { genCurrentDag = it }, label = { Text("হাল দাগ(সমূহ)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(genRelation, { genRelation = it }, label = { Text("সম্পর্ক (Split/Merge)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(genDecimal, { genDecimal = it }, label = { Text("জমি (শতাংশ)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(genKhatian, { genKhatian = it }, label = { Text("খতিয়ান লিংক (সাবেক → হাল)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = genVerified, onCheckedChange = { genVerified = it })
                                    Text("নকশা ও পর্চা ম্যানুয়ালি মিলিয়ে নিশ্চিত করা হয়েছে", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = {
                                        repo.saveDagGenealogy(
                                            DagGenealogyRecord(
                                                id = "dg_${System.currentTimeMillis()}",
                                                mouza = genMouza,
                                                relationType = genRelation,
                                                formerSurvey = genFormerSurvey,
                                                formerDagNumbers = genFormerDag,
                                                currentSurvey = genCurrentSurvey,
                                                currentDagNumbers = genCurrentDag,
                                                khatianLink = genKhatian,
                                                transferDecimal = genDecimal.toDoubleOrNull() ?: 0.0,
                                                verifiedManually = genVerified
                                            )
                                        )
                                        toast = "সাবেক-হাল দাগ সম্পর্ক সেভ হয়েছে"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("save_dag_genealogy_btn")
                                ) { Text("দাগ সম্পর্ক সংরক্ষণ করুন") }

                                OutlinedTextField(
                                    value = searchDagQuery,
                                    onValueChange = { searchDagQuery = it },
                                    label = { Text("A07 দাগ / মৌজা / খতিয়ান সার্চ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        val matched = snap.dagGenealogies.filter {
                            searchDagQuery.isBlank() ||
                                it.mouza.contains(searchDagQuery, true) ||
                                it.formerDagNumbers.contains(searchDagQuery, true) ||
                                it.currentDagNumbers.contains(searchDagQuery, true) ||
                                it.khatianLink.contains(searchDagQuery, true)
                        }
                        items(matched, key = { it.id }) { g ->
                            Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("${g.mouza} • ${g.relationType}", fontWeight = FontWeight.Bold)
                                    Text("${g.formerSurvey} দাগ: ${g.formerDagNumbers} ➔ ${g.currentSurvey} দাগ: ${g.currentDagNumbers} (${g.transferDecimal} শতক)", style = MaterialTheme.typography.bodySmall)
                                    Text("খতিয়ান: ${g.khatianLink} | যাচাই: ${if (g.verifiedManually) "হ্যাঁ" else "অযাচাইকৃত"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            val neighbours = listOf(
                                AdjacentDagNeighbour("n", "উত্তর", northDag, "", northLen.toDoubleOrNull() ?: 0.0, northDag.isNotBlank()),
                                AdjacentDagNeighbour("s", "দক্ষিণ", southDag, "", southLen.toDoubleOrNull() ?: 0.0, southDag.isNotBlank()),
                                AdjacentDagNeighbour("e", "পূর্ব", eastDag, "", eastLen.toDoubleOrNull() ?: 0.0, eastDag.isNotBlank()),
                                AdjacentDagNeighbour("w", "পশ্চিম", westDag, "", westLen.toDoubleOrNull() ?: 0.0, westDag.isNotBlank())
                            )
                            val (totalShared, adjWarnings) = ExpansionMathAndSurveyEngine.summarizeAdjacency(neighbours)

                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("A05. চারিদিকের প্রতিবেশী দাগ ও শেয়ার্ড দৈর্ঘ্য", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(northDag, { northDag = it }, label = { Text("উত্তর দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(northLen, { northLen = it }, label = { Text("দৈর্ঘ্য (ফুট)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(southDag, { southDag = it }, label = { Text("দক্ষিণ দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(southLen, { southLen = it }, label = { Text("দৈর্ঘ্য (ফুট)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(eastDag, { eastDag = it }, label = { Text("পূর্ব দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(eastLen, { eastLen = it }, label = { Text("দৈর্ঘ্য (ফুট)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(westDag, { westDag = it }, label = { Text("পশ্চিম দাগ") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(westLen, { westLen = it }, label = { Text("দৈর্ঘ্য (ফুট)") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Text("মোট ৪-সীমানা পরিসীমা: ${String.format(Locale.US, "%.2f ফুট", totalShared)}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                adjWarnings.forEach { w ->
                                    Text("⚠ $w", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                }

                                Spacer(Modifier.height(6.dp))
                                Text("A08. CS / RS / BS ও সরেজমিন ক্ষেত্রফল তুলনা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(csAreaComp, { csAreaComp = it }, label = { Text("CS শতক") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(rsAreaComp, { rsAreaComp = it }, label = { Text("RS শতক") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(bsAreaComp, { bsAreaComp = it }, label = { Text("BS শতক") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(fieldAreaComp, { fieldAreaComp = it }, label = { Text("ফিল্ড মাপ") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                val diffFieldVsBs = (fieldAreaComp.toDoubleOrNull() ?: 0.0) - (bsAreaComp.toDoubleOrNull() ?: 0.0)
                                Text(
                                    "সরেজমিন বনাম বিএস পার্থক্য: ${String.format(Locale.US, "%+.3f শতক", diffFieldVsBs)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (kotlin.math.abs(diffFieldVsBs) > 0.25) Color(0xFFC62828) else EmeraldPrimary
                                )
                            }
                        }
                    }

                    3 -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().neuSurface(20.dp, 5.dp).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("A09. স্কেল মৌজা প্রিন্ট ইঞ্জিন (A0 / A1 / A2 / A3 / A4)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf("A4", "A3", "A2", "A1", "A0")) { sz ->
                                        NeuPillChip(text = sz, selected = printPaper == sz, onClick = { printPaper = sz })
                                    }
                                }
                                OutlinedTextField(printScale, { printScale = it }, label = { Text("ম্যাপ স্কেল") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(printDagLabel, { printDagLabel = it }, label = { Text("শিরোনাম ও দাগ নং") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = printGrid, onCheckedChange = { printGrid = it })
                                    Text("সার্ভে গ্রিড, লেজেন্ড ও উত্তর দিক যুক্ত করুন", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val bmp = UniversalExportEngine.renderCadastralPrintBitmap(
                                                title = "ডিজিটাল মৌজা প্রিন্ট",
                                                paperSize = printPaper,
                                                scaleLabel = printScale,
                                                dagLabel = printDagLabel,
                                                includeGrid = printGrid,
                                                includeAreaLabels = true
                                            )
                                            val res = ExportStreamWriter.savePdfBitmap(context, bmp, "mouza_${printPaper}_${System.currentTimeMillis()}")
                                            toast = "PDF সেভ হয়েছে: ${res.displayPath}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_mouza_print_pdf")
                                    ) { Text("PDF সেভ ($printPaper)") }

                                    OutlinedButton(
                                        onClick = {
                                            val bmp = UniversalExportEngine.renderCadastralPrintBitmap(
                                                title = "ডিজিটাল মৌজা প্রিন্ট",
                                                paperSize = printPaper,
                                                scaleLabel = printScale,
                                                dagLabel = printDagLabel,
                                                includeGrid = printGrid,
                                                includeAreaLabels = true
                                            )
                                            UniversalExportEngine.printBitmapDocument(context, "Mouza_$printPaper", bmp)
                                        },
                                        modifier = Modifier.weight(1f).testTag("native_print_mouza_btn")
                                    ) { Text("প্রিন্ট ডায়ালগ") }
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

private fun enhanceHistoricalMapBitmap(
    src: Bitmap,
    contrast: Float,
    stainSuppressPct: Float,
    grayscaleInk: Boolean
): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val cm = ColorMatrix()
    if (grayscaleInk) cm.setSaturation(0f)
    val scale = contrast
    val shift = (-0.5f * scale + 0.5f) * 255f + (stainSuppressPct * 0.7f)
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, shift,
            0f, scale, 0f, 0f, shift,
            0f, 0f, scale, 0f, shift,
            0f, 0f, 0f, 1f, 0f
        )
    )
    cm.postConcat(contrastMatrix)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}
