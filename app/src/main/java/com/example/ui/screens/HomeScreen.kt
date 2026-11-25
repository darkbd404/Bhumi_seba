package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Straighten
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PC_INSTALL_APP_URL
import com.example.model.SavedMeasurementRecord
import com.example.model.SavedProjectType
import com.example.model.SavedSurveyProject
import com.example.ui.components.AnimatedCompassBadge
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.NeuFeatureCard
import com.example.ui.components.PcInstallLinkCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    savedRecords: List<SavedMeasurementRecord>,
    savedProjects: List<SavedSurveyProject> = emptyList(),
    onToggleTheme: () -> Unit,
    onOpenMouzaPdf: () -> Unit,
    onOpenRealMap: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenTools: (initialTab: String) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    onRestoreProject: (SavedSurveyProject) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { visible = true }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        val quickCols = if (maxWidth >= 840.dp) 4 else 2
        val featureCols = if (maxWidth >= 840.dp) 3 else 2

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1120.dp),
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
                            text = "ভূমি সেবা ডিজিটাল",
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

        // Clean Auto-Sliding Image Hero Carousel (no overlaid text/buttons — pure crisp artwork)
        item {
            val bannerImages = remember {
                listOf(
                    R.drawable.img_banner_paddy,
                    R.drawable.img_banner_survey,
                    R.drawable.img_banner_mouza,
                    R.drawable.img_banner_delta,
                    R.drawable.img_land_hero
                )
            }
            val pagerState = rememberPagerState(pageCount = { bannerImages.size })

            LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                if (!pagerState.isScrollInProgress) {
                    delay(5000L)
                    val nextPage = (pagerState.currentPage + 1) % bannerImages.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
                    )
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 5 }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                            .neuSurface(cornerRadius = 24.dp, elevation = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                .absoluteValue.coerceIn(0f, 1f)
                            Image(
                                painter = painterResource(id = bannerImages[page]),
                                contentDescription = "Bangladesh Land Survey Banner ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val s = 1f - 0.05f * pageOffset
                                        scaleX = s
                                        scaleY = s
                                        alpha = 1f - 0.25f * pageOffset
                                    }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(bannerImages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 22.dp else 7.dp,
                                animationSpec = tween(250),
                                label = "bannerDotWidth"
                            )
                            val dotColor by animateColorAsState(
                                targetValue = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                animationSpec = tween(250),
                                label = "bannerDotColor"
                            )
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }

        // Featured Own Websites Strip
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(420, delayMillis = 60, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(420, delayMillis = 60, easing = FastOutSlowInEasing)) { it / 6 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "তাৎক্ষণিক ভূমি পোর্টাল (ইন-অ্যাপ ব্রাউজার)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val quickPortals = listOf(
                        Triple("DLRMS Land", "মৌজা ও খতিয়ান", "quick_dlrms_button" to Pair(EmeraldPrimary) { onOpenUrlInApp("https://dlrmslandgovbd.com", "DLRMS Land Portal") }),
                        Triple("E-Porcha Tech", "ই-পর্চা ও ডাউনলোড", "quick_eporcha_button" to Pair(TealSecondary) { onOpenUrlInApp("https://eporcha.tech/", "E-Porcha Tech") }),
                        Triple("সরকারি DLRMS", "dlrms.land.gov.bd", "quick_dlrms_gov_button" to Pair(Color(0xFF1A73E8)) { onOpenUrlInApp("https://dlrms.land.gov.bd", "ই-পর্চা / খতিয়ান (DLRMS)") }),
                        Triple("খতিয়ান অনুসন্ধান", "settlement.gov.bd", "quick_settlement_button" to Pair(SurveyGold) { onOpenUrlInApp("https://settlement.gov.bd/Khatian/", "খতিয়ান অনুসন্ধান") })
                    )
                    quickPortals.chunked(quickCols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { (title, sub, meta) ->
                                PortalQuickPill(
                                    title = title,
                                    subtitle = sub,
                                    accent = meta.second.first,
                                    testTag = meta.first,
                                    modifier = Modifier.weight(1f),
                                    onClick = meta.second.second
                                )
                            }
                            repeat(quickCols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Main Neumorphic Feature Grid
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(460, delayMillis = 120, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(460, delayMillis = 120, easing = FastOutSlowInEasing)) { it / 6 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "সকল জরিপ ও ভূমি সেবা টুলস",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val features = listOf<@Composable (Modifier) -> Unit>(
                        { m ->
                            NeuFeatureCard(
                                title = "মৌজা নকশা মাপ",
                                subtitle = "1000x HD জুম, পলিগন, অটো-ফিল ও PNG সেভ",
                                icon = Icons.Rounded.PictureAsPdf,
                                accentColor = EmeraldPrimary,
                                badge = "1000x HD",
                                testTag = "card_mouza_pdf",
                                modifier = m,
                                onClick = onOpenMouzaPdf
                            )
                        },
                        { m ->
                            NeuFeatureCard(
                                title = "রিয়েল স্যাটেলাইট",
                                subtitle = "HD Google, Esri, OSM ম্যাপে লুপ পয়েন্টার ও GPS",
                                icon = Icons.Rounded.Public,
                                accentColor = TealSecondary,
                                badge = "HD Layers",
                                testTag = "card_real_map",
                                modifier = m,
                                onClick = onOpenRealMap
                            )
                        },
                        { m ->
                            NeuFeatureCard(
                                title = "সকল ভূমি সেবা",
                                subtitle = "নামজারি, খাজনা, মৌজা ম্যাপ ও সরকারি পোর্টাল",
                                icon = Icons.Rounded.Language,
                                accentColor = Color(0xFF1A73E8),
                                badge = "22 Portals",
                                testTag = "card_services",
                                modifier = m,
                                onClick = onOpenServices
                            )
                        },
                        { m ->
                            NeuFeatureCard(
                                title = "জরিপ ক্যালকুলেটর",
                                subtitle = "শতাংশ, কাঠা, দৈর্ঘ্য, আকৃতি, ফারায়েজ ও স্কেল",
                                icon = Icons.Rounded.Calculate,
                                accentColor = SurveyGold,
                                badge = "7 Tools",
                                testTag = "card_tools",
                                modifier = m,
                                onClick = { onOpenTools("unit") }
                            )
                        },
                        { m ->
                            NeuFeatureCard(
                                title = "গুগল ভূমি সার্চ",
                                subtitle = "বড় পেজে সরাসরি Google-এ যেকোনো বিষয় খুঁজুন",
                                icon = Icons.Rounded.Search,
                                accentColor = Color(0xFF1A73E8),
                                badge = "Google",
                                testTag = "card_ai_assistant",
                                modifier = m,
                                onClick = onOpenAiAssistant
                            )
                        },
                        { m ->
                            NeuFeatureCard(
                                title = "পরিচিতি ও যোগাযোগ",
                                subtitle = "ডেভেলপার প্রোফাইল ও সাপোর্ট",
                                icon = Icons.Rounded.Person,
                                accentColor = Color(0xFF00897B),
                                badge = "About",
                                testTag = "card_about",
                                modifier = m,
                                onClick = onOpenAbout
                            )
                        }
                    )

                    features.chunked(featureCols).forEach { rowCards ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCards.forEach { card -> card(Modifier.weight(1f)) }
                            repeat(featureCols - rowCards.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Quick Calculator Shortcuts
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(500, delayMillis = 180, easing = FastOutSlowInEasing)) { it / 6 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "তাৎক্ষণিক পরিমাপ ক্যালকুলেটর",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val calcQuick = listOf(
                        Triple("দৈর্ঘ্য কনভার্টার", "ফুট, লিংক, চেইন, গজ", "quick_tool_len" to Pair(EmeraldPrimary) { onOpenTools("len") }),
                        Triple("আকৃতি মাপ", "আয়ত, ত্রিভুজ ও ৪-বাহু", "quick_tool_shape" to Pair(TealSecondary) { onOpenTools("shape") }),
                        Triple("স্কেল ও চেইন", "১৬\"/৩২\"/৬৪\" নকশা হিসাব", "quick_tool_scale" to Pair(Color(0xFF1A73E8)) { onOpenTools("scale") }),
                        Triple("দলিল ও দাম", "মূল্য, স্ট্যাম্প ও বণ্টন", "quick_tool_tax" to Pair(SurveyGold) { onOpenTools("tax") })
                    )
                    calcQuick.chunked(quickCols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { (title, sub, meta) ->
                                PortalQuickPill(
                                    title = title,
                                    subtitle = sub,
                                    accent = meta.second.first,
                                    testTag = meta.first,
                                    modifier = Modifier.weight(1f),
                                    onClick = meta.second.second
                                )
                            }
                            repeat(quickCols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Saved Restorable Projects (Mouza PDF + Real Map data snapshots)
        if (savedProjects.isNotEmpty()) {
            item {
                Text(
                    text = "সংরক্ষিত ম্যাপ ও PDF কাজ (${savedProjects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(savedProjects.take(8), key = { it.id }) { proj ->
                SavedProjectRow(
                    project = proj,
                    onClick = { onRestoreProject(proj) },
                    onDelete = { onDeleteProject(proj.id) }
                )
            }
        }

        // Saved Measurement History (PNG Exports)
        if (savedRecords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সাম্প্রতিক সেভকৃত PNG পরিমাপ (${savedRecords.size})",
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

        // PC / Web Install & Share Link Card
        item {
            PcInstallLinkCard(
                url = PC_INSTALL_APP_URL,
                onCopySuccess = { msg -> toastMessage = msg }
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

        BottomSwipeToastBanner(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
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

@Composable
private fun SavedProjectRow(
    project: SavedSurveyProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.timestampMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(project.timestampMillis))
    }
    val isMouza = project.type == SavedProjectType.MOUZA_PDF
    val badgeText = if (isMouza) "মৌজা PDF/ছবি" else "রিয়েল ম্যাপ"
    val accent = if (isMouza) EmeraldPrimary else TealSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_project_${project.id}")
            .neuClickable(cornerRadius = 18.dp, elevation = 5.dp, onClick = onClick)
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMouza) Icons.Rounded.PictureAsPdf else Icons.Rounded.Map,
                    contentDescription = null,
                    tint = accent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$badgeText • ${project.itemCount}টি • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(Locale.US, "%.2f শতক • %.2f কাঠা • রিস্টোর করুন", project.decimalArea, project.kathaArea),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Restore, contentDescription = "Restore Project", tint = accent)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Project", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
