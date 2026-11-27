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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Pentagon
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.MouzaScale
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.GpsAveragingDialog
import com.example.ui.components.ImportedParcelsSheet
import com.example.ui.components.KhatianLookupBottomSheet
import com.example.ui.components.KmzDemarcationExportDialog
import com.example.ui.components.MapCoordinateToolboxDialog
import com.example.ui.components.NeuPillChip
import com.example.ui.components.PrecisionNudgePad
import com.example.ui.components.UtilityBarChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.RealMapViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "MissingPermission")
@Composable
fun RealMapScreen(
    onNavigateBack: () -> Unit,
    savedProjectId: String? = null,
    viewModel: RealMapViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var showScaleMenu by remember { mutableStateOf(false) }
    var showOverlayBar by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    var pointerModeEnabled by remember { mutableStateOf(true) }
    var loupeEnabled by remember { mutableStateOf(true) }
    var loupeZoomLevel by remember { mutableDoubleStateOf(3.0) }
    var showNudgePad by remember { mutableStateOf(false) }
    var nudgeTargetIsPoint by remember { mutableStateOf(true) }
    var nudgeStepLevel by remember { mutableIntStateOf(0) }
    var showKhatianSheet by remember { mutableStateOf(false) }
    var showSavedProjectsSheet by remember { mutableStateOf(false) }
    var showSaveProjectDialog by remember { mutableStateOf(false) }
    var showCoordinateDialog by remember { mutableStateOf(false) }
    var showKmzDialog by remember { mutableStateOf(false) }
    var showGpsAvgDialog by remember { mutableStateOf(false) }
    var showImportedParcelsSheet by remember { mutableStateOf(false) }
    var currentCenterLat by remember { mutableDoubleStateOf(23.8103) }
    var currentCenterLon by remember { mutableDoubleStateOf(90.4125) }
    var localToastMessage by remember { mutableStateOf<String?>(null) }

    val khatianPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openKhatianDocumentUri(it) }
    }

    val mouzaOverlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.openMouzaOverlayDocumentUri(it)
            showOverlayBar = true
        }
    }

    val spatialFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importSpatialDocumentUri(it) }
    }

    BackHandler(
        enabled = showSavedProjectsSheet || showSaveProjectDialog || showKhatianSheet ||
            showCoordinateDialog || showKmzDialog || showGpsAvgDialog || showImportedParcelsSheet
    ) {
        when {
            showSaveProjectDialog -> showSaveProjectDialog = false
            showCoordinateDialog -> showCoordinateDialog = false
            showKmzDialog -> showKmzDialog = false
            showGpsAvgDialog -> showGpsAvgDialog = false
            showImportedParcelsSheet -> showImportedParcelsSheet = false
            showKhatianSheet -> showKhatianSheet = false
            showSavedProjectsSheet -> showSavedProjectsSheet = false
        }
    }

    LaunchedEffect(savedProjectId) {
        if (!savedProjectId.isNullOrBlank()) {
            viewModel.restoreMapProjectById(savedProjectId)
        }
    }

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
    LaunchedEffect(pointerModeEnabled, loupeEnabled, loupeZoomLevel, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setLoupeConfig(${pointerModeEnabled && loupeEnabled}, $loupeZoomLevel);",
                null
            )
        }
    }
    LaunchedEffect(state.isPlotLocked, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setLocked(${state.isPlotLocked});",
                null
            )
        }
    }
    LaunchedEffect(state.selectedMouzaScale, state.showMouzaGrid, mapReady) {
        if (mapReady) {
            val s = state.selectedMouzaScale
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setMouzaScale('${s.ratioLabel}', ${s.feetPerInch}, ${state.showMouzaGrid});",
                null
            )
        }
    }
    LaunchedEffect(state.hdSharpMode, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setHdSharp(${state.hdSharpMode});",
                null
            )
        }
    }
    LaunchedEffect(state.mouzaOverlayDataUrl, mapReady) {
        if (mapReady) {
            val data = state.mouzaOverlayDataUrl
            if (data.isNullOrBlank()) {
                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.clearMouzaOverlay();", null)
            } else {
                val jsonUrl = JSONObject.quote(data)
                webViewRef?.evaluateJavascript(
                    "window.bhumiMap && window.bhumiMap.setMouzaOverlayImage($jsonUrl, ${state.mouzaOverlayWidthMeters}, ${state.mouzaOverlayOpacity}, ${state.mouzaOverlayRotationDeg}, ${state.mouzaOverlayLocked});",
                    null
                )
            }
        }
    }
    LaunchedEffect(
        state.mouzaOverlayWidthMeters,
        state.mouzaOverlayOpacity,
        state.mouzaOverlayRotationDeg,
        state.mouzaOverlayLocked,
        mapReady
    ) {
        if (mapReady && !state.mouzaOverlayDataUrl.isNullOrBlank()) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.updateMouzaOverlayParams(${state.mouzaOverlayWidthMeters}, ${state.mouzaOverlayOpacity}, ${state.mouzaOverlayRotationDeg}, ${state.mouzaOverlayLocked});",
                null
            )
        }
    }
    LaunchedEffect(state.pendingRestoreProject, mapReady) {
        val proj = state.pendingRestoreProject ?: return@LaunchedEffect
        if (mapReady) {
            val ptsArr = JSONArray()
            proj.mapPoints.forEach { p ->
                ptsArr.put(JSONObject().apply {
                    put("lat", p.lat)
                    put("lon", p.lon)
                })
            }
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.restoreState(${proj.mapCenterLat}, ${proj.mapCenterLon}, ${proj.mapZoom}, '${proj.mapCategoryId}', $ptsArr, ${proj.mapPlotLocked});",
                null
            )
            viewModel.consumePendingRestore()
        }
    }
    LaunchedEffect(state.hideMeasures, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setHideMeasures(${state.hideMeasures});",
                null
            )
        }
    }
    LaunchedEffect(state.parcelSnapEnabled, mapReady) {
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setParcelSnapMode(${state.parcelSnapEnabled});",
                null
            )
        }
    }
    LaunchedEffect(state.importedParcels, mapReady) {
        if (mapReady) {
            val arr = JSONArray()
            state.importedParcels.forEach { p ->
                val pts = JSONArray()
                p.pointsLatLon.forEach { (la, lo) ->
                    pts.put(JSONObject().apply { put("lat", la); put("lon", lo) })
                }
                arr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("dag", p.dagNumber)
                    put("decimal", p.decimal)
                    put("points", pts)
                })
            }
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setImportedParcels($arr);",
                null
            )
        }
    }
    LaunchedEffect(state.pendingFlyToLatLon, mapReady) {
        val target = state.pendingFlyToLatLon ?: return@LaunchedEffect
        if (mapReady) {
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.flyTo(${target.first}, ${target.second}, 17, false);",
                null
            )
            viewModel.consumePendingFlyTo()
        }
    }
    LaunchedEffect(state.pendingSnapParcelPoints, mapReady) {
        val pts = state.pendingSnapParcelPoints ?: return@LaunchedEffect
        if (mapReady) {
            val arr = JSONArray()
            pts.forEach { (la, lo) ->
                arr.put(JSONObject().apply { put("lat", la); put("lon", lo) })
            }
            webViewRef?.evaluateJavascript(
                "window.bhumiMap && window.bhumiMap.setPolygonPoints($arr);",
                null
            )
            viewModel.consumePendingSnapParcel()
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

                // Separate Khatian Upload & Search Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_khatian_button")
                        .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = { showKhatianSheet = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = "Upload Khatian & Search Dag",
                        tint = if (state.khatianPreviewBitmap != null || state.khatianRecords.isNotEmpty()) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Saved Map Projects Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_saved_projects_button")
                        .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = { showSavedProjectsSheet = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Saved Maps", tint = MaterialTheme.colorScheme.primary)
                }

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

            // Fixed Top Utility Bar 2 (keeps Zoom, Loupe, Roads/Labels, Khatian, Layer, Fine-Nudge OUTSIDE the map!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilityBarChip(
                    label = "-",
                    icon = Icons.Rounded.Remove,
                    selected = false,
                    onClick = {
                        webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.zoomBy(-1);", null)
                    }
                )
                UtilityBarChip(
                    label = "+",
                    icon = Icons.Rounded.Add,
                    selected = false,
                    onClick = {
                        webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.zoomBy(1);", null)
                    }
                )
                UtilityBarChip(
                    label = if (!loupeEnabled) "লুপ বন্ধ" else "লুপ ${loupeZoomLevel}x",
                    icon = Icons.Rounded.ZoomIn,
                    selected = loupeEnabled && pointerModeEnabled,
                    testTag = "map_loupe_toggle",
                    onClick = {
                        if (!pointerModeEnabled) {
                            pointerModeEnabled = true
                            loupeEnabled = true
                        } else if (!loupeEnabled) {
                            loupeEnabled = true
                            loupeZoomLevel = 3.0
                        } else if (loupeZoomLevel < 4.4) {
                            loupeZoomLevel = 4.5
                        } else {
                            loupeEnabled = false
                        }
                    }
                )
                if (state.selectedLayer.supportsLabelOverlay) {
                    UtilityBarChip(
                        label = if (state.showLabelsOverlay) "নাম চালু" else "নাম বন্ধ",
                        icon = Icons.Rounded.Map,
                        selected = state.showLabelsOverlay,
                        onClick = { viewModel.toggleLabelsOverlay() }
                    )
                }
                Box {
                    UtilityBarChip(
                        label = state.selectedMouzaScale.ratioLabel,
                        icon = Icons.Rounded.Tune,
                        selected = state.showMouzaGrid || state.mouzaOverlayDataUrl != null,
                        testTag = "map_mouza_scale_chip",
                        onClick = { showScaleMenu = true }
                    )
                    DropdownMenu(
                        expanded = showScaleMenu,
                        onDismissRequest = { showScaleMenu = false }
                    ) {
                        MouzaScale.entries.forEach { sc ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${sc.labelBn} (${sc.ratioLabel})",
                                        fontWeight = if (sc == state.selectedMouzaScale) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sc == state.selectedMouzaScale) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.selectMouzaScale(sc)
                                    showScaleMenu = false
                                }
                            )
                        }
                    }
                }
                UtilityBarChip(
                    label = "চেইন গ্রিড",
                    icon = Icons.Rounded.GridOn,
                    selected = state.showMouzaGrid,
                    testTag = "map_mouza_grid_toggle",
                    onClick = { viewModel.toggleMouzaGrid() }
                )
                UtilityBarChip(
                    label = "নকশা ওভারলে",
                    icon = Icons.Rounded.AddPhotoAlternate,
                    selected = showOverlayBar || state.mouzaOverlayDataUrl != null,
                    testTag = "map_mouza_overlay_toggle",
                    onClick = {
                        if (state.mouzaOverlayDataUrl == null) {
                            mouzaOverlayLauncher.launch(arrayOf("application/pdf", "image/*"))
                        } else {
                            showOverlayBar = !showOverlayBar
                        }
                    }
                )
                UtilityBarChip(
                    label = "HD শার্প",
                    icon = Icons.Rounded.AutoFixHigh,
                    selected = state.hdSharpMode,
                    testTag = "map_hd_sharp_toggle",
                    onClick = { viewModel.toggleHdSharpMode() }
                )
                UtilityBarChip(
                    label = state.selectedLayer.titleEn,
                    icon = Icons.Rounded.Layers,
                    selected = false,
                    onClick = { showLayerMenu = true }
                )
                UtilityBarChip(
                    label = "খতিয়ান",
                    icon = Icons.Rounded.Description,
                    selected = state.khatianRecords.isNotEmpty() || state.khatianPreviewBitmap != null,
                    onClick = { showKhatianSheet = true }
                )
                UtilityBarChip(
                    label = "সূক্ষ্ম মুভ",
                    icon = Icons.Rounded.OpenWith,
                    selected = showNudgePad,
                    testTag = "map_nudge_toggle",
                    onClick = { showNudgePad = !showNudgePad }
                )
                UtilityBarChip(
                    label = "GPS",
                    icon = Icons.Rounded.MyLocation,
                    selected = false,
                    testTag = "map_gps_button",
                    onClick = { requestOrUseLocation(addPointIfWalking = false) }
                )
                if (state.pointCount > 0) {
                    UtilityBarChip(
                        label = if (state.isPlotLocked) "লকড (আনলক)" else "প্লট লক",
                        icon = if (state.isPlotLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        selected = state.isPlotLocked,
                        testTag = "map_lock_toggle",
                        onClick = { viewModel.togglePlotLock() }
                    )
                }
                UtilityBarChip(
                    label = if (state.importedParcels.isEmpty()) "KML/GeoJSON" else "পার্সেল (${state.importedParcels.size})",
                    icon = Icons.Rounded.UploadFile,
                    selected = state.importedParcels.isNotEmpty(),
                    testTag = "map_import_spatial_chip",
                    onClick = {
                        if (state.importedParcels.isEmpty()) spatialFileLauncher.launch(arrayOf("*/*"))
                        else showImportedParcelsSheet = true
                    }
                )
                UtilityBarChip(
                    label = "পার্সেল স্ন্যাপ",
                    icon = Icons.Rounded.AutoFixHigh,
                    selected = state.parcelSnapEnabled,
                    testTag = "map_parcel_snap_chip",
                    onClick = { viewModel.toggleParcelSnapMode() }
                )
                UtilityBarChip(
                    label = "মাপ লুকান",
                    icon = Icons.Rounded.VisibilityOff,
                    selected = state.hideMeasures,
                    testTag = "map_hide_measures_chip",
                    onClick = { viewModel.toggleHideMeasures() }
                )
                UtilityBarChip(
                    label = "স্থানাঙ্ক",
                    icon = Icons.Rounded.Explore,
                    selected = showCoordinateDialog,
                    testTag = "map_coordinates_chip",
                    onClick = {
                        val wv = webViewRef
                        if (wv != null) {
                            wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.getFullState();") { raw ->
                                val parsed = parseFullMapState(raw)
                                currentCenterLat = parsed.centerLat
                                currentCenterLon = parsed.centerLon
                                showCoordinateDialog = true
                            }
                        } else {
                            showCoordinateDialog = true
                        }
                    }
                )
                UtilityBarChip(
                    label = "১০সে. GPS",
                    icon = Icons.Rounded.MyLocation,
                    selected = showGpsAvgDialog,
                    testTag = "map_gps_avg_chip",
                    onClick = { showGpsAvgDialog = true }
                )
                UtilityBarChip(
                    label = "সেটব্যাক",
                    icon = Icons.Rounded.Route,
                    selected = state.roadSetbackResultFt != null,
                    testTag = "map_setback_chip",
                    onClick = {
                        webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.getExportPayload();") { raw ->
                            viewModel.computeRoadSetbackFromPoints(parseExportPayload(raw).points)
                        }
                    }
                )
                UtilityBarChip(
                    label = "স্প্লিট সিঙ্ক",
                    icon = Icons.Rounded.Compare,
                    selected = state.splitMouzaSyncEnabled,
                    testTag = "map_split_sync_chip",
                    onClick = { viewModel.toggleSplitMouzaSync() }
                )
                UtilityBarChip(
                    label = "KMZ রপ্তানি",
                    icon = Icons.Rounded.ShareLocation,
                    selected = showKmzDialog,
                    testTag = "map_kmz_export_chip",
                    onClick = { showKmzDialog = true }
                )
            }

            // Optional Split Mouza Side-by-Side Sync Panel when enabled
            val splitBmp = remember(state.mouzaOverlayDataUrl, state.splitMouzaSyncEnabled) {
                val url = state.mouzaOverlayDataUrl
                if (state.splitMouzaSyncEnabled && !url.isNullOrBlank() && url.contains(",")) {
                    runCatching {
                        val rawBytes = android.util.Base64.decode(url.substringAfter(","), android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    }.getOrNull()
                } else null
            }
            if (state.splitMouzaSyncEnabled && splitBmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .neuSurface(cornerRadius = 16.dp, elevation = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .pointerInput(splitBmp) {
                            detectTapGestures { tap ->
                                val nx = (tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) - 0.5f
                                val ny = (tap.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f) - 0.5f
                                webViewRef?.evaluateJavascript(
                                    "window.bhumiMap && window.bhumiMap.syncMoveRelativeToOverlay($nx, $ny);",
                                    null
                                )
                            }
                        }
                ) {
                    Image(
                        bitmap = splitBmp.asImageBitmap(),
                        contentDescription = "Split Mouza Sync Pane",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "মৌজা সিঙ্ক (ট্যাপ করলে স্যাটেলাইট সেখানে সরবে)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Unobstructed Zero-CDN Self-Contained Interactive Mercator Tile Engine WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
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

                                    @JavascriptInterface
                                    fun onLockedEditBlocked() {
                                        viewModel.notifyLockedEditBlocked()
                                    }

                                    @JavascriptInterface
                                    fun onSnapMapTap(lat: Double, lon: Double) {
                                        viewModel.handleMapTapInSnapMode(lat, lon)
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
                }
            }

            // Active Khatian Target Banner (outside map)
            AnimatedVisibility(
                visible = state.activeSearchTarget != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                state.activeSearchTarget?.let { (kh, dag) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "লক্ষ্য: খতিয়ান ${kh.ifBlank { "-" }} • দাগ $dag (ম্যাপে পিন বসিয়ে সেভ করুন)",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearActiveSearchTarget() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Search Target", modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        }
                    }
                }
            }

            // Docked Contextual Action Row BELOW map (keeps satellite map clear!)
            val showMapActionRow = pointerModeEnabled ||
                state.selectedCategory == MapMeasureCategory.GPS_WALK ||
                state.selectedPinIdx >= 0 ||
                state.pointCount > 0
            AnimatedVisibility(
                visible = showMapActionRow,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.pointCount > 0) {
                        UtilityBarChip(
                            label = if (state.isPlotLocked) "লকড • আনলক করুন" else "পরিমাপ লক করুন",
                            icon = if (state.isPlotLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            selected = state.isPlotLocked,
                            testTag = "map_docked_lock_button",
                            onClick = { viewModel.togglePlotLock() }
                        )
                    }

                    if (pointerModeEnabled) {
                        Button(
                            onClick = {
                                if (state.isPlotLocked) {
                                    viewModel.notifyLockedEditBlocked()
                                } else {
                                    webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.addCenterPoint();", null)
                                }
                            },
                            modifier = Modifier.testTag("map_pointer_add_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isPlotLocked) Color(0xFF78909C) else EmeraldPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                if (state.isPlotLocked) Icons.Rounded.Lock else Icons.Rounded.GpsFixed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (state.isPlotLocked) "প্লট লকড" else "পয়েন্টারে পিন বসান",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }

                    if (state.selectedPinIdx >= 0 && !state.isPlotLocked) {
                        Button(
                            onClick = {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.moveSelectedPinToCenter();", null)
                            },
                            modifier = Modifier.testTag("map_move_pin_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Text("পিন #${state.selectedPinIdx + 1} এখানে আনুন", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }

                    if (state.selectedCategory == MapMeasureCategory.GPS_WALK) {
                        Button(
                            onClick = {
                                if (state.isPlotLocked) {
                                    viewModel.notifyLockedEditBlocked()
                                } else {
                                    if (!state.isGpsWalkActive) {
                                        viewModel.setGpsWalkActive(true)
                                    }
                                    requestOrUseLocation(addPointIfWalking = true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isPlotLocked) Color(0xFF78909C) else EmeraldPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                if (state.isPlotLocked) Icons.Rounded.Lock else Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("GPS পয়েন্ট যোগ", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }

            // Docked Fine-Tune Nudge D-Pad Controller (placed cleanly BELOW the map so map stays 100% clear)
            val mapHasPoints = state.pointCount > 0
            val mapTargetPoint = nudgeTargetIsPoint && mapHasPoints
            val mapNudgeLabel = if (!mapTargetPoint) {
                "ক্রসহেয়ার"
            } else if (state.selectedPinIdx >= 0) {
                "পিন #${state.selectedPinIdx + 1}"
            } else {
                "পিন #${state.pointCount}"
            }

            // Docked Old Mouza Overlay Alignment Panel (below map, keeps Google Satellite 100% clear)
            AnimatedVisibility(
                visible = showOverlayBar && state.mouzaOverlayDataUrl != null,
                enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 18.dp, elevation = 6.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পুরাতন মৌজা ওভারলে (${state.selectedMouzaScale.ratioLabel})",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (state.mouzaOverlayPageCount > 1) {
                                IconButton(
                                    onClick = { viewModel.selectMouzaOverlayPage(state.mouzaOverlayCurrentPage - 1) },
                                    enabled = state.mouzaOverlayCurrentPage > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev Overlay Page", modifier = Modifier.size(18.dp))
                                }
                                Text("${state.mouzaOverlayCurrentPage + 1}/${state.mouzaOverlayPageCount}", style = MaterialTheme.typography.labelSmall)
                                IconButton(
                                    onClick = { viewModel.selectMouzaOverlayPage(state.mouzaOverlayCurrentPage + 1) },
                                    enabled = state.mouzaOverlayCurrentPage < state.mouzaOverlayPageCount - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Overlay Page", modifier = Modifier.size(18.dp))
                                }
                            }
                            TextButton(
                                onClick = { webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.centerMouzaOverlayHere();", null) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("কেন্দ্রে আনুন", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = { viewModel.toggleMouzaOverlayLock() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.mouzaOverlayLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = "Lock Overlay",
                                    tint = if (state.mouzaOverlayLocked) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { mouzaOverlayLauncher.launch(arrayOf("application/pdf", "image/*")) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Change Mouza Overlay", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    viewModel.clearMouzaOverlay()
                                    showOverlayBar = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove Overlay", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("স্বচ্ছতা", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(46.dp))
                        Slider(
                            value = state.mouzaOverlayOpacity,
                            onValueChange = { viewModel.setMouzaOverlayOpacity(it) },
                            valueRange = 0.08f..0.95f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                        Text("আকার", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
                        Slider(
                            value = state.mouzaOverlayWidthMeters.toFloat(),
                            onValueChange = { viewModel.setMouzaOverlayWidthMeters(it.toDouble()) },
                            valueRange = 60f..1800f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ঘূর্ণন ${state.mouzaOverlayRotationDeg.toInt()}°", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(62.dp))
                        Slider(
                            value = state.mouzaOverlayRotationDeg,
                            onValueChange = { viewModel.setMouzaOverlayRotation(it) },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showNudgePad,
                enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                PrecisionNudgePad(
                    targetIsPoint = mapTargetPoint,
                    hasAnyPoint = mapHasPoints,
                    activePointLabel = mapNudgeLabel,
                    stepLevel = nudgeStepLevel,
                    onStepLevelChange = { nudgeStepLevel = it },
                    onToggleTargetMode = { nudgeTargetIsPoint = it },
                    onCyclePoint = { delta ->
                        nudgeTargetIsPoint = true
                        webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.cycleSelectedPin($delta);", null)
                    },
                    onNudge = { dxPx, dyPx ->
                        val tMode = if (mapTargetPoint) "pin" else "pointer"
                        if (mapTargetPoint && state.isPlotLocked) {
                            viewModel.notifyLockedEditBlocked()
                        } else {
                            webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.nudge($dxPx, $dyPx, '$tMode');", null)
                        }
                    },
                    onClose = { showNudgePad = false }
                )
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
                            val mouzaInchEq = perimeterFeet / state.selectedMouzaScale.feetPerInch.coerceAtLeast(1.0)
                            val linksEq = perimeterFeet / 0.66
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.0f বর্গফুট | পরিধি: %.1f ফু (%.0f লিংক) | স্কেল %s: %.2f\"",
                                    areaRes.sqFeet, perimeterFeet, linksEq, state.selectedMouzaScale.ratioLabel, mouzaInchEq
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
                            if (state.isPlotLocked) {
                                viewModel.notifyLockedEditBlocked()
                            } else {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.undo();", null)
                            }
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
                            if (state.isPlotLocked) {
                                viewModel.notifyLockedEditBlocked()
                            } else {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.deleteSelectedPin();", null)
                            }
                        },
                        enabled = state.pointCount > 0,
                        modifier = Modifier.testTag("map_delete_pin_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            if (state.isPlotLocked) Icons.Rounded.Lock else Icons.Rounded.Remove,
                            contentDescription = "Delete Pin",
                            modifier = Modifier.size(16.dp),
                            tint = if (state.isPlotLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD6405A)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (state.isPlotLocked) "লকড" else if (state.selectedPinIdx >= 0) "পিন #${state.selectedPinIdx + 1}" else "বিন্দু ডিলিট",
                            color = if (state.isPlotLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD6405A),
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (state.isPlotLocked) {
                                viewModel.notifyLockedEditBlocked()
                            } else {
                                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.clearAll();", null)
                            }
                        },
                        enabled = state.pointCount > 0,
                        modifier = Modifier.testTag("map_clear_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                    val triggerMapExport: (Boolean) -> Unit = { asPdf ->
                        val wv = webViewRef
                        if (wv != null) {
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
                                            cropRectNormalized = parsed.cropRect,
                                            asPdf = asPdf
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { showSaveProjectDialog = true },
                        enabled = state.pointCount > 0 && !state.isExporting,
                        modifier = Modifier.testTag("map_save_data_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("ডাটা", maxLines = 1)
                    }
                    Button(
                        onClick = { triggerMapExport(false) },
                        enabled = state.pointCount > 0 && !state.isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_export_png_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("PNG", maxLines = 1)
                    }
                    Button(
                        onClick = { triggerMapExport(true) },
                        enabled = state.pointCount > 0 && !state.isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_export_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF", maxLines = 1)
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

    // Save Map Data Project Dialog
    if (showSaveProjectDialog) {
        var projTitle by remember(state.pointCount, state.selectedCategory) {
            mutableStateOf("${state.selectedCategory.titleBn} (${state.pointCount} পিন)")
        }
        AlertDialog(
            onDismissRequest = { showSaveProjectDialog = false },
            title = { Text("ম্যাপ ডাটা সংরক্ষণ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "এই স্যাটেলাইট ম্যাপের লোকেশন, জুম, লেয়ার ও সব পিন সংরক্ষিত থাকবে—পরে ক্লিক করে আবার রিস্টোর ও এডিট করতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = projTitle,
                        onValueChange = { projTitle = it },
                        label = { Text("জমি / প্রজেক্টের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wv = webViewRef
                        if (wv != null) {
                            wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.getFullState();") { raw ->
                                val parsed = parseFullMapState(raw)
                                viewModel.saveMapProject(
                                    customTitle = projTitle,
                                    centerLat = parsed.centerLat,
                                    centerLon = parsed.centerLon,
                                    zoom = parsed.zoom,
                                    pointsLatLon = parsed.points,
                                    modeId = parsed.mode
                                )
                            }
                        }
                        showSaveProjectDialog = false
                    }
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProjectDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Saved Map Projects Restore Sheet
    if (showSavedProjectsSheet) {
        ModalBottomSheet(onDismissRequest = { showSavedProjectsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "সংরক্ষিত স্যাটেলাইট ম্যাপ কাজ (${state.savedProjects.size}টি)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.savedProjects.isEmpty()) {
                    Text(
                        text = "এখনো কোনো ম্যাপ ডাটা সংরক্ষণ করা হয়নি। ম্যাপে পিন বসিয়ে 'ডাটা সেভ' চাপলে এখানে দেখাবে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.savedProjects, key = { it.id }) { proj ->
                            val dateStr = remember(proj.timestampMillis) {
                                SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(proj.timestampMillis))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                                    .clickable {
                                        showSavedProjectsSheet = false
                                        viewModel.restoreMapProject(proj)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proj.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${proj.mapPoints.size} পিন • $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f শতক • %.2f কাঠা (রিস্টোর করতে ট্যাপ করুন)", proj.decimalArea, proj.kathaArea),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSavedProject(proj.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Map Project", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    // Separate Khatian Upload & Dag-Number Lookup Sheet
    if (showKhatianSheet) {
        KhatianLookupBottomSheet(
            khatianDocName = state.khatianDocName,
            khatianPreviewBitmap = state.khatianPreviewBitmap,
            khatianPageCount = state.khatianPageCount,
            khatianCurrentPage = state.khatianCurrentPage,
            isLoadingKhatian = state.isLoadingKhatian,
            khatianRecords = state.khatianRecords,
            onUploadKhatianClick = { khatianPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
            onSelectKhatianPage = { viewModel.selectKhatianPage(it) },
            onAddKhatianRecord = { kh, dags, owner, area ->
                viewModel.addKhatianRecord(kh, dags, owner, area)
            },
            onDeleteKhatianRecord = { viewModel.deleteKhatianRecord(it) },
            onSelectDagForMap = { kh, dag -> viewModel.setActiveKhatianTarget(kh, dag) },
            onDismiss = { showKhatianSheet = false }
        )
    }

    if (showCoordinateDialog) {
        MapCoordinateToolboxDialog(
            initialLat = currentCenterLat,
            initialLon = currentCenterLon,
            onFlyTo = { la, lo ->
                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.flyTo($la, $lo, 18, false);", null)
            },
            onAddPin = { la, lo ->
                if (state.isPlotLocked) viewModel.notifyLockedEditBlocked()
                else webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.addPoint($la, $lo);", null)
            },
            onDismiss = { showCoordinateDialog = false }
        )
    }

    if (showKmzDialog) {
        KmzDemarcationExportDialog(
            hasMouzaOverlay = !state.mouzaOverlayDataUrl.isNullOrBlank(),
            scaleLabel = state.selectedMouzaScale.ratioLabel,
            onExportKmz = { meta ->
                val wv = webViewRef
                if (wv != null) {
                    wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.getExportPayload();") { rawPayload ->
                        val parsedPts = parseExportPayload(rawPayload).points
                        wv.evaluateJavascript("window.bhumiMap && window.bhumiMap.getOverlayBoundsNSEW();") { rawBounds ->
                            val bounds = runCatching {
                                val cleaned = rawBounds?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"")
                                if (cleaned.isNullOrBlank() || cleaned == "null") null
                                else {
                                    val arr = JSONArray(cleaned)
                                    doubleArrayOf(arr.getDouble(0), arr.getDouble(1), arr.getDouble(2), arr.getDouble(3))
                                }
                            }.getOrNull()
                            viewModel.exportEnhancedKmz(parsedPts, meta, bounds)
                        }
                    }
                }
            },
            onDismiss = { showKmzDialog = false }
        )
    }

    if (showGpsAvgDialog) {
        GpsAveragingDialog(
            maxAccuracyM = state.maxGpsAccuracyM,
            onMaxAccuracyChange = { viewModel.setMaxGpsAccuracyM(it) },
            onUseAveragedPoint = { la, lo ->
                webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.flyTo($la, $lo, 19, true);", null)
                if (!state.isPlotLocked) {
                    webViewRef?.evaluateJavascript("window.bhumiMap && window.bhumiMap.addPoint($la, $lo);", null)
                }
            },
            onDismiss = { showGpsAvgDialog = false }
        )
    }

    if (showImportedParcelsSheet) {
        ImportedParcelsSheet(
            parcels = state.importedParcels,
            onSelectParcel = { viewModel.loadParcelIntoMap(it) },
            onClearAll = { viewModel.clearImportedParcels() },
            onDismiss = { showImportedParcelsSheet = false }
        )
    }
}

private data class FullMapStatePayload(
    val centerLat: Double,
    val centerLon: Double,
    val zoom: Double,
    val mode: String,
    val points: List<Pair<Double, Double>>
)

private fun parseFullMapState(rawJsResult: String?): FullMapStatePayload {
    if (rawJsResult.isNullOrBlank() || rawJsResult == "null") {
        return FullMapStatePayload(23.8103, 90.4125, 16.0, "area", emptyList())
    }
    return runCatching {
        val cleaned = rawJsResult.trim().removeSurrounding("\"").replace("\\\"", "\"")
        val json = JSONObject(cleaned)
        val ptsArr = json.optJSONArray("points")
        val pts = mutableListOf<Pair<Double, Double>>()
        if (ptsArr != null) {
            for (i in 0 until ptsArr.length()) {
                val obj = ptsArr.getJSONObject(i)
                pts.add(obj.getDouble("lat") to obj.getDouble("lon"))
            }
        }
        FullMapStatePayload(
            centerLat = json.optDouble("centerLat", 23.8103),
            centerLon = json.optDouble("centerLon", 90.4125),
            zoom = json.optDouble("zoom", 16.0),
            mode = json.optString("mode", "area"),
            points = pts
        )
    }.getOrDefault(FullMapStatePayload(23.8103, 90.4125, 16.0, "area", emptyList()))
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
              const dpr = () => Math.max(Math.min(window.devicePixelRatio || 1, 3.0), 2.0);

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
                z: 17,
                mode: 'area',
                points: [],
                selectedPinIdx: -1,
                locked: false,
                pointerMode: true,
                loupeEnabled: true,
                loupeZoom: 3.0,
                loupeAnim: 1.0,
                hdSharp: true,
                hideMeasures: false,
                parcelSnapMode: false,
                importedParcels: [],
                mouzaRatio: '1:3,960',
                mouzaFeetPerInch: 330.0,
                showMouzaGrid: false,
                overlay: {
                  img: null,
                  loaded: false,
                  center: null,
                  widthM: 450,
                  opacity: 0.48,
                  rotDeg: 0,
                  locked: false
                },
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
                if (TC.size > 900) {
                  const first = TC.keys().next().value;
                  TC.delete(first);
                }
                const shard = ((x + y) & 3).toString();
                entry.img.onload = () => { entry.state = 'ok'; scheduleDraw(); };
                entry.img.onerror = () => {
                  entry.state = 'err';
                  if (z > 14) {
                    const pz = Math.max(12, z - 2);
                    const pScale = 1 << (z - pz);
                    fetchTile(urlTmpl, keyPrefix, pz, Math.floor(x / pScale), Math.floor(y / pScale));
                  }
                };
                entry.img.src = urlTmpl.replace('{s}', shard).replace('{z}', z).replace('{x}', x).replace('{y}', y);
                return entry;
              }

              function drawTileLayer(urlTmpl, keyPrefix, maxNative) {
                const w = rc.clientWidth, h = rc.clientHeight;
                const zt = Math.max(1, Math.min(maxNative, Math.round(state.z + 0.45)));
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
                      ctx.drawImage(tile.img, sx, sy, tilePxOnScreen + 0.75, tilePxOnScreen + 0.75);
                    } else {
                      // Auto pre-fetch crisp parent fallback so zoomed rural plots never vanish!
                      if (zt >= 18) {
                        const fz = zt - 2;
                        const fs = 4;
                        fetchTile(urlTmpl, keyPrefix, fz, Math.floor(wrapX / fs), Math.floor(ty / fs));
                      }
                      for (let dz = 1; dz <= 5 && (zt - dz) >= 1; dz++) {
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
                          ctx.drawImage(pTile.img, subX, subY, subW, subH, sx, sy, tilePxOnScreen + 0.75, tilePxOnScreen + 0.75);
                          break;
                        }
                      }
                    }
                  }
                }
              }

              function drawMouzaOverlay(w, h) {
                const ov = state.overlay;
                if (!ov.img || !ov.loaded || !ov.center) return;
                const centerLL = m2ll(ov.center.x, ov.center.y);
                const mpp = 156543.03392 * Math.cos(centerLL.lat * Math.PI / 180) / Math.pow(2, state.z);
                const ws = 256 * Math.pow(2, state.z);
                const sx = (ov.center.x - state.center.x) * ws + w / 2;
                const sy = (ov.center.y - state.center.y) * ws + h / 2;
                const drawW = ov.widthM / Math.max(0.01, mpp);
                const drawH = drawW * (ov.img.height / Math.max(1, ov.img.width));
                ctx.save();
                ctx.globalAlpha = ov.opacity;
                ctx.translate(sx, sy);
                ctx.rotate(ov.rotDeg * Math.PI / 180);
                ctx.drawImage(ov.img, -drawW / 2, -drawH / 2, drawW, drawH);
                if (!ov.locked) {
                  ctx.globalAlpha = 0.85;
                  ctx.strokeStyle = '#ffd54f';
                  ctx.lineWidth = 2;
                  ctx.setLineDash([8, 6]);
                  ctx.strokeRect(-drawW / 2, -drawH / 2, drawW, drawH);
                }
                ctx.restore();
              }

              function drawMouzaGrid(w, h) {
                if (!state.showMouzaGrid) return;
                const centerLL = m2ll(state.center.x, state.center.y);
                const mpp = 156543.03392 * Math.cos(centerLL.lat * Math.PI / 180) / Math.pow(2, state.z);
                const fpp = mpp * 3.280839895;
                // Choose 1 Chain (66 ft) or 1 Mouza Inch (330/165/82.5/66 ft) depending on zoom
                let stepFt = 66.0;
                if (stepFt / fpp < 46) stepFt = state.mouzaFeetPerInch;
                const stepPx = stepFt / fpp;
                if (stepPx < 24 || stepPx > 900) return;

                ctx.save();
                ctx.strokeStyle = 'rgba(255, 213, 79, 0.36)';
                ctx.lineWidth = 1.1;
                const startX = (w / 2) % stepPx;
                const startY = (h / 2) % stepPx;
                for (let x = startX; x < w; x += stepPx) {
                  ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
                }
                for (let y = startY; y < h; y += stepPx) {
                  ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
                }
                ctx.restore();
                drawLabel('গ্রিড: ' + stepFt.toFixed(0) + "' (" + state.mouzaRatio + ')', w - 102, 20, 'rgba(15, 107, 84, 0.88)', '#fad165');
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
                ctx.save();
                if (state.hdSharp) {
                  ctx.filter = 'contrast(1.12) saturate(1.10) brightness(1.02)';
                }
                drawTileLayer(tileUrl, tileUrl, maxNativeZ);
                if (showEsriOverlay) {
                  drawTileLayer(ROADS_URL, 'esri_roads', 18);
                  drawTileLayer(PLACES_URL, 'esri_places', 18);
                }
                ctx.restore();

                drawMouzaOverlay(w, h);
                if (includeTextLabels && !state.hideMeasures) drawMouzaGrid(w, h);

                // Draw imported KML/KMZ/GeoJSON/DXF parcels
                if (Array.isArray(state.importedParcels) && state.importedParcels.length) {
                  state.importedParcels.forEach(p => {
                    if (!p.points || p.points.length < 3) return;
                    const ps = p.points.map(toScreen);
                    ctx.save();
                    ctx.beginPath();
                    ps.forEach((pt, idx) => idx ? ctx.lineTo(pt.x, pt.y) : ctx.moveTo(pt.x, pt.y));
                    ctx.closePath();
                    ctx.fillStyle = 'rgba(0, 188, 212, 0.16)';
                    ctx.fill();
                    ctx.setLineDash([6, 4]);
                    ctx.lineWidth = 2.2;
                    ctx.strokeStyle = '#00e5ff';
                    ctx.stroke();
                    ctx.restore();
                    if (includeTextLabels && !state.hideMeasures) {
                      let cx = 0, cy = 0;
                      ps.forEach(pt => { cx += pt.x; cy += pt.y; });
                      cx /= ps.length; cy /= ps.length;
                      const lbl = (p.dag ? ('দাগ ' + p.dag) : p.name) + ' • ' + (p.decimal || 0).toFixed(2) + ' শতক';
                      drawLabel(lbl, cx, cy, 'rgba(0, 131, 143, 0.88)', '#ffffff');
                    }
                  });
                }

                const pts = state.points;
                const spts = pts.map(toScreen);
                const showEdgeLabels = includeTextLabels && !state.hideMeasures;

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

                  if (showEdgeLabels) {
                    drawLabel('R: ' + (radM * 3.28084).toFixed(1) + "'", (spts[0].x + spts[1].x) / 2, (spts[0].y + spts[1].y) / 2);
                  }
                } else if (state.mode === 'distance') {
                  if (spts.length >= 2) {
                    ctx.beginPath();
                    spts.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
                    ctx.lineWidth = 4;
                    ctx.strokeStyle = '#ffd54f';
                    ctx.stroke();
                    if (showEdgeLabels) {
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
                    if (showEdgeLabels) {
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

                    if (showEdgeLabels) {
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

                // Draw draggable vertex pins (or locked teal markers when finalized)
                spts.forEach((p, idx) => {
                  if (state.locked) {
                    ctx.beginPath();
                    ctx.arc(p.x, p.y, 7.2, 0, Math.PI * 2);
                    ctx.fillStyle = '#00bfa5';
                    ctx.fill();
                    ctx.lineWidth = 2.4;
                    ctx.strokeStyle = '#ffffff';
                    ctx.stroke();
                  } else {
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
                  }
                });

                if (state.locked && spts.length > 0 && includeTextLabels) {
                  let cx = 0, cy = 0;
                  spts.forEach(p => { cx += p.x; cy += p.y; });
                  cx /= spts.length; cy /= spts.length;
                  drawLabel('🔒 লকড', cx, cy - 16, 'rgba(0, 121, 107, 0.94)', '#ffffff');
                }

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

                // Animated Live Circular Magnifying Loupe showing HD zoomed imagery under pointer
                const targetLoupe = (state.pointerMode && state.loupeEnabled && w > 180 && h > 220) ? 1.0 : 0.0;
                if (Math.abs(state.loupeAnim - targetLoupe) > 0.015) {
                  state.loupeAnim += (targetLoupe - state.loupeAnim) * 0.28;
                  scheduleDraw();
                } else {
                  state.loupeAnim = targetLoupe;
                }
                if (state.loupeAnim > 0.05 && w > 180 && h > 220) {
                  const lx = w / 2;
                  const lr = 54 * state.loupeAnim;
                  const ly = Math.min(86, Math.max(66, 54 + 16));
                  const zoomFactor = state.loupeZoom || 3.0;
                  ctx.save();
                  ctx.globalAlpha = Math.min(1, state.loupeAnim * 1.1);
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
                  ctx.globalAlpha = Math.min(1, state.loupeAnim * 1.1);
                  ctx.strokeStyle = 'rgba(229, 57, 53, 0.92)';
                  ctx.lineWidth = 1.6;
                  ctx.beginPath();
                  ctx.moveTo(lx - lr + 8, ly); ctx.lineTo(lx - 7, ly);
                  ctx.moveTo(lx + 7, ly); ctx.lineTo(lx + lr - 8, ly);
                  ctx.moveTo(lx, ly - lr + 8); ctx.lineTo(lx, ly - 7);
                  ctx.moveTo(lx, ly + 7); ctx.lineTo(lx, ly + lr - 8);
                  ctx.stroke();
                  ctx.beginPath();
                  ctx.arc(lx, ly, 3.2 * state.loupeAnim, 0, Math.PI * 2);
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

                  if (state.loupeAnim > 0.65) {
                    ctx.font = 'bold 10px sans-serif';
                    const tag = zoomFactor.toFixed(1) + 'x জুম';
                    const tw = ctx.measureText(tag).width + 10;
                    ctx.fillStyle = 'rgba(15, 107, 84, 0.92)';
                    ctx.beginPath();
                    ctx.roundRect(lx - tw / 2, ly + lr - 13, tw, 16, 8);
                    ctx.fill();
                    ctx.fillStyle = '#ffffff';
                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';
                    ctx.fillText(tag, lx, ly + lr - 5);
                  }
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
                  // Check if user tapped/grabbed an existing corner vertex pin
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
                  const canDragPin = !state.locked && draggingPinIdx >= 0 && (!state.pointerMode || state.selectedPinIdx === draggingPinIdx);
                  if (canDragPin) {
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

              function notifyLockBlock() {
                if (window.AndroidMapBridge && window.AndroidMapBridge.onLockedEditBlocked) {
                  window.AndroidMapBridge.onLockedEditBlocked();
                }
              }

              function onUp(e) {
                if (!pointers.has(e.pointerId)) return;
                const pt = rel(e);
                const wasLast = pointers.size === 1;
                pointers.delete(e.pointerId);
                if (wasLast && dragStart && dragStart.mv < 9 && (Date.now() - dragStart.t) < 480 && e.type === 'pointerup') {
                  if (state.locked) {
                    notifyLockBlock();
                    draggingPinIdx = -1;
                    return;
                  }
                  if (draggingPinIdx >= 0) {
                    state.selectedPinIdx = (state.selectedPinIdx === draggingPinIdx) ? -1 : draggingPinIdx;
                  } else if (state.parcelSnapMode && window.AndroidMapBridge && window.AndroidMapBridge.onSnapMapTap) {
                    const ll = screenToLatLon(pt.x, pt.y);
                    window.AndroidMapBridge.onSnapMapTap(ll.lat, ll.lon);
                    draggingPinIdx = -1;
                    return;
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
                } else if (wasLast && !state.locked && draggingPinIdx >= 0) {
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
                setLoupeConfig: (enabled, zoom) => {
                  state.loupeEnabled = !!enabled;
                  if (zoom && zoom > 1.0) state.loupeZoom = zoom;
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
                setLocked: (locked) => {
                  state.locked = !!locked;
                  if (state.locked) state.selectedPinIdx = -1;
                  scheduleDraw();
                },
                setMouzaScale: (ratio, fpi, showGrid) => {
                  state.mouzaRatio = ratio || '1:3,960';
                  state.mouzaFeetPerInch = fpi || 330.0;
                  state.showMouzaGrid = !!showGrid;
                  scheduleDraw();
                },
                setHdSharp: (enabled) => {
                  state.hdSharp = !!enabled;
                  scheduleDraw();
                },
                setMouzaOverlayImage: (dataUrl, widthM, opacity, rotDeg, locked) => {
                  const img = new Image();
                  img.onload = () => {
                    state.overlay.img = img;
                    state.overlay.loaded = true;
                    if (!state.overlay.center) {
                      state.overlay.center = { x: state.center.x, y: state.center.y };
                    }
                    state.overlay.widthM = widthM || 450;
                    state.overlay.opacity = opacity || 0.48;
                    state.overlay.rotDeg = rotDeg || 0;
                    state.overlay.locked = !!locked;
                    scheduleDraw();
                  };
                  img.src = dataUrl;
                },
                updateMouzaOverlayParams: (widthM, opacity, rotDeg, locked) => {
                  state.overlay.widthM = widthM || 450;
                  state.overlay.opacity = opacity || 0.48;
                  state.overlay.rotDeg = rotDeg || 0;
                  state.overlay.locked = !!locked;
                  scheduleDraw();
                },
                centerMouzaOverlayHere: () => {
                  state.overlay.center = { x: state.center.x, y: state.center.y };
                  scheduleDraw();
                },
                clearMouzaOverlay: () => {
                  state.overlay.img = null;
                  state.overlay.loaded = false;
                  state.overlay.center = null;
                  scheduleDraw();
                },
                addPoint: (lat, lon) => {
                  if (state.locked) { notifyLockBlock(); return; }
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
                  if (state.locked) { notifyLockBlock(); return; }
                  const ll = screenToLatLon(rc.clientWidth / 2, rc.clientHeight / 2);
                  if (state.mode === 'circle' && state.points.length >= 2) {
                    state.points[1] = ll;
                    state.selectedPinIdx = -1;
                  } else {
                    state.points.push(ll);
                    state.selectedPinIdx = -1;
                  }
                  notifyBridge();
                  scheduleDraw();
                },
                deleteSelectedPin: () => {
                  if (state.locked) { notifyLockBlock(); return; }
                  if (!state.points.length) return;
                  const idx = (state.selectedPinIdx >= 0 && state.selectedPinIdx < state.points.length)
                    ? state.selectedPinIdx : (state.points.length - 1);
                  state.points.splice(idx, 1);
                  state.selectedPinIdx = state.points.length ? Math.min(idx, state.points.length - 1) : -1;
                  notifyBridge();
                  scheduleDraw();
                },
                moveSelectedPinToCenter: () => {
                  if (state.locked) { notifyLockBlock(); return; }
                  if (state.selectedPinIdx < 0 || state.selectedPinIdx >= state.points.length) return;
                  state.points[state.selectedPinIdx] = screenToLatLon(rc.clientWidth / 2, rc.clientHeight / 2);
                  notifyBridge();
                  scheduleDraw();
                },
                cycleSelectedPin: (delta) => {
                  const n = state.points.length;
                  if (!n) return;
                  const cur = state.selectedPinIdx >= 0 ? state.selectedPinIdx : (n - 1);
                  state.selectedPinIdx = ((cur + delta) % n + n) % n;
                  notifyBridge();
                  scheduleDraw();
                },
                nudge: (dxPx, dyPx, targetMode) => {
                  const ws = 256 * Math.pow(2, state.z);
                  if (targetMode === 'pin' && state.points.length > 0) {
                    if (state.locked) { notifyLockBlock(); return; }
                    const idx = (state.selectedPinIdx >= 0 && state.selectedPinIdx < state.points.length)
                      ? state.selectedPinIdx : (state.points.length - 1);
                    state.selectedPinIdx = idx;
                    const m = ll2m(state.points[idx].lat, state.points[idx].lon);
                    state.points[idx] = m2ll(m.x + dxPx / ws, m.y + dyPx / ws);
                    notifyBridge();
                  } else {
                    state.center.x += dxPx / ws;
                    state.center.y += dyPx / ws;
                  }
                  scheduleDraw();
                },
                getFullState: () => {
                  const c = m2ll(state.center.x, state.center.y);
                  return JSON.stringify({
                    centerLat: c.lat,
                    centerLon: c.lon,
                    zoom: state.z,
                    mode: state.mode,
                    points: state.points
                  });
                },
                restoreState: (lat, lon, zoom, mode, pts, locked) => {
                  state.center = ll2m(lat, lon);
                  state.z = Math.min(maxViewZ, zoom || 17);
                  if (mode) state.mode = mode;
                  state.points = Array.isArray(pts) ? pts.map(p => ({ lat: p.lat, lon: p.lon })) : [];
                  state.locked = !!locked;
                  state.selectedPinIdx = -1;
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
                  if (state.locked) { notifyLockBlock(); return; }
                  if (state.points.length) state.points.pop();
                  state.selectedPinIdx = state.points.length - 1;
                  notifyBridge();
                  scheduleDraw();
                },
                clearAll: () => {
                  if (state.locked) { notifyLockBlock(); return; }
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
                },
                setHideMeasures: (h) => {
                  state.hideMeasures = !!h;
                  scheduleDraw();
                },
                setParcelSnapMode: (enabled) => {
                  state.parcelSnapMode = !!enabled;
                  scheduleDraw();
                },
                setImportedParcels: (arr) => {
                  state.importedParcels = Array.isArray(arr) ? arr : [];
                  scheduleDraw();
                },
                setPolygonPoints: (pts) => {
                  if (state.locked) { notifyLockBlock(); return; }
                  state.points = Array.isArray(pts) ? pts.map(p => ({ lat: p.lat, lon: p.lon })) : [];
                  state.selectedPinIdx = -1;
                  notifyBridge();
                  scheduleDraw();
                },
                syncMoveRelativeToOverlay: (dxNorm, dyNorm) => {
                  const ov = state.overlay;
                  const baseCenter = (ov && ov.center) ? ov.center : state.center;
                  const cLL = m2ll(baseCenter.x, baseCenter.y);
                  const mPerDegLat = 111132.92;
                  const mPerDegLon = 111412.84 * Math.cos(cLL.lat * Math.PI / 180);
                  const wM = (ov && ov.widthM) ? ov.widthM : 450;
                  const hM = (ov && ov.img) ? wM * (ov.img.height / Math.max(1, ov.img.width)) : wM;
                  const targetLat = cLL.lat - dyNorm * (hM / mPerDegLat);
                  const targetLon = cLL.lon + dxNorm * (wM / mPerDegLon);
                  state.center = ll2m(targetLat, targetLon);
                  scheduleDraw();
                },
                getOverlayBoundsNSEW: () => {
                  const ov = state.overlay;
                  if (!ov || !ov.img || !ov.loaded || !ov.center) return null;
                  const cLL = m2ll(ov.center.x, ov.center.y);
                  const wM = ov.widthM || 450;
                  const hM = wM * (ov.img.height / Math.max(1, ov.img.width));
                  const dLat = (hM / 2) / 111132.92;
                  const dLon = (wM / 2) / (111412.84 * Math.cos(cLL.lat * Math.PI / 180));
                  return JSON.stringify([cLL.lat + dLat, cLL.lat - dLat, cLL.lon + dLon, cLL.lon - dLon]);
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
