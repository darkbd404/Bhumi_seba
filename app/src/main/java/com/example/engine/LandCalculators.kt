package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

data class LandAreaResult(
    val sqFeet: Double,
    val sqMeters: Double,
    val decimal: Double,     // শতাংশ (1 Decimal = 435.6 sq ft)
    val ajutangsho: Double,  // অযুতাংশ (1 Decimal = 100 Ajutangsho)
    val katha: Double,       // কাঠা (Standard BD = 720 sq ft)
    val bigha: Double,       // বিঘা (20 Katha = 14,400 sq ft)
    val acre: Double,        // একর (100 Decimal = 43,560 sq ft)
    val hectare: Double,     // হেক্টর (247.105 Decimal)
    val sqHat: Double,       // বর্গহাত (1 Hat = 1.5 ft -> 2.25 sq ft)
    val gonda: Double,       // গণ্ডা (Noakhali/Chattogram/Sylhet standard 1 Kani = 20 Gonda; 864 sq ft per Gonda in 120-decimal Kani, or 345.6 in 40-dec Kani — labeled clearly)
)

object LandCalculators {
    const val SQ_FT_PER_DECIMAL = 435.6
    const val SQ_FT_PER_KATHA_BD = 720.0
    const val SQ_FT_PER_BIGHA_BD = 14400.0
    const val SQ_FT_PER_ACRE = 43560.0
    const val SQ_FT_PER_SQ_METER = 10.7639104167
    const val SQ_FT_PER_HECTARE = 107639.104167
    const val SQ_FT_PER_SQ_HAT = 2.25 // 18 inch = 1.5 ft -> 1.5*1.5 = 2.25 sq ft
    const val SQ_FT_PER_GUNTER_LINK_SQ = 0.4356 // 1 link = 0.66 ft

    fun fromSqFeet(sqFeet: Double): LandAreaResult {
        val safe = sqFeet.coerceAtLeast(0.0)
        val decimal = safe / SQ_FT_PER_DECIMAL
        return LandAreaResult(
            sqFeet = safe,
            sqMeters = safe / SQ_FT_PER_SQ_METER,
            decimal = decimal,
            ajutangsho = decimal * 100.0,
            katha = safe / SQ_FT_PER_KATHA_BD,
            bigha = safe / SQ_FT_PER_BIGHA_BD,
            acre = safe / SQ_FT_PER_ACRE,
            hectare = safe / SQ_FT_PER_HECTARE,
            sqHat = safe / SQ_FT_PER_SQ_HAT,
            gonda = decimal / 2.0 // Standard 40-decimal Kani (1 Gonda = 2 Decimal)
        )
    }

    fun fromDecimal(decimal: Double): LandAreaResult = fromSqFeet(decimal * SQ_FT_PER_DECIMAL)
    fun fromKatha(katha: Double): LandAreaResult = fromSqFeet(katha * SQ_FT_PER_KATHA_BD)
    fun fromBigha(bigha: Double): LandAreaResult = fromSqFeet(bigha * SQ_FT_PER_BIGHA_BD)
    fun fromAcre(acre: Double): LandAreaResult = fromSqFeet(acre * SQ_FT_PER_ACRE)
    fun fromSqMeters(sqMeters: Double): LandAreaResult = fromSqFeet(sqMeters * SQ_FT_PER_SQ_METER)

    /**
     * Triangle area by Heron's formula (sides in feet). Returns null if triangle inequality fails.
     */
    fun triangleHeronSqFeet(a: Double, b: Double, c: Double): Double? {
        if (a <= 0 || b <= 0 || c <= 0) return null
        if (a + b <= c || a + c <= b || b + c <= a) return null
        val s = (a + b + c) / 2.0
        return sqrt(s * (s - a) * (s - b) * (s - c))
    }

    /**
     * Exact Quadrilateral area using 4 sides + 1 diagonal (splits into 2 Heron triangles).
     * If diagonal is null or <= 0, falls back to Bangladesh Amin Average Method: ((a+c)/2) * ((b+d)/2).
     */
    fun fourSideSurveySqFeet(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        diagonal: Double? = null
    ): Pair<Double, Boolean> {
        if (diagonal != null && diagonal > 0) {
            val t1 = triangleHeronSqFeet(north, east, diagonal)
            val t2 = triangleHeronSqFeet(south, west, diagonal)
            if (t1 != null && t2 != null) {
                return (t1 + t2) to true
            }
        }
        val avgLength = (north + south) / 2.0
        val avgWidth = (east + west) / 2.0
        return (avgLength * avgWidth) to false
    }

    fun circleAreaSqFeet(radiusFeet: Double): Double {
        if (radiusFeet <= 0) return 0.0
        return PI * radiusFeet * radiusFeet
    }

    fun trapezoidAreaSqFeet(parallelA: Double, parallelB: Double, height: Double): Double {
        if (parallelA < 0 || parallelB < 0 || height <= 0) return 0.0
        return ((parallelA + parallelB) / 2.0) * height
    }

    /**
     * Traditional Khatian Share (আনা - গণ্ডা - কড়া - ক্রান্তি - তিল) to Decimal Ratio (0.0 .. 1.0)
     * 16 Ana = 1 Whole (1.0)
     * 1 Ana = 20 Gonda
     * 1 Gonda = 4 Kora
     * 1 Kora = 3 Kranti
     * 1 Kranti = 20 Til
     * Total Til in 16 Ana = 16 * 20 * 4 * 3 * 20 = 76,800 Til
     */
    fun hissaToRatio(
        ana: Int,
        gonda: Int,
        kora: Int,
        kranti: Int,
        til: Int
    ): Double {
        val totalTil = (ana * 20.0 * 4.0 * 3.0 * 20.0) +
            (gonda * 4.0 * 3.0 * 20.0) +
            (kora * 3.0 * 20.0) +
            (kranti * 20.0) +
            til
        return (totalTil / 76800.0).coerceIn(0.0, 1.0)
    }

    data class FaraizShare(
        val relationBn: String,
        val totalFraction: Double,
        val perPersonFraction: Double,
        val count: Int,
        val totalDecimal: Double,
        val perPersonDecimal: Double
    )

    /**
     * Simplified authentic Sunni Muslim Inheritance (Faraiz) land distribution calculator.
     */
    fun calculateFaraiz(
        totalDecimal: Double,
        deceasedIsMale: Boolean,
        hasSpouse: Boolean,
        sons: Int,
        daughters: Int,
        hasFather: Boolean,
        hasMother: Boolean
    ): List<FaraizShare> {
        if (totalDecimal <= 0.0) return emptyList()
        val hasChildren = sons > 0 || daughters > 0

        var spouseShare = 0.0
        if (hasSpouse) {
            spouseShare = if (deceasedIsMale) {
                if (hasChildren) 1.0 / 8.0 else 1.0 / 4.0
            } else {
                if (hasChildren) 1.0 / 4.0 else 1.0 / 2.0
            }
        }

        val motherShare = if (hasMother) {
            if (hasChildren) 1.0 / 6.0 else 1.0 / 3.0
        } else 0.0

        val fatherFixedShare = if (hasFather && hasChildren) 1.0 / 6.0 else 0.0

        val fixedSum = spouseShare + motherShare + fatherFixedShare
        var remainder = (1.0 - fixedSum).coerceAtLeast(0.0)

        val result = mutableListOf<FaraizShare>()
        if (hasSpouse && spouseShare > 0) {
            val label = if (deceasedIsMale) "স্ত্রী (Wife)" else "স্বামী (Husband)"
            result.add(
                FaraizShare(
                    relationBn = label,
                    totalFraction = spouseShare,
                    perPersonFraction = spouseShare,
                    count = 1,
                    totalDecimal = totalDecimal * spouseShare,
                    perPersonDecimal = totalDecimal * spouseShare
                )
            )
        }
        if (hasMother && motherShare > 0) {
            result.add(
                FaraizShare(
                    relationBn = "মাতা (Mother)",
                    totalFraction = motherShare,
                    perPersonFraction = motherShare,
                    count = 1,
                    totalDecimal = totalDecimal * motherShare,
                    perPersonDecimal = totalDecimal * motherShare
                )
            )
        }

        if (sons > 0) {
            // Sons + Daughters share remainder in 2:1 ratio
            val units = sons * 2 + daughters
            val sonTotal = remainder * (sons * 2.0 / units)
            val daughterTotal = remainder * (daughters.toDouble() / units)
            if (hasFather && fatherFixedShare > 0) {
                result.add(
                    FaraizShare(
                        relationBn = "পিতা (Father)",
                        totalFraction = fatherFixedShare,
                        perPersonFraction = fatherFixedShare,
                        count = 1,
                        totalDecimal = totalDecimal * fatherFixedShare,
                        perPersonDecimal = totalDecimal * fatherFixedShare
                    )
                )
            }
            result.add(
                FaraizShare(
                    relationBn = "পুত্র ($sons জন)",
                    totalFraction = sonTotal,
                    perPersonFraction = sonTotal / sons,
                    count = sons,
                    totalDecimal = totalDecimal * sonTotal,
                    perPersonDecimal = (totalDecimal * sonTotal) / sons
                )
            )
            if (daughters > 0) {
                result.add(
                    FaraizShare(
                        relationBn = "কন্যা ($daughters জন)",
                        totalFraction = daughterTotal,
                        perPersonFraction = daughterTotal / daughters,
                        count = daughters,
                        totalDecimal = totalDecimal * daughterTotal,
                        perPersonDecimal = (totalDecimal * daughterTotal) / daughters
                    )
                )
            }
        } else if (daughters > 0) {
            // Only daughters (no sons): 1 daughter = 1/2, 2+ daughters = 2/3
            val daughtersTarget = if (daughters == 1) 0.5 else 2.0 / 3.0
            val actualDaughters = minOf(daughtersTarget, remainder)
            remainder -= actualDaughters
            result.add(
                FaraizShare(
                    relationBn = "কন্যা ($daughters জন)",
                    totalFraction = actualDaughters,
                    perPersonFraction = actualDaughters / daughters,
                    count = daughters,
                    totalDecimal = totalDecimal * actualDaughters,
                    perPersonDecimal = (totalDecimal * actualDaughters) / daughters
                )
            )
            if (hasFather) {
                val fatherTotal = fatherFixedShare + remainder
                result.add(
                    FaraizShare(
                        relationBn = "পিতা (Father - অংশ + আসাবা)",
                        totalFraction = fatherTotal,
                        perPersonFraction = fatherTotal,
                        count = 1,
                        totalDecimal = totalDecimal * fatherTotal,
                        perPersonDecimal = totalDecimal * fatherTotal
                    )
                )
            }
        } else {
            // No children
            if (hasFather) {
                result.add(
                    FaraizShare(
                        relationBn = "পিতা (Father - অবশিষ্টভোগী)",
                        totalFraction = remainder,
                        perPersonFraction = remainder,
                        count = 1,
                        totalDecimal = totalDecimal * remainder,
                        perPersonDecimal = totalDecimal * remainder
                    )
                )
            }
        }

        // Normalize if total fraction < 1 (Radd when no residuary)
        val totalAllocated = result.sumOf { it.totalFraction }
        if (totalAllocated > 0 && abs(totalAllocated - 1.0) > 0.001 && !hasFather && sons == 0) {
            // Apply Radd proportionately to non-spouse heirs or all if no spouse
            val scale = 1.0 / totalAllocated
            return result.map {
                val adjFrac = it.totalFraction * scale
                it.copy(
                    totalFraction = adjFrac,
                    perPersonFraction = adjFrac / it.count,
                    totalDecimal = totalDecimal * adjFrac,
                    perPersonDecimal = (totalDecimal * adjFrac) / it.count
                )
            }
        }
        return result
    }

    data class DeedFeeBreakdown(
        val deedValueBdt: Double,
        val stampDuty: Double,         // 1.5%
        val registrationFee: Double,   // 1.0%
        val localGovtTax: Double,      // 3.0% (Union/Upazila) or 2.0% (City/Pourashava)
        val sourceGainTax: Double,     // Est. 4% - 8% (user selectable)
        val totalGovtCost: Double
    )

    fun estimateDeedCost(
        deedValueBdt: Double,
        isCityArea: Boolean,
        gainTaxPercent: Double
    ): DeedFeeBreakdown {
        val safeVal = deedValueBdt.coerceAtLeast(0.0)
        val stamp = safeVal * 0.015
        val regFee = safeVal * 0.01
        val localTax = safeVal * (if (isCityArea) 0.02 else 0.03)
        val gainTax = safeVal * (gainTaxPercent / 100.0)
        return DeedFeeBreakdown(
            deedValueBdt = safeVal,
            stampDuty = stamp,
            registrationFee = regFee,
            localGovtTax = localTax,
            sourceGainTax = gainTax,
            totalGovtCost = stamp + regFee + localTax + gainTax
        )
    }
}
