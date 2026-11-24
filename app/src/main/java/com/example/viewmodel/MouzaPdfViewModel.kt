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
import com.example.model.MouzaFilterPreset
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.MouzaSelectionTool
import com.example.model.SavedMeasurementRecord
import com.example.ui.theme.PlotPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class MouzaPdfUiState(
    val isLoadingPdf: Boolean = false,
    val isRenderingHd: Boolean = false,
    val isAutoFilling: Boolean = false,
    val isExporting: Boolean = false,
    val fileName: String = "",
    val isPdf: Boolean = true,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val displayBitmap: Bitmap? = null,
    val viewportTile: MouzaPdfEngine.ViewportTileResult? = null,
    val hdReady: Boolean = false,
    val pdfPageWidthPts: Int = 1000,
    val pdfPageHeightPts: Int = 1000,
    val selectedScale: MouzaScale = MouzaScale.SCALE_16,
    val calibrationMultiplier: Double = 1.0,
    val gapSealLevel: Int = 1, // 0 = Exact, 1 = Balanced, 2 = Strong
    val activeTool: MouzaSelectionTool = MouzaSelectionTool.AUTO_TAP,
    val selectedPlots: List<MouzaPlot> = emptyList(),
    val canRedo: Boolean = false,
    val manualPolygonDraft: List<Pair<Float, Float>> = emptyList(),
    val tapeDraft: List<Pair<Float, Float>> = emptyList(),
    val completedTapes: List<List<Pair<Float, Float>>> = emptyList(),
    val khatianTitle: String = "",
    val filterPreset: MouzaFilterPreset = MouzaFilterPreset.NORMAL,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val overlayMaskBitmap: Bitmap? = null,
    val focusPlotRequest: MouzaPlot? = null,
    val statusMessage: String? = null,
    val lastSavedPath: String? = null
)

class MouzaPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = AppPreferences(appContext)

    private val _uiState = MutableStateFlow(MouzaPdfUiState())
    val uiState: StateFlow<MouzaPdfUiState> = _uiState.asStateFlow()

    private var activeDocFile: File? = null
    private var activeIsPdf: Boolean = true
    private var nextPlotId = 1
    private var renderJob: Job? = null
    private var viewportTileJob: Job? = null
    private var autoFillJob: Job? = null
    private var wallIndexFallback: PlotWallIndex? = null
    private val redoStack = ArrayDeque<MouzaPlot>()

    fun openPdfUri(uri: Uri) = openDocumentUri(uri)

    fun openDocumentUri(uri: Uri) {
        renderJob?.cancel()
        viewportTileJob?.cancel()
        autoFillJob?.cancel()
        redoStack.clear()

        renderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingPdf = true,
                    statusMessage = "নকশা ফাইল লোড হচ্ছে...",
                    selectedPlots = emptyList(),
                    canRedo = false,
                    manualPolygonDraft = emptyList(),
                    tapeDraft = emptyList(),
                    completedTapes = emptyList(),
                    viewportTile = null,
                    overlayMaskBitmap = null
                )
            }
            try {
                val displayName = resolveFileName(uri)
                val mime = appContext.contentResolver.getType(uri)
                val info = MouzaPdfEngine.copyDocumentToCache(appContext, uri, displayName, mime)
                activeDocFile = info.tempFile
                activeIsPdf = info.isPdf
                wallIndexFallback = null

                _uiState.update {
                    it.copy(
                        fileName = info.fileName,
                        isPdf = info.isPdf,
                        pageCount = info.pageCount,
                        currentPage = 0,
                        pdfPageWidthPts = info.baseWidthPts,
                        pdfPageHeightPts = info.baseHeightPts,
                        isLoadingPdf = false
                    )
                }
                loadPageInternal(0)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPdf = false,
                        statusMessage = "ফাইল খোলা যায়নি: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun selectPage(pageIndex: Int) {
        if (pageIndex == _uiState.value.currentPage) return
        if (pageIndex !in 0 until _uiState.value.pageCount) return
        renderJob?.cancel()
        viewportTileJob?.cancel()
        autoFillJob?.cancel()
        redoStack.clear()
        renderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentPage = pageIndex,
                    selectedPlots = emptyList(),
                    canRedo = false,
                    manualPolygonDraft = emptyList(),
                    tapeDraft = emptyList(),
                    completedTapes = emptyList(),
                    viewportTile = null,
                    overlayMaskBitmap = null,
                    hdReady = false
                )
            }
            loadPageInternal(pageIndex)
        }
    }

    private suspend fun loadPageInternal(pageIndex: Int) {
        val file = activeDocFile ?: return
        val preview = MouzaPdfEngine.renderBaseDocument(appContext, file, activeIsPdf, pageIndex, ultraHd = false)
        _uiState.update {
            it.copy(
                displayBitmap = preview.bitmap,
                pdfPageWidthPts = preview.pdfPageWidthPts,
                pdfPageHeightPts = preview.pdfPageHeightPts,
                hdReady = false,
                isRenderingHd = true
            )
        }

        val hd = MouzaPdfEngine.renderBaseDocument(appContext, file, activeIsPdf, pageIndex, ultraHd = true)
        _uiState.update {
            it.copy(
                displayBitmap = hd.bitmap,
                pdfPageWidthPts = hd.pdfPageWidthPts,
                pdfPageHeightPts = hd.pdfPageHeightPts,
                hdReady = true,
                isRenderingHd = false,
                statusMessage = "Ultra-HD ম্যাপ প্রস্তুত — দাগের ভেতরে ট্যাপ করুন"
            )
        }
    }

    /**
     * Triggered after zoom/pan stops or slows down to re-render the visible viewport region at true native HD (up to 1000x zoom).
     */
    fun requestViewportHdRender(
        zoomScale: Float,
        normLeft: Float,
        normTop: Float,
        normRight: Float,
        normBottom: Float,
        viewportWidthPx: Int,
        viewportHeightPx: Int
    ) {
        val file = activeDocFile ?: return
        if (zoomScale <= 1.15f) {
            viewportTileJob?.cancel()
            if (_uiState.value.viewportTile != null) {
                _uiState.update { it.copy(viewportTile = null) }
            }
            return
        }

        viewportTileJob?.cancel()
        viewportTileJob = viewModelScope.launch {
            delay(130)
            _uiState.update { it.copy(isRenderingHd = true) }
            val tile = MouzaPdfEngine.renderViewportTile(
                docFile = file,
                isPdf = activeIsPdf,
                pageIndex = _uiState.value.currentPage,
                normLeft = normLeft,
                normTop = normTop,
                normRight = normRight,
                normBottom = normBottom,
                targetWidthPx = viewportWidthPx,
                targetHeightPx = viewportHeightPx
            )
            _uiState.update {
                it.copy(
                    viewportTile = tile ?: it.viewportTile,
                    isRenderingHd = false
                )
            }
        }
    }

    fun setGapSealLevel(level: Int) {
        val clamped = level.coerceIn(0, 2)
        _uiState.update { it.copy(gapSealLevel = clamped) }
    }

    fun setScale(scale: MouzaScale) {
        _uiState.update { state ->
            val updatedPlots = state.selectedPlots.map { plot ->
                val poly = plot.effectivePolygon()
                if (poly != null) {
                    plot.copy(
                        exactAreaSqFeet = MouzaPdfEngine.computePolygonAreaSqFeet(
                            poly, state.pdfPageWidthPts, state.pdfPageHeightPts, scale, 1.0
                        )
                    )
                } else plot
            }
            state.copy(
                selectedScale = scale,
                calibrationMultiplier = 1.0,
                selectedPlots = updatedPlots
            )
        }
    }

    fun calibrateWithKnownPlot(plotId: Int, actualDecimal: Double) {
        if (actualDecimal <= 0.0) return
        val state = _uiState.value
        val plot = state.selectedPlots.find { it.id == plotId } ?: return
        val poly = plot.effectivePolygon()
        val rawDecimal = if (poly != null) {
            MouzaPdfEngine.computePolygonAreaSqFeet(
                poly, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, 1.0
            ) / LandCalculators.SQ_FT_PER_DECIMAL
        } else {
            val baseSqFtPerPx = MouzaPdfEngine.computeSqFeetPerGridPixel(
                pdfPageWidthPts = state.pdfPageWidthPts,
                gridWidth = plot.gridWidth,
                scale = state.selectedScale,
                customCalibrationMultiplier = 1.0
            )
            (plot.pixelCount * baseSqFtPerPx) / LandCalculators.SQ_FT_PER_DECIMAL
        }
        if (rawDecimal > 0.0) {
            val mult = actualDecimal / rawDecimal
            val updated = state.selectedPlots.map { p ->
                val pPoly = p.effectivePolygon()
                if (pPoly != null) {
                    p.copy(
                        exactAreaSqFeet = MouzaPdfEngine.computePolygonAreaSqFeet(
                            pPoly, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, mult
                        )
                    )
                } else p
            }
            _uiState.update {
                it.copy(
                    calibrationMultiplier = mult,
                    selectedPlots = updated,
                    statusMessage = "স্কেল ক্যালিব্রেশন সম্পন্ন!"
                )
            }
        }
    }

    fun setTool(tool: MouzaSelectionTool) {
        val hint = when (tool) {
            MouzaSelectionTool.AUTO_TAP -> "দাগের ভেতরে ট্যাপ করলে অটো সিলেক্ট হবে (কোণা টেনে ঠিক করা যাবে)"
            MouzaSelectionTool.EDIT_CORNERS -> "হলুদ কোণাগুলো টেনে বা লাইনের মাঝে ট্যাপ করে সীমানা ঠিক করুন"
            MouzaSelectionTool.TAPE_MEASURE -> "ম্যাপে ট্যাপ করে বিন্দু বসান — দূরত্ব ফুট/লিংকে দেখাবে"
            MouzaSelectionTool.POLYGON_DRAW -> "দাগের কোণাগুলোতে ট্যাপ করে 'সম্পন্ন' চাপুন"
            MouzaSelectionTool.PAN_ONLY -> "শুধু জুম ও প্যান মোড"
        }
        _uiState.update {
            it.copy(
                activeTool = tool,
                manualPolygonDraft = emptyList(),
                statusMessage = hint
            )
        }
    }

    fun onMapTap(normX: Float, normY: Float) {
        val state = _uiState.value
        if (state.displayBitmap == null) return

        when (state.activeTool) {
            MouzaSelectionTool.PAN_ONLY -> return
            MouzaSelectionTool.EDIT_CORNERS -> {
                // Allow tapping near a selected polygon's edge midpoint to split edge for fine adjustment
                trySplitNearestEdge(normX, normY)
                return
            }
            MouzaSelectionTool.POLYGON_DRAW -> {
                _uiState.update {
                    it.copy(manualPolygonDraft = it.manualPolygonDraft + (normX to normY))
                }
                return
            }
            MouzaSelectionTool.TAPE_MEASURE -> {
                _uiState.update {
                    it.copy(tapeDraft = it.tapeDraft + (normX to normY))
                }
                return
            }
            MouzaSelectionTool.AUTO_TAP -> {
                val file = activeDocFile ?: return
                if (state.isAutoFilling) return
                autoFillJob?.cancel()
                autoFillJob = viewModelScope.launch {
                    _uiState.update { it.copy(isAutoFilling = true) }
                    val color = PlotPalette[(nextPlotId - 1) % PlotPalette.size]
                    val res = MouzaPdfEngine.autoFillPlotPolygon(
                        docFile = file,
                        isPdf = activeIsPdf,
                        pageIndex = state.currentPage,
                        pageWidthPts = state.pdfPageWidthPts,
                        pageHeightPts = state.pdfPageHeightPts,
                        scale = state.selectedScale,
                        calibrationMultiplier = state.calibrationMultiplier,
                        normX = normX,
                        normY = normY,
                        plotId = nextPlotId,
                        plotIndex = state.selectedPlots.size + 1,
                        color = color,
                        existingPlots = state.selectedPlots,
                        gapSealLevel = state.gapSealLevel
                    )
                    when (res) {
                        is MouzaPdfEngine.AutoFillResult.Success -> {
                            nextPlotId++
                            redoStack.clear()
                            val updated = state.selectedPlots + res.plot
                            _uiState.update {
                                it.copy(
                                    isAutoFilling = false,
                                    selectedPlots = updated,
                                    canRedo = false,
                                    statusMessage = "দাগ #${res.plot.plotIndex} সিলেক্ট হয়েছে (প্রয়োজনে 'পয়েন্ট এডিট'-এ কোণা ঠিক করুন)"
                                )
                            }
                        }
                        is MouzaPdfEngine.AutoFillResult.AlreadySelected -> {
                            val updated = state.selectedPlots.filterNot { it.id == res.plotId }
                                .mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) }
                            _uiState.update {
                                it.copy(
                                    isAutoFilling = false,
                                    selectedPlots = updated,
                                    statusMessage = "দাগ আনসিলেক্ট করা হয়েছে"
                                )
                            }
                        }
                        is MouzaPdfEngine.AutoFillResult.Failed -> {
                            _uiState.update {
                                it.copy(
                                    isAutoFilling = false,
                                    statusMessage = res.reasonBn
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Dragging a polygon vertex handle manually corrects any imperfect auto-fill corner and updates area live!
     */
    fun updatePlotVertex(plotId: Int, vertexIdx: Int, newNormX: Float, newNormY: Float) {
        val state = _uiState.value
        val updated = state.selectedPlots.map { plot ->
            if (plot.id != plotId) return@map plot
            val poly = plot.effectivePolygon()?.toMutableList() ?: return@map plot
            if (vertexIdx !in poly.indices) return@map plot
            poly[vertexIdx] = newNormX.coerceIn(0f, 1f) to newNormY.coerceIn(0f, 1f)
            val sqFt = MouzaPdfEngine.computePolygonAreaSqFeet(
                poly, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, state.calibrationMultiplier
            )
            val cx = poly.map { it.first }.average().toFloat()
            val cy = poly.map { it.second }.average().toFloat()
            plot.copy(
                polygonNormalized = poly,
                manualPolygonNormalized = if (plot.manualPolygonNormalized != null) poly else null,
                centroidNormalized = cx to cy,
                exactAreaSqFeet = sqFt
            )
        }
        _uiState.update { it.copy(selectedPlots = updated) }
    }

    private fun trySplitNearestEdge(normX: Float, normY: Float) {
        val state = _uiState.value
        var bestPlotId = -1
        var bestEdgeAfterIdx = -1
        var bestDistSq = 0.00045f // ~2% of page normalized distance threshold

        for (plot in state.selectedPlots) {
            val poly = plot.effectivePolygon() ?: continue
            if (poly.size < 3) continue
            for (i in poly.indices) {
                val a = poly[i]
                val b = poly[(i + 1) % poly.size]
                val mx = (a.first + b.first) / 2f
                val my = (a.second + b.second) / 2f
                val dSq = (normX - mx) * (normX - mx) + (normY - my) * (normY - my)
                if (dSq < bestDistSq) {
                    bestDistSq = dSq
                    bestPlotId = plot.id
                    bestEdgeAfterIdx = i + 1
                }
            }
        }

        if (bestPlotId != -1 && bestEdgeAfterIdx >= 0) {
            val updated = state.selectedPlots.map { plot ->
                if (plot.id != bestPlotId) return@map plot
                val poly = plot.effectivePolygon()?.toMutableList() ?: return@map plot
                poly.add(bestEdgeAfterIdx.coerceIn(0, poly.size), normX to normY)
                val sqFt = MouzaPdfEngine.computePolygonAreaSqFeet(
                    poly, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, state.calibrationMultiplier
                )
                plot.copy(
                    polygonNormalized = poly,
                    manualPolygonNormalized = if (plot.manualPolygonNormalized != null) poly else null,
                    exactAreaSqFeet = sqFt
                )
            }
            _uiState.update { it.copy(selectedPlots = updated, statusMessage = "নতুন কোণা যোগ হয়েছে — টেনে সরান") }
        }
    }

    fun finishManualPolygon() {
        val state = _uiState.value
        val draft = state.manualPolygonDraft
        if (draft.size < 3) {
            _uiState.update { it.copy(statusMessage = "কমপক্ষে ৩টি কোণা সিলেক্ট করুন।") }
            return
        }
        val aspect = state.pdfPageHeightPts.toFloat() / state.pdfPageWidthPts.toFloat().coerceAtLeast(1f)
        val gw = 1500
        val gh = (1500f * aspect).roundToInt().coerceAtLeast(200)
        val color = PlotPalette[(nextPlotId - 1) % PlotPalette.size]
        val sqFt = MouzaPdfEngine.computePolygonAreaSqFeet(
            draft, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, state.calibrationMultiplier
        )
        val plot = MouzaPdfEngine.buildManualPolygonPlot(
            normalizedPoints = draft,
            gridWidth = gw,
            gridHeight = gh,
            plotId = nextPlotId,
            plotIndex = state.selectedPlots.size + 1,
            color = color
        ).copy(exactAreaSqFeet = sqFt)
        nextPlotId++
        redoStack.clear()
        val updated = state.selectedPlots + plot
        _uiState.update {
            it.copy(
                manualPolygonDraft = emptyList(),
                selectedPlots = updated,
                canRedo = false,
                statusMessage = "ম্যানুয়াল দাগ #${plot.plotIndex} যুক্ত হয়েছে"
            )
        }
    }

    fun undoManualPoint() {
        val draft = _uiState.value.manualPolygonDraft
        if (draft.isNotEmpty()) {
            _uiState.update { it.copy(manualPolygonDraft = draft.dropLast(1)) }
        }
    }

    fun undoTapePoint() {
        val draft = _uiState.value.tapeDraft
        if (draft.isNotEmpty()) {
            _uiState.update { it.copy(tapeDraft = draft.dropLast(1)) }
        }
    }

    fun clearTapeDraft() {
        _uiState.update { it.copy(tapeDraft = emptyList(), completedTapes = emptyList()) }
    }

    fun finishTapeMeasure() {
        val draft = _uiState.value.tapeDraft
        if (draft.size < 2) return
        _uiState.update {
            it.copy(
                completedTapes = it.completedTapes + listOf(draft),
                tapeDraft = emptyList(),
                statusMessage = "ফিতা মাপ সংরক্ষিত হয়েছে"
            )
        }
    }

    fun removePlot(plotId: Int) {
        val removed = _uiState.value.selectedPlots.find { it.id == plotId }
        if (removed != null) redoStack.addLast(removed)
        val updated = _uiState.value.selectedPlots.filterNot { it.id == plotId }
            .mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) }
        _uiState.update {
            it.copy(
                selectedPlots = updated,
                canRedo = redoStack.isNotEmpty(),
                statusMessage = "দাগ বাদ দেওয়া হয়েছে"
            )
        }
    }

    fun undoLastPlot() {
        val state = _uiState.value
        if (state.activeTool == MouzaSelectionTool.TAPE_MEASURE && state.tapeDraft.isNotEmpty()) {
            undoTapePoint()
            return
        }
        val current = state.selectedPlots
        if (current.isEmpty()) return
        redoStack.addLast(current.last())
        _uiState.update {
            it.copy(
                selectedPlots = current.dropLast(1),
                canRedo = true,
                statusMessage = "সর্বশেষ দাগ বাদ দেওয়া হয়েছে"
            )
        }
    }

    fun redoLastPlot() {
        val popped = redoStack.removeLastOrNull() ?: return
        _uiState.update { state ->
            val updated = state.selectedPlots + popped.copy(plotIndex = state.selectedPlots.size + 1)
            state.copy(
                selectedPlots = updated,
                canRedo = redoStack.isNotEmpty(),
                statusMessage = "দাগ পুনরুদ্ধার হয়েছে"
            )
        }
    }

    fun clearAllPlots() {
        redoStack.clear()
        _uiState.update {
            it.copy(
                selectedPlots = emptyList(),
                canRedo = false,
                manualPolygonDraft = emptyList(),
                tapeDraft = emptyList(),
                completedTapes = emptyList(),
                overlayMaskBitmap = null,
                statusMessage = "সব পরিমাপ পরিষ্কার করা হয়েছে"
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

    fun setFilterPreset(preset: MouzaFilterPreset) {
        _uiState.update { it.copy(filterPreset = preset) }
    }

    fun setBrightnessContrast(brightness: Float, contrast: Float) {
        _uiState.update {
            it.copy(
                brightness = brightness.coerceIn(0.5f, 1.8f),
                contrast = contrast.coerceIn(0.5f, 2.5f)
            )
        }
    }

    fun requestFocusPlot(plot: MouzaPlot) {
        _uiState.update { it.copy(focusPlotRequest = plot) }
    }

    fun consumeFocusPlot() {
        _uiState.update { it.copy(focusPlotRequest = null) }
    }

    fun sqFeetPerGridPixel(): Double {
        val gw = _uiState.value.selectedPlots.firstOrNull()?.gridWidth ?: 1500
        return MouzaPdfEngine.computeSqFeetPerGridPixel(
            pdfPageWidthPts = _uiState.value.pdfPageWidthPts,
            gridWidth = gw,
            scale = _uiState.value.selectedScale,
            customCalibrationMultiplier = _uiState.value.calibrationMultiplier
        )
    }

    fun plotAreaSqFeet(plot: MouzaPlot): Double {
        return plot.exactAreaSqFeet ?: (plot.pixelCount * sqFeetPerGridPixel())
    }

    fun totalSelectedSqFeet(): Double {
        return _uiState.value.selectedPlots.sumOf { plotAreaSqFeet(it) }
    }

    fun exportSelectedOnlyPng(selectedOnly: Boolean = true) {
        val state = _uiState.value
        val source = state.displayBitmap ?: return
        if (selectedOnly && state.selectedPlots.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "আগে কমপক্ষে ১টি দাগ সিলেক্ট করুন।") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "HD PNG তৈরি হচ্ছে...") }
            try {
                val sqFtPerPx = sqFeetPerGridPixel()
                val res = PngExporter.exportSelectedMouzaPlotsPng(
                    context = appContext,
                    sourceBitmap = source,
                    plots = state.selectedPlots,
                    sqFeetPerGridPixel = sqFtPerPx,
                    khatianTitle = state.khatianTitle,
                    docFile = activeDocFile,
                    isPdf = activeIsPdf,
                    pageIndex = state.currentPage,
                    pageWidthPts = state.pdfPageWidthPts,
                    pageHeightPts = state.pdfPageHeightPts,
                    scale = state.selectedScale,
                    calibrationMultiplier = state.calibrationMultiplier,
                    selectedOnly = selectedOnly
                )
                val totalSqFt = max(totalSelectedSqFeet(), 0.0)
                val area = LandCalculators.fromSqFeet(totalSqFt)
                if (state.selectedPlots.isNotEmpty()) {
                    prefs.addMeasurementRecord(
                        SavedMeasurementRecord(
                            id = "mouza_${System.currentTimeMillis()}",
                            title = state.khatianTitle.ifBlank { "মৌজা সিলেক্টেড (${state.selectedPlots.size} দাগ)" },
                            source = if (activeIsPdf) "Mouza PDF" else "Mouza Image",
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
                        lastSavedPath = res.displayPath,
                        statusMessage = "সেভ হয়েছে: ${res.displayPath}"
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
