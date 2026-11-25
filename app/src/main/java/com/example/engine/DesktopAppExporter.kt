package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.SurveyorProfile
import java.io.File
import java.io.FileOutputStream

object DesktopAppExporter {

    data class DesktopExportResult(
        val uri: Uri?,
        val displayPath: String,
        val fileName: String
    )

    fun saveStandalonePcHtmlApp(
        context: Context,
        surveyorProfile: SurveyorProfile = SurveyorProfile()
    ): DesktopExportResult {
        val htmlContent = buildStandaloneDesktopHtml(context, surveyorProfile)
        val bytes = htmlContent.toByteArray(Charsets.UTF_8)
        val fileName = "BhumiSebaDigital_PC.html"
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/BhumiSebaDigital"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/html")
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            DesktopExportResult(uri, "Downloads/BhumiSebaDigital/$fileName", fileName)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadsDir, "BhumiSebaDigital").apply { mkdirs() }
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            DesktopExportResult(Uri.fromFile(file), file.absolutePath, fileName)
        }
    }

    fun buildStandaloneDesktopHtml(
        context: Context,
        profile: SurveyorProfile = SurveyorProfile()
    ): String {
        val template = runCatching {
            context.assets.open("pc_app.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse { buildStandaloneDesktopHtml() }
        val iconDataUrl = encodeDrawableDataUrl(context, com.example.R.drawable.ic_launcher_art, maxDim = 160, asJpeg = false, quality = 90)
        val avatarDataUrl = encodeSurveyorAvatarDataUrl(context, profile.photoUri)
        return injectProfilePlaceholders(template, profile, iconDataUrl, avatarDataUrl)
    }

    internal fun buildStandaloneDesktopHtml(): String {
        val template = runCatching {
            DesktopAppExporter::class.java.classLoader
                ?.getResourceAsStream("pc_app.html")
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
        }.getOrNull() ?: runCatching {
            File("src/main/assets/pc_app.html").takeIf { it.exists() }?.readText(Charsets.UTF_8)
        }.getOrNull() ?: runCatching {
            File("app/src/main/assets/pc_app.html").takeIf { it.exists() }?.readText(Charsets.UTF_8)
        }.getOrNull()

        if (!template.isNullOrBlank()) {
            return injectProfilePlaceholders(template, SurveyorProfile(), "", "")
        }

        return """
            <!DOCTYPE html>
            <html lang="bn">
            <head><meta charset="UTF-8"><title>ভূমি সেবা ডিজিটাল — PC ভার্সন</title>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script></head>
            <body><h1>ভূমি সেবা ডিজিটাল — PC ভার্সন</h1></body></html>
        """.trimIndent()
    }

    private fun encodeSurveyorAvatarDataUrl(context: Context, photoUri: String?): String {
        if (!photoUri.isNullOrBlank()) {
            val fromUri = runCatching {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { ins ->
                    android.graphics.BitmapFactory.decodeStream(ins)
                }?.let { bmp ->
                    bitmapToDataUrl(bmp, maxDim = 240, asJpeg = true, quality = 86)
                }
            }.getOrNull()
            if (!fromUri.isNullOrBlank()) return fromUri
        }
        return encodeDrawableDataUrl(context, com.example.R.drawable.img_dev_avatar, maxDim = 240, asJpeg = true, quality = 86)
    }

    private fun encodeDrawableDataUrl(
        context: Context,
        resId: Int,
        maxDim: Int,
        asJpeg: Boolean,
        quality: Int
    ): String {
        return runCatching {
            val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId) ?: return ""
            bitmapToDataUrl(bmp, maxDim, asJpeg, quality)
        }.getOrDefault("")
    }

    private fun bitmapToDataUrl(
        raw: android.graphics.Bitmap,
        maxDim: Int,
        asJpeg: Boolean,
        quality: Int
    ): String {
        val scaled = if (raw.width > maxDim || raw.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
            android.graphics.Bitmap.createScaledBitmap(
                raw,
                (raw.width * ratio).toInt().coerceAtLeast(1),
                (raw.height * ratio).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== raw) raw.recycle() }
        } else raw
        val bytes = java.io.ByteArrayOutputStream().use { out ->
            scaled.compress(
                if (asJpeg) android.graphics.Bitmap.CompressFormat.JPEG else android.graphics.Bitmap.CompressFormat.PNG,
                quality,
                out
            )
            scaled.recycle()
            out.toByteArray()
        }
        val mime = if (asJpeg) "image/jpeg" else "image/png"
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:$mime;base64,$b64"
    }

    private fun injectProfilePlaceholders(
        html: String,
        profile: SurveyorProfile,
        iconDataUrl: String,
        avatarDataUrl: String
    ): String {
        fun esc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
        return html
            .replace("__SURVEYOR_NAME__", esc(profile.name.ifBlank { "Abdus Salam" }))
            .replace("__SURVEYOR_PHONE__", esc(profile.phone.ifBlank { "01620230864" }))
            .replace("__SURVEYOR_EMAIL__", esc(profile.email.ifBlank { "salam230864@gmail.com" }))
            .replace("__SURVEYOR_DESIGNATION__", esc(profile.designation.ifBlank { "ডিজিটাল সার্ভেয়ার • ভূমি সেবা ডিজিটাল" }))
            .replace("__APP_ICON_DATA_URL__", iconDataUrl)
            .replace("__SURVEYOR_AVATAR_DATA_URL__", avatarDataUrl)
    }
}
