package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.LandCalculators
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.MouzaSelectionTool
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.MouzaPdfViewModel
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun MouzaPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: MouzaPdfViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openPdfUri(it) }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    var scale by remember(state.fileName, state.currentPage) { mutableFloatStateOf(1f) }
    var offsetX by remember(state.fileName, state.currentPage) { mutableFloatStateOf(0f) }
    var offsetY by remember(state.fileName, state.currentPage) { mutableFloatStateOf(0f) }

    var showScaleMenu by remember { mutableStateOf(false) }
    var showGapMenu by remember { mutableStateOf(false) }
    var isSummaryExpanded by remember { mutableStateOf(false) }
    var editingPlot by remember { mutableStateOf<MouzaPlot?>(null) }
    var showCalibrateDialog by remember { mutableStateOf<MouzaPlot?>(null) }

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
                        text = state.fileName.ifBlank { "মৌজা ম্যাপ PDF ভিউয়ার" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.hdReady) "Ultra-HD Ready • ${state.selectedScale.ratioLabel}"
                        else if (state.displayBitmap != null) "HD প্রস্তুত হচ্ছে..."
                        else "ফোন স্টোরেজ থেকে মৌজা PDF নিন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.testTag("pick_pdf_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.displayBitmap == null) "PDF নিন" else "বদলান")
                }
            }

            if (state.isLoadingPdf || state.isRenderingHd || state.isIndexingPlots) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Tool & Scale Filter Strip
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
                        text = "অটো দাগ সিলেক্ট",
                        selected = state.activeTool == MouzaSelectionTool.AUTO_TAP,
                        icon = Icons.Rounded.TouchApp,
                        testTag = "tool_auto_tap",
                        onClick = { viewModel.setTool(MouzaSelectionTool.AUTO_TAP) }
                    )
                    NeuPillChip(
                        text = "পলিগন",
                        selected = state.activeTool == MouzaSelectionTool.POLYGON_DRAW,
                        icon = Icons.Rounded.Draw,
                        testTag = "tool_manual_polygon",
                        onClick = { viewModel.setTool(MouzaSelectionTool.POLYGON_DRAW) }
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
                                text = { Text("সাধারণ (Exact — স্পষ্ট রেখা)") },
                                onClick = { viewModel.setGapSealLevel(0); showGapMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("ব্যালেন্সড (সুপারিশকৃত)") },
                                onClick = { viewModel.setGapSealLevel(1); showGapMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("শক্তিশালী (ভাঙা দাগ-সীমানার জন্য)") },
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

            // Main Ultra-HD Zoom/Pan Canvas Area
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
                            text = "মৌজা ম্যাপ PDF সিলেক্ট করুন",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF16222B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "১. Ultra-HD কোয়ালিটিতে ল্যাগ ছাড়াই ১৬x জুম করুন\n২. দাগের ভেতরে ট্যাপ করলেই দাগ নাম্বারসহ সম্পূর্ণ প্লট সিলেক্ট হবে\n৩. একাধিক দাগ সিলেক্ট করে শুধু সিলেক্টেড অংশ PNG সেভ করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF536574)
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.testTag("empty_pick_pdf_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ফোন স্টোরেজ থেকে PDF বাছুন")
                        }
                    }
                } else {
                    val boxW = constraints.maxWidth.toFloat()
                    val boxH = constraints.maxHeight.toFloat()
                    val fitScale = min(boxW / bmp.width.toFloat(), boxH / bmp.height.toFloat())
                    val fitW = bmp.width * fitScale
                    val fitH = bmp.height * fitScale
                    val imgLeft = (boxW - fitW) / 2f
                    val imgTop = (boxH - fitH) / 2f

                    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                    val overlayBitmap = remember(state.overlayMaskBitmap) {
                        state.overlayMaskBitmap?.asImageBitmap()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bmp, state.activeTool) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 16f)
                                    val maxOffX = (boxW * (newScale - 1f)) * 0.65f + boxW * 0.25f
                                    val maxOffY = (boxH * (newScale - 1f)) * 0.65f + boxH * 0.25f
                                    scale = newScale
                                    if (newScale <= 1.01f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        offsetX = (offsetX + pan.x).coerceIn(-maxOffX, maxOffX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxOffY, maxOffY)
                                    }
                                }
                            }
                            .pointerInput(bmp, state.activeTool, fitW, fitH, imgLeft, imgTop) {
                                detectTapGestures(
                                    onDoubleTap = { tap ->
                                        if (scale > 2.2f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            val targetScale = 3.5f
                                            val cx = boxW / 2f
                                            val cy = boxH / 2f
                                            offsetX = (cx - tap.x) * (targetScale - 1f)
                                            offsetY = (cy - tap.y) * (targetScale - 1f)
                                            scale = targetScale
                                        }
                                    },
                                    onTap = { tap ->
                                        val cx = boxW / 2f
                                        val cy = boxH / 2f
                                        val unscaledX = (tap.x - cx - offsetX) / scale + cx
                                        val unscaledY = (tap.y - cy - offsetY) / scale + cy
                                        val normX = (unscaledX - imgLeft) / fitW
                                        val normY = (unscaledY - imgTop) / fitH
                                        if (normX in 0f..1f && normY in 0f..1f) {
                                            viewModel.onMapTap(normX, normY)
                                        }
                                    }
                                )
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = offsetX
                                    translationY = offsetY
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin.Center
                                }
                        ) {
                            val dstOffset = IntOffset(imgLeft.roundToInt(), imgTop.roundToInt())
                            val dstSize = IntSize(fitW.roundToInt(), fitH.roundToInt())

                            // Draw crisp Ultra-HD Mouza Sheet
                            drawImage(
                                image = imageBitmap,
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )

                            // Draw Flood-Filled Auto-Selected Plots Overlay
                            if (overlayBitmap != null) {
                                drawImage(
                                    image = overlayBitmap,
                                    dstOffset = dstOffset,
                                    dstSize = dstSize
                                )
                            }

                            // Draw Manual Polygon Plots
                            state.selectedPlots.forEach { plot ->
                                val poly = plot.manualPolygonNormalized
                                if (poly != null && poly.size >= 3) {
                                    val path = Path()
                                    poly.forEachIndexed { idx, (nx, ny) ->
                                        val px = imgLeft + nx * fitW
                                        val py = imgTop + ny * fitH
                                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    path.close()
                                    drawPath(path, color = plot.color.copy(alpha = 0.35f))
                                    drawPath(path, color = plot.color, style = Stroke(width = 3f / scale))
                                }
                            }

                            // Draw Active Draft Polygon
                            val draft = state.manualPolygonDraft
                            if (draft.isNotEmpty()) {
                                val path = Path()
                                draft.forEachIndexed { idx, (nx, ny) ->
                                    val px = imgLeft + nx * fitW
                                    val py = imgTop + ny * fitH
                                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    drawCircle(
                                        color = EmeraldPrimary,
                                        radius = 8f / scale,
                                        center = Offset(px, py)
                                    )
                                }
                                if (draft.size >= 2) {
                                    drawPath(
                                        path = path,
                                        color = EmeraldPrimary,
                                        style = Stroke(width = 3f / scale)
                                    )
                                }
                            }
                        }

                        // Floating Zoom & Reset Badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xBB16222B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1fx", scale),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (scale > 1.05f) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC0F6B54))
                                        .clickable {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.ZoomOutMap,
                                        contentDescription = "Reset Zoom",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Manual Polygon Draft Action Pill
                        if (state.activeTool == MouzaSelectionTool.POLYGON_DRAW && state.manualPolygonDraft.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xEE16222B))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "বিন্দু: ${state.manualPolygonDraft.size}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(onClick = { viewModel.undoManualPoint() }) {
                                    Text("পিছনে", color = Color(0xFFFFD54F))
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

            // Bottom Selected Plots & Export Sheet
            if (state.displayBitmap != null) {
                val sqFtPerPx = viewModel.sqFeetPerGridPixel()
                val totalSqFt = state.selectedPlots.sumOf { it.pixelCount * sqFtPerPx }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSummaryExpanded = !isSummaryExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.selectedPlots.isEmpty()) {
                                    "যেকোনো দাগের ভেতরে ট্যাপ করুন"
                                } else {
                                    "সিলেক্টেড: ${state.selectedPlots.size}টি দাগ  •  ${String.format(Locale.US, "%.2f", totalArea.decimal)} শতাংশ"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.selectedPlots.isEmpty()) {
                                    "দাগ নাম্বারসহ সম্পূর্ণ প্লট অটো সিলেক্ট হবে"
                                } else {
                                    String.format(
                                        Locale.US,
                                        "%.2f কাঠা  |  %.3f বিঘা  |  %.1f বর্গফুট",
                                        totalArea.katha, totalArea.bigha, totalArea.sqFeet
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.selectedPlots.isNotEmpty()) {
                            Icon(
                                imageVector = if (isSummaryExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                                contentDescription = "Toggle Details"
                            )
                        }
                    }

                    AnimatedVisibility(visible = isSummaryExpanded && state.selectedPlots.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.khatianTitle,
                                onValueChange = { viewModel.updateKhatianTitle(it) },
                                label = { Text("খতিয়ান নং / মালিকের নাম (PNG ফুটারের জন্য)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(state.selectedPlots, key = { it.id }) { plot ->
                                    val pArea = LandCalculators.fromSqFeet(plot.pixelCount * sqFtPerPx)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(plot.color.copy(alpha = 0.1f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
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
                                            Column {
                                                Text(
                                                    text = buildString {
                                                        append("#${plot.plotIndex}")
                                                        if (plot.dagNumber.isNotBlank()) append(" • দাগ ${plot.dagNumber}")
                                                        if (plot.khatianNumber.isNotBlank()) append(" (খ: ${plot.khatianNumber})")
                                                    },
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = String.format(
                                                        Locale.US,
                                                        "%.2f শতাংশ | %.2f কাঠা | %.0f বর্গফুট",
                                                        pArea.decimal, pArea.katha, pArea.sqFeet
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { showCalibrateDialog = plot },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Tune,
                                                    contentDescription = "Calibrate Scale",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { editingPlot = plot },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Edit,
                                                    contentDescription = "Edit Dag/Khatian",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.removePlot(plot.id) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.undoLastPlot() },
                            enabled = state.selectedPlots.isNotEmpty(),
                            modifier = Modifier.testTag("mouza_undo_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearAllPlots() },
                            enabled = state.selectedPlots.isNotEmpty(),
                            modifier = Modifier.testTag("mouza_clear_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { viewModel.exportSelectedOnlyPng() },
                            enabled = state.selectedPlots.isNotEmpty() && !state.isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mouza_export_png_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            if (state.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("সিলেক্টেড অংশ PNG সেভ", maxLines = 1)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 12.dp, end = 12.dp)
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
