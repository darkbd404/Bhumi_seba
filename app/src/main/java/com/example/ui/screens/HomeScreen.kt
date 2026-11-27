package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.LandCalculators
import com.example.model.CustomUnitPreset
import com.example.model.GlobalSurveySettings
import com.example.model.SavedMeasurementRecord
import com.example.model.SavedProjectType
import com.example.model.SavedSurveyProject
import com.example.model.SurveyorProfile
import com.example.ui.components.AnimatedCompassBadge
import com.example.ui.components.NeuFeatureCard
import com.example.ui.components.NeuPillChip
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
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    savedRecords: List<SavedMeasurementRecord>,
    savedProjects: List<SavedSurveyProject> = emptyList(),
    surveyorProfile: SurveyorProfile = SurveyorProfile(),
    pinnedToolIds: List<String> = emptyList(),
    globalSettings: GlobalSurveySettings = GlobalSurveySettings(),
    onTogglePinTool: (String) -> Unit = {},
    onSaveGlobalSettings: (GlobalSurveySettings) -> Unit = {},
    onToggleTheme: () -> Unit,
    onNavigateRoute: (Any) -> Unit = {},
    onOpenAbout: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    onRestoreProject: (SavedSurveyProject) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(HomeModuleCategory.ALL) }
    var showGlobalSettingsDialog by remember { mutableStateOf(false) }

    val allModules = remember { HomeModuleCatalog.allModules }
    val pinnedModules = remember(pinnedToolIds, allModules) {
        pinnedToolIds.mapNotNull { id -> allModules.firstOrNull { it.id == id } }
    }
    val filteredModules = remember(searchQuery, selectedCategory, allModules) {
        allModules.filter { mod ->
            val catOk = selectedCategory == HomeModuleCategory.ALL || mod.category == selectedCategory
            val qOk = searchQuery.isBlank() ||
                mod.titleBn.contains(searchQuery, true) ||
                mod.subtitleBn.contains(searchQuery, true) ||
                mod.keywords.contains(searchQuery, true) ||
                mod.number.toString() == searchQuery.trim()
            catOk && qOk
        }
    }

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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AnimatedCompassBadge(sizeDp = 48)
                        Column {
                            Text(
                                text = "ভূমি সেবা ডিজিটাল",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${allModules.size}টি আলাদা জরিপ ও CAD টুলস",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_global_settings_btn")
                                .neuClickable(cornerRadius = 14.dp, elevation = 4.dp) {
                                    showGlobalSettingsDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "গ্লোবাল ইউনিট ও সংখ্যা সেটিংস",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                                .size(48.dp)
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

            // Clean Auto-Sliding Image Hero Carousel
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
                                .height(180.dp)
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

            // M17: Pinned Tools Quick Strip
            if (pinnedModules.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.PushPin, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "পিন করা টুলস (${pinnedModules.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "প্রয়োজনীয় কার্ডের পিন আইকনে চাপুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("home_pinned_strip")
                        ) {
                            items(pinnedModules, key = { "pin_${it.id}" }) { mod ->
                                Surface(
                                    color = mod.accent.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .width(164.dp)
                                        .testTag("pinned_${mod.testTag}")
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onNavigateRoute(mod.route) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(mod.icon, contentDescription = null, tint = mod.accent, modifier = Modifier.size(20.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mod.titleBn,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = mod.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = mod.accent,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Instant Land Portals (In-App Browser)
            item {
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

            // Search + Category Filter Header for ALL 52 Individual Modules
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সকল আলাদা জরিপ ও সেবা টুলস (${filteredModules.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (globalSettings.useBanglaDigits) "সংখ্যা: ০-৯" else "Digits: 0-9",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f))
                                .clickable { showGlobalSettingsDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("টুলস খুঁজুন (যেমন: মৌজা, প্যান্টোগ্রাফ, CAD, খতিয়ান, দলিল, ±)...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_module_search")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(HomeModuleCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.labelBn, maxLines = 1) },
                                modifier = Modifier.testTag("home_cat_${cat.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // All Individual Separated Module Cards Directly on HomeScreen
            items(
                items = filteredModules.chunked(featureCols),
                key = { row -> row.joinToString("_") { it.id } }
            ) { rowMods ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowMods.forEach { mod ->
                        val isPinned = mod.id in pinnedToolIds
                        Box(modifier = Modifier.weight(1f)) {
                            NeuFeatureCard(
                                title = "${mod.number}. ${mod.titleBn}",
                                subtitle = mod.subtitleBn,
                                icon = mod.icon,
                                accentColor = mod.accent,
                                badge = mod.badge,
                                testTag = mod.testTag,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onNavigateRoute(mod.route) }
                            )
                            IconButton(
                                onClick = { onTogglePinTool(mod.id) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                                    .size(36.dp)
                                    .testTag("pin_btn_${mod.id}")
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = if (isPinned) "Unpin ${mod.titleBn}" else "Pin ${mod.titleBn}",
                                    tint = if (isPinned) mod.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    repeat(featureCols - rowMods.size) { Spacer(Modifier.weight(1f)) }
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
                        useBanglaDigits = globalSettings.useBanglaDigits,
                        precision = globalSettings.decimalPrecision,
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
                    SavedRecordRow(
                        record = rec,
                        useBanglaDigits = globalSettings.useBanglaDigits,
                        precision = globalSettings.decimalPrecision
                    )
                }
            }

            item { Spacer(Modifier.height(14.dp)) }
        }
    }

    if (showGlobalSettingsDialog) {
        GlobalSurveySettingsDialog(
            current = globalSettings,
            onDismiss = { showGlobalSettingsDialog = false },
            onSave = { updated ->
                onSaveGlobalSettings(updated)
                showGlobalSettingsDialog = false
            }
        )
    }
}

@Composable
private fun GlobalSurveySettingsDialog(
    current: GlobalSurveySettings,
    onDismiss: () -> Unit,
    onSave: (GlobalSurveySettings) -> Unit
) {
    var useBanglaDigits by remember(current) { mutableStateOf(current.useBanglaDigits) }
    var precision by remember(current) { mutableStateOf(current.decimalPrecision) }
    var defaultOldPaper by remember(current) { mutableStateOf(current.defaultOldPaper) }
    var highErrPct by remember(current) { mutableStateOf(current.confidenceHighMaxErrPct.toString()) }
    var medErrPct by remember(current) { mutableStateOf(current.confidenceMedMaxErrPct.toString()) }
    var activePresetId by remember(current) { mutableStateOf(current.activeUnitPresetId) }
    var customPresets by remember(current) { mutableStateOf(current.customUnitPresets) }

    var newPresetName by remember { mutableStateOf("") }
    var newKathaSqFt by remember { mutableStateOf("720") }
    var newKaniDecimals by remember { mutableStateOf("40") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("গ্লোবাল সার্ভে ও একক সেটিংস", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("বাংলা সংখ্যা প্রদর্শন (০-৯ ↔ 0-9)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = useBanglaDigits, onCheckedChange = { useBanglaDigits = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সব কাগজ ডিফল্টে পুরাতন কাগজ লুকে", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = defaultOldPaper, onCheckedChange = { defaultOldPaper = it })
                }

                Text("দশমিক ঘর (Precision):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4, 6).forEach { p ->
                        NeuPillChip(text = "${p} ঘর", selected = precision == p) { precision = p }
                    }
                }

                Text("আস্থার সীমা (Confidence ±% Threshold):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = highErrPct,
                        onValueChange = { highErrPct = it },
                        label = { Text("উচ্চ আস্থা ≤%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = medErrPct,
                        onValueChange = { medErrPct = it },
                        label = { Text("মধ্যম আস্থা ≤%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("আঞ্চলিক ইউনিট প্রিসেট:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                customPresets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activePresetId == preset.id) EmeraldPrimary.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { activePresetId = preset.id }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (activePresetId == preset.id) "✓ " else ""}${preset.nameBn}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (activePresetId == preset.id) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("নতুন এলাকার নাম (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKathaSqFt,
                        onValueChange = { newKathaSqFt = it },
                        label = { Text("১ কাঠা ft²") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newKaniDecimals,
                        onValueChange = { newKaniDecimals = it },
                        label = { Text("১ কানি শতক") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (newPresetName.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val katha = newKathaSqFt.toDoubleOrNull() ?: 720.0
                            val kani = newKaniDecimals.toDoubleOrNull() ?: 40.0
                            val id = "preset_${System.currentTimeMillis()}"
                            val created = CustomUnitPreset(
                                id = id,
                                nameBn = "${newPresetName.trim()} (কাঠা=${katha.toInt()} ft², কানি=${kani.toInt()} শতক)",
                                decimalSqFt = 435.6,
                                kathaSqFt = katha.coerceAtLeast(100.0),
                                bighaSqFt = katha.coerceAtLeast(100.0) * 20.0,
                                kaniDecimals = kani.coerceAtLeast(10.0)
                            )
                            customPresets = customPresets + created
                            activePresetId = id
                            newPresetName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ কাস্টম প্রিসেট সংরক্ষণ") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        current.copy(
                            useBanglaDigits = useBanglaDigits,
                            decimalPrecision = precision,
                            defaultOldPaper = defaultOldPaper,
                            confidenceHighMaxErrPct = highErrPct.toDoubleOrNull()?.coerceIn(0.1, 10.0) ?: 1.5,
                            confidenceMedMaxErrPct = medErrPct.toDoubleOrNull()?.coerceIn(0.5, 25.0) ?: 4.0,
                            activeUnitPresetId = activePresetId,
                            customUnitPresets = customPresets
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) { Text("সেভ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
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
private fun SavedRecordRow(
    record: SavedMeasurementRecord,
    useBanglaDigits: Boolean,
    precision: Int
) {
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
                text = "${LandCalculators.formatNumber(record.decimalArea, precision, useBanglaDigits)} শতক",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${LandCalculators.formatNumber(record.kathaArea, precision, useBanglaDigits)} কাঠা",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavedProjectRow(
    project: SavedSurveyProject,
    useBanglaDigits: Boolean,
    precision: Int,
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
                    text = "${LandCalculators.formatNumber(project.decimalArea, precision, useBanglaDigits)} শতক • ${LandCalculators.formatNumber(project.kathaArea, precision, useBanglaDigits)} কাঠা • রিস্টোর করুন",
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
