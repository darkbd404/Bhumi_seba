package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.GpsFixed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.components.BottomSwipeToastBanner
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
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    var pointerModeEnabled by remember { mutableStateOf(true) }
    var localToastMessage by remember { mutableStateOf<String?>(null) }

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
            localToastMessage = "লোকেশন পারমিশন ছাড়া GPS ব্যবহার করা যাবে না।"
            viewModel.setGpsWalkActive(false)
        }
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
                    localToastMessage = "বর্তমান GPS লোকেশন পাওয়া যাচ্ছে না — GPS চালু করে চেষ্টা করুন।"
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

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
        if (!state.statusMessage.isNullOrBlank()) {
            localToastMessage = state.statusMessage
            viewModel.clearStatusMessage()
        }
    }

    // Push layer & label updates cleanly to Canvas Tile Engine
    LaunchedEffect(state.selectedLayer, state.showLabelsOverlay, mapReady) {
        if (mapReady) {
            val l = state.selectedLayer
            val overlay = l.supportsLabelOverlay && state.showLabelsOverlay
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setTileLayer('${l.tileUrl}', '${l.attribution}', ${l.maxNativeZoom}, ${l.maxZoom}, $overlay);",
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
    LaunchedEffect(pointerModeEnabled, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setPointerMode($pointerModeEnabled);",
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
                                        text = if (layer == state.selectedLayer) "✓ ${layer.titleBn}"
                                        else layer.titleBn,
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

            // Measurement Category Chips + Precision Pointer Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuPillChip(
                    text = "পয়েন্টার",
                    selected = pointerModeEnabled,
                    icon = Icons.Rounded.GpsFixed,
                    testTag = "map_pointer_toggle",
                    onClick = { pointerModeEnabled = !pointerModeEnabled }
                )
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

            // Zero-CDN Self-Contained Interactive Mercator Tile Engine WebView
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
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(0xFF14221D.toInt())
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.loadsImagesAutomatically = true
                            settings.allowContentAccess = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    mapReady = true
                                    view?.evaluateJavascript("window.bhumiMap && window.bhumiMap.resize();", null)
                                }
                            }
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onMeasurementChanged(
                                        points: Int,
                                        areaSqM: Double,
                                        perimeterM: Double,
                                        selectedPinIdx: Int
                                    ) {
                                        viewModel.updateMeasurementFromBridge(points, areaSqM, perimeterM, selectedPinIdx)
                                    }
                                },
                                "AndroidMapBridge"
                            )
                            loadDataWithBaseURL(
                                null,
                                buildCanvasTileMapHtml(state.selectedLayer, state.showLabelsOverlay),
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

                // Right-side Zoom + / - & My GPS Location FABs
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC16222B))
                            .clickable {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.zoomBy(1);", null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC16222B))
                            .clickable {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.zoomBy(-1);", null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (pointerModeEnabled) Color(0xEE0F6B54) else Color(0xCC16222B))
                            .clickable { pointerModeEnabled = !pointerModeEnabled },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GpsFixed,
                            contentDescription = "Toggle Precision Pointer",
                            tint = if (pointerModeEnabled) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
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

                // Precision Large Center Crosshair Pointer (non-blocking overlay)
                if (pointerModeEnabled) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.55f),
                            radius = 44f,
                            center = center,
                            style = Stroke(width = 7f)
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = 44f,
                            center = center,
                            style = Stroke(width = 3.2f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f),
                            radius = 22f,
                            center = center,
                            style = Stroke(width = 1.6f)
                        )
                        val gap = 12f
                        val arm = 64f
                        drawLine(Color.Black.copy(alpha = 0.72f), Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), strokeWidth = 6f)
                        drawLine(Color.Black.copy(alpha = 0.72f), Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), strokeWidth = 6f)
                        drawLine(Color.Black.copy(alpha = 0.72f), Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), strokeWidth = 6f)
                        drawLine(Color.Black.copy(alpha = 0.72f), Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), strokeWidth = 6f)
                        drawLine(Color(0xFFFFD54F), Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), strokeWidth = 3f)
                        drawLine(Color(0xFFFFD54F), Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), strokeWidth = 3f)
                        drawLine(Color(0xFFFFD54F), Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), strokeWidth = 3f)
                        drawLine(Color(0xFFFFD54F), Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), strokeWidth = 3f)
                        drawCircle(Color.White, radius = 6.5f, center = center)
                        drawCircle(Color(0xFFE53935), radius = 3.6f, center = center)
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 12.dp, end = 64.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.addCenterPoint();", null)
                            },
                            modifier = Modifier.testTag("map_pointer_add_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xEE0F6B54)),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("পয়েন্টারে পিন বসান", fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        AnimatedVisibility(visible = state.selectedPinIdx >= 0) {
                            Button(
                                onClick = {
                                    webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.moveSelectedPinToCenter();", null)
                                },
                                modifier = Modifier.testTag("map_move_pin_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xEE1565C0)),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text("পিন #${state.selectedPinIdx + 1} সরান", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
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
                        text = "ম্যাপে পয়েন্টার রেখে জুম লুপে দেখে পিন বসান — পিন ট্যাপ/ড্র্যাগ করে এডিট বা ডিলিট করা যাবে",
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.deleteSelectedPin();", null)
                        },
                        enabled = state.pointCount > 0,
                        modifier = Modifier.testTag("map_delete_pin_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Delete Pin", modifier = Modifier.size(16.dp), tint = Color(0xFFD6405A))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (state.selectedPinIdx >= 0) "পিন #${state.selectedPinIdx + 1}" else "বিন্দু ডিলিট",
                            color = Color(0xFFD6405A),
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.clearAll();", null)
                        },
                        enabled = state.pointCount > 0,
                        modifier = Modifier.testTag("map_clear_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = {
                            val wv = webViewRef ?: return@Button
                            scope.launch {
                                wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.fitSelectedBounds();", null)
                                delay(420)
                                wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.getExportPayload();") { rawPayload ->
                                    val parsed = parseExportPayload(rawPayload)
                                    captureWebViewBitmap(context, wv) { fallbackBmp ->
                                        viewModel.exportSelectedMapPlotPng(
                                            pointsLatLon = parsed.points,
                                            mode = parsed.mode,
                                            fallbackBitmap = fallbackBmp,
                                            cropRectNormalized = parsed.cropRect
                                        )
                                    }
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

        // Bottom Swipeable Toast Banner
        BottomSwipeToastBanner(
            message = localToastMessage,
            onDismiss = { localToastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp)
        )
    }
}

private data class MapExportPayload(
    val mode: String,
    val points: List<Pair<Double, Double>>,
    val cropRect: RectF?
)

private fun parseExportPayload(rawJsResult: String?): MapExportPayload {
    if (rawJsResult.isNullOrBlank() || rawJsResult == "null") {
        return MapExportPayload("area", emptyList(), null)
    }
    return runCatching {
        val cleaned = rawJsResult.trim().removeSurrounding("\"").replace("\\\"", "\"")
        val json = JSONObject(cleaned)
        val mode = json.optString("mode", "area")
        val ptsArr = json.optJSONArray("points")
        val pts = mutableListOf<Pair<Double, Double>>()
        if (ptsArr != null) {
            for (i in 0 until ptsArr.length()) {
                val obj = ptsArr.getJSONObject(i)
                pts.add(obj.getDouble("lat") to obj.getDouble("lon"))
            }
        }
        val cr = json.optJSONObject("cropRect")?.let { c ->
            RectF(
                c.getDouble("left").toFloat(),
                c.getDouble("top").toFloat(),
                c.getDouble("right").toFloat(),
                c.getDouble("bottom").toFloat()
            )
        }
        MapExportPayload(mode, pts, cr)
    }.getOrElse { MapExportPayload("area", emptyList(), null) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun captureWebViewBitmap(
    context: Context,
    webView: WebView,
    onResult: (Bitmap?) -> Unit
) {
    val w = webView.width.coerceAtLeast(100)
    val h = webView.height.coerceAtLeast(100)
    val activity = context.findActivity()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null && webView.isAttachedToWindow) {
        val loc = IntArray(2)
        webView.getLocationInWindow(loc)
        val rect = android.graphics.Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(
                activity.window,
                rect,
                bmp,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        onResult(bmp)
                    } else {
                        bmp.recycle()
                        onResult(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
            return
        } catch (_: Exception) {
            bmp.recycle()
        }
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    webView.draw(Canvas(bmp))
    onResult(bmp)
}

/**
 * 100% Self-Contained Zero-CDN HTML5 Canvas Mercator Tile Engine.
 * - Zero external scripts (never hangs on blocked CDN!)
 * - Supports maxNativeZoom upscaling up to Z22 + parent tile fallback so zoomed tiles NEVER disappear
 * - Supports interactive tap to place vertices, drag to adjust pins, pinch-to-zoom, and Esri label overlays
 */
private fun buildCanvasTileMapHtml(initialLayer: MapLayerOption, showLabels: Boolean): String {
    val initialOverlay = initialLayer.supportsLabelOverlay && showLabels
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #14221d; touch-action: none; }
            #rc { display: block; width: 100%; height: 100%; touch-action: none; }
            #attr {
              position: fixed; left: 8px; bottom: 4px; font-family: sans-serif;
              font-size: 10px; color: rgba(255,255,255,0.88);
              text-shadow: 0 1px 3px rgba(0,0,0,0.85); pointer-events: none;
            }
          </style>
        </head>
        <body>
          <canvas id="rc"></canvas>
          <div id="attr"></div>
          <script>
            (function() {
              const rc = document.getElementById('rc');
              const attrEl = document.getElementById('attr');
              const ctx = rc.getContext('2d');
              const dpr = () => Math.min(window.devicePixelRatio || 1, 3.0);

              // Mercator projection helpers [0..1]
              function ll2m(lat, lon) {
                const s = Math.sin(lat * Math.PI / 180);
                return {
                  x: (lon + 180) / 360,
                  y: 0.5 - Math.log((1 + s) / (1 - s)) / (4 * Math.PI)
                };
              }
              function m2ll(x, y) {
                return {
                  lat: 180 / Math.PI * Math.atan(Math.sinh(Math.PI * (1 - 2 * y))),
                  lon: x * 360 - 180
                };
              }
              function haversineM(a, b) {
                const R = 6371000;
                const dLat = (b.lat - a.lat) * Math.PI / 180;
                const dLon = (b.lon - a.lon) * Math.PI / 180;
                const la1 = a.lat * Math.PI / 180, la2 = b.lat * Math.PI / 180;
                const s1 = Math.sin(dLat / 2), s2 = Math.sin(dLon / 2);
                const h = s1 * s1 + Math.cos(la1) * Math.cos(la2) * s2 * s2;
                return 2 * R * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
              }
              function geodesicAreaSqM(pts) {
                if (pts.length < 3) return 0;
                const R = 6378137.0;
                let area = 0;
                for (let i = 0; i < pts.length; i++) {
                  const p1 = pts[i], p2 = pts[(i + 1) % pts.length];
                  area += ((p2.lon - p1.lon) * Math.PI / 180) *
                    (2 + Math.sin(p1.lat * Math.PI / 180) + Math.sin(p2.lat * Math.PI / 180));
                }
                return Math.abs(area * R * R / 2.0);
              }

              let tileUrl = '${initialLayer.tileUrl}';
              let tileAttr = '${initialLayer.attribution}';
              let maxNativeZ = ${initialLayer.maxNativeZoom};
              let maxViewZ = ${initialLayer.maxZoom};
              let showEsriOverlay = $initialOverlay;

              const ROADS_URL = 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}';
              const PLACES_URL = 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}';

              const state = {
                center: ll2m(23.8103, 90.4125),
                z: 16,
                mode: 'area',
                points: [],
                selectedPinIdx: -1,
                pointerMode: true,
                userLoc: null
              };

              // Tile cache: key -> { img, state: 'loading'|'ok'|'err' }
              const TC = new Map();
              let rafScheduled = false;
              function scheduleDraw() {
                if (!rafScheduled) {
                  rafScheduled = true;
                  requestAnimationFrame(() => { rafScheduled = false; draw(); });
                }
              }

              function fetchTile(urlTmpl, keyPrefix, z, x, y) {
                const k = keyPrefix + '|' + z + '|' + x + '|' + y;
                if (TC.has(k)) return TC.get(k);
                const entry = { img: new Image(), state: 'loading' };
                TC.set(k, entry);
                if (TC.size > 640) {
                  const first = TC.keys().next().value;
                  TC.delete(first);
                }
                entry.img.onload = () => { entry.state = 'ok'; scheduleDraw(); };
                entry.img.onerror = () => { entry.state = 'err'; };
                entry.img.src = urlTmpl.replace('{z}', z).replace('{x}', x).replace('{y}', y);
                return entry;
              }

              function drawTileLayer(urlTmpl, keyPrefix, maxNative) {
                const w = rc.clientWidth, h = rc.clientHeight;
                const zt = Math.max(1, Math.min(maxNative, Math.round(state.z + 0.35)));
                const wsView = 256 * Math.pow(2, state.z);
                const tilePxOnScreen = 256 * Math.pow(2, state.z - zt);
                const n = Math.pow(2, zt);

                const mLeft = state.center.x - (w / 2) / wsView;
                const mTop = state.center.y - (h / 2) / wsView;
                const mRight = state.center.x + (w / 2) / wsView;
                const mBottom = state.center.y + (h / 2) / wsView;

                const txMin = Math.floor(mLeft * n), txMax = Math.floor(mRight * n);
                const tyMin = Math.floor(mTop * n), tyMax = Math.floor(mBottom * n);

                for (let ty = tyMin; ty <= tyMax; ty++) {
                  if (ty < 0 || ty >= n) continue;
                  for (let tx = txMin; tx <= txMax; tx++) {
                    const wrapX = ((tx % n) + n) % n;
                    const sx = (tx / n - state.center.x) * wsView + w / 2;
                    const sy = (ty / n - state.center.y) * wsView + h / 2;

                    const tile = fetchTile(urlTmpl, keyPrefix, zt, wrapX, ty);
                    if (tile.state === 'ok') {
                      ctx.drawImage(tile.img, sx, sy, tilePxOnScreen + 0.65, tilePxOnScreen + 0.65);
                    } else {
                      // Parent zoom fallback so map is never blank while loading or zoomed deep!
                      for (let dz = 1; dz <= 4 && (zt - dz) >= 1; dz++) {
                        const pz = zt - dz;
                        const pScale = 1 << dz;
                        const ptx = Math.floor(wrapX / pScale);
                        const pty = Math.floor(ty / pScale);
                        const pk = keyPrefix + '|' + pz + '|' + ptx + '|' + pty;
                        const pTile = TC.get(pk);
                        if (pTile && pTile.state === 'ok') {
                          const iw = pTile.img.width || 256;
                          const ih = pTile.img.height || 256;
                          const subW = iw / pScale, subH = ih / pScale;
                          const subX = (wrapX % pScale) * subW;
                          const subY = (ty % pScale) * subH;
                          ctx.drawImage(pTile.img, subX, subY, subW, subH, sx, sy, tilePxOnScreen + 0.65, tilePxOnScreen + 0.65);
                          break;
                        }
                      }
                    }
                  }
                }
              }

              function toScreen(pt) {
                const w = rc.clientWidth, h = rc.clientHeight;
                const ws = 256 * Math.pow(2, state.z);
                const m = ll2m(pt.lat, pt.lon);
                return {
                  x: (m.x - state.center.x) * ws + w / 2,
                  y: (m.y - state.center.y) * ws + h / 2
                };
              }

              function screenToLatLon(sx, sy) {
                const w = rc.clientWidth, h = rc.clientHeight;
                const ws = 256 * Math.pow(2, state.z);
                return m2ll(
                  state.center.x + (sx - w / 2) / ws,
                  state.center.y + (sy - h / 2) / ws
                );
              }

              function drawLabel(text, x, y, bg, fg) {
                ctx.save();
                ctx.font = 'bold 11px sans-serif';
                const tw = ctx.measureText(text).width;
                const bw = tw + 12, bh = 20;
                ctx.fillStyle = bg || 'rgba(15, 107, 84, 0.92)';
                ctx.strokeStyle = '#ffd54f';
                ctx.lineWidth = 1.5;
                ctx.beginPath();
                ctx.roundRect(x - bw / 2, y - bh / 2, bw, bh, 6);
                ctx.fill();
                ctx.stroke();
                ctx.fillStyle = fg || '#ffffff';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText(text, x, y);
                ctx.restore();
              }

              function notifyBridge() {
                const pts = state.points;
                if (state.selectedPinIdx >= pts.length) state.selectedPinIdx = pts.length - 1;
                let area = 0, perimeter = 0;
                if (state.mode === 'circle' && pts.length >= 2) {
                  const r = haversineM(pts[0], pts[1]);
                  area = Math.PI * r * r;
                  perimeter = 2 * Math.PI * r;
                } else if (state.mode === 'distance') {
                  for (let i = 0; i < pts.length - 1; i++) perimeter += haversineM(pts[i], pts[i + 1]);
                } else {
                  if (pts.length >= 3) area = geodesicAreaSqM(pts);
                  const edges = pts.length === 2 ? 1 : (pts.length >= 3 ? pts.length : 0);
                  for (let i = 0; i < edges; i++) perimeter += haversineM(pts[i], pts[(i + 1) % pts.length]);
                }
                if (window.AndroidMapBridge) {
                  window.AndroidMapBridge.onMeasurementChanged(pts.length, area, perimeter, state.selectedPinIdx);
                }
              }

              function drawMapContent(w, h, includeTextLabels) {
                drawTileLayer(tileUrl, tileUrl, maxNativeZ);
                if (showEsriOverlay) {
                  drawTileLayer(ROADS_URL, 'esri_roads', 18);
                  drawTileLayer(PLACES_URL, 'esri_places', 18);
                }

                const pts = state.points;
                const spts = pts.map(toScreen);

                if (state.mode === 'circle' && pts.length >= 2) {
                  const radM = haversineM(pts[0], pts[1]);
                  const radPx = Math.hypot(spts[1].x - spts[0].x, spts[1].y - spts[0].y);
                  ctx.beginPath();
                  ctx.arc(spts[0].x, spts[0].y, radPx, 0, Math.PI * 2);
                  ctx.fillStyle = 'rgba(15, 157, 88, 0.30)';
                  ctx.fill();
                  ctx.lineWidth = 3;
                  ctx.strokeStyle = '#ffd54f';
                  ctx.stroke();

                  ctx.beginPath();
                  ctx.setLineDash([6, 5]);
                  ctx.moveTo(spts[0].x, spts[0].y);
                  ctx.lineTo(spts[1].x, spts[1].y);
                  ctx.stroke();
                  ctx.setLineDash([]);

                  if (includeTextLabels) {
                    drawLabel('R: ' + (radM * 3.28084).toFixed(1) + "'", (spts[0].x + spts[1].x) / 2, (spts[0].y + spts[1].y) / 2);
                  }
                } else if (state.mode === 'distance') {
                  if (spts.length >= 2) {
                    ctx.beginPath();
                    spts.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
                    ctx.lineWidth = 4;
                    ctx.strokeStyle = '#ffd54f';
                    ctx.stroke();
                    if (includeTextLabels) {
                      for (let i = 0; i < pts.length - 1; i++) {
                        const dM = haversineM(pts[i], pts[i + 1]);
                        drawLabel((dM * 3.28084).toFixed(1) + "'", (spts[i].x + spts[i + 1].x) / 2, (spts[i].y + spts[i + 1].y) / 2);
                      }
                    }
                  }
                } else {
                  if (spts.length === 2) {
                    ctx.beginPath();
                    ctx.setLineDash([6, 6]);
                    ctx.moveTo(spts[0].x, spts[0].y);
                    ctx.lineTo(spts[1].x, spts[1].y);
                    ctx.lineWidth = 3;
                    ctx.strokeStyle = '#ffd54f';
                    ctx.stroke();
                    ctx.setLineDash([]);
                    if (includeTextLabels) {
                      const dM = haversineM(pts[0], pts[1]);
                      drawLabel((dM * 3.28084).toFixed(1) + "'", (spts[0].x + spts[1].x) / 2, (spts[0].y + spts[1].y) / 2);
                    }
                  } else if (spts.length >= 3) {
                    ctx.beginPath();
                    spts.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
                    ctx.closePath();
                    ctx.fillStyle = 'rgba(15, 157, 88, 0.34)';
                    ctx.fill();
                    ctx.lineWidth = 3.5;
                    ctx.strokeStyle = '#ffd54f';
                    ctx.stroke();

                    if (includeTextLabels) {
                      for (let i = 0; i < pts.length; i++) {
                        const j = (i + 1) % pts.length;
                        const dM = haversineM(pts[i], pts[j]);
                        const segPx = Math.hypot(spts[j].x - spts[i].x, spts[j].y - spts[i].y);
                        if (segPx > 36) {
                          drawLabel((dM * 3.28084).toFixed(1) + "'", (spts[i].x + spts[j].x) / 2, (spts[i].y + spts[j].y) / 2);
                        }
                      }
                    }
                  }
                }

                // Draw draggable vertex pins (highlight selected pin)
                spts.forEach((p, idx) => {
                  const isSel = idx === state.selectedPinIdx;
                  if (isSel) {
                    ctx.beginPath();
                    ctx.arc(p.x, p.y, 14, 0, Math.PI * 2);
                    ctx.fillStyle = 'rgba(229, 57, 53, 0.28)';
                    ctx.fill();
                    ctx.lineWidth = 2.2;
                    ctx.strokeStyle = '#ff5252';
                    ctx.stroke();
                  }
                  ctx.beginPath();
                  ctx.arc(p.x, p.y, isSel ? 9.5 : 8.5, 0, Math.PI * 2);
                  ctx.fillStyle = isSel ? '#ff5252' : '#ffd54f';
                  ctx.fill();
                  ctx.lineWidth = 2.8;
                  ctx.strokeStyle = isSel ? '#ffffff' : '#0f6b54';
                  ctx.stroke();
                });

                // Draw user GPS dot
                if (state.userLoc) {
                  const u = toScreen(state.userLoc);
                  ctx.beginPath();
                  ctx.arc(u.x, u.y, 14, 0, Math.PI * 2);
                  ctx.fillStyle = 'rgba(26, 115, 232, 0.25)';
                  ctx.fill();
                  ctx.beginPath();
                  ctx.arc(u.x, u.y, 7.5, 0, Math.PI * 2);
                  ctx.fillStyle = '#1a73e8';
                  ctx.fill();
                  ctx.lineWidth = 2.5;
                  ctx.strokeStyle = '#ffffff';
                  ctx.stroke();
                }
              }

              function draw() {
                const w = rc.clientWidth, h = rc.clientHeight;
                if (!w || !h) return;
                const ratio = dpr();
                const cw = Math.round(w * ratio), ch = Math.round(h * ratio);
                if (rc.width !== cw || rc.height !== ch) { rc.width = cw; rc.height = ch; }
                ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
                ctx.imageSmoothingEnabled = true;
                ctx.imageSmoothingQuality = 'high';

                // Subtle background behind loading tiles
                ctx.fillStyle = '#182923';
                ctx.fillRect(0, 0, w, h);

                drawMapContent(w, h, true);

                // Live 3.0x Circular Magnifying Loupe showing HD zoomed imagery under pointer
                if (state.pointerMode && w > 180 && h > 220) {
                  const lx = w / 2;
                  const lr = 54;
                  const ly = Math.min(92, Math.max(68, lr + 18));
                  const zoomFactor = 3.0;
                  ctx.save();
                  ctx.beginPath();
                  ctx.arc(lx, ly, lr, 0, Math.PI * 2);
                  ctx.clip();
                  ctx.fillStyle = '#182923';
                  ctx.fillRect(lx - lr, ly - lr, lr * 2, lr * 2);
                  ctx.translate(lx, ly);
                  ctx.scale(zoomFactor, zoomFactor);
                  ctx.translate(-w / 2, -h / 2);
                  drawMapContent(w, h, false);
                  ctx.restore();

                  // Crisp inner reticle inside loupe
                  ctx.save();
                  ctx.strokeStyle = 'rgba(229, 57, 53, 0.92)';
                  ctx.lineWidth = 1.6;
                  ctx.beginPath();
                  ctx.moveTo(lx - lr + 8, ly); ctx.lineTo(lx - 7, ly);
                  ctx.moveTo(lx + 7, ly); ctx.lineTo(lx + lr - 8, ly);
                  ctx.moveTo(lx, ly - lr + 8); ctx.lineTo(lx, ly - 7);
                  ctx.moveTo(lx, ly + 7); ctx.lineTo(lx, ly + lr - 8);
                  ctx.stroke();
                  ctx.beginPath();
                  ctx.arc(lx, ly, 3.2, 0, Math.PI * 2);
                  ctx.fillStyle = '#e53935';
                  ctx.fill();

                  // Loupe border & badge
                  ctx.beginPath();
                  ctx.arc(lx, ly, lr, 0, Math.PI * 2);
                  ctx.lineWidth = 3.2;
                  ctx.strokeStyle = '#ffd54f';
                  ctx.stroke();
                  ctx.beginPath();
                  ctx.arc(lx, ly, lr + 2, 0, Math.PI * 2);
                  ctx.lineWidth = 1.4;
                  ctx.strokeStyle = 'rgba(0,0,0,0.55)';
                  ctx.stroke();

                  ctx.font = 'bold 10px sans-serif';
                  const tag = '3x জুম';
                  const tw = ctx.measureText(tag).width + 10;
                  ctx.fillStyle = 'rgba(15, 107, 84, 0.92)';
                  ctx.beginPath();
                  ctx.roundRect(lx - tw / 2, ly + lr - 13, tw, 16, 8);
                  ctx.fill();
                  ctx.fillStyle = '#ffffff';
                  ctx.textAlign = 'center';
                  ctx.textBaseline = 'middle';
                  ctx.fillText(tag, lx, ly + lr - 5);
                  ctx.restore();
                }

                // Scale bar at bottom-left
                const centerLL = m2ll(state.center.x, state.center.y);
                const mpp = 156543.03392 * Math.cos(centerLL.lat * Math.PI / 180) / Math.pow(2, state.z);
                const steps = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 50000];
                const barM = steps.find(v => v / mpp >= 65) || 1000;
                const barPx = barM / mpp;
                ctx.fillStyle = 'rgba(255,255,255,0.92)';
                ctx.fillRect(12, h - 22, barPx, 3);
                ctx.fillRect(12, h - 27, 2, 8);
                ctx.fillRect(12 + barPx - 2, h - 27, 2, 8);
                ctx.font = 'bold 11px sans-serif';
                ctx.strokeStyle = 'rgba(0,0,0,0.8)';
                ctx.lineWidth = 3;
                const barLbl = barM >= 1000 ? (barM / 1000) + ' km' : barM + ' m';
                ctx.strokeText(barLbl, 14, h - 29);
                ctx.fillText(barLbl, 14, h - 29);

                attrEl.textContent = tileAttr + ' • Z' + state.z.toFixed(1);
              }

              // Touch / pointer gesture engine (Pan + Pinch Zoom + Drag Pin + Tap to Add)
              const pointers = new Map();
              let dragStart = null;
              let draggingPinIdx = -1;

              function rel(e) {
                const r = rc.getBoundingClientRect();
                return { x: e.clientX - r.left, y: e.clientY - r.top };
              }
              function zoomAround(dz, px, py) {
                const w = rc.clientWidth, h = rc.clientHeight;
                const ws1 = 256 * Math.pow(2, state.z);
                const nextZ = Math.max(3, Math.min(maxViewZ, state.z + dz));
                const ws2 = 256 * Math.pow(2, nextZ);
                const mx = state.center.x + (px - w / 2) / ws1;
                const my = state.center.y + (py - h / 2) / ws1;
                state.z = nextZ;
                state.center.x = mx - (px - w / 2) / ws2;
                state.center.y = my - (py - h / 2) / ws2;
              }

              rc.addEventListener('pointerdown', e => {
                rc.setPointerCapture(e.pointerId);
                const pt = rel(e);
                pointers.set(e.pointerId, pt);
                if (pointers.size === 1) {
                  dragStart = { x: pt.x, y: pt.y, t: Date.now(), mv: 0 };
                  draggingPinIdx = -1;
                  // Check if user grabbed an existing corner vertex pin
                  let bestDist = 26;
                  state.points.forEach((ll, idx) => {
                    const sp = toScreen(ll);
                    const d = Math.hypot(sp.x - pt.x, sp.y - pt.y);
                    if (d < bestDist) { bestDist = d; draggingPinIdx = idx; }
                  });
                } else {
                  dragStart.mv = 999;
                  draggingPinIdx = -1;
                }
              });

              rc.addEventListener('pointermove', e => {
                const prev = pointers.get(e.pointerId);
                if (!prev) return;
                const curr = rel(e);
                const ws = 256 * Math.pow(2, state.z);

                if (pointers.size === 1) {
                  dragStart.mv = Math.max(dragStart.mv, Math.hypot(curr.x - dragStart.x, curr.y - dragStart.y));
                  if (draggingPinIdx >= 0) {
                    state.selectedPinIdx = draggingPinIdx;
                    state.points[draggingPinIdx] = screenToLatLon(curr.x, curr.y);
                    notifyBridge();
                  } else {
                    state.center.x -= (curr.x - prev.x) / ws;
                    state.center.y -= (curr.y - prev.y) / ws;
                  }
                } else if (pointers.size === 2) {
                  const other = [...pointers.entries()].find(([id]) => id !== e.pointerId)[1];
                  const prevMid = { x: (prev.x + other.x) / 2, y: (prev.y + other.y) / 2 };
                  const currMid = { x: (curr.x + other.x) / 2, y: (curr.y + other.y) / 2 };
                  const d1 = Math.hypot(prev.x - other.x, prev.y - other.y) || 1;
                  const d2 = Math.hypot(curr.x - other.x, curr.y - other.y) || 1;
                  dragStart.mv = 999;
                  state.center.x -= (currMid.x - prevMid.x) / ws;
                  state.center.y -= (currMid.y - prevMid.y) / ws;
                  zoomAround(Math.log2(d2 / d1), currMid.x, currMid.y);
                }

                prev.x = curr.x; prev.y = curr.y;
                scheduleDraw();
              });

              function onUp(e) {
                if (!pointers.has(e.pointerId)) return;
                const pt = rel(e);
                const wasLast = pointers.size === 1;
                pointers.delete(e.pointerId);
                if (wasLast && dragStart && dragStart.mv < 9 && (Date.now() - dragStart.t) < 480 && e.type === 'pointerup') {
                  if (draggingPinIdx >= 0) {
                    state.selectedPinIdx = (state.selectedPinIdx === draggingPinIdx) ? -1 : draggingPinIdx;
                  } else {
                    const ll = screenToLatLon(pt.x, pt.y);
                    if (state.mode === 'circle' && state.points.length >= 2) {
                      state.points[1] = ll;
                      state.selectedPinIdx = 1;
                    } else {
                      state.points.push(ll);
                      state.selectedPinIdx = state.points.length - 1;
                    }
                  }
                  notifyBridge();
                  scheduleDraw();
                } else if (wasLast && draggingPinIdx >= 0) {
                  state.selectedPinIdx = draggingPinIdx;
                  notifyBridge();
                  scheduleDraw();
                }
                if (wasLast) draggingPinIdx = -1;
              }
              rc.addEventListener('pointerup', onUp);
              rc.addEventListener('pointercancel', onUp);
              rc.addEventListener('wheel', e => {
                e.preventDefault();
                const pt = rel(e);
                const dz = e.deltaY < 0 ? 0.35 : -0.35;
                zoomAround(dz, pt.x, pt.y);
                scheduleDraw();
              }, { passive: false });
              window.addEventListener('resize', scheduleDraw);

              window.bhumiMap = {
                resize: () => scheduleDraw(),
                setPointerMode: (enabled) => {
                  state.pointerMode = !!enabled;
                  scheduleDraw();
                },
                setTileLayer: (url, attr, nativeMax, viewMax, overlay) => {
                  tileUrl = url;
                  tileAttr = attr;
                  maxNativeZ = nativeMax || 18;
                  maxViewZ = viewMax || 23;
                  showEsriOverlay = !!overlay;
                  scheduleDraw();
                },
                setMode: (newMode) => {
                  state.mode = newMode;
                  notifyBridge();
                  scheduleDraw();
                },
                zoomBy: (dz) => {
                  zoomAround(dz, rc.clientWidth / 2, rc.clientHeight / 2);
                  scheduleDraw();
                },
                addPoint: (lat, lon) => {
                  if (state.mode === 'circle' && state.points.length >= 2) {
                    state.points[1] = { lat, lon };
                    state.selectedPinIdx = 1;
                  } else {
                    state.points.push({ lat, lon });
                    state.selectedPinIdx = state.points.length - 1;
                  }
                  notifyBridge();
                  scheduleDraw();
                },
                addCenterPoint: () => {
                  const ll = screenToLatLon(rc.clientWidth / 2, rc.clientHeight / 2);
                  if (state.mode === 'circle' && state.points.length >= 2) {
                    state.points[1] = ll;
                    state.selectedPinIdx = 1;
                  } else {
                    state.points.push(ll);
                    state.selectedPinIdx = state.points.length - 1;
                  }
                  notifyBridge();
                  scheduleDraw();
                },
                deleteSelectedPin: () => {
                  if (!state.points.length) return;
                  const idx = (state.selectedPinIdx >= 0 && state.selectedPinIdx < state.points.length)
                    ? state.selectedPinIdx : (state.points.length - 1);
                  state.points.splice(idx, 1);
                  state.selectedPinIdx = state.points.length ? Math.min(idx, state.points.length - 1) : -1;
                  notifyBridge();
                  scheduleDraw();
                },
                moveSelectedPinToCenter: () => {
                  if (state.selectedPinIdx < 0 || state.selectedPinIdx >= state.points.length) return;
                  state.points[state.selectedPinIdx] = screenToLatLon(rc.clientWidth / 2, rc.clientHeight / 2);
                  notifyBridge();
                  scheduleDraw();
                },
                flyTo: (lat, lon, zoom, showPin) => {
                  state.center = ll2m(lat, lon);
                  state.z = Math.min(maxViewZ, zoom || 17);
                  if (showPin) state.userLoc = { lat, lon };
                  scheduleDraw();
                },
                updateUserLoc: (lat, lon) => {
                  state.userLoc = { lat, lon };
                  scheduleDraw();
                },
                undo: () => {
                  if (state.points.length) state.points.pop();
                  state.selectedPinIdx = state.points.length - 1;
                  notifyBridge();
                  scheduleDraw();
                },
                clearAll: () => {
                  state.points = [];
                  state.selectedPinIdx = -1;
                  notifyBridge();
                  scheduleDraw();
                },
                fitSelectedBounds: () => {
                  if (!state.points.length) return;
                  const ms = state.points.map(p => ll2m(p.lat, p.lon));
                  const mnx = Math.min(...ms.map(m => m.x)), mxx = Math.max(...ms.map(m => m.x));
                  const mny = Math.min(...ms.map(m => m.y)), mxy = Math.max(...ms.map(m => m.y));
                  state.center = { x: (mnx + mxx) / 2, y: (mny + mxy) / 2 };
                  const spanX = Math.max(mxx - mnx, 1e-7), spanY = Math.max(mxy - mny, 1e-7);
                  const w = rc.clientWidth * 0.72, h = rc.clientHeight * 0.72;
                  const zx = Math.log2(w / (256 * spanX));
                  const zy = Math.log2(h / (256 * spanY));
                  state.z = Math.max(4, Math.min(maxViewZ, Math.min(zx, zy)));
                  draw();
                },
                getSelectedCropRect: () => {
                  if (!state.points.length) return null;
                  const spts = state.points.map(toScreen);
                  const w = rc.clientWidth || 1, h = rc.clientHeight || 1;
                  const pad = 42;
                  const minX = Math.min(...spts.map(p => p.x)) - pad;
                  const minY = Math.min(...spts.map(p => p.y)) - pad;
                  const maxX = Math.max(...spts.map(p => p.x)) + pad;
                  const maxY = Math.max(...spts.map(p => p.y)) + pad;
                  return JSON.stringify({
                    left: Math.max(0, minX / w),
                    top: Math.max(0, minY / h),
                    right: Math.min(1, maxX / w),
                    bottom: Math.min(1, maxY / h)
                  });
                },
                getExportPayload: () => {
                  const spts = state.points.map(toScreen);
                  const w = rc.clientWidth || 1, h = rc.clientHeight || 1;
                  const pad = 42;
                  let cropRect = null;
                  if (spts.length) {
                    const minX = Math.min(...spts.map(p => p.x)) - pad;
                    const minY = Math.min(...spts.map(p => p.y)) - pad;
                    const maxX = Math.max(...spts.map(p => p.x)) + pad;
                    const maxY = Math.max(...spts.map(p => p.y)) + pad;
                    cropRect = {
                      left: Math.max(0, minX / w),
                      top: Math.max(0, minY / h),
                      right: Math.min(1, maxX / w),
                      bottom: Math.min(1, maxY / h)
                    };
                  }
                  return JSON.stringify({
                    mode: state.mode,
                    points: state.points,
                    cropRect: cropRect
                  });
                }
              };

              // Immediate initial render
              scheduleDraw();
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
}
