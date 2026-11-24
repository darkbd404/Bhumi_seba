package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.engine.LandCalculators
import com.example.engine.PngExporter
import com.example.model.MapLayerOption
import com.example.model.MapMeasureCategory
import com.example.model.SavedMeasurementRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class MapSearchSuggestion(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

data class RealMapUiState(
    val selectedLayer: MapLayerOption = MapLayerOption.ESRI_SATELLITE,
    val selectedCategory: MapMeasureCategory = MapMeasureCategory.AREA,
    val pointCount: Int = 0,
    val areaSqMeters: Double = 0.0,
    val perimeterMeters: Double = 0.0,
    val splitSharesCount: Int = 2,
    val isGpsWalkActive: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<MapSearchSuggestion> = emptyList(),
    val isExporting: Boolean = false,
    val statusMessage: String? = null
)

class RealMapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = AppPreferences(appContext)

    private val _uiState = MutableStateFlow(RealMapUiState())
    val uiState: StateFlow<RealMapUiState> = _uiState.asStateFlow()

    fun selectLayer(layer: MapLayerOption) {
        _uiState.update { it.copy(selectedLayer = layer) }
    }

    fun selectCategory(category: MapMeasureCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                isGpsWalkActive = if (category != MapMeasureCategory.GPS_WALK) false else it.isGpsWalkActive
            )
        }
    }

    fun setGpsWalkActive(active: Boolean) {
        _uiState.update { it.copy(isGpsWalkActive = active) }
    }

    fun setSplitSharesCount(count: Int) {
        _uiState.update { it.copy(splitSharesCount = count.coerceIn(2, 20)) }
    }

    fun updateMeasurementFromBridge(points: Int, areaSqM: Double, perimeterM: Double) {
        _uiState.update {
            it.copy(
                pointCount = points,
                areaSqMeters = areaSqM.coerceAtLeast(0.0),
                perimeterMeters = perimeterM.coerceAtLeast(0.0)
            )
        }
    }

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun searchLocation(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList()) }
            try {
                val results = withContext(Dispatchers.IO) {
                    val encoded = URLEncoder.encode("$trimmed, Bangladesh", "UTF-8")
                    val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "BhumiSurveyBD/1.0")
                        connectTimeout = 8000
                        readTimeout = 8000
                    }
                    val body = conn.inputStream.bufferedReader().readText()
                    val arr = JSONArray(body)
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        MapSearchSuggestion(
                            displayName = obj.getString("display_name"),
                            lat = obj.getString("lat").toDouble(),
                            lon = obj.getString("lon").toDouble()
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = results,
                        statusMessage = if (results.isEmpty()) "কোনো স্থান পাওয়া যায়নি।" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        statusMessage = "লোকেশন খোঁজা যায়নি — ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                    )
                }
            }
        }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun exportSelectedMapPng(
        fullMapBitmap: Bitmap,
        cropRectNormalized: RectF?
    ) {
        val state = _uiState.value
        val sqFeet = state.areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER
        val area = LandCalculators.fromSqFeet(sqFeet)
        val perimeterFeet = state.perimeterMeters * 3.280839895

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "ম্যাপের সিলেক্টেড এরিয়া PNG সেভ হচ্ছে...") }
            try {
                val line1 = if (state.areaSqMeters > 0.0) {
                    String.format(
                        Locale.US,
                        "মোট: %.2f শতাংশ  |  %.2f কাঠা  |  %.3f বিঘা",
                        area.decimal, area.katha, area.bigha
                    )
                } else {
                    String.format(
                        Locale.US,
                        "মোট দূরত্ব: %.2f ফুট  |  %.2f মিটার  |  %.2f লিংক",
                        perimeterFeet, state.perimeterMeters, perimeterFeet / 0.66
                    )
                }
                val line2 = String.format(
                    Locale.US,
                    "%.1f বর্গফুট  |  পরিধি: %.1f ফুট (%.1f মিটার)  |  লেয়ার: %s",
                    area.sqFeet, perimeterFeet, state.perimeterMeters, state.selectedLayer.titleEn
                )

                val res = PngExporter.exportSelectedMapRegionPng(
                    context = appContext,
                    fullMapBitmap = fullMapBitmap,
                    cropRectNormalized = cropRectNormalized,
                    summaryHeader = "Bhumi Survey BD • ${state.selectedCategory.titleBn}",
                    summaryLine1 = line1,
                    summaryLine2 = line2
                )

                if (state.areaSqMeters > 0.0) {
                    prefs.addMeasurementRecord(
                        SavedMeasurementRecord(
                            id = "map_${System.currentTimeMillis()}",
                            title = "${state.selectedLayer.titleEn} (${state.pointCount} পয়েন্ট)",
                            source = "Real Map",
                            decimalArea = area.decimal,
                            kathaArea = area.katha,
                            sqFeetArea = area.sqFeet,
                            timestampMillis = System.currentTimeMillis()
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "সফলভাবে সেভ হয়েছে: ${res.displayPath}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "সেভ করা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
