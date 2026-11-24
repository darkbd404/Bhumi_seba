package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pentagon
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.LandCalculators
import com.example.model.MapLayerOption
import com.example.model.MapMeasureCategory
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.RealMapViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled", "MissingPermission")
@Composable
fun RealMapScreen(
    onNavigateBack: () -> Unit,
    viewModel: RealMapViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    var pendingCenterGps by remember { mutableStateOf(false) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    webViewRef?.evaluateJavascript(
                        "window.bhumiMap && window.bhumiMap.flyTo(${loc.latitude}, ${loc.longitude}, 18, true);",
                        null
                    )
                    if (state.selectedCategory == MapMeasureCategory.GPS_WALK && state.isGpsWalkActive) {
                        webViewRef?.evaluateJavascript(
                            "window.bhumiMap && window.bhumiMap.addPoint(${loc.latitude}, ${loc.longitude});",
                            null
                        )
                    }
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("লোকেশন পারমিশন ছাড়া GPS ব্যবহার করা যাবে না।")
            }
            viewModel.setGpsWalkActive(false)
        }
        pendingCenterGps = false
    }

    fun requestOrUseLocation(addPointIfWalking: Boolean = false) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                if (loc != null) {
                    webViewRef?.evaluateJavascript(
                        "window.bhumiMap && window.bhumiMap.flyTo(${loc.latitude}, ${loc.longitude}, 18, true);",
                        null
                    )
                    if (addPointIfWalking) {
                        webViewRef?.evaluateJavascript(
                            "window.bhumiMap && window.bhumiMap.addPoint(${loc.latitude}, ${loc.longitude});",
                            null
                        )
                    }
                } else {
                    scope.launch { snackbarHostState.showSnackbar("বর্তমান GPS লোকেশন পাওয়া যাচ্ছে না।") }
                }
            }
        } else {
            pendingCenterGps = true
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Continuous GPS Walk location callback when GPS Walk mode is active
    DisposableEffect(state.isGpsWalkActive, state.selectedCategory) {
        val shouldTrack = state.isGpsWalkActive && state.selectedCategory == MapMeasureCategory.GPS_WALK
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                webViewRef?.evaluateJavascript(
                    "window.bhumiMap && window.bhumiMap.updateUserLoc(${loc.latitude}, ${loc.longitude});",
                    null
                )
            }
        }
        if (shouldTrack && hasPerm) {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateDistanceMeters(2.0f)
                .build()
            fusedClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
        }
        onDispose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Push layer, road/area labels, and category updates to Leaflet JS
    LaunchedEffect(state.selectedLayer, state.showLabelsOverlay, mapReady) {
        if (mapReady) {
            val l = state.selectedLayer
            val overlay = l.supportsLabelOverlay && state.showLabelsOverlay
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setTileLayer('${l.tileUrl}', '${l.attribution}', ${l.maxZoom}, $overlay);",
                null
            )
        }
    }
    LaunchedEffect(state.selectedCategory, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setMode('${state.selectedCategory.id}');",
                null
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar + Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_back_button")
                        .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("উপজেলা, মৌজা বা জেলা খুঁজুন...", maxLines = 1) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        viewModel.searchLocation(state.searchQuery)
                    }),
                    trailingIcon = {
                        IconButton(onClick = {
                            keyboard?.hide()
                            viewModel.searchLocation(state.searchQuery)
                        }) {
                            if (state.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = "Search Location")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_search_input")
                )

                // Layer Picker Button
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("map_layer_button")
                            .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = { showLayerMenu = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = "Map Layers", tint = EmeraldPrimary)
                    }
                    DropdownMenu(
                        expanded = showLayerMenu,
                        onDismissRequest = { showLayerMenu = false }
                    ) {
                        MapLayerOption.entries.forEach { layer ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (layer == state.selectedLayer) "✓ ${layer.titleBn} (${layer.titleEn})"
                                        else "${layer.titleBn} (${layer.titleEn})",
                                        fontWeight = if (layer == state.selectedLayer) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.selectLayer(layer)
                                    showLayerMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Search Suggestions Dropdown
            AnimatedVisibility(visible = state.searchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .neuSurface(cornerRadius = 16.dp, elevation = 6.dp)
                        .padding(6.dp)
                ) {
                    state.searchResults.forEach { item ->
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    webViewRef?.evaluateJavascript(
                                        "window.bhumiMap && window.bhumiMap.flyTo(${item.lat}, ${item.lon}, 17, false);",
                                        null
                                    )
                                    viewModel.clearSearchResults()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Measurement Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapMeasureCategory.entries.forEach { cat ->
                    val icon = when (cat) {
                        MapMeasureCategory.AREA -> Icons.Rounded.Pentagon
                        MapMeasureCategory.DISTANCE -> Icons.Rounded.Straighten
                        MapMeasureCategory.GPS_WALK -> Icons.Rounded.DirectionsWalk
                        MapMeasureCategory.CIRCLE -> Icons.Rounded.RadioButtonChecked
                        MapMeasureCategory.SPLIT -> Icons.Rounded.PieChart
                    }
                    NeuPillChip(
                        text = cat.titleBn,
                        selected = state.selectedCategory == cat,
                        icon = icon,
                        testTag = "map_cat_${cat.id}",
                        onClick = { viewModel.selectCategory(cat) }
                    )
                }
            }

            // Interactive Leaflet Multi-Layer Map Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .neuSurface(cornerRadius = 22.dp, elevation = 6.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.loadsImagesAutomatically = true
                            settings.allowContentAccess = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.userAgentString = "BhumiSurveyBD/1.0 (Linux; Android)"
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    mapReady = true
                                    view?.evaluateJavascript("window.map && window.map.invalidateSize();", null)
                                }
                            }
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onMeasurementChanged(points: Int, areaSqM: Double, perimeterM: Double) {
                                        viewModel.updateMeasurementFromBridge(points, areaSqM, perimeterM)
                                    }
                                },
                                "AndroidMapBridge"
                            )
                            loadDataWithBaseURL(
                                "https://bhumisurvey.app/",
                                buildLeafletHtml(state.selectedLayer, state.showLabelsOverlay),
                                "text/html",
                                "UTF-8",
                                null
                            )
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Active Layer Pill Badge + Road/Area Labels Toggle
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC16222B))
                            .clickable { showLayerMenu = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "লেয়ার: ${state.selectedLayer.titleEn}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (state.selectedLayer.supportsLabelOverlay) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (state.showLabelsOverlay) Color(0xDD0F6B54) else Color(0xCC37474F)
                                )
                                .clickable { viewModel.toggleLabelsOverlay() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (state.showLabelsOverlay) "✓ রাস্তা ও এলাকার নাম" else "রাস্তা/এলাকার নাম: বন্ধ",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // My GPS Location FAB & GPS Walk Drop Pin button
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (state.selectedCategory == MapMeasureCategory.GPS_WALK) {
                        Button(
                            onClick = {
                                if (!state.isGpsWalkActive) {
                                    viewModel.setGpsWalkActive(true)
                                }
                                requestOrUseLocation(addPointIfWalking = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("GPS পয়েন্ট যোগ")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("map_gps_button")
                            .clip(CircleShape)
                            .background(Color(0xEE0F6B54))
                            .clickable { requestOrUseLocation(addPointIfWalking = false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "My Location",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom Live Measurement & Selected-Only PNG Export Panel
            val sqFeet = state.areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER
            val areaRes = LandCalculators.fromSqFeet(sqFeet)
            val perimeterFeet = state.perimeterMeters * 3.280839895

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .neuSurface(cornerRadius = 22.dp, elevation = 8.dp)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.pointCount == 0) {
                    Text(
                        text = state.selectedCategory.subtitleBn,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ম্যাপে দাগের কোণাগুলোতে ট্যাপ করুন (পিন টেনে ঠিক করা যাবে)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.selectedCategory == MapMeasureCategory.DISTANCE) {
                                    String.format(
                                        Locale.US,
                                        "দূরত্ব: %.2f ফুট  •  %.2f মিটার",
                                        perimeterFeet, state.perimeterMeters
                                    )
                                } else {
                                    String.format(
                                        Locale.US,
                                        "%.2f শতাংশ  •  %.2f কাঠা  •  %.3f বিঘা",
                                        areaRes.decimal, areaRes.katha, areaRes.bigha
                                    )
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.0f বর্গফুট | পরিধি: %.1f ফু (%.1f মি) | পয়েন্ট: %d",
                                    areaRes.sqFeet, perimeterFeet, state.perimeterMeters, state.pointCount
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Plot Split Co-Sharer Guide
                    if (state.selectedCategory == MapMeasureCategory.SPLIT && areaRes.decimal > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ভাগ (${state.splitSharesCount} অংশ): প্রতি ভাগ = ${
                                    String.format(Locale.US, "%.2f শতক / %.2f কাঠা", areaRes.decimal / state.splitSharesCount, areaRes.katha / state.splitSharesCount)
                                }",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.setSplitSharesCount(state.splitSharesCount - 1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease Shares", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.setSplitSharesCount(state.splitSharesCount + 1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase Shares", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.undo();", null)
                        },
                        enabled = state.pointCount > 0,
                        modifier = Modifier.testTag("map_undo_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.clearAll();", null)
                        },
                        enabled = state.pointCount > 0,
                        modifier = Modifier.testTag("map_clear_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = {
                            val wv = webViewRef ?: return@Button
                            scope.launch {
                                // Fit map nicely to selected polygon first, wait briefly, then capture strictly the selected region!
                                wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.fitSelectedBounds();", null)
                                delay(380)
                                wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.getSelectedCropRect();") { rawJson ->
                                    val cropNorm = parseCropRect(rawJson)
                                    val bmp = Bitmap.createBitmap(
                                        wv.width.coerceAtLeast(100),
                                        wv.height.coerceAtLeast(100),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val c = Canvas(bmp)
                                    wv.draw(c)
                                    viewModel.exportSelectedMapPng(bmp, cropNorm)
                                }
                            }
                        },
                        enabled = state.pointCount > 0 && !state.isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_export_png_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("সিলেক্টেড অংশ PNG সেভ", maxLines = 1)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 12.dp, end = 12.dp)
        )
    }
}

private fun parseCropRect(rawJsResult: String?): RectF? {
    if (rawJsResult.isNullOrBlank() || rawJsResult == "null") return null
    return runCatching {
        val cleaned = rawJsResult.trim().removeSurrounding("\"").replace("\\\"", "\"")
        val json = JSONObject(cleaned)
        RectF(
            json.getDouble("left").toFloat(),
            json.getDouble("top").toFloat(),
            json.getDouble("right").toFloat(),
            json.getDouble("bottom").toFloat()
        )
    }.getOrNull()
}

private fun buildLeafletHtml(initialLayer: MapLayerOption, showLabels: Boolean): String {
    val initialOverlay = initialLayer.supportsLabelOverlay && showLabels
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
          <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
          <style>
            html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #14221d; }
            .edge-label {
              background: rgba(15, 107, 84, 0.92);
              color: #fff;
              border: 1.5px solid #ffd54f;
              border-radius: 6px;
              padding: 2px 5px;
              font-size: 11px;
              font-weight: bold;
              white-space: nowrap;
              box-shadow: 0 2px 4px rgba(0,0,0,0.35);
            }
            .pin-dot {
              width: 16px; height: 16px;
              background: #ffd54f;
              border: 3px solid #0f6b54;
              border-radius: 50%;
              box-shadow: 0 2px 6px rgba(0,0,0,0.5);
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', { zoomControl: false, attributionControl: true }).setView([23.8103, 90.4125], 16);
            var currentTile = L.tileLayer('${initialLayer.tileUrl}', {
              maxZoom: ${initialLayer.maxZoom},
              attribution: '${initialLayer.attribution}'
            }).addTo(map);

            var roadsOverlay = null;
            var placesOverlay = null;

            function syncLabelOverlays(enableOverlay) {
              if (roadsOverlay) { map.removeLayer(roadsOverlay); roadsOverlay = null; }
              if (placesOverlay) { map.removeLayer(placesOverlay); placesOverlay = null; }
              if (enableOverlay) {
                roadsOverlay = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}', {
                  maxZoom: 19, opacity: 0.9
                }).addTo(map);
                placesOverlay = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', {
                  maxZoom: 19, opacity: 0.95
                }).addTo(map);
              }
            }
            syncLabelOverlays($initialOverlay);
            setTimeout(function() { map.invalidateSize(); }, 250);

            var mode = 'area';
            var points = [];
            var markers = [];
            var edgeLabels = [];
            var activeShape = null;
            var userMarker = null;

            function geodesicAreaSqM(latLngs) {
              var n = latLngs.length;
              if (n < 3) return 0;
              var R = 6378137.0;
              var area = 0;
              for (var i = 0; i < n; i++) {
                var p1 = latLngs[i];
                var p2 = latLngs[(i + 1) % n];
                var lat1 = p1.lat * Math.PI / 180;
                var lon1 = p1.lng * Math.PI / 180;
                var lat2 = p2.lat * Math.PI / 180;
                var lon2 = p2.lng * Math.PI / 180;
                area += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
              }
              return Math.abs(area * R * R / 2.0);
            }

            function rebuildVisuals() {
              if (activeShape) { map.removeLayer(activeShape); activeShape = null; }
              edgeLabels.forEach(function(l) { map.removeLayer(l); });
              edgeLabels = [];

              var area = 0;
              var perimeter = 0;

              if (mode === 'circle' && points.length >= 2) {
                var radius = map.distance(points[0], points[1]);
                activeShape = L.circle(points[0], {
                  radius: radius,
                  color: '#ffd54f',
                  weight: 3,
                  fillColor: '#0f9d58',
                  fillOpacity: 0.35
                }).addTo(map);
                area = Math.PI * radius * radius;
                perimeter = 2 * Math.PI * radius;
                var mid = L.latLng((points[0].lat + points[1].lat)/2, (points[0].lng + points[1].lng)/2);
                var lbl = L.marker(mid, {
                  interactive: false,
                  icon: L.divIcon({ className: 'edge-label', html: 'R: ' + (radius * 3.28084).toFixed(1) + ' ফু' })
                }).addTo(map);
                edgeLabels.push(lbl);
              } else if (mode === 'distance') {
                if (points.length >= 2) {
                  activeShape = L.polyline(points, { color: '#ffd54f', weight: 4 }).addTo(map);
                  for (var i = 0; i < points.length - 1; i++) {
                    var d = map.distance(points[i], points[i+1]);
                    perimeter += d;
                    var mid = L.latLng((points[i].lat + points[i+1].lat)/2, (points[i].lng + points[i+1].lng)/2);
                    edgeLabels.push(L.marker(mid, {
                      interactive: false,
                      icon: L.divIcon({ className: 'edge-label', html: (d * 3.28084).toFixed(1) + ' ফু' })
                    }).addTo(map));
                  }
                }
              } else {
                if (points.length === 2) {
                  activeShape = L.polyline(points, { color: '#ffd54f', weight: 3, dashArray: '6,6' }).addTo(map);
                  var d = map.distance(points[0], points[1]);
                  perimeter = d;
                  var mid = L.latLng((points[0].lat + points[1].lat)/2, (points[0].lng + points[1].lng)/2);
                  edgeLabels.push(L.marker(mid, {
                    interactive: false,
                    icon: L.divIcon({ className: 'edge-label', html: (d * 3.28084).toFixed(1) + ' ফু' })
                  }).addTo(map));
                } else if (points.length >= 3) {
                  activeShape = L.polygon(points, {
                    color: '#ffd54f',
                    weight: 3.5,
                    fillColor: '#0f9d58',
                    fillOpacity: 0.35
                  }).addTo(map);
                  area = geodesicAreaSqM(points);
                  for (var i = 0; i < points.length; i++) {
                    var p1 = points[i];
                    var p2 = points[(i + 1) % points.length];
                    var d = map.distance(p1, p2);
                    perimeter += d;
                    var mid = L.latLng((p1.lat + p2.lat)/2, (p1.lng + p2.lng)/2);
                    edgeLabels.push(L.marker(mid, {
                      interactive: false,
                      icon: L.divIcon({ className: 'edge-label', html: (d * 3.28084).toFixed(1) + ' ফু' })
                    }).addTo(map));
                  }
                }
              }

              if (window.AndroidMapBridge) {
                window.AndroidMapBridge.onMeasurementChanged(points.length, area, perimeter);
              }
            }

            function addVertex(latlng) {
              if (mode === 'circle' && points.length >= 2) {
                points[1] = latlng;
                markers[1].setLatLng(latlng);
                rebuildVisuals();
                return;
              }
              var idx = points.length;
              points.push(latlng);
              var marker = L.marker(latlng, {
                draggable: true,
                icon: L.divIcon({ className: 'pin-dot', iconSize: [16, 16], iconAnchor: [8, 8] })
              }).addTo(map);
              marker.on('drag', function(e) {
                var pos = markers.indexOf(marker);
                if (pos >= 0) {
                  points[pos] = e.target.getLatLng();
                  rebuildVisuals();
                }
              });
              markers.push(marker);
              rebuildVisuals();
            }

            map.on('click', function(e) {
              addVertex(e.latlng);
            });

            window.bhumiMap = {
              setTileLayer: function(url, attr, maxZ, enableOverlay) {
                if (currentTile) map.removeLayer(currentTile);
                currentTile = L.tileLayer(url, { maxZoom: maxZ, attribution: attr }).addTo(map);
                syncLabelOverlays(!!enableOverlay);
                if (currentTile.bringToBack) currentTile.bringToBack();
              },
              setMode: function(newMode) {
                mode = newMode;
                rebuildVisuals();
              },
              addPoint: function(lat, lon) {
                addVertex(L.latLng(lat, lon));
              },
              flyTo: function(lat, lon, zoom, showPin) {
                map.setView([lat, lon], zoom);
                if (showPin) {
                  if (userMarker) map.removeLayer(userMarker);
                  userMarker = L.circleMarker([lat, lon], {
                    radius: 8, color: '#ffffff', weight: 3, fillColor: '#1a73e8', fillOpacity: 1
                  }).addTo(map);
                }
              },
              updateUserLoc: function(lat, lon) {
                if (userMarker) userMarker.setLatLng([lat, lon]);
                else userMarker = L.circleMarker([lat, lon], {
                  radius: 8, color: '#ffffff', weight: 3, fillColor: '#1a73e8', fillOpacity: 1
                }).addTo(map);
              },
              undo: function() {
                if (points.length === 0) return;
                points.pop();
                var m = markers.pop();
                if (m) map.removeLayer(m);
                rebuildVisuals();
              },
              clearAll: function() {
                points = [];
                markers.forEach(function(m) { map.removeLayer(m); });
                markers = [];
                rebuildVisuals();
              },
              fitSelectedBounds: function() {
                if (activeShape && activeShape.getBounds) {
                  map.fitBounds(activeShape.getBounds(), { padding: [36, 36], animate: false });
                }
              },
              getSelectedCropRect: function() {
                if (!activeShape || !activeShape.getBounds) return null;
                var b = activeShape.getBounds();
                var nw = map.latLngToContainerPoint(b.getNorthWest());
                var se = map.latLngToContainerPoint(b.getSouthEast());
                var w = map.getSize().x;
                var h = map.getSize().y;
                var pad = 28;
                return JSON.stringify({
                  left: Math.max(0, (nw.x - pad) / w),
                  top: Math.max(0, (nw.y - pad) / h),
                  right: Math.min(1, (se.x + pad) / w),
                  bottom: Math.min(1, (se.y + pad) / h)
                });
              }
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}
