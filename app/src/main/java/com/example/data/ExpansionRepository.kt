package com.example.data

import android.content.Context
import com.example.model.CustomServiceDefinition
import com.example.model.CustomWorkflowStep
import com.example.model.DagGenealogyRecord
import com.example.model.EditableLegalClause
import com.example.model.FieldBookEntry
import com.example.model.KhajnaHoldingRecord
import com.example.model.LandServiceWorkflowKind
import com.example.model.RevenueCaseRecord
import com.example.model.ServiceApplicationCase
import com.example.model.SurveyInstrumentItem
import com.example.model.SurveyQuotationRecord
import com.example.model.ToolCalculationHistoryEntry
import com.example.model.UniversalArchiveDocument
import com.example.model.WorkflowStepKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ExpansionSnapshot(
    val dagGenealogies: List<DagGenealogyRecord> = defaultDagGenealogies(),
    val fieldBookEntries: List<FieldBookEntry> = emptyList(),
    val serviceCases: List<ServiceApplicationCase> = emptyList(),
    val khajnaHoldings: List<KhajnaHoldingRecord> = defaultHoldings(),
    val revenueCases: List<RevenueCaseRecord> = emptyList(),
    val legalClauses: List<EditableLegalClause> = defaultClauses(),
    val instruments: List<SurveyInstrumentItem> = defaultInstruments(),
    val quotations: List<SurveyQuotationRecord> = emptyList(),
    val customServices: List<CustomServiceDefinition> = defaultCustomServices(),
    val archiveDocs: List<UniversalArchiveDocument> = defaultArchiveDocs(),
    val calcHistory: List<ToolCalculationHistoryEntry> = emptyList()
)

private fun defaultDagGenealogies() = listOf(
    DagGenealogyRecord(
        id = "dg_sample_1",
        mouza = "পুবাইল (নমুনা)",
        jlNumber = "৮৫",
        relationType = "বিভাজন (Split)",
        formerSurvey = "আরএস (RS)",
        formerDagNumbers = "৫১২",
        currentSurvey = "বিএস (BS)",
        currentDagNumbers = "১০৪২, ১০৪৩",
        khatianLink = "সাবেক ২১০ → হাল ৪১২",
        ownerLink = "মোঃ রফিকুল ইসলাম ও শরিকগণ",
        transferDecimal = 33.5,
        verifiedManually = true,
        notes = "নমুনা রেকর্ড — একই নম্বর মানেই একই জমি নয়; মৌজা ম্যাপ ও পর্চা মিলিয়ে যাচাই করুন।"
    )
)

private fun defaultHoldings() = listOf(
    KhajnaHoldingRecord(
        id = "kh_hold_1",
        holdingNo = "নমুনা-হোল্ডিং-২০১",
        ownerName = "মোঃ রফিকুল ইসলাম (নমুনা)",
        mouza = "পুবাইল",
        jlNo = "৮৫",
        khatianNo = "৪১২",
        dagNo = "১০৪২",
        areaDecimal = 20.0,
        annualRatePerDecimalBdt = 10.0,
        arrearsYears = 2,
        dakhilaRef = "",
        receiptVerified = false
    )
)

private fun defaultClauses() = listOf(
    EditableLegalClause("cl_sale", "বিক্রয়", "নিষ্কণ্টক স্বত্ব হস্তান্তর ধারা", "অদ্য বিক্রেতা নিম্ন তফসিলের ভূমি সম্পূর্ণ নিষ্কণ্টক অবস্থায় ক্রেতার বরাবর বিক্রয় ও দখল হস্তান্তর করিলেন।", "Vendor conveys absolute, encumbrance-free title and delivers peaceful possession."),
    EditableLegalClause("cl_indemnity", "ক্ষতিপূরণ (Indemnity)", "স্বত্বগত ত্রুটির দায়বদ্ধতা", "ভবিষ্যতে পূর্ব-স্বত্ব সংক্রান্ত কোনো দাবি বা আপত্তি উত্থাপিত হইলে প্রথম পক্ষ নিজ দায়িত্বে ও খরচে উহা নিষ্পত্তি করিবেন।", "Executant indemnifies transferee against any prior title defect or claim."),
    EditableLegalClause("cl_partition", "বণ্টন", "সহ-শরিক আপস বণ্টন ধারা", "সকল অংশীদার পৃথক চৌহদ্দি অনুযায়ী নিজ নিজ হিস্যার দখল ও নামজারি করাইয়া লইতে পারিবেন।", "Each co-sharer may mutate and possess their demarcated share separately."),
    EditableLegalClause("cl_heba", "হেবা", "বিনিময়হীন স্বেচ্ছা দান", "রক্তসম্পর্কীয় স্নেহের বশবর্তী হইয়া কোনো আর্থিক বিনিময় ছাড়াই এই হেবা দলিল সম্পাদিত হইল।", "Executed voluntarily out of natural love and affection without monetary consideration."),
    EditableLegalClause("cl_possession", "দখল", "সরেজমিন দখল ও সীমানা খুঁটি", "সরেজমিনে সীমানা খুঁটি স্থাপনপূর্বক পরিমাপকৃত জমি বুঝাইয়া দেওয়া হইল।", "Physical demarcation pillars installed and possession delivered on spot."),
    EditableLegalClause("cl_dispute", "বিরোধ নিষ্পত্তি", "আপস ও দেওয়ানি এখতিয়ার", "চুক্তি সংক্রান্ত কোনো বিরোধ প্রথমে সালিসের মাধ্যমে এবং ব্যর্থতায় সংশ্লিষ্ট দেওয়ানি আদালতে নিষ্পত্তি হইবে।", "Disputes shall first undergo amicable mediation failing which competent civil courts apply.")
)

private fun defaultInstruments() = listOf(
    SurveyInstrumentItem("inst_ts", "Leica / South Total Station", "Total Station", "TS-BD-2026-01", "2026-01-15", "পুবাইল জরিপ", "ক্যালিব্রেটেড ও সচল"),
    SurveyInstrumentItem("inst_gnss", "Dual-Freq RTK GNSS Rover", "GNSS RTK", "RTK-BD-8821", "2026-02-01", "নমুনা ফিল্ড ট্র্যাক", "NMEA/RTK প্রস্তুত"),
    SurveyInstrumentItem("inst_lvl", "Auto Level (32x)", "Auto Level", "LVL-2025-44", "2026-01-10", "কমন স্টোর", "কলিমেশন যাচাইকৃত")
)

private fun defaultCustomServices() = listOf(
    CustomServiceDefinition(
        id = "cs_namjari_helper",
        serviceName = "ই-নামজারি (কেনা জমি) অটোমেশন ওয়ার্কফ্লো",
        category = "নামজারি ও রেকর্ড",
        description = "রেজিস্ট্রি দলিল থেকে নতুন খতিয়ান ও DCR সংগ্রহের সম্পূর্ণ ধাপ",
        officialPortalUrl = "https://mutation.land.gov.bd/",
        requiredDocsCsv = "রেজিস্ট্রিকৃত দলিল, পূর্বের খতিয়ান, খাজনা দাখিলা, NID, পাসপোর্ট ছবি",
        steps = listOf(
            CustomWorkflowStep("s1", "আবেদনকারীর তথ্য ও দলিল নম্বর এন্ট্রি", WorkflowStepKind.INFO_COLLECTION, completed = true),
            CustomWorkflowStep("s2", "মূল কাগজপত্র ও তফসিল মিলিয়ে যাচাই", WorkflowStepKind.DOC_VERIFICATION),
            CustomWorkflowStep("s3", "সার্ভেয়ার প্রতিবেদন: দখল সঠিক আছে কি?", WorkflowStepKind.DECISION_YES_NO),
            CustomWorkflowStep("s4", "আবেদন ফি (৭০) ও DCR ফি (১,১০০) অনলাইন পেমেন্ট", WorkflowStepKind.PAYMENT_STAGE),
            CustomWorkflowStep("s5", "এসিল্যান্ড শুনানি ও হাজিরা", WorkflowStepKind.HEARING_STAGE),
            CustomWorkflowStep("s6", "চূড়ান্ত নামজারি খতিয়ান ও ডিসিআর সংরক্ষণ", WorkflowStepKind.FINAL_ARCHIVE)
        )
    )
)

private fun defaultArchiveDocs() = listOf(
    UniversalArchiveDocument(
        id = "arch_sample_1",
        docType = "খতিয়ান",
        title = "নমুনা বিএস খতিয়ান ৪১২ — পুবাইল মৌজা",
        ownerName = "মোঃ রফিকুল ইসলাম",
        mouza = "পুবাইল",
        dagNo = "১০৪২",
        khatianNo = "৪১২",
        docDate = "2026-01-15",
        projectRef = "পুবাইল নমুনা প্রজেক্ট",
        verificationStatus = "নমুনা তথ্য (সরকারি সনদ নয়)",
        summaryContent = "দাগ ১০৪২ (নাল), পরিমাণ ৩৩.৫০ শতাংশ।"
    )
)

class ExpansionRepository(context: Context) {

    private val storeFile = File(context.applicationContext.filesDir, "bhumi_expansion_vault.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    private val _snapshot = MutableStateFlow(loadFromDisk())
    val snapshot: StateFlow<ExpansionSnapshot> = _snapshot.asStateFlow()

    private fun loadFromDisk(): ExpansionSnapshot {
        if (!storeFile.exists()) return ExpansionSnapshot()
        return runCatching {
            json.decodeFromString<ExpansionSnapshot>(storeFile.readText(Charsets.UTF_8))
        }.getOrDefault(ExpansionSnapshot())
    }

    private fun persist(next: ExpansionSnapshot) {
        _snapshot.value = next
        runCatching {
            storeFile.parentFile?.mkdirs()
            storeFile.writeText(json.encodeToString(next), Charsets.UTF_8)
        }
    }

    fun saveDagGenealogy(rec: DagGenealogyRecord) =
        persist(_snapshot.value.let { it.copy(dagGenealogies = (listOf(rec) + it.dagGenealogies.filterNot { x -> x.id == rec.id }).take(100)) })

    fun saveFieldBookEntry(entry: FieldBookEntry) =
        persist(_snapshot.value.let { it.copy(fieldBookEntries = (listOf(entry) + it.fieldBookEntries.filterNot { x -> x.id == entry.id }).take(200)) })

    fun saveServiceCase(c: ServiceApplicationCase) =
        persist(_snapshot.value.let { it.copy(serviceCases = (listOf(c) + it.serviceCases.filterNot { x -> x.id == c.id }).take(100)) })

    fun saveKhajnaHolding(h: KhajnaHoldingRecord) =
        persist(_snapshot.value.let { it.copy(khajnaHoldings = (listOf(h) + it.khajnaHoldings.filterNot { x -> x.id == h.id }).take(100)) })

    fun saveRevenueCase(r: RevenueCaseRecord) =
        persist(_snapshot.value.let { it.copy(revenueCases = (listOf(r) + it.revenueCases.filterNot { x -> x.id == r.id }).take(100)) })

    fun saveLegalClause(c: EditableLegalClause) =
        persist(_snapshot.value.let { it.copy(legalClauses = (listOf(c) + it.legalClauses.filterNot { x -> x.id == c.id }).take(120)) })

    fun saveInstrument(item: SurveyInstrumentItem) =
        persist(_snapshot.value.let { it.copy(instruments = (listOf(item) + it.instruments.filterNot { x -> x.id == item.id }).take(80)) })

    fun saveQuotation(q: SurveyQuotationRecord) =
        persist(_snapshot.value.let { it.copy(quotations = (listOf(q) + it.quotations.filterNot { x -> x.id == q.id }).take(80)) })

    fun saveCustomService(svc: CustomServiceDefinition) =
        persist(_snapshot.value.let { it.copy(customServices = (listOf(svc) + it.customServices.filterNot { x -> x.id == svc.id }).take(60)) })

    fun saveArchiveDoc(doc: UniversalArchiveDocument) =
        persist(_snapshot.value.let { it.copy(archiveDocs = (listOf(doc) + it.archiveDocs.filterNot { x -> x.id == doc.id }).take(150)) })

    fun deleteArchiveDoc(id: String) =
        persist(_snapshot.value.let { it.copy(archiveDocs = it.archiveDocs.filterNot { x -> x.id == id }) })

    fun logCalculation(moduleTag: String, summaryTitle: String, resultSummary: String) {
        val entry = ToolCalculationHistoryEntry(
            id = "hist_${System.currentTimeMillis()}_${moduleTag}",
            moduleTag = moduleTag,
            summaryTitle = summaryTitle,
            resultSummary = resultSummary
        )
        persist(_snapshot.value.let { it.copy(calcHistory = (listOf(entry) + it.calcHistory).take(120)) })
    }

    companion object {
        @Volatile private var instance: ExpansionRepository? = null
        fun getInstance(context: Context): ExpansionRepository =
            instance ?: synchronized(this) { instance ?: ExpansionRepository(context).also { instance = it } }
    }
}
