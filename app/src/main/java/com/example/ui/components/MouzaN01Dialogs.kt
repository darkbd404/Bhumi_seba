package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.LandCalculators
import com.example.engine.MouzaPdfEngine
import com.example.model.CalibrationRefLine
import com.example.model.GlobalSurveySettings
import com.example.model.MeasureBetweenResult
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.ParcelTableRow
import com.example.model.PdfInspectionReport
import com.example.model.ScaleCalibrationProfile
import com.example.model.ShoelaceStepRow
import com.example.ui.theme.EmeraldPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouzaCalibrationSheet(
    selectedScale: MouzaScale,
    calibrationScaleX: Double,
    calibrationScaleY: Double,
    calibrationErrPct: Double,
    paperShrinkageNote: String,
    refLines: List<CalibrationRefLine>,
    globalSettings: GlobalSurveySettings,
    pdfReport: PdfInspectionReport?,
    plots: List<MouzaPlot>,
    hasTapeLine: Boolean,
    onSelectScale: (MouzaScale) -> Unit,
    onApplySuggestedPdfScale: () -> Unit,
    onAddRefLineFromTape: (actualFeet: Double, label: String) -> Unit,
    onRemoveRefLine: (String) -> Unit,
    onVerifyPaperSize: (widthIn: Double, heightIn: Double) -> Unit,
    onCalibrateWithKnownPlot: (plotId: Int, actualDecimal: Double) -> Unit,
    onApplyProfile: (ScaleCalibrationProfile) -> Unit,
    onSaveProfile: (name: String, stage: String) -> Unit,
    onDismiss: () -> Unit
) {
    var knownFeetText by remember { mutableStateOf("100") }
    var refLineLabel by remember { mutableStateOf("") }
    var paperWText by remember { mutableStateOf("36") }
    var paperHText by remember { mutableStateOf("24") }
    var profileName by remember { mutableStateOf("") }
    var selectedPlotId by remember { mutableIntStateOf(plots.firstOrNull()?.id ?: -1) }
    var knownDecimalText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "৫-পদ্ধতি স্কেল ক্যালিব্রেশন ও পেপার সঙ্কোচন",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(
                    Locale.US,
                    "বর্তমান: %s • X:%.3f Y:%.3f • আনুমানিক স্কেল ত্রুটি ±%.2f%%",
                    selectedScale.ratioLabel, calibrationScaleX, calibrationScaleY, calibrationErrPct
                ),
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold
            )

            // Method 1: Auto Text Scale Suggestion (suggest only, user approves)
            if (pdfReport?.suggestedFeetPerInch != null && pdfReport.detectedScaleText != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PDF টেক্সট থেকে প্রস্তাবিত স্কেল:", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                            Text(pdfReport.detectedScaleText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = onApplySuggestedPdfScale, shape = RoundedCornerShape(10.dp)) {
                            Text("প্রয়োগ")
                        }
                    }
                }
            }

            // Method 2: Standard Preset & Saved Calibration Profiles
            Text("১. স্ট্যান্ডার্ড ও সেভ করা প্রোফাইল", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MouzaScale.entries.forEach { s ->
                    NeuPillChip(text = s.ratioLabel, selected = selectedScale == s, onClick = { onSelectScale(s) })
                }
            }
            if (globalSettings.savedCalibrationProfiles.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    globalSettings.savedCalibrationProfiles.forEach { prof ->
                        OutlinedButton(
                            onClick = { onApplyProfile(prof) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(prof.nameBn, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Method 3: Multi-Line Known Distance + X/Y Shrinkage
            Text("২. বহু-রেখা পরিচিত দূরত্ব (X/Y সঙ্কোচন সংশোধন)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = if (hasTapeLine) "ফিতা মাপ-এ আঁকা সর্বশেষ ২ বিন্দুর আসল দূরত্ব (ফুটে) দিয়ে রেফারেন্স রেখা যুক্ত করুন:"
                else "টিপস: নকশায় 'ফিতা মাপ' দিয়ে X বা Y বরাবর একটি জানা রেখা আঁকলে এখানে যোগ করতে পারবেন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = knownFeetText,
                    onValueChange = { knownFeetText = it },
                    label = { Text("আসল ফুট") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = refLineLabel,
                    onValueChange = { refLineLabel = it },
                    label = { Text("নাম (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val ft = knownFeetText.toDoubleOrNull() ?: 0.0
                        if (ft > 0.0) onAddRefLineFromTape(ft, refLineLabel)
                    },
                    enabled = hasTapeLine,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ যোগ")
                }
            }
            refLines.forEach { ln ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${ln.label} • আসল ${String.format(Locale.US, "%.1f", ln.actualFeet)}'", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            String.format(Locale.US, "আগে: %.1f' • সমন্বিত বিচ্যুতি: %.2f%%", ln.measuredFeetBefore, ln.deviationPct),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ln.deviationPct > 2.5) MaterialTheme.colorScheme.error else EmeraldPrimary
                        )
                    }
                    IconButton(onClick = { onRemoveRefLine(ln.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ref line", modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider()

            // Method 4: Physical Paper Size Shrinkage Check
            Text("৩. ফিজিক্যাল পেপার সাইজ যাচাই (ইঞ্চি)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = paperWText,
                    onValueChange = { paperWText = it },
                    label = { Text("প্রস্থ (ইঞ্চি)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = paperHText,
                    onValueChange = { paperHText = it },
                    label = { Text("উচ্চতা (ইঞ্চি)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        onVerifyPaperSize(paperWText.toDoubleOrNull() ?: 0.0, paperHText.toDoubleOrNull() ?: 0.0)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("যাচাই")
                }
            }
            if (paperShrinkageNote.isNotBlank()) {
                Text(paperShrinkageNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            // Method 5: Known Plot Decimal Calibration
            if (plots.isNotEmpty()) {
                HorizontalDivider()
                Text("৪. পরিচিত দাগ (শতক) দিয়ে ক্যালিব্রেট", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    plots.forEach { p ->
                        val lbl = if (p.dagNumber.isNotBlank()) "দাগ ${p.dagNumber}" else "দাগ #${p.plotIndex}"
                        NeuPillChip(text = lbl, selected = selectedPlotId == p.id, onClick = { selectedPlotId = p.id })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = knownDecimalText,
                        onValueChange = { knownDecimalText = it },
                        label = { Text("খতিয়ানের সঠিক শতক") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val dec = knownDecimalText.toDoubleOrNull() ?: 0.0
                            if (selectedPlotId > 0 && dec > 0.0) onCalibrateWithKnownPlot(selectedPlotId, dec)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("সেট")
                    }
                }
            }

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("প্রোফাইল নাম সেভ করুন") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { onSaveProfile(profileName, "RS") }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("প্রোফাইল সেভ")
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
fun MouzaCleanerCompareDialog(
    originalBitmap: Bitmap,
    cleanedBitmap: Bitmap?,
    onGenerateClean: (contrast: Float, threshold: Int, despeckle: Boolean, invert: Boolean) -> Unit,
    onExportCleaned: (asPdf: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var contrast by remember { mutableFloatStateOf(1.35f) }
    var threshold by remember { mutableIntStateOf(188) }
    var despeckle by remember { mutableStateOf(true) }
    var invert by remember { mutableStateOf(false) }
    var splitFrac by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(Unit) {
        if (cleanedBitmap == null) {
            onGenerateClean(contrast, threshold, despeckle, invert)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ম্যাপ ক্লিনার • আগে / পরে স্লাইডার", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    val origImg = remember(originalBitmap) { originalBitmap.asImageBitmap() }
                    val cleanImg = remember(cleanedBitmap) { cleanedBitmap?.asImageBitmap() }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = origImg,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                        if (cleanImg != null) {
                            val splitX = size.width * splitFrac
                            clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                                drawImage(
                                    image = cleanImg,
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                )
                            }
                            drawLine(Color(0xFF0F6B54), Offset(splitX, 0f), Offset(splitX, size.height), strokeWidth = 5f)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(8.dp)) {
                            Text("আসল (Before)", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Surface(color = EmeraldPrimary.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp)) {
                            Text("পরিষ্কার (After)", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Text("আগে / পরে স্লাইডার (${(splitFrac * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = splitFrac, onValueChange = { splitFrac = it }, valueRange = 0.05f..0.95f)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("কন্ট্রাস্ট ${String.format(Locale.US, "%.1f", contrast)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 1.0f..2.2f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("কালো সীমা $threshold", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                    Slider(value = threshold.toFloat(), onValueChange = { threshold = it.toInt() }, valueRange = 120f..235f, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = despeckle, onCheckedChange = { despeckle = it })
                        Text("দাগ-ময়লা ছাঁটাই", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = invert, onCheckedChange = { invert = it })
                        Text("ইনভার্ট", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(onClick = { onGenerateClean(contrast, threshold, despeckle, invert) }, shape = RoundedCornerShape(10.dp)) {
                        Text("আপডেট")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onExportCleaned(false) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ক্লিন PNG")
                    }
                    Button(onClick = { onExportCleaned(true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ক্লিন PDF")
                    }
                }
            }
        }
    }
}

@Composable
fun MouzaParcelTableDialog(
    rows: List<ParcelTableRow>,
    getShoelaceSteps: (plotId: Int) -> Pair<Double, List<ShoelaceStepRow>>,
    onExportCsv: () -> Unit,
    onExportAccuracyPdf: () -> Unit,
    onExportAnnotatedPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var sortByAreaDesc by remember { mutableStateOf(false) }
    var shoelacePlotRow by remember { mutableStateOf<ParcelTableRow?>(null) }

    val filtered = remember(rows, search, sortByAreaDesc) {
        val q = search.trim().lowercase(Locale.US)
        val base = if (q.isEmpty()) rows else rows.filter {
            it.dagNumber.lowercase(Locale.US).contains(q) ||
                it.khatianNumber.lowercase(Locale.US).contains(q) ||
                it.ownerName.lowercase(Locale.US).contains(q)
        }
        if (sortByAreaDesc) base.sortedByDescending { it.decimal } else base
    }
    val usable = filtered.filterNot { it.isRoad }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("পার্সেল টেবিল ও শ্যুলেস হিসাব (${filtered.size}টি)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("দাগ / খতিয়ান / মালিক খুঁজুন...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    NeuPillChip(text = if (sortByAreaDesc) "বড়→ছোট" else "ক্রম", selected = sortByAreaDesc, onClick = { sortByAreaDesc = !sortByAreaDesc })
                }

                Surface(shape = RoundedCornerShape(12.dp), color = EmeraldPrimary.copy(alpha = 0.12f)) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "ব্যবহারযোগ্য মোট (${usable.size} দাগ, রাস্তা বাদে): %.3f শতক • %.3f কাঠা • %.0f ft²",
                            usable.sumOf { it.decimal }, usable.sumOf { it.katha }, usable.sumOf { it.sqFeet }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.plotId }) { row ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { shoelacePlotRow = row }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "দাগ: ${row.dagNumber}  •  খতিয়ান: ${row.khatianNumber}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (row.isRoad) "রাস্তা" else String.format(Locale.US, "%.2f ± %.2f শতক", row.decimal, row.errorDecimal),
                                        color = if (row.isRoad) MaterialTheme.colorScheme.error else EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "কাঠা: %.3f | বিঘা: %.3f | %.1f ft² | পরিসীমা: %.1f ft | মালিক: %s",
                                        row.katha, row.bigha, row.sqFeet, row.perimeterFt, row.ownerName
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "ধাপে ধাপে শ্যুলেস হিসাব দেখতে ট্যাপ করুন →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("CSV", maxLines = 1)
                    }
                    OutlinedButton(onClick = onExportAccuracyPdf, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("± ত্রুটি PDF", maxLines = 1)
                    }
                    Button(
                        onClick = onExportAnnotatedPdf,
                        modifier = Modifier.weight(1.3f).testTag("export_annotated_pdf_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("অ্যানোটেটেড PDF", maxLines = 1)
                    }
                }
            }
        }
    }

    shoelacePlotRow?.let { row ->
        val (areaSqFt, steps) = remember(row.plotId) { getShoelaceSteps(row.plotId) }
        AlertDialog(
            onDismissRequest = { shoelacePlotRow = null },
            title = { Text("শ্যুলেস (Shoelace) ধাপ — দাগ ${row.dagNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = String.format(Locale.US, "সূত্র: Area = ½ |Σ(xᵢ·yᵢ₊₁ − xᵢ₊₁·yᵢ)| = %.2f ft² (%.3f শতক)", areaSqFt, areaSqFt / 435.6),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    steps.forEach { st ->
                        Text(
                            text = String.format(
                                Locale.US,
                                "%d) (%.1f×%.1f) − (%.1f×%.1f) = %.1f",
                                st.vertexIndex, st.xFt, st.yNextFt, st.xNextFt, st.yFt, st.netTerm
                            ),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { shoelacePlotRow = null }) { Text("বন্ধ করুন") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouzaPlotActionsSheet(
    plot: MouzaPlot,
    allPlots: List<MouzaPlot>,
    pageWidthPts: Int,
    pageHeightPts: Int,
    scale: MouzaScale,
    scaleX: Double,
    scaleY: Double,
    hasDraftPolygon: Boolean,
    hasTapeLine: Boolean,
    measureBetweenResult: MeasureBetweenResult?,
    onSaveDetails: (dag: String, khatian: String, owner: String, remarks: String) -> Unit,
    onToggleLock: () -> Unit,
    onToggleRoad: () -> Unit,
    onToggleHideMeasures: () -> Unit,
    onToggleHighlight: () -> Unit,
    onAddHoleFromDraft: () -> Unit,
    onLockSideLength: (edgeIdx: Int, targetFeet: Double) -> Unit,
    onSplitByLastTapeLine: () -> Unit,
    onMeasureBetweenPlot: (otherPlotId: Int) -> Unit,
    onMeasureRoadFrontage: () -> Unit,
    onDismiss: () -> Unit
) {
    var dag by remember(plot.id) { mutableStateOf(plot.dagNumber) }
    var khat by remember(plot.id) { mutableStateOf(plot.khatianNumber) }
    var owner by remember(plot.id) { mutableStateOf(plot.ownerName) }
    var remarks by remember(plot.id) { mutableStateOf(plot.remarks) }
    val poly = plot.effectivePolygon().orEmpty()
    var selectedEdgeIdx by remember(plot.id) { mutableIntStateOf(0) }
    var targetEdgeFtText by remember(plot.id, selectedEdgeIdx) {
        val lenFt = if (poly.size >= 2) {
            val a = poly[selectedEdgeIdx.coerceIn(0, poly.lastIndex)]
            val b = poly[(selectedEdgeIdx + 1) % poly.size]
            MouzaPdfEngine.computeDistanceFeet(a, b, pageWidthPts, pageHeightPts, scale, 1.0, scaleX, scaleY)
        } else 0.0
        mutableStateOf(String.format(Locale.US, "%.2f", lenFt))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "দাগ মেনু (⋮) — দাগ #${plot.plotIndex} ${if (plot.dagNumber.isNotBlank()) "(${plot.dagNumber})" else ""}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = dag, onValueChange = { dag = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = khat, onValueChange = { khat = it }, label = { Text("খতিয়ান নং") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("মালিকের নাম") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("মন্তব্য") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = { onSaveDetails(dag.trim(), khat.trim(), owner.trim(), remarks.trim()) },
                enabled = !plot.isLocked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("তথ্য সংরক্ষণ করুন")
            }

            HorizontalDivider()
            Text("দাগ কন্ট্রোল ও লেআউট", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NeuPillChip(text = if (plot.isLocked) "লকড" else "লক করুন", selected = plot.isLocked, onClick = onToggleLock)
                NeuPillChip(text = if (plot.isRoad) "রাস্তা (বাদ)" else "রাস্তা চিহ্ন", selected = plot.isRoad, onClick = onToggleRoad)
                NeuPillChip(text = if (plot.hideMeasures) "মাপ লুকানো" else "মাপ লুকান", selected = plot.hideMeasures, onClick = onToggleHideMeasures)
                NeuPillChip(text = "হাইলাইট", selected = false, onClick = onToggleHighlight)
            }

            HorizontalDivider()
            Text("উন্নত জ্যামিতি: হোল কাট-আউট, বাহু লক ও বিভক্তি", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddHoleFromDraft,
                    enabled = hasDraftPolygon && !plot.isLocked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.LayersClear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("হোল/পুকুর বাদ", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onSplitByLastTapeLine,
                    enabled = hasTapeLine && !plot.isLocked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("রেখায় ২ ভাগ", maxLines = 1)
                }
            }

            if (poly.size >= 3) {
                Text("বাহু দৈর্ঘ্য লক (Lock Side Length):", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    poly.indices.forEach { idx ->
                        NeuPillChip(
                            text = "বাহু #${idx + 1}",
                            selected = selectedEdgeIdx == idx,
                            onClick = { selectedEdgeIdx = idx }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = targetEdgeFtText,
                        onValueChange = { targetEdgeFtText = it },
                        label = { Text("সঠিক দৈর্ঘ্য (ফুট)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val ft = targetEdgeFtText.toDoubleOrNull() ?: 0.0
                            if (ft > 0.0) onLockSideLength(selectedEdgeIdx, ft)
                        },
                        enabled = !plot.isLocked,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("দৈর্ঘ্য সেট")
                    }
                }
            }

            HorizontalDivider()
            Text("মধ্যবর্তী দূরত্ব ও রাস্তার মুখ (Measure Between)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                allPlots.filter { it.id != plot.id }.forEach { other ->
                    val lbl = other.dagNumber.ifBlank { "#${other.plotIndex}" }
                    OutlinedButton(onClick = { onMeasureBetweenPlot(other.id) }, shape = RoundedCornerShape(10.dp)) {
                        Text("↔ দাগ $lbl", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(onClick = onMeasureRoadFrontage, enabled = hasTapeLine, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("রাস্তার মুখ (ফিতা রেখা)", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (measureBetweenResult != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = EmeraldPrimary.copy(alpha = 0.12f)) {
                    Text(
                        text = "${measureBetweenResult.modeTitleBn}: ${measureBetweenResult.detailsBn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouzaPdfInspectAndToolsSheet(
    report: PdfInspectionReport?,
    renderDpi: Int,
    pageRotationDeg: Int,
    pageCount: Int,
    currentPage: Int,
    pageCompareBitmap: Bitmap?,
    displayBitmap: Bitmap?,
    onSetDpi: (Int) -> Unit,
    onRotate90: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onLoadComparePage: (Int?) -> Unit,
    onImportUncompressedVectors: () -> Unit,
    onDismiss: () -> Unit
) {
    var compareSplit by remember { mutableFloatStateOf(0.5f) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PDF সক্ষমতা, DPI, রোটেশন ও পৃষ্ঠা তুলনা", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (report != null) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "পৃষ্ঠা: $pageCount | ভেক্টর পাথ: ${if (report.hasVectorPaths) "${report.extractedVectorPolygons.size} পলিগন" else "পাওয়া যায়নি"} | টেক্সট: ${if (report.hasTextObjects) "হ্যাঁ" else "না"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(report.disclosureBn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (report.extractedVectorPolygons.isNotEmpty()) {
                            Button(onClick = onImportUncompressedVectors, shape = RoundedCornerShape(10.dp)) {
                                Text("ভেক্টর পলিগন (${report.extractedVectorPolygons.size}টি) প্লটে যুক্ত করুন")
                            }
                        }
                    }
                }
            }

            Text("রেন্ডার DPI ও পৃষ্ঠা ঘূর্ণন (${pageRotationDeg}°):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(150, 300, 600).forEach { dpi ->
                    NeuPillChip(text = "$dpi DPI", selected = renderDpi == dpi, onClick = { onSetDpi(dpi) })
                }
                OutlinedButton(onClick = onRotate90, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("৯০° ঘোরান")
                }
            }

            if (pageCount > 1) {
                Text("পৃষ্ঠা নির্বাচন ও পাশাপাশি তুলনা (Page Compare):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until pageCount).forEach { idx ->
                        NeuPillChip(
                            text = "পৃষ্ঠা ${idx + 1}",
                            selected = idx == currentPage,
                            onClick = { onSelectPage(idx) }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onLoadComparePage(null) }, shape = RoundedCornerShape(10.dp)) {
                        Text("তুলনা বন্ধ")
                    }
                    (0 until pageCount).filter { it != currentPage }.forEach { idx ->
                        OutlinedButton(onClick = { onLoadComparePage(idx) }, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("পৃষ্ঠা ${idx + 1} তুলনা")
                        }
                    }
                }
                if (pageCompareBitmap != null && displayBitmap != null) {
                    val leftImg = remember(displayBitmap) { displayBitmap.asImageBitmap() }
                    val rightImg = remember(pageCompareBitmap) { pageCompareBitmap.asImageBitmap() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(leftImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                            val sx = size.width * compareSplit
                            clipRect(left = sx, top = 0f, right = size.width, bottom = size.height) {
                                drawImage(rightImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                            }
                            drawLine(EmeraldPrimary, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 4f)
                        }
                    }
                    Slider(value = compareSplit, onValueChange = { compareSplit = it }, valueRange = 0.05f..0.95f)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
