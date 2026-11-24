package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

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
                    "দাগের সীমানা খোলা বা বাইরের অংশে ট্যাপ হয়েছে। 'গ্যাপ সিল' বাড়িয়ে আবার চেষ্টা করুন অথবা 'ম্যানুয়াল পলিগন' ব্যবহার করুন।"
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

            // 4-way neighbor expansion
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
        // within the plot's bounding box so the selection is 100% solid and clean!
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

            // Anything inside bbox that is NOT reachable from bbox exterior is an interior hole -> absorb!
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

    suspend fun copyPdfToCache(context: Context, uri: Uri, fileName: String): PdfDocumentInfo =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "active_mouza_${System.currentTimeMillis()}.pdf")
            // Clean older cached PDFs
            context.cacheDir.listFiles { f -> f.name.startsWith("active_mouza_") }?.forEach {
                runCatching { it.delete() }
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output, bufferSize = 65536)
                }
            } ?: error("PDF ফাইলটি পড়া যায়নি।")

            val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()

            PdfDocumentInfo(
                tempFile = cacheFile,
                pageCount = count.coerceAtLeast(1),
                fileName = fileName
            )
        }

    /**
     * Renders a PDF page either as an instant preview (~1100px) or Ultra-HD Master (up to 4096px).
     */
    suspend fun renderPdfPage(
        context: Context,
        pdfFile: File,
        pageIndex: Int,
        ultraHd: Boolean
    ): RenderedPageResult = withContext(Dispatchers.Default) {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val safePageIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
        val page = renderer.openPage(safePageIdx)

        val pagePtsW = page.width
        val pagePtsH = page.height

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true
        val targetMaxDim = if (ultraHd) {
            if (isLowRam) 2800 else 3840
        } else {
            1100
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
            if (ultraHd) PdfRenderer.Page.RENDER_MODE_FOR_PRINT else PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
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

    /**
     * Builds the Precision Mouza Wall Index:
     * 1. Extracts dark lines via luminance thresholding.
     * 2. Identifies small isolated dark connected components (Dag numbers, symbols, specks)
     *    and strips them from the wall mask so plot flood-fill covers the whole plot cleanly!
     * 3. Bridges 1px-2px broken boundary gaps without inflating plot walls.
     */
    suspend fun buildPlotWallIndex(
        sourceBitmap: Bitmap,
        gapSealLevel: Int // 0 = Exact, 1 = Balanced (default), 2 = Strong
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

        // Step 1: Luminance thresholding for Mouza map ink
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

        // Step 2: Connected-component filtering to remove isolated Dag Numbers & interior text
        // Plot boundary lines form large interconnected networks; Dag numbers are small isolated glyphs.
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

                    // 8-connected check so slightly diagonal digits stay grouped together
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

                // If this dark component is small & isolated, it's a Dag number or dot -> strip from wallMask!
                if (!isLargeStructure && count <= compPixels.size) {
                    for (k in 0 until count) {
                        wallMask[compPixels[k]] = 0
                    }
                }
            }
        }

        // Step 3: Opposite-neighbor gap bridging (seals 1px or 2px breaks in borders without thickening lines)
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
            // Second pass: seal 2px gaps along horizontal/vertical/diagonal directions
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

    /**
     * Builds a Manual Polygon MouzaPlot using ray-casting point-in-polygon over the grid
     * so manual polygon selections also measure exact area and export identically as PNG!
     */
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
            manualPolygonNormalized = normalizedPoints
        )
    }
}
