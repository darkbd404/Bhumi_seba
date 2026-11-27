package com.example.ui.components

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Shared Vintage Aged Stamp-Paper / Antique Parchment Renderer (Canvas + Compose).
 * Used for Custom Deeds, Agreements, Contracts, Land Schedules, Historical Deed Reproductions,
 * and Custom Khatians both on-screen and inside PNG/PDF exports.
 */
object VintagePaperRenderer {
    const val PARCHMENT_TOP = 0xFFF7EBD0.toInt()
    const val PARCHMENT_MID = 0xFFF0DFB8.toInt()
    const val PARCHMENT_BOTTOM = 0xFFE6CE9C.toInt()
    const val INK_SEPIA_DARK = 0xFF2B1C10.toInt()
    const val INK_STAMP_GREEN = 0xFF174E3D.toInt()
    const val INK_BORDER_BROWN = 0xFF6B4C25.toInt()
    const val INK_RED_SEAL = 0xFF9C251C.toInt()

    fun drawAgedStampPaperBackground(
        canvas: AndroidCanvas,
        w: Int,
        h: Int,
        headerLabelBn: String = "বাংলাদেশ নন-জুডিশিয়াল স্ট্যাম্প / প্রাচীন দলিল কাগজ শৈলী",
        stampValueLabel: String = "১০০ টাকা (নমুনা)",
        serialTag: String = "নং- BD-REF-2026",
        showStampCrest: Boolean = true
    ): Float {
        // 1. Warm aged parchment vertical gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(PARCHMENT_TOP, PARCHMENT_MID, PARCHMENT_BOTTOM),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // 2. Deterministic paper fiber specks & soft aging tea-spots
        val fiberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(18, 122, 86, 40)
            style = Paint.Style.FILL
        }
        var seed = 1337L
        for (i in 0 until 260) {
            seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
            val fx = ((seed % 1000) / 1000f) * w
            seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
            val fy = ((seed % 1000) / 1000f) * h
            val r = 1.2f + (i % 5) * 0.85f
            canvas.drawCircle(fx, fy, r, fiberPaint)
        }
        // Subtle aged vignette along edges
        val edgeShadow = Paint().apply {
            color = AndroidColor.argb(24, 107, 76, 37)
            style = Paint.Style.STROKE
            strokeWidth = 34f
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), edgeShadow)

        // Subtle horizontal traditional fold-crease line at midpoint
        val creasePaint = Paint().apply {
            color = AndroidColor.argb(26, 92, 62, 28)
            strokeWidth = 3f
        }
        canvas.drawLine(28f, h * 0.5f, w - 28f, h * 0.5f, creasePaint)

        // 3. Double antique ornamental deed border
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_BORDER_BROWN
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_STAMP_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(24f, 24f, w - 24f, h - 24f, outerBorder)
        canvas.drawRect(34f, 34f, w - 34f, h - 34f, innerBorder)

        if (!showStampCrest) return 60f

        // 4. Top Stamp Crest Header Box (Traditional BD Non-Judicial Stamp Look)
        val crestTop = 48f
        val crestBottom = 174f
        val crestRect = RectF(52f, crestTop, w - 52f, crestBottom)
        val crestFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(28, 23, 78, 61)
        }
        canvas.drawRoundRect(crestRect, 12f, 12f, crestFill)
        canvas.drawRoundRect(crestRect, 12f, 12f, innerBorder)

        // Left & Right embossed seal medallions
        val sealStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_STAMP_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val sealLeftX = 116f
        val sealRightX = w - 116f
        val sealY = (crestTop + crestBottom) / 2f
        canvas.drawCircle(sealLeftX, sealY, 42f, sealStroke)
        canvas.drawCircle(sealLeftX, sealY, 34f, outerBorder)
        canvas.drawCircle(sealRightX, sealY, 42f, sealStroke)
        canvas.drawCircle(sealRightX, sealY, 34f, outerBorder)

        val sealText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_STAMP_GREEN
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("নমুনা", sealLeftX, sealY - 2f, sealText)
        canvas.drawText("স্ট্যাম্প", sealLeftX, sealY + 18f, sealText)
        canvas.drawText("খসড়া", sealRightX, sealY - 2f, sealText)
        canvas.drawText("দলিল", sealRightX, sealY + 18f, sealText)

        val hdrTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_STAMP_GREEN
            textSize = 27f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val stampBadgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_RED_SEAL
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val serialP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_SEPIA_DARK
            textSize = 17f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(headerLabelBn, w / 2f, crestTop + 42f, hdrTitle)
        canvas.drawText("মূল্যমান / ধরন: $stampValueLabel", w / 2f, crestTop + 76f, stampBadgeP)
        canvas.drawText("$serialTag  •  আনসার্টিফায়েড খসড়া/পুনর্গঠন (আসল সরকারি স্ট্যাম্প নয়)", w / 2f, crestTop + 108f, serialP)

        return crestBottom + 22f
    }
}

/**
 * Compose wrapper rendering Deeds, Khatians, Agreements, Schedules, and Custom Legal Papers
 * inside an authentic aged Bangladeshi stamp-paper / old parchment frame.
 */
@Composable
fun VintageStampPaperCard(
    headerTitle: String,
    stampBadge: String = "প্রাচীন কাগজ শৈলী (খসড়া)",
    serialNumber: String = "BD-DRAFT-2026",
    disclaimerText: String = "অস্বীকৃতি: এটি আনসার্টিফায়েড ডিজিটাল খসড়া/পুনর্গঠন; আসল সরকারি রেজিস্ট্রিকৃত দলিল নয়।",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val parchmentBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF7EBD0),
            Color(0xFFF0DFB8),
            Color(0xFFE7D09F)
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(parchmentBrush)
            .border(2.dp, Color(0xFF6B4C25), RoundedCornerShape(14.dp))
            .padding(5.dp)
            .border(1.dp, Color(0xFF174E3D), RoundedCornerShape(10.dp))
    ) {
        // Subtle aged paper spots and traditional mid-fold crease overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            val spotColor = Color(0x166B4C25)
            for (i in 0 until 28) {
                val x = ((i * 137 + 41) % 100) / 100f * size.width
                val y = ((i * 251 + 19) % 100) / 100f * size.height
                drawCircle(spotColor, radius = (2 + (i % 4)).toFloat(), center = Offset(x, y))
            }
            drawLine(
                color = Color(0x226B4C25),
                start = Offset(12f, size.height * 0.5f),
                end = Offset(size.width - 12f, size.height * 0.5f),
                strokeWidth = 2f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Traditional Stamp-Paper Top Crest
            Surface(
                color = Color(0x1A174E3D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF174E3D).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF174E3D),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stampBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C251C)
                        )
                        Text(
                            text = serialNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF3E2916)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF6B4C25).copy(alpha = 0.35f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFF9C251C).copy(alpha = 0.35f))
            Text(
                text = disclaimerText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8C1D18),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
