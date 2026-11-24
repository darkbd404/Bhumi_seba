package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SavedMeasurementRecord
import com.example.ui.components.AnimatedCompassBadge
import com.example.ui.components.NeuFeatureCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    savedRecords: List<SavedMeasurementRecord>,
    onToggleTheme: () -> Unit,
    onOpenMouzaPdf: () -> Unit,
    onOpenRealMap: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenTools: (initialTab: String) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedCompassBadge(sizeDp = 48)
                    Column {
                        Text(
                            text = "Bhumi Survey BD",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ডিজিটাল মৌজা ও ভূমি জরিপ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("toggle_theme_button")
                            .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onToggleTheme),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("home_about_button")
                            .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onOpenAbout),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Developer Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Hero Banner Card
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 5 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .neuSurface(cornerRadius = 24.dp, elevation = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_land_hero),
                        contentDescription = "Bangladesh Land Survey Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x55082E24),
                                        Color(0xDE062920)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "1000x Ultra-HD মৌজা ম্যাপ (PDF / ছবি) ও রিয়েল স্যাটেলাইট",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFE082)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "ফোন থেকে মৌজা নকশা (PDF/ছবি) ও স্যাটেলাইট ম্যাপে নিখুঁত জমি মাপুন",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenMouzaPdf,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hero_mouza_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("মৌজা নকশা", maxLines = 1)
                                }
                                Button(
                                    onClick = onOpenRealMap,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hero_realmap_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SurveyGold,
                                        contentColor = Color(0xFF1F1600)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("রিয়েল ম্যাপ", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Featured Own Websites Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "তাৎক্ষণিক ভূমি পোর্টাল (ইন-অ্যাপ ব্রাউজার)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PortalQuickPill(
                        title = "DLRMS Land",
                        subtitle = "মৌজা ও খতিয়ান",
                        accent = EmeraldPrimary,
                        testTag = "quick_dlrms_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenUrlInApp("https://dlrmslandgovbd.com", "DLRMS Land Portal") }
                    )
                    PortalQuickPill(
                        title = "E-Porcha Tech",
                        subtitle = "ই-পর্চা ও ডাউনলোড",
                        accent = TealSecondary,
                        testTag = "quick_eporcha_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenUrlInApp("https://eporcha.tech/", "E-Porcha Tech") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PortalQuickPill(
                        title = "সরকারি DLRMS",
                        subtitle = "dlrms.land.gov.bd",
                        accent = Color(0xFF1A73E8),
                        testTag = "quick_dlrms_gov_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenUrlInApp("https://dlrms.land.gov.bd", "ই-পর্চা / খতিয়ান (DLRMS)") }
                    )
                    PortalQuickPill(
                        title = "খতিয়ান অনুসন্ধান",
                        subtitle = "settlement.gov.bd",
                        accent = SurveyGold,
                        testTag = "quick_settlement_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenUrlInApp("https://settlement.gov.bd/Khatian/", "খতিয়ান অনুসন্ধান") }
                    )
                }
            }
        }

        // Main Neumorphic Feature Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "সকল জরিপ ও ভূমি সেবা টুলস",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuFeatureCard(
                        title = "মৌজা নকশা মাপ",
                        subtitle = "1000x HD জুম, অটো-ফিল, ফিতা ও PNG সেভ",
                        icon = Icons.Rounded.PictureAsPdf,
                        accentColor = EmeraldPrimary,
                        badge = "1000x HD",
                        testTag = "card_mouza_pdf",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMouzaPdf
                    )
                    NeuFeatureCard(
                        title = "রিয়েল স্যাটেলাইট",
                        subtitle = "Google, Esri, OSM, Topo ম্যাপে পিন ও GPS",
                        icon = Icons.Rounded.Public,
                        accentColor = TealSecondary,
                        badge = "7 Layers",
                        testTag = "card_real_map",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRealMap
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuFeatureCard(
                        title = "সকল ভূমি সেবা",
                        subtitle = "নামজারি, খাজনা, মৌজা ম্যাপ ও সরকারি পোর্টাল",
                        icon = Icons.Rounded.Language,
                        accentColor = Color(0xFF1A73E8),
                        badge = "22 Portals",
                        testTag = "card_services",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenServices
                    )
                    NeuFeatureCard(
                        title = "জরিপ ক্যালকুলেটর",
                        subtitle = "শতাংশ, কাঠা, দৈর্ঘ্য, আকৃতি, ফারায়েজ ও স্কেল",
                        icon = Icons.Rounded.Calculate,
                        accentColor = SurveyGold,
                        badge = "7 Tools",
                        testTag = "card_tools",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTools("unit") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuFeatureCard(
                        title = "AI ভূমি সহায়ক",
                        subtitle = "আইন, নামজারি ও নিকটস্থ ভূমি অফিস খুঁজুন",
                        icon = Icons.Rounded.AutoAwesome,
                        accentColor = Color(0xFF7B3FE4),
                        badge = "Gemini",
                        testTag = "card_ai_assistant",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAiAssistant
                    )
                    NeuFeatureCard(
                        title = "পরিচিতি ও যোগাযোগ",
                        subtitle = "ডেভেলপার প্রোফাইল ও সাপোর্ট",
                        icon = Icons.Rounded.Person,
                        accentColor = Color(0xFF00897B),
                        badge = "About",
                        testTag = "card_about",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAbout
                    )
                }
            }
        }

        // Quick Calculator Shortcuts (matching reference UI.html tools)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "তাৎক্ষণিক পরিমাপ ক্যালকুলেটর",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PortalQuickPill(
                        title = "দৈর্ঘ্য কনভার্টার",
                        subtitle = "ফুট, লিংক, চেইন, গজ",
                        accent = EmeraldPrimary,
                        testTag = "quick_tool_len",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTools("len") }
                    )
                    PortalQuickPill(
                        title = "আকৃতি মাপ",
                        subtitle = "আয়ত, ত্রিভুজ ও ৪-বাহু",
                        accent = TealSecondary,
                        testTag = "quick_tool_shape",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTools("shape") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PortalQuickPill(
                        title = "স্কেল ও চেইন",
                        subtitle = "১৬\"/৩২\"/৬৪\" নকশা হিসাব",
                        accent = Color(0xFF1A73E8),
                        testTag = "quick_tool_scale",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTools("scale") }
                    )
                    PortalQuickPill(
                        title = "দলিল ও দাম",
                        subtitle = "মূল্য, স্ট্যাম্প ও বণ্টন",
                        accent = SurveyGold,
                        testTag = "quick_tool_tax",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTools("tax") }
                    )
                }
            }
        }

        // Saved Measurement History
        if (savedRecords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সাম্প্রতিক সেভকৃত পরিমাপ (${savedRecords.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("মুছুন")
                    }
                }
            }
            items(savedRecords.take(6), key = { it.id }) { rec ->
                SavedRecordRow(rec)
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PortalQuickPill(
    title: String,
    subtitle: String,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .testTag(testTag)
            .neuClickable(cornerRadius = 18.dp, elevation = 5.dp, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedRecordRow(record: SavedMeasurementRecord) {
    val dateStr = remember(record.timestampMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(record.timestampMillis))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Straighten,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
            }
            Column {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.source} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format(Locale.US, "%.2f শতক", record.decimalArea),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(Locale.US, "%.2f কাঠা", record.kathaArea),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
