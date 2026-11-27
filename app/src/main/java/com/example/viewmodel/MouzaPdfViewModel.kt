package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.engine.GeometryAndAccuracyEngine
import com.example.engine.LandCalculators
import com.example.engine.MouzaN01Extensions
import com.example.engine.MouzaPdfEngine
import com.example.engine.PlotWallIndex
import com.example.engine.PngExporter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.CalibrationRefLine
import com.example.model.GlobalSurveySettings
import com.example.model.KhatianRecord
import com.example.model.MeasureBetweenResult
import com.example.model.MouzaFilterPreset
import com.example.model.MouzaPlot
import com.example.model.MouzaScale
import com.example.model.MouzaSelectionTool
import com.example.model.ParcelTableRow
import com.example.model.PdfInspectionReport
import com.example.model.SavedMeasurementRecord
import com.example.model.SavedProjectType
import com.example.model.SavedSurveyProject
import com.example.model.ScaleCalibrationProfile
import com.example.model.SerializablePoint
import com.example.model.SerializedMouzaPlot
import com.example.model.ShoelaceStepRow
import com.example.ui.theme.PlotPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class MouzaPdfUiState(
    val isLoadingPdf: Boolean = false,
    val isRenderingHd: Boolean = false,
    val isAutoFilling: Boolean = false,
    val isExporting: Boolean = false,
    val isEncryptedPdfError: Boolean = false,
    val fileName: String = "",
    val isPdf: Boolean = true,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val renderDpi: Int = 300,
    val pageRotationDeg: Int = 0,
    val displayBitmap: Bitmap? = null,
    val cleanedPreviewBitmap: Bitmap? = null,
    val pageCompareIndex: Int? = null,
    val pageCompareBitmap: Bitmap? = null,
    val viewportTile: MouzaPdfEngine.ViewportTileResult? = null,
    val hdReady: Boolean = false,
    val pdfPageWidthPts: Int = 1000,
    val pdfPageHeightPts: Int = 1000,
    val pdfInspectionReport: PdfInspectionReport? = null,
    val selectedScale: MouzaScale = MouzaScale.SCALE_16,
    val calibrationMultiplier: Double = 1.0,
    val calibrationScaleX: Double = 1.0,
    val calibrationScaleY: Double = 1.0,
    val calibrationErrPct: Double = 0.6,
    val paperShrinkageNote: String = "",
    val calibrationRefLines: List<CalibrationRefLine> = emptyList(),
    val globalSettings: GlobalSurveySettings = GlobalSurveySettings(),
    val gapSealLevel: Int = 1, // 0 = Exact, 1 = Balanced, 2 = Strong
    val activeTool: MouzaSelectionTool = MouzaSelectionTool.POLYGON_DRAW,
    val selectedPlots: List<MouzaPlot> = emptyList(),
    val canRedo: Boolean = false,
    val selectedVertex: Pair<Int, Int>? = null, // (plotId, vertexIndex)
    val manualPolygonDraft: List<Pair<Float, Float>> = emptyList(),
    val selectedDraftIdx: Int? = null,
    val tapeDraft: List<Pair<Float, Float>> = emptyList(),
    val selectedTapeIdx: Int? = null,
    val completedTapes: List<List<Pair<Float, Float>>> = emptyList(),
    val measureBetweenResult: MeasureBetweenResult? = null,
    val khatianTitle: String = "",
    val khatianDocName: String = "",
    val khatianIsPdf: Boolean = true,
    val khatianPageCount: Int = 0,
    val khatianCurrentPage: Int = 0,
    val khatianPreviewBitmap: Bitmap? = null,
    val isLoadingKhatian: Boolean = false,
    val khatianRecords: List<KhatianRecord> = emptyList(),
    val activeSearchTarget: Pair<String, String>? = null, // (khatianNo, dagNo)
    val highlightedPlotId: Int? = null,
    val filterPreset: MouzaFilterPreset = MouzaFilterPreset.NORMAL,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val overlayMaskBitmap: Bitmap? = null,
    val focusPlotRequest: MouzaPlot? = null,
    val activeProjectId: String? = null,
    val savedProjects: List<SavedSurveyProject> = emptyList(),
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
    private var activeKhatianFile: File? = null
    private var activeKhatianIsPdf: Boolean = true
    private var nextPlotId = 1
    private var renderJob: Job? = null
    private var viewportTileJob: Job? = null
    private var autoFillJob: Job? = null
    private var wallIndexFallback: PlotWallIndex? = null
    private val redoStack = ArrayDeque<MouzaPlot>()

    init {
        viewModelScope.launch {
            prefs.savedProjectsFlow.collect { all ->
                _uiState.update {
                    it.copy(savedProjects = all.filter { p -> p.type == SavedProjectType.MOUZA_PDF })
                }
            }
        }
        viewModelScope.launch {
            prefs.globalSettingsFlow.collect { gs ->
                _uiState.update { it.copy(globalSettings = gs) }
            }
        }
    }

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
                    isEncryptedPdfError = false,
                    activeProjectId = null,
                    statusMessage = "নকশা ফাইল লোড হচ্ছে...",
                    selectedPlots = emptyList(),
                    canRedo = false,
                    manualPolygonDraft = emptyList(),
                    selectedDraftIdx = null,
                    tapeDraft = emptyList(),
                    selectedTapeIdx = null,
                    completedTapes = emptyList(),
                    viewportTile = null,
                    overlayMaskBitmap = null,
                    cleanedPreviewBitmap = null,
                    pageCompareBitmap = null,
                    pageCompareIndex = null,
                    pageRotationDeg = 0
                )
            }
            try {
                val displayName = resolveFileName(uri)
                val mime = appContext.contentResolver.getType(uri)
                val info = MouzaPdfEngine.copyDocumentToCache(appContext, uri, displayName, mime)
                activeDocFile = info.tempFile
                activeIsPdf = info.isPdf
                wallIndexFallback = null

                val report = withContext(Dispatchers.IO) {
                    MouzaN01Extensions.inspectPdfStructure(info.tempFile)
                }

                _uiState.update {
                    it.copy(
                        fileName = info.fileName,
                        isPdf = info.isPdf,
                        pageCount = info.pageCount,
                        currentPage = 0,
                        pdfPageWidthPts = info.baseWidthPts,
                        pdfPageHeightPts = info.baseHeightPts,
                        pdfInspectionReport = report
                    )
                }
                loadPageInternal(0)
            } catch (sec: SecurityException) {
                _uiState.update {
                    it.copy(
                        isLoadingPdf = false,
                        isEncryptedPdfError = true,
                        statusMessage = "এই PDF পাসওয়ার্ড/এনক্রিপশন দ্বারা সুরক্ষিত — পাসওয়ার্ডমুক্ত কপি ব্যবহার করুন।"
                    )
                }
            } catch (e: Exception) {
                val isEnc = e.message?.contains("password", ignoreCase = true) == true ||
                    e.message?.contains("encrypt", ignoreCase = true) == true
                _uiState.update {
                    it.copy(
                        isLoadingPdf = false,
                        isEncryptedPdfError = isEnc,
                        statusMessage = if (isEnc) {
                            "এই PDF পাসওয়ার্ড-সুরক্ষিত — আনলক করা কপি নিন।"
                        } else {
                            "ফাইল খোলা যায়নি: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
                        }
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
                    isLoadingPdf = true,
                    currentPage = pageIndex,
                    selectedPlots = emptyList(),
                    canRedo = false,
                    manualPolygonDraft = emptyList(),
                    tapeDraft = emptyList(),
                    completedTapes = emptyList(),
                    viewportTile = null,
                    overlayMaskBitmap = null,
                    cleanedPreviewBitmap = null,
                    hdReady = false
                )
            }
            loadPageInternal(pageIndex)
        }
    }

    private suspend fun loadPageInternal(pageIndex: Int) {
        val file = activeDocFile ?: return
        val state = _uiState.value
        val base = MouzaPdfEngine.renderBaseDocument(
            appContext, file, activeIsPdf, pageIndex, ultraHd = true, renderDpi = state.renderDpi
        )
        val rotated = if (state.pageRotationDeg != 0) {
            withContext(Dispatchers.Default) {
                MouzaN01Extensions.rotateBitmapDeg(base.bitmap, state.pageRotationDeg)
            }
        } else base.bitmap
        val wPts = if (state.pageRotationDeg == 90 || state.pageRotationDeg == 270) base.pdfPageHeightPts else base.pdfPageWidthPts
        val hPts = if (state.pageRotationDeg == 90 || state.pageRotationDeg == 270) base.pdfPageWidthPts else base.pdfPageHeightPts

        _uiState.update {
            it.copy(
                displayBitmap = rotated,
                pdfPageWidthPts = wPts,
                pdfPageHeightPts = hPts,
                hdReady = true,
                isRenderingHd = false,
                isLoadingPdf = false,
                statusMessage = "ম্যাপ প্রস্তুত (${state.renderDpi} DPI) — দাগের ভেতরে ট্যাপ বা পয়েন্টার ব্যবহার করুন"
            )
        }
    }

    internal fun shouldReuseViewportTile(
        existing: MouzaPdfEngine.ViewportTileResult?,
        zoomScale: Float,
        normLeft: Float,
        normTop: Float,
        normRight: Float,
        normBottom: Float,
        viewportWidthPx: Int,
        viewportHeightPx: Int
    ): Boolean {
        if (existing == null || existing.bitmap.isRecycled) return false
        val coversRect = normLeft >= existing.normLeft &&
            normRight <= existing.normRight &&
            normTop >= existing.normTop &&
            normBottom <= existing.normBottom
        val zoomMatches = zoomScale in (existing.zoomScale * 0.85f)..(existing.zoomScale * 1.22f)
        val hasEnoughRes = existing.bitmap.width >= (viewportWidthPx * 1.15f).roundToInt().coerceAtMost(2400) ||
            existing.bitmap.height >= (viewportHeightPx * 1.15f).roundToInt().coerceAtMost(2600)
        return coversRect && zoomMatches && hasEnoughRes
    }

    /**
     * Silently sharpens the visible viewport at high zoom (>= 2.2x) with 30% padded caching
     * and crisp re-rendering whenever the user zooms deeper 2x-1000x.
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
        if (zoomScale < 2.2f) {
            viewportTileJob?.cancel()
            if (_uiState.value.viewportTile != null) {
                _uiState.update { it.copy(viewportTile = null) }
            }
            return
        }

        val existing = _uiState.value.viewportTile
        if (shouldReuseViewportTile(existing, zoomScale, normLeft, normTop, normRight, normBottom, viewportWidthPx, viewportHeightPx)) {
            return
        }

        val spanW = (normRight - normLeft).coerceAtLeast(0.0005f)
        val spanH = (normBottom - normTop).coerceAtLeast(0.0005f)
        val padL = (normLeft - spanW * 0.28f).coerceIn(0f, 0.9995f)
        val padT = (normTop - spanH * 0.28f).coerceIn(0f, 0.9995f)
        val padR = (normRight + spanW * 0.28f).coerceIn(padL + 0.0005f, 1f)
        val padB = (normBottom + spanH * 0.28f).coerceIn(padT + 0.0005f, 1f)

        viewportTileJob?.cancel()
        viewportTileJob = viewModelScope.launch {
            delay(120)
            val tile = MouzaPdfEngine.renderViewportTile(
                docFile = file,
                isPdf = activeIsPdf,
                pageIndex = _uiState.value.currentPage,
                normLeft = padL,
                normTop = padT,
                normRight = padR,
                normBottom = padB,
                targetWidthPx = (viewportWidthPx * 1.85f).roundToInt().coerceIn(900, 2400),
                targetHeightPx = (viewportHeightPx * 1.85f).roundToInt().coerceIn(900, 2600)
            )
            if (tile != null && MouzaPdfEngine.hasInkPixels(tile.bitmap)) {
                _uiState.update {
                    it.copy(viewportTile = tile.copy(zoomScale = zoomScale))
                }
            } else {
                tile?.bitmap?.recycle()
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
                if (plot.isLocked) return@map plot
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
        if (plot.isLocked) {
            _uiState.update { it.copy(statusMessage = "দাগ #${plot.plotIndex} লক করা আছে — ক্যালিব্রেট করতে আগে আনলক করুন") }
            return
        }
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
                if (p.isLocked) return@map p
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

    fun togglePlotLock(plotId: Int) {
        val current = _uiState.value.selectedPlots.find { it.id == plotId } ?: return
        setPlotLocked(plotId, !current.isLocked)
    }

    fun setPlotLocked(plotId: Int, locked: Boolean) {
        _uiState.update { state ->
            val target = state.selectedPlots.find { it.id == plotId } ?: return@update state
            val updated = state.selectedPlots.map { p ->
                if (p.id == plotId) p.copy(isLocked = locked) else p
            }
            val clearedVertex = if (locked && state.selectedVertex?.first == plotId) null else state.selectedVertex
            val label = target.dagNumber.ifBlank { "#${target.plotIndex}" }
            state.copy(
                selectedPlots = updated,
                selectedVertex = clearedVertex,
                statusMessage = if (locked) {
                    "দাগ $label লক করা হয়েছে — এডিট, রিসাইজ বা মুভ বন্ধ"
                } else {
                    "দাগ $label আনলক করা হয়েছে — এখন এডিট করতে পারবেন"
                }
            )
        }
    }

    fun setTool(tool: MouzaSelectionTool) {
        val hint = when (tool) {
            MouzaSelectionTool.POLYGON_DRAW -> "দাগের কোণাগুলোতে ট্যাপ বা পয়েন্টারে বসান — বিন্দু টেনে/ডিলিটও করা যাবে"
            MouzaSelectionTool.AUTO_TAP -> "দাগের ভেতরে ট্যাপ করলে অটো সিলেক্ট হবে (কোণা টেনে ঠিক করা যাবে)"
            MouzaSelectionTool.EDIT_CORNERS -> "হলুদ কোণা ট্যাপ করে সিলেক্ট/ডিলিট বা ড্র্যাগ করে সীমানা ঠিক করুন"
            MouzaSelectionTool.TAPE_MEASURE -> "ম্যাপে ট্যাপ করে বিন্দু বসান — দূরত্ব ফুট/লিংকে দেখাবে"
            MouzaSelectionTool.PAN_ONLY -> "শুধু জুম ও প্যান মোড"
        }
        _uiState.update {
            it.copy(
                activeTool = tool,
                selectedVertex = null,
                selectedDraftIdx = null,
                statusMessage = hint
            )
        }
    }

    fun onPointerPlace(normX: Float, normY: Float) {
        val state = _uiState.value
        if (state.displayBitmap == null) return
        when (state.activeTool) {
            MouzaSelectionTool.PAN_ONLY -> return
            MouzaSelectionTool.EDIT_CORNERS -> snapNearestVertexTo(normX, normY)
            MouzaSelectionTool.POLYGON_DRAW -> {
                _uiState.update {
                    val next = it.manualPolygonDraft + (normX to normY)
                    it.copy(
                        manualPolygonDraft = next,
                        selectedDraftIdx = null
                    )
                }
            }
            MouzaSelectionTool.TAPE_MEASURE -> {
                _uiState.update {
                    it.copy(tapeDraft = it.tapeDraft + (normX to normY))
                }
            }
            MouzaSelectionTool.AUTO_TAP -> onMapTap(normX, normY)
        }
    }

    fun onMapTap(normX: Float, normY: Float, hitRadiusNorm: Float = 0.004f) {
        val state = _uiState.value
        if (state.displayBitmap == null) return
        val maxHitSq = (hitRadiusNorm * hitRadiusNorm).coerceAtLeast(1e-7f)

        when (state.activeTool) {
            MouzaSelectionTool.PAN_ONLY -> return
            MouzaSelectionTool.EDIT_CORNERS -> {
                // First check if user tapped near an existing corner (unlocked preferred, block if locked)
                val hitAnyVertex = findNearestVertex(state.selectedPlots, normX, normY, maxDistSq = maxHitSq * 1.6f)
                if (hitAnyVertex != null) {
                    val hitPlot = state.selectedPlots.find { it.id == hitAnyVertex.first }
                    if (hitPlot?.isLocked == true) {
                        _uiState.update {
                            it.copy(
                                selectedVertex = null,
                                statusMessage = "দাগ #${hitPlot.plotIndex} লক করা আছে — এডিট করতে আগে আনলক করুন"
                            )
                        }
                        return
                    }
                    _uiState.update {
                        it.copy(
                            selectedVertex = hitAnyVertex,
                            statusMessage = "কোণা সিলেক্ট হয়েছে — ড্র্যাগ করুন বা 'বিন্দু ডিলিট' চাপুন"
                        )
                    }
                    return
                }
                // Otherwise allow tapping near an edge midpoint to split edge for fine adjustment
                trySplitNearestEdge(normX, normY)
                return
            }
            MouzaSelectionTool.POLYGON_DRAW -> {
                // Only select an existing draft point if tapped right on top of its screen dot
                val hitDraft = findNearestDraftIndex(state.manualPolygonDraft, normX, normY, maxDistSq = maxHitSq)
                if (hitDraft != null) {
                    _uiState.update {
                        it.copy(
                            selectedDraftIdx = if (it.selectedDraftIdx == hitDraft) null else hitDraft,
                            statusMessage = if (it.selectedDraftIdx == hitDraft) null
                            else "বিন্দু #${hitDraft + 1} সিলেক্ট হয়েছে — ড্র্যাগ বা ডিলিট করুন"
                        )
                    }
                } else {
                    _uiState.update {
                        val next = it.manualPolygonDraft + (normX to normY)
                        it.copy(
                            manualPolygonDraft = next,
                            selectedDraftIdx = null
                        )
                    }
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
                            val target = state.activeSearchTarget
                            val taggedPlot = if (target != null) {
                                res.plot.copy(
                                    khatianNumber = target.first.ifBlank { res.plot.khatianNumber },
                                    dagNumber = target.second.ifBlank { res.plot.dagNumber }
                                )
                            } else res.plot
                            val updated = state.selectedPlots + taggedPlot
                            _uiState.update {
                                it.copy(
                                    isAutoFilling = false,
                                    selectedPlots = updated,
                                    activeSearchTarget = null,
                                    highlightedPlotId = taggedPlot.id,
                                    canRedo = false,
                                    statusMessage = if (target != null) {
                                        "খতিয়ান ${target.first} • দাগ ${target.second} প্লট যুক্ত হয়েছে!"
                                    } else {
                                        "দাগ #${taggedPlot.plotIndex} সিলেক্ট হয়েছে (প্রয়োজনে 'পয়েন্ট এডিট'-এ কোণা ঠিক করুন)"
                                    }
                                )
                            }
                        }
                        is MouzaPdfEngine.AutoFillResult.AlreadySelected -> {
                            val existing = state.selectedPlots.find { it.id == res.plotId }
                            if (existing?.isLocked == true) {
                                _uiState.update {
                                    it.copy(
                                        isAutoFilling = false,
                                        statusMessage = "দাগ #${existing.plotIndex} লক করা আছে — আনসিলেক্ট করতে আগে আনলক করুন"
                                    )
                                }
                            } else {
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
        val targetPlot = state.selectedPlots.find { it.id == plotId } ?: return
        if (targetPlot.isLocked) {
            _uiState.update { it.copy(statusMessage = "দাগ #${targetPlot.plotIndex} লক করা আছে — সরাতে আগে আনলক করুন") }
            return
        }
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

    fun snapNearestVertexTo(normX: Float, normY: Float) {
        val state = _uiState.value
        val unlockedPlots = state.selectedPlots.filterNot { it.isLocked }
        val chosen = state.selectedVertex?.let { sv ->
            val owner = state.selectedPlots.find { it.id == sv.first }
            if (owner?.isLocked == true) {
                _uiState.update { it.copy(statusMessage = "দাগ #${owner.plotIndex} লক করা আছে — আগে আনলক করুন") }
                return
            }
            sv
        } ?: findNearestVertex(unlockedPlots, normX, normY, maxDistSq = Float.MAX_VALUE)

        if (chosen != null) {
            updatePlotVertex(chosen.first, chosen.second, normX, normY)
            _uiState.update {
                it.copy(
                    selectedVertex = chosen,
                    statusMessage = "কোণা পয়েন্টারে সরানো হয়েছে"
                )
            }
        } else if (state.selectedPlots.isNotEmpty()) {
            _uiState.update { it.copy(statusMessage = "লক করা দাগ এডিট করা যাবে না — আগে আনলক করুন") }
        }
    }

    fun selectVertex(plotId: Int, vertexIdx: Int) {
        val plot = _uiState.value.selectedPlots.find { it.id == plotId }
        if (plot?.isLocked == true) {
            _uiState.update { it.copy(statusMessage = "দাগ #${plot.plotIndex} লক করা আছে — আগে আনলক করুন") }
            return
        }
        _uiState.update { it.copy(selectedVertex = plotId to vertexIdx) }
    }

    fun deleteSelectedOrNearestVertex(centerNormX: Float, centerNormY: Float) {
        val state = _uiState.value
        val unlockedPlots = state.selectedPlots.filterNot { it.isLocked }
        val target = state.selectedVertex ?: findNearestVertex(unlockedPlots, centerNormX, centerNormY, maxDistSq = Float.MAX_VALUE)
        if (target == null) {
            val msg = if (state.selectedPlots.any { it.isLocked }) {
                "লক করা দাগের কোণা ডিলিট করা যাবে না — আগে আনলক করুন"
            } else {
                "ডিলিট করার মতো কোনো কোণা পাওয়া যায়নি"
            }
            _uiState.update { it.copy(statusMessage = msg) }
            return
        }
        val plot = state.selectedPlots.find { it.id == target.first } ?: return
        if (plot.isLocked) {
            _uiState.update { it.copy(statusMessage = "দাগ #${plot.plotIndex} লক করা আছে — আগে আনলক করুন") }
            return
        }
        val poly = plot.effectivePolygon()?.toMutableList() ?: return
        if (poly.size <= 3) {
            _uiState.update {
                it.copy(statusMessage = "দাগে কমপক্ষে ৩টি কোণা থাকতে হবে (পুরো দাগ বাদ দিতে 'বাদ' চাপুন)")
            }
            return
        }
        if (target.second !in poly.indices) return
        poly.removeAt(target.second)
        val sqFt = MouzaPdfEngine.computePolygonAreaSqFeet(
            poly, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, state.calibrationMultiplier
        )
        val cx = poly.map { it.first }.average().toFloat()
        val cy = poly.map { it.second }.average().toFloat()
        val updated = state.selectedPlots.map { p ->
            if (p.id != plot.id) p else p.copy(
                polygonNormalized = poly,
                manualPolygonNormalized = if (p.manualPolygonNormalized != null) poly else null,
                centroidNormalized = cx to cy,
                exactAreaSqFeet = sqFt
            )
        }
        _uiState.update {
            it.copy(
                selectedPlots = updated,
                selectedVertex = null,
                statusMessage = "অপ্রয়োজনীয় কোণা ডিলিট করা হয়েছে"
            )
        }
    }

    fun selectDraftPoint(idx: Int?) {
        _uiState.update { it.copy(selectedDraftIdx = idx) }
    }

    fun updateManualDraftPoint(idx: Int, newNormX: Float, newNormY: Float) {
        _uiState.update { state ->
            val draft = state.manualPolygonDraft.toMutableList()
            if (idx !in draft.indices) return@update state
            draft[idx] = newNormX.coerceIn(0f, 1f) to newNormY.coerceIn(0f, 1f)
            state.copy(manualPolygonDraft = draft, selectedDraftIdx = idx)
        }
    }

    fun deleteSelectedOrLastDraftPoint() {
        _uiState.update { state ->
            val draft = state.manualPolygonDraft.toMutableList()
            if (draft.isEmpty()) return@update state
            val idx = state.selectedDraftIdx?.takeIf { it in draft.indices } ?: draft.lastIndex
            draft.removeAt(idx)
            state.copy(
                manualPolygonDraft = draft,
                selectedDraftIdx = draft.lastIndex.takeIf { it >= 0 },
                statusMessage = "বিন্দু ডিলিট করা হয়েছে"
            )
        }
    }

    fun clearManualDraft() {
        _uiState.update {
            it.copy(
                manualPolygonDraft = emptyList(),
                selectedDraftIdx = null,
                statusMessage = "ড্রাফট বিন্দু মুছে ফেলা হয়েছে"
            )
        }
    }

    private fun findNearestVertex(
        plots: List<MouzaPlot>,
        normX: Float,
        normY: Float,
        maxDistSq: Float
    ): Pair<Int, Int>? {
        var bestPlotId = -1
        var bestIdx = -1
        var bestDistSq = maxDistSq
        plots.forEach { plot ->
            val poly = plot.effectivePolygon() ?: return@forEach
            poly.forEachIndexed { idx, (vx, vy) ->
                val dSq = (vx - normX) * (vx - normX) + (vy - normY) * (vy - normY)
                if (dSq < bestDistSq) {
                    bestDistSq = dSq
                    bestPlotId = plot.id
                    bestIdx = idx
                }
            }
        }
        return if (bestPlotId != -1 && bestIdx != -1) bestPlotId to bestIdx else null
    }

    private fun findNearestDraftIndex(
        draft: List<Pair<Float, Float>>,
        normX: Float,
        normY: Float,
        maxDistSq: Float
    ): Int? {
        var bestIdx = -1
        var bestDistSq = maxDistSq
        draft.forEachIndexed { idx, (vx, vy) ->
            val dSq = (vx - normX) * (vx - normX) + (vy - normY) * (vy - normY)
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                bestIdx = idx
            }
        }
        return if (bestIdx >= 0) bestIdx else null
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
            val targetPlot = state.selectedPlots.find { it.id == bestPlotId }
            if (targetPlot?.isLocked == true) {
                _uiState.update { it.copy(statusMessage = "দাগ #${targetPlot.plotIndex} লক করা আছে — আগে আনলক করুন") }
                return
            }
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
        val target = state.activeSearchTarget
        val plot = MouzaPdfEngine.buildManualPolygonPlot(
            normalizedPoints = draft,
            gridWidth = gw,
            gridHeight = gh,
            plotId = nextPlotId,
            plotIndex = state.selectedPlots.size + 1,
            color = color
        ).copy(
            exactAreaSqFeet = sqFt,
            khatianNumber = target?.first ?: "",
            dagNumber = target?.second ?: ""
        )
        nextPlotId++
        redoStack.clear()
        val updated = state.selectedPlots + plot
        _uiState.update {
            it.copy(
                manualPolygonDraft = emptyList(),
                selectedDraftIdx = null,
                selectedPlots = updated,
                activeSearchTarget = null,
                highlightedPlotId = plot.id,
                canRedo = false,
                statusMessage = if (target != null) {
                    "খতিয়ান ${target.first} • দাগ ${target.second} যুক্ত হয়েছে"
                } else {
                    "ম্যানুয়াল দাগ #${plot.plotIndex} যুক্ত হয়েছে"
                }
            )
        }
    }

    fun undoManualPoint() {
        val draft = _uiState.value.manualPolygonDraft
        if (draft.isNotEmpty()) {
            _uiState.update {
                val next = draft.dropLast(1)
                it.copy(manualPolygonDraft = next, selectedDraftIdx = next.lastIndex.takeIf { idx -> idx >= 0 })
            }
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
        val removed = _uiState.value.selectedPlots.find { it.id == plotId } ?: return
        if (removed.isLocked) {
            _uiState.update {
                it.copy(statusMessage = "দাগ #${removed.plotIndex} লক করা আছে — বাদ দিতে আগে আনলক করুন")
            }
            return
        }
        redoStack.addLast(removed)
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
        val lastUnlockedIdx = current.indexOfLast { !it.isLocked }
        if (lastUnlockedIdx < 0) {
            _uiState.update {
                it.copy(statusMessage = "সব দাগ লক করা আছে — আনডু করতে আগে আনলক করুন")
            }
            return
        }
        val removed = current[lastUnlockedIdx]
        redoStack.addLast(removed)
        val updated = current.filterIndexed { i, _ -> i != lastUnlockedIdx }
            .mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) }
        _uiState.update {
            it.copy(
                selectedPlots = updated,
                canRedo = true,
                statusMessage = "সর্বশেষ আনলক দাগ বাদ দেওয়া হয়েছে"
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
        val current = _uiState.value.selectedPlots
        val keptLocked = current.filter { it.isLocked }
            .mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) }
        redoStack.clear()
        _uiState.update {
            it.copy(
                selectedPlots = keptLocked,
                canRedo = false,
                selectedVertex = null,
                manualPolygonDraft = emptyList(),
                tapeDraft = emptyList(),
                completedTapes = emptyList(),
                overlayMaskBitmap = null,
                statusMessage = if (keptLocked.isNotEmpty()) {
                    "আনলক দাগ মুছে ফেলা হয়েছে (লক করা ${keptLocked.size}টি সুরক্ষিত)"
                } else {
                    "সব পরিমাপ পরিষ্কার করা হয়েছে"
                }
            )
        }
    }

    fun updatePlotTags(plotId: Int, dagNumber: String, khatianNumber: String) {
        val target = _uiState.value.selectedPlots.find { it.id == plotId } ?: return
        if (target.isLocked) {
            _uiState.update { it.copy(statusMessage = "দাগ #${target.plotIndex} লক করা আছে — আগে আনলক করুন") }
            return
        }
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
        val state = _uiState.value
        val poly = plot.effectivePolygon()
        if (poly != null && poly.size >= 3) {
            val pageFtW = (state.pdfPageWidthPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleX
            val pageFtH = (state.pdfPageHeightPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleY
            val outerFt = poly.map { (it.first * pageFtW) to (it.second * pageFtH) }
            val holesFt = plot.holePolygonsNormalized.map { h ->
                h.map { (it.first * pageFtW) to (it.second * pageFtH) }
            }
            return GeometryAndAccuracyEngine.computeNetAreaWithHolesSqFt(outerFt, holesFt)
        }
        return plot.exactAreaSqFeet ?: (plot.pixelCount * sqFeetPerGridPixel())
    }

    fun totalSelectedSqFeet(): Double {
        return _uiState.value.selectedPlots.filterNot { it.isRoad }.sumOf { plotAreaSqFeet(it) }
    }

    fun exportSelectedOnlyPdf(selectedOnly: Boolean = true) =
        exportSelectedOnlyPng(selectedOnly = selectedOnly, asPdf = true)

    fun exportSelectedOnlyPng(selectedOnly: Boolean = true, asPdf: Boolean = false) {
        val state = _uiState.value
        val source = state.displayBitmap ?: return
        if (selectedOnly && state.selectedPlots.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "আগে কমপক্ষে ১টি দাগ সিলেক্ট করুন।") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    statusMessage = if (asPdf) "মৌজা শিট PDF তৈরি হচ্ছে..." else "মৌজা শিট PNG তৈরি হচ্ছে..."
                )
            }
            try {
                val sqFtPerPx = sqFeetPerGridPixel()
                val profile = prefs.surveyorProfileFlow.first()
                val res = if (asPdf) {
                    PngExporter.exportSelectedMouzaPlotsPdf(
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
                        selectedOnly = selectedOnly,
                        surveyorProfile = profile
                    )
                } else {
                    PngExporter.exportSelectedMouzaPlotsPng(
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
                        selectedOnly = selectedOnly,
                        surveyorProfile = profile
                    )
                }
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
                        statusMessage = "সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun showStatusHint(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun cycleActivePoint(delta: Int) {
        val state = _uiState.value
        when (state.activeTool) {
            MouzaSelectionTool.POLYGON_DRAW -> {
                val n = state.manualPolygonDraft.size
                if (n == 0) return
                val cur = state.selectedDraftIdx ?: (n - 1)
                val next = ((cur + delta) % n + n) % n
                _uiState.update { it.copy(selectedDraftIdx = next) }
            }
            MouzaSelectionTool.EDIT_CORNERS -> {
                val allVertices = state.selectedPlots
                    .filterNot { it.isLocked }
                    .flatMap { plot ->
                        val poly = plot.effectivePolygon() ?: emptyList()
                        poly.indices.map { idx -> plot.id to idx }
                    }
                if (allVertices.isEmpty()) {
                    if (state.selectedPlots.any { it.isLocked }) {
                        _uiState.update { it.copy(statusMessage = "সব দাগ লক করা আছে — কোণা সরাতে আনলক করুন") }
                    }
                    return
                }
                val curIdx = state.selectedVertex?.let { sv -> allVertices.indexOf(sv) } ?: -1
                val nextIdx = if (curIdx < 0) {
                    if (delta >= 0) 0 else allVertices.lastIndex
                } else {
                    ((curIdx + delta) % allVertices.size + allVertices.size) % allVertices.size
                }
                _uiState.update { it.copy(selectedVertex = allVertices[nextIdx]) }
            }
            MouzaSelectionTool.TAPE_MEASURE -> {
                val n = state.tapeDraft.size
                if (n == 0) return
                val cur = state.selectedTapeIdx ?: (n - 1)
                val next = ((cur + delta) % n + n) % n
                _uiState.update { it.copy(selectedTapeIdx = next) }
            }
            else -> Unit
        }
    }

    fun nudgeActivePoint(deltaNormX: Float, deltaNormY: Float) {
        val state = _uiState.value
        when (state.activeTool) {
            MouzaSelectionTool.POLYGON_DRAW -> {
                if (state.manualPolygonDraft.isEmpty()) return
                val idx = state.selectedDraftIdx?.takeIf { it in state.manualPolygonDraft.indices }
                    ?: state.manualPolygonDraft.lastIndex
                val (ox, oy) = state.manualPolygonDraft[idx]
                updateManualDraftPoint(idx, ox + deltaNormX, oy + deltaNormY)
            }
            MouzaSelectionTool.EDIT_CORNERS -> {
                val target = state.selectedVertex ?: run {
                    val lastUnlockedPlot = state.selectedPlots.lastOrNull { !it.isLocked } ?: run {
                        if (state.selectedPlots.any { it.isLocked }) {
                            _uiState.update { it.copy(statusMessage = "সব দাগ লক করা আছে — আগে আনলক করুন") }
                        }
                        return
                    }
                    val poly = lastUnlockedPlot.effectivePolygon() ?: return
                    if (poly.isEmpty()) return
                    lastUnlockedPlot.id to 0
                }
                val plot = state.selectedPlots.find { it.id == target.first } ?: return
                if (plot.isLocked) {
                    _uiState.update { it.copy(statusMessage = "দাগ #${plot.plotIndex} লক করা আছে — আগে আনলক করুন") }
                    return
                }
                val poly = plot.effectivePolygon() ?: return
                if (target.second !in poly.indices) return
                val (ox, oy) = poly[target.second]
                _uiState.update { it.copy(selectedVertex = target) }
                updatePlotVertex(target.first, target.second, ox + deltaNormX, oy + deltaNormY)
            }
            MouzaSelectionTool.TAPE_MEASURE -> {
                if (state.tapeDraft.isEmpty()) return
                val idx = state.selectedTapeIdx?.takeIf { it in state.tapeDraft.indices }
                    ?: state.tapeDraft.lastIndex
                val draft = state.tapeDraft.toMutableList()
                val (ox, oy) = draft[idx]
                draft[idx] = (ox + deltaNormX).coerceIn(0f, 1f) to (oy + deltaNormY).coerceIn(0f, 1f)
                _uiState.update { it.copy(tapeDraft = draft, selectedTapeIdx = idx) }
            }
            else -> Unit
        }
    }

    fun saveMouzaProject(customTitle: String) {
        val state = _uiState.value
        val srcFile = activeDocFile
        if (srcFile == null || !srcFile.exists() || state.displayBitmap == null) {
            _uiState.update { it.copy(statusMessage = "আগে একটি মৌজা PDF বা ছবি খুলুন।") }
            return
        }
        viewModelScope.launch {
            try {
                val savedDir = File(appContext.filesDir, "saved_mouza_docs").apply { mkdirs() }
                val destFile = withContext(Dispatchers.IO) {
                    if (srcFile.parentFile?.absolutePath == savedDir.absolutePath && srcFile.exists()) {
                        srcFile
                    } else {
                        val ext = srcFile.extension.ifBlank { if (activeIsPdf) "pdf" else "png" }
                        val target = File(savedDir, "mouza_doc_${System.currentTimeMillis()}.$ext")
                        srcFile.copyTo(target, overwrite = true)
                        target
                    }
                }
                activeDocFile = destFile

                val totalSqFt = totalSelectedSqFeet()
                val area = LandCalculators.fromSqFeet(totalSqFt)
                val projId = state.activeProjectId ?: "mouza_proj_${System.currentTimeMillis()}"
                val cleanTitle = customTitle.trim().ifBlank {
                    state.khatianTitle.ifBlank {
                        state.fileName.ifBlank { "মৌজা প্রজেক্ট" }
                    }
                }

                val serializedPlots = state.selectedPlots.map { plot ->
                    val poly = plot.effectivePolygon() ?: listOf(
                        (plot.minX.toFloat() / plot.gridWidth) to (plot.minY.toFloat() / plot.gridHeight),
                        (plot.maxX.toFloat() / plot.gridWidth) to (plot.minY.toFloat() / plot.gridHeight),
                        (plot.maxX.toFloat() / plot.gridWidth) to (plot.maxY.toFloat() / plot.gridHeight),
                        (plot.minX.toFloat() / plot.gridWidth) to (plot.maxY.toFloat() / plot.gridHeight)
                    )
                    SerializedMouzaPlot(
                        id = plot.id,
                        plotIndex = plot.plotIndex,
                        dagNumber = plot.dagNumber,
                        khatianNumber = plot.khatianNumber,
                        colorArgb = plot.color.toArgb().toLong(),
                        polygonNormalized = poly.map { SerializablePoint(it.first, it.second) },
                        exactAreaSqFeet = plotAreaSqFeet(plot),
                        isLocked = plot.isLocked,
                        ownerName = plot.ownerName,
                        remarks = plot.remarks,
                        isRoad = plot.isRoad,
                        hideMeasures = plot.hideMeasures,
                        holePolygonsNormalized = plot.holePolygonsNormalized.map { h ->
                            h.map { SerializablePoint(it.first, it.second) }
                        }
                    )
                }

                val project = SavedSurveyProject(
                    id = projId,
                    title = cleanTitle,
                    type = SavedProjectType.MOUZA_PDF,
                    timestampMillis = System.currentTimeMillis(),
                    decimalArea = area.decimal,
                    kathaArea = area.katha,
                    sqFeetArea = area.sqFeet,
                    itemCount = serializedPlots.size.coerceAtLeast(state.manualPolygonDraft.size),
                    docFilePath = destFile.absolutePath,
                    docFileName = state.fileName,
                    isPdf = activeIsPdf,
                    pageIndex = state.currentPage,
                    pdfPageWidthPts = state.pdfPageWidthPts,
                    pdfPageHeightPts = state.pdfPageHeightPts,
                    mouzaScaleName = state.selectedScale.name,
                    calibrationMultiplier = state.calibrationMultiplier,
                    calibrationScaleX = state.calibrationScaleX,
                    calibrationScaleY = state.calibrationScaleY,
                    gapSealLevel = state.gapSealLevel,
                    filterPresetName = state.filterPreset.name,
                    brightness = state.brightness,
                    contrast = state.contrast,
                    khatianTitle = state.khatianTitle,
                    savedPlots = serializedPlots,
                    manualPolygonDraft = state.manualPolygonDraft.map { SerializablePoint(it.first, it.second) },
                    completedTapes = state.completedTapes.map { line -> line.map { SerializablePoint(it.first, it.second) } },
                    tapeDraft = state.tapeDraft.map { SerializablePoint(it.first, it.second) },
                    khatianFilePath = activeKhatianFile?.takeIf { it.exists() }?.absolutePath,
                    khatianFileName = state.khatianDocName,
                    khatianIsPdf = activeKhatianIsPdf,
                    khatianRecords = state.khatianRecords
                )

                prefs.saveSurveyProject(project)
                _uiState.update {
                    it.copy(
                        activeProjectId = projId,
                        statusMessage = "প্রজেক্ট ডাটা সংরক্ষিত হয়েছে — পরে রিস্টোর করতে পারবেন"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "প্রজেক্ট ডাটা সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
                }
            }
        }
    }

    fun restoreMouzaProjectById(projectId: String) {
        viewModelScope.launch {
            val proj = prefs.getSurveyProjectById(projectId)
            if (proj == null) {
                _uiState.update { it.copy(statusMessage = "সংরক্ষিত প্রজেক্টটি খুঁজে পাওয়া যায়নি") }
                return@launch
            }
            restoreMouzaProject(proj)
        }
    }

    fun restoreMouzaProject(project: SavedSurveyProject) {
        val docPath = project.docFilePath
        val docFile = docPath?.let { File(it) }
        if (docFile == null || !docFile.exists()) {
            _uiState.update { it.copy(statusMessage = "সংরক্ষিত PDF/ছবি ফাইলটি আর ফোনে নেই") }
            return
        }
        renderJob?.cancel()
        viewportTileJob?.cancel()
        autoFillJob?.cancel()
        redoStack.clear()

        renderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingPdf = true,
                    activeProjectId = project.id,
                    statusMessage = "সংরক্ষিত মৌজা কাজ লোড হচ্ছে..."
                )
            }
            try {
                activeDocFile = docFile
                activeIsPdf = project.isPdf
                wallIndexFallback = null

                val scaleEnum = runCatching { MouzaScale.valueOf(project.mouzaScaleName) }.getOrDefault(MouzaScale.SCALE_16)
                val filterEnum = runCatching { MouzaFilterPreset.valueOf(project.filterPresetName) }.getOrDefault(MouzaFilterPreset.NORMAL)
                val pageCnt = if (project.isPdf) {
                    withContext(Dispatchers.IO) {
                        android.os.ParcelFileDescriptor.open(docFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                            android.graphics.pdf.PdfRenderer(pfd).use { it.pageCount }
                        }
                    }
                } else 1

                val base = MouzaPdfEngine.renderBaseDocument(
                    appContext, docFile, project.isPdf, project.pageIndex.coerceIn(0, (pageCnt - 1).coerceAtLeast(0)), ultraHd = true
                )

                val aspect = base.pdfPageHeightPts.toFloat() / base.pdfPageWidthPts.toFloat().coerceAtLeast(1f)
                val gw = 1500
                val gh = (1500f * aspect).roundToInt().coerceAtLeast(200)

                val restoredPlots = project.savedPlots.map { sp ->
                    val pts = sp.polygonNormalized.map { it.x to it.y }
                    val sqFt = if (pts.size >= 3) {
                        MouzaPdfEngine.computePolygonAreaSqFeet(
                            pts, base.pdfPageWidthPts, base.pdfPageHeightPts, scaleEnum, project.calibrationMultiplier
                        )
                    } else sp.exactAreaSqFeet
                    val basePlot = MouzaPdfEngine.buildManualPolygonPlot(
                        normalizedPoints = pts,
                        gridWidth = gw,
                        gridHeight = gh,
                        plotId = sp.id,
                        plotIndex = sp.plotIndex,
                        color = Color(sp.colorArgb.toInt())
                    )
                    basePlot.copy(
                        dagNumber = sp.dagNumber,
                        khatianNumber = sp.khatianNumber,
                        exactAreaSqFeet = sqFt,
                        isLocked = sp.isLocked,
                        ownerName = sp.ownerName,
                        remarks = sp.remarks,
                        isRoad = sp.isRoad,
                        hideMeasures = sp.hideMeasures,
                        holePolygonsNormalized = sp.holePolygonsNormalized.map { h -> h.map { it.x to it.y } }
                    )
                }
                nextPlotId = (restoredPlots.maxOfOrNull { it.id } ?: 0) + 1

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
                        isLoadingPdf = false,
                        isRenderingHd = false,
                        hdReady = true,
                        fileName = project.docFileName,
                        isPdf = project.isPdf,
                        pageCount = pageCnt,
                        currentPage = project.pageIndex.coerceIn(0, (pageCnt - 1).coerceAtLeast(0)),
                        displayBitmap = base.bitmap,
                        viewportTile = null,
                        pdfPageWidthPts = base.pdfPageWidthPts,
                        pdfPageHeightPts = base.pdfPageHeightPts,
                        selectedScale = scaleEnum,
                        calibrationMultiplier = project.calibrationMultiplier,
                        calibrationScaleX = project.calibrationScaleX,
                        calibrationScaleY = project.calibrationScaleY,
                        gapSealLevel = project.gapSealLevel,
                        filterPreset = filterEnum,
                        brightness = project.brightness,
                        contrast = project.contrast,
                        khatianTitle = project.khatianTitle,
                        khatianDocName = project.khatianFileName,
                        khatianIsPdf = project.khatianIsPdf,
                        khatianPageCount = restoredKhatianPages,
                        khatianCurrentPage = 0,
                        khatianPreviewBitmap = restoredKhatianBmp,
                        khatianRecords = project.khatianRecords,
                        selectedPlots = restoredPlots,
                        manualPolygonDraft = project.manualPolygonDraft.map { it.x to it.y },
                        selectedDraftIdx = null,
                        tapeDraft = project.tapeDraft.map { it.x to it.y },
                        selectedTapeIdx = null,
                        completedTapes = project.completedTapes.map { line -> line.map { pt -> pt.x to pt.y } },
                        activeTool = if (restoredPlots.isNotEmpty()) MouzaSelectionTool.EDIT_CORNERS else MouzaSelectionTool.POLYGON_DRAW,
                        statusMessage = "রিস্টোর সম্পন্ন: ${project.title} (${restoredPlots.size}টি দাগ)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPdf = false,
                        statusMessage = "রিস্টোর ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun deleteSavedProject(projectId: String) {
        viewModelScope.launch {
            prefs.deleteSurveyProject(projectId)
            if (_uiState.value.activeProjectId == projectId) {
                _uiState.update { it.copy(activeProjectId = null) }
            }
        }
    }

    fun openKhatianDocumentUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingKhatian = true, statusMessage = "খতিয়ান ফাইল আপলোড হচ্ছে...") }
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
                    val target = File(savedDir, "khatian_${System.currentTimeMillis()}.$ext")
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
                        khatianTitle = it.khatianTitle.ifBlank { info.fileName.substringBeforeLast('.') },
                        statusMessage = "খতিয়ান আপলোড সম্পন্ন — এখন দাগ নং যোগ করে ম্যাপে খুঁজুন"
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
            id = "khat_${System.currentTimeMillis()}",
            khatianNumber = khatianNumber.trim(),
            dagNumbers = dags,
            ownerName = ownerName.trim(),
            areaSummary = areaSummary.trim(),
            pageIndex = _uiState.value.khatianCurrentPage
        )
        _uiState.update {
            it.copy(
                khatianRecords = listOf(rec) + it.khatianRecords,
                khatianTitle = it.khatianTitle.ifBlank {
                    buildString {
                        if (khatianNumber.isNotBlank()) append("খতিয়ান ${khatianNumber.trim()}")
                        if (ownerName.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(ownerName.trim())
                        }
                    }
                },
                statusMessage = "খতিয়ান নোট যুক্ত হয়েছে — দাগ নং ক্লিক করে ম্যাপে খুঁজুন"
            )
        }
    }

    fun deleteKhatianRecord(id: String) {
        _uiState.update {
            it.copy(khatianRecords = it.khatianRecords.filterNot { r -> r.id == id })
        }
    }

    fun findOrTargetDagFromKhatian(khatianNo: String, dagNo: String) {
        val state = _uiState.value
        val matched = state.selectedPlots.firstOrNull { p ->
            (dagNo.isNotBlank() && p.dagNumber.equals(dagNo.trim(), ignoreCase = true)) ||
                (dagNo.isBlank() && khatianNo.isNotBlank() && p.khatianNumber.equals(khatianNo.trim(), ignoreCase = true))
        }
        if (matched != null) {
            _uiState.update {
                it.copy(
                    focusPlotRequest = matched,
                    highlightedPlotId = matched.id,
                    activeSearchTarget = null,
                    statusMessage = "ম্যাচিং দাগ ${matched.dagNumber.ifBlank { "#${matched.plotIndex}" }} জুম করা হয়েছে!"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    activeSearchTarget = khatianNo.trim() to dagNo.trim(),
                    statusMessage = "খুঁজছেন: খতিয়ান ${khatianNo.ifBlank { "-" }} • দাগ $dagNo — নকশায় প্লট সিলেক্ট করলেই ট্যাগ হবে"
                )
            }
        }
    }

    fun clearActiveSearchTarget() {
        _uiState.update { it.copy(activeSearchTarget = null) }
    }

    fun clearHighlightedPlot() {
        _uiState.update { it.copy(highlightedPlotId = null) }
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

    // ==================== M03 + N01 PHASE 1 EXTENSIONS ====================

    fun setRenderDpi(dpi: Int) {
        if (dpi == _uiState.value.renderDpi) return
        _uiState.update { it.copy(renderDpi = dpi) }
        if (activeDocFile != null) {
            renderJob?.cancel()
            renderJob = viewModelScope.launch {
                _uiState.update { it.copy(isLoadingPdf = true, viewportTile = null) }
                loadPageInternal(_uiState.value.currentPage)
            }
        }
    }

    fun rotatePage90() {
        val next = (_uiState.value.pageRotationDeg + 90) % 360
        _uiState.update { it.copy(pageRotationDeg = next) }
        if (activeDocFile != null) {
            renderJob?.cancel()
            renderJob = viewModelScope.launch {
                _uiState.update { it.copy(isLoadingPdf = true, viewportTile = null) }
                loadPageInternal(_uiState.value.currentPage)
            }
        }
    }

    fun loadPageForComparison(pageIndex: Int?) {
        if (pageIndex == null) {
            _uiState.update { it.copy(pageCompareIndex = null, pageCompareBitmap = null) }
            return
        }
        val file = activeDocFile ?: return
        if (!activeIsPdf || pageIndex !in 0 until _uiState.value.pageCount) return
        viewModelScope.launch {
            runCatching {
                MouzaPdfEngine.renderBaseDocument(appContext, file, true, pageIndex, ultraHd = false)
            }.onSuccess { rendered ->
                _uiState.update {
                    it.copy(
                        pageCompareIndex = pageIndex,
                        pageCompareBitmap = rendered.bitmap,
                        statusMessage = "তুলনার জন্য পৃষ্ঠা ${pageIndex + 1} লোড হয়েছে"
                    )
                }
            }
        }
    }

    fun applySuggestedPdfScale() {
        val fpi = _uiState.value.pdfInspectionReport?.suggestedFeetPerInch ?: return
        val matched = MouzaScale.entries.minByOrNull { kotlin.math.abs(it.feetPerInch - fpi) } ?: MouzaScale.SCALE_16
        setScale(matched)
        _uiState.update { it.copy(statusMessage = "PDF টেক্সট থেকে প্রস্তাবিত স্কেল (${matched.labelBn}) সেট হয়েছে") }
    }

    fun importUncompressedPdfVectors() {
        val report = _uiState.value.pdfInspectionReport ?: return
        val polys = report.extractedVectorPolygons
        if (polys.isEmpty()) {
            _uiState.update { it.copy(statusMessage = report.disclosureBn) }
            return
        }
        val state = _uiState.value
        val aspect = state.pdfPageHeightPts.toFloat() / state.pdfPageWidthPts.toFloat().coerceAtLeast(1f)
        val gw = 1500
        val gh = (1500f * aspect).roundToInt().coerceAtLeast(200)
        val added = polys.take(30).mapIndexed { idx, pts ->
            val pId = nextPlotId++
            val col = PlotPalette[(pId - 1) % PlotPalette.size]
            MouzaPdfEngine.buildManualPolygonPlot(pts, gw, gh, pId, state.selectedPlots.size + idx + 1, col).copy(
                exactAreaSqFeet = MouzaPdfEngine.computePolygonAreaSqFeet(
                    pts, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale,
                    state.calibrationMultiplier, state.calibrationScaleX, state.calibrationScaleY
                )
            )
        }
        _uiState.update {
            it.copy(
                selectedPlots = it.selectedPlots + added,
                statusMessage = "PDF ভেক্টর থেকে ${added.size}টি প্লট সরাসরি যুক্ত হয়েছে!"
            )
        }
    }

    fun addCalibrationRefLineFromLastTape(actualFeet: Double, label: String = "") {
        val state = _uiState.value
        val tape = state.tapeDraft.takeIf { it.size >= 2 } ?: state.completedTapes.lastOrNull()
        if (tape == null || tape.size < 2 || actualFeet <= 0.0) {
            _uiState.update { it.copy(statusMessage = "প্রথমে 'ফিতা মাপ' দিয়ে ২ বিন্দু আঁকুন এবং সঠিক ফুট লিখুন") }
            return
        }
        val p1 = tape[tape.size - 2]
        val p2 = tape[tape.size - 1]
        val before = MouzaPdfEngine.computeDistanceFeet(
            p1, p2, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, 1.0, 1.0, 1.0
        )
        val ln = CalibrationRefLine(
            id = "cal_${System.currentTimeMillis()}",
            label = label.ifBlank { "রেখা #${state.calibrationRefLines.size + 1}" },
            p1Norm = p1,
            p2Norm = p2,
            actualFeet = actualFeet,
            measuredFeetBefore = before
        )
        recomputeCalibrationLines(state.calibrationRefLines + ln, 0.0, 0.0)
    }

    fun removeCalibrationRefLine(id: String) {
        recomputeCalibrationLines(_uiState.value.calibrationRefLines.filterNot { it.id == id }, 0.0, 0.0)
    }

    fun verifyPaperSizeAndMultiLineCalibration(expectedPaperWIn: Double, expectedPaperHIn: Double) {
        recomputeCalibrationLines(_uiState.value.calibrationRefLines, expectedPaperWIn, expectedPaperHIn)
    }

    private fun recomputeCalibrationLines(lines: List<CalibrationRefLine>, expWIn: Double, expHIn: Double) {
        val state = _uiState.value
        val out = MouzaN01Extensions.calibrateScaleMultiLine(
            lines = lines,
            pageWidthPts = state.pdfPageWidthPts,
            pageHeightPts = state.pdfPageHeightPts,
            baseScale = state.selectedScale,
            expectedPaperWidthIn = expWIn,
            expectedPaperHeightIn = expHIn
        )
        val sx = if (lines.isEmpty()) 1.0 else out.scaleXMultiplier
        val sy = if (lines.isEmpty()) 1.0 else out.scaleYMultiplier
        val mult = sx * sy
        val updatedPlots = recomputePlotsWithScale(state.selectedPlots, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale, sx, sy)
        _uiState.update {
            it.copy(
                calibrationRefLines = out.updatedLines,
                calibrationScaleX = sx,
                calibrationScaleY = sy,
                calibrationMultiplier = mult,
                calibrationErrPct = out.rmsDeviationPct.coerceAtLeast(0.4),
                paperShrinkageNote = out.paperShrinkageNoteBn,
                selectedPlots = updatedPlots,
                statusMessage = if (lines.isEmpty()) "রেফারেন্স লাইন মুছে ফেলা হয়েছে"
                else String.format(Locale.US, "বহু-রেখা X/Y ক্যালিব্রেশন সম্পন্ন (X:%.3f Y:%.3f • গড় বিচ্যুতি %.2f%%)", sx, sy, out.rmsDeviationPct)
            )
        }
    }

    fun applyCalibrationProfile(profile: ScaleCalibrationProfile) {
        val state = _uiState.value
        val matched = MouzaScale.entries.minByOrNull { kotlin.math.abs(it.feetPerInch - profile.feetPerInch) } ?: MouzaScale.SCALE_16
        val sx = profile.scaleXMultiplier
        val sy = profile.scaleYMultiplier
        val updatedPlots = recomputePlotsWithScale(state.selectedPlots, state.pdfPageWidthPts, state.pdfPageHeightPts, matched, sx, sy)
        _uiState.update {
            it.copy(
                selectedScale = matched,
                calibrationScaleX = sx,
                calibrationScaleY = sy,
                calibrationMultiplier = sx * sy,
                selectedPlots = updatedPlots,
                statusMessage = "ক্যালিব্রেশন প্রোফাইল প্রয়োগ হয়েছে: ${profile.nameBn}"
            )
        }
    }

    fun saveCurrentCalibrationProfile(name: String, surveyStage: String = "RS") {
        val state = _uiState.value
        val profile = ScaleCalibrationProfile(
            id = "cal_prof_${System.currentTimeMillis()}",
            nameBn = name.trim().ifBlank { "${state.selectedScale.ratioLabel} কাস্টম প্রোফাইল" },
            surveyStage = surveyStage,
            feetPerInch = state.selectedScale.feetPerInch,
            scaleXMultiplier = state.calibrationScaleX,
            scaleYMultiplier = state.calibrationScaleY
        )
        val updatedProfiles = state.globalSettings.savedCalibrationProfiles + profile
        val updatedSettings = state.globalSettings.copy(savedCalibrationProfiles = updatedProfiles)
        viewModelScope.launch {
            prefs.saveGlobalSettings(updatedSettings)
            _uiState.update { it.copy(statusMessage = "ক্যালিব্রেশন প্রোফাইল স্থায়ীভাবে সেভ হয়েছে") }
        }
    }

    private fun recomputePlotsWithScale(
        plots: List<MouzaPlot>,
        wPts: Int,
        hPts: Int,
        scale: MouzaScale,
        sx: Double,
        sy: Double
    ): List<MouzaPlot> {
        val pageFtW = (wPts / 72.0) * scale.feetPerInch * sx
        val pageFtH = (hPts / 72.0) * scale.feetPerInch * sy
        return plots.map { p ->
            if (p.isLocked) return@map p
            val poly = p.effectivePolygon() ?: return@map p
            val outerFt = poly.map { (it.first * pageFtW) to (it.second * pageFtH) }
            val holesFt = p.holePolygonsNormalized.map { h -> h.map { (it.first * pageFtW) to (it.second * pageFtH) } }
            p.copy(exactAreaSqFeet = GeometryAndAccuracyEngine.computeNetAreaWithHolesSqFt(outerFt, holesFt))
        }
    }

    fun generateCleanedPreview(contrastGain: Float, threshold: Int, despeckle: Boolean, invert: Boolean) {
        val src = _uiState.value.displayBitmap ?: return
        viewModelScope.launch {
            val cleaned = withContext(Dispatchers.Default) {
                MouzaN01Extensions.cleanMouzaBitmap(src, contrastGain, threshold, despeckle, invert)
            }
            _uiState.update { it.copy(cleanedPreviewBitmap = cleaned) }
        }
    }

    fun exportCleanedDocument(asPdf: Boolean) {
        val bmp = _uiState.value.cleanedPreviewBitmap ?: _uiState.value.displayBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val path = withContext(Dispatchers.IO) {
                    if (asPdf) {
                        MouzaN01Extensions.exportCleanedPdf(appContext, bmp, _uiState.value.fileName).absolutePath
                    } else {
                        val dir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "CleanedMouzaDocs").apply { mkdirs() }
                        val f = File(dir, "Cleaned_${System.currentTimeMillis()}.png")
                        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                        f.absolutePath
                    }
                }
                _uiState.update { it.copy(isExporting = false, statusMessage = "পরিষ্কার নকশা সেভ হয়েছে: $path") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, statusMessage = "সেভ ব্যর্থ: ${e.localizedMessage}") }
            }
        }
    }

    // Plot Context Menu Actions (Road, Hide Measures, Highlight, Hole Cut-Out, Lock Side Length, Split by Line)
    fun togglePlotRoad(plotId: Int) {
        _uiState.update { st ->
            val updated = st.selectedPlots.map { p -> if (p.id == plotId) p.copy(isRoad = !p.isRoad) else p }
            val target = updated.find { it.id == plotId }
            st.copy(
                selectedPlots = updated,
                statusMessage = if (target?.isRoad == true) "দাগটি 'রাস্তা' হিসেবে চিহ্নিত (মোট ব্যবহারযোগ্য জমিতে বাদ)" else "রাস্তা চিহ্ন সরানো হয়েছে"
            )
        }
    }

    fun togglePlotHideMeasures(plotId: Int) {
        _uiState.update { st ->
            val updated = st.selectedPlots.map { p -> if (p.id == plotId) p.copy(hideMeasures = !p.hideMeasures) else p }
            st.copy(selectedPlots = updated)
        }
    }

    fun togglePlotHighlight(plotId: Int) {
        _uiState.update { st ->
            val next = if (st.highlightedPlotId == plotId) null else plotId
            st.copy(highlightedPlotId = next)
        }
    }

    fun updatePlotDetails(plotId: Int, dag: String, khatian: String, owner: String, remarks: String, color: Color? = null) {
        _uiState.update { st ->
            val target = st.selectedPlots.find { it.id == plotId } ?: return@update st
            if (target.isLocked) return@update st.copy(statusMessage = "দাগটি লকড আছে — আগে আনলক করুন")
            val updated = st.selectedPlots.map { p ->
                if (p.id == plotId) p.copy(
                    dagNumber = dag,
                    khatianNumber = khatian,
                    ownerName = owner,
                    remarks = remarks,
                    color = color ?: p.color
                ) else p
            }
            st.copy(selectedPlots = updated, statusMessage = "দাগের তথ্য আপডেট হয়েছে")
        }
    }

    fun addHoleFromDraftToPlot(plotId: Int) {
        val state = _uiState.value
        val holePoly = state.manualPolygonDraft
        if (holePoly.size < 3) {
            _uiState.update { it.copy(statusMessage = "হোল/বাদ অংশ তৈরি করতে আগে পলিগন দিয়ে কমপক্ষে ৩টি বিন্দু আঁকুন") }
            return
        }
        val updated = state.selectedPlots.map { p ->
            if (p.id != plotId || p.isLocked) p
            else p.copy(holePolygonsNormalized = p.holePolygonsNormalized + listOf(holePoly))
        }
        _uiState.update {
            it.copy(
                selectedPlots = updated,
                manualPolygonDraft = emptyList(),
                selectedDraftIdx = null,
                statusMessage = "হোল/পুকুর/বাদ অংশ যুক্ত হয়েছে এবং নিট ক্ষেত্রফল হিসাব আপডেট হয়েছে"
            )
        }
    }

    fun lockPlotSideLength(plotId: Int, edgeIndex: Int, targetFeet: Double) {
        val state = _uiState.value
        val target = state.selectedPlots.find { it.id == plotId } ?: return
        if (target.isLocked) {
            _uiState.update { it.copy(statusMessage = "দাগটি লকড আছে — আগে আনলক করুন") }
            return
        }
        val poly = target.effectivePolygon() ?: return
        val pageFtW = (state.pdfPageWidthPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleX
        val pageFtH = (state.pdfPageHeightPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleY
        val adjusted = GeometryAndAccuracyEngine.adjustNormalizedPolygonEdgeLength(
            polyNorm = poly,
            edgeIndex = edgeIndex,
            targetFeet = targetFeet,
            pageWidthFt = pageFtW,
            pageHeightFt = pageFtH
        )
        val sqFt = MouzaPdfEngine.computePolygonAreaSqFeet(
            adjusted, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale,
            state.calibrationMultiplier, state.calibrationScaleX, state.calibrationScaleY
        )
        _uiState.update { st ->
            st.copy(
                selectedPlots = st.selectedPlots.map { p ->
                    if (p.id == plotId) p.copy(polygonNormalized = adjusted, manualPolygonNormalized = null, exactAreaSqFeet = sqFt) else p
                },
                statusMessage = "বাহু #${edgeIndex + 1} দৈর্ঘ্য ${String.format(Locale.US, "%.2f", targetFeet)} ফুটে সেট হয়েছে"
            )
        }
    }

    fun splitPlotByLastTapeLine(plotId: Int) {
        val state = _uiState.value
        val target = state.selectedPlots.find { it.id == plotId } ?: return
        if (target.isLocked) {
            _uiState.update { it.copy(statusMessage = "দাগটি লকড আছে — ভাগ করতে আনলক করুন") }
            return
        }
        val poly = target.effectivePolygon() ?: return
        val cutLine = state.tapeDraft.takeIf { it.size >= 2 } ?: state.completedTapes.lastOrNull()
        if (cutLine == null || cutLine.size < 2) {
            _uiState.update { it.copy(statusMessage = "বিভক্ত করতে প্রথমে 'ফিতা মাপ' দিয়ে প্লটের ওপর দিয়ে ২ বিন্দুর কাটা রেখা আঁকুন") }
            return
        }
        val split = MouzaN01Extensions.splitNormalizedPolygonByLine(poly, cutLine[cutLine.size - 2], cutLine[cutLine.size - 1])
        if (split == null) {
            _uiState.update { it.copy(statusMessage = "কাটা রেখাটি দাগ #${target.plotIndex}-কে দুই ভাগে ছেদ করেনি") }
            return
        }
        val aspect = state.pdfPageHeightPts.toFloat() / state.pdfPageWidthPts.toFloat().coerceAtLeast(1f)
        val gw = 1500
        val gh = (1500f * aspect).roundToInt().coerceAtLeast(200)
        val idA = nextPlotId++
        val idB = nextPlotId++
        val pA = MouzaPdfEngine.buildManualPolygonPlot(split.first, gw, gh, idA, target.plotIndex, target.color).copy(
            dagNumber = target.dagNumber.ifBlank { "" }.let { if (it.isNotBlank()) "$it/A" else "" },
            khatianNumber = target.khatianNumber,
            ownerName = target.ownerName
        )
        val pB = MouzaPdfEngine.buildManualPolygonPlot(split.second, gw, gh, idB, state.selectedPlots.size + 1, PlotPalette[(idB - 1) % PlotPalette.size]).copy(
            dagNumber = target.dagNumber.ifBlank { "" }.let { if (it.isNotBlank()) "$it/B" else "" },
            khatianNumber = target.khatianNumber,
            ownerName = target.ownerName
        )
        val remaining = state.selectedPlots.filterNot { it.id == plotId } + pA + pB
        _uiState.update {
            it.copy(
                selectedPlots = remaining.mapIndexed { idx, p -> p.copy(plotIndex = idx + 1) },
                tapeDraft = emptyList(),
                statusMessage = "দাগটি কাটা রেখা বরাবর ২টি অংশে ভাগ হয়েছে!"
            )
        }
    }

    fun measureCornerToCornerDistance(p1Norm: Pair<Float, Float>, p2Norm: Pair<Float, Float>) {
        val state = _uiState.value
        val dist = MouzaPdfEngine.computeDistanceFeet(
            p1Norm, p2Norm, state.pdfPageWidthPts, state.pdfPageHeightPts, state.selectedScale,
            state.calibrationMultiplier, state.calibrationScaleX, state.calibrationScaleY
        )
        val fmt = LandCalculators.formatFeetInchesSuta(dist)
        _uiState.update {
            it.copy(
                measureBetweenResult = MeasureBetweenResult(
                    modeTitleBn = "কোণা ↔ কোণা দূরত্ব",
                    distanceFt = dist,
                    distanceFormatted = fmt,
                    detailsBn = String.format(Locale.US, "%.2f ফুট (%s) • %.2f লিংক • %.2f মি.", dist, fmt, dist / 0.66, dist * 0.3048),
                    p1Norm = p1Norm,
                    p2Norm = p2Norm
                )
            )
        }
    }

    fun measureBetweenPlots(plotIdA: Int, plotIdB: Int) {
        val state = _uiState.value
        val pA = state.selectedPlots.find { it.id == plotIdA }?.effectivePolygon() ?: return
        val pB = state.selectedPlots.find { it.id == plotIdB }?.effectivePolygon() ?: return
        val pageFtW = (state.pdfPageWidthPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleX
        val pageFtH = (state.pdfPageHeightPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleY
        val res = GeometryAndAccuracyEngine.measureBetweenPolygonsFt(pA, pB, pageFtW, pageFtH)
        _uiState.update { it.copy(measureBetweenResult = res, statusMessage = res.detailsBn) }
    }

    fun measurePlotRoadFrontageFromTape(plotId: Int) {
        val state = _uiState.value
        val poly = state.selectedPlots.find { it.id == plotId }?.effectivePolygon() ?: return
        val roadLine = state.tapeDraft.takeIf { it.size >= 2 } ?: state.completedTapes.lastOrNull()
        if (roadLine == null || roadLine.size < 2) {
            _uiState.update { it.copy(statusMessage = "আগে 'ফিতা মাপ' দিয়ে রাস্তার রেখা আঁকুন") }
            return
        }
        val pageFtW = (state.pdfPageWidthPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleX
        val pageFtH = (state.pdfPageHeightPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleY
        val res = GeometryAndAccuracyEngine.computePlotRoadFrontageFt(
            polyNorm = poly,
            roadANorm = roadLine[roadLine.size - 2],
            roadBNorm = roadLine[roadLine.size - 1],
            pageWidthFt = pageFtW,
            pageHeightFt = pageFtH
        )
        _uiState.update { it.copy(measureBetweenResult = res, statusMessage = res.detailsBn) }
    }

    fun clearMeasureBetween() {
        _uiState.update { it.copy(measureBetweenResult = null) }
    }

    fun buildParcelTableRows(): List<ParcelTableRow> {
        val state = _uiState.value
        return state.selectedPlots.map { plot ->
            GeometryAndAccuracyEngine.buildParcelTableRow(
                plot = plot,
                pageWidthPts = state.pdfPageWidthPts,
                pageHeightPts = state.pdfPageHeightPts,
                scale = state.selectedScale,
                calibrationX = state.calibrationScaleX,
                calibrationY = state.calibrationScaleY,
                calibrationErrPct = state.calibrationErrPct,
                settings = state.globalSettings
            )
        }
    }

    fun getShoelaceStepsForPlot(plotId: Int): Pair<Double, List<ShoelaceStepRow>> {
        val state = _uiState.value
        val poly = state.selectedPlots.find { it.id == plotId }?.effectivePolygon() ?: return 0.0 to emptyList()
        val pageFtW = (state.pdfPageWidthPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleX
        val pageFtH = (state.pdfPageHeightPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleY
        val vertsFt = poly.map { (it.first * pageFtW) to (it.second * pageFtH) }
        return GeometryAndAccuracyEngine.computeShoelaceWithSteps(vertsFt)
    }

    fun exportParcelTableCsvFile() {
        val rows = buildParcelTableRows()
        if (rows.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "রপ্তানির আগে কমপক্ষে ১টি দাগ সিলেক্ট করুন") }
            return
        }
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val csv = GeometryAndAccuracyEngine.exportParcelTableCsv(rows, _uiState.value.fileName.ifBlank { "Mouza Parcel Table" })
                val dir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "ParcelTables").apply { mkdirs() }
                val f = File(dir, "Parcel_Table_${System.currentTimeMillis()}.csv")
                f.writeText(csv)
                f.absolutePath
            }
            _uiState.update { it.copy(statusMessage = "পার্সেল টেবিল CSV সেভ হয়েছে: $path") }
        }
    }

    fun exportNonDestructiveAnnotatedPdfFile() {
        val state = _uiState.value
        val src = state.displayBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "অ্যানোটেটেড ভেক্টর PDF তৈরি হচ্ছে...") }
            try {
                val profile = prefs.surveyorProfileFlow.first()
                val rows = buildParcelTableRows()
                val file = withContext(Dispatchers.IO) {
                    MouzaN01Extensions.exportNonDestructiveAnnotatedPdf(
                        context = appContext,
                        sourceBitmap = src,
                        plots = state.selectedPlots,
                        parcelRows = rows,
                        title = state.khatianTitle.ifBlank { state.fileName.ifBlank { "মৌজা শিট" } },
                        scale = state.selectedScale,
                        pageWidthPts = state.pdfPageWidthPts,
                        pageHeightPts = state.pdfPageHeightPts,
                        scaleXMult = state.calibrationScaleX,
                        scaleYMult = state.calibrationScaleY,
                        surveyorProfile = profile,
                        settings = state.globalSettings
                    )
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastSavedPath = file.absolutePath,
                        statusMessage = "অ্যানোটেটেড PDF সেভ হয়েছে (মূল PDF অক্ষত): ${file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, statusMessage = "রপ্তানি ব্যর্থ: ${e.localizedMessage}") }
            }
        }
    }

    fun exportAccuracyReportPdfForPlots() {
        val state = _uiState.value
        val rows = buildParcelTableRows()
        if (rows.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "আগে কমপক্ষে ১টি দাগ সিলেক্ট করুন") }
            return
        }
        viewModelScope.launch {
            val usableArea = rows.filterNot { it.isRoad }.sumOf { it.sqFeet }
            val usablePerim = rows.filterNot { it.isRoad }.sumOf { it.perimeterFt }
            val pageFtW = (state.pdfPageWidthPts / 72.0) * state.selectedScale.feetPerInch * state.calibrationScaleX
            val uncertainty = GeometryAndAccuracyEngine.computeMeasurementUncertainty(
                areaSqFeet = usableArea,
                perimeterFeet = usablePerim,
                edgeLengthsFt = emptyList(),
                calibrationErrPct = state.calibrationErrPct,
                ftPerPixel = pageFtW / 1500.0,
                zoomScale = 2.0f,
                settings = state.globalSettings
            )
            val file = withContext(Dispatchers.IO) {
                GeometryAndAccuracyEngine.exportAccuracyReportPdf(
                    context = appContext,
                    title = state.khatianTitle.ifBlank { state.fileName.ifBlank { "মৌজা সার্ভে" } },
                    uncertainty = uncertainty,
                    parcelRows = rows
                )
            }
            _uiState.update { it.copy(statusMessage = "নির্ভুলতা (±) PDF রিপোর্ট সেভ হয়েছে: ${file.name}") }
        }
    }
}
