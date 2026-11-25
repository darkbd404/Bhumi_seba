package com.example.ui.screens

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.LandCalculators
import com.example.engine.MouzaPdfEngine
import com.example.model.MouzaFilterPreset
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.MouzaSelectionTool
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.KhatianLookupBottomSheet
import com.example.ui.components.NeuPillChip
import com.example.ui.components.PrecisionNudgePad
import com.example.ui.components.UtilityBarChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.MouzaPdfViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouzaPdfScreen(
    onNavigateBack: () -> Unit,
    savedProjectId: String? = null,
    viewModel: MouzaPdfViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(savedProjectId) {
        if (!savedProjectId.isNullOrBlank()) {
            viewModel.restoreMouzaProjectById(savedProjectId)
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openDocumentUri(it) }
    }

    val khatianPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openKhatianDocumentUri(it) }
    }

    val scope = rememberCoroutineScope()
    var scale by remember(state.fileName, state.currentPage) { mutableFloatStateOf(1f) }
    var offsetX by remember(state.fileName, state.currentPage) { mutableFloatStateOf(0f) }
    var offsetY by remember(state.fileName, state.currentPage) { mutableFloatStateOf(0f) }
    var trackedBoxW by remember(state.fileName, state.currentPage) { mutableFloatStateOf(900f) }
    var trackedBoxH by remember(state.fileName, state.currentPage) { mutableFloatStateOf(900f) }
    var trackedFitW by remember(state.fileName, state.currentPage) { mutableFloatStateOf(900f) }
    var trackedFitH by remember(state.fileName, state.currentPage) { mutableFloatStateOf(900f) }

    LaunchedEffect(state.highlightedPlotId) {
        if (state.highlightedPlotId != null) {
            kotlinx.coroutines.delay(5500L)
            viewModel.clearHighlightedPlot()
        }
    }

    fun animateCameraTo(targetScale: Float, targetOffX: Float, targetOffY: Float) {
        val startScale = scale
        val startX = offsetX
        val startY = offsetY
        scope.launch {
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) {
                val t = value
                scale = startScale + (targetScale - startScale) * t
                offsetX = startX + (targetOffX - startX) * t
                offsetY = startY + (targetOffY - startY) * t
            }
        }
    }

    var showScaleMenu by remember { mutableStateOf(false) }
    var showGapMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showKhatianSheet by remember { mutableStateOf(false) }
    var showSavedProjectsSheet by remember { mutableStateOf(false) }
    var showSaveProjectDialog by remember { mutableStateOf(false) }
    var pointerModeEnabled by remember { mutableStateOf(true) }
    var loupeEnabled by remember { mutableStateOf(true) }
    var loupeZoomLevel by remember { mutableFloatStateOf(3.5f) }
    var showNudgePad by remember { mutableStateOf(false) }
    var nudgeTargetIsPoint by remember { mutableStateOf(true) }
    var nudgeStepLevel by remember { mutableIntStateOf(0) }
    var editingPlot by remember { mutableStateOf<MouzaPlot?>(null) }
    var showCalibrateDialog by remember { mutableStateOf<MouzaPlot?>(null) }

    val loupeAnimScale by animateFloatAsState(
        targetValue = if (pointerModeEnabled && loupeEnabled) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "mouzaLoupeScale"
    )
    val pulseTransition = rememberInfiniteTransition(label = "mouzaPulse")
    val highlightPulse by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "mouzaHighlightPulse"
    )

    BackHandler(
        enabled = showReportSheet || showFilterSheet || showKhatianSheet || showSavedProjectsSheet ||
            showSaveProjectDialog || editingPlot != null || showCalibrateDialog != null
    ) {
        when {
            showSaveProjectDialog -> showSaveProjectDialog = false
            showCalibrateDialog != null -> showCalibrateDialog = null
            editingPlot != null -> editingPlot = null
            showKhatianSheet -> showKhatianSheet = false
            showSavedProjectsSheet -> showSavedProjectsSheet = false
            showFilterSheet -> showFilterSheet = false
            showReportSheet -> showReportSheet = false
        }
    }

    val colorMatrixFilter = remember(state.filterPreset, state.brightness, state.contrast) {
        buildFilterColorMatrix(state.filterPreset, state.brightness, state.contrast)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("mouza_back_button")
                        .neuClickable(cornerRadius = 12.dp, elevation = 4.dp, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.fileName.ifBlank { "মৌজা ম্যাপ (PDF / ছবি)" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.hdReady) "HD • ${state.selectedScale.ratioLabel} • 1000x জুম"
                        else if (state.displayBitmap != null) "HD প্রস্তুত হচ্ছে..."
                        else "স্টোরেজ থেকে PDF বা ছবি নিন",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Separate Khatian Upload & Search Button
                IconButton(
                    onClick = { showKhatianSheet = true },
                    modifier = Modifier.size(36.dp).testTag("mouza_khatian_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = "Upload Khatian & Search Dag",
                        tint = if (state.khatianPreviewBitmap != null || state.khatianRecords.isNotEmpty()) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Saved Mouza Projects Sheet Trigger
                IconButton(
                    onClick = { showSavedProjectsSheet = true },
                    modifier = Modifier.size(36.dp).testTag("mouza_saved_projects_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = "Saved Projects",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (state.displayBitmap != null) {
                    IconButton(
                        onClick = { viewModel.undoLastPlot() },
                        enabled = state.selectedPlots.isNotEmpty() || state.tapeDraft.isNotEmpty(),
                        modifier = Modifier.size(34.dp).testTag("mouza_undo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", modifier = Modifier.size(19.dp))
                    }
                    IconButton(
                        onClick = { viewModel.redoLastPlot() },
                        enabled = state.canRedo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo", modifier = Modifier.size(19.dp))
                    }
                    IconButton(
                        onClick = { viewModel.clearAllPlots() },
                        enabled = state.selectedPlots.isNotEmpty() || state.tapeDraft.isNotEmpty() || state.completedTapes.isNotEmpty(),
                        modifier = Modifier.size(34.dp).testTag("mouza_clear_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                    }
                }

                Button(
                    onClick = { docPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.testTag("pick_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.displayBitmap == null) "ফাইল নিন" else "বদলান", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (state.isLoadingPdf || state.isAutoFilling) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Fixed Top Function Bar 1: Selection Tools & Scale
            if (state.displayBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuPillChip(
                        text = "পয়েন্টার",
                        selected = pointerModeEnabled,
                        icon = Icons.Rounded.GpsFixed,
                        testTag = "tool_pointer_toggle",
                        onClick = { pointerModeEnabled = !pointerModeEnabled }
                    )
                    NeuPillChip(
                        text = "পলিগন",
                        selected = state.activeTool == MouzaSelectionTool.POLYGON_DRAW,
                        icon = Icons.Rounded.Draw,
                        testTag = "tool_manual_polygon",
                        onClick = { viewModel.setTool(MouzaSelectionTool.POLYGON_DRAW) }
                    )
                    NeuPillChip(
                        text = "অটো ফিল",
                        selected = state.activeTool == MouzaSelectionTool.AUTO_TAP,
                        icon = Icons.Rounded.AutoFixHigh,
                        testTag = "tool_auto_tap",
                        onClick = { viewModel.setTool(MouzaSelectionTool.AUTO_TAP) }
                    )
                    NeuPillChip(
                        text = "পয়েন্ট এডিট",
                        selected = state.activeTool == MouzaSelectionTool.EDIT_CORNERS,
                        icon = Icons.Rounded.Adjust,
                        testTag = "tool_edit_corners",
                        onClick = { viewModel.setTool(MouzaSelectionTool.EDIT_CORNERS) }
                    )
                    NeuPillChip(
                        text = "ফিতা মাপ",
                        selected = state.activeTool == MouzaSelectionTool.TAPE_MEASURE,
                        icon = Icons.Rounded.Straighten,
                        testTag = "tool_tape_measure",
                        onClick = { viewModel.setTool(MouzaSelectionTool.TAPE_MEASURE) }
                    )
                    NeuPillChip(
                        text = "প্যান/মুভ",
                        selected = state.activeTool == MouzaSelectionTool.PAN_ONLY,
                        icon = Icons.Rounded.PanTool,
                        testTag = "tool_pan_only",
                        onClick = { viewModel.setTool(MouzaSelectionTool.PAN_ONLY) }
                    )

                    Box {
                        NeuPillChip(
                            text = state.selectedScale.ratioLabel,
                            selected = false,
                            icon = Icons.Rounded.Tune,
                            testTag = "scale_picker_chip",
                            onClick = { showScaleMenu = true }
                        )
                        DropdownMenu(
                            expanded = showScaleMenu,
                            onDismissRequest = { showScaleMenu = false }
                        ) {
                            MouzaScale.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.labelBn} (${s.ratioLabel})") },
                                    onClick = {
                                        viewModel.setScale(s)
                                        showScaleMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        val gapLabel = when (state.gapSealLevel) {
                            0 -> "সিল: সাধারণ"
                            2 -> "সিল: শক্তিশালী"
                            else -> "সিল: ব্যালেন্সড"
                        }
                        NeuPillChip(
                            text = gapLabel,
                            selected = false,
                            testTag = "gap_seal_chip",
                            onClick = { showGapMenu = true }
                        )
                        DropdownMenu(
                            expanded = showGapMenu,
                            onDismissRequest = { showGapMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("সাধারণ (স্পষ্ট রেখার জন্য)") },
                                onClick = { viewModel.setGapSealLevel(0); showGapMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("ব্যালেন্সড (সুপারিশকৃত)") },
                                onClick = { viewModel.setGapSealLevel(1); showGapMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("শক্তিশালী (ভাঙা সীমানার জন্য)") },
                                onClick = { viewModel.setGapSealLevel(2); showGapMenu = false }
                            )
                        }
                    }

                    if (state.pageCount > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .neuSurface(cornerRadius = 14.dp, elevation = 3.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.selectPage(state.currentPage - 1) },
                                enabled = state.currentPage > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev Page")
                            }
                            Text(
                                text = "${state.currentPage + 1}/${state.pageCount}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            IconButton(
                                onClick = { viewModel.selectPage(state.currentPage + 1) },
                                enabled = state.currentPage < state.pageCount - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Page")
                            }
                        }
                    }
                }

                // Fixed Top Utility Bar 2 (keeps Zoom, Fit, Magnifier, Filter, Khatian & Fine-Nudge toggle OUTSIDE the PDF canvas!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UtilityBarChip(
                        label = String.format(Locale.US, "%.1fx", scale),
                        icon = null,
                        selected = scale > 1.05f,
                        onClick = { animateCameraTo(1f, 0f, 0f) }
                    )
                    UtilityBarChip(
                        label = "-",
                        icon = Icons.Rounded.Remove,
                        selected = false,
                        onClick = {
                            val next = (scale / 1.6f).coerceIn(1f, 1000f)
                            if (next <= 1.02f) animateCameraTo(1f, 0f, 0f)
                            else animateCameraTo(next, offsetX, offsetY)
                        }
                    )
                    UtilityBarChip(
                        label = "+",
                        icon = Icons.Rounded.Add,
                        selected = false,
                        onClick = {
                            animateCameraTo((scale * 1.6f).coerceIn(1f, 1000f), offsetX, offsetY)
                        }
                    )
                    UtilityBarChip(
                        label = "ফিট",
                        icon = Icons.Rounded.ZoomOutMap,
                        selected = false,
                        onClick = { animateCameraTo(1f, 0f, 0f) }
                    )
                    UtilityBarChip(
                        label = if (!loupeEnabled) "লুপ বন্ধ" else "লুপ ${loupeZoomLevel}x",
                        icon = Icons.Rounded.ZoomIn,
                        selected = loupeEnabled && pointerModeEnabled,
                        testTag = "mouza_loupe_toggle",
                        onClick = {
                            if (!pointerModeEnabled) {
                                pointerModeEnabled = true
                                loupeEnabled = true
                            } else if (!loupeEnabled) {
                                loupeEnabled = true
                                loupeZoomLevel = 2.5f
                            } else if (loupeZoomLevel < 3.4f) {
                                loupeZoomLevel = 3.5f
                            } else if (loupeZoomLevel < 4.9f) {
                                loupeZoomLevel = 5.0f
                            } else {
                                loupeEnabled = false
                            }
                        }
                    )
                    UtilityBarChip(
                        label = "ফিল্টার",
                        icon = Icons.Rounded.FilterBAndW,
                        selected = state.filterPreset != MouzaFilterPreset.NORMAL,
                        onClick = { showFilterSheet = true }
                    )
                    UtilityBarChip(
                        label = "খতিয়ান",
                        icon = Icons.Rounded.Description,
                        selected = state.khatianRecords.isNotEmpty() || state.khatianPreviewBitmap != null,
                        onClick = { showKhatianSheet = true }
                    )
                    UtilityBarChip(
                        label = "সূক্ষ্ম মুভ",
                        icon = Icons.Rounded.OpenWith,
                        selected = showNudgePad,
                        testTag = "mouza_nudge_toggle",
                        onClick = { showNudgePad = !showNudgePad }
                    )
                }
            }

            // Main Live-Tile Ultra-HD 1000x Canvas Area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .neuSurface(cornerRadius = 20.dp, elevation = 6.dp, backgroundColor = Color.White)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                val bmp = state.displayBitmap
                if (bmp == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .neuSurface(
                                    cornerRadius = 26.dp,
                                    elevation = 6.dp,
                                    backgroundColor = EmeraldPrimary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "মৌজা ম্যাপ PDF বা ছবি সিলেক্ট করুন",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF16222B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "১. ১০০০x পর্যন্ত আসল Ultra-HD ভেক্টর জুম — লেখা বা দাগ কখনো ঝাপসা হবে না\n২. 'অটো ফিল'-এ ট্যাপ করলেই দাগ নাম্বারসহ সম্পূর্ণ প্লট সিলেক্ট হয়ে যাবে\n৩. সামান্য এদিক-সেদিক হলে 'পয়েন্ট এডিট'-এ জুম করে কোণা হাতে টেনে ঠিক করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF536574)
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { docPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                            modifier = Modifier.testTag("empty_pick_pdf_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ফোন স্টোরেজ থেকে PDF বা ছবি বাছুন")
                        }
                        if (state.savedProjects.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showSavedProjectsSheet = true },
                                modifier = Modifier.testTag("empty_restore_mouza_button"),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("আগের সংরক্ষিত কাজ (${state.savedProjects.size}টি) রিস্টোর করুন")
                            }
                        }
                    }
                } else {
                    val boxW = constraints.maxWidth.toFloat().coerceAtLeast(100f)
                    val boxH = constraints.maxHeight.toFloat().coerceAtLeast(100f)
                    val fitScale = min(boxW / bmp.width.toFloat(), boxH / bmp.height.toFloat())
                    val fitW = bmp.width * fitScale
                    val fitH = bmp.height * fitScale
                    val imgLeft = (boxW - fitW) / 2f
                    val imgTop = (boxH - fitH) / 2f
                    val cx = boxW / 2f
                    val cy = boxH / 2f

                    SideEffect {
                        trackedBoxW = boxW
                        trackedBoxH = boxH
                        trackedFitW = fitW
                        trackedFitH = fitH
                    }

                    fun toScreenX(nx: Float): Float = ((imgLeft + nx * fitW) - cx) * scale + cx + offsetX
                    fun toScreenY(ny: Float): Float = ((imgTop + ny * fitH) - cy) * scale + cy + offsetY
                    fun toNormX(sx: Float): Float = (((sx - cx - offsetX) / scale + cx) - imgLeft) / fitW
                    fun toNormY(sy: Float): Float = (((sy - cy - offsetY) / scale + cy) - imgTop) / fitH

                    // Focus smoothly with animation on a plot requested from Khatian or Report sheet
                    LaunchedEffect(state.focusPlotRequest) {
                        val fp = state.focusPlotRequest ?: return@LaunchedEffect
                        val poly = fp.effectivePolygon()
                        if (!poly.isNullOrEmpty()) {
                            val minNx = poly.minOf { it.first }
                            val maxNx = poly.maxOf { it.first }
                            val minNy = poly.minOf { it.second }
                            val maxNy = poly.maxOf { it.second }
                            val pw = ((maxNx - minNx) * fitW).coerceAtLeast(20f)
                            val ph = ((maxNy - minNy) * fitH).coerceAtLeast(20f)
                            val targetScale = min(boxW / (pw * 1.6f), boxH / (ph * 1.6f)).coerceIn(1.5f, 80f)
                            val targetCenterNx = (minNx + maxNx) / 2f
                            val targetCenterNy = (minNy + maxNy) / 2f
                            animateCameraTo(
                                targetScale = targetScale,
                                targetOffX = -((imgLeft + targetCenterNx * fitW) - cx) * targetScale,
                                targetOffY = -((imgTop + targetCenterNy * fitH) - cy) * targetScale
                            )
                        }
                        viewModel.consumeFocusPlot()
                    }

                    // Live Viewport Ultra-HD Re-Render on pan/zoom stop (up to 1000x!)
                    LaunchedEffect(scale, offsetX, offsetY, state.fileName, state.currentPage, boxW, boxH) {
                        val nl = toNormX(0f).coerceIn(0f, 0.9999f)
                        val nt = toNormY(0f).coerceIn(0f, 0.9999f)
                        val nr = toNormX(boxW).coerceIn(nl + 0.0002f, 1f)
                        val nb = toNormY(boxH).coerceIn(nt + 0.0002f, 1f)
                        viewModel.requestViewportHdRender(
                            zoomScale = scale,
                            normLeft = nl,
                            normTop = nt,
                            normRight = nr,
                            normBottom = nb,
                            viewportWidthPx = boxW.roundToInt(),
                            viewportHeightPx = boxH.roundToInt()
                        )
                    }

                    val latestState by rememberUpdatedState(state)
                    val latestPointerMode by rememberUpdatedState(pointerModeEnabled)
                    val latestFitW by rememberUpdatedState(fitW)
                    val latestFitH by rememberUpdatedState(fitH)
                    val latestImgLeft by rememberUpdatedState(imgLeft)
                    val latestImgTop by rememberUpdatedState(imgTop)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // PC / Trackpad Scroll-Wheel Zoom support
                            .pointerInput(bmp, fitW, fitH) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val change = event.changes.firstOrNull() ?: continue
                                            val deltaY = change.scrollDelta.y
                                            if (deltaY != 0f) {
                                                val zoomFactor = if (deltaY < 0f) 1.18f else 1f / 1.18f
                                                val oldScale = scale
                                                val newScale = (oldScale * zoomFactor).coerceIn(1f, 1000f)
                                                if (newScale <= 1.01f) {
                                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                                } else {
                                                    val pos = change.position
                                                    val ratio = newScale / oldScale
                                                    val nextOffX = (offsetX - (pos.x - cx)) * ratio + (pos.x - cx)
                                                    val nextOffY = (offsetY - (pos.y - cy)) * ratio + (pos.y - cy)
                                                    val maxOffX = max(boxW * newScale * 0.75f, boxW * 0.5f)
                                                    val maxOffY = max(boxH * newScale * 0.75f, boxH * 0.5f)
                                                    scale = newScale
                                                    offsetX = nextOffX.coerceIn(-maxOffX, maxOffX)
                                                    offsetY = nextOffY.coerceIn(-maxOffY, maxOffY)
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                            // Unified multi-touch Pan + Pinch-Zoom + Vertex Drag (never restarts on draft edits!)
                            .pointerInput(bmp, boxW, boxH) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val curState = latestState
                                    var activeDraftDrag: Int? = null
                                    var activeVertexDrag: Pair<Int, Int>? = null

                                    fun curToScreenX(nx: Float): Float =
                                        ((latestImgLeft + nx * latestFitW) - cx) * scale + cx + offsetX
                                    fun curToScreenY(ny: Float): Float =
                                        ((latestImgTop + ny * latestFitH) - cy) * scale + cy + offsetY
                                    fun curToNormX(sx: Float): Float =
                                        (((sx - cx - offsetX) / scale + cx) - latestImgLeft) / latestFitW
                                    fun curToNormY(sy: Float): Float =
                                        (((sy - cy - offsetY) / scale + cy) - latestImgTop) / latestFitH

                                    if (curState.activeTool == MouzaSelectionTool.EDIT_CORNERS && curState.selectedPlots.isNotEmpty()) {
                                        var best: Pair<Int, Int>? = null
                                        var bestDist = 48f
                                        curState.selectedPlots.forEach { plot ->
                                            val poly = plot.effectivePolygon() ?: return@forEach
                                            poly.forEachIndexed { idx, (nx, ny) ->
                                                val d = hypot(curToScreenX(nx) - down.position.x, curToScreenY(ny) - down.position.y)
                                                if (d < bestDist) {
                                                    bestDist = d
                                                    best = plot.id to idx
                                                }
                                            }
                                        }
                                        activeVertexDrag = best
                                    } else if (curState.activeTool == MouzaSelectionTool.POLYGON_DRAW && curState.manualPolygonDraft.isNotEmpty()) {
                                        // Drag draft point if pointer mode is off OR user drags an explicitly selected draft point
                                        val selIdx = curState.selectedDraftIdx
                                        if (!latestPointerMode) {
                                            var bestIdx: Int? = null
                                            var bestDist = 42f
                                            curState.manualPolygonDraft.forEachIndexed { idx, (nx, ny) ->
                                                val d = hypot(curToScreenX(nx) - down.position.x, curToScreenY(ny) - down.position.y)
                                                if (d < bestDist) {
                                                    bestDist = d
                                                    bestIdx = idx
                                                }
                                            }
                                            activeDraftDrag = bestIdx
                                        } else if (selIdx != null && selIdx in curState.manualPolygonDraft.indices) {
                                            val (nx, ny) = curState.manualPolygonDraft[selIdx]
                                            if (hypot(curToScreenX(nx) - down.position.x, curToScreenY(ny) - down.position.y) < 48f) {
                                                activeDraftDrag = selIdx
                                            }
                                        }
                                    }

                                    var totalMove = 0f
                                    var wasMultiTouch = false
                                    val touchSlop = viewConfiguration.touchSlop

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) break

                                        if (pressed.size >= 2) {
                                            wasMultiTouch = true
                                            activeDraftDrag = null
                                            activeVertexDrag = null
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            val centroid = event.calculateCentroid(useCurrent = false)
                                            val oldScale = scale
                                            val newScale = (oldScale * zoom).coerceIn(1f, 1000f)
                                            if (newScale <= 1.01f) {
                                                scale = 1f; offsetX = 0f; offsetY = 0f
                                            } else {
                                                val ratio = newScale / oldScale
                                                val nextOffX = (offsetX - (centroid.x - cx)) * ratio + (centroid.x - cx) + pan.x
                                                val nextOffY = (offsetY - (centroid.y - cy)) * ratio + (centroid.y - cy) + pan.y
                                                val maxOffX = max(boxW * newScale * 0.75f, boxW * 0.5f)
                                                val maxOffY = max(boxH * newScale * 0.75f, boxH * 0.5f)
                                                scale = newScale
                                                offsetX = nextOffX.coerceIn(-maxOffX, maxOffX)
                                                offsetY = nextOffY.coerceIn(-maxOffY, maxOffY)
                                            }
                                            pressed.forEach { it.consume() }
                                        } else {
                                            val c = pressed.first()
                                            val delta = c.positionChange()
                                            totalMove += delta.getDistance()
                                            if (!wasMultiTouch && totalMove > touchSlop) {
                                                when {
                                                    activeDraftDrag != null -> {
                                                        viewModel.updateManualDraftPoint(
                                                            activeDraftDrag,
                                                            curToNormX(c.position.x),
                                                            curToNormY(c.position.y)
                                                        )
                                                    }
                                                    activeVertexDrag != null -> {
                                                        viewModel.updatePlotVertex(
                                                            activeVertexDrag.first,
                                                            activeVertexDrag.second,
                                                            curToNormX(c.position.x),
                                                            curToNormY(c.position.y)
                                                        )
                                                    }
                                                    else -> {
                                                        val maxOffX = max(boxW * scale * 0.75f, boxW * 0.5f)
                                                        val maxOffY = max(boxH * scale * 0.75f, boxH * 0.5f)
                                                        offsetX = (offsetX + delta.x).coerceIn(-maxOffX, maxOffX)
                                                        offsetY = (offsetY + delta.y).coerceIn(-maxOffY, maxOffY)
                                                    }
                                                }
                                                c.consume()
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(bmp, fitW, fitH, imgLeft, imgTop) {
                                detectTapGestures(
                                    onDoubleTap = { tap ->
                                        if (scale > 2.4f) {
                                            animateCameraTo(1f, 0f, 0f)
                                        } else {
                                            val targetScale = 4f
                                            val ratio = targetScale / scale
                                            animateCameraTo(
                                                targetScale,
                                                (offsetX - (tap.x - cx)) * ratio + (tap.x - cx),
                                                (offsetY - (tap.y - cy)) * ratio + (tap.y - cy)
                                            )
                                        }
                                    },
                                    onTap = { tap ->
                                        val nx = toNormX(tap.x)
                                        val ny = toNormY(tap.y)
                                        if (nx in 0f..1f && ny in 0f..1f) {
                                            val hitNorm = (22.dp.toPx() / (min(fitW, fitH) * scale)).coerceIn(0.0002f, 0.03f)
                                            viewModel.onMapTap(nx, ny, hitNorm)
                                        }
                                    }
                                )
                            }
                    ) {
                        val edgeTextPaint = remember {
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                textSize = 27f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                        }
                        val edgeStrokePaint = remember {
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                style = Paint.Style.STROKE
                                strokeWidth = 7f
                                textSize = 27f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                        }
                        val centerLabelPaint = remember {
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                textSize = 31f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                        }
                        val centerStrokePaint = remember {
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                style = Paint.Style.STROKE
                                strokeWidth = 8f
                                textSize = 31f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                        }

                        // Unscaled Screen-Space Vector + Live HD Tile Canvas (zero rounding jitter up to 1000x!)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sheetL = toScreenX(0f)
                            val sheetT = toScreenY(0f)
                            val sheetR = toScreenX(1f)
                            val sheetB = toScreenY(1f)

                            drawIntoCanvas { canvas ->
                                val native = canvas.nativeCanvas
                                val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                                    colorFilter = colorMatrixFilter
                                }

                                // 1. Draw base overview bitmap across visible screen (fast crop via srcRect so 1000x never explodes Canvas matrix)
                                val visL = ((0f - sheetL) / (sheetR - sheetL)).coerceIn(0f, 1f)
                                val visT = ((0f - sheetT) / (sheetB - sheetT)).coerceIn(0f, 1f)
                                val visR = ((size.width - sheetL) / (sheetR - sheetL)).coerceIn(0f, 1f)
                                val visB = ((size.height - sheetT) / (sheetB - sheetT)).coerceIn(0f, 1f)
                                if (visR > visL && visB > visT) {
                                    val srcR = Rect(
                                        (visL * bmp.width).toInt().coerceIn(0, bmp.width - 1),
                                        (visT * bmp.height).toInt().coerceIn(0, bmp.height - 1),
                                        (visR * bmp.width).roundToInt().coerceIn(1, bmp.width),
                                        (visB * bmp.height).roundToInt().coerceIn(1, bmp.height)
                                    )
                                    val dstR = RectF(
                                        sheetL + visL * (sheetR - sheetL),
                                        sheetT + visT * (sheetB - sheetT),
                                        sheetL + visR * (sheetR - sheetL),
                                        sheetT + visB * (sheetB - sheetT)
                                    )
                                    native.drawBitmap(bmp, srcR, dstR, bitmapPaint)
                                }

                                // 2. Overlay crisp native-resolution Viewport HD Tile when zoomed in (>= 2.2x)
                                val hdTile = state.viewportTile
                                if (hdTile != null && scale >= 2.2f && !hdTile.bitmap.isRecycled) {
                                    val tileDst = RectF(
                                        toScreenX(hdTile.normLeft),
                                        toScreenY(hdTile.normTop),
                                        toScreenX(hdTile.normRight),
                                        toScreenY(hdTile.normBottom)
                                    )
                                    native.drawBitmap(
                                        hdTile.bitmap,
                                        Rect(0, 0, hdTile.bitmap.width, hdTile.bitmap.height),
                                        tileDst,
                                        bitmapPaint
                                    )
                                }
                            }

                            // 3. Draw Auto-Filled / Manual Polygons + Side Edge Feet Lengths + Vertex Handles
                            state.selectedPlots.forEach { plot ->
                                val poly = plot.effectivePolygon()
                                if (poly != null && poly.size >= 3) {
                                    val screenPts = poly.map { (nx, ny) ->
                                        Offset(toScreenX(nx), toScreenY(ny))
                                    }
                                    val path = Path().apply {
                                        screenPts.forEachIndexed { idx, pt ->
                                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                        }
                                        close()
                                    }
                                    val isHighlighted = state.highlightedPlotId == plot.id
                                    drawPath(
                                        path,
                                        color = plot.color.copy(alpha = if (isHighlighted) 0.25f + 0.22f * highlightPulse else 0.30f)
                                    )
                                    drawPath(
                                        path,
                                        color = if (isHighlighted) Color(0xFFFFD54F) else plot.color,
                                        style = Stroke(width = if (isHighlighted) 6.5f else 4.5f)
                                    )

                                    val centroidPt = plot.centroidNormalized?.let {
                                        Offset(toScreenX(it.first), toScreenY(it.second))
                                    } ?: Offset(
                                        screenPts.map { it.x }.average().toFloat(),
                                        screenPts.map { it.y }.average().toFloat()
                                    )

                                    val plotColorInt = plot.color.toArgb()
                                    edgeTextPaint.color = plotColorInt

                                    // Edge foot measurements + corner dots
                                    val isEditMode = state.activeTool == MouzaSelectionTool.EDIT_CORNERS
                                    for (i in poly.indices) {
                                        val sA = screenPts[i]
                                        val sB = screenPts[(i + 1) % screenPts.size]
                                        val pA = poly[i]
                                        val pB = poly[(i + 1) % poly.size]

                                        // Edge length label
                                        val edgePx = hypot(sB.x - sA.x, sB.y - sA.y)
                                        if (edgePx >= 56f) {
                                            val feet = MouzaPdfEngine.computeDistanceFeet(
                                                pA, pB,
                                                state.pdfPageWidthPts,
                                                state.pdfPageHeightPts,
                                                state.selectedScale,
                                                state.calibrationMultiplier
                                            )
                                            val text = LandCalculators.formatCompactFeet(feet)
                                            var ang = Math.toDegrees(
                                                atan2((sB.y - sA.y).toDouble(), (sB.x - sA.x).toDouble())
                                            ).toFloat()
                                            if (ang > 90f) ang -= 180f
                                            if (ang < -90f) ang += 180f

                                            val mx = (sA.x + sB.x) / 2f
                                            val my = (sA.y + sB.y) / 2f
                                            drawIntoCanvas { canvas ->
                                                val nc = canvas.nativeCanvas
                                                nc.save()
                                                nc.translate(mx, my)
                                                nc.rotate(ang)
                                                nc.drawText(text, 0f, -10f, edgeStrokePaint)
                                                nc.drawText(text, 0f, -10f, edgeTextPaint)
                                                nc.restore()
                                            }
                                        }

                                        // Midpoint split indicator in Edit Corners mode
                                        if (isEditMode && edgePx >= 48f) {
                                            val mx = (sA.x + sB.x) / 2f
                                            val my = (sA.y + sB.y) / 2f
                                            drawCircle(Color.White, radius = 6f, center = Offset(mx, my))
                                            drawCircle(plot.color, radius = 4f, center = Offset(mx, my))
                                        }

                                        // Vertex corner handle (highlight selected corner brightly for edit/delete)
                                        val isSelVertex = state.selectedVertex == (plot.id to i)
                                        val handleRadius = when {
                                            isSelVertex -> 16f
                                            isEditMode -> 13f
                                            else -> 6.5f
                                        }
                                        drawCircle(
                                            color = if (isSelVertex) Color(0xFFE53935) else Color.White,
                                            radius = handleRadius + 3f,
                                            center = sA
                                        )
                                        drawCircle(
                                            color = when {
                                                isSelVertex -> Color(0xFFFFEA00)
                                                isEditMode -> Color(0xFFFFB300)
                                                else -> plot.color
                                            },
                                            radius = handleRadius,
                                            center = sA
                                        )
                                    }

                                    // Plot label inside polygon when big enough
                                    val minX = screenPts.minOf { it.x }
                                    val maxX = screenPts.maxOf { it.x }
                                    val minY = screenPts.minOf { it.y }
                                    val maxY = screenPts.maxOf { it.y }
                                    if (maxX - minX > 72f && maxY - minY > 44f) {
                                        val plotArea = LandCalculators.fromSqFeet(viewModel.plotAreaSqFeet(plot))
                                        val titleText = if (plot.dagNumber.isNotBlank()) "দাগ ${plot.dagNumber}" else "দাগ #${plot.plotIndex}"
                                        val subText = String.format(Locale.US, "%.2f শতক", plotArea.decimal)
                                        drawIntoCanvas { canvas ->
                                            val nc = canvas.nativeCanvas
                                            centerLabelPaint.color = plotColorInt
                                            nc.drawText(titleText, centroidPt.x, centroidPt.y - 6f, centerStrokePaint)
                                            nc.drawText(titleText, centroidPt.x, centroidPt.y - 6f, centerLabelPaint)
                                            nc.drawText(subText, centroidPt.x, centroidPt.y + 30f, centerStrokePaint)
                                            nc.drawText(subText, centroidPt.x, centroidPt.y + 30f, centerLabelPaint)
                                        }
                                    }
                                }
                            }

                            // 4. Draw Completed & Active Tape Measure Lines + Edge Distances
                            fun drawTapePath(pts: List<Pair<Float, Float>>, isDraft: Boolean) {
                                if (pts.isEmpty()) return
                                val screenPts = pts.map { Offset(toScreenX(it.first), toScreenY(it.second)) }
                                val path = Path().apply {
                                    screenPts.forEachIndexed { idx, pt ->
                                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                    }
                                }
                                if (screenPts.size >= 2) {
                                    drawPath(path, color = Color(0xFFE08A00), style = Stroke(width = 5f))
                                }
                                screenPts.forEachIndexed { i, pt ->
                                    if (i > 0) {
                                        val prev = screenPts[i - 1]
                                        val feet = MouzaPdfEngine.computeDistanceFeet(
                                            pts[i - 1], pts[i],
                                            state.pdfPageWidthPts,
                                            state.pdfPageHeightPts,
                                            state.selectedScale,
                                            state.calibrationMultiplier
                                        )
                                        val lbl = LandCalculators.formatFeetInches(feet)
                                        val mx = (prev.x + pt.x) / 2f
                                        val my = (prev.y + pt.y) / 2f
                                        drawIntoCanvas { canvas ->
                                            val nc = canvas.nativeCanvas
                                            edgeTextPaint.color = android.graphics.Color.parseColor("#B36B00")
                                            nc.drawText(lbl, mx, my - 10f, edgeStrokePaint)
                                            nc.drawText(lbl, mx, my - 10f, edgeTextPaint)
                                        }
                                    }
                                    drawCircle(Color.White, radius = if (isDraft) 11f else 8f, center = pt)
                                    drawCircle(Color(0xFFFFC107), radius = if (isDraft) 8f else 5.5f, center = pt)
                                }
                            }
                            state.completedTapes.forEach { drawTapePath(it, isDraft = false) }
                            drawTapePath(state.tapeDraft, isDraft = true)

                            // 5. Draw Active Manual Polygon Draft (highlight selected draft point for edit/delete)
                            val draft = state.manualPolygonDraft
                            if (draft.isNotEmpty()) {
                                val path = Path()
                                val screenPts = draft.map { (nx, ny) -> Offset(toScreenX(nx), toScreenY(ny)) }
                                screenPts.forEachIndexed { idx, pt ->
                                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                                }
                                if (draft.size >= 2) {
                                    drawPath(path, color = EmeraldPrimary, style = Stroke(width = 4.5f))
                                }
                                screenPts.forEachIndexed { idx, pt ->
                                    val isSel = state.selectedDraftIdx == idx
                                    drawCircle(
                                        color = if (isSel) Color(0xFFE53935) else Color.White,
                                        radius = if (isSel) 15f else 12f,
                                        center = pt
                                    )
                                    drawCircle(
                                        color = if (isSel) Color(0xFFFFD54F) else EmeraldPrimary,
                                        radius = if (isSel) 11f else 8.5f,
                                        center = pt
                                    )
                                }
                            }

                            // 6. Enlarged Precision Crosshair Pointer + Smooth Animated Magnifying Loupe
                            if (pointerModeEnabled) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = 44f
                                val gap = 12f
                                val arm = 64f

                                drawCircle(
                                    color = Color.White.copy(alpha = 0.9f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 6.5f)
                                )
                                drawCircle(
                                    color = Color(0xFFE53935),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 3.2f)
                                )
                                drawLine(Color.White, Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), strokeWidth = 6.5f)
                                drawLine(Color.White, Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), strokeWidth = 6.5f)
                                drawLine(Color.White, Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), strokeWidth = 6.5f)
                                drawLine(Color.White, Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), strokeWidth = 6.5f)
                                drawLine(Color(0xFFE53935), Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), strokeWidth = 3.2f)
                                drawLine(Color(0xFFE53935), Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), strokeWidth = 3.2f)
                                drawLine(Color(0xFFE53935), Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), strokeWidth = 3.2f)
                                drawLine(Color(0xFFE53935), Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), strokeWidth = 3.2f)
                                drawCircle(Color.White, radius = 7f, center = center)
                                drawCircle(Color(0xFFE53935), radius = 4.2f, center = center)

                                // Animated Live Magnifying Loupe at top-center
                                if (loupeAnimScale > 0.05f) {
                                    val loupeR = 54.dp.toPx() * loupeAnimScale
                                    val loupeX = size.width / 2f
                                    val loupeY = (loupeR + 10.dp.toPx()).coerceAtMost(center.y - radius - 12f)
                                    if (loupeY > loupeR) {
                                        val mag = loupeZoomLevel
                                        drawIntoCanvas { canvas ->
                                            val native = canvas.nativeCanvas
                                            val clipPath = android.graphics.Path().apply {
                                                addCircle(loupeX, loupeY, loupeR, android.graphics.Path.Direction.CW)
                                            }
                                            native.save()
                                            native.clipPath(clipPath)
                                            native.drawColor(android.graphics.Color.WHITE)

                                            native.translate(loupeX, loupeY)
                                            native.scale(mag, mag)
                                            native.translate(-center.x, -center.y)

                                            val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                                                colorFilter = colorMatrixFilter
                                            }
                                            val hdTile = state.viewportTile
                                            val crossNx = toNormX(center.x)
                                            val crossNy = toNormY(center.y)
                                            val useHd = hdTile != null && scale >= 2.2f && !hdTile.bitmap.isRecycled &&
                                                crossNx in hdTile.normLeft..hdTile.normRight &&
                                                crossNy in hdTile.normTop..hdTile.normBottom
                                            if (useHd && hdTile != null) {
                                                val tileDst = RectF(
                                                    toScreenX(hdTile.normLeft),
                                                    toScreenY(hdTile.normTop),
                                                    toScreenX(hdTile.normRight),
                                                    toScreenY(hdTile.normBottom)
                                                )
                                                native.drawBitmap(
                                                    hdTile.bitmap,
                                                    Rect(0, 0, hdTile.bitmap.width, hdTile.bitmap.height),
                                                    tileDst,
                                                    bitmapPaint
                                                )
                                            } else {
                                                val halfNormW = (loupeR / (fitW * scale * mag)).coerceAtLeast(0.01f)
                                                val halfNormH = (loupeR / (fitH * scale * mag)).coerceAtLeast(0.01f)
                                                val cL = (crossNx - halfNormW * 1.4f).coerceIn(0f, 0.99f)
                                                val cT = (crossNy - halfNormH * 1.4f).coerceIn(0f, 0.99f)
                                                val cR = (crossNx + halfNormW * 1.4f).coerceIn(cL + 0.005f, 1f)
                                                val cB = (crossNy + halfNormH * 1.4f).coerceIn(cT + 0.005f, 1f)
                                                val srcR = Rect(
                                                    (cL * bmp.width).toInt().coerceIn(0, bmp.width - 1),
                                                    (cT * bmp.height).toInt().coerceIn(0, bmp.height - 1),
                                                    (cR * bmp.width).roundToInt().coerceIn(1, bmp.width),
                                                    (cB * bmp.height).roundToInt().coerceIn(1, bmp.height)
                                                )
                                                val dstR = RectF(
                                                    toScreenX(cL),
                                                    toScreenY(cT),
                                                    toScreenX(cR),
                                                    toScreenY(cB)
                                                )
                                                native.drawBitmap(bmp, srcR, dstR, bitmapPaint)
                                            }

                                            val loupePolyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                color = android.graphics.Color.parseColor("#0F6B54")
                                                style = Paint.Style.STROKE
                                                strokeWidth = 2.2f
                                            }
                                            state.selectedPlots.forEach { plot ->
                                                val poly = plot.effectivePolygon() ?: return@forEach
                                                if (poly.size >= 3) {
                                                    val pPath = android.graphics.Path()
                                                    poly.forEachIndexed { idx, (nx, ny) ->
                                                        val px = toScreenX(nx)
                                                        val py = toScreenY(ny)
                                                        if (idx == 0) pPath.moveTo(px, py) else pPath.lineTo(px, py)
                                                    }
                                                    pPath.close()
                                                    loupePolyPaint.color = plot.color.toArgb()
                                                    native.drawPath(pPath, loupePolyPaint)
                                                }
                                            }
                                            if (draft.size >= 2) {
                                                val dPath = android.graphics.Path()
                                                draft.forEachIndexed { idx, (nx, ny) ->
                                                    val px = toScreenX(nx)
                                                    val py = toScreenY(ny)
                                                    if (idx == 0) dPath.moveTo(px, py) else dPath.lineTo(px, py)
                                                }
                                                loupePolyPaint.color = android.graphics.Color.parseColor("#0F6B54")
                                                native.drawPath(dPath, loupePolyPaint)
                                            }
                                            native.restore()
                                        }

                                        val lensCenter = Offset(loupeX, loupeY)
                                        drawCircle(
                                            color = Color(0xFF0F6B54).copy(alpha = 0.28f),
                                            radius = loupeR + 6.dp.toPx(),
                                            center = lensCenter,
                                            style = Stroke(width = 4.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color(0xFF0F6B54),
                                            radius = loupeR,
                                            center = lensCenter,
                                            style = Stroke(width = 3.5.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = loupeR + 2.dp.toPx(),
                                            center = lensCenter,
                                            style = Stroke(width = 1.8.dp.toPx())
                                        )
                                        val lArm = 16.dp.toPx() * loupeAnimScale
                                        val lGap = 4.dp.toPx() * loupeAnimScale
                                        drawLine(Color(0xFFE53935), Offset(loupeX - lArm, loupeY), Offset(loupeX - lGap, loupeY), strokeWidth = 2.4f)
                                        drawLine(Color(0xFFE53935), Offset(loupeX + lGap, loupeY), Offset(loupeX + lArm, loupeY), strokeWidth = 2.4f)
                                        drawLine(Color(0xFFE53935), Offset(loupeX, loupeY - lArm), Offset(loupeX, loupeY - lGap), strokeWidth = 2.4f)
                                        drawLine(Color(0xFFE53935), Offset(loupeX, loupeY + lGap), Offset(loupeX, loupeY + lArm), strokeWidth = 2.4f)
                                        drawCircle(Color(0xFFE53935), radius = 3.2f * loupeAnimScale, center = lensCenter)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dedicated Docked Controls BELOW the PDF Canvas (keeps the entire Mouza PDF clear!)
            if (state.displayBitmap != null) {
                val hasPolyDraft = state.activeTool == MouzaSelectionTool.POLYGON_DRAW && state.manualPolygonDraft.isNotEmpty()
                val hasEditPlots = state.activeTool == MouzaSelectionTool.EDIT_CORNERS && state.selectedPlots.isNotEmpty()
                val hasAnyNudgePoint = when (state.activeTool) {
                    MouzaSelectionTool.POLYGON_DRAW -> state.manualPolygonDraft.isNotEmpty()
                    MouzaSelectionTool.EDIT_CORNERS -> state.selectedPlots.any { !it.effectivePolygon().isNullOrEmpty() }
                    MouzaSelectionTool.TAPE_MEASURE -> state.tapeDraft.isNotEmpty()
                    else -> false
                }
                val effectiveTargetPoint = nudgeTargetIsPoint && hasAnyNudgePoint
                val activePointLbl = if (!effectiveTargetPoint) {
                    "ক্রসহেয়ার"
                } else {
                    when (state.activeTool) {
                        MouzaSelectionTool.POLYGON_DRAW -> {
                            val idx = state.selectedDraftIdx?.takeIf { it in state.manualPolygonDraft.indices }
                                ?: state.manualPolygonDraft.lastIndex
                            "বিন্দু #${idx + 1}"
                        }
                        MouzaSelectionTool.EDIT_CORNERS -> {
                            val sv = state.selectedVertex
                            if (sv != null) "কোণা #${sv.second + 1}" else "কোণা #1"
                        }
                        MouzaSelectionTool.TAPE_MEASURE -> {
                            val idx = state.selectedTapeIdx?.takeIf { it in state.tapeDraft.indices }
                                ?: state.tapeDraft.lastIndex
                            "ফিতা #${idx + 1}"
                        }
                        else -> "ক্রসহেয়ার"
                    }
                }

                // Active Khatian Target Banner (when user tapped a Dag from Khatian sheet)
                AnimatedVisibility(
                    visible = state.activeSearchTarget != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    state.activeSearchTarget?.let { (kh, dag) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "খুঁজছেন: খতিয়ান ${kh.ifBlank { "-" }} • দাগ $dag (নকশায় সিলেক্ট করুন)",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearActiveSearchTarget() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear Search Target", modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                            }
                        }
                    }
                }

                // Exact normalized coordinates under screen center crosshair
                fun centerNormX(): Float = 0.5f - (offsetX / (trackedFitW.coerceAtLeast(1f) * scale))
                fun centerNormY(): Float = 0.5f - (offsetY / (trackedFitH.coerceAtLeast(1f) * scale))

                // Docked Contextual Action Strip below map
                val showActionRow = (pointerModeEnabled && state.activeTool != MouzaSelectionTool.PAN_ONLY) || hasPolyDraft || hasEditPlots
                AnimatedVisibility(
                    visible = showActionRow,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pointerModeEnabled && state.activeTool != MouzaSelectionTool.PAN_ONLY) {
                            val ptrBtnLabel = when (state.activeTool) {
                                MouzaSelectionTool.POLYGON_DRAW -> "কোণা বসান"
                                MouzaSelectionTool.AUTO_TAP -> "দাগ সিলেক্ট"
                                MouzaSelectionTool.EDIT_CORNERS -> "কোণা এখানে আনুন"
                                MouzaSelectionTool.TAPE_MEASURE -> "মাপ বিন্দু"
                                MouzaSelectionTool.PAN_ONLY -> ""
                            }
                            Button(
                                onClick = {
                                    val nx = centerNormX()
                                    val ny = centerNormY()
                                    if (nx in 0f..1f && ny in 0f..1f) {
                                        viewModel.onPointerPlace(nx, ny)
                                    } else {
                                        viewModel.showStatusHint("পয়েন্টারটি ম্যাপের ভেতরে রাখুন")
                                    }
                                },
                                modifier = Modifier.testTag("mouza_pointer_select_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(ptrBtnLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }

                        if (hasEditPlots) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteSelectedOrNearestVertex(
                                        centerNormX().coerceIn(0f, 1f),
                                        centerNormY().coerceIn(0f, 1f)
                                    )
                                },
                                modifier = Modifier.testTag("mouza_delete_vertex_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text("কোণা ডিলিট", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }

                        if (hasPolyDraft) {
                            OutlinedButton(
                                onClick = { viewModel.deleteSelectedOrLastDraftPoint() },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text("বিন্দু ডিলিট (${state.manualPolygonDraft.size})", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearManualDraft() },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                            ) {
                                Text("মুছুন", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.finishManualPolygon() },
                                enabled = state.manualPolygonDraft.size >= 3,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("সম্পন্ন", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }
                    }
                }

                // Docked Fine-Tune Nudge D-Pad Controller (placed cleanly BELOW the map so map stays 100% unobstructed)
                AnimatedVisibility(
                    visible = showNudgePad,
                    enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    PrecisionNudgePad(
                        targetIsPoint = effectiveTargetPoint,
                        hasAnyPoint = hasAnyNudgePoint,
                        activePointLabel = activePointLbl,
                        stepLevel = nudgeStepLevel,
                        onStepLevelChange = { nudgeStepLevel = it },
                        onToggleTargetMode = { nudgeTargetIsPoint = it },
                        onCyclePoint = { delta ->
                            nudgeTargetIsPoint = true
                            viewModel.cycleActivePoint(delta)
                        },
                        onNudge = { dxPx, dyPx ->
                            if (effectiveTargetPoint) {
                                viewModel.nudgeActivePoint(
                                    deltaNormX = dxPx / (trackedFitW.coerceAtLeast(1f) * scale),
                                    deltaNormY = dyPx / (trackedFitH.coerceAtLeast(1f) * scale)
                                )
                            } else {
                                val maxOffX = max(trackedBoxW * scale * 0.75f, trackedBoxW * 0.5f)
                                val maxOffY = max(trackedBoxH * scale * 0.75f, trackedBoxH * 0.5f)
                                offsetX = (offsetX - dxPx).coerceIn(-maxOffX, maxOffX)
                                offsetY = (offsetY - dyPx).coerceIn(-maxOffY, maxOffY)
                            }
                        },
                        onClose = { showNudgePad = false }
                    )
                }
            }

            // Bottom Sheet / Tape Bar (matches video & UI.html fbar/tbar)
            if (state.displayBitmap != null) {
                if (state.activeTool == MouzaSelectionTool.TAPE_MEASURE) {
                    var totalTapeFt = 0.0
                    for (i in 1 until state.tapeDraft.size) {
                        totalTapeFt += MouzaPdfEngine.computeDistanceFeet(
                            state.tapeDraft[i - 1], state.tapeDraft[i],
                            state.pdfPageWidthPts, state.pdfPageHeightPts,
                            state.selectedScale, state.calibrationMultiplier
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .neuSurface(cornerRadius = 22.dp, elevation = 8.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.tapeDraft.size >= 2) {
                                "${LandCalculators.formatFeetInches(totalTapeFt)} • ${String.format(Locale.US, "%.1f", totalTapeFt * 12.0 / 7.92)} লিংক • ${String.format(Locale.US, "%.1f", totalTapeFt / 3.0)} গজ • ${String.format(Locale.US, "%.1f", totalTapeFt * 0.3048)} মি."
                            } else "ফিতা মাপ (নকশায় ট্যাপ করে বিন্দু বসান)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFC47A00),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.undoTapePoint() },
                                enabled = state.tapeDraft.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("পিছনে")
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearTapeDraft() },
                                enabled = state.tapeDraft.isNotEmpty() || state.completedTapes.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("মুছুন", color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = { viewModel.finishTapeMeasure() },
                                enabled = state.tapeDraft.size >= 2,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("সম্পন্ন")
                            }
                        }
                    }
                } else {
                    val totalSqFt = viewModel.totalSelectedSqFeet()
                    val totalArea = LandCalculators.fromSqFeet(totalSqFt)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .neuSurface(cornerRadius = 22.dp, elevation = 8.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state.selectedPlots.isEmpty()) {
                                        "যেকোনো দাগের ভেতরে ট্যাপ করুন"
                                    } else {
                                        String.format(Locale.US, "%.2f শতাংশ", totalArea.decimal)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (state.selectedPlots.isEmpty()) {
                                        "দাগ নাম্বারসহ সম্পূর্ণ প্লট অটো সিলেক্ট হবে"
                                    } else {
                                        String.format(
                                            Locale.US,
                                            "কাঠা: %.2f • বর্গফুট: %.0f • দাগ: %dটি",
                                            totalArea.katha, totalArea.sqFeet, state.selectedPlots.size
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showSaveProjectDialog = true },
                                    modifier = Modifier.testTag("mouza_save_data_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ডাটা সেভ", maxLines = 1)
                                }
                                Button(
                                    onClick = { showReportSheet = true },
                                    modifier = Modifier.testTag("mouza_report_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text("PNG ও সেভ", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Swipeable Toast Banner (never blocks top bar or map canvas!)
        BottomSwipeToastBanner(
            message = state.statusMessage,
            onDismiss = { viewModel.clearStatusMessage() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 94.dp)
        )
    }

    // Report & Export PNG Bottom Sheet (matching video + UI.html)
    if (showReportSheet) {
        val totalSqFt = viewModel.totalSelectedSqFeet()
        val totalArea = LandCalculators.fromSqFeet(totalSqFt)
        ModalBottomSheet(onDismissRequest = { showReportSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "চিহ্নিত দাগসমূহ (${state.selectedPlots.size}টি)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%.2f শতাংশ • %.2f কাঠা • %.0f বর্গফুট • %.3f বিঘা • %.4f একর",
                        totalArea.decimal, totalArea.katha, totalArea.sqFeet, totalArea.bigha, totalArea.acre
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = state.khatianTitle,
                    onValueChange = { viewModel.updateKhatianTitle(it) },
                    label = { Text("খতিয়ান নং / মালিকের নাম (PNG ফুটারের জন্য)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (state.selectedPlots.isEmpty()) {
                    Text(
                        text = "এখনো কোনো দাগ সিলেক্ট করা হয়নি।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.selectedPlots, key = { it.id }) { plot ->
                            val pArea = LandCalculators.fromSqFeet(viewModel.plotAreaSqFeet(plot))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(plot.color.copy(alpha = 0.10f))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(plot.color)
                                        )
                                        Text(
                                            text = buildString {
                                                append("দাগ #${plot.plotIndex}")
                                                if (plot.dagNumber.isNotBlank()) append(" (${plot.dagNumber})")
                                                if (plot.khatianNumber.isNotBlank()) append(" • খ: ${plot.khatianNumber}")
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                viewModel.requestFocusPlot(plot)
                                                showReportSheet = false
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.CenterFocusStrong, contentDescription = "Zoom to Plot", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { showCalibrateDialog = plot },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.Tune, contentDescription = "Calibrate Scale", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { editingPlot = plot },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Dag/Khatian", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.removePlot(plot.id) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.3f শতক • %.3f কাঠা • %.0f বর্গফুট • %.3f বিঘা • %.4f একর",
                                        pArea.decimal, pArea.katha, pArea.sqFeet, pArea.bigha, pArea.acre
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "২ভাবে সেভ করুন (PNG ছবি অথবা পরে রিস্টোরের জন্য PDF/ম্যাপ ডাটা):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportSelectedOnlyPng(selectedOnly = true)
                            showReportSheet = false
                        },
                        enabled = state.selectedPlots.isNotEmpty() && !state.isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mouza_export_png_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PNG: নির্বাচিত", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.exportSelectedOnlyPng(selectedOnly = false)
                            showReportSheet = false
                        },
                        enabled = !state.isExporting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("পুরো PNG", maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = {
                        showReportSheet = false
                        showSaveProjectDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("mouza_sheet_save_project_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ম্যাপ/PDF ডাটা সেভ করুন (পরে রিস্টোরের জন্য)")
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    // Save Project Data Dialog (for restoring later without re-measuring)
    if (showSaveProjectDialog) {
        var projTitle by remember(state.fileName, state.khatianTitle) {
            mutableStateOf(
                state.khatianTitle.ifBlank {
                    state.fileName.substringBeforeLast('.').ifBlank { "মৌজা জরিপ" }
                }
            )
        }
        AlertDialog(
            onDismissRequest = { showSaveProjectDialog = false },
            title = { Text("ম্যাপ / PDF ডাটা সংরক্ষণ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "এই কাজটি অ্যাপের ভেতর স্থায়ীভাবে সংরক্ষিত হবে—পরে যেকোনো সময় ক্লিক করে আগের সব দাগ, পয়েন্ট ও ফিতা মাপ সরাসরি রিস্টোর করতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = projTitle,
                        onValueChange = { projTitle = it },
                        label = { Text("প্রজেক্ট / খতিয়ান নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveMouzaProject(projTitle)
                        showSaveProjectDialog = false
                    }
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProjectDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Saved Mouza Projects Sheet (tap any item to restore full PDF + plots/draft/tapes)
    if (showSavedProjectsSheet) {
        ModalBottomSheet(onDismissRequest = { showSavedProjectsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "সংরক্ষিত মৌজা PDF/ম্যাপ কাজ (${state.savedProjects.size}টি)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.savedProjects.isEmpty()) {
                    Text(
                        text = "এখনো কোনো মৌজা প্রজেক্ট ডাটা সেভ করা হয়নি। নকশায় কাজ করে 'ডাটা সেভ' চাপলে এখানে দেখাবে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.savedProjects, key = { it.id }) { proj ->
                            val dateStr = remember(proj.timestampMillis) {
                                SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(proj.timestampMillis))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                                    .clickable {
                                        showSavedProjectsSheet = false
                                        viewModel.restoreMouzaProject(proj)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proj.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${proj.docFileName} • ${proj.savedPlots.size} দাগ • $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f শতক • %.2f কাঠা (রিস্টোর করতে ট্যাপ করুন)", proj.decimalArea, proj.kathaArea),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSavedProject(proj.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Project", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    // Image Clarity / Filter Enhancement Sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "নকশা ফিল্টার ও স্পষ্টতা",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ঝাপসা নকশার লেখা ও দাগ পরিষ্কার করুন (অটো ফিলে কোনো সমস্যা হবে না)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val presets = MouzaFilterPreset.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { preset ->
                                Box(modifier = Modifier.weight(1f)) {
                                    NeuPillChip(
                                        text = preset.labelBn,
                                        selected = state.filterPreset == preset,
                                        onClick = { viewModel.setFilterPreset(preset) }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("উজ্জ্বলতা (${String.format(Locale.US, "%.0f%%", state.brightness * 100)})", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = state.brightness,
                    onValueChange = { viewModel.setBrightnessContrast(it, state.contrast) },
                    valueRange = 0.5f..1.8f
                )

                Text("কন্ট্রাস্ট (${String.format(Locale.US, "%.0f%%", state.contrast * 100)})", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = state.contrast,
                    onValueChange = { viewModel.setBrightnessContrast(state.brightness, it) },
                    valueRange = 0.5f..2.5f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setFilterPreset(MouzaFilterPreset.NORMAL)
                            viewModel.setBrightnessContrast(1f, 1f)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("রিসেট")
                    }
                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("প্রয়োগ সম্পন্ন")
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    // Separate Khatian Upload & Dag-Number Lookup Sheet
    if (showKhatianSheet) {
        KhatianLookupBottomSheet(
            khatianDocName = state.khatianDocName,
            khatianPreviewBitmap = state.khatianPreviewBitmap,
            khatianPageCount = state.khatianPageCount,
            khatianCurrentPage = state.khatianCurrentPage,
            isLoadingKhatian = state.isLoadingKhatian,
            khatianRecords = state.khatianRecords,
            taggedPlots = state.selectedPlots,
            onUploadKhatianClick = { khatianPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
            onSelectKhatianPage = { viewModel.selectKhatianPage(it) },
            onAddKhatianRecord = { kh, dags, owner, area ->
                viewModel.addKhatianRecord(kh, dags, owner, area)
            },
            onDeleteKhatianRecord = { viewModel.deleteKhatianRecord(it) },
            onSelectDagForMap = { kh, dag -> viewModel.findOrTargetDagFromKhatian(kh, dag) },
            onFocusExistingPlot = { plot ->
                viewModel.requestFocusPlot(plot)
            },
            onDismiss = { showKhatianSheet = false }
        )
    }

    // Edit Dag & Khatian Tag Dialog
    editingPlot?.let { plot ->
        var dag by remember(plot.id) { mutableStateOf(plot.dagNumber) }
        var khat by remember(plot.id) { mutableStateOf(plot.khatianNumber) }
        AlertDialog(
            onDismissRequest = { editingPlot = null },
            title = { Text("দাগ #${plot.plotIndex} তথ্য যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dag,
                        onValueChange = { dag = it },
                        label = { Text("দাগ নম্বর (যেমন: ৪৫২)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = khat,
                        onValueChange = { khat = it },
                        label = { Text("খতিয়ান নম্বর (যেমন: ১২৮)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updatePlotTags(plot.id, dag.trim(), khat.trim())
                    editingPlot = null
                }) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlot = null }) { Text("বাতিল") }
            }
        )
    }

    // Known-Plot Scale Calibration Dialog
    showCalibrateDialog?.let { plot ->
        var knownDec by remember(plot.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCalibrateDialog = null },
            title = { Text("দাগ #${plot.plotIndex} দিয়ে স্কেল ক্যালিব্রেট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "আপনার খতিয়ান অনুযায়ী এই দাগটির আসল পরিমাণ (শতাংশে) লিখলে পুরো মৌজা ম্যাপের সব দাগের মাপ ১০০% সঠিক হয়ে যাবে।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = knownDec,
                        onValueChange = { knownDec = it },
                        label = { Text("আসল পরিমাণ (শতাংশ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    knownDec.toDoubleOrNull()?.let { viewModel.calibrateWithKnownPlot(plot.id, it) }
                    showCalibrateDialog = null
                }) {
                    Text("ক্যালিব্রেট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrateDialog = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun CanvasFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xCC16222B))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

private fun buildFilterColorMatrix(
    preset: MouzaFilterPreset,
    brightness: Float,
    contrast: Float
): ColorMatrixColorFilter? {
    if (preset == MouzaFilterPreset.NORMAL && brightness == 1f && contrast == 1f) return null

    val cm = ColorMatrix()
    when (preset) {
        MouzaFilterPreset.NORMAL -> Unit
        MouzaFilterPreset.DOC_SCAN -> {
            val gray = ColorMatrix().apply { setSaturation(0f) }
            val boost = contrastMatrix(1.35f, 1.08f)
            cm.postConcat(gray)
            cm.postConcat(boost)
        }
        MouzaFilterPreset.HIGH_CONTRAST -> {
            cm.postConcat(contrastMatrix(1.8f, 1.0f))
        }
        MouzaFilterPreset.BW -> {
            cm.postConcat(ColorMatrix().apply { setSaturation(0f) })
            cm.postConcat(contrastMatrix(2.1f, 1.0f))
        }
        MouzaFilterPreset.BRIGHT -> {
            cm.postConcat(contrastMatrix(1.0f, 1.3f))
        }
        MouzaFilterPreset.INVERT -> {
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }
    if (brightness != 1f || contrast != 1f) {
        cm.postConcat(contrastMatrix(contrast, brightness))
    }
    return ColorMatrixColorFilter(cm)
}

private fun contrastMatrix(contrast: Float, brightness: Float): ColorMatrix {
    val t = (1f - contrast) * 128f + (brightness - 1f) * 128f
    return ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
}
