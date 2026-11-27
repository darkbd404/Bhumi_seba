package com.example.engine

import com.example.model.FinancialRateConfig
import com.example.model.InheritanceCalculationReport
import com.example.model.InheritanceLawType
import com.example.model.PolygonVertex
import com.example.model.RegionalUnitProfile
import com.example.model.UniversalInheritanceShare
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RegionalAreaBreakdown(
    val profile: RegionalUnitProfile,
    val sqFeet: Double,
    val sqMeters: Double,
    val decimal: Double,
    val ajutangsho: Double,
    val katha: Double,
    val bigha: Double,
    val acre: Double,
    val hectare: Double,
    val kani: Double,
    val gonda: Double,
    val kora: Double,
    val kranti: Double,
    val til: Double,
    val customUnitValue: Double
)

data class TraverseLegInput(
    val label: String,
    val bearingDeg: Double,
    val distanceFt: Double
)

data class TraverseLegResult(
    val label: String,
    val bearingDeg: Double,
    val rawDistanceFt: Double,
    val rawDeltaN: Double,
    val rawDeltaE: Double,
    val adjustedN: Double,
    val adjustedE: Double
)

data class ClosedTraverseResult(
    val totalPerimeterFt: Double,
    val closingErrorN: Double,
    val closingErrorE: Double,
    val linearMisclosureFt: Double,
    val precisionRatio: String,
    val adjustedAreaSqFt: Double,
    val adjustedDecimal: Double,
    val legs: List<TraverseLegResult>,
    val vertices: List<PolygonVertex>
)

data class CoordinateGeometryResult(
    val sqFeet: Double,
    val decimal: Double,
    val perimeterFt: Double,
    val edgeLengthsFt: List<Double>,
    val centroid: PolygonVertex
)

data class FinanceValuationSummary(
    val mouzaTotalValueBdt: Double,
    val stampDutyBdt: Double,
    val registrationFeeBdt: Double,
    val localGovtTaxBdt: Double,
    val gainTaxBdt: Double,
    val mutationCostBdt: Double,
    val annualLandTaxBdt: Double,
    val totalGovtAndProcessCostBdt: Double,
    val totalAcquisitionCostBdt: Double,
    val netProfitOrLossBdt: Double,
    val perShareCostBdt: Double
)

object SurveyMathProEngine {

    val DEFAULT_REGIONAL_PROFILES = listOf(
        RegionalUnitProfile(
            id = "bd_standard",
            regionNameBn = "ঢাকা / বাংলাদেশ স্ট্যান্ডার্ড (১৬৫ অযুতাংশ কাঠা)",
            regionNameEn = "Standard Bangladesh (720 sq.ft Katha)",
            kathaSqFt = 720.0,
            bighaSqFt = 14400.0,
            kaniSqFt = 17424.0,
            gondaSqFt = 871.2,
            koraSqFt = 217.8,
            krantiSqFt = 72.6,
            tilSqFt = 3.63,
            customUnitName = "বর্গহাত",
            customUnitSqFt = 2.25,
            isDefault = true
        ),
        RegionalUnitProfile(
            id = "chattogram_40",
            regionNameBn = "চট্টগ্রাম / কক্সবাজার (৪০ শতকে কানি)",
            regionNameEn = "Chattogram / Cox's Bazar (40 Dec Kani)",
            kathaSqFt = 720.0,
            bighaSqFt = 14400.0,
            kaniSqFt = 40.0 * 435.6,      // 17,424 sq ft
            gondaSqFt = (40.0 * 435.6) / 20.0, // 2 decimal = 871.2 sq ft
            koraSqFt = (40.0 * 435.6) / 80.0,
            krantiSqFt = (40.0 * 435.6) / 240.0,
            tilSqFt = (40.0 * 435.6) / 4800.0,
            customUnitName = "কানি (৪০ শতক)",
            customUnitSqFt = 40.0 * 435.6
        ),
        RegionalUnitProfile(
            id = "noakhali_shahi_120",
            regionNameBn = "নোয়াখালী / ফেনী / কুমিল্লা শাহী (১২০ শতকে কানি)",
            regionNameEn = "Noakhali / Feni Shahi Kani (120 Dec)",
            kathaSqFt = 720.0,
            bighaSqFt = 14400.0,
            kaniSqFt = 120.0 * 435.6,     // 52,272 sq ft
            gondaSqFt = (120.0 * 435.6) / 20.0, // 6 decimal = 2,613.6 sq ft
            koraSqFt = (120.0 * 435.6) / 80.0,
            krantiSqFt = (120.0 * 435.6) / 240.0,
            tilSqFt = (120.0 * 435.6) / 4800.0,
            customUnitName = "শাহী কানি (১২০ শতক)",
            customUnitSqFt = 120.0 * 435.6
        ),
        RegionalUnitProfile(
            id = "sylhet_keyar",
            regionNameBn = "সিলেট অঞ্চল (৩০ শতকে কেয়ার / ৩৬০ শতকে হাল)",
            regionNameEn = "Sylhet (30 Dec Keyar / 360 Dec Hal)",
            kathaSqFt = 720.0,
            bighaSqFt = 14400.0,
            kaniSqFt = 30.0 * 435.6,      // 1 Keyar = 30 Decimal
            gondaSqFt = (30.0 * 435.6) / 20.0,
            koraSqFt = (30.0 * 435.6) / 80.0,
            krantiSqFt = (30.0 * 435.6) / 240.0,
            tilSqFt = (30.0 * 435.6) / 4800.0,
            customUnitName = "হাল (১২ কেয়ার = ৩৬০ শতক)",
            customUnitSqFt = 360.0 * 435.6
        ),
        RegionalUnitProfile(
            id = "dinajpur_north_katha",
            regionNameBn = "উত্তরবঙ্গ / রাজশাহী আঞ্চলিক (১৬২০ বর্গফুটে কাঠা)",
            regionNameEn = "Northern BD Local (1620 sq.ft Katha)",
            kathaSqFt = 1620.0,
            bighaSqFt = 32400.0,
            kaniSqFt = 17424.0,
            gondaSqFt = 871.2,
            koraSqFt = 217.8,
            krantiSqFt = 72.6,
            tilSqFt = 3.63,
            customUnitName = "স্থানীয় কাঠা (১৬২০ ব.ফু)",
            customUnitSqFt = 1620.0
        )
    )

    fun convertSqFeetWithProfile(sqFeet: Double, profile: RegionalUnitProfile): RegionalAreaBreakdown {
        val safe = sqFeet.coerceAtLeast(0.0)
        val dec = safe / LandCalculators.SQ_FT_PER_DECIMAL
        return RegionalAreaBreakdown(
            profile = profile,
            sqFeet = safe,
            sqMeters = safe / LandCalculators.SQ_FT_PER_SQ_METER,
            decimal = dec,
            ajutangsho = dec * 100.0,
            katha = if (profile.kathaSqFt > 0) safe / profile.kathaSqFt else 0.0,
            bigha = if (profile.bighaSqFt > 0) safe / profile.bighaSqFt else 0.0,
            acre = safe / LandCalculators.SQ_FT_PER_ACRE,
            hectare = safe / LandCalculators.SQ_FT_PER_HECTARE,
            kani = if (profile.kaniSqFt > 0) safe / profile.kaniSqFt else 0.0,
            gonda = if (profile.gondaSqFt > 0) safe / profile.gondaSqFt else 0.0,
            kora = if (profile.koraSqFt > 0) safe / profile.koraSqFt else 0.0,
            kranti = if (profile.krantiSqFt > 0) safe / profile.krantiSqFt else 0.0,
            til = if (profile.tilSqFt > 0) safe / profile.tilSqFt else 0.0,
            customUnitValue = if (profile.customUnitSqFt > 0) safe / profile.customUnitSqFt else 0.0
        )
    }

    /** Law of Cosines: c = sqrt(a^2 + b^2 - 2ab*cos(C)), plus exact Heron/SAS area */
    fun solveTriangleSideAndArea(sideA: Double, sideB: Double, angleDeg: Double): Triple<Double, Double, Double>? {
        if (sideA <= 0.0 || sideB <= 0.0 || angleDeg <= 0.0 || angleDeg >= 180.0) return null
        val rad = Math.toRadians(angleDeg)
        val sideC = sqrt((sideA * sideA + sideB * sideB - 2.0 * sideA * sideB * cos(rad)).coerceAtLeast(0.0))
        val areaSqFt = 0.5 * sideA * sideB * sin(rad)
        val perimeter = sideA + sideB + sideC
        return Triple(sideC, areaSqFt, perimeter)
    }

    /** Parallelogram area = base * height (or sideA * sideB * sin(angle)) */
    fun parallelogramSqFeet(base: Double, height: Double): Double {
        if (base <= 0.0 || height <= 0.0) return 0.0
        return base * height
    }

    /** Coordinate Geometry (Shoelace Formula) on planar (X, Y) coordinates in feet */
    fun computeCoordinateGeometry(vertices: List<PolygonVertex>): CoordinateGeometryResult? {
        if (vertices.size < 3) return null
        var sum = 0.0
        var cx = 0.0
        var cy = 0.0
        var perim = 0.0
        val edges = ArrayList<Double>(vertices.size)
        val n = vertices.size
        for (i in 0 until n) {
            val p1 = vertices[i]
            val p2 = vertices[(i + 1) % n]
            val cross = (p1.x * p2.y) - (p2.x * p1.y)
            sum += cross
            cx += (p1.x + p2.x) * cross
            cy += (p1.y + p2.y) * cross
            val edge = hypot(p2.x - p1.x, p2.y - p1.y)
            edges.add(edge)
            perim += edge
        }
        val signedArea = sum * 0.5
        val area = abs(signedArea)
        val centroid = if (abs(signedArea) > 1e-6) {
            PolygonVertex(cx / (6.0 * signedArea), cy / (6.0 * signedArea))
        } else {
            PolygonVertex(vertices.map { it.x }.average(), vertices.map { it.y }.average())
        }
        return CoordinateGeometryResult(
            sqFeet = area,
            decimal = area / LandCalculators.SQ_FT_PER_DECIMAL,
            perimeterFt = perim,
            edgeLengthsFt = edges,
            centroid = centroid
        )
    }

    /** Bowditch Compass-Rule Closed Traverse Adjustment */
    fun solveClosedTraverse(legs: List<TraverseLegInput>): ClosedTraverseResult? {
        if (legs.size < 3 || legs.any { it.distanceFt <= 0.0 }) return null
        val totalPerim = legs.sumOf { it.distanceFt }
        if (totalPerim <= 0.0) return null

        val rawDeltas = legs.map { leg ->
            val rad = Math.toRadians(leg.bearingDeg)
            val dN = leg.distanceFt * cos(rad)
            val dE = leg.distanceFt * sin(rad)
            dN to dE
        }
        val errN = rawDeltas.sumOf { it.first }
        val errE = rawDeltas.sumOf { it.second }
        val misclosure = hypot(errN, errE)
        val ratioDenom = if (misclosure > 1e-6) (totalPerim / misclosure).toLong() else 999_999L
        val precisionStr = if (misclosure <= 1e-6) "1 : ∞ (নিখুঁত)" else "1 : $ratioDenom"

        var curN = 0.0
        var curE = 0.0
        val vertices = mutableListOf(PolygonVertex(0.0, 0.0))
        val legResults = mutableListOf<TraverseLegResult>()

        legs.forEachIndexed { idx, leg ->
            val (rawN, rawE) = rawDeltas[idx]
            val weight = leg.distanceFt / totalPerim
            val adjN = rawN - (errN * weight)
            val adjE = rawE - (errE * weight)
            curN += adjN
            curE += adjE
            if (idx < legs.lastIndex) {
                vertices.add(PolygonVertex(curE, curN))
            }
            legResults.add(
                TraverseLegResult(
                    label = leg.label,
                    bearingDeg = leg.bearingDeg,
                    rawDistanceFt = leg.distanceFt,
                    rawDeltaN = rawN,
                    rawDeltaE = rawE,
                    adjustedN = adjN,
                    adjustedE = adjE
                )
            )
        }

        val coordRes = computeCoordinateGeometry(vertices) ?: return null
        return ClosedTraverseResult(
            totalPerimeterFt = totalPerim,
            closingErrorN = errN,
            closingErrorE = errE,
            linearMisclosureFt = misclosure,
            precisionRatio = precisionStr,
            adjustedAreaSqFt = coordRes.sqFeet,
            adjustedDecimal = coordRes.decimal,
            legs = legResults,
            vertices = vertices
        )
    }

    /** Multi-Law Configurable Inheritance Calculator for Bangladesh */
    fun calculateUniversalInheritance(
        lawType: InheritanceLawType,
        totalDecimal: Double,
        deceasedIsMale: Boolean,
        hasSpouse: Boolean,
        sons: Int,
        daughters: Int,
        hasFather: Boolean,
        hasMother: Boolean,
        brothers: Int = 0,
        sisters: Int = 0
    ): InheritanceCalculationReport {
        val safeArea = totalDecimal.coerceAtLeast(0.0)
        val warnings = mutableListOf<String>()
        if (safeArea <= 0.0) {
            return InheritanceCalculationReport(
                lawType = lawType,
                lawVersion = "BD-RuleSet-2026.1",
                totalDecimal = 0.0,
                shares = emptyList(),
                warnings = listOf("জমির পরিমাণ ০-এর বেশি দিন")
            )
        }

        val shares = mutableListOf<UniversalInheritanceShare>()
        var legalReview = false

        when (lawType) {
            InheritanceLawType.MUSLIM_SUNNI -> {
                val baseShares = LandCalculators.calculateFaraiz(
                    totalDecimal = safeArea,
                    deceasedIsMale = deceasedIsMale,
                    hasSpouse = hasSpouse,
                    sons = sons,
                    daughters = daughters,
                    hasFather = hasFather,
                    hasMother = hasMother
                )
                baseShares.forEach {
                    shares.add(
                        UniversalInheritanceShare(
                            relationBn = it.relationBn,
                            totalFraction = it.totalFraction,
                            perPersonFraction = it.perPersonFraction,
                            count = it.count,
                            totalDecimal = it.totalDecimal,
                            perPersonDecimal = it.perPersonDecimal,
                            statusNote = "হানাফি ফারায়েজ নিয়ম"
                        )
                    )
                }
                val allocatedFrac = shares.sumOf { it.totalFraction }
                val remFrac = (1.0 - allocatedFrac).coerceAtLeast(0.0)
                if (remFrac > 0.001 && !hasFather && sons == 0 && (brothers > 0 || sisters > 0)) {
                    val units = brothers * 2 + sisters
                    val perUnit = remFrac / units
                    if (brothers > 0) {
                        val bTot = perUnit * brothers * 2
                        shares.add(
                            UniversalInheritanceShare(
                                relationBn = "আপন ভাই ($brothers জন - আসাবা)",
                                totalFraction = bTot,
                                perPersonFraction = bTot / brothers,
                                count = brothers,
                                totalDecimal = safeArea * bTot,
                                perPersonDecimal = (safeArea * bTot) / brothers,
                                statusNote = "পিতা ও পুত্র না থাকায় অবশিষ্টভোগী"
                            )
                        )
                    }
                    if (sisters > 0) {
                        val sTot = perUnit * sisters
                        shares.add(
                            UniversalInheritanceShare(
                                relationBn = "আপন বোন ($sisters জন - আসাবা)",
                                totalFraction = sTot,
                                perPersonFraction = sTot / sisters,
                                count = sisters,
                                totalDecimal = safeArea * sTot,
                                perPersonDecimal = (safeArea * sTot) / sisters,
                                statusNote = "ভাইদের সাথে ২:১ অনুপাত"
                            )
                        )
                    }
                } else if (brothers > 0 || sisters > 0) {
                    warnings.add("পিতা বা পুত্র জীবিত থাকায় ভাই/বোন ফারায়েজে বঞ্চিত (Mahjub) — বিশেষ জটিলতায় মুফতি/আইনজীবীর পরামর্শ নিন।")
                }
                if (remFrac > 0.01 && shares.sumOf { it.totalFraction } < 0.99) {
                    legalReview = true
                    warnings.add("অবশিষ্ট অংশ (${String.format("%.2f%%", remFrac * 100)}) দূরবর্তী ওয়ারিশ/বায়তুল মালের আওতায় পড়তে পারে।")
                }
            }

            InheritanceLawType.HINDU_DAYABHAGA,
            InheritanceLawType.BUDDHIST_BD -> {
                // Bangladesh Dayabhaga system:
                // 1) Widow (since Hindu Women's Rights to Property Act 1937) + Sons share equally (class 1 Sapinda / Agnatic heirs)
                // 2) If no son & no widow, Daughters inherit; unmarried preferred before married with male issue
                // 3) If none, Father, then Mother
                val hasSon = sons > 0
                if (hasSon) {
                    val countSlots = sons + (if (hasSpouse && deceasedIsMale) 1 else 0)
                    val perSlot = 1.0 / countSlots
                    if (hasSpouse && deceasedIsMale) {
                        shares.add(
                            UniversalInheritanceShare(
                                relationBn = "স্ত্রী (বিধবা অংশ — ১৯৩৭ আইন)",
                                totalFraction = perSlot,
                                perPersonFraction = perSlot,
                                count = 1,
                                totalDecimal = safeArea * perSlot,
                                perPersonDecimal = safeArea * perSlot,
                                statusNote = "বাংলাদেশ হিন্দু আইনে সীমিত স্বত্ব (Life Interest) — আইনজীবীর পর্যালোচনা আবশ্যক"
                            )
                        )
                        legalReview = true
                    }
                    shares.add(
                        UniversalInheritanceShare(
                            relationBn = "পুত্র ($sons জন)",
                            totalFraction = perSlot * sons,
                            perPersonFraction = perSlot,
                            count = sons,
                            totalDecimal = safeArea * perSlot * sons,
                            perPersonDecimal = safeArea * perSlot,
                            statusNote = "দায়ভাগ অনুযায়ী পুত্রগণ সমান অংশীদার"
                        )
                    )
                    if (daughters > 0) {
                        warnings.add("বাংলাদেশ প্রচলিত দায়ভাগ আইনে পুত্র জীবিত থাকলে কন্যারা সরাসরি অংশ পান না (স্ত্রীধন/উইলের বিষয় আলাদা)।")
                    }
                } else if (hasSpouse && deceasedIsMale) {
                    shares.add(
                        UniversalInheritanceShare(
                            relationBn = "স্ত্রী (একমাত্র প্রাথমিক ওয়ারিশ)",
                            totalFraction = 1.0,
                            perPersonFraction = 1.0,
                            count = 1,
                            totalDecimal = safeArea,
                            perPersonDecimal = safeArea,
                            statusNote = "জীবনস্বত্ব — স্ত্রীর অবর্তমানে স্বামীর নিকটাত্মীয়/কন্যা পাবেন"
                        )
                    )
                    legalReview = true
                } else if (daughters > 0) {
                    val perD = 1.0 / daughters
                    shares.add(
                        UniversalInheritanceShare(
                            relationBn = "কন্যা ($daughters জন)",
                            totalFraction = 1.0,
                            perPersonFraction = perD,
                            count = daughters,
                            totalDecimal = safeArea,
                            perPersonDecimal = safeArea * perD,
                            statusNote = "অবিবাহিতা কন্যা বা পুত্রসন্তানবতী কন্যা অগ্রাধিকার পান — বিবাহিত/নিঃসন্তান অবস্থায় আইনি যাচাই প্রয়োজন"
                        )
                    )
                    legalReview = true
                } else if (hasFather) {
                    shares.add(
                        UniversalInheritanceShare("পিতা", 1.0, 1.0, 1, safeArea, safeArea, "দায়ভাগ ক্রম অনুযায়ী")
                    )
                } else if (hasMother) {
                    shares.add(
                        UniversalInheritanceShare("মাতা", 1.0, 1.0, 1, safeArea, safeArea, "দায়ভাগ ক্রম অনুযায়ী")
                    )
                } else {
                    legalReview = true
                    warnings.add("প্রাথমিক ওয়ারিশ না থাকায় সপিণ্ড/সকুল্য গোত্রীয় ওয়ারিশ নির্ধারণে আইনজীবীর সহায়তা নিন।")
                }

                if (lawType == InheritanceLawType.BUDDHIST_BD) {
                    legalReview = true
                    warnings.add("বাংলাদেশে বৌদ্ধ সম্প্রদায়ের উত্তরাধিকার প্রথা ও দায়ভাগ আইনের অধীন বিবেচিত হতে পারে — আঞ্চলিক প্রথা যাচাই করুন।")
                }
                if (!deceasedIsMale) {
                    legalReview = true
                    warnings.add("মৃত মহিলা হলে স্ত্রীধন (Stridhan) উত্তরাধিকার নিয়ম ভিন্ন হয় — দেওয়ানি/পারিবারিক আইন যাচাই আবশ্যক।")
                }
            }

            InheritanceLawType.CHRISTIAN_BD -> {
                // Succession Act 1925 (Sections 33-40)
                val totalChildren = sons + daughters
                if (hasSpouse && totalChildren > 0) {
                    val spouseFrac = 1.0 / 3.0
                    val childPool = 2.0 / 3.0
                    val perChild = childPool / totalChildren
                    shares.add(
                        UniversalInheritanceShare(
                            relationBn = if (deceasedIsMale) "স্ত্রী (Widow)" else "স্বামী (Widower)",
                            totalFraction = spouseFrac,
                            perPersonFraction = spouseFrac,
                            count = 1,
                            totalDecimal = safeArea * spouseFrac,
                            perPersonDecimal = safeArea * spouseFrac,
                            statusNote = "Succession Act 1925 ধারা ৩৩ (১/৩ অংশ)"
                        )
                    )
                    if (sons > 0) {
                        shares.add(
                            UniversalInheritanceShare(
                                relationBn = "পুত্র ($sons জন)",
                                totalFraction = perChild * sons,
                                perPersonFraction = perChild,
                                count = sons,
                                totalDecimal = safeArea * perChild * sons,
                                perPersonDecimal = safeArea * perChild,
                                statusNote = "পুত্র ও কন্যা সমান অংশীদার"
                            )
                        )
                    }
                    if (daughters > 0) {
                        shares.add(
                            UniversalInheritanceShare(
                                relationBn = "কন্যা ($daughters জন)",
                                totalFraction = perChild * daughters,
                                perPersonFraction = perChild,
                                count = daughters,
                                totalDecimal = safeArea * perChild * daughters,
                                perPersonDecimal = safeArea * perChild,
                                statusNote = "পুত্র ও কন্যা সমান অংশীদার"
                            )
                        )
                    }
                } else if (hasSpouse && totalChildren == 0) {
                    val hasKindred = hasFather || hasMother || brothers > 0 || sisters > 0
                    val spouseFrac = if (hasKindred) 0.5 else 1.0
                    shares.add(
                        UniversalInheritanceShare(
                            relationBn = if (deceasedIsMale) "স্ত্রী" else "স্বামী",
                            totalFraction = spouseFrac,
                            perPersonFraction = spouseFrac,
                            count = 1,
                            totalDecimal = safeArea * spouseFrac,
                            perPersonDecimal = safeArea * spouseFrac,
                            statusNote = if (hasKindred) "আত্মীয় থাকায় ১/২ অংশ" else "কোনো আত্মীয় না থাকায় সম্পূর্ণ অংশ"
                        )
                    )
                    if (hasKindred) {
                        val rem = 0.5
                        when {
                            hasFather -> shares.add(
                                UniversalInheritanceShare("পিতা", rem, rem, 1, safeArea * rem, safeArea * rem, "ধারা ৪২")
                            )
                            hasMother || brothers > 0 || sisters > 0 -> {
                                val kCount = (if (hasMother) 1 else 0) + brothers + sisters
                                val each = rem / kCount
                                if (hasMother) {
                                    shares.add(UniversalInheritanceShare("মাতা", each, each, 1, safeArea * each, safeArea * each, "ধারা ৪৩"))
                                }
                                if (brothers + sisters > 0) {
                                    val bsCount = brothers + sisters
                                    shares.add(
                                        UniversalInheritanceShare(
                                            "ভাই/বোন ($bsCount জন)",
                                            each * bsCount,
                                            each,
                                            bsCount,
                                            safeArea * each * bsCount,
                                            safeArea * each,
                                            "ধারা ৪৩ (সমান অংশ)"
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (totalChildren > 0) {
                    val perChild = 1.0 / totalChildren
                    if (sons > 0) {
                        shares.add(UniversalInheritanceShare("পুত্র ($sons জন)", perChild * sons, perChild, sons, safeArea * perChild * sons, safeArea * perChild, "ধারা ৩৭"))
                    }
                    if (daughters > 0) {
                        shares.add(UniversalInheritanceShare("কন্যা ($daughters জন)", perChild * daughters, perChild, daughters, safeArea * perChild * daughters, safeArea * perChild, "ধারা ৩৭"))
                    }
                } else if (hasFather) {
                    shares.add(UniversalInheritanceShare("পিতা", 1.0, 1.0, 1, safeArea, safeArea, "ধারা ৪২"))
                } else if (hasMother || brothers > 0 || sisters > 0) {
                    val kCount = (if (hasMother) 1 else 0) + brothers + sisters
                    val each = 1.0 / kCount
                    if (hasMother) shares.add(UniversalInheritanceShare("মাতা", each, each, 1, safeArea * each, safeArea * each, "ধারা ৪৩"))
                    if (brothers + sisters > 0) {
                        val c = brothers + sisters
                        shares.add(UniversalInheritanceShare("ভাই/বোন ($c জন)", each * c, each, c, safeArea * each * c, safeArea * each, "ধারা ৪৩"))
                    }
                } else {
                    legalReview = true
                    warnings.add("কোনো নিকটাত্মীয় নির্বাচিত হয়নি।")
                }
            }
        }

        val totalAllocated = shares.sumOf { it.totalFraction }
        return InheritanceCalculationReport(
            lawType = lawType,
            lawVersion = "BD-RuleSet-2026.1",
            totalDecimal = safeArea,
            shares = shares,
            unallocatedFraction = (1.0 - totalAllocated).coerceAtLeast(0.0),
            requiresLegalReview = legalReview || warnings.isNotEmpty(),
            warnings = warnings
        )
    }

    fun calculateFinancialBreakdown(
        areaDecimal: Double,
        customSalePriceBdt: Double?,
        otherExpensesBdt: Double,
        ownerShareCount: Int,
        rates: FinancialRateConfig
    ): FinanceValuationSummary {
        val safeArea = areaDecimal.coerceAtLeast(0.0)
        val mouzaVal = safeArea * rates.mouzaRatePerDecimalBdt.coerceAtLeast(0.0)
        val deedVal = (customSalePriceBdt?.takeIf { it > 0.0 } ?: mouzaVal).coerceAtLeast(0.0)
        val stamp = deedVal * (rates.stampDutyPct / 100.0)
        val reg = deedVal * (rates.registrationFeePct / 100.0)
        val localTax = deedVal * (rates.localGovtTaxPct / 100.0)
        val gainTax = deedVal * (rates.sourceGainTaxPct / 100.0)
        val mutation = rates.mutationFeeBdt.coerceAtLeast(0.0)
        val annualKhajna = safeArea * rates.landTaxPerDecimalBdt.coerceAtLeast(0.0)
        val totalGovt = stamp + reg + localTax + gainTax + mutation
        val totalAcq = deedVal + totalGovt + otherExpensesBdt.coerceAtLeast(0.0)
        val profitLoss = if (customSalePriceBdt != null && customSalePriceBdt > 0.0) {
            customSalePriceBdt - (mouzaVal + totalGovt + otherExpensesBdt.coerceAtLeast(0.0))
        } else {
            0.0
        }
        val divisor = ownerShareCount.coerceAtLeast(1)
        return FinanceValuationSummary(
            mouzaTotalValueBdt = mouzaVal,
            stampDutyBdt = stamp,
            registrationFeeBdt = reg,
            localGovtTaxBdt = localTax,
            gainTaxBdt = gainTax,
            mutationCostBdt = mutation,
            annualLandTaxBdt = annualKhajna,
            totalGovtAndProcessCostBdt = totalGovt,
            totalAcquisitionCostBdt = totalAcq,
            netProfitOrLossBdt = profitLoss,
            perShareCostBdt = totalAcq / divisor
        )
    }
}

/**
 * Pure Kotlin safe recursive-descent arithmetic parser for the Custom Form & Formula Builder.
 * Supports: +, -, *, /, ^, parentheses (), unary +/-, sqrt(), abs(), round(), and variable identifiers.
 */
object FormulaExpressionEvaluator {
    fun evaluate(expression: String, variables: Map<String, Double>): Double? {
        val cleaned = expression.trim()
        if (cleaned.isBlank()) return null
        return runCatching {
            val parser = Parser(cleaned, variables)
            val result = parser.parseExpression()
            parser.skipWhitespace()
            if (parser.pos < cleaned.length) return null
            if (result.isNaN() || result.isInfinite()) null else result
        }.getOrNull()
    }

    private class Parser(private val src: String, private val vars: Map<String, Double>) {
        var pos = 0

        fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun eat(ch: Char): Boolean {
            skipWhitespace()
            if (pos < src.length && src[pos] == ch) {
                pos++
                return true
            }
            return false
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                x = when {
                    eat('+') -> x + parseTerm()
                    eat('-') -> x - parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parsePower()
            while (true) {
                x = when {
                    eat('*') -> x * parsePower()
                    eat('/') -> {
                        val d = parsePower()
                        if (abs(d) < 1e-12) Double.NaN else x / d
                    }
                    else -> return x
                }
            }
        }

        private fun parsePower(): Double {
            var x = parseFactor()
            if (eat('^')) {
                x = x.pow(parsePower())
            }
            return x
        }

        private fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()
            skipWhitespace()
            val startPos = pos
            return when {
                eat('(') -> {
                    val v = parseExpression()
                    if (!eat(')')) error("Missing ')'")
                    v
                }
                pos < src.length && (src[pos].isDigit() || src[pos] == '.') -> {
                    while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
                    src.substring(startPos, pos).toDouble()
                }
                pos < src.length && (src[pos].isLetter() || src[pos] == '_') -> {
                    while (pos < src.length && (src[pos].isLetterOrDigit() || src[pos] == '_')) pos++
                    val name = src.substring(startPos, pos)
                    skipWhitespace()
                    if (eat('(')) {
                        val arg = parseExpression()
                        if (!eat(')')) error("Missing ')'")
                        when (name.lowercase()) {
                            "sqrt" -> sqrt(arg.coerceAtLeast(0.0))
                            "abs" -> abs(arg)
                            "round" -> kotlin.math.round(arg)
                            "sin" -> sin(Math.toRadians(arg))
                            "cos" -> cos(Math.toRadians(arg))
                            else -> error("Unknown func $name")
                        }
                    } else {
                        when (name.lowercase()) {
                            "pi" -> PI
                            else -> vars[name] ?: vars[name.lowercase()] ?: 0.0
                        }
                    }
                }
                else -> error("Unexpected char at $pos")
            }
        }
    }
}
