package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppPreferences
import com.example.engine.GeometryAndAccuracyEngine
import com.example.engine.LandCalculators
import com.example.model.ConfidenceLevel
import com.example.model.GlobalSurveySettings
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AccuracyReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { AppPreferences(context) }
    val globalSettings by prefs.globalSettingsFlow.collectAsStateWithLifecycle(initialValue = GlobalSurveySettings())
    val scope = rememberCoroutineScope()

    var modeTab by remember { mutableStateOf(0) } // 0 = Accuracy & ± Engine, 1 = Tape Polygon Builder
    var areaDecimalInput by remember { mutableStateOf("12.50") }
    var perimeterFtInput by remember { mutableStateOf("310.0") }
    var calErrPctInput by remember { mutableStateOf("0.75") }
    var ftPerPxInput by remember { mutableStateOf("0.28") }
    var zoomInput by remember { mutableStateOf("2.5") }
    var gpsAccMInput by remember { mutableStateOf("0.0") }
    var includeDisclaimer by remember { mutableStateOf(true) }

    // Tape polygon inputs: length(ft)@bearing(deg)
    var tapeLegsInput by remember {
        mutableStateOf("80'0\" @ 90\n60'0\" @ 180\n80'0\" @ 270\n60'0\" @ 0")
    }

    var toast by remember { mutableStateOf<String?>(null) }

    val decVal = LandCalculators.toEnglishDigits(areaDecimalInput).toDoubleOrNull() ?: 0.0
    val sqFtVal = decVal * globalSettings.activePreset().decimalSqFt
    val perimVal = LandCalculators.toEnglishDigits(perimeterFtInput).toDoubleOrNull() ?: 0.0
    val calErr = LandCalculators.toEnglishDigits(calErrPctInput).toDoubleOrNull() ?: 0.5
    val ftPerPx = LandCalculators.toEnglishDigits(ftPerPxInput).toDoubleOrNull() ?: 0.3
    val zoom = (LandCalculators.toEnglishDigits(zoomInput).toFloatOrNull() ?: 1f).coerceIn(0.5f, 100f)
    val gpsAcc = LandCalculators.toEnglishDigits(gpsAccMInput).toDoubleOrNull() ?: 0.0

    val uncertainty = remember(sqFtVal, perimVal, calErr, ftPerPx, zoom, gpsAcc, includeDisclaimer, globalSettings) {
        GeometryAndAccuracyEngine.computeMeasurementUncertainty(
            areaSqFeet = sqFtVal,
            perimeterFeet = perimVal,
            edgeLengthsFt = listOf(perimVal / 4.0, perimVal / 4.0, perimVal / 4.0, perimVal / 4.0),
            calibrationErrPct = calErr,
            ftPerPixel = ftPerPx,
            zoomScale = zoom,
            gpsAccuracyM = gpsAcc,
            settings = globalSettings,
            includeDisclaimer = includeDisclaimer
        )
    }

    val tapeResult = remember(tapeLegsInput) {
        val dists = mutableListOf<Double>()
        val bearings = mutableListOf<Double>()
        tapeLegsInput.lines().forEach { line ->
            val parts = line.split("@")
            if (parts.size == 2) {
                val d = LandCalculators.parseFeetInchesSuta(parts[0])
                val b = LandCalculators.toEnglishDigits(parts[1]).trim().toDoubleOrNull()
                if (d != null && b != null) {
                    dists.add(d)
                    bearings.add(b)
                }
            }
        }
        GeometryAndAccuracyEngine.buildPolygonFromTapeLegs(dists, bearings)
    }

    val useBn = globalSettings.useBanglaDigits
    val prec = globalSettings.decimalPrecision

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
                titleBn = "নির্ভুলতা (± ত্রুটি) ও ফিতা পলিগন",
                subtitleEn = "M02 Uncertainty Propagation • Confidence Rating • M01 Tape Polygon Builder",
                helpTitle = "± নির্ভুলতা বিশ্লেষণ নির্দেশিকা",
                helpSteps = listOf(
                    "মৌজা স্কেল ত্রুটি %, ছবির রেজোলিউশন (ft/px), জুম ও GPS নির্ভুলতা থেকে প্রকৃত ± ত্রুটি হিসাব হয়।",
                    "আস্থার সীমা (% Threshold) গ্লোবাল সেটিংস বা এই স্ক্রিন থেকে কাস্টমাইজ করা যায়।",
                    "ফিতা পলিগন ট্যাবে ফুট-ইঞ্চি-সুতা ও বিয়ারিং দিয়ে পলিগন ও ক্লোজার ত্রুটি বের করুন।"
                ),
                onBack = onBack,
                backTestTag = "accuracy_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeuPillChip("± ত্রুটি ও আস্থা রিপোর্ট (M02)", modeTab == 0, testTag = "acc_tab_err") { modeTab = 0 }
                        NeuPillChip("ফিতা পলিগন ও শোল্যাস (M01)", modeTab == 1, testTag = "acc_tab_tape") { modeTab = 1 }
                    }
                }

                if (modeTab == 0) {
                    item {
                        val badgeColor = when (uncertainty.confidence) {
                            ConfidenceLevel.HIGH -> Color(0xFF0F6B54)
                            ConfidenceLevel.MEDIUM -> Color(0xFFD98200)
                            ConfidenceLevel.LOW -> Color(0xFFC62828)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 6.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("পরিমাপ বিশ্লেষণ ফলাফল", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = badgeColor)
                                        Text(
                                            text = "${uncertainty.confidence.labelBn} (±${LandCalculators.formatNumber(uncertainty.relativeErrPct, 2, useBn)}%)",
                                            color = badgeColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "শতাংশ: ${LandCalculators.formatNumber(uncertainty.decimal, prec, useBn)} ± ${LandCalculators.formatNumber(uncertainty.decimalErr, prec, useBn)} শতক",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "কাঠা: ${LandCalculators.formatNumber(uncertainty.katha, prec, useBn)} ± ${LandCalculators.formatNumber(uncertainty.kathaErr, prec, useBn)} কাঠা  |  বর্গফুট: ${LandCalculators.formatNumber(uncertainty.areaSqFt, 1, useBn)} ± ${LandCalculators.formatNumber(uncertainty.areaErrSqFt, 1, useBn)} ft²",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "পরিসীমা: ${LandCalculators.formatNumber(uncertainty.perimeterFt, 2, useBn)} ± ${LandCalculators.formatNumber(uncertainty.perimeterErrFt, 2, useBn)} ফুট",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = uncertainty.sourceSummaryBn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("ত্রুটির উৎস প্যারামিটার", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = areaDecimalInput,
                                    onValueChange = { areaDecimalInput = it },
                                    label = { Text("জমি (শতক)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = perimeterFtInput,
                                    onValueChange = { perimeterFtInput = it },
                                    label = { Text("পরিসীমা (ফুট)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = calErrPctInput,
                                    onValueChange = { calErrPctInput = it },
                                    label = { Text("স্কেল ত্রুটি %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ftPerPxInput,
                                    onValueChange = { ftPerPxInput = it },
                                    label = { Text("রেজোলিউশন (ft/px)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = zoomInput,
                                    onValueChange = { zoomInput = it },
                                    label = { Text("পয়েন্ট জুম (x)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = gpsAccMInput,
                                    onValueChange = { gpsAccMInput = it },
                                    label = { Text("GPS ত্রুটি (মি.)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("রিপোর্টে \"সরকারি জরিপের বিকল্প নয়\" লাইন রাখুন", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Switch(checked = includeDisclaimer, onCheckedChange = { includeDisclaimer = it })
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val pdf = GeometryAndAccuracyEngine.exportAccuracyReportPdf(
                                            context = context,
                                            title = "জরিপ নির্ভুলতা প্রতিবেদন",
                                            uncertainty = uncertainty
                                        )
                                        toast = "PDF রিপোর্ট সেভ হয়েছে: ${pdf.name}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("accuracy_export_pdf_btn")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Accuracy Report PDF সেভ করুন")
                            }
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("ফিতা মাপ + বিয়ারিং → পলিগন ও ক্লোজার", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = tapeLegsInput,
                                onValueChange = { tapeLegsInput = it },
                                label = { Text("প্রতি লাইন: দৈর্ঘ্য(ফুট/ইঞ্চি) @ বিয়ারিং(ডিগ্রি)") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("tape_polygon_input")
                            )
                            val areaRes = LandCalculators.fromSqFeet(tapeResult.areaSqFeet, globalSettings.activePreset())
                            Text(
                                text = "ক্ষেত্রফল: ${LandCalculators.formatNumber(areaRes.decimal, prec, useBn)} শতক (${LandCalculators.formatNumber(areaRes.katha, prec, useBn)} কাঠা | ${LandCalculators.formatNumber(areaRes.sqFeet, 1, useBn)} ft²)",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "পরিসীমা: ${LandCalculators.formatNumber(tapeResult.perimeterFeet, 2, useBn)} ft  |  ক্লোজার ত্রুটি: ${LandCalculators.formatNumber(tapeResult.closureErrorFeet, 3, useBn)} ft (1:${tapeResult.closureRatioDenom.toInt()})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = {
                                    areaDecimalInput = String.format(Locale.US, "%.3f", areaRes.decimal)
                                    perimeterFtInput = String.format(Locale.US, "%.2f", tapeResult.perimeterFeet)
                                    modeTab = 0
                                    toast = "ফিতা পরিমাপ নির্ভুলতা রিপোর্টে পাঠানো হয়েছে"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("এই মাপ ± Accuracy রিপোর্টে পাঠান")
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
