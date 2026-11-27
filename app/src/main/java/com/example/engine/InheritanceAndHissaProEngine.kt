package com.example.engine

import com.example.model.AnaGondaBreakdown
import com.example.model.CircleSectorResult
import com.example.model.CrossKhatianHolding
import com.example.model.DeceasedHeirRedistributionReport
import com.example.model.FormulaCheatSheetEntry
import com.example.model.HanafiFaraizReport
import com.example.model.HanafiHeirInput
import com.example.model.HanafiShareLine
import com.example.model.HinduInheritanceReport
import com.example.model.HinduInheritanceRules
import com.example.model.HinduSchool
import com.example.model.HissaAnalysisReport
import com.example.model.HissaOwnerComputed
import com.example.model.HissaOwnerEntry
import com.example.model.RegionalUnitProfile
import com.example.model.UnknownSideShape
import com.example.model.UnknownSideSolveResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object InheritanceAndHissaProEngine {

    private const val TOTAL_TIL_IN_16_ANA = 76800.0

    // ==================== 1. ANA-GONDA-KORA-KRANTI-TIL CONVERSION ====================

    fun anaGondaToRatio(ana: Int, gonda: Int, kora: Int, kranti: Int, til: Int): Double {
        val totalTil = (ana * 4800.0) + (gonda * 240.0) + (kora * 60.0) + (kranti * 20.0) + til
        return (totalTil / TOTAL_TIL_IN_16_ANA).coerceAtLeast(0.0)
    }

    fun ratioToAnaGondaBreakdown(ratio: Double): AnaGondaBreakdown {
        val safeRatio = ratio.coerceAtLeast(0.0)
        var remTil = (safeRatio * TOTAL_TIL_IN_16_ANA).roundToInt()
        val ana = remTil / 4800
        remTil %= 4800
        val gonda = remTil / 240
        remTil %= 240
        val kora = remTil / 60
        remTil %= 60
        val kranti = remTil / 20
        val til = remTil % 20
        return AnaGondaBreakdown(
            ratio = safeRatio,
            percentage = safeRatio * 100.0,
            ana = ana,
            gonda = gonda,
            kora = kora,
            kranti = kranti,
            til = til
        )
    }

    private fun buildShareLine(
        key: String,
        relationBn: String,
        count: Int,
        totalFraction: Double,
        totalDecimal: Double,
        ruleNote: String,
        unitProfile: RegionalUnitProfile,
        isBlocked: Boolean = false
    ): HanafiShareLine {
        val safeCount = count.coerceAtLeast(1)
        val perPersonFrac = totalFraction / safeCount
        val totDec = totalDecimal * totalFraction
        val perDec = totDec / safeCount
        val sqFtPerDec = LandCalculators.SQ_FT_PER_DECIMAL
        return HanafiShareLine(
            key = key,
            relationBn = relationBn,
            count = count,
            totalFraction = totalFraction,
            perPersonFraction = perPersonFrac,
            totalDecimal = totDec,
            perPersonDecimal = perDec,
            totalKatha = (totDec * sqFtPerDec) / unitProfile.kathaSqFt.coerceAtLeast(100.0),
            totalBigha = (totDec * sqFtPerDec) / unitProfile.bighaSqFt.coerceAtLeast(100.0),
            anaGonda = ratioToAnaGondaBreakdown(totalFraction),
            perPersonAnaGonda = ratioToAnaGondaBreakdown(perPersonFrac),
            shareRuleNote = ruleNote,
            isBlocked = isBlocked
        )
    }

    // ==================== 2. HANAFI FARAIZ (AWL, RADD, HAJB, MANASIKHA) ====================

    private data class MutableHeirSlot(
        val key: String,
        val relationBn: String,
        val count: Int,
        var fraction: Double,
        val note: String,
        val isSpouse: Boolean = false
    )

    fun calculateHanafiFaraiz(
        input: HanafiHeirInput,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): HanafiFaraizReport {
        val totalDec = input.totalDecimal.coerceAtLeast(0.0)
        val notes = mutableListOf<String>()
        val blockedNotes = mutableListOf<String>()
        val slots = mutableListOf<MutableHeirSlot>()

        val hasSon = input.sons > 0
        val hasDaughter = input.daughters > 0
        val hasSonSon = !hasSon && input.sonSons > 0
        val hasSonDaughter = !hasSon && input.sonDaughters > 0
        val hasDescendant = hasSon || hasDaughter || hasSonSon || hasSonDaughter
        val hasMaleDescendant = hasSon || hasSonSon
        val hasFather = input.hasFather
        val hasGrandfather = !hasFather && input.hasPaternalGrandfather
        val hasMother = input.hasMother
        val siblingCount = (if (!hasFather && !hasMaleDescendant && !hasGrandfather) {
            input.fullBrothers + input.fullSisters + input.paternalBrothers + input.paternalSisters
        } else 0)
        val rawSiblingCount = input.fullBrothers + input.fullSisters + input.paternalBrothers + input.paternalSisters

        // Blocking notifications (Hajb)
        if (hasSon && (input.sonSons > 0 || input.sonDaughters > 0)) {
            blockedNotes.add("পুত্র জীবিত থাকায় নাতি/নাতনি (পুত্রের সন্তান) হজব/বঞ্চিত।")
        }
        if (hasFather && input.hasPaternalGrandfather) {
            blockedNotes.add("পিতা জীবিত থাকায় দাদা বঞ্চিত।")
        }
        if (hasMother && (input.hasPaternalGrandmother || input.hasMaternalGrandmother)) {
            blockedNotes.add("মাতা জীবিত থাকায় দাদি/নানি বঞ্চিত।")
        }
        if (hasFather && input.hasPaternalGrandmother) {
            blockedNotes.add("পিতা জীবিত থাকায় পিতার মাতা (দাদি) বঞ্চিত।")
        }
        if ((hasFather || hasGrandfather || hasMaleDescendant) && rawSiblingCount > 0) {
            blockedNotes.add("পিতা/দাদা/পুত্র/নাতি জীবিত থাকায় ভাই-বোনেরা বঞ্চিত।")
        }
        if (!hasFather && !hasGrandfather && !hasMaleDescendant && input.fullBrothers > 0 &&
            (input.paternalBrothers > 0 || input.paternalSisters > 0)
        ) {
            blockedNotes.add("আপন ভাই জীবিত থাকায় বৈমাত্রেয় (সৎ) ভাই-বোন বঞ্চিত।")
        }

        // 1) Spouse
        if (input.spouseCount > 0) {
            if (input.deceasedIsMale) {
                val wives = input.spouseCount.coerceIn(1, 4)
                val wifeFrac = if (hasDescendant) 1.0 / 8.0 else 1.0 / 4.0
                slots.add(
                    MutableHeirSlot(
                        key = "spouse",
                        relationBn = if (wives == 1) "স্ত্রী" else "স্ত্রীগণ ($wives জন)",
                        count = wives,
                        fraction = wifeFrac,
                        note = if (hasDescendant) "সন্তান/নাতি থাকায় ১/৮ অংশ" else "সন্তান না থাকায় ১/৪ অংশ",
                        isSpouse = true
                    )
                )
                notes.add("স্ত্রী অংশ: ${if (hasDescendant) "১/৮" else "১/৪"}")
            } else {
                val husbFrac = if (hasDescendant) 1.0 / 4.0 else 1.0 / 2.0
                slots.add(
                    MutableHeirSlot(
                        key = "spouse",
                        relationBn = "স্বামী",
                        count = 1,
                        fraction = husbFrac,
                        note = if (hasDescendant) "সন্তান/নাতি থাকায় ১/৪ অংশ" else "সন্তান না থাকায় ১/২ অংশ",
                        isSpouse = true
                    )
                )
                notes.add("স্বামী অংশ: ${if (hasDescendant) "১/৪" else "১/২"}")
            }
        }

        // 2) Mother & Grandmothers
        if (hasMother) {
            val motherGetsSixth = hasDescendant || rawSiblingCount >= 2
            val motherFrac = if (motherGetsSixth) 1.0 / 6.0 else 1.0 / 3.0
            // Umariyatayn case: Spouse + Mother + Father with no children/siblings -> Mother gets 1/3 of remainder
            val finalMotherFrac = if (!hasDescendant && rawSiblingCount < 2 && hasFather && input.spouseCount > 0) {
                val spouseF = slots.firstOrNull { it.isSpouse }?.fraction ?: 0.0
                (1.0 - spouseF) / 3.0
            } else motherFrac
            slots.add(
                MutableHeirSlot(
                    key = "mother",
                    relationBn = "মাতা",
                    count = 1,
                    fraction = finalMotherFrac,
                    note = when {
                        !hasDescendant && rawSiblingCount < 2 && hasFather && input.spouseCount > 0 ->
                            "উমারিয়্যাতাইন নিয়ম: স্বামী/স্ত্রীর পর অবশিষ্টের ১/৩"
                        motherGetsSixth -> "সন্তান বা ২+ ভাইবোন থাকায় ১/৬ অংশ"
                        else -> "সন্তান/২+ ভাইবোন না থাকায় ১/৩ অংশ"
                    }
                )
            )
        } else {
            val gmList = mutableListOf<Pair<String, String>>()
            if (input.hasMaternalGrandmother) gmList.add("grandmother_mat" to "নানি (মাতার মাতা)")
            if (input.hasPaternalGrandmother && !hasFather) gmList.add("grandmother_pat" to "দাদি (পিতার মাতা)")
            if (gmList.isNotEmpty()) {
                val perGm = (1.0 / 6.0) / gmList.size
                gmList.forEach { (k, lbl) ->
                    slots.add(MutableHeirSlot(k, lbl, 1, perGm, "মাতা না থাকায় দাদি/নানি মিলিতভাবে ১/৬ অংশ"))
                }
            }
        }

        // 3) Father / Grandfather fixed 1/6 if male descendant exists (or female descendant -> 1/6 + Asaba)
        if (hasFather && hasDescendant) {
            slots.add(
                MutableHeirSlot(
                    key = "father",
                    relationBn = "পিতা",
                    count = 1,
                    fraction = 1.0 / 6.0,
                    note = "সন্তান থাকায় নির্দিষ্ট ১/৬ অংশ (পুত্র না থাকলে অবশিষ্টভোগীও)"
                )
            )
        } else if (hasGrandfather && hasDescendant) {
            slots.add(
                MutableHeirSlot(
                    key = "grandfather",
                    relationBn = "দাদা",
                    count = 1,
                    fraction = 1.0 / 6.0,
                    note = "পিতা না থাকায় ও সন্তান থাকায় ১/৬ অংশ"
                )
            )
        }

        // 4) Daughters (fixed when no sons)
        if (!hasSon && hasDaughter) {
            val dFrac = if (input.daughters == 1) 0.5 else 2.0 / 3.0
            slots.add(
                MutableHeirSlot(
                    key = "daughters",
                    relationBn = "কন্যা (${input.daughters} জন)",
                    count = input.daughters,
                    fraction = dFrac,
                    note = if (input.daughters == 1) "একমাত্র কন্যা (পুত্র নেই) ১/২ অংশ" else "একাধিক কন্যা (পুত্র নেই) ২/৩ অংশ"
                )
            )
        }

        // 5) Son's Daughters fixed when no sons and no sonSons (only if daughters < 2)
        if (!hasSon && !hasSonSon && hasSonDaughter) {
            if (!hasDaughter) {
                val sdFrac = if (input.sonDaughters == 1) 0.5 else 2.0 / 3.0
                slots.add(
                    MutableHeirSlot(
                        key = "son_daughters",
                        relationBn = "নাতনি - পুত্রের কন্যা (${input.sonDaughters} জন)",
                        count = input.sonDaughters,
                        fraction = sdFrac,
                        note = if (input.sonDaughters == 1) "পুত্র/কন্যা না থাকায় ১/২ অংশ" else "পুত্র/কন্যা না থাকায় ২/৩ অংশ"
                    )
                )
            } else if (input.daughters == 1) {
                slots.add(
                    MutableHeirSlot(
                        key = "son_daughters",
                        relationBn = "নাতনি - পুত্রের কন্যা (${input.sonDaughters} জন)",
                        count = input.sonDaughters,
                        fraction = 1.0 / 6.0,
                        note = "১ কন্যার সাথে ২/৩ পূর্ণ করতে ১/৬ অংশ (তাকমিলা)"
                    )
                )
            } else {
                blockedNotes.add("২+ কন্যা থাকায় এবং নাতি না থাকায় নাতনি (পুত্রের কন্যা) বঞ্চিত।")
            }
        }

        // 6) Fixed shares for sisters when NO descendant, NO father, NO grandfather, and NO corresponding brother
        val canSistersInherit = !hasDescendant && !hasFather && !hasGrandfather
        if (canSistersInherit && input.fullBrothers == 0 && input.fullSisters > 0) {
            val fsFrac = if (input.fullSisters == 1) 0.5 else 2.0 / 3.0
            slots.add(
                MutableHeirSlot(
                    key = "full_sisters",
                    relationBn = "আপন বোন (${input.fullSisters} জন)",
                    count = input.fullSisters,
                    fraction = fsFrac,
                    note = if (input.fullSisters == 1) "সন্তান/পিতা/ভাই না থাকায় ১/২ অংশ" else "সন্তান/পিতা/ভাই না থাকায় ২/৩ অংশ"
                )
            )
        }
        if (canSistersInherit && input.fullBrothers == 0 && input.paternalBrothers == 0 && input.paternalSisters > 0) {
            if (input.fullSisters == 0) {
                val psFrac = if (input.paternalSisters == 1) 0.5 else 2.0 / 3.0
                slots.add(
                    MutableHeirSlot(
                        key = "paternal_sisters",
                        relationBn = "সৎ বোন - পিতৃক (${input.paternalSisters} জন)",
                        count = input.paternalSisters,
                        fraction = psFrac,
                        note = if (input.paternalSisters == 1) "১/২ অংশ" else "২/৩ অংশ"
                    )
                )
            } else if (input.fullSisters == 1) {
                slots.add(
                    MutableHeirSlot(
                        key = "paternal_sisters",
                        relationBn = "সৎ বোন - পিতৃক (${input.paternalSisters} জন)",
                        count = input.paternalSisters,
                        fraction = 1.0 / 6.0,
                        note = "১ আপন বোনের সাথে ২/৩ পূর্ণ করতে ১/৬ অংশ"
                    )
                )
            } else {
                blockedNotes.add("২+ আপন বোন থাকায় ও সৎ ভাই না থাকায় পিতৃক সৎ বোন বঞ্চিত।")
            }
        }

        // Apply custom overrides before Asaba/Awl/Radd if user manually edited fractions
        var customOverrideApplied = false
        if (input.customFractionOverrides.isNotEmpty()) {
            slots.forEach { slot ->
                input.customFractionOverrides[slot.key]?.let { ov ->
                    slot.fraction = ov.coerceIn(0.0, 1.0)
                    customOverrideApplied = true
                }
            }
        }

        // 7) Allocate Remainder (Asaba / Residuary) if fixed sum < 1.0
        val fixedSumBeforeAsaba = slots.sumOf { it.fraction }
        var remainder = (1.0 - fixedSumBeforeAsaba).coerceAtLeast(0.0)
        var hasAsaba = false

        if (remainder > 1e-6) {
            when {
                // Case A: Sons (+ Daughters 2:1)
                hasSon -> {
                    hasAsaba = true
                    val units = input.sons * 2.0 + input.daughters
                    val sonF = remainder * ((input.sons * 2.0) / units)
                    slots.add(
                        MutableHeirSlot(
                            key = "sons",
                            relationBn = "পুত্র (${input.sons} জন)",
                            count = input.sons,
                            fraction = sonF,
                            note = if (hasDaughter) "আসাবা (পুত্র:কন্যা ২:১ অনুপাত)" else "অবশিষ্টভোগী (আসাবা)"
                        )
                    )
                    if (hasDaughter) {
                        val dF = remainder * (input.daughters.toDouble() / units)
                        slots.add(
                            MutableHeirSlot(
                                key = "daughters",
                                relationBn = "কন্যা (${input.daughters} জন)",
                                count = input.daughters,
                                fraction = dF,
                                note = "পুত্রের সাথে ২:১ আসাবা"
                            )
                        )
                    }
                    remainder = 0.0
                }
                // Case B: Son's Sons (+ Son's Daughters 2:1 when daughters < 2 or even if 2+ daughters exist!)
                !hasSon && input.sonSons > 0 -> {
                    hasAsaba = true
                    val units = input.sonSons * 2.0 + input.sonDaughters
                    val ssF = remainder * ((input.sonSons * 2.0) / units)
                    slots.add(
                        MutableHeirSlot(
                            key = "son_sons",
                            relationBn = "নাতি - পুত্রের পুত্র (${input.sonSons} জন)",
                            count = input.sonSons,
                            fraction = ssF,
                            note = "আসাবা (পুত্র না থাকায়)"
                        )
                    )
                    if (input.sonDaughters > 0) {
                        val sdF = remainder * (input.sonDaughters.toDouble() / units)
                        slots.add(
                            MutableHeirSlot(
                                key = "son_daughters",
                                relationBn = "নাতনি - পুত্রের কন্যা (${input.sonDaughters} জন)",
                                count = input.sonDaughters,
                                fraction = sdF,
                                note = "নাতির সাথে ২:১ আসাবা"
                            )
                        )
                    }
                    remainder = 0.0
                }
                // Case C: Father gets remainder as Asaba
                hasFather -> {
                    hasAsaba = true
                    val existingFather = slots.find { it.key == "father" }
                    if (existingFather != null) {
                        existingFather.fraction += remainder
                    } else {
                        slots.add(MutableHeirSlot("father", "পিতা", 1, remainder, "সন্তান না থাকায় পিতা আসাবা (অবশিষ্টভোগী)"))
                    }
                    remainder = 0.0
                }
                // Case D: Grandfather gets remainder as Asaba (Hanafi rule when father absent)
                hasGrandfather -> {
                    hasAsaba = true
                    val existingGf = slots.find { it.key == "grandfather" }
                    if (existingGf != null) {
                        existingGf.fraction += remainder
                    } else {
                        slots.add(MutableHeirSlot("grandfather", "দাদা", 1, remainder, "পিতা না থাকায় দাদা আসাবা"))
                    }
                    remainder = 0.0
                }
                // Case E: Full Sisters with female descendants (Asaba ma'al ghair)
                hasDescendant && !hasMaleDescendant && input.fullBrothers == 0 && input.fullSisters > 0 -> {
                    hasAsaba = true
                    slots.add(
                        MutableHeirSlot(
                            key = "full_sisters",
                            relationBn = "আপন বোন (${input.fullSisters} জন)",
                            count = input.fullSisters,
                            fraction = remainder,
                            note = "কন্যা/নাতনির সাথে আসাবা মা'আল গাইর"
                        )
                    )
                    remainder = 0.0
                }
                // Case F: Full Brothers (+ Full Sisters 2:1)
                !hasMaleDescendant && input.fullBrothers > 0 -> {
                    hasAsaba = true
                    val units = input.fullBrothers * 2.0 + input.fullSisters
                    val fbF = remainder * ((input.fullBrothers * 2.0) / units)
                    slots.add(
                        MutableHeirSlot(
                            key = "full_brothers",
                            relationBn = "আপন ভাই (${input.fullBrothers} জন)",
                            count = input.fullBrothers,
                            fraction = fbF,
                            note = "আসাবা (ভাই:বোন ২:১)"
                        )
                    )
                    if (input.fullSisters > 0) {
                        val fsF = remainder * (input.fullSisters.toDouble() / units)
                        slots.add(
                            MutableHeirSlot(
                                key = "full_sisters",
                                relationBn = "আপন বোন (${input.fullSisters} জন)",
                                count = input.fullSisters,
                                fraction = fsF,
                                note = "আপন ভাইয়ের সাথে ২:১ আসাবা"
                            )
                        )
                    }
                    remainder = 0.0
                }
                // Case G: Paternal Sisters with female descendants (when no full brother/sister)
                hasDescendant && !hasMaleDescendant && input.fullBrothers == 0 && input.fullSisters == 0 &&
                    input.paternalBrothers == 0 && input.paternalSisters > 0 -> {
                    hasAsaba = true
                    slots.add(
                        MutableHeirSlot(
                            key = "paternal_sisters",
                            relationBn = "সৎ বোন - পিতৃক (${input.paternalSisters} জন)",
                            count = input.paternalSisters,
                            fraction = remainder,
                            note = "কন্যার সাথে আসাবা মা'আল গাইর"
                        )
                    )
                    remainder = 0.0
                }
                // Case H: Paternal Brothers (+ Paternal Sisters 2:1)
                !hasMaleDescendant && input.fullBrothers == 0 && input.paternalBrothers > 0 -> {
                    hasAsaba = true
                    val units = input.paternalBrothers * 2.0 + input.paternalSisters
                    val pbF = remainder * ((input.paternalBrothers * 2.0) / units)
                    slots.add(
                        MutableHeirSlot(
                            key = "paternal_brothers",
                            relationBn = "সৎ ভাই - পিতৃক (${input.paternalBrothers} জন)",
                            count = input.paternalBrothers,
                            fraction = pbF,
                            note = "আসাবা"
                        )
                    )
                    if (input.paternalSisters > 0) {
                        val psF = remainder * (input.paternalSisters.toDouble() / units)
                        slots.add(
                            MutableHeirSlot(
                                key = "paternal_sisters",
                                relationBn = "সৎ বোন - পিতৃক (${input.paternalSisters} জন)",
                                count = input.paternalSisters,
                                fraction = psF,
                                note = "সৎ ভাইয়ের সাথে ২:১ আসাবা"
                            )
                        )
                    }
                    remainder = 0.0
                }
            }
        }

        // 8) AWL (Sum > 1.0) or RADD (Sum < 1.0 and no Asaba)
        val totalBeforeAdj = slots.sumOf { it.fraction }
        var awlApplied = false
        var raddApplied = false
        var adjSummary = "স্বাভাবিক বণ্টন (মোট অংশ = ১.০০০০)"

        if (totalBeforeAdj > 1.0001) {
            awlApplied = true
            val factor = 1.0 / totalBeforeAdj
            slots.forEach { it.fraction *= factor }
            adjSummary = String.format(
                Locale.US,
                "আউল (Awl) প্রয়োগ: নির্দিষ্ট অংশের যোগফল %.4f (> ১.০) হওয়ায় আনুপাতিকভাবে হ্রাস করা হয়েছে।",
                totalBeforeAdj
            )
            notes.add(adjSummary)
        } else if (totalBeforeAdj > 1e-6 && totalBeforeAdj < 0.9999 && !hasAsaba) {
            raddApplied = true
            val nonSpouseSlots = slots.filter { !it.isSpouse }
            if (nonSpouseSlots.isNotEmpty()) {
                val spouseSum = slots.filter { it.isSpouse }.sumOf { it.fraction }
                val targetNonSpousePool = (1.0 - spouseSum).coerceAtLeast(0.0)
                val currentNonSpouseSum = nonSpouseSlots.sumOf { it.fraction }.coerceAtLeast(1e-9)
                val scale = targetNonSpousePool / currentNonSpouseSum
                nonSpouseSlots.forEach { it.fraction *= scale }
                adjSummary = "রাদ্দ (Radd) প্রয়োগ: অবশিষ্ট অংশ স্বামী/স্ত্রী ছাড়া রক্তসম্পর্কের ওয়ারিশদের মাঝে আনুপাতিকভাবে বণ্টিত হয়েছে।"
            } else {
                // Only spouse exists -> Hanafi Radd returns remainder to spouse if no Dhawil Arham
                val scale = 1.0 / totalBeforeAdj
                slots.forEach { it.fraction *= scale }
                adjSummary = "রাদ্দ (Radd) প্রয়োগ: অন্য কোনো ওয়ারিশ না থাকায় অবশিষ্ট অংশ স্বামী/স্ত্রীর নিকট ফিরেছে।"
            }
            notes.add(adjSummary)
        }

        val shareLines = slots.map { slot ->
            buildShareLine(
                key = slot.key,
                relationBn = slot.relationBn,
                count = slot.count,
                totalFraction = slot.fraction,
                totalDecimal = totalDec,
                ruleNote = slot.note,
                unitProfile = unitProfile
            )
        }

        return HanafiFaraizReport(
            totalDecimal = totalDec,
            shares = shareLines,
            blockedHeirsNotes = blockedNotes,
            awlApplied = awlApplied,
            raddApplied = raddApplied,
            customOverrideApplied = customOverrideApplied,
            awlOrRaddSummary = adjSummary,
            stepByStepNotes = notes,
            totalAllocatedFraction = shareLines.sumOf { it.totalFraction }
        )
    }

    /**
     * Manasikha / Deceased Heir 2-Stage Redistribution:
     * Redistributes one deceased heir's inherited decimal area among their own heirs.
     */
    fun redistributeDeceasedHeirShare(
        deceasedHeirLabel: String,
        inheritedDecimal: Double,
        stage2Input: HanafiHeirInput,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): DeceasedHeirRedistributionReport {
        val stage2Report = calculateHanafiFaraiz(
            stage2Input.copy(totalDecimal = inheritedDecimal),
            unitProfile
        )
        return DeceasedHeirRedistributionReport(
            deceasedHeirLabel = deceasedHeirLabel.ifBlank { "মৃত ওয়ারিশ" },
            inheritedDecimal = inheritedDecimal,
            stage2Report = stage2Report,
            combinedLivingShares = stage2Report.shares
        )
    }

    // ==================== 3. HINDU INHERITANCE (DAYABHAGA VS MITAKSHARA) ====================

    fun calculateHinduInheritance(
        totalDecimal: Double,
        deceasedIsMale: Boolean,
        hasSpouse: Boolean,
        sons: Int,
        daughters: Int,
        hasFather: Boolean,
        hasMother: Boolean,
        rules: HinduInheritanceRules,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): HinduInheritanceReport {
        val safeDec = totalDecimal.coerceAtLeast(0.0)
        val steps = mutableListOf<String>()
        val lines = mutableListOf<HanafiShareLine>()

        when (rules.school) {
            HinduSchool.DAYABHAGA -> {
                steps.add("দায়ভাগ প্রথা (বাংলাদেশ প্রচলিত): পিতার জীবদ্দশায় পুত্রদের জন্মগত অংশ থাকে না; মৃত্যুর পর সম্পত্তি বণ্টিত হয়।")
                if (sons > 0) {
                    val slots = sons + (if (hasSpouse && deceasedIsMale && rules.widowGetsEqualSonShare) 1 else 0)
                    val each = 1.0 / slots.coerceAtLeast(1)
                    if (hasSpouse && deceasedIsMale && rules.widowGetsEqualSonShare) {
                        lines.add(buildShareLine("widow", "স্ত্রী (বিধবা জীবনস্বত্ব)", 1, each, safeDec, "১৯৩৭ আইন অনুযায়ী পুত্রের সমান ১ অংশ", unitProfile))
                    }
                    lines.add(buildShareLine("sons", "পুত্র ($sons জন)", sons, each * sons, safeDec, "সকল পুত্র সমান অংশীদার", unitProfile))
                    if (daughters > 0) {
                        steps.add("পুত্র জীবিত থাকায় দায়ভাগ প্রথায় কন্যারা সাধারণত সরাসরি অংশ পান না (বিশেষ উইল বা স্ত্রীধন ছাড়া)।")
                    }
                } else if (hasSpouse && deceasedIsMale) {
                    lines.add(buildShareLine("widow", "স্ত্রী (বিধবা)", 1, 1.0, safeDec, "পুত্র না থাকায় স্ত্রী সম্পূর্ণ অংশ পান (জীবনস্বত্ব)", unitProfile))
                } else if (daughters > 0) {
                    lines.add(buildShareLine("daughters", "কন্যা ($daughters জন)", daughters, 1.0, safeDec, "পুত্র ও বিধবা না থাকায় কন্যারা অংশীদার", unitProfile))
                } else if (hasFather) {
                    lines.add(buildShareLine("father", "পিতা", 1, 1.0, safeDec, "দায়ভাগ ক্রম অনুযায়ী পিতা", unitProfile))
                } else if (hasMother) {
                    lines.add(buildShareLine("mother", "মাতা", 1, 1.0, safeDec, "দায়ভাগ ক্রম অনুযায়ী মাতা", unitProfile))
                }
            }
            HinduSchool.MITAKSHARA -> {
                steps.add("মিতাক্ষরা প্রথা: সহদায়াদ (Coparcenary) অনুযায়ী জন্মসূত্রে অধিকার বিবেচিত হয়; সংশোধিত নিয়ম টগল প্রযোজ্য।")
                val includeDaughters = rules.daughtersEqualInMitakshara && daughters > 0
                val includeMother = rules.motherGetsEqualOnPartition && hasMother
                val includeWidow = hasSpouse && deceasedIsMale && rules.widowGetsEqualSonShare
                val totalUnits = sons + (if (includeDaughters) daughters else 0) + (if (includeWidow) 1 else 0) + (if (includeMother) 1 else 0)
                if (totalUnits > 0) {
                    val each = 1.0 / totalUnits
                    if (sons > 0) lines.add(buildShareLine("sons", "পুত্র ($sons জন)", sons, each * sons, safeDec, "সহদায়াদ সমান অংশ", unitProfile))
                    if (includeDaughters) lines.add(buildShareLine("daughters", "কন্যা ($daughters জন)", daughters, each * daughters, safeDec, "সমান সহদায়াদ অংশ", unitProfile))
                    if (includeWidow) lines.add(buildShareLine("widow", "স্ত্রী", 1, each, safeDec, "বণ্টনে স্ত্রীর সমান অংশ", unitProfile))
                    if (includeMother) lines.add(buildShareLine("mother", "মাতা", 1, each, safeDec, "বণ্টনে মাতার সমান অংশ", unitProfile))
                } else if (hasFather) {
                    lines.add(buildShareLine("father", "পিতা", 1, 1.0, safeDec, "পিতা একমাত্র উত্তরাধিকারী", unitProfile))
                }
            }
        }
        if (!deceasedIsMale) {
            steps.add("মৃত মহিলা হলে স্ত্রীধন (Stridhan) নিয়মে কন্যা ও অবিবাহিতা কন্যাগণ অগ্রাধিকার পেতে পারেন — আইনজীবীর পর্যালোচনা আবশ্যক।")
        }
        return HinduInheritanceReport(
            rules = rules,
            totalDecimal = safeDec,
            shares = lines,
            stepByStepExplanation = steps,
            requiresLegalReview = true
        )
    }

    // ==================== 4. MULTI-OWNER HISSA & CROSS-KHATIAN ====================

    fun analyzeHissaOwners(
        dagTotalDecimal: Double,
        owners: List<HissaOwnerEntry>,
        soldOrDeductedRatio: Double = 0.0,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): HissaAnalysisReport {
        val safeDagDec = dagTotalDecimal.coerceAtLeast(0.0)
        val sqFtPerDec = LandCalculators.SQ_FT_PER_DECIMAL
        val computed = owners.map { o ->
            val r = (o.ratio() * (1.0 - soldOrDeductedRatio.coerceIn(0.0, 1.0))).coerceAtLeast(0.0)
            val dec = safeDagDec * r
            val sq = dec * sqFtPerDec
            val br = ratioToAnaGondaBreakdown(r)
            HissaOwnerComputed(
                id = o.id,
                name = o.name.ifBlank { "মালিক" },
                ratio = r,
                decimal = dec,
                katha = sq / unitProfile.kathaSqFt.coerceAtLeast(100.0),
                sqFt = sq,
                anaGondaText = br.formattedBn()
            )
        }
        val totalRatio = computed.sumOf { it.ratio }
        val totalAna = totalRatio * 16.0
        val mismatchAna = totalAna - 16.0
        val isExact = abs(totalRatio - 1.0) <= 0.001
        val note = when {
            isExact -> "✓ ১৬ আনা (১.০০০০) সম্পূর্ণ ও নির্ভুলভাবে মিলেছে।"
            totalRatio < 1.0 -> String.format(Locale.US, "⚠ মোট %.2f আনা (%.3f অংশ) — ১৬ আনা পূরণে %.2f আনা কম আছে।", totalAna, totalRatio, -mismatchAna)
            else -> String.format(Locale.US, "⚠ মোট %.2f আনা (%.3f অংশ) — ১৬ আনার চেয়ে %.2f আনা বেশি হয়েছে!", totalAna, totalRatio, mismatchAna)
        }
        return HissaAnalysisReport(
            dagTotalDecimal = safeDagDec,
            owners = computed,
            totalRatio = totalRatio,
            totalAnaEquivalent = totalAna,
            isExact16Ana = isExact,
            mismatchAna = mismatchAna,
            statusNote = note
        )
    }

    fun aggregateCrossKhatianOwnerShare(
        holdings: List<CrossKhatianHolding>,
        unitProfile: RegionalUnitProfile = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES.first()
    ): Triple<Double, Double, Double> {
        val totalDec = holdings.sumOf { it.dagTotalDecimal.coerceAtLeast(0.0) * it.ownerRatio.coerceIn(0.0, 1.0) }
        val totalSqFt = totalDec * LandCalculators.SQ_FT_PER_DECIMAL
        val totalKatha = totalSqFt / unitProfile.kathaSqFt.coerceAtLeast(100.0)
        return Triple(totalDec, totalKatha, totalSqFt)
    }

    // ==================== 5. UNKNOWN-SIDE GEOMETRY SOLVER & CIRCLE/SECTOR ====================

    fun solveUnknownSide(
        shape: UnknownSideShape,
        areaSqFt: Double,
        v1: Double,
        v2: Double = 0.0,
        v3: Double = 0.0,
        v4: Double = 0.0
    ): UnknownSideSolveResult {
        if (areaSqFt <= 0.0) {
            return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "ক্ষেত্রফল ০-এর বেশি দিন")
        }
        return when (shape) {
            UnknownSideShape.RECTANGLE -> {
                if (v1 <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "জানা বাহু ০-এর বেশি দিন")
                val side2 = areaSqFt / v1
                UnknownSideSolveResult(
                    solvedValue = side2,
                    labelBn = "অজানা অপর বাহু (প্রস্থ/দৈর্ঘ্য)",
                    formulaUsed = "অজানা বাহু = ক্ষেত্রফল ÷ জানা বাহু",
                    stepByStep = String.format(Locale.US, "%.2f ব.ফু ÷ %.2f ফু = %.3f ফু (%s)", areaSqFt, v1, side2, LandCalculators.formatFeetInchesSuta(side2)),
                    isValid = true
                )
            }
            UnknownSideShape.TRAPEZIUM_HEIGHT -> {
                val sumParallel = v1 + v2
                if (sumParallel <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "দুই সমান্তরাল বাহুর যোগফল > ০ হতে হবে")
                val h = (2.0 * areaSqFt) / sumParallel
                UnknownSideSolveResult(
                    solvedValue = h,
                    labelBn = "সমান্তরাল বাহুদ্বয়ের লম্ব দূরত্ব (উচ্চতা)",
                    formulaUsed = "h = (২ × ক্ষেত্রফল) ÷ (a + b)",
                    stepByStep = String.format(Locale.US, "(২ × %.2f) ÷ (%.2f + %.2f) = %.3f ফু", areaSqFt, v1, v2, h),
                    isValid = true
                )
            }
            UnknownSideShape.TRAPEZIUM_SIDE -> {
                // v1 = height, v2 = known parallel side
                if (v1 <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "উচ্চতা > ০ হতে হবে")
                val otherSide = ((2.0 * areaSqFt) / v1) - v2
                if (otherSide <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "প্রদত্ত ক্ষেত্রফল জানা বাহু ও উচ্চতার চেয়ে ছোট")
                UnknownSideSolveResult(
                    solvedValue = otherSide,
                    labelBn = "অজানা ২য় সমান্তরাল বাহু",
                    formulaUsed = "b = ((২ × ক্ষেত্রফল) ÷ h) − a",
                    stepByStep = String.format(Locale.US, "((২ × %.2f) ÷ %.2f) − %.2f = %.3f ফু", areaSqFt, v1, v2, otherSide),
                    isValid = true
                )
            }
            UnknownSideShape.TRIANGLE_BASE_HEIGHT -> {
                if (v1 <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "ভূমি > ০ হতে হবে")
                val h = (2.0 * areaSqFt) / v1
                UnknownSideSolveResult(
                    solvedValue = h,
                    labelBn = "ত্রিভুজের লম্ব উচ্চতা",
                    formulaUsed = "h = (২ × ক্ষেত্রফল) ÷ ভূমি",
                    stepByStep = String.format(Locale.US, "(২ × %.2f) ÷ %.2f = %.3f ফু", areaSqFt, v1, h),
                    isValid = true
                )
            }
            UnknownSideShape.TRIANGLE_SAS -> {
                // v1 = known side a, v2 = angle C in deg (0..180)
                if (v1 <= 0.0 || v2 <= 0.0 || v2 >= 180.0) {
                    return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "সঠিক বাহু ও কোণ (১°–১৭৯°) দিন")
                }
                val rad = Math.toRadians(v2)
                val sideB = (2.0 * areaSqFt) / (v1 * sin(rad))
                val sideC = sqrt((v1 * v1 + sideB * sideB - 2.0 * v1 * sideB * cos(rad)).coerceAtLeast(0.0))
                UnknownSideSolveResult(
                    solvedValue = sideB,
                    secondaryValue = sideC,
                    labelBn = "২য় বাহু (b) ও বিপরীত ৩য় বাহু (c)",
                    formulaUsed = "b = 2A ÷ (a·sin C),  c = √(a² + b² − 2ab·cos C)",
                    stepByStep = String.format(Locale.US, "b = %.3f ফু | c = %.3f ফু", sideB, sideC),
                    isValid = true
                )
            }
            UnknownSideShape.PARALLELOGRAM -> {
                if (v1 <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "ভূমি > ০ হতে হবে")
                val h = areaSqFt / v1
                UnknownSideSolveResult(
                    solvedValue = h,
                    labelBn = "সামান্তরিকের লম্ব উচ্চতা",
                    formulaUsed = "h = ক্ষেত্রফল ÷ ভূমি",
                    stepByStep = String.format(Locale.US, "%.2f ÷ %.2f = %.3f ফু", areaSqFt, v1, h),
                    isValid = true
                )
            }
            UnknownSideShape.QUAD_WITH_DIAGONAL -> {
                // v1 = diagonal d, v2 = side1, v3 = side2 (adjacent on triangle 1), v4 = side3 (on triangle 2)
                val tri1 = LandCalculators.triangleHeronSqFeet(v2, v3, v1)
                    ?: return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "প্রথম ত্রিভুজের বাহু ও কর্ণ দিয়ে ত্রিভুজ গঠন সম্ভব নয়")
                val remArea = areaSqFt - tri1
                if (remArea <= 0.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "প্রথম ত্রিভুজের ক্ষেত্রফলই মোট ক্ষেত্রফলের সমান বা বেশি")
                // Area2 = 0.5 * v1 * v4 * sin(alpha) => sin(alpha) = 2*remArea/(v1*v4)
                val sinAlpha = (2.0 * remArea) / (v1 * v4.coerceAtLeast(1e-6))
                if (sinAlpha > 1.0) return UnknownSideSolveResult(0.0, null, "ত্রুটি", "", "", false, "৩য় বাহু দিয়ে উক্ত ক্ষেত্রফল গঠন অসম্ভব")
                val alpha = kotlin.math.asin(sinAlpha.coerceIn(-1.0, 1.0))
                val side4 = sqrt((v1 * v1 + v4 * v4 - 2.0 * v1 * v4 * cos(alpha)).coerceAtLeast(0.0))
                UnknownSideSolveResult(
                    solvedValue = side4,
                    labelBn = "অজানা ৪র্থ বাহুর সম্ভাব্য দৈর্ঘ্য",
                    formulaUsed = "Δ₁ (Heron) বাদে Δ₂ = ১/২·d·c·sin α → x = √(d² + c² − 2dc·cos α)",
                    stepByStep = String.format(Locale.US, "Δ₁=%.2f ব.ফু, Δ₂=%.2f ব.ফু → ৪র্থ বাহু ≈ %.3f ফু", tri1, remArea, side4),
                    isValid = true
                )
            }
        }
    }

    fun solveCircleSector(radiusFt: Double, centralAngleDeg: Double): CircleSectorResult {
        val r = radiusFt.coerceAtLeast(0.0)
        val deg = centralAngleDeg.coerceIn(0.0, 360.0)
        val rad = Math.toRadians(deg)
        val fullArea = PI * r * r
        val circum = 2.0 * PI * r
        val arc = r * rad
        val chord = 2.0 * r * sin(rad * 0.5)
        val sector = 0.5 * r * r * rad
        val segment = 0.5 * r * r * (rad - sin(rad))
        return CircleSectorResult(
            radiusFt = r,
            centralAngleDeg = deg,
            fullCircleAreaSqFt = fullArea,
            circumferenceFt = circum,
            arcLengthFt = arc,
            chordLengthFt = chord,
            sectorAreaSqFt = sector,
            segmentAreaSqFt = segment,
            sectorDecimal = sector / LandCalculators.SQ_FT_PER_DECIMAL
        )
    }

    // ==================== 6. OFFLINE FORMULA CHEAT-SHEET ====================

    val OFFLINE_FORMULA_CHEAT_SHEET: List<FormulaCheatSheetEntry> = listOf(
        FormulaCheatSheetEntry(
            id = "heron",
            titleBn = "বিষমবাহু ত্রিভুজ (হেরনের সূত্র — ১০০% নির্ভুল)",
            formulaText = "s = (a + b + c) ÷ 2\nক্ষেত্রফল = √[s(s−a)(s−b)(s−c)]",
            workedExampleBn = "বাহু ৩০', ৪০', ৫০' হলে s = (৩০+৪০+৫০)÷২ = ৬০' → ক্ষেত্রফল = √(৬০×৩০×২০×১০) = ৬০০ ব.ফু. = ১.৩৭৭ শতক।",
            verificationNote = "যেকোনো অনিয়মিত জমি কর্ণ দিয়ে ত্রিভুজে ভেঙে এই সূত্রে মাপাই আমিন জরিপের সর্বোচ্চ নির্ভুল পদ্ধতি।"
        ),
        FormulaCheatSheetEntry(
            id = "quad_diag",
            titleBn = "চতুর্ভুজ (৪ বাহু + কর্ণ বনাম গড় পদ্ধতি)",
            formulaText = "নির্ভুল: Δ₁(উত্তর, পূর্ব, কর্ণ) + Δ₂(দক্ষিণ, পশ্চিম, কর্ণ)\nআনুমানিক গড়: ((উত্তর+দক্ষিণ)÷2) × ((পূর্ব+পশ্চিম)÷2)",
            workedExampleBn = "সকল কোণ ৯০° না হলে গড় পদ্ধতিতে জমি বেশি দেখায়! তাই সবসময় কমপক্ষে ১টি কর্ণ মেপে ২ ত্রিভুজে মাপবেন।",
            verificationNote = "গড় পদ্ধতি কেবল আয়তক্ষেত্রের কাছাকাছি জমিতে দ্রুত ধারণার জন্য।"
        ),
        FormulaCheatSheetEntry(
            id = "trapezium",
            titleBn = "ট্র্যাপিজিয়াম (২ সমান্তরাল বাহু + লম্ব দূরত্ব)",
            formulaText = "ক্ষেত্রফল = ((a + b) ÷ 2) × h",
            workedExampleBn = "রাস্তার পাশের ৪৫' ও অপর পাশ ৫৫' সমান্তরাল এবং লম্ব প্রস্থ ৪০' হলে: ((৪৫+৫৫)÷২)×৪০ = ২০০০ ব.ফু. = ৪.৫৯১ শতক।",
            verificationNote = "উচ্চতা (h) অবশ্যই সমান্তরাল বাহুদ্বয়ের উপর লম্ব (৯০°) হতে হবে।"
        ),
        FormulaCheatSheetEntry(
            id = "circle_arc",
            titleBn = "বৃত্ত, বৃত্তচাপ ও সেক্টর (পুকুর/গোল জমি)",
            formulaText = "পূর্ণ বৃত্ত = πr² | পরিধি = 2πr\nসেক্টর = (θ° ÷ 360°) × πr² | বৃত্তচাপ = (θ° ÷ 360°) × 2πr",
            workedExampleBn = "ব্যাসার্ধ ২৮ ফুট, কোণ ৯০° হলে: সেক্টর = (৯০÷৩৬০)×৩.১৪১৬×২৮² = ৬১৫.৭৫ ব.ফু. = ১.৪১৪ শতক।",
            verificationNote = "বাঁকা কোণা বা পুকুর পাড়ের জমি মাপতে সেক্টর ও সেগমেন্ট সূত্র কার্যকর।"
        ),
        FormulaCheatSheetEntry(
            id = "shoelace",
            titleBn = "বহুভুজ কো-অর্ডিনেট (Shoelace / Survey Formula)",
            formulaText = "Area = ½ | Σ (xᵢ·yᵢ₊₁ − xᵢ₊₁·yᵢ) |",
            workedExampleBn = "যেকোনো N-বাহুর নকশা বা GPS/টোটাল স্টেশন পয়েন্ট থেকে নির্ভুল ক্ষেত্রফল।",
            verificationNote = "অ্যাপের ম্যাপ ও নকশা ক্যানভাসে এটি স্বয়ংক্রিয়ভাবে ব্যবহৃত হয়।"
        ),
        FormulaCheatSheetEntry(
            id = "ana_gonda",
            titleBn = "আনা-গণ্ডা-কড়া-ক্রান্তি-তিল সম্পর্ক",
            formulaText = "১ টাকা (সম্পূর্ণ) = ১৬ আনা = ৩২০ গণ্ডা = ১২৮০ কড়া = ৩৮৪০ ক্রান্তি = ৭৬,৮০০ তিল",
            workedExampleBn = "৪ আনা ১০ গণ্ডা = (৪×৪৮০০ + ১০×২৪০) ÷ ৭৬৮০০ = ০.২৮১২৫ (২৮.১২৫%)।",
            verificationNote = "CS/SA/RS খতিয়ানে মালিকের অংশ বের করতে এই রূপান্তর অপরিহার্য।"
        )
    )
}
