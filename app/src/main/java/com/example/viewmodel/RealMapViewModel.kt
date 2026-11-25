package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.engine.LandCalculators
import com.example.engine.MouzaPdfEngine
import com.example.engine.PngExporter
import com.example.model.KhatianRecord
import com.example.model.MapLayerOption
import com.example.model.MapMeasureCategory
import com.example.model.MouzaScale
import com.example.model.SavedMeasurementRecord
import com.example.model.SavedProjectType
import com.example.model.SavedSurveyProject
import com.example.model.SerializableLatLon
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
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
    val selectedLayer: MapLayerOption = MapLayerOption.GOOGLE_HYBRID,
    val showLabelsOverlay: Boolean = true,
    val selectedCategory: MapMeasureCategory = MapMeasureCategory.AREA,
    val pointCount: Int = 0,
    val selectedPinIdx: Int = -1,
    val areaSqMeters: Double = 0.0,
    val perimeterMeters: Double = 0.0,
    val splitSharesCount: Int = 2,
    val isGpsWalkActive: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<MapSearchSuggestion> = emptyList(),
    val isExporting: Boolean = false,
    val activeProjectId: String? = null,
    val savedProjects: List<SavedSurveyProject> = emptyList(),
    val pendingRestoreProject: SavedSurveyProject? = null,
    val khatianDocName: String = "",
    val khatianIsPdf: Boolean = true,
    val khatianPageCount: Int = 0,
    val khatianCurrentPage: Int = 0,
    val khatianPreviewBitmap: Bitmap? = null,
    val isLoadingKhatian: Boolean = false,
    val khatianRecords: List<KhatianRecord> = emptyList(),
    val activeSearchTarget: Pair<String, String>? = null, // (khatianNo, dagNo)
    val isPlotLocked: Boolean = false,
    val selectedMouzaScale: MouzaScale = MouzaScale.SCALE_16,
    val showMouzaGrid: Boolean = false,
    val hdSharpMode: Boolean = true,
    val mouzaOverlayDocName: String = "",
    val mouzaOverlayDataUrl: String? = null,
    val mouzaOverlayIsPdf: Boolean = false,
    val mouzaOverlayPageCount: Int = 0,
    val mouzaOverlayCurrentPage: Int = 0,
    val mouzaOverlayOpacity: Float = 0.48f,
    val mouzaOverlayWidthMeters: Double = 450.0,
    val mouzaOverlayRotationDeg: Float = 0f,
    val mouzaOverlayLocked: Boolean = false,
    val isLoadingMouzaOverlay: Boolean = false,
    val statusMessage: String? = null
)

class RealMapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = AppPreferences(appContext)
    private var activeKhatianFile: File? = null
    private var activeKhatianIsPdf: Boolean = true
    private var activeMouzaOverlayFile: File? = null
    private var activeMouzaOverlayIsPdf: Boolean = false

    private val _uiState = MutableStateFlow(RealMapUiState())
    val uiState: StateFlow<RealMapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.savedProjectsFlow.collect { all ->
                _uiState.update {
                    it.copy(savedProjects = all.filter { p -> p.type == SavedProjectType.REAL_MAP })
                }
            }
        }
    }

    fun selectLayer(layer: MapLayerOption) {
        _uiState.update { it.copy(selectedLayer = layer) }
    }

    fun toggleLabelsOverlay() {
        _uiState.update { it.copy(showLabelsOverlay = !it.showLabelsOverlay) }
    }

    fun selectMouzaScale(scale: MouzaScale) {
        _uiState.update {
            it.copy(
                selectedMouzaScale = scale,
                statusMessage = "মৌজা স্কেল: ${scale.labelBn} (${scale.ratioLabel}) — ১\" = ${scale.feetPerInch.toInt()} ফুট"
            )
        }
    }

    fun toggleMouzaGrid() {
        _uiState.update {
            val next = !it.showMouzaGrid
            it.copy(
                showMouzaGrid = next,
                statusMessage = if (next) "মৌজা চেইন/স্কেল গ্রিড চালু হয়েছে" else "মৌজা গ্রিড বন্ধ করা হয়েছে"
            )
        }
    }

    fun toggleHdSharpMode() {
        _uiState.update {
            val next = !it.hdSharpMode
            it.copy(
                hdSharpMode = next,
                statusMessage = if (next) "Google/Satellite Ultra-HD শার্পনেস চালু" else "স্বাভাবিক রেন্ডারিং মোড"
            )
        }
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

    fun updateMeasurementFromBridge(
        points: Int,
        areaSqM: Double,
        perimeterM: Double,
        selectedPinIdx: Int = -1
    ) {
        _uiState.update {
            it.copy(
                pointCount = points,
                selectedPinIdx = selectedPinIdx,
                areaSqMeters = areaSqM.coerceAtLeast(0.0),
                perimeterMeters = perimeterM.coerceAtLeast(0.0),
                isPlotLocked = if (points == 0) false else it.isPlotLocked
            )
        }
    }

    fun togglePlotLock() {
        val current = _uiState.value
        if (current.pointCount == 0 && !current.isPlotLocked) {
            _uiState.update { it.copy(statusMessage = "লক করার আগে ম্যাপে পিন বসিয়ে জমি পরিমাপ করুন") }
            return
        }
        setPlotLocked(!current.isPlotLocked)
    }

    fun setPlotLocked(locked: Boolean) {
        _uiState.update {
            it.copy(
                isPlotLocked = locked,
                selectedPinIdx = if (locked) -1 else it.selectedPinIdx,
                statusMessage = if (locked) {
                    "পরিমাপ লক করা হয়েছে — পিন এডিট, রিসাইজ বা মুভ হবে না"
                } else {
                    "পরিমাপ আনলক করা হয়েছে — এখন পিন এডিট করতে পারবেন"
                }
            )
        }
    }

    fun notifyLockedEditBlocked() {
        _uiState.update {
            it.copy(statusMessage = "এই পরিমাপটি লক করা আছে — এডিট করতে আগে আনলক করুন")
        }
    }

    fun saveMapProject(
        customTitle: String,
        centerLat: Double,
        centerLon: Double,
        zoom: Double,
        pointsLatLon: List<Pair<Double, Double>>,
        modeId: String
    ) {
        val state = _uiState.value
        val sqFeet = state.areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER
        val area = LandCalculators.fromSqFeet(sqFeet)
        val perimeterFeet = state.perimeterMeters * 3.280839895
        val projId = state.activeProjectId ?: "map_proj_${System.currentTimeMillis()}"
        val cleanTitle = customTitle.trim().ifBlank {
            "${state.selectedCategory.titleBn} (${pointsLatLon.size} পিন)"
        }
        val project = SavedSurveyProject(
            id = projId,
            title = cleanTitle,
            type = SavedProjectType.REAL_MAP,
            timestampMillis = System.currentTimeMillis(),
            decimalArea = area.decimal,
            kathaArea = area.katha,
            sqFeetArea = area.sqFeet,
            perimeterFeet = perimeterFeet,
            itemCount = pointsLatLon.size,
            mapLayerId = state.selectedLayer.id,
            showLabelsOverlay = state.showLabelsOverlay,
            mouzaScaleName = state.selectedMouzaScale.name,
            mapCategoryId = modeId.ifBlank { state.selectedCategory.id },
            mapCenterLat = centerLat,
            mapCenterLon = centerLon,
            mapZoom = zoom,
            mapPoints = pointsLatLon.map { SerializableLatLon(it.first, it.second) },
            mapPlotLocked = state.isPlotLocked,
            splitSharesCount = state.splitSharesCount,
            khatianFilePath = activeKhatianFile?.takeIf { it.exists() }?.absolutePath,
            khatianFileName = state.khatianDocName,
            khatianIsPdf = activeKhatianIsPdf,
            khatianRecords = state.khatianRecords
        )
        viewModelScope.launch {
            prefs.saveSurveyProject(project)
            _uiState.update {
                it.copy(
                    activeProjectId = projId,
                    statusMessage = "ম্যাপ ডাটা সংরক্ষিত হয়েছে — পরে রিস্টোর করতে পারবেন"
                )
            }
        }
    }

    fun restoreMapProjectById(projectId: String) {
        viewModelScope.launch {
            val proj = prefs.getSurveyProjectById(projectId)
            if (proj == null) {
                _uiState.update { it.copy(statusMessage = "সংরক্ষিত ম্যাপ ডাটা খুঁজে পাওয়া যায়নি") }
                return@launch
            }
            restoreMapProject(proj)
        }
    }

    fun restoreMapProject(project: SavedSurveyProject) {
        val layer = MapLayerOption.entries.find { it.id == project.mapLayerId } ?: MapLayerOption.GOOGLE_HYBRID
        val cat = MapMeasureCategory.entries.find { it.id == project.mapCategoryId } ?: MapMeasureCategory.AREA
        val scale = runCatching { MouzaScale.valueOf(project.mouzaScaleName) }.getOrDefault(MouzaScale.SCALE_16)
        viewModelScope.launch {
            val khatianFile = project.khatianFilePath?.let { File(it) }?.takeIf { it.exists() }
            activeKhatianFile = khatianFile
            activeKhatianIsPdf = project.khatianIsPdf
            var restoredKhatianBmp: Bitmap? = null
            var restoredKhatianPages = 0
            if (khatianFile != null) {
                runCatching {
                    restoredKhatianPages = if (project.khatianIsPdf) {
                        withContext(Dispatchers.IO) {
                            android.os.ParcelFileDescriptor.open(khatianFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                                android.graphics.pdf.PdfRenderer(pfd).use { it.pageCount }
                            }
                        }
                    } else 1
                    restoredKhatianBmp = MouzaPdfEngine.renderBaseDocument(
                        appContext, khatianFile, project.khatianIsPdf, 0, ultraHd = false
                    ).bitmap
                }
            }
            _uiState.update {
                it.copy(
                    activeProjectId = project.id,
                    selectedLayer = layer,
                    showLabelsOverlay = project.showLabelsOverlay,
                    selectedMouzaScale = scale,
                    selectedCategory = cat,
                    splitSharesCount = project.splitSharesCount.coerceIn(2, 20),
                    pendingRestoreProject = project,
                    khatianDocName = project.khatianFileName,
                    khatianIsPdf = project.khatianIsPdf,
                    khatianPageCount = restoredKhatianPages,
                    khatianCurrentPage = 0,
                    khatianPreviewBitmap = restoredKhatianBmp,
                    khatianRecords = project.khatianRecords,
                    isPlotLocked = project.mapPlotLocked,
                    statusMessage = "রিস্টোর হয়েছে: ${project.title}"
                )
            }
        }
    }

    fun consumePendingRestore() {
        _uiState.update { it.copy(pendingRestoreProject = null) }
    }

    fun deleteSavedProject(projectId: String) {
        viewModelScope.launch {
            prefs.deleteSurveyProject(projectId)
            if (_uiState.value.activeProjectId == projectId) {
                _uiState.update { it.copy(activeProjectId = null) }
            }
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
                        setRequestProperty("User-Agent", "BhumiSebaDigital/1.0")
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

    fun exportSelectedMapPlotPng(
        pointsLatLon: List<Pair<Double, Double>>,
        mode: String,
        fallbackBitmap: Bitmap?,
        cropRectNormalized: RectF?,
        asPdf: Boolean = false
    ) {
        val state = _uiState.value
        val sqFeet = state.areaSqMeters * LandCalculators.SQ_FT_PER_SQ_METER
        val area = LandCalculators.fromSqFeet(sqFeet)
        val perimeterFeet = state.perimeterMeters * 3.280839895

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    statusMessage = if (asPdf) "মৌজা স্টাইল PDF তৈরি হচ্ছে..." else "মৌজা স্টাইল PNG সেভ হচ্ছে..."
                )
            }
            try {
                val profile = prefs.surveyorProfileFlow.first()
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

                val res = if (asPdf) {
                    PngExporter.exportSelectedMapPlotPdf(
                        context = appContext,
                        pointsLatLon = pointsLatLon,
                        mode = mode,
                        layer = state.selectedLayer,
                        showLabelsOverlay = state.showLabelsOverlay,
                        areaSqMeters = state.areaSqMeters,
                        fallbackBitmap = fallbackBitmap,
                        fallbackCropRect = cropRectNormalized,
                        summaryHeader = "ভূমি সেবা ডিজিটাল • ${state.selectedCategory.titleBn}",
                        mouzaScale = state.selectedMouzaScale,
                        surveyorProfile = profile
                    )
                } else {
                    PngExporter.exportSelectedMapPlotPng(
                        context = appContext,
                        pointsLatLon = pointsLatLon,
                        mode = mode,
                        layer = state.selectedLayer,
                        showLabelsOverlay = state.showLabelsOverlay,
                        areaSqMeters = state.areaSqMeters,
                        fallbackBitmap = fallbackBitmap,
                        fallbackCropRect = cropRectNormalized,
                        summaryHeader = "ভূমি সেবা ডিজিটাল • ${state.selectedCategory.titleBn}",
                        summaryLine1 = line1,
                        summaryLine2 = line2,
                        mouzaScale = state.selectedMouzaScale,
                        surveyorProfile = profile
                    )
                }

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

    fun exportSelectedMapPng(
        fullMapBitmap: Bitmap,
        cropRectNormalized: RectF?
    ) = exportSelectedMapPlotPng(
        pointsLatLon = emptyList(),
        mode = _uiState.value.selectedCategory.id,
        fallbackBitmap = fullMapBitmap,
        cropRectNormalized = cropRectNormalized
    )

    fun openKhatianDocumentUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingKhatian = true, statusMessage = "খতিয়ান আপলোড হচ্ছে...") }
            try {
                val displayName = resolveFileName(uri)
                val mime = appContext.contentResolver.getType(uri)
                val info = MouzaPdfEngine.copyDocumentToCache(
                    context = appContext,
                    uri = uri,
                    fileName = displayName,
                    mimeType = mime,
                    storageDirName = "active_work_docs",
                    cachePrefix = "active_khatian_"
                )
                val savedDir = File(appContext.filesDir, "saved_khatian_docs").apply { mkdirs() }
                val destFile = withContext(Dispatchers.IO) {
                    val ext = info.tempFile.extension.ifBlank { if (info.isPdf) "pdf" else "png" }
                    val target = File(savedDir, "khatian_map_${System.currentTimeMillis()}.$ext")
                    info.tempFile.copyTo(target, overwrite = true)
                    target
                }
                activeKhatianFile = destFile
                activeKhatianIsPdf = info.isPdf
                val preview = MouzaPdfEngine.renderBaseDocument(appContext, destFile, info.isPdf, 0, ultraHd = true)
                _uiState.update {
                    it.copy(
                        isLoadingKhatian = false,
                        khatianDocName = info.fileName,
                        khatianIsPdf = info.isPdf,
                        khatianPageCount = info.pageCount,
                        khatianCurrentPage = 0,
                        khatianPreviewBitmap = preview.bitmap,
                        statusMessage = "খতিয়ান আপলোড সম্পন্ন — দাগ নং যোগ করে ম্যাপে মাপুন"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingKhatian = false,
                        statusMessage = "খতিয়ান খোলা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun selectKhatianPage(pageIndex: Int) {
        val file = activeKhatianFile ?: return
        if (!activeKhatianIsPdf || pageIndex !in 0 until _uiState.value.khatianPageCount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingKhatian = true) }
            runCatching {
                MouzaPdfEngine.renderBaseDocument(appContext, file, true, pageIndex, ultraHd = true)
            }.onSuccess { rendered ->
                _uiState.update {
                    it.copy(
                        isLoadingKhatian = false,
                        khatianCurrentPage = pageIndex,
                        khatianPreviewBitmap = rendered.bitmap
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingKhatian = false) }
            }
        }
    }

    fun addKhatianRecord(
        khatianNumber: String,
        dagNumbersCsv: String,
        ownerName: String,
        areaSummary: String
    ) {
        val dags = dagNumbersCsv
            .split(",", " ", "।", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val rec = KhatianRecord(
            id = "khat_map_${System.currentTimeMillis()}",
            khatianNumber = khatianNumber.trim(),
            dagNumbers = dags,
            ownerName = ownerName.trim(),
            areaSummary = areaSummary.trim(),
            pageIndex = _uiState.value.khatianCurrentPage
        )
        _uiState.update {
            it.copy(
                khatianRecords = listOf(rec) + it.khatianRecords,
                statusMessage = "খতিয়ান নোট যুক্ত হয়েছে"
            )
        }
    }

    fun deleteKhatianRecord(id: String) {
        _uiState.update {
            it.copy(khatianRecords = it.khatianRecords.filterNot { r -> r.id == id })
        }
    }

    fun setActiveKhatianTarget(khatianNo: String, dagNo: String) {
        _uiState.update {
            it.copy(
                activeSearchTarget = khatianNo.trim() to dagNo.trim(),
                statusMessage = "লক্ষ্য সেট হয়েছে: খতিয়ান ${khatianNo.ifBlank { "-" }} • দাগ $dagNo"
            )
        }
    }

    fun clearActiveSearchTarget() {
        _uiState.update { it.copy(activeSearchTarget = null) }
    }

    private fun resolveFileName(uri: Uri): String {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "khatian.pdf"
            }
        }
        return "khatian.pdf"
    }

    fun openMouzaOverlayDocumentUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMouzaOverlay = true, statusMessage = "মৌজা নকশা ওভারলে লোড হচ্ছে...") }
            try {
                val displayName = resolveFileName(uri)
                val mime = appContext.contentResolver.getType(uri)
                val info = MouzaPdfEngine.copyDocumentToCache(
                    context = appContext,
                    uri = uri,
                    fileName = displayName,
                    mimeType = mime,
                    storageDirName = "active_work_docs",
                    cachePrefix = "active_map_mouza_"
                )
                activeMouzaOverlayFile = info.tempFile
                activeMouzaOverlayIsPdf = info.isPdf
                val rendered = MouzaPdfEngine.renderBaseDocument(
                    appContext, info.tempFile, info.isPdf, 0, ultraHd = false
                )
                val dataUrl = encodeBitmapToDataUrl(rendered.bitmap)
                rendered.bitmap.recycle()
                _uiState.update {
                    it.copy(
                        isLoadingMouzaOverlay = false,
                        mouzaOverlayDocName = info.fileName,
                        mouzaOverlayDataUrl = dataUrl,
                        mouzaOverlayIsPdf = info.isPdf,
                        mouzaOverlayPageCount = info.pageCount,
                        mouzaOverlayCurrentPage = 0,
                        mouzaOverlayLocked = false,
                        statusMessage = "নকশা ওভারলে যুক্ত হয়েছে — স্বচ্ছতা ও আকার সেট করে Google ম্যাপে মিলিয়ে নিন"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMouzaOverlay = false,
                        statusMessage = "নকশা ওভারলে খোলা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun selectMouzaOverlayPage(pageIndex: Int) {
        val file = activeMouzaOverlayFile ?: return
        if (!activeMouzaOverlayIsPdf || pageIndex !in 0 until _uiState.value.mouzaOverlayPageCount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMouzaOverlay = true) }
            runCatching {
                val rendered = MouzaPdfEngine.renderBaseDocument(appContext, file, true, pageIndex, ultraHd = false)
                val dataUrl = encodeBitmapToDataUrl(rendered.bitmap)
                rendered.bitmap.recycle()
                dataUrl
            }.onSuccess { dataUrl ->
                _uiState.update {
                    it.copy(
                        isLoadingMouzaOverlay = false,
                        mouzaOverlayCurrentPage = pageIndex,
                        mouzaOverlayDataUrl = dataUrl
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMouzaOverlay = false) }
            }
        }
    }

    fun setMouzaOverlayOpacity(opacity: Float) {
        _uiState.update { it.copy(mouzaOverlayOpacity = opacity.coerceIn(0.05f, 0.95f)) }
    }

    fun setMouzaOverlayWidthMeters(widthMeters: Double) {
        _uiState.update { it.copy(mouzaOverlayWidthMeters = widthMeters.coerceIn(40.0, 4000.0)) }
    }

    fun setMouzaOverlayRotation(deg: Float) {
        val wrapped = ((deg % 360f) + 360f) % 360f
        _uiState.update { it.copy(mouzaOverlayRotationDeg = wrapped) }
    }

    fun toggleMouzaOverlayLock() {
        _uiState.update {
            val next = !it.mouzaOverlayLocked
            it.copy(
                mouzaOverlayLocked = next,
                statusMessage = if (next) "মৌজা নকশা ওভারলে লক করা হয়েছে — এখন পিন বসাতে পারবেন"
                else "মৌজা নকশা ওভারলে আনলক হয়েছে"
            )
        }
    }

    fun clearMouzaOverlay() {
        activeMouzaOverlayFile = null
        _uiState.update {
            it.copy(
                mouzaOverlayDocName = "",
                mouzaOverlayDataUrl = null,
                mouzaOverlayPageCount = 0,
                mouzaOverlayCurrentPage = 0,
                mouzaOverlayLocked = false,
                statusMessage = "মৌজা নকশা ওভারলে সরানো হয়েছে"
            )
        }
    }

    private suspend fun encodeBitmapToDataUrl(src: Bitmap): String = withContext(Dispatchers.Default) {
        val maxDim = 1280
        val scale = minOf(1f, maxDim.toFloat() / maxOf(src.width, src.height))
        val target = if (scale < 0.98f) {
            Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt().coerceAtLeast(16),
                (src.height * scale).toInt().coerceAtLeast(16),
                true
            )
        } else src
        val baos = ByteArrayOutputStream()
        target.compress(Bitmap.CompressFormat.PNG, 90, baos)
        if (target !== src) target.recycle()
        "data:image/png;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
