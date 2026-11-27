package com.example.data

import android.content.Context
import com.example.engine.DocumentAndReportEngine
import com.example.engine.SurveyMathProEngine
import com.example.model.CustomFieldType
import com.example.model.CustomFormFieldDef
import com.example.model.CustomFormTemplate
import com.example.model.DeedDraftDocument
import com.example.model.DeedTemplateType
import com.example.model.FieldSurveyTrack
import com.example.model.FinancialRateConfig
import com.example.model.GeoWaypoint
import com.example.model.LandDivisionResultRecord
import com.example.model.LandVaultBackupPayload
import com.example.model.OcrScannedDocument
import com.example.model.RegionalUnitProfile
import com.example.model.SurveyReportConfig
import com.example.model.SurveyReportType
import com.example.model.UnifiedSurveyProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class WorkstationSnapshot(
    val regionalUnits: List<RegionalUnitProfile> = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES,
    val unifiedProjects: List<UnifiedSurveyProject> = emptyList(),
    val deedDrafts: List<DeedDraftDocument> = emptyList(),
    val ocrDocuments: List<OcrScannedDocument> = emptyList(),
    val fieldTracks: List<FieldSurveyTrack> = emptyList(),
    val divisionRecords: List<LandDivisionResultRecord> = emptyList(),
    val reportConfigs: List<SurveyReportConfig> = emptyList(),
    val customTemplates: List<CustomFormTemplate> = defaultFormTemplates(),
    val financialRate: FinancialRateConfig = defaultFinancialRate(),
    val vaultPinHash: String = ""
)

private fun defaultFormTemplates(): List<CustomFormTemplate> = listOf(
    CustomFormTemplate(
        id = "tpl_bd_plot_calc",
        title = "৪-বাহু জমি ও মূল্য মূল্যায়ন টেমপ্লেট",
        description = "উত্তর/দক্ষিণ/পূর্ব/পশ্চিম মাপ ও শতক প্রতি দর দিয়ে অটো হিসাব",
        fields = listOf(
            CustomFormFieldDef("f1", "n", "উত্তর বাহু (ফুট)", CustomFieldType.NUMBER, "60"),
            CustomFormFieldDef("f2", "s", "দক্ষিণ বাহু (ফুট)", CustomFieldType.NUMBER, "58"),
            CustomFormFieldDef("f3", "e", "পূর্ব বাহু (ফুট)", CustomFieldType.NUMBER, "40"),
            CustomFormFieldDef("f4", "w", "পশ্চিম বাহু (ফুট)", CustomFieldType.NUMBER, "42"),
            CustomFormFieldDef("f5", "rate", "শতক প্রতি দর (টাকা)", CustomFieldType.NUMBER, "200000"),
            CustomFormFieldDef("f6", "area_dec", "আনুমানিক জমি (শতাংশ)", CustomFieldType.FORMULA, "", formulaExpr = "(((n+s)/2)*((e+w)/2))/435.6"),
            CustomFormFieldDef("f7", "total_val", "মোট মূল্য (টাকা)", CustomFieldType.FORMULA, "", formulaExpr = "((((n+s)/2)*((e+w)/2))/435.6)*rate"),
            CustomFormFieldDef("f8", "class", "জমির শ্রেণি", CustomFieldType.DROPDOWN, "নাল", "নাল,ভিটা,বাড়ি,পুকুর,বাণিজ্যিক")
        )
    )
)

private fun defaultFinancialRate() = FinancialRateConfig(
    id = "bd_rate_2026",
    locationLabel = "বাংলাদেশ সাব-রেজিস্ট্রি অফিস (ইউনিয়ন/উপজেলা)",
    effectiveDate = "2026-01-01",
    stampDutyPct = 1.5,
    registrationFeePct = 1.0,
    localGovtTaxPct = 3.0,
    sourceGainTaxPct = 4.0,
    mutationFeeBdt = 1170.0,
    landTaxPerDecimalBdt = 10.0,
    mouzaRatePerDecimalBdt = 250000.0
)

class WorkstationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val storeFile = File(appContext.filesDir, "bhumi_workstation_vault.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    private val _snapshot = MutableStateFlow(loadFromDisk())
    val snapshot: StateFlow<WorkstationSnapshot> = _snapshot.asStateFlow()

    @kotlinx.serialization.Serializable
    private data class DiskModel(
        val regionalUnits: List<RegionalUnitProfile> = SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES,
        val unifiedProjects: List<UnifiedSurveyProject> = emptyList(),
        val deedDrafts: List<DeedDraftDocument> = emptyList(),
        val ocrDocuments: List<OcrScannedDocument> = emptyList(),
        val fieldTracks: List<FieldSurveyTrack> = emptyList(),
        val divisionRecords: List<LandDivisionResultRecord> = emptyList(),
        val reportConfigs: List<SurveyReportConfig> = emptyList(),
        val customTemplates: List<CustomFormTemplate> = defaultFormTemplates(),
        val financialRate: FinancialRateConfig = defaultFinancialRate(),
        val vaultPinHash: String = ""
    )

    private fun loadFromDisk(): WorkstationSnapshot {
        if (!storeFile.exists()) return WorkstationSnapshot()
        return runCatching {
            val dm = json.decodeFromString<DiskModel>(storeFile.readText(Charsets.UTF_8))
            WorkstationSnapshot(
                regionalUnits = dm.regionalUnits.ifEmpty { SurveyMathProEngine.DEFAULT_REGIONAL_PROFILES },
                unifiedProjects = dm.unifiedProjects,
                deedDrafts = dm.deedDrafts,
                ocrDocuments = dm.ocrDocuments,
                fieldTracks = dm.fieldTracks,
                divisionRecords = dm.divisionRecords,
                reportConfigs = dm.reportConfigs,
                customTemplates = dm.customTemplates.ifEmpty { defaultFormTemplates() },
                financialRate = dm.financialRate,
                vaultPinHash = dm.vaultPinHash
            )
        }.getOrDefault(WorkstationSnapshot())
    }

    private fun persist(next: WorkstationSnapshot) {
        _snapshot.value = next
        runCatching {
            storeFile.parentFile?.mkdirs()
            val dm = DiskModel(
                regionalUnits = next.regionalUnits,
                unifiedProjects = next.unifiedProjects,
                deedDrafts = next.deedDrafts,
                ocrDocuments = next.ocrDocuments,
                fieldTracks = next.fieldTracks,
                divisionRecords = next.divisionRecords,
                reportConfigs = next.reportConfigs,
                customTemplates = next.customTemplates,
                financialRate = next.financialRate,
                vaultPinHash = next.vaultPinHash
            )
            storeFile.writeText(json.encodeToString(dm), Charsets.UTF_8)
        }
    }

    fun saveRegionalUnitProfile(profile: RegionalUnitProfile) {
        val cur = _snapshot.value
        val updated = (listOf(profile) + cur.regionalUnits.filterNot { it.id == profile.id }).take(20)
        persist(cur.copy(regionalUnits = updated))
    }

    fun saveUnifiedProject(project: UnifiedSurveyProject) {
        val cur = _snapshot.value
        val updated = (listOf(project) + cur.unifiedProjects.filterNot { it.id == project.id }).take(80)
        persist(cur.copy(unifiedProjects = updated))
    }

    fun deleteUnifiedProject(id: String) {
        val cur = _snapshot.value
        persist(cur.copy(unifiedProjects = cur.unifiedProjects.filterNot { it.id == id }))
    }

    fun saveDeedDraft(doc: DeedDraftDocument) {
        val cur = _snapshot.value
        val updated = (listOf(doc) + cur.deedDrafts.filterNot { it.id == doc.id }).take(60)
        persist(cur.copy(deedDrafts = updated))
    }

    fun deleteDeedDraft(id: String) {
        val cur = _snapshot.value
        persist(cur.copy(deedDrafts = cur.deedDrafts.filterNot { it.id == id }))
    }

    fun saveOcrDocument(doc: OcrScannedDocument) {
        val cur = _snapshot.value
        val updated = (listOf(doc) + cur.ocrDocuments.filterNot { it.id == doc.id }).take(60)
        persist(cur.copy(ocrDocuments = updated))
    }

    fun saveFieldTrack(track: FieldSurveyTrack) {
        val cur = _snapshot.value
        val updated = (listOf(track) + cur.fieldTracks.filterNot { it.id == track.id }).take(60)
        persist(cur.copy(fieldTracks = updated))
    }

    fun saveDivisionRecord(rec: LandDivisionResultRecord) {
        val cur = _snapshot.value
        val updated = (listOf(rec) + cur.divisionRecords.filterNot { it.id == rec.id }).take(50)
        persist(cur.copy(divisionRecords = updated))
    }

    fun saveReportConfig(cfg: SurveyReportConfig) {
        val cur = _snapshot.value
        val updated = (listOf(cfg) + cur.reportConfigs.filterNot { it.id == cfg.id }).take(50)
        persist(cur.copy(reportConfigs = updated))
    }

    fun saveCustomTemplate(tpl: CustomFormTemplate) {
        val cur = _snapshot.value
        val updated = (listOf(tpl) + cur.customTemplates.filterNot { it.id == tpl.id }).take(40)
        persist(cur.copy(customTemplates = updated))
    }

    fun saveFinancialRate(rate: FinancialRateConfig) {
        persist(_snapshot.value.copy(financialRate = rate))
    }

    fun setVaultPin(pin: String) {
        val hash = if (pin.isBlank()) "" else DocumentAndReportEngine.sha256Hex("BHUMI_PIN_SALT:$pin")
        persist(_snapshot.value.copy(vaultPinHash = hash))
    }

    fun verifyVaultPin(pin: String): Boolean {
        val expected = _snapshot.value.vaultPinHash
        if (expected.isBlank()) return true
        return DocumentAndReportEngine.sha256Hex("BHUMI_PIN_SALT:$pin") == expected
    }

    fun seedSampleBangladeshProject() {
        val cur = _snapshot.value
        val sampleProj = UnifiedSurveyProject(
            id = "sample_bd_pubail_2026",
            projectName = "নমুনা প্রকল্প: পুবাইল মৌজা (দাগ ১০৪২-১০৪৩)",
            ownerName = "মোঃ রফিকুল ইসলাম (নমুনা)",
            ownerPhone = "01620230864",
            district = "গাজীপুর",
            upazila = "গাজীপুর সদর",
            mouza = "পুবাইল",
            jlNumber = "৮৫",
            khatianNumbers = "বিএস ৪১২ (সাবেক আরএস ২১০)",
            dagNumbers = "১০৪২, ১০৪৩",
            plotCount = 2,
            totalDecimalArea = 33.5,
            notes = "নমুনা প্রজেক্ট — অ্যাপের সব মডিউল পরীক্ষা করার জন্য। এটি কোনো প্রকৃত সরকারি রেকর্ড নয়।",
            tags = listOf("নমুনা", "বিএস", "ফিল্ড জরিপ")
        )
        val sampleTrack = FieldSurveyTrack(
            id = "sample_track_pubail",
            title = "নমুনা GPS ট্র্যাক — পুবাইল দাগ ১০৪২",
            mode = "Sample Reference",
            waypoints = listOf(
                GeoWaypoint("w1", "উত্তর-পশ্চিম কোণ", 23.93010, 90.47110, 12.0, 4.0, 1.1, 1.5, "Sample GPS"),
                GeoWaypoint("w2", "উত্তর-পূর্ব কোণ", 23.93012, 90.47148, 12.2, 4.2, 1.1, 1.5, "Sample GPS"),
                GeoWaypoint("w3", "দক্ষিণ-পূর্ব কোণ", 23.92984, 90.47146, 11.9, 3.8, 1.0, 1.4, "Sample GPS"),
                GeoWaypoint("w4", "দক্ষিণ-পশ্চিম কোণ", 23.92982, 90.47108, 12.0, 4.0, 1.1, 1.5, "Sample GPS")
            ),
            areaSqFt = 14450.0,
            perimeterFt = 480.0
        )
        val sampleDeed = DeedDraftDocument(
            id = "sample_deed_pubail",
            templateType = DeedTemplateType.BONTANNAMA_PARTITION,
            title = "নমুনা বণ্টননামা খসড়া — পুবাইল মৌজা",
            firstPartyName = "মোঃ রফিকুল ইসলাম",
            firstPartyFather = "মৃত করিম উদ্দিন",
            firstPartyAddress = "পুবাইল, গাজীপুর সদর",
            secondPartyName = "মোঃ শফিকুল ইসলাম",
            secondPartyFather = "মৃত করিম উদ্দিন",
            secondPartyAddress = "পুবাইল, গাজীপুর সদর",
            district = "গাজীপুর",
            upazila = "গাজীপুর সদর",
            mouza = "পুবাইল",
            jlNumber = "৮৫",
            khatianNumber = "৪১২",
            dagNumber = "১০৪২",
            areaDecimal = "16.75",
            boundaryNorth = "হাল দাগ ১০৪১",
            boundarySouth = "হাল ১২ ফুট রাস্তা",
            boundaryEast = "দাগ ১০৪৩",
            boundaryWest = "সরকারি খাল"
        )
        val sampleReport = SurveyReportConfig(
            id = "sample_report_pubail",
            reportType = SurveyReportType.MEASUREMENT_REPORT,
            title = "নমুনা জমি পরিমাপ প্রতিবেদন — পুবাইল দাগ ১০৪২",
            ownerName = "মোঃ রফিকুল ইসলাম",
            district = "গাজীপুর",
            upazila = "গাজীপুর সদর",
            mouza = "পুবাইল",
            jlNumber = "৮৫",
            khatianDagSummary = "বিএস খতিয়ান ৪১২ • দাগ ১০৪২",
            areaSummaryText = "৩৩.৫০ শতাংশ (২০.৩০ কাঠা)",
            sideLengthsText = "উঃ ১২৬' • দঃ ১২৪' • পূঃ ১১৮' • পঃ ১১৬'",
            coordinatesText = "UTM 46N: 242712E, 2648310N"
        )
        persist(
            cur.copy(
                unifiedProjects = (listOf(sampleProj) + cur.unifiedProjects.filterNot { it.id == sampleProj.id }),
                fieldTracks = (listOf(sampleTrack) + cur.fieldTracks.filterNot { it.id == sampleTrack.id }),
                deedDrafts = (listOf(sampleDeed) + cur.deedDrafts.filterNot { it.id == sampleDeed.id }),
                reportConfigs = (listOf(sampleReport) + cur.reportConfigs.filterNot { it.id == sampleReport.id })
            )
        )
    }

    suspend fun exportBackupJson(appPrefs: AppPreferences): String {
        val snap = _snapshot.value
        val payload = LandVaultBackupPayload(
            regionalUnits = snap.regionalUnits,
            unifiedProjects = snap.unifiedProjects,
            deedDrafts = snap.deedDrafts,
            ocrDocuments = snap.ocrDocuments,
            fieldTracks = snap.fieldTracks,
            divisionRecords = snap.divisionRecords,
            reportConfigs = snap.reportConfigs,
            customTemplates = snap.customTemplates,
            customKhatians = appPrefs.customKhatiansFlow.first(),
            savedProjects = appPrefs.savedProjectsFlow.first()
        )
        return json.encodeToString(payload)
    }

    suspend fun restoreFromBackupJson(rawJson: String, appPrefs: AppPreferences): Pair<Boolean, String> {
        return runCatching {
            val payload = json.decodeFromString<LandVaultBackupPayload>(rawJson)
            val cur = _snapshot.value
            persist(
                cur.copy(
                    regionalUnits = payload.regionalUnits.ifEmpty { cur.regionalUnits },
                    unifiedProjects = payload.unifiedProjects,
                    deedDrafts = payload.deedDrafts,
                    ocrDocuments = payload.ocrDocuments,
                    fieldTracks = payload.fieldTracks,
                    divisionRecords = payload.divisionRecords,
                    reportConfigs = payload.reportConfigs,
                    customTemplates = payload.customTemplates.ifEmpty { cur.customTemplates }
                )
            )
            payload.customKhatians.forEach { appPrefs.saveCustomKhatian(it) }
            payload.savedProjects.forEach { appPrefs.saveSurveyProject(it) }
            true to "সফলভাবে রিস্টোর হয়েছে (${payload.unifiedProjects.size} প্রকল্প, ${payload.customKhatians.size} খতিয়ান)"
        }.getOrElse { err ->
            false to "রিস্টোর ব্যর্থ: ${err.localizedMessage ?: "ভুল ফরম্যাট"}"
        }
    }

    companion object {
        @Volatile
        private var instance: WorkstationRepository? = null

        fun getInstance(context: Context): WorkstationRepository {
            return instance ?: synchronized(this) {
                instance ?: WorkstationRepository(context).also { instance = it }
            }
        }
    }
}
