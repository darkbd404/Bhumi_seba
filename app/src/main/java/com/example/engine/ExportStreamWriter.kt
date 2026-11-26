package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Platform-agnostic OutputStream / FileOutputStream wrapper for saving PNG and PDF
 * exports reliably across Android (MediaStore Q+ and legacy FileOutputStream) and
 * Windows / PC / JVM environments.
 */
object ExportStreamWriter {

    fun writeToStream(
        outputStream: OutputStream,
        writeBlock: (OutputStream) -> Unit
    ) {
        BufferedOutputStream(outputStream).use { buffered ->
            writeBlock(buffered)
            buffered.flush()
        }
    }

    fun writeToFile(
        targetFile: File,
        writeBlock: (OutputStream) -> Unit
    ): File {
        targetFile.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        FileOutputStream(targetFile).use { fos ->
            writeToStream(fos, writeBlock)
        }
        return targetFile
    }

    fun encodeToByteArray(writeBlock: (OutputStream) -> Unit): ByteArray {
        val baos = ByteArrayOutputStream()
        writeToStream(baos, writeBlock)
        return baos.toByteArray()
    }

    fun resolveDesktopFallbackDir(
        subFolder: String = "BhumiSebaDigital",
        preferDocuments: Boolean = false
    ): File {
        val home = System.getProperty("user.home")?.takeIf { it.isNotBlank() } ?: "."
        val baseFolder = if (preferDocuments) "Documents" else "Pictures"
        return File(File(home, baseFolder), subFolder).apply { mkdirs() }
    }

    fun savePngBitmap(
        context: Context?,
        bitmap: Bitmap,
        fileName: String,
        subFolder: String = "BhumiSebaDigital",
        desktopFallbackDir: File? = null
    ): PngExporter.ExportResult {
        val w = bitmap.width
        val h = bitmap.height
        val safeName = if (fileName.endsWith(".png", ignoreCase = true)) fileName else "$fileName.png"

        if (desktopFallbackDir != null || context == null) {
            val dir = desktopFallbackDir ?: resolveDesktopFallbackDir(subFolder, preferDocuments = false)
            val file = writeToFile(File(dir, safeName)) { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            return PngExporter.ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = "${Environment.DIRECTORY_PICTURES}/$subFolder"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    writeToStream(out) { buffered ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, buffered)
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bitmap.recycle()
            PngExporter.ExportResult(uri, "Pictures/$subFolder/$safeName", w, h)
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, subFolder).apply { mkdirs() }
            val file = writeToFile(File(targetDir, safeName)) { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            PngExporter.ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
        }
    }

    fun savePdfBitmap(
        context: Context?,
        bitmap: Bitmap,
        fileName: String,
        subFolder: String = "BhumiSebaDigital",
        desktopFallbackDir: File? = null
    ): PngExporter.ExportResult {
        val w = bitmap.width
        val h = bitmap.height
        val safeName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"

        val nativePdfResult = runCatching {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(w, h, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            pdfDoc.finishPage(page)
            try {
                savePdfDocument(context, pdfDoc, w, h, safeName, subFolder, desktopFallbackDir)
            } finally {
                runCatching { pdfDoc.close() }
            }
        }.getOrNull()

        if (nativePdfResult != null) {
            val validDesktop = desktopFallbackDir == null ||
                (File(nativePdfResult.displayPath).let { it.exists() && it.length() > 16 })
            if (validDesktop) {
                bitmap.recycle()
                return nativePdfResult
            }
        }

        // Portable pure-Kotlin PDF 1.4 writer fallback (works on Windows/PC/JVM as well as Android)
        return try {
            val pdfBytes = encodeBitmapAsPortablePdfBytes(bitmap)
            if (desktopFallbackDir != null || context == null) {
                val dir = desktopFallbackDir ?: resolveDesktopFallbackDir(subFolder, preferDocuments = true)
                val file = writeToFile(File(dir, safeName)) { out -> out.write(pdfBytes) }
                PngExporter.ExportResult(Uri.fromFile(file), file.absolutePath, w, h)
            } else {
                val path = saveBinaryFileToDownloads(context, pdfBytes, safeName, "application/pdf", subFolder)
                PngExporter.ExportResult(null, path, w, h)
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    internal fun encodeBitmapAsPortablePdfBytes(bitmap: Bitmap): ByteArray {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val imgBytes = encodeToByteArray { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
        }
        val pageW = 842
        val pageH = ((842.0 * h) / w).toInt().coerceAtLeast(1)

        return encodeToByteArray { out ->
            val offsets = mutableListOf(0L)
            var pos = 0L
            fun writeAscii(s: String) {
                val b = s.toByteArray(Charsets.US_ASCII)
                out.write(b)
                pos += b.size
            }
            fun writeRaw(b: ByteArray) {
                out.write(b)
                pos += b.size
            }

            writeAscii("%PDF-1.4\n%BSD\n")
            offsets += pos
            writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            offsets += pos
            writeAscii("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
            offsets += pos
            writeAscii("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageW $pageH] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\nendobj\n")
            offsets += pos
            writeAscii("4 0 obj\n<< /Type /XObject /Subtype /Image /Width $w /Height $h /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${imgBytes.size} >>\nstream\n")
            writeRaw(imgBytes)
            writeAscii("\nendstream\nendobj\n")
            val contentStream = "q\n$pageW 0 0 $pageH 0 0 cm\n/Im0 Do\nQ\n"
            offsets += pos
            writeAscii("5 0 obj\n<< /Length ${contentStream.length} >>\nstream\n${contentStream}endstream\nendobj\n")

            val xrefPos = pos
            writeAscii("xref\n0 ${offsets.size}\n0000000000 65535 f \n")
            for (i in 1 until offsets.size) {
                writeAscii(offsets[i].toString().padStart(10, '0') + " 00000 n \n")
            }
            writeAscii("trailer\n<< /Size ${offsets.size} /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF")
        }
    }

    fun savePdfDocument(
        context: Context?,
        pdfDoc: PdfDocument,
        width: Int,
        height: Int,
        fileName: String,
        subFolder: String = "BhumiSebaDigital",
        desktopFallbackDir: File? = null
    ): PngExporter.ExportResult {
        val safeName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"

        if (desktopFallbackDir != null || context == null) {
            val dir = desktopFallbackDir ?: resolveDesktopFallbackDir(subFolder, preferDocuments = true)
            val file = writeToFile(File(dir, safeName)) { out ->
                pdfDoc.writeTo(out)
            }
            return PngExporter.ExportResult(Uri.fromFile(file), file.absolutePath, width, height)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$subFolder"
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, safeName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    writeToStream(out) { buffered ->
                        pdfDoc.writeTo(buffered)
                    }
                }
                values.clear()
                values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            PngExporter.ExportResult(uri, "Documents/$subFolder/$safeName", width, height)
        } else {
            @Suppress("DEPRECATION")
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val targetDir = File(docsDir, subFolder).apply { mkdirs() }
            val file = writeToFile(File(targetDir, safeName)) { out ->
                pdfDoc.writeTo(out)
            }
            PngExporter.ExportResult(Uri.fromFile(file), file.absolutePath, width, height)
        }
    }

    fun saveBinaryFileToDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        subFolder: String = "BhumiSebaDigital"
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$subFolder"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    writeToStream(out) { buffered -> buffered.write(bytes) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            "Downloads/$subFolder/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(dlDir, subFolder).apply { mkdirs() }
            val file = writeToFile(File(targetDir, fileName)) { out ->
                out.write(bytes)
            }
            file.absolutePath
        }
    }
}
