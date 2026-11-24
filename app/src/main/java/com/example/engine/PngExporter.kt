package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.MouzaPlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PngExporter {

    data class ExportResult(
        val uri: Uri?,
        val displayPath: String,
        val width: Int,
        val height: Int
    )

    /**
     * Exports ONLY the selected plots from the Ultra-HD Mouza PDF bitmap into a crisp PNG,
     * cropping tightly to the selected plots' bounding box and masking out unselected surroundings.
     */
    suspend fun exportSelectedMouzaPlotsPng(
        context: Context,
        sourceBitmap: Bitmap,
        plots: List<MouzaPlot>,
        sqFeetPerGridPixel: Double,
        khatianTitle: String
    ): ExportResult = withContext(Dispatchers.Default) {
        require(plots.isNotEmpty()) { "কোনো দাগ সিলেক্ট করা হয়নি।" }

        val first = plots.first()
        val gw = first.gridWidth
        val gh = first.gridHeight
        val scaleX = sourceBitmap.width.toFloat() / gw.toFloat()
        val scaleY = sourceBitmap.height.toFloat() / gh.toFloat()

        // Compute tight bounding box across all selected plots in sourceBitmap pixel coordinates
        var bMinX = Int.MAX_VALUE
        var bMinY = Int.MAX_VALUE
        var bMaxX = 0
        var bMaxY = 0

        for (p in plots) {
            if (p.manualPolygonNormalized != null && p.manualPolygonNormalized.isNotEmpty()) {
                for ((nx, ny) in p.manualPolygonNormalized) {
                    val px = (nx * sourceBitmap.width).toInt()
                    val py = (ny * sourceBitmap.height).toInt()
                    bMinX = min(bMinX, px)
                    bMinY = min(bMinY, py)
                    bMaxX = max(bMaxX, px)
                    bMaxY = max(bMaxY, py)
                }
            } else {
                bMinX = min(bMinX, (p.minX * scaleX).toInt())
                bMinY = min(bMinY, (p.minY * scaleY).toInt())
                bMaxX = max(bMaxX, ((p.maxX + 1) * scaleX).toInt())
                bMaxY = max(bMaxY, ((p.maxY + 1) * scaleY).toInt())
            }
        }

        val pad = 48
        val cropLeft = (bMinX - pad).coerceAtLeast(0)
        val cropTop = (bMinY - pad).coerceAtLeast(0)
        val cropRight = (bMaxX + pad).coerceAtMost(sourceBitmap.width)
        val cropBottom = (bMaxY + pad).coerceAtMost(sourceBitmap.height)

        val cropW = (cropRight - cropLeft).coerceAtLeast(240)
        val cropH = (cropBottom - cropTop).coerceAtLeast(200)

        // Ensure minimum comfortable export resolution so details & footer remain ultra-sharp
        val minExportWidth = 1080f
        val maxExportWidth = 2800f
        val zoomUp = when {
            cropW < minExportWidth -> (minExportWidth / cropW).coerceAtMost(3.5f)
            cropW > maxExportWidth -> maxExportWidth / cropW
            else -> 1f
        }

        val mapOutW = (cropW * zoomUp).toInt().coerceAtLeast(600)
        val mapOutH = (cropH * zoomUp).toInt().coerceAtLeast(420)
        val footerHeight = 170
        val outBitmap = Bitmap.createBitmap(mapOutW, mapOutH + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // Clean paper background
        canvas.drawColor(Color.WHITE)

        // Build exact mask bitmap in crop space so ONLY selected plot areas are copied from Ultra-HD source
        val maskBitmap = Bitmap.createBitmap(mapOutW, mapOutH, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
        }

        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Draw each plot into the mask
        val gridCellW = scaleX * zoomUp
        val gridCellH = scaleY * zoomUp

        for (plot in plots) {
            if (plot.manualPolygonNormalized != null && plot.manualPolygonNormalized.size >= 3) {
                val path = Path()
                plot.manualPolygonNormalized.forEachIndexed { idx, (nx, ny) ->
                    val px = (nx * sourceBitmap.width - cropLeft) * zoomUp
                    val py = (ny * sourceBitmap.height - cropTop) * zoomUp
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                maskCanvas.drawPath(path, maskPaint)
            } else {
                // Draw high-density mask cells expanded slightly (1.35x) so edges align smoothly
                val gwLocal = plot.gridWidth
                val expand = 0.35f
                for (idx in plot.maskIndices) {
                    val gx = idx % gwLocal
                    val gy = idx / gwLocal
                    val left = (gx * scaleX - cropLeft) * zoomUp - expand
                    val top = (gy * scaleY - cropTop) * zoomUp - expand
                    maskCanvas.drawRect(
                        left,
                        top,
                        left + gridCellW + expand * 2,
                        top + gridCellH + expand * 2,
                        maskPaint
                    )
                }
            }
        }

        // Extract cropped Ultra-HD PDF region masked exclusively to selected plots
        val layerBitmap = Bitmap.createBitmap(mapOutW, mapOutH, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)
        val srcRect = Rect(cropLeft, cropTop, cropRight, cropBottom)
        val dstRect = RectF(0f, 0f, mapOutW.toFloat(), mapOutH.toFloat())
        layerCanvas.drawBitmap(sourceBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        layerCanvas.drawBitmap(maskBitmap, 0f, 0f, xferPaint)
        maskBitmap.recycle()

        // Draw soft tinted highlight and crisp plot border over the isolated selected region
        canvas.drawBitmap(layerBitmap, 0f, 0f, null)
        layerBitmap.recycle()

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (plot in plots) {
            val argb = android.graphics.Color.argb(
                (plot.color.alpha * 255).toInt(),
                (plot.color.red * 255).toInt(),
                (plot.color.green * 255).toInt(),
                (plot.color.blue * 255).toInt()
            )
            tintPaint.color = android.graphics.Color.argb(44, Color.red(argb), Color.green(argb), Color.blue(argb))
            borderPaint.color = argb

            var centerX = 0f
            var centerY = 0f

            if (plot.manualPolygonNormalized != null && plot.manualPolygonNormalized.size >= 3) {
                val path = Path()
                var sumX = 0f
                var sumY = 0f
                plot.manualPolygonNormalized.forEachIndexed { idx, (nx, ny) ->
                    val px = (nx * sourceBitmap.width - cropLeft) * zoomUp
                    val py = (ny * sourceBitmap.height - cropTop) * zoomUp
                    sumX += px
                    sumY += py
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, tintPaint)
                canvas.drawPath(path, borderPaint)
                centerX = sumX / plot.manualPolygonNormalized.size
                centerY = sumY / plot.manualPolygonNormalized.size
            } else {
                centerX = (((plot.minX + plot.maxX) * 0.5f * scaleX) - cropLeft) * zoomUp
                centerY = (((plot.minY + plot.maxY) * 0.5f * scaleY) - cropTop) * zoomUp
            }

            // Plot summary tag near center
            val area = LandCalculators.fromSqFeet(plot.pixelCount * sqFeetPerGridPixel)
            val tag = buildString {
                if (plot.dagNumber.isNotBlank()) append("দাগ ${plot.dagNumber} • ")
                else append("#${plot.plotIndex} • ")
                append(String.format(Locale.US, "%.2f শতক", area.decimal))
            }
            val tw = labelPaint.measureText(tag)
            pillPaint.color = Color.argb(215, Color.red(argb), Color.green(argb), Color.blue(argb))
            val pillRect = RectF(centerX - tw / 2f - 16f, centerY - 24f, centerX + tw / 2f + 16f, centerY + 14f)
            canvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
            canvas.drawText(tag, centerX, centerY + 3f, labelPaint)
        }

        // Draw clean Survey Summary Footer
        val totalSqFt = plots.sumOf { it.pixelCount * sqFeetPerGridPixel }
        val totalArea = LandCalculators.fromSqFeet(totalSqFt)
        drawFooterStamp(
            canvas = canvas,
            width = mapOutW,
            topY = mapOutH.toFloat(),
            footerHeight = footerHeight.toFloat(),
            headerText = if (khatianTitle.isNotBlank()) "Bhumi Survey BD • $khatianTitle" else "Bhumi Survey BD • Selected Mouza Plots (${plots.size})",
            line1 = String.format(
                Locale.US,
                "মোট: %.2f শতাংশ (Decimal)  |  %.2f কাঠা  |  %.3f বিঘা",
                totalArea.decimal, totalArea.katha, totalArea.bigha
            ),
            line2 = String.format(
                Locale.US,
                "%.1f বর্গফুট (Sq.Ft)  |  %.1f বর্গমিটার (Sq.M)  |  %.4f একর",
                totalArea.sqFeet, totalArea.sqMeters, totalArea.acre
            )
        )

        val fileName = "Mouza_Selected_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    /**
     * Exports ONLY the selected measured region from the Real Map WebView canvas,
     * cropped strictly around the measured polygon/line bounding box with crisp survey footer.
     */
    suspend fun exportSelectedMapRegionPng(
        context: Context,
        fullMapBitmap: Bitmap,
        cropRectNormalized: RectF?, // 0f..1f bounding box of selected polygon on screen
        summaryHeader: String,
        summaryLine1: String,
        summaryLine2: String
    ): ExportResult = withContext(Dispatchers.Default) {
        val bw = fullMapBitmap.width
        val bh = fullMapBitmap.height

        val cropRect = if (cropRectNormalized != null) {
            val left = (cropRectNormalized.left * bw).toInt().coerceIn(0, bw - 10)
            val top = (cropRectNormalized.top * bh).toInt().coerceIn(0, bh - 10)
            val right = (cropRectNormalized.right * bw).toInt().coerceIn(left + 10, bw)
            val bottom = (cropRectNormalized.bottom * bh).toInt().coerceIn(top + 10, bh)
            Rect(left, top, right, bottom)
        } else {
            Rect(0, 0, bw, bh)
        }

        val cw = cropRect.width().coerceAtLeast(320)
        val ch = cropRect.height().coerceAtLeast(240)
        val scale = if (cw < 1080) (1080f / cw).coerceAtMost(2.5f) else 1f
        val outW = (cw * scale).toInt()
        val outH = (ch * scale).toInt()
        val footerHeight = 165

        val outBitmap = Bitmap.createBitmap(outW, outH + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            fullMapBitmap,
            cropRect,
            RectF(0f, 0f, outW.toFloat(), outH.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        drawFooterStamp(
            canvas = canvas,
            width = outW,
            topY = outH.toFloat(),
            footerHeight = footerHeight.toFloat(),
            headerText = summaryHeader,
            line1 = summaryLine1,
            line2 = summaryLine2
        )

        val fileName = "Map_Survey_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        saveBitmapToPictures(context, outBitmap, fileName)
    }

    private fun drawFooterStamp(
        canvas: Canvas,
        width: Int,
        topY: Float,
        footerHeight: Float,
        headerText: String,
        line1: String,
        line2: String
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#0F6B54") }
        canvas.drawRect(0f, topY, width.toFloat(), topY + footerHeight, bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAD165")
            textSize = (width * 0.028f).coerceIn(24f, 36f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (width * 0.024f).coerceIn(20f, 30f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8F3EA")
            textSize = (width * 0.020f).coerceIn(18f, 25f)
        }

        val padX = 28f
        canvas.drawText(headerText, padX, topY + 46f, titlePaint)
        canvas.drawText(line1, padX, topY + 95f, bodyPaint)
        canvas.drawText(line2, padX, topY + 138f, subPaint)
    }

    private fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String): ExportResult {
        val w = bitmap.width
        val h = bitmap.height
        val relativePath = "${Environment.DIRECTORY_PICTURES}/BhumiSurveyBD"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bitmap.recycle()
            ExportResult(uri, "Pictures/BhumiSurveyBD/$fileName", w, h)
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, "BhumiSurveyBD").apply { mkdirs() }
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
        }
    }
}
