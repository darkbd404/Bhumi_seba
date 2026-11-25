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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.engine.DesktopAppExporter
import com.example.engine.LocalPcWebServer
import com.example.model.SurveyorProfile
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
    val infinite = rememberInfiniteTransition(label = "brandPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brandScale"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .neuSurface(cornerRadius = 14.dp, elevation = 5.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_art),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )
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
    // Level 0 = ultra-fine 1% precision (0.06 screen px) so e.g. 27.11 -> 27.90 moves smoothly unit-by-unit
    val stepPx = when (stepLevel) {
        0 -> 0.06f
        1 -> 0.35f
        2 -> 2.0f
        else -> 8.0f
    }
    val stepLabel = when (stepLevel) {
        0 -> "১% অতি সূক্ষ্ম"
        1 -> "সূক্ষ্ম (১px)"
        2 -> "মাঝারি (৪px)"
        else -> "বেশি (১২px)"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF215232C))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Target Switch (পয়েন্টার / বিন্দু) + Point Cycler + Step Precision Selector + Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x28FFFFFF))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!targetIsPoint) Color(0xFF0F6B54) else Color.Transparent)
                        .clickable { onToggleTargetMode(false) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (targetIsPoint && hasAnyPoint) Color(0xFFE08A00) else Color.Transparent
                        )
                        .clickable(enabled = hasAnyPoint) { onToggleTargetMode(true) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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

            if (hasAnyPoint) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
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
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Step precision cycle pill
            Box(
                modifier = Modifier
                    .testTag("nudge_step_toggle")
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E3A46))
                    .clickable { onStepLevelChange((stepLevel + 1) % 4) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
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

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x28FFFFFF))
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

        // Row 2: Dedicated horizontal D-Pad strip (placed below the map so the map stays 100% clear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NudgeDirButton(
                symbol = "◀ বাম",
                desc = "Move Left",
                testTag = "nudge_left_btn",
                modifier = Modifier.weight(1f),
                onTrigger = { onNudge(-stepPx, 0f) }
            )
            NudgeDirButton(
                symbol = "▲ উপর",
                desc = "Move Up",
                testTag = "nudge_up_btn",
                modifier = Modifier.weight(1f),
                onTrigger = { onNudge(0f, -stepPx) }
            )
            NudgeDirButton(
                symbol = "▼ নিচ",
                desc = "Move Down",
                testTag = "nudge_down_btn",
                modifier = Modifier.weight(1f),
                onTrigger = { onNudge(0f, stepPx) }
            )
            NudgeDirButton(
                symbol = "ডান ▶",
                desc = "Move Right",
                testTag = "nudge_right_btn",
                modifier = Modifier.weight(1f),
                onTrigger = { onNudge(stepPx, 0f) }
            )
        }
    }
}

@Composable
private fun NudgeDirButton(
    symbol: String,
    desc: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onTrigger: () -> Unit
) {
    val latestTrigger by androidx.compose.runtime.rememberUpdatedState(onTrigger)
    Box(
        modifier = modifier
            .height(38.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x36FFFFFF))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
                        down.consume()
                        latestTrigger()
                        var elapsed = 0L
                        var nextFire = 280L
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
                                    nextFire += 75L
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun PcInstallLinkCard(
    surveyorProfile: SurveyorProfile = SurveyorProfile(),
    onPreviewPcMode: (String) -> Unit = {},
    onCopySuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val serverState by LocalPcWebServer.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .neuSurface(cornerRadius = 22.dp, elevation = 6.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A73E8).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.Computer,
                    contentDescription = "PC / Laptop Version",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ভূমি সেবা ডিজিটাল — একক সিস্টেম (PC ও Android)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Android-এর হুবহু একই ডিজাইন ও সব ফিচার (মৌজা, স্যাটেলাইট, কাস্টম খতিয়ান, ২২ পোর্টাল, ৭ ক্যালকুলেটর) PC ব্রাউজারে চালাতে সার্ভার চালু করুন বা অফলাইন .html ডাউনলোড করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Method 1: Live Phone Wi-Fi / Hotspot Server for PC Browser (Real working local HTTP server)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (serverState.isRunning) {
                        LocalPcWebServer.stop()
                        onCopySuccess("PC লাইভ সার্ভার বন্ধ হয়েছে")
                    } else {
                        LocalPcWebServer.start(context, surveyorProfile)
                        onCopySuccess("PC লাইভ সার্ভার চালু হচ্ছে...")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serverState.isRunning) Color(0xFFC93B52) else Color(0xFF0F6B54)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pc_server_toggle_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (serverState.isRunning) androidx.compose.material.icons.Icons.Rounded.Stop
                    else androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (serverState.isRunning) "PC সার্ভার চালু আছে (বন্ধ করুন)"
                    else "PC লাইভ Wi-Fi সার্ভার চালু করুন",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (serverState.isRunning) {
                IconButton(
                    onClick = {
                        LocalPcWebServer.refreshNetworkUrl()
                        onCopySuccess("Wi-Fi / Hotspot IP আপডেট হয়েছে")
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Refresh,
                        contentDescription = "Refresh IP"
                    )
                }
            }
        }

        if (serverState.isRunning && serverState.lanUrl.isNotBlank()) {
            Surface(
                color = Color(0xFF0F6B54).copy(alpha = 0.09f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = Color(0xFF0F6B54),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = serverState.lanUrl,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(serverState.lanUrl))
                                    onCopySuccess("PC সার্ভার লিংক কপি হয়েছে!")
                                }
                        )
                    }

                    Text(
                        text = serverState.networkHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(serverState.lanUrl))
                                onCopySuccess("PC সার্ভার লিংক কপি হয়েছে!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_link_copy_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("কপি", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "ভূমি সেবা ডিজিটাল — PC লাইভ সার্ভার")
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "কম্পিউটারের Chrome/Edge ব্রাউজারে এই ঠিকানা লিখে খুলুন (একই Wi-Fi বা Hotspot):\n${serverState.lanUrl}"
                                    )
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(shareIntent, "PC সার্ভার লিংক শেয়ার করুন")
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_link_share_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("শেয়ার", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                if (serverState.loopbackUrl.isNotBlank()) {
                                    onPreviewPcMode(serverState.loopbackUrl)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_preview_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.OpenInBrowser,
                                contentDescription = "Preview PC Version",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("প্রিভিউ", maxLines = 1)
                        }
                    }
                }
            }
        }

        if (!serverState.errorMessage.isNullOrBlank()) {
            Text(
                text = serverState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Method 2: Guaranteed 100% Offline Standalone PC Software File (.html)
        OutlinedButton(
            onClick = {
                runCatching {
                    val res = DesktopAppExporter.saveStandalonePcHtmlApp(context, surveyorProfile)
                    onCopySuccess("অফলাইন PC সফটওয়্যার সেভ হয়েছে: ${res.displayPath}")
                    if (res.uri != null) {
                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/html"
                            putExtra(android.content.Intent.EXTRA_STREAM, res.uri)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "BhumiSebaDigital_PC.html — কম্পিউটার ভার্সন")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(sendIntent, "PC-তে পাঠান (WhatsApp/Telegram/Email)")
                        )
                    }
                }.onFailure { err ->
                    onCopySuccess("PC ফাইল তৈরি ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pc_download_html_app_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Download,
                contentDescription = "Save Offline PC Software",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("অফলাইন PC সফটওয়্যার (.html) সেভ ও PC-তে পাঠান", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun UtilityBarChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalNeuPalette.current
    Row(
        modifier = Modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Color(0xFF0F6B54)
                else palette.surface.copy(alpha = 0.92f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
