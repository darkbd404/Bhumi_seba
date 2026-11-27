package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.test.core.app.ApplicationProvider
import com.example.engine.MouzaPdfEngine
import com.example.engine.PlotWallIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ভূমি সেবা ডিজিটাল", appName)
    }

    @Test
    fun mouzaPlotDetector_selectsWholePlotIncludingDagNumberAndSeals1pxGap() = runTest {
        // Synthetic 600x400 Mouza map with two adjacent plots and a dark Dag Number "142" inside Plot 1
        val bmp = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val linePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        // Outer network spanning large enough to stay in wallMask
        canvas.drawRect(50f, 50f, 550f, 350f, linePaint)
        canvas.drawLine(300f, 50f, 300f, 350f, linePaint)

        // Draw a small 1px gap in the dividing line at y=200 to test gap sealing
        val gapPaint = Paint().apply { color = Color.WHITE; strokeWidth = 1f }
        canvas.drawPoint(300f, 200f, gapPaint)

        // Draw a dark Dag Number inside Plot 1 (center ~175, 200)
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        canvas.drawText("142", 155f, 208f, textPaint)

        val wallIndex = MouzaPdfEngine.buildPlotWallIndex(bmp, gapSealLevel = 1)
        val result = wallIndex.selectPlotAt(
            normX = 0.20f, // Inside Plot 1 (x=120)
            normY = 0.50f, // y=200
            plotId = 1,
            plotIndex = 1,
            color = ComposeColor.Green,
            existingPlots = emptyList()
        )

        assertTrue("Expected TapResult.Success but got $result", result is PlotWallIndex.TapResult.Success)
        val plot = (result as PlotWallIndex.TapResult.Success).plot
        // Plot 1 spans roughly 250x300 = ~75,000 px (not leaked into Plot 2 which would be ~150,000 px)
        assertTrue("Plot pixel count (${plot.pixelCount}) should match single plot", plot.pixelCount in 65_000..80_000)
        // Verify the center dag number coordinates (175, 200) are included in selection!
        val centerIdx = 200 * wallIndex.gridWidth + 175
        assertTrue("Dag number interior pixel must be included in plot selection", plot.maskIndices.binarySearch(centerIdx) >= 0)
    }

    @Test
    fun mouzaPdfViewModel_pointerPlacesMultipleClosePointsAndDeletesSelected() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.MouzaPdfViewModel(app)

        // Inject a synthetic rendered bitmap via reflection-free field check or test onPointerPlace after setting bitmap state
        val stateField = com.example.viewmodel.MouzaPdfViewModel::class.java.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.MouzaPdfUiState>
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        flow.value = flow.value.copy(displayBitmap = bmp, pdfPageWidthPts = 1000, pdfPageHeightPts = 1000)

        // Simulate placing 4 corners of a zoomed-in small plot (< 0.01 normalized apart) using Pointer button
        vm.onPointerPlace(0.450f, 0.450f)
        vm.onPointerPlace(0.456f, 0.450f)
        vm.onPointerPlace(0.456f, 0.458f)
        vm.onPointerPlace(0.450f, 0.458f)

        assertEquals(4, vm.uiState.value.manualPolygonDraft.size)

        // Delete 2nd point and verify 3 points remain
        vm.selectDraftPoint(1)
        vm.deleteSelectedOrLastDraftPoint()
        assertEquals(3, vm.uiState.value.manualPolygonDraft.size)
    }

    @Test
    fun mouzaPdfViewModel_nudgeAndCyclePointMovesPointSlightly() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.MouzaPdfViewModel(app)
        val stateField = com.example.viewmodel.MouzaPdfViewModel::class.java.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.MouzaPdfUiState>
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        flow.value = flow.value.copy(displayBitmap = bmp, pdfPageWidthPts = 1000, pdfPageHeightPts = 1000)

        vm.onPointerPlace(0.20f, 0.20f)
        vm.onPointerPlace(0.40f, 0.20f)
        vm.onPointerPlace(0.30f, 0.40f)

        // Cycle to point 0 and nudge slightly right (+0.005) and down (+0.003)
        vm.cycleActivePoint(1) // wraps from lastIndex (2) -> 0
        assertEquals(0, vm.uiState.value.selectedDraftIdx)
        vm.nudgeActivePoint(0.005f, 0.003f)
        val nudged = vm.uiState.value.manualPolygonDraft[0]
        assertEquals(0.205f, nudged.first, 0.0001f)
        assertEquals(0.203f, nudged.second, 0.0001f)
    }

    @Test
    fun appPreferences_saveAndRestoreSurveyProject() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.AppPreferences(context)
        val proj = com.example.model.SavedSurveyProject(
            id = "test_proj_1",
            title = "দাগ ৪২ আমন জমি",
            type = com.example.model.SavedProjectType.REAL_MAP,
            timestampMillis = 123456789L,
            decimalArea = 15.5,
            kathaArea = 9.39,
            sqFeetArea = 6751.8,
            itemCount = 4,
            mapCenterLat = 23.81,
            mapCenterLon = 90.41,
            mapZoom = 18.5,
            mapPoints = listOf(
                com.example.model.SerializableLatLon(23.810, 90.410),
                com.example.model.SerializableLatLon(23.811, 90.410),
                com.example.model.SerializableLatLon(23.811, 90.411)
            )
        )
        prefs.saveSurveyProject(proj)
        val restored = prefs.getSurveyProjectById("test_proj_1")
        assertEquals("দাগ ৪২ আমন জমি", restored?.title)
        assertEquals(3, restored?.mapPoints?.size)
        prefs.deleteSurveyProject("test_proj_1")
        assertEquals(null, prefs.getSurveyProjectById("test_proj_1"))
    }

    @Test
    fun mouzaPdfViewModel_khatianDagLookupAndAutoTaggingWorks() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.MouzaPdfViewModel(app)
        val stateField = com.example.viewmodel.MouzaPdfViewModel::class.java.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.MouzaPdfUiState>
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        flow.value = flow.value.copy(displayBitmap = bmp, pdfPageWidthPts = 1000, pdfPageHeightPts = 1000)

        vm.addKhatianRecord(
            khatianNumber = "১২৮",
            dagNumbersCsv = "৪৫২, ৪৫৩",
            ownerName = "আবুল কালাম",
            areaSummary = "২৭.৯ শতাংশ"
        )
        assertEquals(1, vm.uiState.value.khatianRecords.size)
        assertEquals(listOf("৪৫২", "৪৫৩"), vm.uiState.value.khatianRecords.first().dagNumbers)

        // Targeting an untagged Dag sets activeSearchTarget
        vm.findOrTargetDagFromKhatian("১২৮", "৪৫২")
        assertEquals("১২৮" to "৪৫২", vm.uiState.value.activeSearchTarget)

        // Drawing manual polygon auto-tags it with Khatian & Dag
        vm.onPointerPlace(0.10f, 0.10f)
        vm.onPointerPlace(0.30f, 0.10f)
        vm.onPointerPlace(0.30f, 0.30f)
        vm.finishManualPolygon()

        assertEquals(1, vm.uiState.value.selectedPlots.size)
        val tagged = vm.uiState.value.selectedPlots.first()
        assertEquals("১২৮", tagged.khatianNumber)
        assertEquals("৪৫২", tagged.dagNumber)
        assertEquals(null, vm.uiState.value.activeSearchTarget)

        // Targeting already tagged Dag focuses & highlights the plot
        vm.findOrTargetDagFromKhatian("১২৮", "৪৫২")
        assertEquals(tagged.id, vm.uiState.value.highlightedPlotId)
        assertEquals(tagged.id, vm.uiState.value.focusPlotRequest?.id)
    }

    @Test
    fun mouzaPdfViewModel_microStepNudgeChangesPointGraduallyWithoutJumping() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.MouzaPdfViewModel(app)
        val stateField = com.example.viewmodel.MouzaPdfViewModel::class.java.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.MouzaPdfUiState>
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        flow.value = flow.value.copy(displayBitmap = bmp, pdfPageWidthPts = 1000, pdfPageHeightPts = 1000)

        vm.onPointerPlace(0.10f, 0.10f)
        vm.onPointerPlace(0.25f, 0.10f)
        vm.onPointerPlace(0.25f, 0.25f)
        vm.finishManualPolygon()

        val initialSqFt = vm.totalSelectedSqFeet()
        val microDeltaX = 0.22f / 900f // 1% step level (0.22px on 900px fit width)
        vm.cycleActivePoint(1)
        vm.nudgeActivePoint(microDeltaX, 0f)
        val nextSqFt = vm.totalSelectedSqFeet()
        val deltaDecimal = kotlin.math.abs(nextSqFt - initialSqFt) / 435.6

        // Must change area smoothly by much less than 0.25 decimal per click
        assertTrue("Expected gradual decimal change < 0.25, was $deltaDecimal", deltaDecimal < 0.25)
    }

    @Test
    fun mouzaPdfViewModel_lockedPlotPreventsEditingResizingAndDeletionUntilUnlocked() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.MouzaPdfViewModel(app)
        val stateField = com.example.viewmodel.MouzaPdfViewModel::class.java.getDeclaredField("_uiState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.MouzaPdfUiState>
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        flow.value = flow.value.copy(displayBitmap = bmp, pdfPageWidthPts = 1000, pdfPageHeightPts = 1000)

        // Create Plot 1 (will be locked)
        vm.onPointerPlace(0.10f, 0.10f)
        vm.onPointerPlace(0.30f, 0.10f)
        vm.onPointerPlace(0.30f, 0.30f)
        vm.finishManualPolygon()

        // Create Plot 2 (will remain unlocked)
        vm.onPointerPlace(0.50f, 0.50f)
        vm.onPointerPlace(0.70f, 0.50f)
        vm.onPointerPlace(0.70f, 0.70f)
        vm.finishManualPolygon()

        assertEquals(2, vm.uiState.value.selectedPlots.size)
        val plot1Initial = vm.uiState.value.selectedPlots[0]
        val plot2Initial = vm.uiState.value.selectedPlots[1]

        // Lock Plot 1
        vm.togglePlotLock(plot1Initial.id)
        assertTrue(vm.uiState.value.selectedPlots[0].isLocked)
        assertTrue(!vm.uiState.value.selectedPlots[1].isLocked)

        // Attempt vertex movement, nudge, edge split, delete vertex, calibration, scale change, and removal on locked Plot 1
        vm.updatePlotVertex(plot1Initial.id, 0, 0.05f, 0.05f)
        vm.selectVertex(plot1Initial.id, 0)
        vm.deleteSelectedOrNearestVertex(0.10f, 0.10f)
        vm.calibrateWithKnownPlot(plot1Initial.id, 99.0)
        vm.setScale(com.example.model.MouzaScale.SCALE_32)
        vm.removePlot(plot1Initial.id)

        val lockedAfterAttempts = vm.uiState.value.selectedPlots.first { it.id == plot1Initial.id }
        assertEquals(plot1Initial.effectivePolygon(), lockedAfterAttempts.effectivePolygon())
        assertEquals(plot1Initial.exactAreaSqFeet ?: 0.0, lockedAfterAttempts.exactAreaSqFeet ?: 0.0, 0.001)

        // Plot 2 (unlocked) should have been recalculated when scale changed, while Plot 1 remained protected
        val plot2AfterScale = vm.uiState.value.selectedPlots.first { it.id == plot2Initial.id }
        assertTrue((plot2AfterScale.exactAreaSqFeet ?: 0.0) < (plot2Initial.exactAreaSqFeet ?: 0.0))

        // Clear all should delete unlocked Plot 2 but preserve locked Plot 1
        vm.clearAllPlots()
        assertEquals(1, vm.uiState.value.selectedPlots.size)
        assertEquals(plot1Initial.id, vm.uiState.value.selectedPlots.first().id)

        // Unlock Plot 1 and verify editing and removal now succeed
        vm.togglePlotLock(plot1Initial.id)
        assertTrue(!vm.uiState.value.selectedPlots.first().isLocked)
        vm.updatePlotVertex(plot1Initial.id, 0, 0.05f, 0.05f)
        assertEquals(0.05f to 0.05f, vm.uiState.value.selectedPlots.first().effectivePolygon()?.get(0))
        vm.removePlot(plot1Initial.id)
        assertEquals(0, vm.uiState.value.selectedPlots.size)
    }

    @Test
    fun realMapViewModel_lockToggleProtectsFinalizedMeasurementAndAutoResetsWhenEmpty() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.RealMapViewModel(app)

        // Cannot lock before placing pins
        vm.togglePlotLock()
        assertTrue(!vm.uiState.value.isPlotLocked)

        // Add points then lock
        vm.updateMeasurementFromBridge(points = 4, areaSqM = 404.68, perimeterM = 80.0, selectedPinIdx = 2)
        vm.togglePlotLock()
        assertTrue(vm.uiState.value.isPlotLocked)
        assertEquals(-1, vm.uiState.value.selectedPinIdx)

        vm.notifyLockedEditBlocked()
        assertTrue(vm.uiState.value.statusMessage?.contains("লক") == true)

        // Unlock toggles off
        vm.togglePlotLock()
        assertTrue(!vm.uiState.value.isPlotLocked)

        // Locking again and clearing points auto-unlocks
        vm.togglePlotLock()
        assertTrue(vm.uiState.value.isPlotLocked)
        vm.updateMeasurementFromBridge(points = 0, areaSqM = 0.0, perimeterM = 0.0, selectedPinIdx = -1)
        assertTrue(!vm.uiState.value.isPlotLocked)
    }

    @Test
    fun appPreferences_surveyorProfileDefaultsAndSaveLoad() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.AppPreferences(context)
        val initial = prefs.surveyorProfileFlow.first()
        assertEquals("Abdus Salam", initial.name)
        assertEquals("01620230864", initial.phone)
        assertEquals("salam230864@gmail.com", initial.email)

        val updated = initial.copy(
            name = "মোঃ আব্দুস সালাম",
            phone = "01711000000",
            email = "surveyor@example.com",
            designation = "সার্ভেয়ার"
        )
        prefs.saveSurveyorProfile(updated)
        val loaded = prefs.surveyorProfileFlow.first()
        assertEquals("মোঃ আব্দুস সালাম", loaded.name)
        assertEquals("01711000000", loaded.phone)
        assertEquals("surveyor@example.com", loaded.email)
        assertEquals("সার্ভেয়ার", loaded.designation)
    }

    @Test
    fun mouzaPdfViewModel_shouldReuseViewportTileRefusesDeepZoomOrLowResTile() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.MouzaPdfViewModel(app)
        val hiResTile = MouzaPdfEngine.ViewportTileResult(
            bitmap = Bitmap.createBitmap(1800, 1800, Bitmap.Config.ARGB_8888),
            normLeft = 0.10f,
            normTop = 0.10f,
            normRight = 0.50f,
            normBottom = 0.50f,
            zoomScale = 4.0f
        )

        // Matching zoom & sufficient resolution within cached bounds reuses tile
        assertTrue(
            vm.shouldReuseViewportTile(
                existing = hiResTile,
                zoomScale = 4.2f,
                normLeft = 0.15f,
                normTop = 0.15f,
                normRight = 0.45f,
                normBottom = 0.45f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1600
            )
        )

        // Zooming deeper (>1.22x) must NOT reuse stale tile so PDF re-renders sharply
        assertTrue(
            !vm.shouldReuseViewportTile(
                existing = hiResTile,
                zoomScale = 6.0f,
                normLeft = 0.22f,
                normTop = 0.22f,
                normRight = 0.38f,
                normBottom = 0.38f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1600
            )
        )

        // A low-resolution tile must NOT be reused for a large viewport
        val lowResTile = hiResTile.copy(
            bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        )
        assertTrue(
            !vm.shouldReuseViewportTile(
                existing = lowResTile,
                zoomScale = 4.0f,
                normLeft = 0.15f,
                normTop = 0.15f,
                normRight = 0.45f,
                normBottom = 0.45f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1600
            )
        )
    }

    @Test
    fun pngExporter_contextCropBoundsIncludeSurroundingLandAndDesktopHtmlIsValid() {
        val smallPlot = com.example.model.MouzaPlot(
            id = 1,
            plotIndex = 1,
            color = ComposeColor.Green,
            pixelCount = 100,
            gridWidth = 1000,
            gridHeight = 1000,
            minX = 480,
            minY = 480,
            maxX = 520,
            maxY = 520,
            centroidNormalized = 0.50f to 0.50f,
            maskIndices = IntArray(0),
            polygonNormalized = listOf(
                0.48f to 0.48f,
                0.52f to 0.48f,
                0.52f to 0.52f,
                0.48f to 0.52f
            )
        )
        val bounds = com.example.engine.PngExporter.computeMouzaContextCropBounds(
            plots = listOf(smallPlot),
            selectedOnly = true
        )
        // Ensures at least 0.06 normalized surrounding context on every side around the plot
        assertTrue("Left bound ${bounds[0]} should pad <= 0.42", bounds[0] <= 0.42f)
        assertTrue("Top bound ${bounds[1]} should pad <= 0.42", bounds[1] <= 0.42f)
        assertTrue("Right bound ${bounds[2]} should pad >= 0.58", bounds[2] >= 0.58f)
        assertTrue("Bottom bound ${bounds[3]} should pad >= 0.58", bounds[3] >= 0.58f)

        val html = com.example.engine.DesktopAppExporter.buildStandaloneDesktopHtml()
        assertTrue(html.contains("ভূমি সেবা ডিজিটাল"))
        assertTrue(html.contains("leaflet"))
    }

    @Test
    fun realMapViewModel_mouzaScaleGridAndHdSharpModesWork() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.RealMapViewModel(app)

        assertEquals(com.example.model.MouzaScale.SCALE_16, vm.uiState.value.selectedMouzaScale)
        assertTrue(vm.uiState.value.hdSharpMode)
        assertTrue(!vm.uiState.value.showMouzaGrid)

        vm.selectMouzaScale(com.example.model.MouzaScale.SCALE_32)
        assertEquals(com.example.model.MouzaScale.SCALE_32, vm.uiState.value.selectedMouzaScale)

        vm.toggleMouzaGrid()
        assertTrue(vm.uiState.value.showMouzaGrid)

        vm.toggleHdSharpMode()
        assertTrue(!vm.uiState.value.hdSharpMode)

        vm.setMouzaOverlayOpacity(0.75f)
        vm.setMouzaOverlayRotation(375f)
        vm.setMouzaOverlayWidthMeters(600.0)
        vm.toggleMouzaOverlayLock()

        assertEquals(0.75f, vm.uiState.value.mouzaOverlayOpacity, 0.001f)
        assertEquals(15f, vm.uiState.value.mouzaOverlayRotationDeg, 0.001f)
        assertEquals(600.0, vm.uiState.value.mouzaOverlayWidthMeters, 0.001)
        assertTrue(vm.uiState.value.mouzaOverlayLocked)
    }

    @Test
    fun customKhatianViewModel_ownersDagsSaveLoadAndSheetRender() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.CustomKhatianViewModel(app)

        vm.updateHeaderFields(
            khatianType = "বিএস (BS) খতিয়ান",
            division = "ঢাকা",
            district = "গাজীপুর",
            upazila = "সদর",
            mouzaName = "পুবাইল",
            jlNumber = "৮৫",
            khatianNumber = "৪১২",
            totalKhajna = "১২০ টাকা"
        )
        vm.addOrUpdateOwner(
            editId = null,
            name = "রফিকুল ইসলাম",
            fatherOrHusband = "মৃত করিম উদ্দিন",
            address = "পুবাইল",
            shareText = "৮ আনা",
            shareRatio = 0.5
        )
        vm.addOrUpdateDag(
            editId = null,
            dagNumber = "১০৪২",
            landClass = "নাল",
            totalDecimal = 40.0,
            thisKhatianShare = 0.5,
            explicitThisKhatianDecimal = null,
            remarks = "হাল"
        )
        assertEquals(20.0, vm.totalKhatianDecimal(), 0.001)

        val prefs = com.example.data.AppPreferences(app)
        val doc = vm.uiState.value.document
        prefs.saveCustomKhatian(doc)
        val saved = prefs.getCustomKhatianById(doc.id)
        assertEquals("৪১২", saved?.khatianNumber)
        assertEquals(1, saved?.owners?.size)
        assertEquals(1, saved?.dags?.size)

        vm.createNewBlankKhatian()
        assertEquals("", vm.uiState.value.document.khatianNumber)
        if (saved != null) {
            vm.loadCustomKhatian(saved)
        }

        val bmp = com.example.engine.KhatianSheetExporter.buildKhatianSheetBitmap(
            context = app,
            doc = doc,
            surveyorProfile = com.example.model.SurveyorProfile()
        )
        assertTrue("Expected wide ledger bitmap, got ${bmp.width}x${bmp.height}", bmp.width >= 1800 && bmp.height >= 900)
        bmp.recycle()
    }

    @Test
    fun desktopExporter_andLocalPcServer_serveFullPcVersionWithout404() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customProfile = com.example.model.SurveyorProfile(
            name = "Abdus Salam",
            phone = "01620230864",
            email = "salam230864@gmail.com"
        )
        val html = com.example.engine.DesktopAppExporter.buildStandaloneDesktopHtml(context, customProfile)
        assertTrue(html.contains("ভূমি সেবা ডিজিটাল"))
        assertTrue(html.contains("salam230864@gmail.com"))
        assertTrue(html.contains("khSummary"))
        assertTrue(html.contains("scr_home"))
        assertTrue(html.contains("scr_mouza"))
        assertTrue(html.contains("scr_sat"))
        assertTrue(html.contains("scr_khatian"))
        assertTrue(html.contains("scr_services"))
        assertTrue(html.contains("scr_tools"))
        assertTrue(html.contains("faraiz"))
        assertTrue(html.contains("hissa"))
        assertTrue(!html.contains("ais-pre-unuuvzicd3jk2gwzkjmqiu"))

        try {
            com.example.engine.LocalPcWebServer.start(context, customProfile)
            var port = 0
            for (i in 0..40) {
                val st = com.example.engine.LocalPcWebServer.state.value
                if (st.isRunning && st.port > 0) {
                    port = st.port
                    break
                }
                Thread.sleep(50)
            }
            assertTrue("LocalPcWebServer should start on a valid port", port > 0)

            val conn = java.net.URL("http://127.0.0.1:$port/").openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            assertEquals(200, conn.responseCode)
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(body.contains("কাস্টম খতিয়ান"))
            assertTrue(body.contains("অটো ফিল"))
            conn.disconnect()

            val mfConn = java.net.URL("http://127.0.0.1:$port/manifest.webmanifest").openConnection() as java.net.HttpURLConnection
            assertEquals(200, mfConn.responseCode)
            val mfBody = mfConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(mfBody.contains("standalone"))
            mfConn.disconnect()

            listOf("/icon.png", "/avatar.jpg", "/banner/0").forEach { assetPath ->
                val aConn = java.net.URL("http://127.0.0.1:$port$assetPath").openConnection() as java.net.HttpURLConnection
                assertEquals("Expected 200 for $assetPath", 200, aConn.responseCode)
                val bytes = aConn.inputStream.readBytes()
                assertTrue("Expected non-empty bytes for $assetPath", bytes.isNotEmpty())
                aConn.disconnect()
            }

            // Verify /api/status and /api/sync endpoints
            com.example.engine.LocalPcWebServer.updateSyncPayload("""{"version":1,"khatians":[{"id":"kh_test","khatianNumber":"৭৭৭"}]}""")
            val statusConn = java.net.URL("http://127.0.0.1:$port/api/status").openConnection() as java.net.HttpURLConnection
            assertEquals(200, statusConn.responseCode)
            val statusBody = statusConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(statusBody.contains("LOCAL_WIFI"))
            statusConn.disconnect()

            val syncConn = java.net.URL("http://127.0.0.1:$port/api/sync").openConnection() as java.net.HttpURLConnection
            assertEquals(200, syncConn.responseCode)
            val syncBody = syncConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(syncBody.contains("৭৭৭"))
            syncConn.disconnect()
        } finally {
            com.example.engine.LocalPcWebServer.stop()
        }
    }

    @Test
    fun exportStreamWriter_savesValidPngAndPdfFilesToDesktopFallbackDir() {
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "bhumi_export_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val bmpPng = android.graphics.Bitmap.createBitmap(120, 80, android.graphics.Bitmap.Config.ARGB_8888)
            val pngRes = com.example.engine.ExportStreamWriter.savePngBitmap(
                context = null,
                bitmap = bmpPng,
                fileName = "test_khatian_sheet",
                desktopFallbackDir = tempDir
            )
            val pngFile = java.io.File(pngRes.displayPath)
            assertTrue("PNG file should exist", pngFile.exists())
            val pngBytes = pngFile.readBytes()
            assertTrue("PNG file must not be empty", pngBytes.size > 16)
            assertEquals(0x89.toByte(), pngBytes[0])
            assertEquals('P'.code.toByte(), pngBytes[1])
            assertEquals('N'.code.toByte(), pngBytes[2])
            assertEquals('G'.code.toByte(), pngBytes[3])

            val bmpPdf = android.graphics.Bitmap.createBitmap(120, 80, android.graphics.Bitmap.Config.ARGB_8888)
            val pdfRes = com.example.engine.ExportStreamWriter.savePdfBitmap(
                context = null,
                bitmap = bmpPdf,
                fileName = "test_khatian_sheet",
                desktopFallbackDir = tempDir
            )
            val pdfFile = java.io.File(pdfRes.displayPath)
            assertTrue("PDF file should exist", pdfFile.exists())
            val pdfHeader = String(pdfFile.readBytes().copyOfRange(0, 5), Charsets.US_ASCII)
            assertEquals("%PDF-", pdfHeader)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun pcNetworkSyncManager_andPcHtmlKhatianFix_workAcrossNetworks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            "https://ntfy.sh",
            com.example.engine.PcNetworkSyncManager.normalizeEndpointUrl("")
        )
        assertEquals(
            "https://www.salam.whf.bz",
            com.example.engine.PcNetworkSyncManager.normalizeEndpointUrl("www.salam.whf.bz/")
        )

        // Verify ANSI-stripped extraction of public HTTPS URLs from tunnel stdout
        val lhrAnsiLine = "\u001B[32mabc123def.lhr.life\u001B[0m tunneled with tls termination, \u001B[36mhttps://abc123def.lhr.life\u001B[0m"
        assertEquals(
            "https://abc123def.lhr.life",
            com.example.engine.PublicPcTunnelManager.extractPublicTunnelUrl(lhrAnsiLine)
        )
        val pinggyLine = "Forwarding https://xyz987.a.free.pinggy.link -> http://127.0.0.1:8765"
        assertEquals(
            "https://xyz987.a.free.pinggy.link",
            com.example.engine.PublicPcTunnelManager.extractPublicTunnelUrl(pinggyLine)
        )

        val roomCode = com.example.engine.PcNetworkSyncManager.ensureRelayRoomCode(context, "Bhumi_Test 99!")
        assertEquals("bhumi_test99", roomCode)
        assertEquals(
            "https://ntfy.sh/bhumi_test99",
            com.example.engine.PcNetworkSyncManager.buildCloudRelayTopicUrl("https://ntfy.sh/", roomCode)
        )

        val sampleKhatian = com.example.model.CustomKhatianDocument(
            id = "kh_remote_1",
            khatianNumber = "৯১২",
            mouzaName = "কাকরাইল",
            dags = listOf(com.example.model.KhatianDagEntry(id = "d1", dagNumber = "১০১", totalDecimal = 35.0))
        )
        val syncUrl = com.example.engine.PcNetworkSyncManager.buildCrossNetworkSyncUrl(
            baseUrl = "https://abc123def.lhr.life",
            profile = com.example.model.SurveyorProfile(),
            khatians = listOf(sampleKhatian),
            relayRoomCode = roomCode
        )
        assertTrue(syncUrl.startsWith("https://abc123def.lhr.life/#sync="))
        assertTrue(syncUrl.endsWith("&relay=bhumi_test99"))
        val encodedPart = syncUrl.substringAfter("#sync=").substringBefore("&relay=")
        val decodedJson = String(
            android.util.Base64.decode(encodedPart, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING),
            Charsets.UTF_8
        )
        assertTrue(decodedJson.contains("৯১২"))
        assertTrue(decodedJson.contains("Abdus Salam"))

        // Without active tunnel, REMOTE_ACCESS falls back cleanly to real public Cloud Relay topic URL
        val relayState = com.example.engine.PcNetworkSyncManager.refreshState(
            context = context,
            settings = com.example.model.PcSyncSettings(
                mode = com.example.model.PcSyncMode.REMOTE_ACCESS,
                relayRoomCode = roomCode
            ),
            serverState = com.example.engine.PcServerState(isRunning = false),
            tunnelState = com.example.engine.PublicTunnelState(status = com.example.engine.TunnelStatus.STOPPED)
        )
        assertEquals(com.example.model.PcSyncMode.REMOTE_ACCESS, relayState.activeMode)
        assertEquals("https://ntfy.sh/bhumi_test99", relayState.activeTargetUrl)

        // With active public tunnel, REMOTE_ACCESS targets the live tunnel HTTPS URL + relay code
        val tunneledState = com.example.engine.PcNetworkSyncManager.refreshState(
            context = context,
            settings = com.example.model.PcSyncSettings(
                mode = com.example.model.PcSyncMode.REMOTE_ACCESS,
                relayRoomCode = roomCode
            ),
            serverState = com.example.engine.PcServerState(isRunning = true, port = 8765),
            tunnelState = com.example.engine.PublicTunnelState(
                status = com.example.engine.TunnelStatus.CONNECTED,
                publicUrl = "https://abc123def.lhr.life",
                providerName = "localhost.run"
            )
        )
        assertEquals("https://abc123def.lhr.life/#relay=bhumi_test99", tunneledState.activeTargetUrl)

        val html = com.example.engine.DesktopAppExporter.buildStandaloneDesktopHtml(context, com.example.model.SurveyorProfile())
        assertTrue("pc_app.html must declare fn in exportKhatianSheet", html.contains("const fn = `Khatian_"))
        assertTrue("pc_app.html must declare thumb canvas in exportKhatianSheet", html.contains("const thumb = document.createElement(\"canvas\")"))
        assertTrue("pc_app.html must include downloadCanvasAsPng", html.contains("function downloadCanvasAsPng"))
        assertTrue("pc_app.html must include cross-network sync parser", html.contains("function applyCrossNetworkSyncFromUrl"))
        assertTrue("pc_app.html must include live Cloud Relay sync", html.contains("function startCloudRelaySync"))
        assertTrue("pc_app.html must include Cloud Relay modal", html.contains("function openRelayModal"))
        assertTrue("pc_app.html must not contain 5462/5463", !html.contains("৫৪৬২") && !html.contains("৫৪৬৩"))
    }

    @Test
    fun surveyMathProEngine_regionalUnitsTraverseCosineFormulaAndInheritanceWork() {
        // 1. Regional units: Chattogram (40-dec Kani) vs Noakhali (120-dec Shahi Kani)
        val ctgProfile = com.example.engine.SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first { it.id == "chattogram_40" }
        val noaProfile = com.example.engine.SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first { it.id == "noakhali_shahi_120" }
        val sqFt120Dec = 120.0 * com.example.engine.LandCalculators.SQ_FT_PER_DECIMAL
        val ctgRes = com.example.engine.SurveyMathProEngine.convertSqFeetWithProfile(sqFt120Dec, ctgProfile)
        val noaRes = com.example.engine.SurveyMathProEngine.convertSqFeetWithProfile(sqFt120Dec, noaProfile)
        assertEquals(3.0, ctgRes.kani, 0.0001)
        assertEquals(1.0, noaRes.kani, 0.0001)

        // 2. Unknown side (3-4-5 right triangle: sideA=30, sideB=40, angle=90 -> sideC=50, area=600)
        val tri = com.example.engine.SurveyMathProEngine.solveTriangleSideAndArea(30.0, 40.0, 90.0)!!
        assertEquals(50.0, tri.first, 0.001)
        assertEquals(600.0, tri.second, 0.001)

        // 3. Coordinate Geometry (Shoelace) on 100x80 rectangle
        val cg = com.example.engine.SurveyMathProEngine.computeCoordinateGeometry(
            listOf(
                com.example.model.PolygonVertex(0.0, 0.0),
                com.example.model.PolygonVertex(100.0, 0.0),
                com.example.model.PolygonVertex(100.0, 80.0),
                com.example.model.PolygonVertex(0.0, 80.0)
            )
        )!!
        assertEquals(8000.0, cg.sqFeet, 0.01)

        // 4. Bowditch Closed Traverse adjustment
        val trav = com.example.engine.SurveyMathProEngine.solveClosedTraverse(
            listOf(
                com.example.engine.TraverseLegInput("AB", 0.0, 100.0),
                com.example.engine.TraverseLegInput("BC", 90.0, 80.0),
                com.example.engine.TraverseLegInput("CD", 180.0, 100.0),
                com.example.engine.TraverseLegInput("DA", 270.0, 80.0)
            )
        )!!
        assertEquals(8000.0, trav.adjustedAreaSqFt, 0.05)
        assertEquals(0.0, trav.linearMisclosureFt, 0.001)

        // 5. Multi-Law Inheritance: Hindu Dayabhaga & Christian Succession Act 1925
        val hinduRep = com.example.engine.SurveyMathProEngine.calculateUniversalInheritance(
            lawType = com.example.model.InheritanceLawType.HINDU_DAYABHAGA,
            totalDecimal = 60.0,
            deceasedIsMale = true,
            hasSpouse = true,
            sons = 2,
            daughters = 1,
            hasFather = false,
            hasMother = false
        )
        assertEquals(20.0, hinduRep.shares.first().totalDecimal, 0.01) // Widow gets 1/3
        assertTrue(hinduRep.requiresLegalReview)

        val christianRep = com.example.engine.SurveyMathProEngine.calculateUniversalInheritance(
            lawType = com.example.model.InheritanceLawType.CHRISTIAN_BD,
            totalDecimal = 90.0,
            deceasedIsMale = true,
            hasSpouse = true,
            sons = 1,
            daughters = 1,
            hasFather = false,
            hasMother = false
        )
        assertEquals(30.0, christianRep.shares[0].totalDecimal, 0.01) // Spouse 1/3 = 30
        assertEquals(30.0, christianRep.shares[1].totalDecimal, 0.01) // Son 1/3 = 30
        assertEquals(30.0, christianRep.shares[2].totalDecimal, 0.01) // Daughter 1/3 = 30

        // 6. Custom Formula Evaluator
        val formulaVal = com.example.engine.FormulaExpressionEvaluator.evaluate("((n+s)/2)*e/435.6", mapOf("n" to 100.0, "s" to 100.0, "e" to 43.56))!!
        assertEquals(10.0, formulaVal, 0.001)
    }

    @Test
    fun polygonDividerEngine_splitsEqualAndRoadWithAreaConservation() {
        val plot = listOf(
            com.example.model.PolygonVertex(0.0, 0.0),
            com.example.model.PolygonVertex(120.0, 0.0),
            com.example.model.PolygonVertex(120.0, 90.0),
            com.example.model.PolygonVertex(0.0, 90.0)
        )
        val divRes = com.example.engine.PolygonDividerEngine.dividePolygon(
            id = "test_split_1",
            title = "Test Split",
            vertices = plot,
            mode = com.example.model.LandSplitMode.EQUAL,
            shares = listOf(
                com.example.model.DivisionShareSpec("মালিক ১", 1.0),
                com.example.model.DivisionShareSpec("মালিক ২", 1.0),
                com.example.model.DivisionShareSpec("মালিক ৩", 1.0)
            ),
            roadWidthFt = 10.0,
            cutAngleDeg = 0.0
        )!!
        assertEquals(10800.0, divRes.originalAreaSqFt, 0.5)
        assertTrue(divRes.roadReservedSqFt > 800.0)
        assertTrue("Area conservation must hold", divRes.isValidConservation)
        assertEquals(3, divRes.parcels.size)
        assertEquals(divRes.usableAreaSqFt / 3.0, divRes.parcels[0].areaSqFt, 1.5)
    }

    @Test
    fun gisCoordinateEngine_utmRoundTripNmeaParsingSpatialExportsAndPantographAffine() {
        // 1. WGS84 <-> UTM Zone 46N Dhaka round-trip within 1cm
        val lat = 23.8103
        val lon = 90.4125
        val utm = com.example.engine.GisCoordinateEngine.latLonToUtm(lat, lon)
        assertEquals(46, utm.zone)
        val (backLat, backLon) = com.example.engine.GisCoordinateEngine.utmToLatLon(utm)
        assertEquals(lat, backLat, 0.0001)
        assertEquals(lon, backLon, 0.0001)

        // 2. NMEA-0183 GGA parsing (RTK fix quality = 4)
        val nmea = "\$GNGGA,061500.00,2348.6180,N,09024.7500,E,4,12,0.7,11.5,M,-46.2,M,,*61\n" +
            "\$GNGGA,061510.00,2348.6180,N,09024.7720,E,4,12,0.7,11.6,M,-46.2,M,,*62\n" +
            "\$GNGGA,061520.00,2348.6000,N,09024.7720,E,4,12,0.8,11.5,M,-46.2,M,,*63"
        val parsed = com.example.engine.GisCoordinateEngine.parseNmeaLog(nmea)
        assertEquals(3, parsed.validFixes)
        assertEquals(3, parsed.rtkFixCount)

        // 3. Spatial exports (GeoJSON, KML, KMZ, GPX, DXF)
        assertTrue(com.example.engine.GisCoordinateEngine.exportGeoJson("Plot1", parsed.waypoints).contains("\"Polygon\""))
        assertTrue(com.example.engine.GisCoordinateEngine.exportKml("Plot1", parsed.waypoints).contains("<Polygon>"))
        val kmz = com.example.engine.GisCoordinateEngine.exportKmzBytes("Plot1", parsed.waypoints)
        assertEquals('P'.code.toByte(), kmz[0]) // ZIP magic
        assertEquals('K'.code.toByte(), kmz[1])
        assertTrue(com.example.engine.GisCoordinateEngine.exportGpx("Plot1", parsed.waypoints).contains("<gpx"))
        assertTrue(com.example.engine.GisCoordinateEngine.exportPlanarDxf("Plot1", listOf(0.0 to 0.0, 10.0 to 0.0, 10.0 to 10.0)).contains("LWPOLYLINE"))

        // 4. Pantograph 2D Affine fit with zero error on exact scaled/shifted pairs
        val cp = listOf(
            com.example.model.ControlPointPair("1", "CP1", 100.0, 100.0, 50.0, 50.0),
            com.example.model.ControlPointPair("2", "CP2", 300.0, 100.0, 150.0, 50.0),
            com.example.model.ControlPointPair("3", "CP3", 300.0, 300.0, 150.0, 150.0)
        )
        val panto = com.example.engine.GisCoordinateEngine.computePantographTransform(
            baseStage = com.example.model.CadastralSurveyStage.CS,
            overlayStage = com.example.model.CadastralSurveyStage.RS,
            transformType = com.example.model.TransformModelType.AFFINE,
            pairs = cp
        )
        assertEquals(2.0, panto.scaleX, 0.001)
        assertTrue(panto.rmseErrorPx < 0.01)
    }

    @Test
    fun documentAndReportEngine_docxOcrAesEncryptionAndReportRenderWork() {
        // 1. OpenXML DOCX archive
        val docx = com.example.engine.DocumentAndReportEngine.buildDocxBytes("খসড়া দলিল", "১. প্রথম পক্ষ\n২. দ্বিতীয় পক্ষ")
        assertEquals('P'.code.toByte(), docx[0])
        assertEquals('K'.code.toByte(), docx[1])
        assertTrue(docx.size > 200)

        // 2. Bangla OCR field extraction
        val raw = "মৌজা: পুবাইল\nজে.এল. নং: ৮৫\nখতিয়ান নং: ৪১২\nদাগ নং: ১০৪২\nজমির পরিমাণ: ৩৩.৫০ শতাংশ"
        val fields = com.example.engine.DocumentAndReportEngine.extractKhatianFieldsFromText(raw)
        assertEquals("412", fields.first { it.key == "khatian" }.value)
        assertEquals("1042", fields.first { it.key == "dag" }.value)
        assertEquals("পুবাইল", fields.first { it.key == "mouza" }.value)

        // 3. AES-256-GCM Vault encryption/decryption
        val secretJson = "{\"project\":\"Pubail\"}"
        val enc = com.example.engine.DocumentAndReportEngine.encryptVaultJson(secretJson, "myPass2026")
        assertTrue(enc.startsWith("BHUMI_VAULT_V1|"))
        val dec = com.example.engine.DocumentAndReportEngine.decryptVaultJson(enc, "myPass2026")
        assertEquals(secretJson, dec)
        assertEquals(null, com.example.engine.DocumentAndReportEngine.decryptVaultJson(enc, "wrongPass"))

        // 4. A4 Report bitmap render
        val reportBmp = com.example.engine.DocumentAndReportEngine.renderSurveyReportBitmap(
            com.example.model.SurveyReportConfig(id = "r1", mouza = "পুবাইল", pageSize = "A4", orientation = "Portrait")
        )
        assertEquals(1240, reportBmp.width)
        assertEquals(1754, reportBmp.height)
        reportBmp.recycle()
    }

    @Test
    fun workstationRepository_seedSampleAndBackupRestoreRoundTrip() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.AppPreferences(context)
        val repo = com.example.data.WorkstationRepository.getInstance(context)
        repo.seedSampleBangladeshProject()
        assertTrue(repo.snapshot.value.unifiedProjects.any { it.id == "sample_bd_pubail_2026" })

        val backup = repo.exportBackupJson(prefs)
        assertTrue(backup.contains("sample_bd_pubail_2026"))
        val (ok, _) = repo.restoreFromBackupJson(backup, prefs)
        assertTrue(ok)
    }

    @Test
    fun expansionMathAndSurveyEngine_partsABCFCalculationsAndChecksWork() {
        // Part A02 Old Map calibration + distortion
        val cal = com.example.engine.ExpansionMathAndSurveyEngine.analyzeOldMapMeasurement(
            pixelSpanMeasured = 420.0,
            knownGroundDistFt = 100.0,
            secondaryPixelSpan = 400.0,
            secondaryKnownDistFt = 100.0,
            polygonPixelArea = 168100.0
        )
        assertEquals(4.1, cal.pixelsPerFoot, 0.01)
        assertTrue(cal.scaleDistortionPct > 4.0)
        assertTrue(cal.mapAreaDecimal > 20.0)

        // Part B01 Total Station Shot & MLM
        val shot = com.example.engine.ExpansionMathAndSurveyEngine.computeTotalStationShot(
            stationN = 1000.0, stationE = 1000.0, stationRl = 10.0,
            instrumentHeight = 5.0, prismHeight = 5.0,
            horizontalAzimuthDeg = 90.0, verticalAngleFromHorizDeg = 0.0,
            slopeDistFt = 100.0
        )
        assertEquals(1000.0, shot.targetNorthFt, 0.01)
        assertEquals(1100.0, shot.targetEastFt, 0.01)
        assertEquals(10.0, shot.targetElevationFt, 0.01)

        val mlm = com.example.engine.ExpansionMathAndSurveyEngine.computeMissingLine(1000.0, 1000.0, 10.0, 1030.0, 1040.0, 10.0)
        assertEquals(50.0, mlm.horizontalDistFt, 0.01)

        // Part B02 Angular Misclosure
        val (theo, mis, _) = com.example.engine.ExpansionMathAndSurveyEngine.checkPolygonAngularMisclosure(listOf(90.0, 90.0, 90.0, 90.5))
        assertEquals(360.0, theo, 0.001)
        assertEquals(0.5, mis, 0.001)

        // Part B03 Auto Level Book Arithmetic Check (Sum BS - Sum FS == Sum Rise - Sum Fall == Last RL - First RL)
        val lvl = com.example.engine.ExpansionMathAndSurveyEngine.computeLevelBook(
            benchmarkRl = 10.0,
            entries = listOf(
                com.example.model.LevelReadingEntry("1", "BM1", 0.0, bs = 1.500),
                com.example.model.LevelReadingEntry("2", "IS1", 50.0, intermediateSight = 1.200),
                com.example.model.LevelReadingEntry("3", "BM2", 100.0, fs = 1.800)
            ),
            knownClosingBenchmarkRl = 9.700
        )
        assertTrue("Level book arithmetic check must balance", lvl.arithmeticBalanced)
        assertEquals(9.700, lvl.rows.last().rl, 0.001)
        assertEquals(0.0, lvl.closingError, 0.001)

        // Part B07 Cut & Fill
        val ew = com.example.engine.ExpansionMathAndSurveyEngine.computeEarthworkCutFill(100.0, 20.0, 10.0, 10.0, 12.0, 12.0)
        assertTrue(ew.isFill)
        assertEquals(4000.0, ew.netVolumeCuFt, 0.1)

        // Part C05 Duplicate Holdings Detection
        val dupes = com.example.engine.ExpansionMathAndSurveyEngine.detectDuplicateHoldings(
            listOf(
                com.example.model.KhajnaHoldingRecord("1", "H1", "A", "পুবাইল", khatianNo = "412", dagNo = "1042", areaDecimal = 10.0),
                com.example.model.KhajnaHoldingRecord("2", "H2", "B", "পুবাইল", khatianNo = "412", dagNo = "1042", areaDecimal = 10.0)
            )
        )
        assertEquals(1, dupes.size)

        // Part F01 & F05 Reconciliation & Net Usable Land
        val rec = com.example.engine.ExpansionMathAndSurveyEngine.reconcileLandRecords(40.0, 20.0, 5.0, 14.5)
        assertEquals(15.0, rec.netRecordDecimal, 0.001)
        assertEquals(-0.5, rec.differenceDecimal, 0.001)

        val usable = com.example.engine.ExpansionMathAndSurveyEngine.computeNetUsableLand(
            grossSqFt = 10000.0, roadLengthFt = 100.0, roadWidthFt = 10.0,
            pondSqFt = 500.0, canalSqFt = 0.0, buildingSqFt = 500.0, otherRestrictionSqFt = 0.0
        )
        assertEquals(8000.0, usable.netUsableSqFt, 0.01)
    }

    @Test
    fun universalExportEngine_xlsxCsvShapefileZipAndCadastralBitmapsWork() {
        val headers = listOf("Dag", "AreaDec")
        val rows = listOf(listOf("1042", "33.5"), listOf("1043", "18.2"))

        val xlsx = com.example.engine.UniversalExportEngine.buildXlsxBytes("Plots", headers, rows)
        assertEquals('P'.code.toByte(), xlsx[0])
        assertEquals('K'.code.toByte(), xlsx[1])
        assertTrue(xlsx.size > 300)

        val csv = com.example.engine.UniversalExportEngine.buildCsvText(headers, rows)
        assertTrue(csv.contains("Dag,AreaDec"))
        assertTrue(csv.contains("1042,33.5"))

        val shpZip = com.example.engine.UniversalExportEngine.buildShapefileZipBytes(
            "pubail_plot",
            listOf(
                com.example.model.GeoWaypoint("1", "P1", 23.810, 90.412),
                com.example.model.GeoWaypoint("2", "P2", 23.810, 90.413),
                com.example.model.GeoWaypoint("3", "P3", 23.811, 90.413)
            )
        )
        assertEquals('P'.code.toByte(), shpZip[0])
        assertEquals('K'.code.toByte(), shpZip[1])
        assertTrue(shpZip.size > 200)

        val a4 = com.example.engine.UniversalExportEngine.renderCadastralPrintBitmap("Test", "A4", "16\"=1M", "1042", true, true)
        assertEquals(1240, a4.width)
        assertEquals(1754, a4.height)
        a4.recycle()

        val docBmp = com.example.engine.UniversalExportEngine.renderDocumentReportBitmap("Title", "Sub", "Line 1\nLine 2")
        assertEquals(1240, docBmp.width)
        assertTrue(docBmp.height >= 1754)
        docBmp.recycle()
    }

    @Test
    fun expansionRepository_crudServiceCasesCustomWorkflowAndArchiveWork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.ExpansionRepository.getInstance(context)

        assertTrue(repo.snapshot.value.legalClauses.isNotEmpty())
        assertTrue(repo.snapshot.value.customServices.isNotEmpty())

        repo.saveServiceCase(
            com.example.model.ServiceApplicationCase(
                id = "case_test_1",
                serviceKind = com.example.model.LandServiceWorkflowKind.E_MUTATION,
                title = "পুবাইল নামজারি টেস্ট"
            )
        )
        assertTrue(repo.snapshot.value.serviceCases.any { it.id == "case_test_1" })

        repo.saveArchiveDoc(
            com.example.model.UniversalArchiveDocument(
                id = "arch_test_1",
                docType = "দলিল",
                title = "সাফ-কবলা নথি",
                dagNo = "1042"
            )
        )
        assertTrue(repo.snapshot.value.archiveDocs.any { it.id == "arch_test_1" })
        repo.deleteArchiveDoc("arch_test_1")
        assertTrue(repo.snapshot.value.archiveDocs.none { it.id == "arch_test_1" })
    }

    @Test
    fun cadAndDimensionEngine_evaluatesAll30DimensionsAnd8UnitsWithDxfSvgLayout() {
        val layers = com.example.engine.CadAndDimensionEngine.defaultLayers()
        assertEquals(16, layers.size)

        val poly = listOf(
            com.example.model.PolygonVertex(0.0, 0.0),
            com.example.model.PolygonVertex(120.0, 0.0),
            com.example.model.PolygonVertex(120.0, 90.0),
            com.example.model.PolygonVertex(0.0, 90.0)
        )
        val plotEntity = com.example.model.CadEntity(
            id = "plot_1",
            type = com.example.model.CadEntityType.POLYLINE,
            points = poly,
            closed = true
        )

        // All 8 measurement display units must format 37.667 ft meaningfully
        com.example.model.MeasurementDisplayUnit.entries.forEach { unit ->
            val fmt = com.example.engine.CadAndDimensionEngine.formatFeetDistance(37.667, unit)
            assertTrue("Unit $unit produced blank format", fmt.isNotBlank())
        }

        // All 30 dimension types must evaluate cleanly without throwing
        com.example.model.CadDimensionType.entries.forEachIndexed { idx, dimType ->
            val dim = com.example.model.CadDimensionEntity(
                id = "dim_$idx",
                dimType = dimType,
                p1 = com.example.model.PolygonVertex(0.0, 0.0),
                p2 = com.example.model.PolygonVertex(100.0, 0.0),
                p3 = com.example.model.PolygonVertex(100.0, 60.0),
                unit = com.example.model.MeasurementDisplayUnit.entries[idx % 8]
            )
            val evaluated = com.example.engine.CadAndDimensionEngine.evaluateDimension(dim, listOf(plotEntity))
            assertTrue("Dim $dimType produced blank label", evaluated.formattedLabel.isNotBlank())
        }

        // Auto plot dimensioning creates 4 side dimensions + 4 corner angles + 1 diagonal = 9
        val autoDims = com.example.engine.CadAndDimensionEngine.generateAutoPlotDimensions(
            vertices = poly,
            unit = com.example.model.MeasurementDisplayUnit.FEET_INCHES,
            includeDiagonals = true,
            includeAngles = true,
            includeOrdinates = false
        )
        assertEquals(9, autoDims.size)

        // OSNAP Endpoint snaps within radius (outside perpendicular projection on segment)
        val snapMatch = com.example.engine.CadAndDimensionEngine.resolveSnap(
            raw = com.example.model.PolygonVertex(121.5, -1.5),
            lastAnchor = null,
            entities = listOf(plotEntity),
            settings = com.example.model.CadStyleSettings(),
            snapRadiusFt = 5.0
        )
        assertEquals(com.example.model.CadOsnapMode.ENDPOINT, snapMatch.activeMode)
        assertEquals(120.0, snapMatch.snapped.x, 0.01)

        // DXF, SVG, and Paper-Space A4 layout bitmap
        val dxf = com.example.engine.CadAndDimensionEngine.exportCadToDxf("Pubail", listOf(plotEntity), autoDims, layers)
        assertTrue(dxf.contains("LWPOLYLINE") && dxf.contains("TEXT") && dxf.contains("LAYER"))
        val svg = com.example.engine.CadAndDimensionEngine.exportCadToSvg("Pubail", listOf(plotEntity), autoDims, layers)
        assertTrue(svg.contains("<svg") && svg.contains("<polygon"))
        val bmp = com.example.engine.CadAndDimensionEngine.renderCadLayoutSheetBitmap(
            title = "পুবাইল",
            mouzaInfo = "মৌজা: পুবাইল",
            paperSize = "A4",
            scaleLabel = "1\"=30'",
            entities = listOf(plotEntity),
            dimensions = autoDims,
            layers = layers,
            unit = com.example.model.MeasurementDisplayUnit.FEET_INCHES
        )
        assertTrue(bmp.width >= 1000 && bmp.height >= 1000)
        bmp.recycle()
    }

    @Test
    fun cogoAndSurfaceEngine_inverseIntersectionsTraversesCurvesTinAndScheduleWork() {
        val p1 = com.example.model.CogoPoint("1", 1, "CP", 1000.0, 1000.0, 12.0)
        val p2 = com.example.model.CogoPoint("2", 2, "CP", 1000.0, 1100.0, 12.0)
        val inv = com.example.engine.CogoAndSurfaceEngine.inverse(p1, p2)
        assertEquals(100.0, inv.horizontalDistFt, 0.01)
        assertEquals(90.0, inv.azimuthDeg, 0.01)

        val rad = com.example.engine.CogoAndSurfaceEngine.radiation(p1, 0.0, 50.0, 1.5, 3)
        assertEquals(1050.0, rad.northFt, 0.01)
        assertEquals(1000.0, rad.eastFt, 0.01)

        val bb = com.example.engine.CogoAndSurfaceEngine.intersectBearingBearing(
            com.example.model.PolygonVertex(0.0, 0.0), 0.0,
            com.example.model.PolygonVertex(100.0, 100.0), 270.0
        )!!
        assertEquals(0.0, bb.x, 0.01)
        assertEquals(100.0, bb.y, 0.01)

        val dd = com.example.engine.CogoAndSurfaceEngine.intersectDistanceDistance(
            com.example.model.PolygonVertex(0.0, 0.0), 50.0,
            com.example.model.PolygonVertex(80.0, 0.0), 50.0
        )
        assertEquals(2, dd.size)
        assertEquals(40.0, dd.first().x, 0.01)

        val legs = listOf(
            com.example.engine.TraverseLegInput("AB", 0.0, 100.0),
            com.example.engine.TraverseLegInput("BC", 90.0, 80.0),
            com.example.engine.TraverseLegInput("CD", 180.0, 100.2),
            com.example.engine.TraverseLegInput("DA", 270.0, 79.9)
        )
        com.example.engine.CogoAndSurfaceEngine.TraverseMethod.entries.forEach { method ->
            val adj = com.example.engine.CogoAndSurfaceEngine.adjustClosedTraverse(legs, method)!!
            assertTrue(adj.adjustedAreaSqFt > 7900.0)
            assertTrue("Expected precision ratio starting with 1 :, got ${adj.precisionRatioLabel}", adj.precisionRatioLabel.startsWith("1 :"))
        }

        val curve = com.example.engine.CogoAndSurfaceEngine.computeCircularCurve(
            radiusFt = 200.0,
            deltaDeg = 60.0,
            piPoint = com.example.model.PolygonVertex(100.0, 100.0),
            backAzimuthDeg = 0.0
        )
        assertTrue(curve.arcLengthFt > 200.0 && curve.tangentLengthFt > 100.0)

        val rect = listOf(
            com.example.model.PolygonVertex(0.0, 0.0),
            com.example.model.PolygonVertex(100.0, 0.0),
            com.example.model.PolygonVertex(100.0, 80.0),
            com.example.model.PolygonVertex(0.0, 80.0)
        )
        val hinged = com.example.engine.CogoAndSurfaceEngine.subdivideByHingedLine(rect, 0, 4000.0)!!
        val a1 = com.example.engine.SurveyMathProEngine.computeCoordinateGeometry(hinged.first)!!.sqFeet
        assertEquals(4000.0, a1, 50.0)

        // TIN Surface + Contours + Cut/Fill
        val tinPts = listOf(
            com.example.model.CogoPoint("t1", 1, "SPOT", 0.0, 0.0, 10.0),
            com.example.model.CogoPoint("t2", 2, "SPOT", 0.0, 100.0, 12.0),
            com.example.model.CogoPoint("t3", 3, "SPOT", 100.0, 100.0, 14.0),
            com.example.model.CogoPoint("t4", 4, "SPOT", 100.0, 0.0, 11.0),
            com.example.model.CogoPoint("t5", 5, "SPOT", 50.0, 50.0, 13.0)
        )
        val tin = com.example.engine.CogoAndSurfaceEngine.buildTinSurface(tinPts)
        assertTrue(tin.isNotEmpty())
        val contours = com.example.engine.CogoAndSurfaceEngine.generateContours(tin, 1.0)
        assertTrue(contours.isNotEmpty())
        val vol = com.example.engine.CogoAndSurfaceEngine.computeSurfaceCutFillAgainstDesignElevation(tin, 12.0)
        assertTrue(vol.cutVolumeCuFt >= 0.0 && vol.fillVolumeCuFt >= 0.0)

        // Road alignment & Automated Land Schedule
        val roadRes = com.example.engine.CogoAndSurfaceEngine.analyzeRoadAlignmentAndAccess(
            road = com.example.model.RoadAlignmentSpec(),
            plotPolygon = rect,
            stationPoints = tinPts
        )
        assertTrue(roadRes.totalCenterlineLengthFt > 150.0 && roadRes.stationOffsets.size == 5)
        val sched = com.example.engine.CogoAndSurfaceEngine.generateAutomatedLandSchedule(
            mouza = "পুবাইল", jlNo = "৮৫", khatianNo = "৪১২", dagNo = "১০৪২",
            landClass = "নাল", vertices = rect,
            northNeighbor = "দাগ ১০৪১", southNeighbor = "রাস্তা",
            eastNeighbor = "দাগ ১০৪৩", westNeighbor = "পুকুর",
            languageBn = true
        )
        assertTrue(sched.contains("পুবাইল") && sched.contains("১০৪২") && sched.contains("শতাংশ"))
    }

    @Test
    fun fieldIoQualityDeedVintagePaperAndMasterZipWorkEndToEnd() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val masterRepo = com.example.data.MasterCadRepository.getInstance(context)
        masterRepo.seedPubailMasterSample()
        val snap = masterRepo.snapshot.value
        assertEquals(15, com.example.model.UnionRegisterKind.entries.size)
        assertTrue(snap.unionRegisters.size >= 3)
        assertTrue(snap.historicalDeeds.isNotEmpty())

        // 1. Historical deed OCR parse & dual text
        val ocrSample = "মৌজা: পুবাইল\nজে.এল: ৮৫\nখতিয়ান: ৪১২\nদাগ: ১০৪২\nদলিল নং: 4521\nদাতা: ছমির উদ্দিন\nগ্রহীতা: রফিকুল ইসলাম\nপরিমাণ: ২৪.৭৯ শতাংশ"
        val parsedDeed = com.example.engine.FieldIoQualityAndDeedEngine.parseHistoricalDeedText(ocrSample)
        assertEquals("4521", parsedDeed.deedNumber)
        assertEquals("পুবাইল", parsedDeed.mouza)
        val oldText = com.example.engine.FieldIoQualityAndDeedEngine.buildFaithfulHistoricalReproduction(parsedDeed)
        val modText = com.example.engine.FieldIoQualityAndDeedEngine.buildModernEditableLegalDraft(parsedDeed)
        assertTrue(oldText.contains("সাব-রেজিস্ট্রি") || oldText.contains("পুবাইল"))
        assertTrue(modText.contains("অসত্যায়িত") || modText.contains("খসড়া"))

        // 2. Field PNEZD + LandXML import
        val csvImport = com.example.engine.FieldIoQualityAndDeedEngine.importFieldOrVectorText(
            "1,1000,1000,12.0,BDY\n2,1000,1120,12.3,BDY\n3,1090,1120,12.8,BDY\n4,1090,1000,12.1,BDY"
        )
        assertEquals(4, csvImport.points.size)
        assertTrue(csvImport.entities.isNotEmpty())

        val landXmlImport = com.example.engine.FieldIoQualityAndDeedEngine.importFieldOrVectorText(
            "<LandXML><CgPoints><CgPoint name=\"101\">1000.0 1000.0 12.5</CgPoint><CgPoint name=\"102\">1050.0 1020.0 13.0</CgPoint></CgPoints></LandXML>"
        )
        assertEquals(2, landXmlImport.points.size)

        // 3. Honest Compatibility Matrix (proprietary formats marked nativeInApp = false)
        val dwgInfo = com.example.engine.FieldIoQualityAndDeedEngine.FILE_COMPATIBILITY_MATRIX.first { it.extension.contains("DWG") }
        assertTrue(!dwgInfo.nativeInApp)

        // 4. Master Project ZIP export
        val zipBytes = com.example.engine.FieldIoQualityAndDeedEngine.buildMasterProjectBundleZip(
            projectTitle = "Pubail_Master",
            entities = snap.entities,
            dimensions = snap.dimensions,
            layers = snap.layers,
            cogoPoints = snap.cogoPoints,
            deedDraftText = oldText,
            scheduleText = "Schedule Sample"
        )
        assertEquals('P'.code.toByte(), zipBytes[0])
        assertEquals('K'.code.toByte(), zipBytes[1])
        assertTrue(zipBytes.size > 1000)

        // 5. Vintage Legal Stamp Paper Bitmap must be warm parchment (R > G > B), never pure white #FFFFFF
        val vintageBmp = com.example.engine.DocumentAndReportEngine.renderVintageLegalBitmap(
            title = "সাফ-কবলা দলিল (পুরাতন কাগজ)",
            stampValueLabel = "১০০ টাকা নন-জুডিশিয়াল স্ট্যাম্প",
            serialTag = "DEED-1962",
            bodyText = oldText
        )
        val samplePx = vintageBmp.getPixel(120, 420)
        val r = Color.red(samplePx)
        val g = Color.green(samplePx)
        val b = Color.blue(samplePx)
        assertTrue("Vintage paper must not be pure white (#FFFFFF)", !(r > 250 && g > 250 && b > 250))
        assertTrue("Vintage paper must have warm parchment tint (R >= G > B), got ($r,$g,$b)", r >= g && g > b)
        vintageBmp.recycle()
    }

    @Test
    fun phase1_m01AndM02_customUnitsShoelaceHolesLockSideTapeAndUncertaintyWork() {
        // M01: New BD/Regional units & Bangla digits
        val guntha = com.example.engine.LandCalculators.fromGuntha(1.0)
        assertEquals(2.5, guntha.decimal, 0.0001)
        assertEquals(1089.0, guntha.sqFeet, 0.01)

        val chotak = com.example.engine.LandCalculators.fromChotak(16.0)
        assertEquals(1.0, chotak.katha, 0.0001)
        assertEquals(720.0, chotak.sqFeet, 0.01)

        val lengths = com.example.engine.LandCalculators.convertLength(120.0, com.example.engine.LengthUnit.FEET)
        assertEquals(1.0, lengths[com.example.engine.LengthUnit.ROSHI]!!, 0.0001)
        assertEquals(3657.6, lengths[com.example.engine.LengthUnit.CM]!!, 0.5)
        assertEquals("১২৩.৪৫", com.example.engine.LandCalculators.toBanglaDigits("123.45"))

        val customPreset = com.example.model.CustomUnitPreset(
            id = "sylhet_test",
            nameBn = "টেস্ট কাস্টম",
            decimalSqFt = 435.6,
            kathaSqFt = 1440.0,
            bighaSqFt = 14400.0,
            kaniDecimals = 30.0
        )
        assertEquals(1.0, com.example.engine.LandCalculators.fromSqFeet(1440.0, customPreset).katha, 0.0001)

        // Shoelace step-by-step & Hole cut-out subtraction
        val outer = listOf(0.0 to 0.0, 100.0 to 0.0, 100.0 to 50.0, 0.0 to 50.0)
        val (grossArea, steps) = com.example.engine.GeometryAndAccuracyEngine.computeShoelaceWithSteps(outer)
        assertEquals(5000.0, grossArea, 0.01)
        assertEquals(4, steps.size)
        val hole = listOf(20.0 to 10.0, 40.0 to 10.0, 40.0 to 20.0, 20.0 to 20.0)
        val netArea = com.example.engine.GeometryAndAccuracyEngine.computeNetAreaWithHolesSqFt(outer, listOf(hole))
        assertEquals(4800.0, netArea, 0.01)

        // Lock Side Length solver (100 ft -> 150 ft on 1000x1000 ft page)
        val normSquare = listOf(0.1f to 0.1f, 0.2f to 0.1f, 0.2f to 0.2f, 0.1f to 0.2f)
        val locked = com.example.engine.GeometryAndAccuracyEngine.adjustNormalizedPolygonEdgeLength(
            polyNorm = normSquare,
            edgeIndex = 0,
            targetFeet = 150.0,
            pageWidthFt = 1000.0,
            pageHeightFt = 1000.0
        )
        assertEquals(0.25f, locked[1].first, 0.001f)
        assertEquals(0.10f, locked[1].second, 0.001f)

        // Tape Polygon Builder (100x100 ft closed square)
        val tape = com.example.engine.GeometryAndAccuracyEngine.buildPolygonFromTapeLegs(
            legDistancesFeet = listOf(100.0, 100.0, 100.0, 100.0),
            legBearingsDeg = listOf(0.0, 90.0, 180.0, 270.0)
        )
        assertEquals(10000.0, tape.areaSqFeet, 0.1)
        assertEquals(400.0, tape.perimeterFeet, 0.01)
        assertTrue(tape.closureErrorFeet < 0.01)

        // M02: Uncertainty propagation + ConfidenceLevel thresholds
        val uncHigh = com.example.engine.GeometryAndAccuracyEngine.computeMeasurementUncertainty(
            areaSqFeet = 4356.0,
            perimeterFeet = 264.0,
            edgeLengthsFt = listOf(66.0, 66.0, 66.0, 66.0),
            calibrationErrPct = 0.5,
            ftPerPixel = 0.2,
            zoomScale = 2.0f,
            gpsAccuracyM = 0.0
        )
        assertEquals(10.0, uncHigh.decimal, 0.001)
        assertTrue(uncHigh.decimalErr > 0.0)
        assertEquals(4, uncHigh.edgeErrorsFt.size)
        assertEquals(com.example.model.ConfidenceLevel.HIGH, uncHigh.confidence)

        val uncLow = com.example.engine.GeometryAndAccuracyEngine.computeMeasurementUncertainty(
            areaSqFeet = 4356.0,
            perimeterFeet = 264.0,
            edgeLengthsFt = listOf(66.0, 66.0, 66.0, 66.0),
            calibrationErrPct = 3.0,
            ftPerPixel = 0.5,
            zoomScale = 1.0f,
            gpsAccuracyM = 12.0
        )
        assertEquals(com.example.model.ConfidenceLevel.LOW, uncLow.confidence)
        assertTrue(uncLow.relativeErrPct > 4.0)
    }

    @Test
    fun phase1_m03AndN01_calibrationPdfInspectSplitAnnotatedExportAndParcelCsvWork() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Multi-line X/Y differential shrinkage calibration
        val pagePts = 720 // 10 inches -> 3300 ft at 16"=1mile
        val lines = listOf(
            com.example.model.CalibrationRefLine(
                id = "c1", label = "Horiz",
                p1Norm = 0.1f to 0.2f, p2Norm = 0.3f to 0.2f,
                actualFeet = 693.0, measuredFeetBefore = 660.0 // +5% X
            ),
            com.example.model.CalibrationRefLine(
                id = "c2", label = "Vert",
                p1Norm = 0.2f to 0.1f, p2Norm = 0.2f to 0.3f,
                actualFeet = 627.0, measuredFeetBefore = 660.0 // -5% Y
            )
        )
        val cal = com.example.engine.MouzaN01Extensions.calibrateScaleMultiLine(
            lines = lines,
            pageWidthPts = pagePts,
            pageHeightPts = pagePts,
            baseScale = com.example.model.MouzaScale.SCALE_16,
            expectedPaperWidthIn = 10.0,
            expectedPaperHeightIn = 10.0
        )
        assertEquals(1.05, cal.scaleXMultiplier, 0.005)
        assertEquals(0.95, cal.scaleYMultiplier, 0.005)
        assertTrue(cal.rmsDeviationPct < 0.2)

        // 2. Uncompressed vector PDF + encrypted PDF detection
        val cacheDir = context.cacheDir
        val vecPdf = java.io.File(cacheDir, "test_vec.pdf")
        vecPdf.writeText(
            "%PDF-1.4\n1 0 obj << /Type /Page /MediaBox [0 0 1000 1000] >> endobj\n" +
                "stream\nBT (Scale 32\" = 1 Mile) Tj ET\n100 100 200 150 re S\nendstream\n%%EOF"
        )
        val repVec = com.example.engine.MouzaN01Extensions.inspectPdfStructure(vecPdf)
        assertTrue(repVec.isPdf)
        assertTrue(!repVec.isEncrypted)
        assertTrue(repVec.hasVectorPaths)
        assertEquals(1, repVec.extractedVectorPolygons.size)
        assertEquals(165.0, repVec.suggestedFeetPerInch!!, 0.01)

        val encPdf = java.io.File(cacheDir, "test_enc.pdf")
        encPdf.writeText("%PDF-1.6\n1 0 obj << /Encrypt 2 0 R >> endobj\n%%EOF")
        val repEnc = com.example.engine.MouzaN01Extensions.inspectPdfStructure(encPdf)
        assertTrue(repEnc.isPdf && repEnc.isEncrypted)

        // 3. Split polygon by tape line preserves area
        val sqNorm = listOf(0.2f to 0.2f, 0.6f to 0.2f, 0.6f to 0.6f, 0.2f to 0.6f)
        val split = com.example.engine.MouzaN01Extensions.splitNormalizedPolygonByLine(
            polyNorm = sqNorm,
            p1Norm = 0.4f to 0.1f,
            p2Norm = 0.4f to 0.7f
        )!!
        val areaA = com.example.engine.GeometryAndAccuracyEngine.computeShoelaceWithSteps(split.first.map { it.first.toDouble() to it.second.toDouble() }).first
        val areaB = com.example.engine.GeometryAndAccuracyEngine.computeShoelaceWithSteps(split.second.map { it.first.toDouble() to it.second.toDouble() }).first
        assertEquals(0.08, areaA, 0.002)
        assertEquals(0.08, areaB, 0.002)

        // 4. Plot <-> Plot min distance & Road Frontage & Parcel CSV excluding road
        val p1 = listOf(0.1f to 0.1f, 0.2f to 0.1f, 0.2f to 0.2f, 0.1f to 0.2f)
        val p2 = listOf(0.25f to 0.1f, 0.35f to 0.1f, 0.35f to 0.2f, 0.25f to 0.2f)
        val gap = com.example.engine.GeometryAndAccuracyEngine.measureBetweenPolygonsFt(p1, p2, 1000.0, 1000.0)
        assertEquals(50.0, gap.distanceFt, 0.1)

        val frontage = com.example.engine.GeometryAndAccuracyEngine.computePlotRoadFrontageFt(
            polyNorm = p1,
            roadANorm = 0.05f to 0.22f,
            roadBNorm = 0.30f to 0.22f,
            pageWidthFt = 1000.0,
            pageHeightFt = 1000.0
        )
        assertEquals(100.0, frontage.distanceFt, 0.5)

        val plots = listOf(
            com.example.model.MouzaPlot(1, 1, dagNumber = "101", pixelCount = 0, maskIndices = IntArray(0), gridWidth = 100, gridHeight = 100, minX = 0, minY = 0, maxX = 10, maxY = 10, color = ComposeColor.Green, polygonNormalized = p1),
            com.example.model.MouzaPlot(2, 2, dagNumber = "999", pixelCount = 0, maskIndices = IntArray(0), gridWidth = 100, gridHeight = 100, minX = 0, minY = 0, maxX = 10, maxY = 10, color = ComposeColor.Gray, polygonNormalized = p2, isRoad = true)
        )
        val rows = plots.map {
            com.example.engine.GeometryAndAccuracyEngine.buildParcelTableRow(
                plot = it,
                pageWidthPts = 720,
                pageHeightPts = 720,
                scale = com.example.model.MouzaScale.SCALE_16,
                calibrationX = 1.0,
                calibrationY = 1.0,
                calibrationErrPct = 0.5,
                settings = com.example.model.GlobalSurveySettings()
            )
        }
        val csv = com.example.engine.GeometryAndAccuracyEngine.exportParcelTableCsv(rows, "Pubail_Test")
        assertTrue(csv.contains("TOTAL (Usable 1 Plots)"))

        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val outPdf = com.example.engine.MouzaN01Extensions.exportNonDestructiveAnnotatedPdf(
            context = context,
            sourceBitmap = bmp,
            plots = plots,
            parcelRows = rows,
            title = "Pubail",
            scale = com.example.model.MouzaScale.SCALE_16,
            pageWidthPts = 720,
            pageHeightPts = 720,
            scaleXMult = 1.0,
            scaleYMult = 1.0,
            surveyorProfile = com.example.model.SurveyorProfile()
        )
        bmp.recycle()
        assertTrue(outPdf.exists() && outPdf.length() > 500)
        val hdr = outPdf.inputStream().use { ByteArray(5).apply { it.read(this) } }
        assertEquals("%PDF-", String(hdr))
    }

    @Test
    fun phase1_n02_pantographScaleHomographyTpsOutlierAndOverlapTableWork() {
        // 1. Scale Pantograph 1:3960 -> 1:1980 = 2.0x
        val sp = com.example.engine.PantographEngine.computeScalePantograph(3960.0, 1980.0)
        assertEquals(2.0, sp.magnificationFactor, 0.001)
        assertEquals(100.0, sp.percentChange, 0.01)

        // 2. Projective Homography exact recovery (2x + 10, 2y + 20)
        val cleanPairs = listOf(
            com.example.model.ControlPointPair("1", "P1", 10.0, 20.0, 0.0, 0.0),
            com.example.model.ControlPointPair("2", "P2", 210.0, 20.0, 100.0, 0.0),
            com.example.model.ControlPointPair("3", "P3", 210.0, 220.0, 100.0, 100.0),
            com.example.model.ControlPointPair("4", "P4", 10.0, 220.0, 0.0, 100.0)
        )
        val proj = com.example.engine.PantographEngine.solveTransformModel(
            baseStage = com.example.model.CadastralSurveyStage.RS,
            overlayStage = com.example.model.CadastralSurveyStage.CS,
            transformType = com.example.model.TransformModelType.PROJECTIVE,
            pairs = cleanPairs
        )
        assertTrue(proj.summary.rmseErrorPx < 0.01)
        val (tx, ty) = proj.forwardTransform(50.0, 50.0)
        assertEquals(110.0, tx, 0.05)
        assertEquals(120.0, ty, 0.05)

        // 3. TPS exact interpolation at control points
        val tps = com.example.engine.PantographEngine.solveTransformModel(
            baseStage = com.example.model.CadastralSurveyStage.RS,
            overlayStage = com.example.model.CadastralSurveyStage.CS,
            transformType = com.example.model.TransformModelType.TPS,
            pairs = cleanPairs
        )
        val (tpsX, tpsY) = tps.forwardTransform(100.0, 100.0)
        assertEquals(210.0, tpsX, 0.2)
        assertEquals(220.0, tpsY, 0.2)

        // 4. Outlier detection + leave-one-out RMSE improvement
        val noisyPairs = cleanPairs + com.example.model.ControlPointPair("5", "OUTLIER", 110.0 + 45.0, 120.0, 50.0, 50.0)
        val withOutlier = com.example.engine.PantographEngine.solveTransformModel(
            baseStage = com.example.model.CadastralSurveyStage.RS,
            overlayStage = com.example.model.CadastralSurveyStage.CS,
            transformType = com.example.model.TransformModelType.AFFINE,
            pairs = noisyPairs
        )
        assertTrue(withOutlier.outlierSuggestions.isNotEmpty())
        val topOut = withOutlier.outlierSuggestions.first()
        assertEquals("OUTLIER", topOut.label)
        assertTrue(topOut.rmseIfRemoved < topOut.rmseBefore)

        // 5. Sutherland-Hodgman Overlap Table (50% overlap of 100x100 squares)
        val oldDags = listOf(
            com.example.engine.DagPolygonInput("101", listOf(0.0 to 0.0, 100.0 to 0.0, 100.0 to 100.0, 0.0 to 100.0))
        )
        val newDags = listOf(
            com.example.engine.DagPolygonInput("201", listOf(50.0 to 0.0, 150.0 to 0.0, 150.0 to 100.0, 50.0 to 100.0))
        )
        val table = com.example.engine.PantographEngine.computeDagOverlapTable(
            oldDags = oldDags,
            newDags = newDags,
            overlayToBaseTransform = { x, y -> x to y },
            sqFeetPerMapUnitSq = 1.0
        )
        assertEquals(1, table.size)
        assertEquals(5000.0 / 435.6, table.first().overlapDecimal, 0.05)
        assertEquals(50.0, table.first().overlapPctOfOld, 0.5)
        assertEquals(50.0, table.first().overlapPctOfNew, 0.5)
    }

    @Test
    fun phase1_n03_spatialParcelImportKmzExportCoordinatesSetbackAndHomeCatalogWork() = runTest {
        // 1. GeoJSON & KML import
        val geoJson = """
            {"type":"FeatureCollection","features":[{"type":"Feature",
            "properties":{"name":"দাগ ১০৪২","DAG_NO":"1042","OWNER":"সালাম"},
            "geometry":{"type":"Polygon","coordinates":[[
              [90.4120,23.8100],[90.4126,23.8100],[90.4126,23.8105],[90.4120,23.8105],[90.4120,23.8100]
            ]]}}]}
        """.trimIndent()
        val gjParcels = com.example.engine.SpatialParcelEngine.parseGeoJson(geoJson)
        assertEquals(1, gjParcels.size)
        assertEquals("1042", gjParcels.first().dagNumber)
        assertTrue(gjParcels.first().decimal > 50.0)

        // Parcel Snap (inside vs outside)
        val hit = com.example.engine.SpatialParcelEngine.findParcelAt(23.81025, 90.4123, gjParcels)
        assertEquals("1042", hit?.dagNumber)
        assertEquals(null, com.example.engine.SpatialParcelEngine.findParcelAt(23.90, 90.90, gjParcels))

        val kml = """
            <kml><Document><Placemark><name>দাগ 505</name>
            <Polygon><outerBoundaryIs><LinearRing><coordinates>
            90.4100,23.8100,0 90.4105,23.8100,0 90.4105,23.8104,0 90.4100,23.8104,0 90.4100,23.8100,0
            </coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark></Document></kml>
        """.trimIndent()
        val kmlParcels = com.example.engine.SpatialParcelEngine.parseKml(kml)
        assertEquals(1, kmlParcels.size)
        assertEquals("505", kmlParcels.first().dagNumber)

        // 2. Enhanced KMZ export + round-trip parseKmz + verify GroundOverlay PNG inside zip
        val overlayBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }
        val kmzBytes = com.example.engine.SpatialParcelEngine.buildEnhancedKmzBytes(
            pointsLatLon = gjParcels.first().pointsLatLon,
            metadata = com.example.engine.KmzDemarcationMetadata(
                district = "গাজীপুর",
                upazila = "সদর",
                mouza = "পুবাইল",
                jlNumber = "৮৫",
                dagNumber = "1042",
                ownerName = "আব্দুস সালাম"
            ),
            overlayBitmap = overlayBmp,
            overlayBoundsNSEW = doubleArrayOf(23.811, 23.809, 90.413, 90.411)
        )
        overlayBmp.recycle()
        val kmzParcels = com.example.engine.SpatialParcelEngine.parseKmz(kmzBytes)
        assertEquals(1, kmzParcels.size)
        assertEquals("KMZ", kmzParcels.first().sourceFormat)

        val zipEntries = mutableListOf<String>()
        var docKmlText = ""
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(kmzBytes)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                zipEntries.add(e.name)
                if (e.name == "doc.kml") docKmlText = String(zis.readBytes(), java.nio.charset.StandardCharsets.UTF_8)
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        assertTrue("files/mouza_overlay.png" in zipEntries)
        assertTrue(docKmlText.contains("<GroundOverlay>") && docKmlText.contains("পুবাইল"))

        // 3. Road setback & DD/DMS/UTM/BTM coordinates
        val setbackFt = com.example.engine.SpatialParcelEngine.computeRoadSetbackFt(
            plotPointsLatLon = listOf(23.8103 to 90.4125),
            roadALatLon = 23.8100 to 90.4120,
            roadBLatLon = 23.8100 to 90.4130
        )
        assertTrue("Expected ~109 ft setback, got $setbackFt", setbackFt in 95.0..125.0)

        val conv = com.example.engine.SpatialParcelEngine.convertCoordinates(23.8103, 90.4125)
        assertEquals(46, conv.utmZone)
        assertTrue(conv.dmsLat.endsWith("N") && conv.dmsLon.endsWith("E"))
        assertTrue(conv.btmEasting in 530000.0..555000.0)
        assertTrue(conv.btmNorthing in 620000.0..650000.0)

        // 4. HomeModuleCatalog & AppPreferences check
        val modules = com.example.ui.screens.HomeModuleCatalog.allModules
        assertEquals(52, modules.size)
        assertEquals(52, modules.map { it.id }.distinct().size)
        assertEquals(52, modules.map { it.testTag }.distinct().size)
        assertTrue(modules.none { it.titleBn.contains("PC সার্ভার চালু") && it.route !is com.example.model.AboutRoute })

        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.AppPreferences(context)
        prefs.togglePinnedTool("mod_cogo_traverse")
        assertTrue("mod_cogo_traverse" in prefs.pinnedToolIdsFlow.first())
        val updatedSettings = com.example.model.GlobalSurveySettings(decimalPrecision = 3, useBanglaDigits = false)
        prefs.saveGlobalSettings(updatedSettings)
        assertEquals(3, prefs.globalSettingsFlow.first().decimalPrecision)
    }

    @Test
    fun phase2_m06_landDivisionProEngine_cutsAndQuadMaster() {
        val square = listOf(
            com.example.model.PolygonVertex(0.0, 0.0),
            com.example.model.PolygonVertex(100.0, 0.0),
            com.example.model.PolygonVertex(100.0, 100.0),
            com.example.model.PolygonVertex(0.0, 100.0)
        )
        val shares = listOf(
            com.example.model.DivisionShareSpec("মালিক ১", 0.5),
            com.example.model.DivisionShareSpec("মালিক ২", 0.5)
        )
        val report = com.example.engine.LandDivisionProEngine.dividePolygonPro(
            id = "div_1",
            title = "Test Division",
            vertices = square,
            shares = shares,
            roadWidthFt = 10.0
        )
        assertNotNull(report)
        assertEquals(2, report!!.parcels.size)
        val totalNet = report!!.parcels.sumOf { it.netAreaSqFt }
        assertTrue("Total net area ($totalNet) should be close to 9000 sq ft", totalNet in 8900.0..9100.0)

        // QuadMaster
        val quadSpec = com.example.model.QuadMasterSpec(
            northFt = 100.0,
            southFt = 100.0,
            eastFt = 100.0,
            westFt = 100.0,
            useDiagonalMode = true,
            diagonalAcFt = 141.42
        )
        val quad = com.example.engine.LandDivisionProEngine.solveAndSplitQuad(quadSpec, partsCount = 2)
        assertNotNull(quad)
        assertTrue("Quad area should be ~10000 sq ft", quad!!.totalSqFt in 9950.0..10050.0)

        // Plot Planner
        val config = com.example.model.PlotPlannerOptionConfig(
            optionLabel = "Option A",
            targetPlotKatha = 3.0,
            roadWidthFt = 15.0,
            drainOpenSpacePct = 8.0
        )
        val planner = com.example.engine.LandDivisionProEngine.evaluatePlotPlannerOption(square, config)
        assertTrue("Plot count should be > 0", planner.plotCount > 0)
    }

    @Test
    fun phase2_m07_inheritanceAndHissaProEngine_sunniAndAna() {
        // 16-ana breakdown
        val breakdown = com.example.engine.InheritanceAndHissaProEngine.ratioToAnaGondaBreakdown(0.25)
        assertEquals(4, breakdown.ana)

        // Hanafi Faraiz: Husband + 1 Son + 1 Daughter
        val input = com.example.model.HanafiHeirInput(
            totalDecimal = 100.0,
            deceasedIsMale = false,
            spouseCount = 1,
            sons = 1,
            daughters = 1
        )
        val faraiz = com.example.engine.InheritanceAndHissaProEngine.calculateHanafiFaraiz(input)
        assertTrue(faraiz.shares.isNotEmpty())

        // Unknown side solver
        val unknown = com.example.engine.InheritanceAndHissaProEngine.solveUnknownSide(
            com.example.model.UnknownSideShape.RECTANGLE,
            5000.0, 100.0, 0.0, 0.0, 0.0
        )
        assertEquals(50.0, unknown.solvedValue, 0.01)
    }

    @Test
    fun phase2_m08_khatianProEngine_validationAndCompare() {
        val doc = com.example.model.CustomKhatianDocument(
            id = "test_kh_1",
            khatianNumber = "412",
            mouzaName = "পুবাইল",
            owners = listOf(
                com.example.model.KhatianOwnerEntry("o1", "আব্দুস সালাম", "", "", "০.৫০০", 0.5),
                com.example.model.KhatianOwnerEntry("o2", "মোঃ রফিকুল ইসলাম", "", "", "০.৫০০", 0.5)
            ),
            dags = listOf(
                com.example.model.KhatianDagEntry("d1", "1042", "নাল", 50.0, 1.0, 50.0)
            )
        )
        val analysis = com.example.engine.KhatianProEngine.analyzeKhatianOneClick(doc)
        assertTrue("16-ana should be exact when sum is 1.0", analysis.isSixteenAnaExact)
        assertEquals(50.0, analysis.totalDagDecimal, 0.001)
        assertEquals(2, analysis.owners.size)

        // 3-Way Area comparison
        val threeWay = com.example.engine.KhatianProEngine.compareThreeWayAreas(
            mapDecimal = 25.0,
            khatianDecimal = 24.8,
            fieldDecimal = 24.9,
            tolerancePct = 2.0
        )
        assertTrue("Areas within 2% tolerance must pass", threeWay.withinTolerance)

        // Dag Index search
        val index = com.example.engine.KhatianProEngine.buildDagCrossReferenceIndex(listOf(doc), "1042")
        assertEquals(1, index.size)
        assertEquals("1042", index[0].dagNumber)
        assertEquals("412", index[0].khatianNumber)

        // Dag Overlap & Encroachment
        val p1 = listOf(
            com.example.model.PolygonVertex(0.0, 0.0),
            com.example.model.PolygonVertex(100.0, 0.0),
            com.example.model.PolygonVertex(100.0, 100.0),
            com.example.model.PolygonVertex(0.0, 100.0)
        )
        val p2 = listOf(
            com.example.model.PolygonVertex(20.0, 0.0),
            com.example.model.PolygonVertex(120.0, 0.0),
            com.example.model.PolygonVertex(120.0, 100.0),
            com.example.model.PolygonVertex(20.0, 100.0)
        )
        val overlap = com.example.engine.KhatianProEngine.analyzeDagOverlapAndEncroachment(
            formerLabel = "সাবেক ১০৪২",
            currentLabel = "হাল ২১২৫",
            formerPolygon = p1,
            currentPolygon = p2
        )
        assertEquals(8000.0, overlap.overlapAreaSqFt, 100.0)
        assertEquals(2000.0, overlap.encroachmentIntoCurrentSqFt, 100.0)
    }
}
