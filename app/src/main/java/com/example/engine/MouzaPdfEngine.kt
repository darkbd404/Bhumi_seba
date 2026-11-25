package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PlotWallIndex(
    val gridWidth: Int,
    val gridHeight: Int,
    private val wallMask: ByteArray
) {
    sealed class TapResult {
        data class Success(val plot: MouzaPlot) : TapResult()
        data class AlreadySelected(val plotId: Int) : TapResult()
        data class LeakedOrOutside(val reasonBn: String) : TapResult()
    }

    /**
     * Performs ultra-fast scanline flood-fill at normalized coordinates (0..1)
     * cleanly filling the entire plot polygon including over removed dag numbers.
     */
    fun selectPlotAt(
        normX: Float,
        normY: Float,
        plotId: Int,
        plotIndex: Int,
        color: ComposeColor,
        existingPlots: List<MouzaPlot>
    ): TapResult {
        val startX = (normX * gridWidth).toInt().coerceIn(0, gridWidth - 1)
        val startY = (normY * gridHeight).toInt().coerceIn(0, gridHeight - 1)

        // Check if tap landed inside an already-selected plot so user can toggle it off cleanly
        val tapFlat = startY * gridWidth + startX
        for (existing in existingPlots) {
            val poly = existing.effectivePolygon()
            if (poly != null && MouzaPdfEngine.isPointInPolygon(normX, normY, poly)) {
                return TapResult.AlreadySelected(existing.id)
            }
            if (existing.maskIndices.binarySearch(tapFlat) >= 0) {
                return TapResult.AlreadySelected(existing.id)
            }
        }

        // If user tapped right on a dark line, find nearest open cell within 6px radius
        var seedX = startX
        var seedY = startY
        if (wallMask[seedY * gridWidth + seedX] != 0.toByte()) {
            var found = false
            outer@ for (r in 1..6) {
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val nx = startX + dx
                        val ny = startY + dy
                        if (nx in 0 until gridWidth && ny in 0 until gridHeight) {
                            if (wallMask[ny * gridWidth + nx] == 0.toByte()) {
                                seedX = nx
                                seedY = ny
                                found = true
                                break@outer
                            }
                        }
                    }
                }
            }
            if (!found) {
                return TapResult.LeakedOrOutside("দাগের ভেতরের ফাঁকা অংশে ট্যাপ করুন।")
            }
        }

        val totalCells = gridWidth * gridHeight
        val visited = ByteArray(totalCells)
        val queue = IntArray(min(totalCells, 512_000))
        var head = 0
        var tail = 0

        val seedIdx = seedY * gridWidth + seedX
        visited[seedIdx] = 1
        queue[tail++] = seedIdx

        var minX = seedX
        var maxX = seedX
        var minY = seedY
        var maxY = seedY
        var touchesTop = false
        var touchesBottom = false
        var touchesLeft = false
        var touchesRight = false

        val maxAllowedPixels = (totalCells * 0.55).toInt()
        val selectedList = ArrayList<Int>(4096)

        while (head < tail) {
            val curr = queue[head++]
            selectedList.add(curr)

            if (selectedList.size > maxAllowedPixels) {
                return TapResult.LeakedOrOutside(
                    "দাগের সীমানা খোলা বা বাইরের অংশে ট্যাপ হয়েছে। 'গ্যাপ সিল' বাড়িয়ে আবার চেষ্টা করুন অথবা 'পলিগন' ব্যবহার করুন।"
                )
            }

            val cx = curr % gridWidth
            val cy = curr / gridWidth

            if (cx < minX) minX = cx
            if (cx > maxX) maxX = cx
            if (cy < minY) minY = cy
            if (cy > maxY) maxY = cy

            if (cx == 0) touchesLeft = true
            if (cx == gridWidth - 1) touchesRight = true
            if (cy == 0) touchesTop = true
            if (cy == gridHeight - 1) touchesBottom = true

            if (cx + 1 < gridWidth) {
                val n = curr + 1
                if (visited[n] == 0.toByte() && wallMask[n] == 0.toByte()) {
                    visited[n] = 1
                    if (tail < queue.size) queue[tail++] = n
                }
            }
            if (cx - 1 >= 0) {
                val n = curr - 1
                if (visited[n] == 0.toByte() && wallMask[n] == 0.toByte()) {
                    visited[n] = 1
                    if (tail < queue.size) queue[tail++] = n
                }
            }
            if (cy + 1 < gridHeight) {
                val n = curr + gridWidth
                if (visited[n] == 0.toByte() && wallMask[n] == 0.toByte()) {
                    visited[n] = 1
                    if (tail < queue.size) queue[tail++] = n
                }
            }
            if (cy - 1 >= 0) {
                val n = curr - gridWidth
                if (visited[n] == 0.toByte() && wallMask[n] == 0.toByte()) {
                    visited[n] = 1
                    if (tail < queue.size) queue[tail++] = n
                }
            }
        }

        val edgeTouchCount = listOf(touchesTop, touchesBottom, touchesLeft, touchesRight).count { it }
        if (edgeTouchCount >= 3 || selectedList.size < 20) {
            val msg = if (selectedList.size < 20) {
                "খুব ছোট অংশে ট্যাপ হয়েছে — অনুগ্রহ করে দাগের ভেতরে ট্যাপ করুন।"
            } else {
                "ম্যাপের বাইরের মার্জিন সিলেক্ট হয়েছে। অনুগ্রহ করে দাগের ভেতরে ট্যাপ করুন।"
            }
            return TapResult.LeakedOrOutside(msg)
        }

        // Fill any tiny enclosed holes (e.g., inside closed loops of digits like 0, 4, 6, 8, 9)
        val bw = maxX - minX + 1
        val bh = maxY - minY + 1
        if (bw in 3..900 && bh in 3..900) {
            val boxW = bw + 2
            val boxH = bh + 2
            val outsideMask = ByteArray(boxW * boxH)
            val holeQueue = IntArray(boxW * boxH)
            var hHead = 0
            var hTail = 0
            outsideMask[0] = 1
            holeQueue[hTail++] = 0

            while (hHead < hTail) {
                val cur = holeQueue[hHead++]
                val bx = cur % boxW
                val by = cur / boxW

                val neighbors = intArrayOf(
                    if (bx + 1 < boxW) cur + 1 else -1,
                    if (bx - 1 >= 0) cur - 1 else -1,
                    if (by + 1 < boxH) cur + boxW else -1,
                    if (by - 1 >= 0) cur - boxW else -1
                )
                for (n in neighbors) {
                    if (n < 0 || outsideMask[n] != 0.toByte()) continue
                    val nbx = n % boxW
                    val nby = n / boxW
                    val gx = minX + (nbx - 1)
                    val gy = minY + (nby - 1)
                    val isSelected = gx in minX..maxX && gy in minY..maxY &&
                        visited[gy * gridWidth + gx] == 1.toByte()
                    if (!isSelected) {
                        outsideMask[n] = 1
                        holeQueue[hTail++] = n
                    }
                }
            }

            for (gy in minY..maxY) {
                for (gx in minX..maxX) {
                    val idx = gy * gridWidth + gx
                    if (visited[idx] == 0.toByte()) {
                        val nbx = gx - minX + 1
                        val nby = gy - minY + 1
                        if (outsideMask[nby * boxW + nbx] == 0.toByte()) {
                            visited[idx] = 1
                            selectedList.add(idx)
                        }
                    }
                }
            }
        }

        val sortedIndices = selectedList.toIntArray().apply { sort() }
        return TapResult.Success(
            MouzaPlot(
                id = plotId,
                plotIndex = plotIndex,
                pixelCount = sortedIndices.size,
                maskIndices = sortedIndices,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                minX = minX,
                minY = minY,
                maxX = maxX,
                maxY = maxY,
                color = color
            )
        )
    }
}

object MouzaPdfEngine {

    private val renderMutex = Mutex()

    data class DocumentInfo(
        val tempFile: File,
        val pageCount: Int,
        val fileName: String,
        val isPdf: Boolean,
        val baseWidthPts: Int,
        val baseHeightPts: Int
    )

    data class PdfDocumentInfo(
        val tempFile: File,
        val pageCount: Int,
        val fileName: String
    )

    data class RenderedPageResult(
        val bitmap: Bitmap,
        val pdfPageWidthPts: Int,
        val pdfPageHeightPts: Int,
        val isPreview: Boolean
    )

    data class ViewportTileResult(
        val bitmap: Bitmap,
        val normLeft: Float,
        val normTop: Float,
        val normRight: Float,
        val normBottom: Float,
        val zoomScale: Float = 1f
    )

    sealed class AutoFillResult {
        data class Success(val plot: MouzaPlot) : AutoFillResult()
        data class AlreadySelected(val plotId: Int) : AutoFillResult()
        data class Failed(val reasonBn: String) : AutoFillResult()
    }

    suspend fun copyPdfToCache(context: Context, uri: Uri, fileName: String): PdfDocumentInfo {
        val doc = copyDocumentToCache(context, uri, fileName, mimeType = "application/pdf")
        return PdfDocumentInfo(doc.tempFile, doc.pageCount, doc.fileName)
    }

    suspend fun copyDocumentToCache(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String?
    ): DocumentInfo = withContext(Dispatchers.IO) {
        val looksPdf = mimeType?.contains("pdf", ignoreCase = true) == true ||
            fileName.endsWith(".pdf", ignoreCase = true)
        val ext = if (looksPdf) "pdf" else {
            val dotExt = fileName.substringAfterLast('.', "").lowercase()
            if (dotExt in listOf("jpg", "jpeg", "png", "webp")) dotExt else "img"
        }

        context.cacheDir.listFiles { f -> f.name.startsWith("active_mouza_") }?.forEach {
            runCatching { it.delete() }
        }

        val cacheFile = File(context.cacheDir, "active_mouza_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output, bufferSize = 65536)
            }
        } ?: error("ফাইলটি পড়া যায়নি।")

        // Peek first 4 bytes in case MIME didn't report PDF cleanly
        val header = ByteArray(4)
        cacheFile.inputStream().use { it.read(header) }
        val isPdf = looksPdf || String(header, Charsets.US_ASCII) == "%PDF"

        if (isPdf) {
            val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount.coerceAtLeast(1)
            val page = renderer.openPage(0)
            val pw = page.width
            val ph = page.height
            page.close()
            renderer.close()
            pfd.close()
            DocumentInfo(
                tempFile = cacheFile,
                pageCount = count,
                fileName = fileName,
                isPdf = true,
                baseWidthPts = pw,
                baseHeightPts = ph
            )
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(cacheFile.absolutePath, opts)
            DocumentInfo(
                tempFile = cacheFile,
                pageCount = 1,
                fileName = fileName,
                isPdf = false,
                baseWidthPts = opts.outWidth.coerceAtLeast(100),
                baseHeightPts = opts.outHeight.coerceAtLeast(100)
            )
        }
    }

    /**
     * Renders full PDF page (used for initial sheet + wall index).
     */
    suspend fun renderPdfPage(
        context: Context,
        pdfFile: File,
        pageIndex: Int,
        ultraHd: Boolean
    ): RenderedPageResult = renderBaseDocument(
        context = context,
        docFile = pdfFile,
        isPdf = true,
        pageIndex = pageIndex,
        ultraHd = ultraHd
    )

    suspend fun renderBaseDocument(
        context: Context,
        docFile: File,
        isPdf: Boolean,
        pageIndex: Int,
        ultraHd: Boolean
    ): RenderedPageResult = withContext(Dispatchers.Default) {
        if (isPdf) {
            renderMutex.withLock {
                val pfd = ParcelFileDescriptor.open(docFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val safePageIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                val page = renderer.openPage(safePageIdx)

                val pagePtsW = page.width
                val pagePtsH = page.height

                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val isLowRam = am?.isLowRamDevice == true
                val targetMaxDim = if (ultraHd) {
                    if (isLowRam) 1800 else 2400
                } else {
                    1400
                }

                val aspect = pagePtsW.toFloat() / pagePtsH.toFloat().coerceAtLeast(1f)
                val outW: Int
                val outH: Int
                if (pagePtsW >= pagePtsH) {
                    outW = targetMaxDim
                    outH = (targetMaxDim / aspect).toInt().coerceAtLeast(200)
                } else {
                    outH = targetMaxDim
                    outW = (targetMaxDim * aspect).toInt().coerceAtLeast(200)
                }

                val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                page.close()
                renderer.close()
                pfd.close()

                RenderedPageResult(
                    bitmap = bitmap,
                    pdfPageWidthPts = pagePtsW,
                    pdfPageHeightPts = pagePtsH,
                    isPreview = !ultraHd
                )
            }
        } else {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(docFile.absolutePath, boundsOpts)
            val srcW = boundsOpts.outWidth.coerceAtLeast(100)
            val srcH = boundsOpts.outHeight.coerceAtLeast(100)
            val maxDim = if (ultraHd) 3200 else 1200
            var sample = 1
            while (max(srcW, srcH) / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(docFile.absolutePath, decodeOpts)
                ?: error("ছবি ডিকোড করা যায়নি।")
            // Convert image pixels into pseudo-points at 96 DPI -> 72 pts scale so feet math matches UI.html
            val ptsW = (srcW * 72f / 96f).roundToInt().coerceAtLeast(100)
            val ptsH = (srcH * 72f / 96f).roundToInt().coerceAtLeast(100)
            RenderedPageResult(
                bitmap = decoded,
                pdfPageWidthPts = ptsW,
                pdfPageHeightPts = ptsH,
                isPreview = !ultraHd
            )
        }
    }

    /**
     * Renders ONLY the currently visible normalized viewport rectangle [normLeft..normRight, normTop..normBottom]
     * straight from vector PDF (or full-resolution source image) at full device crispness — even at 1000x zoom!
     */
    suspend fun renderViewportTile(
        docFile: File,
        isPdf: Boolean,
        pageIndex: Int,
        normLeft: Float,
        normTop: Float,
        normRight: Float,
        normBottom: Float,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): ViewportTileResult? = withContext(Dispatchers.Default) {
        val nl = normLeft.coerceIn(0f, 0.9999f)
        val nt = normTop.coerceIn(0f, 0.9999f)
        val nr = normRight.coerceIn(nl + 0.0002f, 1f)
        val nb = normBottom.coerceIn(nt + 0.0002f, 1f)

        val outW = targetWidthPx.coerceIn(120, 1800)
        val outH = targetHeightPx.coerceIn(120, 2200)

        if (isPdf) {
            renderMutex.withLock {
                runCatching {
                    val pfd = ParcelFileDescriptor.open(docFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    val safePageIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    val page = renderer.openPage(safePageIdx)

                    val pageW = page.width.toFloat()
                    val pageH = page.height.toFloat()
                    val spanW = (nr - nl) * pageW
                    val spanH = (nb - nt) * pageH
                    val scaleX = outW / spanW
                    val scaleY = outH / spanH
                    val uniformScale = min(scaleX, scaleY)

                    val finalW = (spanW * uniformScale).roundToInt().coerceIn(64, outW)
                    val finalH = (spanH * uniformScale).roundToInt().coerceIn(64, outH)

                    val bitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(Color.WHITE)

                    val matrix = Matrix().apply {
                        postTranslate(-nl * pageW, -nt * pageH)
                        postScale(uniformScale, uniformScale)
                    }
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()

                    ViewportTileResult(bitmap, nl, nt, nr, nb)
                }.getOrNull()
            }
        } else {
            runCatching {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(docFile.absolutePath, boundsOpts)
                val imgW = boundsOpts.outWidth.coerceAtLeast(1)
                val imgH = boundsOpts.outHeight.coerceAtLeast(1)

                val rx0 = (nl * imgW).toInt().coerceIn(0, imgW - 2)
                val ry0 = (nt * imgH).toInt().coerceIn(0, imgH - 2)
                val rx1 = (nr * imgW).roundToInt().coerceIn(rx0 + 2, imgW)
                val ry1 = (nb * imgH).roundToInt().coerceIn(ry0 + 2, imgH)

                val cropW = (rx1 - rx0).coerceAtLeast(2)
                val cropH = (ry1 - ry0).coerceAtLeast(2)
                var sample = 1
                while (cropW / sample > outW * 2 && cropH / sample > outH * 2) {
                    sample *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val region = runCatching {
                    val decoder = BitmapRegionDecoder.newInstance(docFile.absolutePath, false)
                    val bmp = decoder?.decodeRegion(Rect(rx0, ry0, rx1, ry1), decodeOpts)
                    decoder?.recycle()
                    bmp
                }.getOrNull() ?: run {
                    val full = BitmapFactory.decodeFile(docFile.absolutePath, decodeOpts) ?: return@runCatching null
                    val sx0 = (rx0 / sample).coerceIn(0, full.width - 2)
                    val sy0 = (ry0 / sample).coerceIn(0, full.height - 2)
                    val sx1 = (rx1 / sample).coerceIn(sx0 + 2, full.width)
                    val sy1 = (ry1 / sample).coerceIn(sy0 + 2, full.height)
                    val sub = Bitmap.createBitmap(full, sx0, sy0, sx1 - sx0, sy1 - sy0)
                    if (sub != full) full.recycle()
                    sub
                }

                val uniformScale = min(outW.toFloat() / region.width, outH.toFloat() / region.height).coerceAtMost(2.5f)
                val finalW = (region.width * uniformScale).roundToInt().coerceIn(64, outW)
                val finalH = (region.height * uniformScale).roundToInt().coerceIn(64, outH)

                val scaled = if (region.width != finalW || region.height != finalH) {
                    val out = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
                    val c = Canvas(out)
                    c.drawColor(Color.WHITE)
                    c.drawBitmap(
                        region,
                        Rect(0, 0, region.width, region.height),
                        RectF(0f, 0f, finalW.toFloat(), finalH.toFloat()),
                        Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                    region.recycle()
                    out
                } else {
                    region
                }
                ViewportTileResult(scaled, nl, nt, nr, nb)
            }.getOrNull()
        }
    }

    /**
     * High-Precision Local-Window Plot Boundary Auto-Filler (matched to reference UI.html lfill).
     * 1. Renders a crisp local window directly from the vector PDF/image centered on (normX, normY).
     * 2. Computes integral-image local adaptive threshold + stroke thickness T so Dag numbers are ignored.
     * 3. Flood-fills, absorbs interior digits/holes via morphological closing, traces outer contour,
     *    and simplifies via Douglas-Peucker into clean editable vector corners (`polygonNormalized`).
     */
    suspend fun autoFillPlotPolygon(
        docFile: File,
        isPdf: Boolean,
        pageIndex: Int,
        pageWidthPts: Int,
        pageHeightPts: Int,
        scale: MouzaScale,
        calibrationMultiplier: Double,
        normX: Float,
        normY: Float,
        plotId: Int,
        plotIndex: Int,
        color: ComposeColor,
        existingPlots: List<MouzaPlot>,
        gapSealLevel: Int
    ): AutoFillResult = withContext(Dispatchers.Default) {
        // Toggle off if tapped inside any existing polygon plot
        for (existing in existingPlots) {
            val poly = existing.effectivePolygon()
            if (poly != null && isPointInPolygon(normX, normY, poly)) {
                return@withContext AutoFillResult.AlreadySelected(existing.id)
            }
        }

        val aspect = pageHeightPts.toFloat() / pageWidthPts.toFloat().coerceAtLeast(1f)
        var halfWinNormX = 0.14f
        var lastError = "এই দাগের সীমানা খোলা — একটু জুম করে দাগের মাঝখানে ট্যাপ করুন।"

        for (attempt in 0 until 6) {
            val halfWinNormY = halfWinNormX / aspect.coerceAtLeast(0.4f)
            val wW = (halfWinNormX * 2f).coerceAtMost(1f)
            val wH = (halfWinNormY * 2f).coerceAtMost(1f)

            val nl = (normX - wW / 2f).coerceIn(0f, (1f - wW).coerceAtLeast(0f))
            val nt = (normY - wH / 2f).coerceIn(0f, (1f - wH).coerceAtLeast(0f))
            val nr = (nl + wW).coerceAtMost(1f)
            val nb = (nt + wH).coerceAtMost(1f)

            val winSpanW = nr - nl
            val winSpanH = nb - nt
            val maxRenderDim = 1150
            val renderW: Int
            val renderH: Int
            if (winSpanW * pageWidthPts >= winSpanH * pageHeightPts) {
                renderW = maxRenderDim
                renderH = (maxRenderDim * (winSpanH * pageHeightPts) / (winSpanW * pageWidthPts))
                    .roundToInt().coerceIn(220, maxRenderDim)
            } else {
                renderH = maxRenderDim
                renderW = (maxRenderDim * (winSpanW * pageWidthPts) / (winSpanH * pageHeightPts))
                    .roundToInt().coerceIn(220, maxRenderDim)
            }

            val tile = renderViewportTile(
                docFile = docFile,
                isPdf = isPdf,
                pageIndex = pageIndex,
                normLeft = nl,
                normTop = nt,
                normRight = nr,
                normBottom = nb,
                targetWidthPx = renderW,
                targetHeightPx = renderH
            ) ?: break

            val w = tile.bitmap.width
            val h = tile.bitmap.height
            val pxBuf = IntArray(w * h)
            tile.bitmap.getPixels(pxBuf, 0, w, 0, 0, w, h)
            tile.bitmap.recycle()

            val seedX = (((normX - nl) / winSpanW) * w).roundToInt().coerceIn(0, w - 1)
            val seedY = (((normY - nt) / winSpanH) * h).roundToInt().coerceIn(0, h - 1)

            val traced = runLocalLfill(
                pixels = pxBuf,
                w = w,
                h = h,
                seedXIn = seedX,
                seedYIn = seedY,
                gapSealLevel = gapSealLevel
            )

            when (traced) {
                is LfillOutput.WallHit -> {
                    return@withContext AutoFillResult.Failed("দাগের কালো রেখার ওপর ট্যাপ হয়েছে — দাগের ভেতরের সাদা অংশে ট্যাপ করুন।")
                }
                is LfillOutput.TooBigOrOpen -> {
                    lastError = "দাগের সীমানা খোলা অথবা বড় — 'গ্যাপ সিল' বাড়িয়ে মাঝখানে ট্যাপ করুন।"
                    // Expand window and retry if we haven't covered whole page yet
                    if (nl <= 0.001f && nt <= 0.001f && nr >= 0.999f && nb >= 0.999f) break
                    halfWinNormX *= 1.85f
                }
                is LfillOutput.Success -> {
                    val touch = traced.touchesEdge
                    val touchesUnclippedSide = (touch[0] && nl > 0.002f) ||
                        (touch[1] && nt > 0.002f) ||
                        (touch[2] && nr < 0.998f) ||
                        (touch[3] && nb < 0.998f)

                    if (touchesUnclippedSide && !(nl <= 0.002f && nt <= 0.002f && nr >= 0.998f && nb >= 0.998f)) {
                        halfWinNormX *= 1.95f
                        continue
                    }

                    val polyNorm = traced.points.map { (lx, ly) ->
                        (nl + (lx / w.toFloat()) * winSpanW).coerceIn(0f, 1f) to
                            (nt + (ly / h.toFloat()) * winSpanH).coerceIn(0f, 1f)
                    }
                    val cxNorm = (nl + (traced.cx / w.toFloat()) * winSpanW).coerceIn(0f, 1f)
                    val cyNorm = (nt + (traced.cy / h.toFloat()) * winSpanH).coerceIn(0f, 1f)

                    val sqFt = computePolygonAreaSqFeet(
                        polygonNorm = polyNorm,
                        pageWidthPts = pageWidthPts,
                        pageHeightPts = pageHeightPts,
                        scale = scale,
                        calibrationMultiplier = calibrationMultiplier
                    )
                    val baseGridW = 1500
                    val baseGridH = (1500f * aspect).roundToInt().coerceAtLeast(200)
                    val pseudoPxCount = ((computePolygonAreaNormalized(polyNorm) * baseGridW * baseGridH))
                        .roundToInt().coerceAtLeast(1)

                    return@withContext AutoFillResult.Success(
                        MouzaPlot(
                            id = plotId,
                            plotIndex = plotIndex,
                            pixelCount = pseudoPxCount,
                            maskIndices = IntArray(0),
                            gridWidth = baseGridW,
                            gridHeight = baseGridH,
                            minX = ((polyNorm.minOf { it.first }) * baseGridW).toInt(),
                            minY = ((polyNorm.minOf { it.second }) * baseGridH).toInt(),
                            maxX = ((polyNorm.maxOf { it.first }) * baseGridW).toInt(),
                            maxY = ((polyNorm.maxOf { it.second }) * baseGridH).toInt(),
                            color = color,
                            polygonNormalized = polyNorm,
                            centroidNormalized = cxNorm to cyNorm,
                            exactAreaSqFeet = sqFt
                        )
                    )
                }
            }
        }

        AutoFillResult.Failed(lastError)
    }

    private sealed class LfillOutput {
        data object WallHit : LfillOutput()
        data object TooBigOrOpen : LfillOutput()
        data class Success(
            val points: List<Pair<Float, Float>>,
            val cx: Float,
            val cy: Float,
            val touchesEdge: BooleanArray
        ) : LfillOutput()
    }

    private fun runLocalLfill(
        pixels: IntArray,
        w: Int,
        h: Int,
        seedXIn: Int,
        seedYIn: Int,
        gapSealLevel: Int
    ): LfillOutput {
        val n = w * h
        val gray = IntArray(n)
        for (i in 0 until n) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = (r * 77 + g * 150 + b * 29) shr 8
        }

        // Fast local box-mean via separable sliding window (from UI.html lfill)
        val radius = when (gapSealLevel) {
            0 -> 9
            2 -> 15
            else -> 12
        }
        val tmp = IntArray(n)
        val mean = IntArray(n)
        val kernelArea = (2 * radius + 1) * (2 * radius + 1)

        for (y in 0 until h) {
            var sum = 0
            val rowOff = y * w
            for (x in -radius..radius) {
                sum += gray[rowOff + x.coerceIn(0, w - 1)]
            }
            for (x in 0 until w) {
                tmp[rowOff + x] = sum
                sum += gray[rowOff + (x + radius + 1).coerceAtMost(w - 1)] -
                    gray[rowOff + (x - radius).coerceAtLeast(0)]
            }
        }
        for (x in 0 until w) {
            var sum = 0
            for (y in -radius..radius) {
                sum += tmp[y.coerceIn(0, h - 1) * w + x]
            }
            for (y in 0 until h) {
                mean[y * w + x] = sum / kernelArea
                sum += tmp[(y + radius + 1).coerceAtMost(h - 1) * w + x] -
                    tmp[(y - radius).coerceAtLeast(0) * w + x]
            }
        }

        val wall = ByteArray(n)
        val darkAbs = when (gapSealLevel) {
            0 -> 95
            2 -> 128
            else -> 112
        }
        val adaptOffset = when (gapSealLevel) {
            0 -> 36
            2 -> 24
            else -> 30
        }
        for (i in 0 until n) {
            val thr = min(170, mean[i] - adaptOffset)
            if (gray[i] < darkAbs || gray[i] < thr) {
                wall[i] = 1
            }
        }

        // Estimate stroke thickness T from horizontal black run lengths
        val runs = ArrayList<Int>(512)
        val step = max(3, h / 180)
        var yScan = 3
        while (yScan < h) {
            var run = 0
            val row = yScan * w
            for (x in 0 until w) {
                if (wall[row + x] != 0.toByte()) {
                    run++
                } else {
                    if (run in 1 until 80) runs.add(run)
                    run = 0
                }
            }
            yScan += step
        }
        runs.sort()
        val lineT = max(2, if (runs.isNotEmpty()) runs[runs.size / 2] else 3)

        // Dilate wall mildly so hairline pinholes seal cleanly
        val dilateR = (lineT * if (gapSealLevel >= 2) 0.75f else if (gapSealLevel == 0) 0.35f else 0.55f)
            .roundToInt().coerceIn(1, 8)
        val dWall = distanceTransform(wall, w, h, target = 1)
        val w2 = ByteArray(n)
        for (i in 0 until n) {
            if (dWall[i] <= dilateR) w2[i] = 1
        }

        var seedIdx = seedYIn * w + seedXIn
        if (w2[seedIdx] != 0.toByte()) {
            val searchR = max(5, (lineT * 1.4f).roundToInt() + dilateR)
            var found = -1
            outer@ for (r in 1..searchR) {
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val nx = seedXIn + dx
                        val ny = seedYIn + dy
                        if (nx in 0 until w && ny in 0 until h && w2[ny * w + nx] == 0.toByte()) {
                            found = ny * w + nx
                            break@outer
                        }
                    }
                }
            }
            if (found < 0) return LfillOutput.WallHit
            seedIdx = found
        }

        val seen = ByteArray(n)
        val cap = (n * 0.56).toInt()
        val out = IntArray(cap + 8)
        var cnt = 0
        var head = 0
        out[cnt++] = seedIdx
        seen[seedIdx] = 1

        var x0 = w
        var y0 = h
        var x1 = 0
        var y1 = 0

        while (head < cnt) {
            val i = out[head++]
            val cx = i % w
            val cy = i / w
            if (cx < x0) x0 = cx
            if (cx > x1) x1 = cx
            if (cy < y0) y0 = cy
            if (cy > y1) y1 = cy
            if (cnt >= cap) return LfillOutput.TooBigOrOpen

            if (cx > 0 && seen[i - 1] == 0.toByte() && w2[i - 1] == 0.toByte()) {
                seen[i - 1] = 1; out[cnt++] = i - 1
            }
            if (cx + 1 < w && seen[i + 1] == 0.toByte() && w2[i + 1] == 0.toByte()) {
                seen[i + 1] = 1; out[cnt++] = i + 1
            }
            if (cy > 0 && seen[i - w] == 0.toByte() && w2[i - w] == 0.toByte()) {
                seen[i - w] = 1; out[cnt++] = i - w
            }
            if (cy + 1 < h && seen[i + w] == 0.toByte() && w2[i + w] == 0.toByte()) {
                seen[i + w] = 1; out[cnt++] = i + w
            }
        }

        if (cnt < 30) return LfillOutput.TooBigOrOpen
        val touches = booleanArrayOf(x0 <= 1, y0 <= 1, x1 >= w - 2, y1 >= h - 2)
        if (touches.count { it } >= 3) return LfillOutput.TooBigOrOpen

        val rOpen = max(2, (lineT * 0.85f).roundToInt())
        val rClose = max(6, lineT * 5)
        val growR = dilateR + lineT / 2
        val pad = max(rOpen, rClose) + growR + 4
        val bw = x1 - x0 + 1 + pad * 2
        val bh = y1 - y0 + 1 + pad * 2
        val ox = x0 - pad
        val oy = y0 - pad

        if (bw <= 4 || bh <= 4 || bw * bh > 2_600_000) return LfillOutput.TooBigOrOpen

        var m = ByteArray(bw * bh)
        for (k in 0 until cnt) {
            val idx = out[k]
            val gx = idx % w
            val gy = idx / w
            m[(gy - oy) * bw + (gx - ox)] = 1
        }

        m = morphCut(m, bw, bh, rOpen, (seedIdx % w) - ox, (seedIdx / w) - oy)
        m = fillInteriorHoles(m, bw, bh)

        // Absorb Dag digits/lettering touching borders without spilling outside borders
        val closed = morphClose(m, bw, bh, rClose)
        val freeMask = ByteArray(bw * bh)
        val lab = ByteArray(bw * bh)
        for (y in 0 until bh) {
            val gy = y + oy
            for (x in 0 until bw) {
                val gx = x + ox
                if (gx in 0 until w && gy in 0 until h && w2[gy * w + gx] == 0.toByte()) {
                    freeMask[y * bw + x] = 1
                }
            }
        }

        val maxPocketArea = (9 * lineT) * (9 * lineT)
        val st = IntArray(bw * bh)
        val comp = ArrayList<Int>(512)
        for (i0 in freeMask.indices) {
            if (freeMask[i0] == 0.toByte() || lab[i0] != 0.toByte()) continue
            var sp = 0
            st[sp++] = i0
            lab[i0] = 1
            comp.clear()
            var touchesBboxBorder = false
            while (sp > 0) {
                val cur = st[--sp]
                comp.add(cur)
                val px = cur % bw
                val py = cur / bw
                if (px == 0 || py == 0 || px == bw - 1 || py == bh - 1) touchesBboxBorder = true
                if (px > 0 && freeMask[cur - 1] != 0.toByte() && lab[cur - 1] == 0.toByte()) {
                    lab[cur - 1] = 1; st[sp++] = cur - 1
                }
                if (px + 1 < bw && freeMask[cur + 1] != 0.toByte() && lab[cur + 1] == 0.toByte()) {
                    lab[cur + 1] = 1; st[sp++] = cur + 1
                }
                if (py > 0 && freeMask[cur - bw] != 0.toByte() && lab[cur - bw] == 0.toByte()) {
                    lab[cur - bw] = 1; st[sp++] = cur - bw
                }
                if (py + 1 < bh && freeMask[cur + bw] != 0.toByte() && lab[cur + bw] == 0.toByte()) {
                    lab[cur + bw] = 1; st[sp++] = cur + bw
                }
            }
            if (!touchesBboxBorder && comp.size < maxPocketArea) {
                for (ci in comp) {
                    if (closed[ci] != 0.toByte()) m[ci] = 1
                }
            }
        }

        for (i in m.indices) {
            if (m[i] == 0.toByte() && closed[i] != 0.toByte() && freeMask[i] == 0.toByte()) {
                m[i] = 1
            }
        }
        m = fillInteriorHoles(m, bw, bh)

        // Expand slightly over inner half of plot wall stroke so vertices sit directly on centerline
        val dm = distanceTransform(m, bw, bh, target = 1)
        for (i in m.indices) {
            if (m[i] == 0.toByte() && freeMask[i] == 0.toByte() && dm[i] <= growR) {
                m[i] = 1
            }
        }

        var sumX = 0.0
        var sumY = 0.0
        var mass = 0
        for (y in 0 until bh) {
            for (x in 0 until bw) {
                if (m[y * bw + x] != 0.toByte()) {
                    sumX += x
                    sumY += y
                    mass++
                }
            }
        }
        if (mass < 25) return LfillOutput.TooBigOrOpen

        val rawContour = traceOuterContour(m, bw, bh)
        if (rawContour.size < 4) return LfillOutput.TooBigOrOpen

        val smoothWindow = max(2, (lineT * 0.5f).roundToInt())
        val len = rawContour.size
        val smoothed = ArrayList<Pair<Float, Float>>(len)
        for (i in 0 until len) {
            var ax = 0f
            var ay = 0f
            for (j in -smoothWindow..smoothWindow) {
                val p = rawContour[(i + j + len) % len]
                ax += p.first
                ay += p.second
            }
            val d = (2 * smoothWindow + 1).toFloat()
            smoothed.add((ax / d) to (ay / d))
        }

        // Split ring at furthest point from start and run Douglas-Peucker simplification
        var farIdx = 0
        var farDist = 0f
        val p0 = smoothed[0]
        for (i in smoothed.indices) {
            val dist = hypot(smoothed[i].first - p0.first, smoothed[i].second - p0.second)
            if (dist > farDist) {
                farDist = dist
                farIdx = i
            }
        }

        val eps = max(3.2f, 0.015f * sqrt(mass.toFloat()))
        val partA = douglasPeucker(smoothed.subList(0, farIdx + 1), eps)
        val partB = douglasPeucker(smoothed.subList(farIdx, smoothed.size) + p0, eps)
        val combined = ArrayList<Pair<Float, Float>>(partA.size + partB.size)
        for (i in 0 until max(0, partA.size - 1)) {
            combined.add((partA[i].first + ox + 0.5f) to (partA[i].second + oy + 0.5f))
        }
        for (i in 0 until max(0, partB.size - 1)) {
            combined.add((partB[i].first + ox + 0.5f) to (partB[i].second + oy + 0.5f))
        }
        if (combined.size < 3) return LfillOutput.TooBigOrOpen

        val cx = (sumX / mass).toFloat() + ox
        val cy = (sumY / mass).toFloat() + oy
        return LfillOutput.Success(combined, cx, cy, touches)
    }

    private fun distanceTransform(mask: ByteArray, bw: Int, bh: Int, target: Int): IntArray {
        val inf = 60_000
        val d = IntArray(bw * bh) { i -> if (mask[i].toInt() == target) 0 else inf }
        for (y in 0 until bh) {
            val row = y * bw
            for (x in 0 until bw) {
                val i = row + x
                var v = d[i]
                if (x > 0 && d[i - 1] + 1 < v) v = d[i - 1] + 1
                if (y > 0 && d[i - bw] + 1 < v) v = d[i - bw] + 1
                d[i] = v
            }
        }
        for (y in bh - 1 downTo 0) {
            val row = y * bw
            for (x in bw - 1 downTo 0) {
                val i = row + x
                var v = d[i]
                if (x + 1 < bw && d[i + 1] + 1 < v) v = d[i + 1] + 1
                if (y + 1 < bh && d[i + bw] + 1 < v) v = d[i + bw] + 1
                d[i] = v
            }
        }
        return d
    }

    private fun morphClose(m: ByteArray, bw: Int, bh: Int, r: Int): ByteArray {
        val d1 = distanceTransform(m, bw, bh, 1)
        val dilated = ByteArray(bw * bh) { i -> if (d1[i] <= r) 1 else 0 }
        val d2 = distanceTransform(dilated, bw, bh, 0)
        return ByteArray(bw * bh) { i -> if (d2[i] > r) 1 else 0 }
    }

    private fun morphCut(m: ByteArray, bw: Int, bh: Int, r: Int, sx: Int, sy: Int): ByteArray {
        var area0 = 0
        for (v in m) area0 += v.toInt()
        val d = distanceTransform(m, bw, bh, 0)
        val eroded = ByteArray(bw * bh) { i -> if (d[i] > r) 1 else 0 }

        var seed = sy.coerceIn(0, bh - 1) * bw + sx.coerceIn(0, bw - 1)
        if (eroded[seed] == 0.toByte()) {
            var best = Int.MAX_VALUE
            var found = -1
            for (dy in -r * 2..r * 2) {
                for (dx in -r * 2..r * 2) {
                    val nx = sx + dx
                    val ny = sy + dy
                    if (nx in 0 until bw && ny in 0 until bh && eroded[ny * bw + nx] != 0.toByte()) {
                        val q = dx * dx + dy * dy
                        if (q < best) {
                            best = q
                            found = ny * bw + nx
                        }
                    }
                }
            }
            if (found < 0) return m
            seed = found
        }

        val comp = ByteArray(bw * bh)
        val st = IntArray(bw * bh)
        var sp = 0
        st[sp++] = seed
        comp[seed] = 1
        while (sp > 0) {
            val i = st[--sp]
            val x = i % bw
            val y = i / bw
            if (x > 0 && eroded[i - 1] != 0.toByte() && comp[i - 1] == 0.toByte()) {
                comp[i - 1] = 1; st[sp++] = i - 1
            }
            if (x + 1 < bw && eroded[i + 1] != 0.toByte() && comp[i + 1] == 0.toByte()) {
                comp[i + 1] = 1; st[sp++] = i + 1
            }
            if (y > 0 && eroded[i - bw] != 0.toByte() && comp[i - bw] == 0.toByte()) {
                comp[i - bw] = 1; st[sp++] = i - bw
            }
            if (y + 1 < bh && eroded[i + bw] != 0.toByte() && comp[i + bw] == 0.toByte()) {
                comp[i + bw] = 1; st[sp++] = i + bw
            }
        }

        val d2 = distanceTransform(comp, bw, bh, 1)
        val out = ByteArray(bw * bh)
        var area1 = 0
        for (i in out.indices) {
            if (m[i] != 0.toByte() && d2[i] <= r) {
                out[i] = 1
                area1++
            }
        }
        return if (area1 >= area0 * 0.08) out else m
    }

    private fun fillInteriorHoles(m: ByteArray, bw: Int, bh: Int): ByteArray {
        val outside = ByteArray(bw * bh)
        val st = IntArray(bw * bh)
        var sp = 0
        outside[0] = 1
        st[sp++] = 0
        while (sp > 0) {
            val i = st[--sp]
            val x = i % bw
            val y = i / bw
            if (x > 0 && outside[i - 1] == 0.toByte() && m[i - 1] == 0.toByte()) {
                outside[i - 1] = 1; st[sp++] = i - 1
            }
            if (x + 1 < bw && outside[i + 1] == 0.toByte() && m[i + 1] == 0.toByte()) {
                outside[i + 1] = 1; st[sp++] = i + 1
            }
            if (y > 0 && outside[i - bw] == 0.toByte() && m[i - bw] == 0.toByte()) {
                outside[i - bw] = 1; st[sp++] = i - bw
            }
            if (y + 1 < bh && outside[i + bw] == 0.toByte() && m[i + bw] == 0.toByte()) {
                outside[i + bw] = 1; st[sp++] = i + bw
            }
        }
        return ByteArray(bw * bh) { i -> if (outside[i] != 0.toByte()) 0 else 1 }
    }

    private fun traceOuterContour(m: ByteArray, bw: Int, bh: Int): List<Pair<Float, Float>> {
        var st = -1
        for (i in m.indices) {
            if (m[i] != 0.toByte()) {
                st = i; break
            }
        }
        if (st < 0) return emptyList()

        val dirs = arrayOf(
            1 to 0, 1 to 1, 0 to 1, -1 to 1,
            -1 to 0, -1 to -1, 0 to -1, 1 to -1
        )
        val sx = st % bw
        val sy = st / bw
        fun get(a: Int, b: Int) = a in 0 until bw && b in 0 until bh && m[b * bw + a] != 0.toByte()

        var x = sx
        var y = sy
        var bDir = 4
        var steps = 0
        val pts = ArrayList<Pair<Float, Float>>(512)
        do {
            pts.add(x.toFloat() to y.toFloat())
            var k = 1
            while (k <= 8) {
                val d = (bDir + k) % 8
                if (get(x + dirs[d].first, y + dirs[d].second)) break
                k++
            }
            if (k > 8) break
            val d = (bDir + k) % 8
            val pd = (bDir + k - 1) % 8
            val nx = x + dirs[d].first
            val ny = y + dirs[d].second
            val qx = x + dirs[pd].first - nx
            val qy = y + dirs[pd].second - ny
            var nextB = 0
            for (idx in dirs.indices) {
                if (dirs[idx].first == qx && dirs[idx].second == qy) {
                    nextB = idx
                    break
                }
            }
            bDir = nextB
            x = nx
            y = ny
            steps++
        } while ((x != sx || y != sy) && steps < 300_000)
        return pts
    }

    private fun douglasPeucker(points: List<Pair<Float, Float>>, eps: Float): List<Pair<Float, Float>> {
        if (points.size < 3) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        val stack = ArrayList<IntArray>(64)
        stack.add(intArrayOf(0, points.lastIndex))
        while (stack.isNotEmpty()) {
            val pair = stack.removeAt(stack.lastIndex)
            val a = pair[0]
            val b = pair[1]
            var maxD = 0f
            var maxIdx = -1
            val pa = points[a]
            val pb = points[b]
            val dx = pb.first - pa.first
            val dy = pb.second - pa.second
            val len = hypot(dx, dy).coerceAtLeast(1e-5f)
            for (i in a + 1 until b) {
                val p = points[i]
                val d = abs((p.first - pa.first) * dy - (p.second - pa.second) * dx) / len
                if (d > maxD) {
                    maxD = d
                    maxIdx = i
                }
            }
            if (maxD > eps && maxIdx > a) {
                keep[maxIdx] = true
                stack.add(intArrayOf(a, maxIdx))
                stack.add(intArrayOf(maxIdx, b))
            }
        }
        return points.filterIndexed { idx, _ -> keep[idx] }
    }

    fun isPointInPolygon(x: Float, y: Float, poly: List<Pair<Float, Float>>): Boolean {
        if (poly.size < 3) return false
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val xi = poly[i].first
            val yi = poly[i].second
            val xj = poly[j].first
            val yj = poly[j].second
            val intersect = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (it == 0f) 1e-6f else it }) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun computePolygonAreaNormalized(poly: List<Pair<Float, Float>>): Double {
        if (poly.size < 3) return 0.0
        var sum = 0.0
        for (i in poly.indices) {
            val p1 = poly[i]
            val p2 = poly[(i + 1) % poly.size]
            sum += p1.first.toDouble() * p2.second.toDouble() - p2.first.toDouble() * p1.second.toDouble()
        }
        return abs(sum) / 2.0
    }

    fun computePolygonAreaSqFeet(
        polygonNorm: List<Pair<Float, Float>>,
        pageWidthPts: Int,
        pageHeightPts: Int,
        scale: MouzaScale,
        calibrationMultiplier: Double = 1.0
    ): Double {
        val pageInchesW = pageWidthPts / 72.0
        val pageInchesH = pageHeightPts / 72.0
        val pageFtW = pageInchesW * scale.feetPerInch
        val pageFtH = pageInchesH * scale.feetPerInch
        return computePolygonAreaNormalized(polygonNorm) * pageFtW * pageFtH * calibrationMultiplier
    }

    fun computeDistanceFeet(
        p1Norm: Pair<Float, Float>,
        p2Norm: Pair<Float, Float>,
        pageWidthPts: Int,
        pageHeightPts: Int,
        scale: MouzaScale,
        calibrationMultiplier: Double = 1.0
    ): Double {
        val pageFtW = (pageWidthPts / 72.0) * scale.feetPerInch * sqrt(calibrationMultiplier)
        val pageFtH = (pageHeightPts / 72.0) * scale.feetPerInch * sqrt(calibrationMultiplier)
        val dx = (p2Norm.first - p1Norm.first) * pageFtW
        val dy = (p2Norm.second - p1Norm.second) * pageFtH
        return hypot(dx, dy)
    }

    /**
     * Computes exact Sq.Feet per pixel in the detection grid for a given PDF page & Mouza scale.
     */
    fun computeSqFeetPerGridPixel(
        pdfPageWidthPts: Int,
        gridWidth: Int,
        scale: MouzaScale,
        customCalibrationMultiplier: Double = 1.0
    ): Double {
        val pdfInchesWidth = pdfPageWidthPts / 72.0
        val pixelsPerInch = gridWidth / pdfInchesWidth.coerceAtLeast(0.1)
        val sqInchesPerPixel = 1.0 / (pixelsPerInch * pixelsPerInch)
        val sqFeetPerSqInch = scale.feetPerInch * scale.feetPerInch
        return sqInchesPerPixel * sqFeetPerSqInch * customCalibrationMultiplier
    }

    suspend fun buildPlotWallIndex(
        sourceBitmap: Bitmap,
        gapSealLevel: Int
    ): PlotWallIndex = withContext(Dispatchers.Default) {
        val maxGridDim = 1500
        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height
        val scale = min(1f, maxGridDim.toFloat() / max(srcW, srcH).toFloat())
        val gw = (srcW * scale).toInt().coerceAtLeast(100)
        val gh = (srcH * scale).toInt().coerceAtLeast(100)

        val workBitmap = if (scale < 0.99f) {
            Bitmap.createScaledBitmap(sourceBitmap, gw, gh, true)
        } else {
            sourceBitmap
        }

        val pixels = IntArray(gw * gh)
        workBitmap.getPixels(pixels, 0, gw, 0, 0, gw, gh)
        if (workBitmap !== sourceBitmap) {
            workBitmap.recycle()
        }

        val total = gw * gh
        val wallMask = ByteArray(total)

        val inkThreshold = when (gapSealLevel) {
            0 -> 175
            2 -> 218
            else -> 198
        }

        for (i in 0 until total) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val luma = (r * 77 + g * 150 + b * 29) shr 8
            if (luma < inkThreshold) {
                wallMask[i] = 1
            }
        }

        val compVisited = ByteArray(total)
        val bfsQueue = IntArray(min(total, 260_000))
        val compPixels = IntArray(2048)
        val maxGlyphDim = (max(gw, gh) * 0.036f).toInt().coerceIn(22, 54)
        val maxGlyphPixels = maxGlyphDim * maxGlyphDim / 2

        for (i in 0 until total) {
            if (wallMask[i] == 1.toByte() && compVisited[i] == 0.toByte()) {
                var head = 0
                var tail = 0
                var count = 0
                var isLargeStructure = false

                compVisited[i] = 1
                bfsQueue[tail++] = i

                val sx = i % gw
                val sy = i / gw
                var minX = sx
                var maxX = sx
                var minY = sy
                var maxY = sy

                while (head < tail) {
                    val cur = bfsQueue[head++]
                    if (count < compPixels.size) {
                        compPixels[count] = cur
                    }
                    count++

                    val cx = cur % gw
                    val cy = cur / gw
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy

                    if (count > maxGlyphPixels || (maxX - minX) > maxGlyphDim || (maxY - minY) > maxGlyphDim) {
                        isLargeStructure = true
                    }

                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny !in 0 until gh) continue
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            if (nx !in 0 until gw) continue
                            val nIdx = ny * gw + nx
                            if (wallMask[nIdx] == 1.toByte() && compVisited[nIdx] == 0.toByte()) {
                                compVisited[nIdx] = 1
                                if (tail < bfsQueue.size) {
                                    bfsQueue[tail++] = nIdx
                                } else {
                                    isLargeStructure = true
                                }
                            }
                        }
                    }
                }

                if (!isLargeStructure && count <= compPixels.size) {
                    for (k in 0 until count) {
                        wallMask[compPixels[k]] = 0
                    }
                }
            }
        }

        if (gapSealLevel >= 1) {
            val copy = wallMask.clone()
            for (y in 1 until gh - 1) {
                val row = y * gw
                for (x in 1 until gw - 1) {
                    val idx = row + x
                    if (copy[idx] == 0.toByte()) {
                        val horiz = copy[idx - 1] == 1.toByte() && copy[idx + 1] == 1.toByte()
                        val vert = copy[idx - gw] == 1.toByte() && copy[idx + gw] == 1.toByte()
                        val diag1 = copy[idx - gw - 1] == 1.toByte() && copy[idx + gw + 1] == 1.toByte()
                        val diag2 = copy[idx - gw + 1] == 1.toByte() && copy[idx + gw - 1] == 1.toByte()
                        if (horiz || vert || diag1 || diag2) {
                            wallMask[idx] = 1
                        }
                    }
                }
            }
        }

        if (gapSealLevel >= 2) {
            val copy2 = wallMask.clone()
            for (y in 2 until gh - 2) {
                val row = y * gw
                for (x in 2 until gw - 2) {
                    val idx = row + x
                    if (copy2[idx] == 0.toByte()) {
                        val h2 = (copy2[idx - 1] == 1.toByte() || copy2[idx - 2] == 1.toByte()) &&
                            (copy2[idx + 1] == 1.toByte() || copy2[idx + 2] == 1.toByte())
                        val v2 = (copy2[idx - gw] == 1.toByte() || copy2[idx - 2 * gw] == 1.toByte()) &&
                            (copy2[idx + gw] == 1.toByte() || copy2[idx + 2 * gw] == 1.toByte())
                        if (h2 || v2) {
                            wallMask[idx] = 1
                        }
                    }
                }
            }
        }

        PlotWallIndex(gw, gh, wallMask)
    }

    fun buildManualPolygonPlot(
        normalizedPoints: List<Pair<Float, Float>>,
        gridWidth: Int,
        gridHeight: Int,
        plotId: Int,
        plotIndex: Int,
        color: ComposeColor
    ): MouzaPlot {
        var minX = gridWidth - 1
        var minY = gridHeight - 1
        var maxX = 0
        var maxY = 0

        val polyX = FloatArray(normalizedPoints.size)
        val polyY = FloatArray(normalizedPoints.size)
        normalizedPoints.forEachIndexed { idx, (nx, ny) ->
            val gx = (nx * gridWidth).coerceIn(0f, (gridWidth - 1).toFloat())
            val gy = (ny * gridHeight).coerceIn(0f, (gridHeight - 1).toFloat())
            polyX[idx] = gx
            polyY[idx] = gy
            minX = min(minX, gx.toInt())
            minY = min(minY, gy.toInt())
            maxX = max(maxX, gx.toInt())
            maxY = max(maxY, gy.toInt())
        }

        val indices = ArrayList<Int>(1024)
        val n = normalizedPoints.size
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                var inside = false
                var j = n - 1
                for (i in 0 until n) {
                    val xi = polyX[i]
                    val yi = polyY[i]
                    val xj = polyX[j]
                    val yj = polyY[j]
                    val intersect = ((yi > y) != (yj > y)) &&
                        (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (it == 0f) 0.0001f else it }) + xi)
                    if (intersect) inside = !inside
                    j = i
                }
                if (inside) {
                    indices.add(y * gridWidth + x)
                }
            }
        }

        val cx = normalizedPoints.map { it.first }.average().toFloat()
        val cy = normalizedPoints.map { it.second }.average().toFloat()
        val sorted = indices.toIntArray()
        return MouzaPlot(
            id = plotId,
            plotIndex = plotIndex,
            pixelCount = max(sorted.size, 1),
            maskIndices = sorted,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
            color = color,
            manualPolygonNormalized = normalizedPoints,
            polygonNormalized = normalizedPoints,
            centroidNormalized = cx to cy
        )
    }
}
