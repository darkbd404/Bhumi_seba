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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        assertEquals("Bhumi Survey BD", appName)
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
}

