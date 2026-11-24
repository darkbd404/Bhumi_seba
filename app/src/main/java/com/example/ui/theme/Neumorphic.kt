package com.example.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft tactile Neumorphic surface modifier with dual top-left highlight & bottom-right shadow.
 */
fun Modifier.neuSurface(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    backgroundColor: Color? = null,
    borderColor: Color? = null
): Modifier = composed {
    val palette = LocalNeuPalette.current
    val bg = backgroundColor ?: palette.surface
    val shape = RoundedCornerShape(cornerRadius)

    this
        .drawBehind {
            val shadowRadius = elevation.toPx()
            val offset = (elevation * 0.65f).toPx()
            val cr = cornerRadius.toPx()

            drawIntoCanvas { canvas ->
                val lightPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            shadowRadius,
                            if (pressed) offset * 0.4f else -offset,
                            if (pressed) offset * 0.4f else -offset,
                            (if (pressed) palette.darkShadow else palette.lightShadow)
                                .copy(alpha = if (palette.isDark) 0.55f else 0.9f)
                                .toArgb()
                        )
                    }
                }
                val darkPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            shadowRadius,
                            if (pressed) -offset * 0.4f else offset,
                            if (pressed) -offset * 0.4f else offset,
                            (if (pressed) palette.lightShadow else palette.darkShadow)
                                .copy(alpha = if (palette.isDark) 0.85f else 0.65f)
                                .toArgb()
                        )
                    }
                }

                // Avoid BlurMaskFilter crashes by using standard setShadowLayer on rounded rect
                canvas.drawRoundRect(0f, 0f, size.width, size.height, cr, cr, lightPaint)
                canvas.drawRoundRect(0f, 0f, size.width, size.height, cr, cr, darkPaint)
            }
        }
        .clip(shape)
        .background(bg, shape)
        .border(
            width = 1.dp,
            color = borderColor ?: palette.lightShadow.copy(alpha = if (palette.isDark) 0.25f else 0.7f),
            shape = shape
        )
}

/**
 * Spring-animated Neumorphic clickable card modifier.
 */
@Composable
fun Modifier.neuClickable(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    backgroundColor: Color? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "neuScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .neuSurface(
            cornerRadius = cornerRadius,
            elevation = if (isPressed) elevation * 0.4f else elevation,
            pressed = isPressed,
            backgroundColor = backgroundColor
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
