package com.example

import androidx.compose.ui.graphics.Color
import com.example.engine.LandCalculators
import com.example.engine.MouzaPdfEngine
import com.example.engine.PlotWallIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun landUnitConversions_areAccurate() {
        val oneAcre = LandCalculators.fromAcre(1.0)
        assertEquals(100.0, oneAcre.decimal, 0.001)
        assertEquals(43560.0, oneAcre.sqFeet, 0.01)
        assertEquals(60.5, oneAcre.katha, 0.001)
        assertEquals(3.025, oneAcre.bigha, 0.001)
    }

    @Test
    fun hissaCalculator_eightAnaIsHalf() {
        val ratio = LandCalculators.hissaToRatio(ana = 8, gonda = 0, kora = 0, kranti = 0, til = 0)
        assertEquals(0.5, ratio, 0.0001)
    }

    @Test
    fun faraizCalculator_allocatesFullEstate() {
        val shares = LandCalculators.calculateFaraiz(
            totalDecimal = 100.0,
            deceasedIsMale = true,
            hasSpouse = true,
            sons = 2,
            daughters = 1,
            hasFather = false,
            hasMother = true
        )
        val sum = shares.sumOf { it.totalDecimal }
        assertEquals(100.0, sum, 0.01)
        assertTrue(shares.any { it.relationBn.contains("পুত্র") })
    }

    @Test
    fun plotWallIndex_selectsExactPlotWithoutLeaking() {
        val w = 200
        val h = 200
        val wall = ByteArray(w * h)
        // Draw two sealed plot boxes: Plot 1 (20..100, 20..180) and Plot 2 (100..180, 20..180)
        for (x in 20..180) {
            wall[20 * w + x] = 1
            wall[180 * w + x] = 1
        }
        for (y in 20..180) {
            wall[y * w + 20] = 1
            wall[y * w + 100] = 1
            wall[y * w + 180] = 1
        }

        val index = PlotWallIndex(w, h, wall)
        val res = index.selectPlotAt(
            normX = 0.30f, // x = 60 (inside Plot 1)
            normY = 0.50f, // y = 100
            plotId = 1,
            plotIndex = 1,
            color = Color.Green,
            existingPlots = emptyList()
        )
        assertTrue(res is PlotWallIndex.TapResult.Success)
        val plot = (res as PlotWallIndex.TapResult.Success).plot
        // Interior of Plot 1 is (21..99) x (21..179) = 79 * 159 = 12,561 pixels
        assertEquals(79 * 159, plot.pixelCount)
    }

    @Test
    fun manualPolygonBuilder_computesPolygonInterior() {
        val poly = listOf(
            0.1f to 0.1f,
            0.5f to 0.1f,
            0.5f to 0.5f,
            0.1f to 0.5f
        )
        val plot = MouzaPdfEngine.buildManualPolygonPlot(
            normalizedPoints = poly,
            gridWidth = 100,
            gridHeight = 100,
            plotId = 1,
            plotIndex = 1,
            color = Color.Blue
        )
        // 40x40 square in 100x100 grid (~1600 px)
        assertTrue(plot.pixelCount in 1500..1700)
    }

    @Test
    fun lengthConverter_chainLinkFeetMatchesSurveyStandard() {
        val oneChain = LandCalculators.convertLength(1.0, com.example.engine.LengthUnit.CHAIN)
        assertEquals(66.0, oneChain[com.example.engine.LengthUnit.FEET] ?: 0.0, 0.001)
        assertEquals(100.0, oneChain[com.example.engine.LengthUnit.LINK] ?: 0.0, 0.001)
        assertEquals(22.0, oneChain[com.example.engine.LengthUnit.YARD] ?: 0.0, 0.001)
    }

    @Test
    fun mouzaPolygonAreaAndDistance_matchExactGeometry() {
        // 720 pts = 10 inches; at SCALE_16 (330 ft/inch) => 3300 x 3300 ft
        val square = listOf(
            0.0f to 0.0f,
            0.1f to 0.0f,
            0.1f to 0.1f,
            0.0f to 0.1f
        )
        val areaSqFt = MouzaPdfEngine.computePolygonAreaSqFeet(
            polygonNorm = square,
            pageWidthPts = 720,
            pageHeightPts = 720,
            scale = com.example.model.MouzaScale.SCALE_16
        )
        // Side is 1 inch = 330 ft => area = 330 * 330 = 108,900 sq ft
        assertEquals(108900.0, areaSqFt, 1.0)

        val sideFt = MouzaPdfEngine.computeDistanceFeet(
            p1Norm = 0.0f to 0.0f,
            p2Norm = 0.1f to 0.0f,
            pageWidthPts = 720,
            pageHeightPts = 720,
            scale = com.example.model.MouzaScale.SCALE_16
        )
        assertEquals(330.0, sideFt, 0.1)
    }
}
