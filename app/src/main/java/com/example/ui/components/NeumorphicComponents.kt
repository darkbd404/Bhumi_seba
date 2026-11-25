package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnimatedCompassBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 46
) {
    val infinite = rememberInfiniteTransition(label = "compassPulse")
    val angle by infinite.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compassAngle"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compassScale"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .neuSurface(cornerRadius = (sizeDp / 2).dp, elevation = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((sizeDp - 10).dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Explore,
                contentDescription = "Survey Compass",
                tint = Color.White,
                modifier = Modifier
                    .size((sizeDp * 0.52f).dp)
                    .rotate(angle)
            )
        }
    }
}

@Composable
fun NeuFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badge: String? = null,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .testTag(testTag)
            .neuClickable(cornerRadius = 22.dp, elevation = 6.dp, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .neuSurface(
                        cornerRadius = 15.dp,
                        elevation = 4.dp,
                        backgroundColor = accentColor.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(25.dp)
                )
            }

            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NeuPillChip(
    text: String,
    selected: Boolean,
    icon: ImageVector? = null,
    testTag: String = "",
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale"
    )
    val palette = LocalNeuPalette.current
    val bg = if (selected) MaterialTheme.colorScheme.primary else palette.surface
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .neuClickable(
                cornerRadius = 16.dp,
                elevation = if (selected) 2.dp else 5.dp,
                backgroundColor = bg,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalNeuPalette.current.surface,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .neuSurface(
                        cornerRadius = 28.dp,
                        elevation = 4.dp,
                        backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.exit_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.exit_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                modifier = Modifier.testTag("confirm_exit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.exit_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_exit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.exit_cancel))
            }
        }
    )
}

@Composable
fun BottomSwipeToastBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            delay(2200)
            onDismiss()
        }
    }

    var dragX by remember(message) { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 },
        modifier = modifier
    ) {
        val text = message.orEmpty()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset { IntOffset(dragX.roundToInt(), 0) }
                .pointerInput(text) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragX += dragAmount
                            if (abs(dragX) > 95f) {
                                onDismiss()
                            }
                        },
                        onDragEnd = {
                            if (abs(dragX) > 70f) onDismiss() else dragX = 0f
                        },
                        onDragCancel = { dragX = 0f }
                    )
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xEE18262E))
                .clickable { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color(0xFF4CE0A8),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = Color(0xFFB0BEC5),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PrecisionNudgePad(
    targetIsPoint: Boolean,
    hasAnyPoint: Boolean,
    activePointLabel: String,
    stepLevel: Int,
    onStepLevelChange: (Int) -> Unit,
    onToggleTargetMode: (Boolean) -> Unit,
    onCyclePoint: (Int) -> Unit,
    onNudge: (dxPx: Float, dyPx: Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepPx = when (stepLevel) {
        0 -> 1.6f
        2 -> 14.0f
        else -> 5.0f
    }
    val stepLabel = when (stepLevel) {
        0 -> "সূক্ষ্ম"
        2 -> "বেশি"
        else -> "মাঝারি"
    }

    Column(
        modifier = modifier
            .width(178.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE8132028))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: target mode switch + close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!targetIsPoint) Color(0xFF0F6B54) else Color.Transparent)
                        .clickable { onToggleTargetMode(false) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "পয়েন্টার",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (targetIsPoint && hasAnyPoint) Color(0xFFE08A00) else Color.Transparent
                        )
                        .clickable(enabled = hasAnyPoint) { onToggleTargetMode(true) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "বিন্দু",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasAnyPoint) Color.White else Color.White.copy(alpha = 0.38f),
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Nudge Pad",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Point switcher (< বিন্দু #2 >) when points exist
        if (hasAnyPoint) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x28FFFFFF))
                        .clickable { onCyclePoint(-1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = activePointLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (targetIsPoint) Color(0xFFFFD54F) else Color(0xFF80CBC4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x28FFFFFF))
                        .clickable { onCyclePoint(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("›", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4-way D-Pad (Supports quick tap AND continuous hold)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NudgeDirButton(
                symbol = "▲",
                desc = "Move Up",
                testTag = "nudge_up_btn",
                onTrigger = { onNudge(0f, -stepPx) }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NudgeDirButton(
                    symbol = "◀",
                    desc = "Move Left",
                    testTag = "nudge_left_btn",
                    onTrigger = { onNudge(-stepPx, 0f) }
                )
                // Center step-size toggle button
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E3540))
                        .clickable { onStepLevelChange((stepLevel + 1) % 3) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFE082),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                NudgeDirButton(
                    symbol = "▶",
                    desc = "Move Right",
                    testTag = "nudge_right_btn",
                    onTrigger = { onNudge(stepPx, 0f) }
                )
            }
            NudgeDirButton(
                symbol = "▼",
                desc = "Move Down",
                testTag = "nudge_down_btn",
                onTrigger = { onNudge(0f, stepPx) }
            )
        }
    }
}

@Composable
private fun NudgeDirButton(
    symbol: String,
    desc: String,
    testTag: String,
    onTrigger: () -> Unit
) {
    val latestTrigger by androidx.compose.runtime.rememberUpdatedState(onTrigger)
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 38.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x38FFFFFF))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
                        down.consume()
                        latestTrigger()
                        var elapsed = 0L
                        var nextFire = 260L
                        while (true) {
                            val res = withTimeoutOrNull(45L) { awaitPointerEvent() }
                            if (res != null) {
                                val stillPressed = res.changes.any { it.pressed }
                                res.changes.forEach { it.consume() }
                                if (!stillPressed) break
                            } else {
                                elapsed += 45L
                                if (elapsed >= nextFire) {
                                    latestTrigger()
                                    nextFire += 65L
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

