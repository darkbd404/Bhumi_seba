package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.engine.LandCalculators
import com.example.engine.MouzaPdfEngine
import com.example.engine.PlotWallIndex
import com.example.engine.PngExporter
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.MouzaSelectionTool
import com.example.model.SavedMeasurementRecord
import com.example.ui.theme.PlotPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class MouzaPdfUiState(
    val isLoadingPdf: Boolean = false,
    val isRenderingHd: Boolean = false,
    val isIndexingPlots: Boolean = false,
    val isExporting: Boolean = false,
    val fileName: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val displayBitmap: Bitmap? = null,
    val hdReady: Boolean = false,
    val pdfPageWidthPts: Int = 1000,
    val selectedScale: MouzaScale = MouzaScale.SCALE_16,
    val calibrationMultiplier: Double = 1.0,
    val gapSealLevel: Int = 1, // 0 = Exact, 1 = Balanced, 2 = Strong
    val activeTool: MouzaSelectionTool = MouzaSelectionTool.AUTO_TAP,
    val selectedPlots: List<MouzaPlot> = emptyList(),
    val manualPolygonDraft: List<Pair<Float, Float>> = emptyList(),
    val khatianTitle: String = "",
    val overlayMaskBitmap: Bitmap? = null,
    val statusMessage: String? = null,
    val lastSavedPath: String? = null
)

class MouzaPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = AppPreferences(appContext)

    private val _uiState = MutableStateFlow(MouzaPdfUiState())
    val uiState: StateFlow<MouzaPdfUiState> = _uiState.asStateFlow()

    private var activePdfFile: File? = null
    private var wallIndex: PlotWallIndex? = null
    private var nextPlotId = 1
    private var renderJob: Job? = null
    private var indexJob: Job? = null

    fun openPdfUri(uri: Uri) {
        renderJob?.cancel()
        indexJob?.cancel()
        renderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingPdf = true,
                    statusMessage = "PDF ফাইল প্রস্তুত হচ্ছে...",
                    selectedPlots = emptyList(),
                    manualPolygonDraft = emptyList(),
                    overlayMaskBitmap = null
                )
            }
            try {
                val displayName = resolveFileName(uri)
                val info = MouzaPdfEngine.copyPdfToCache(appContext, uri, displayName)
                activePdfFile = info.tempFile
                _uiState.update {
                    it.copy(
                        fileName = info.fileName,
                        pageCount = info.pageCount,
                        currentPage = 0,
                        isLoadingPdf = false
                    )
                }
                loadPageInternal(0)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPdf = false,
                        statusMessage = "PDF খোলা যায়নি: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun selectPage(pageIndex: Int) {
        if (pageIndex == _uiState.value.currentPage) return
        if (pageIndex !in 0 until _uiState.value.pageCount) return
        renderJob?.cancel()
        indexJob?.cancel()
        renderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentPage = pageIndex,
                    selectedPlots = emptyList(),
                    manualPolygonDraft = emptyList(),
                    overlayMaskBitmap = null,
                    hdReady = false
                )
            }
            loadPageInternal(pageIndex)
        }
    }

    private suspend fun loadPageInternal(pageIndex: Int) {
        val file = activePdfFile ?: return
        // Stage 1: Instant preview (<80ms)
        val preview = MouzaPdfEngine.renderPdfPage(appContext, file, pageIndex, ultraHd = false)
        _uiState.update {
            it.copy(
                displayBitmap = preview.bitmap,
                pdfPageWidthPts = preview.pdfPageWidthPts,
                hdReady = false,
                isRenderingHd = true,
                statusMessage = "Ultra-HD তে রূপান্তর হচ্ছে..."
            )
        }

        // Stage 2: Ultra-HD Full Resolution render
        val hd = MouzaPdfEngine.renderPdfPage(appContext, file, pageIndex, ultraHd = true)
        _uiState.update {
            it.copy(
                displayBitmap = hd.bitmap,
                pdfPageWidthPts = hd.pdfPageWidthPts,
                hdReady = true,
                isRenderingHd = false,
                statusMessage = "Ultra-HD ম্যাপ প্রস্তুত — দাগের ভেতরে ট্যাপ করুন"
            )
        }
        rebuildWallIndex(hd.bitmap, _uiState.value.gapSealLevel)
    }

    private fun rebuildWallIndex(bitmap: Bitmap, gapSealLevel: Int) {
        indexJob?.cancel()
        indexJob = viewModelScope.launch {
            _uiState.update { it.copy(isIndexingPlots = true) }
            val index = MouzaPdfEngine.buildPlotWallIndex(bitmap, gapSealLevel)
            wallIndex = index
            _uiState.update { it.copy(isIndexingPlots = false) }
        }
    }

    fun setGapSealLevel(level: Int) {
        val clamped = level.coerceIn(0, 2)
        _uiState.update { it.copy(gapSealLevel = clamped) }
        _uiState.value.displayBitmap?.let { bmp ->
            rebuildWallIndex(bmp, clamped)
        }
    }

    fun setScale(scale: MouzaScale) {
        _uiState.update { it.copy(selectedScale = scale, calibrationMultiplier = 1.0) }
    }

    fun calibrateWithKnownPlot(plotId: Int, actualDecimal: Double) {
        if (actualDecimal <= 0.0) return
        val plot = _uiState.value.selectedPlots.find { it.id == plotId } ?: return
        val index = wallIndex ?: return
        val baseSqFtPerPx = MouzaPdfEngine.computeSqFeetPerGridPixel(
            pdfPageWidthPts = _uiState.value.pdfPageWidthPts,
            gridWidth = index.gridWidth,
            scale = _uiState.value.selectedScale,
            customCalibrationMultiplier = 1.0
        )
        val rawDecimal = (plot.pixelCount * baseSqFtPerPx) / LandCalculators.SQ_FT_PER_DECIMAL
        if (rawDecimal > 0.0) {
            val multiplier = actualDecimal / rawDecimal
            _uiState.update {
                it.copy(
                    calibrationMultiplier = multiplier,
                    statusMessage = "স্কেল ক্যালিব্রেশন সম্পন্ন! সব দাগ এখন সঠিক মাপ দেখাবে।"
                )
            }
        }
    }

    fun setTool(tool: MouzaSelectionTool) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                manualPolygonDraft = emptyList(),
                statusMessage = if (tool == MouzaSelectionTool.AUTO_TAP) {
                    "দাগের ভেতরে ট্যাপ করলে সম্পূর্ণ দাগ অটো সিলেক্ট হবে"
                } else {
                    "দাগের কোণাগুলোতে ট্যাপ করে 'সম্পন্ন' চাপুন"
                }
            )
        }
    }

    fun onMapTap(normX: Float, normY: Float) {
        val state = _uiState.value
        if (state.displayBitmap == null) return

        if (state.activeTool == MouzaSelectionTool.POLYGON_DRAW) {
            _uiState.update {
                it.copy(manualPolygonDraft = it.manualPolygonDraft + (normX to normY))
            }
            return
        }

        val index = wallIndex
        if (index == null || state.isIndexingPlots) {
            _uiState.update { it.copy(statusMessage = "দাগের সীমানা বিশ্লেষণ চলছে, ১ সেকেন্ড অপেক্ষা করুন...") }
            return
        }

        val color = PlotPalette[(nextPlotId - 1) % PlotPalette.size]
        when (val result = index.selectPlotAt(
            normX = normX,
            normY = normY,
            plotId = nextPlotId,
            plotIndex = state.selectedPlots.size + 1,
            color = color,
            existingPlots = state.selectedPlots
        )) {
            is PlotWallIndex.TapResult.Success -> {
                nextPlotId++
                val updated = state.selectedPlots + result.plot
                updatePlotsAndOverlay(updated, "দাগ #${result.plot.plotIndex} সিলেক্ট হয়েছে")
            }
            is PlotWallIndex.TapResult.AlreadySelected -> {
                val updated = state.selectedPlots.filterNot { it.id == result.plotId }
                    .mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) }
                updatePlotsAndOverlay(updated, "দাগ আনসিলেক্ট করা হয়েছে")
            }
            is PlotWallIndex.TapResult.LeakedOrOutside -> {
                _uiState.update { it.copy(statusMessage = result.reasonBn) }
            }
        }
    }

    fun finishManualPolygon() {
        val draft = _uiState.value.manualPolygonDraft
        if (draft.size < 3) {
            _uiState.update { it.copy(statusMessage = "কমপক্ষে ৩টি কোণা সিলেক্ট করুন।") }
            return
        }
        val gw = wallIndex?.gridWidth ?: 1200
        val gh = wallIndex?.gridHeight ?: 900
        val color = PlotPalette[(nextPlotId - 1) % PlotPalette.size]
        val plot = MouzaPdfEngine.buildManualPolygonPlot(
            normalizedPoints = draft,
            gridWidth = gw,
            gridHeight = gh,
            plotId = nextPlotId,
            plotIndex = _uiState.value.selectedPlots.size + 1,
            color = color
        )
        nextPlotId++
        val updated = _uiState.value.selectedPlots + plot
        _uiState.update { it.copy(manualPolygonDraft = emptyList()) }
        updatePlotsAndOverlay(updated, "ম্যানুয়াল দাগ #${plot.plotIndex} যুক্ত হয়েছে")
    }

    fun undoManualPoint() {
        val draft = _uiState.value.manualPolygonDraft
        if (draft.isNotEmpty()) {
            _uiState.update { it.copy(manualPolygonDraft = draft.dropLast(1)) }
        }
    }

    fun removePlot(plotId: Int) {
        val updated = _uiState.value.selectedPlots.filterNot { it.id == plotId }
            .mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) }
        updatePlotsAndOverlay(updated, "দাগ তালিকা থেকে বাদ দেওয়া হয়েছে")
    }

    fun undoLastPlot() {
        val current = _uiState.value.selectedPlots
        if (current.isEmpty()) return
        val updated = current.dropLast(1)
        updatePlotsAndOverlay(updated, "সর্বশেষ সিলেক্টেড দাগ বাদ দেওয়া হয়েছে")
    }

    fun clearAllPlots() {
        _uiState.update {
            it.copy(
                selectedPlots = emptyList(),
                manualPolygonDraft = emptyList(),
                overlayMaskBitmap = null,
                statusMessage = "সব সিলেকশন পরিষ্কার করা হয়েছে"
            )
        }
    }

    fun updatePlotTags(plotId: Int, dagNumber: String, khatianNumber: String) {
        val updated = _uiState.value.selectedPlots.map {
            if (it.id == plotId) it.copy(dagNumber = dagNumber, khatianNumber = khatianNumber) else it
        }
        _uiState.update { it.copy(selectedPlots = updated) }
    }

    fun updateKhatianTitle(title: String) {
        _uiState.update { it.copy(khatianTitle = title) }
    }

    fun sqFeetPerGridPixel(): Double {
        val gw = wallIndex?.gridWidth ?: _uiState.value.selectedPlots.firstOrNull()?.gridWidth ?: 1200
        return MouzaPdfEngine.computeSqFeetPerGridPixel(
            pdfPageWidthPts = _uiState.value.pdfPageWidthPts,
            gridWidth = gw,
            scale = _uiState.value.selectedScale,
            customCalibrationMultiplier = _uiState.value.calibrationMultiplier
        )
    }

    private fun updatePlotsAndOverlay(plots: List<MouzaPlot>, message: String) {
        val gw = wallIndex?.gridWidth ?: plots.firstOrNull()?.gridWidth ?: 1200
        val gh = wallIndex?.gridHeight ?: plots.firstOrNull()?.gridHeight ?: 900

        if (plots.isEmpty()) {
            _uiState.update {
                it.copy(
                    selectedPlots = emptyList(),
                    overlayMaskBitmap = null,
                    statusMessage = message
                )
            }
            return
        }

        // Build crisp fast overlay bitmap once per selection change (zero work on zoom/pan!)
        val overlay = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(gw * gh)
        for (p in plots) {
            if (p.manualPolygonNormalized != null) continue
            val fillArgb = android.graphics.Color.argb(
                105,
                (p.color.red * 255).toInt(),
                (p.color.green * 255).toInt(),
                (p.color.blue * 255).toInt()
            )
            for (idx in p.maskIndices) {
                if (idx in pixels.indices) {
                    pixels[idx] = fillArgb
                }
            }
        }
        overlay.setPixels(pixels, 0, gw, 0, 0, gw, gh)

        _uiState.update {
            it.copy(
                selectedPlots = plots,
                overlayMaskBitmap = overlay,
                statusMessage = message
            )
        }
    }

    fun exportSelectedOnlyPng() {
        val state = _uiState.value
        val source = state.displayBitmap ?: return
        if (state.selectedPlots.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "আগে কমপক্ষে ১টি দাগ সিলেক্ট করুন।") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "সিলেক্টেড জায়গা PNG তৈরি হচ্ছে...") }
            try {
                val sqFtPerPx = sqFeetPerGridPixel()
                val res = PngExporter.exportSelectedMouzaPlotsPng(
                    context = appContext,
                    sourceBitmap = source,
                    plots = state.selectedPlots,
                    sqFeetPerGridPixel = sqFtPerPx,
                    khatianTitle = state.khatianTitle
                )
                val totalSqFt = state.selectedPlots.sumOf { it.pixelCount * sqFtPerPx }
                val area = LandCalculators.fromSqFeet(totalSqFt)
                prefs.addMeasurementRecord(
                    SavedMeasurementRecord(
                        id = "mouza_${System.currentTimeMillis()}",
                        title = state.khatianTitle.ifBlank { "মৌজা সিলেক্টেড (${state.selectedPlots.size} দাগ)" },
                        source = "Mouza PDF",
                        decimalArea = area.decimal,
                        kathaArea = area.katha,
                        sqFeetArea = area.sqFeet,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastSavedPath = res.displayPath,
                        statusMessage = "সফলভাবে গ্যালারিতে সেভ হয়েছে: ${res.displayPath}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "PNG সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun resolveFileName(uri: Uri): String {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "mouza_map.pdf"
            }
        }
        return "mouza_map.pdf"
    }
}
