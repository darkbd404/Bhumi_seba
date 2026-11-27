package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.GisCoordinateEngine
import com.example.engine.LandCalculators
import com.example.engine.SurveyMathProEngine
import com.example.model.PolygonVertex
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun PdfCadStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableIntStateOf(0) } // 0 = PDF & Image Lab, 1 = 2D CAD Vector
    var toast by remember { mutableStateOf<String?>(null) }

    // PDF & Image Studio State
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfPageIndex by remember { mutableIntStateOf(0) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mergeImageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var contrastVal by remember { mutableFloatStateOf(1.0f) }
    var rotationDeg by remember { mutableFloatStateOf(0f) }
    var blackAndWhite by remember { mutableStateOf(false) }

    // 2D CAD State
    var cadVertices by remember {
        mutableStateOf(
            listOf(
                PolygonVertex(0.0, 0.0),
                PolygonVertex(120.0, 0.0),
                PolygonVertex(130.0, 80.0),
                PolygonVertex(10.0, 85.0)
            )
        )
    }
    var undoStack by remember { mutableStateOf<List<List<PolygonVertex>>>(emptyList()) }
    var gridSnapFt by remember { mutableStateOf(true) }
    var cadScaleFtPerUnit by remember { mutableFloatStateOf(200f) }
    var cadTitle by remember { mutableStateOf("BD-CAD-Plot-01") }

    suspend fun renderPdfPage(uri: Uri, pageIdx: Int) = withContext(Dispatchers.IO) {
        runCatching {
            val fd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            fd?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    val safeIdx = pageIdx.coerceIn(0, (count - 1).coerceAtLeast(0))
                    val page = renderer.openPage(safeIdx)
                    val w = (page.width * 1.8f).roundToInt().coerceIn(800, 2000)
                    val h = (page.height * 1.8f).roundToInt().coerceIn(800, 2800)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    Triple(count, safeIdx, bmp)
                }
            }
        }.getOrNull()
    }

    fun applyImageAdjustments(src: Bitmap, contrast: Float, bw: Boolean, rotDeg: Float): Bitmap {
        val m = Matrix().apply { postRotate(rotDeg) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        val out = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        val cm = ColorMatrix().apply {
            if (bw) setSaturation(0f)
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val cMat = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            postConcat(cMat)
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        return out
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            scope.launch {
                val res = renderPdfPage(uri, 0)
                if (res != null) {
                    pdfPageCount = res.first
                    pdfPageIndex = res.second
                    renderedBitmap = res.third
                    toast = "PDF লোড হয়েছে (${res.first} পৃষ্ঠা)"
                } else {
                    toast = "PDF পড়তে সমস্যা হয়েছে"
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val loaded = uris.mapNotNull { u ->
                    runCatching {
                        context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                }
                withContext(Dispatchers.Main) {
                    if (loaded.isNotEmpty()) {
                        mergeImageBitmaps = loaded
                        renderedBitmap = loaded.first()
                        pdfPageCount = loaded.size
                        pdfPageIndex = 0
                        toast = "${loaded.size}টি ইমেজ লোড হয়েছে (PDF তৈরি/এডিটের জন্য প্রস্তুত)"
                    }
                }
            }
        }
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
                .widthIn(max = 1040.dp)
        ) {
            WorkstationTopBar(
                titleBn = "PDF ও 2D CAD স্টুডিও",
                subtitleEn = "PDF↔JPG • Deskew/B&W • Snap CAD • DXF Export",
                helpTitle = "PDF ও CAD ব্যবহার গাইড",
                helpSteps = listOf(
                    "PDF ট্যাবে যেকোনো PDF-এর নির্দিষ্ট পৃষ্ঠা JPG/PNG-তে রপ্তানি করতে পারবেন অথবা একাধিক ছবি মিলিয়ে একটি PDF তৈরি করতে পারবেন।",
                    "পুরোনো ম্যাপ পরিষ্কার করতে Contrast, ঘূর্ণন (Rotate/Deskew) ও B&W ফিল্টার প্রয়োগ করুন।",
                    "CAD ট্যাবে ক্যানভাসে ট্যাপ করে পয়েন্ট বসান, গ্রিড স্ন্যাপ ব্যবহার করুন এবং AutoCAD .DXF বা PNG/PDF সেভ করুন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "pdf_cad_back_button"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text("PDF ও ইমেজ ল্যাব") },
                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                    modifier = Modifier.testTag("tab_pdf_lab")
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("2D CAD ও DXF") },
                    leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null) },
                    modifier = Modifier.testTag("tab_2d_cad")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTab == 0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("PDF ও নকশা ইমেজ আমদানি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("pick_pdf_button")
                                ) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("PDF খুলুন")
                                }
                                OutlinedButton(
                                    onClick = {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.weight(1f).testTag("pick_images_button")
                                ) {
                                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("JPG/PNG নিন")
                                }
                            }

                            if (pdfPageCount > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        enabled = pdfPageIndex > 0,
                                        onClick = {
                                            val nextIdx = (pdfPageIndex - 1).coerceAtLeast(0)
                                            pdfPageIndex = nextIdx
                                            val uri = selectedPdfUri
                                            if (uri != null) {
                                                scope.launch {
                                                    renderedBitmap = renderPdfPage(uri, nextIdx)?.third
                                                }
                                            } else if (mergeImageBitmaps.isNotEmpty()) {
                                                renderedBitmap = mergeImageBitmaps[nextIdx]
                                            }
                                        }
                                    ) { Text("আগের পেজ") }
                                    Text("পৃষ্ঠা ${pdfPageIndex + 1} / $pdfPageCount", fontWeight = FontWeight.Bold)
                                    OutlinedButton(
                                        enabled = pdfPageIndex < pdfPageCount - 1,
                                        onClick = {
                                            val nextIdx = (pdfPageIndex + 1).coerceAtMost(pdfPageCount - 1)
                                            pdfPageIndex = nextIdx
                                            val uri = selectedPdfUri
                                            if (uri != null) {
                                                scope.launch {
                                                    renderedBitmap = renderPdfPage(uri, nextIdx)?.third
                                                }
                                            } else if (mergeImageBitmaps.isNotEmpty()) {
                                                renderedBitmap = mergeImageBitmaps[nextIdx]
                                            }
                                        }
                                    ) { Text("পরের পেজ") }
                                }
                            }

                            val previewBmp = remember(renderedBitmap, contrastVal, blackAndWhite, rotationDeg) {
                                renderedBitmap?.let { applyImageAdjustments(it, contrastVal, blackAndWhite, rotationDeg) }
                            }

                            if (previewBmp != null) {
                                Image(
                                    bitmap = previewBmp.asImageBitmap(),
                                    contentDescription = "PDF/Image Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("উপরের বাটন থেকে PDF বা একাধিক ছবি নির্বাচন করুন", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Text("নকশা পরিষ্কার ও ঘূর্ণন (Image Clean & Deskew)", style = MaterialTheme.typography.labelLarge)
                            Text("কনট্রাস্ট: ${String.format(Locale.US, "%.2fx", contrastVal)}", style = MaterialTheme.typography.labelSmall)
                            Slider(value = contrastVal, onValueChange = { contrastVal = it }, valueRange = 0.6f..2.2f)
                            Text("ঘূর্ণন কোণ (Deskew): ${rotationDeg.toInt()}°", style = MaterialTheme.typography.labelSmall)
                            Slider(value = rotationDeg, onValueChange = { rotationDeg = it }, valueRange = -15f..15f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("সাদা-কালো হাই-কনট্রাস্ট মোড (B&W)")
                                Switch(checked = blackAndWhite, onCheckedChange = { blackAndWhite = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    enabled = previewBmp != null,
                                    onClick = {
                                        val bmp = previewBmp ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            val res = ExportStreamWriter.savePngBitmap(
                                                context,
                                                bmp,
                                                "Bhumi_Page_${System.currentTimeMillis()}"
                                            )
                                            withContext(Dispatchers.Main) {
                                                toast = "PNG সেভ হয়েছে: ${res.displayPath}"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("export_pdf_page_png")
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PNG সেভ")
                                }

                                OutlinedButton(
                                    enabled = mergeImageBitmaps.isNotEmpty() || previewBmp != null,
                                    onClick = {
                                        val pages = if (mergeImageBitmaps.isNotEmpty()) mergeImageBitmaps else listOfNotNull(previewBmp)
                                        scope.launch(Dispatchers.IO) {
                                            val doc = PdfDocument()
                                            pages.forEachIndexed { idx, b ->
                                                val info = PdfDocument.PageInfo.Builder(b.width, b.height, idx + 1).create()
                                                val page = doc.startPage(info)
                                                page.canvas.drawBitmap(b, 0f, 0f, null)
                                                doc.finishPage(page)
                                            }
                                            val first = pages.first()
                                            val res = ExportStreamWriter.savePdfDocument(
                                                context,
                                                doc,
                                                first.width,
                                                first.height,
                                                "Bhumi_Merged_${System.currentTimeMillis()}"
                                            )
                                            doc.close()
                                            withContext(Dispatchers.Main) {
                                                toast = "${pages.size} পৃষ্ঠার PDF সেভ: ${res.displayPath}"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("export_merged_pdf")
                                ) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF মার্জ সেভ")
                                }
                            }
                        }
                    }
                } else {
                    // 2D CAD Studio
                    item {
                        val geom = remember(cadVertices) { SurveyMathProEngine.computeCoordinateGeometry(cadVertices) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = cadTitle,
                                onValueChange = { cadTitle = it },
                                label = { Text("CAD প্লট / ফাইলের নাম") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("৫ ফুট গ্রিড স্ন্যাপ (Grid Snap)")
                                }
                                Switch(checked = gridSnapFt, onCheckedChange = { gridSnapFt = it })
                            }

                            Text("ক্যানভাসে ট্যাপ করে নতুন কোণা (Vertex) যোগ করুন:", style = MaterialTheme.typography.labelMedium)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(270.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0E1B16))
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(gridSnapFt, cadScaleFtPerUnit) {
                                            detectTapGestures { offset ->
                                                val pad = 32f
                                                val worldW = cadScaleFtPerUnit
                                                val worldH = cadScaleFtPerUnit * (size.height.toFloat() / size.width.toFloat().coerceAtLeast(1f))
                                                var xFt = ((offset.x - pad) / (size.width - pad * 2).coerceAtLeast(1f)) * worldW
                                                var yFt = ((size.height - pad - offset.y) / (size.height - pad * 2).coerceAtLeast(1f)) * worldH
                                                if (gridSnapFt) {
                                                    xFt = (xFt / 5f).roundToInt() * 5f
                                                    yFt = (yFt / 5f).roundToInt() * 5f
                                                }
                                                undoStack = undoStack + listOf(cadVertices)
                                                cadVertices = cadVertices + PolygonVertex(xFt.toDouble(), yFt.toDouble())
                                            }
                                        }
                                ) {
                                    val gridStep = 10f
                                    val pad = 32f
                                    val scalePx = (size.width - pad * 2) / cadScaleFtPerUnit
                                    // Draw CAD Grid
                                    var g = 0f
                                    while (g <= cadScaleFtPerUnit) {
                                        val gx = pad + g * scalePx
                                        drawLine(Color(0xFF1E3A30), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                                        val gy = size.height - pad - g * scalePx
                                        if (gy in 0f..size.height) {
                                            drawLine(Color(0xFF1E3A30), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                                        }
                                        g += gridStep
                                    }

                                    // Draw North arrow at top right
                                    val nx = size.width - 30f
                                    val ny = 34f
                                    drawLine(Color(0xFF64FFDA), Offset(nx, ny + 18f), Offset(nx, ny - 16f), strokeWidth = 3f)
                                    drawLine(Color(0xFF64FFDA), Offset(nx, ny - 16f), Offset(nx - 8f, ny - 4f), strokeWidth = 3f)
                                    drawLine(Color(0xFF64FFDA), Offset(nx, ny - 16f), Offset(nx + 8f, ny - 4f), strokeWidth = 3f)

                                    if (cadVertices.isNotEmpty()) {
                                        val path = Path()
                                        cadVertices.forEachIndexed { idx, v ->
                                            val sx = pad + v.x.toFloat() * scalePx
                                            val sy = size.height - pad - v.y.toFloat() * scalePx
                                            if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                                        }
                                        if (cadVertices.size >= 3) {
                                            path.close()
                                            drawPath(path, Color(0xFF0F6B54).copy(alpha = 0.35f))
                                        }
                                        drawPath(path, Color(0xFF64FFDA), style = Stroke(width = 3f))
                                        cadVertices.forEach { v ->
                                            val sx = pad + v.x.toFloat() * scalePx
                                            val sy = size.height - pad - v.y.toFloat() * scalePx
                                            drawCircle(Color(0xFFFFD54F), radius = 6f, center = Offset(sx, sy))
                                        }
                                    }
                                }
                                Text(
                                    text = "N ↑  |  স্কেল ক্ষেত্র: ${cadScaleFtPerUnit.toInt()} ফু",
                                    color = Color(0xFFA7F3D0),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    enabled = undoStack.isNotEmpty(),
                                    onClick = {
                                        val prev = undoStack.lastOrNull() ?: return@OutlinedButton
                                        undoStack = undoStack.dropLast(1)
                                        cadVertices = prev
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Undo")
                                }
                                OutlinedButton(onClick = {
                                    undoStack = undoStack + listOf(cadVertices)
                                    cadVertices = emptyList()
                                }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("পরিষ্কার")
                                }
                                OutlinedButton(onClick = {
                                    undoStack = undoStack + listOf(cadVertices)
                                    cadVertices = listOf(
                                        PolygonVertex(0.0, 0.0),
                                        PolygonVertex(100.0, 0.0),
                                        PolygonVertex(100.0, 80.0),
                                        PolygonVertex(0.0, 80.0)
                                    )
                                }) {
                                    Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("রিসেট ১০০×৮০")
                                }
                            }

                            if (geom != null) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "ক্ষেত্রফল: %.2f ব.ফু (%.3f শতাংশ / %.3f কাঠা) • পরিসীমা: %.1f ফু",
                                        geom.sqFeet,
                                        geom.decimal,
                                        geom.sqFeet / 720.0,
                                        geom.perimeterFt
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "বাহুসমূহ: ${geom.edgeLengthsFt.joinToString(" | ") { String.format(Locale.US, "%.1f'", it) }}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    enabled = cadVertices.size >= 3,
                                    onClick = {
                                        val dxf = GisCoordinateEngine.exportPlanarDxf(
                                            cadTitle,
                                            cadVertices.map { it.x to it.y }
                                        )
                                        val res = DocumentAndReportEngine.saveBinaryDocument(
                                            context,
                                            "${cadTitle.replace(" ", "_")}.dxf",
                                            "application/dxf",
                                            dxf.toByteArray(Charsets.UTF_8)
                                        )
                                        toast = "AutoCAD DXF সেভ হয়েছে: ${res.displayPath}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                    modifier = Modifier.weight(1f).testTag("export_cad_dxf")
                                ) {
                                    Text("DXF সেভ")
                                }

                                Button(
                                    enabled = cadVertices.size >= 3,
                                    onClick = {
                                        val bmp = renderCadPlanBitmap(cadTitle, cadVertices, geom)
                                        val res = ExportStreamWriter.savePdfBitmap(context, bmp, "${cadTitle}_CAD")
                                        toast = "CAD PDF সেভ হয়েছে: ${res.displayPath}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("export_cad_pdf")
                                ) {
                                    Text("CAD PDF সেভ")
                                }
                            }
                        }
                    }
                }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

private fun renderCadPlanBitmap(
    title: String,
    vertices: List<PolygonVertex>,
    geom: com.example.engine.CoordinateGeometryResult?
): Bitmap {
    val w = 1240
    val h = 1240
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(android.graphics.Color.WHITE)

    val minX = vertices.minOf { it.x }
    val maxX = vertices.maxOf { it.x }
    val minY = vertices.minOf { it.y }
    val maxY = vertices.maxOf { it.y }
    val spanX = (maxX - minX).coerceAtLeast(1.0)
    val spanY = (maxY - minY).coerceAtLeast(1.0)
    val pad = 160f
    val scale = minOf((w - pad * 2) / spanX.toFloat(), (h - pad * 2 - 120f) / spanY.toFloat())

    val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F6B54")
        textSize = 36f
        isFakeBoldText = true
    }
    c.drawText("2D CAD নকশা: $title", 64f, 84f, titleP)

    val polyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F6B54")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#220F6B54")
        style = Paint.Style.FILL
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
    }

    val p = android.graphics.Path()
    val pts = vertices.map { v ->
        val sx = pad + (v.x - minX).toFloat() * scale
        val sy = (h - pad - 80f) - (v.y - minY).toFloat() * scale
        sx to sy
    }
    pts.forEachIndexed { idx, (sx, sy) -> if (idx == 0) p.moveTo(sx, sy) else p.lineTo(sx, sy) }
    p.close()
    c.drawPath(p, fillPaint)
    c.drawPath(p, polyPaint)

    pts.forEachIndexed { i, (x1, y1) ->
        val (x2, y2) = pts[(i + 1) % pts.size]
        val len = hypot(vertices[(i + 1) % vertices.size].x - vertices[i].x, vertices[(i + 1) % vertices.size].y - vertices[i].y)
        c.drawText(String.format(Locale.US, "%.1f'", len), (x1 + x2) / 2f, (y1 + y2) / 2f - 8f, labelPaint)
    }

    if (geom != null) {
        c.drawText(
            String.format(Locale.US, "ক্ষেত্রফল: %.2f শতাংশ (%.2f ব.ফু) | পরিসীমা: %.1f ফু", geom.decimal, geom.sqFeet, geom.perimeterFt),
            64f,
            h - 60f,
            labelPaint
        )
    }
    return bmp
}
