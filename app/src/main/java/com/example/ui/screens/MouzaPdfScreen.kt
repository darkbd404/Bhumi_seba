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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.MouzaPdfViewModel
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
    viewModel: MouzaPdfViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openDocumentUri(it) }
    }

    var scale by remember(state.fileName, state.currentPage) { mutableFloatStateOf(1f) }
    var offsetX by remember(state.fileName, state.currentPage) { mutableFloatStateOf(0f) }
    var offsetY by remember(state.fileName, state.currentPage) { mutableFloatStateOf(0f) }

    var showScaleMenu by remember { mutableStateOf(false) }
    var showGapMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var pointerModeEnabled by remember { mutableStateOf(true) }
    var editingPlot by remember { mutableStateOf<MouzaPlot?>(null) }
    var showCalibrateDialog by remember { mutableStateOf<MouzaPlot?>(null) }

    BackHandler(enabled = showReportSheet || showFilterSheet || editingPlot != null || showCalibrateDialog != null) {
        when {
            showCalibrateDialog != null -> showCalibrateDialog = null
            editingPlot != null -> editingPlot = null
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
            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("mouza_back_button")
                        .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.fileName.ifBlank { "মৌজা ম্যাপ (PDF / ছবি)" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.hdReady) "Ultra-HD Ready • ${state.selectedScale.ratioLabel} • 1000x জুম"
                        else if (state.displayBitmap != null) "HD প্রস্তুত হচ্ছে..."
                        else "স্টোরেজ থেকে PDF বা ছবি নিন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Undo / Redo / Clear quick actions when document open
                if (state.displayBitmap != null) {
                    IconButton(
                        onClick = { viewModel.undoLastPlot() },
                        enabled = state.selectedPlots.isNotEmpty() || state.tapeDraft.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("mouza_undo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redoLastPlot() },
                        enabled = state.canRedo,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                    }
                    IconButton(
                        onClick = { viewModel.clearAllPlots() },
                        enabled = state.selectedPlots.isNotEmpty() || state.tapeDraft.isNotEmpty() || state.completedTapes.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("mouza_clear_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Button(
                    onClick = { docPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.testTag("pick_pdf_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.displayBitmap == null) "ফাইল নিন" else "বদলান", maxLines = 1)
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

            // Tool & Scale Strip (matches video & UI.html)
            if (state.displayBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                modifier = Modifier.size(34.dp)
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
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Page")
                            }
                        }
                    }
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

                    fun toScreenX(nx: Float): Float = ((imgLeft + nx * fitW) - cx) * scale + cx + offsetX
                    fun toScreenY(ny: Float): Float = ((imgTop + ny * fitH) - cy) * scale + cy + offsetY
                    fun toNormX(sx: Float): Float = (((sx - cx - offsetX) / scale + cx) - imgLeft) / fitW
                    fun toNormY(sy: Float): Float = (((sy - cy - offsetY) / scale + cy) - imgTop) / fitH

                    // Focus on a plot requested from the Report sheet
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
                            scale = targetScale
                            offsetX = -((imgLeft + targetCenterNx * fitW) - cx) * targetScale
                            offsetY = -((imgTop + targetCenterNy * fitH) - cy) * targetScale
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
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            val targetScale = 4f
                                            val ratio = targetScale / scale
                                            offsetX = (offsetX - (tap.x - cx)) * ratio + (tap.x - cx)
                                            offsetY = (offsetY - (tap.y - cy)) * ratio + (tap.y - cy)
                                            scale = targetScale
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
                                    drawPath(path, color = plot.color.copy(alpha = 0.30f))
                                    drawPath(path, color = plot.color, style = Stroke(width = 4.5f))

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

                            // 6. Enlarged Precision Crosshair Pointer + Live 3.2x Zoom Loupe (when enabled)
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

                                // Live 3.2x Magnifying Loupe at top-center showing the exact zoomed target under crosshair
                                val loupeR = 54.dp.toPx()
                                val loupeX = size.width / 2f
                                val loupeY = (loupeR + 12.dp.toPx()).coerceAtMost(center.y - radius - 14f)
                                if (loupeY > loupeR) {
                                    val mag = 3.2f
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

                                        // Also render polygon & draft lines inside the 3.2x magnifying loupe
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

                                    // Magnifier lens ring + inner fine crosshair
                                    val lensCenter = Offset(loupeX, loupeY)
                                    drawCircle(
                                        color = Color(0xFF0F6B54),
                                        radius = loupeR,
                                        center = lensCenter,
                                        style = Stroke(width = 4.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = loupeR + 2.dp.toPx(),
                                        center = lensCenter,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    val lArm = 16.dp.toPx()
                                    val lGap = 4.dp.toPx()
                                    drawLine(Color(0xFFE53935), Offset(loupeX - lArm, loupeY), Offset(loupeX - lGap, loupeY), strokeWidth = 2.4f)
                                    drawLine(Color(0xFFE53935), Offset(loupeX + lGap, loupeY), Offset(loupeX + lArm, loupeY), strokeWidth = 2.4f)
                                    drawLine(Color(0xFFE53935), Offset(loupeX, loupeY - lArm), Offset(loupeX, loupeY - lGap), strokeWidth = 2.4f)
                                    drawLine(Color(0xFFE53935), Offset(loupeX, loupeY + lGap), Offset(loupeX, loupeY + lArm), strokeWidth = 2.4f)
                                    drawCircle(Color(0xFFE53935), radius = 3.2f, center = lensCenter)
                                }
                            }
                        }

                        // Zoom Percentage Indicator Badge (Top Left)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xBB16222B))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fx / 1000x", scale),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Right-side Floating FAB Stack (Zoom +, Zoom -, Fit, Pointer, Filter)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CanvasFab(
                                icon = Icons.Rounded.Add,
                                desc = "Zoom In",
                                onClick = { scale = (scale * 1.6f).coerceIn(1f, 1000f) }
                            )
                            CanvasFab(
                                icon = Icons.Rounded.Remove,
                                desc = "Zoom Out",
                                onClick = {
                                    val next = (scale / 1.6f).coerceIn(1f, 1000f)
                                    scale = next
                                    if (next <= 1.02f) {
                                        scale = 1f; offsetX = 0f; offsetY = 0f
                                    }
                                }
                            )
                            CanvasFab(
                                icon = Icons.Rounded.ZoomOutMap,
                                desc = "Fit Screen",
                                onClick = { scale = 1f; offsetX = 0f; offsetY = 0f }
                            )
                            CanvasFab(
                                icon = Icons.Rounded.GpsFixed,
                                desc = "Toggle Precision Pointer",
                                tint = if (pointerModeEnabled) Color(0xFFFFD54F) else Color.White,
                                onClick = { pointerModeEnabled = !pointerModeEnabled }
                            )
                            CanvasFab(
                                icon = Icons.Rounded.FilterBAndW,
                                desc = "Enhance Map Filters",
                                tint = if (state.filterPreset != MouzaFilterPreset.NORMAL) Color(0xFFFFD54F) else Color.White,
                                onClick = { showFilterSheet = true }
                            )
                        }

                        // Precision Crosshair Action Bar (includes Delete Point when editing/drawing)
                        val hasPolyDraft = state.activeTool == MouzaSelectionTool.POLYGON_DRAW && state.manualPolygonDraft.isNotEmpty()
                        val hasEditPlots = state.activeTool == MouzaSelectionTool.EDIT_CORNERS && state.selectedPlots.isNotEmpty()
                        androidx.compose.animation.AnimatedVisibility(
                            visible = pointerModeEnabled && state.activeTool != MouzaSelectionTool.PAN_ONLY,
                            enter = fadeIn() + scaleIn(initialScale = 0.9f),
                            exit = fadeOut() + scaleOut(targetScale = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (hasPolyDraft) 70.dp else 10.dp)
                        ) {
                            val ptrBtnLabel = when (state.activeTool) {
                                MouzaSelectionTool.POLYGON_DRAW -> "পয়েন্টারে কোণা বসান"
                                MouzaSelectionTool.AUTO_TAP -> "পয়েন্টারে দাগ সিলেক্ট"
                                MouzaSelectionTool.EDIT_CORNERS -> "কোণা এখানে আনুন"
                                MouzaSelectionTool.TAPE_MEASURE -> "পয়েন্টারে মাপ বিন্দু"
                                MouzaSelectionTool.PAN_ONLY -> ""
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val nx = toNormX(cx)
                                        val ny = toNormY(cy)
                                        if (nx in 0f..1f && ny in 0f..1f) {
                                            viewModel.onPointerPlace(nx, ny)
                                        } else {
                                            viewModel.showStatusHint("পয়েন্টারটি ম্যাপের ভেতরে রাখুন")
                                        }
                                    },
                                    modifier = Modifier.testTag("mouza_pointer_select_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xEE0F6B54)),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(ptrBtnLabel, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                if (hasEditPlots) {
                                    Button(
                                        onClick = {
                                            viewModel.deleteSelectedOrNearestVertex(toNormX(cx), toNormY(cy))
                                        },
                                        modifier = Modifier.testTag("mouza_delete_vertex_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xEEC62828)),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("বিন্দু ডিলিট", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // Manual Polygon Draft Action Pill (Edit / Delete selected point / Undo / Finish)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = hasPolyDraft,
                            enter = fadeIn() + scaleIn(initialScale = 0.92f),
                            exit = fadeOut() + scaleOut(targetScale = 0.92f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xEE16222B))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "বিন্দু: ${state.manualPolygonDraft.size}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(
                                    onClick = { viewModel.deleteSelectedOrLastDraftPoint() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("বিন্দু ডিলিট", color = Color(0xFFFF8A80))
                                }
                                TextButton(
                                    onClick = { viewModel.clearManualDraft() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("সব মুছুন", color = Color(0xFFFFD54F))
                                }
                                Button(
                                    onClick = { viewModel.finishManualPolygon() },
                                    enabled = state.manualPolygonDraft.size >= 3,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("সম্পন্ন")
                                }
                            }
                        }
                    }
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

                            Button(
                                onClick = { showReportSheet = true },
                                modifier = Modifier.testTag("mouza_report_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text("বিস্তারিত ও সেভ", maxLines = 1)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("সেভ: নির্বাচিত দাগ", maxLines = 1)
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
                        Text("পুরো নকশা সেভ", maxLines = 1)
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
