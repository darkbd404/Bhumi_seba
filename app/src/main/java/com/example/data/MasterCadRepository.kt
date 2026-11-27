package com.example.data

import android.content.Context
import com.example.engine.CadAndDimensionEngine
import com.example.model.CadDimensionEntity
import com.example.model.CadDimensionType
import com.example.model.CadEntity
import com.example.model.CadEntityType
import com.example.model.CadLayerConfig
import com.example.model.CadStyleSettings
import com.example.model.CogoPoint
import com.example.model.DeedTemplateType
import com.example.model.HistoricalDeedRecord
import com.example.model.MeasurementDisplayUnit
import com.example.model.PolygonVertex
import com.example.model.RoadAlignmentSpec
import com.example.model.TaxRateSchedule
import com.example.model.UnionRegisterEntry
import com.example.model.UnionRegisterKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MasterCadWorkspaceSnapshot(
    val projectTitle: String = "পুবাইল মৌজা — মাস্টার জরিপ প্রজেক্ট",
    val mouzaInfo: String = "মৌজা: পুবাইল, জে.এল. ৮৫, দাগ ১০৪২",
    val layers: List<CadLayerConfig> = CadAndDimensionEngine.defaultLayers(),
    val entities: List<CadEntity> = emptyList(),
    val dimensions: List<CadDimensionEntity> = emptyList(),
    val styleSettings: CadStyleSettings = CadStyleSettings(),
    val cogoPoints: List<CogoPoint> = emptyList(),
    val historicalDeeds: List<HistoricalDeedRecord> = emptyList(),
    val unionRegisters: List<UnionRegisterEntry> = emptyList(),
    val taxRateSchedule: TaxRateSchedule = TaxRateSchedule(),
    val roadSpec: RoadAlignmentSpec = RoadAlignmentSpec()
)

class MasterCadRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("bhumi_master_cad_v3", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _snapshot = MutableStateFlow(loadOrCreateDefault())
    val snapshot: StateFlow<MasterCadWorkspaceSnapshot> = _snapshot.asStateFlow()

    private fun loadOrCreateDefault(): MasterCadWorkspaceSnapshot {
        val raw = prefs.getString("workspace_json", null)
        if (!raw.isNullOrBlank()) {
            val parsed = runCatching { json.decodeFromString<MasterCadWorkspaceSnapshot>(raw) }.getOrNull()
            if (parsed != null) return parsed
        }
        return createSampleSnapshot()
    }

    private fun persist(next: MasterCadWorkspaceSnapshot) {
        _snapshot.value = next
        runCatching {
            prefs.edit().putString("workspace_json", json.encodeToString(next)).apply()
        }
    }

    fun updateCadWorkspace(
        entities: List<CadEntity> = _snapshot.value.entities,
        dimensions: List<CadDimensionEntity> = _snapshot.value.dimensions,
        layers: List<CadLayerConfig> = _snapshot.value.layers,
        styleSettings: CadStyleSettings = _snapshot.value.styleSettings
    ) {
        persist(_snapshot.value.copy(entities = entities, dimensions = dimensions, layers = layers, styleSettings = styleSettings))
    }

    fun updateCogoPoints(points: List<CogoPoint>) {
        persist(_snapshot.value.copy(cogoPoints = points))
    }

    fun saveHistoricalDeed(record: HistoricalDeedRecord) {
        val list = _snapshot.value.historicalDeeds.filterNot { it.id == record.id } + record
        persist(_snapshot.value.copy(historicalDeeds = list))
    }

    fun saveUnionRegisterEntry(entry: UnionRegisterEntry) {
        val list = _snapshot.value.unionRegisters.filterNot { it.id == entry.id } + entry
        persist(_snapshot.value.copy(unionRegisters = list))
    }

    fun updateTaxRateSchedule(schedule: TaxRateSchedule) {
        persist(_snapshot.value.copy(taxRateSchedule = schedule))
    }

    fun updateRoadSpec(spec: RoadAlignmentSpec) {
        persist(_snapshot.value.copy(roadSpec = spec))
    }

    fun seedPubailMasterSample() {
        persist(createSampleSnapshot())
    }

    private fun createSampleSnapshot(): MasterCadWorkspaceSnapshot {
        val plotPts = listOf(
            PolygonVertex(0.0, 0.0),
            PolygonVertex(120.0, 0.0),
            PolygonVertex(120.0, 90.0),
            PolygonVertex(0.0, 90.0)
        )
        val entities = listOf(
            CadEntity("ent_plot_1", CadEntityType.POLYGON, "layer_plot_bdy", plotPts, closed = true),
            CadEntity("ent_road_1", CadEntityType.POLYLINE, "layer_road", listOf(PolygonVertex(-15.0, -12.0), PolygonVertex(140.0, -12.0)), closed = false),
            CadEntity("ent_lbl_1", CadEntityType.TEXT, "layer_text", listOf(PolygonVertex(45.0, 45.0)), text = "দাগ ১০৪২ (২৪.৭৯ শতক)")
        )
        val dims = CadAndDimensionEngine.generateAutoPlotDimensions(plotPts, MeasurementDisplayUnit.FEET_INCHES)
        val cogoPts = listOf(
            CogoPoint("cp_1", 1, "BDY", 0.0, 0.0, 12.4, "SW Corner"),
            CogoPoint("cp_2", 2, "BDY", 0.0, 120.0, 12.8, "SE Corner"),
            CogoPoint("cp_3", 3, "BDY", 90.0, 120.0, 13.9, "NE Corner"),
            CogoPoint("cp_4", 4, "BDY", 90.0, 0.0, 13.1, "NW Corner"),
            CogoPoint("cp_5", 5, "SPOT", 45.0, 60.0, 13.6, "Center High")
        )
        val sampleHistDeed = HistoricalDeedRecord(
            id = "hist_sample_1984",
            title = "১৯৮৪ সালের পুরাতন সাফ-কবলা দলিল (পুনর্গঠন নমুনা)",
            templateType = DeedTemplateType.SAFKABALA_SALE,
            sellerName = "মৃত হাজী ছমির উদ্দিন",
            sellerFather = "মৃত করিম বক্স",
            sellerAddress = "পুবাইল, গাজীপুর সদর",
            buyerName = "মোঃ রফিকুল ইসলাম",
            buyerFather = "আবদুল খালেক",
            buyerAddress = "পুবাইল, গাজীপুর সদর",
            deedNumber = "৪৫২১/১৯৮৪",
            registrationDate = "১২/০৪/১৯৮৪",
            registrationOffice = "সাব-রেজিস্ট্রি অফিস, গাজীপুর সদর",
            mouza = "পুবাইল",
            jlNumber = "৮৫",
            khatianNumber = "সাবেক ২১০ (আরএস) / হাল ৪১২ (বিএস)",
            dagNumber = "১০৪২",
            areaDecimal = "24.79",
            boundaryNorth = "দাগ ১০৪১ (রহিম উদ্দিন)",
            boundarySouth = "১২ ফুট সরকারি রাস্তা",
            boundaryEast = "দাগ ১০৪৩ (আবুল হোসেন)",
            boundaryWest = "দাগ ১০৩৯ (পুকুর পাড়)"
        )
        val registers = listOf(
            UnionRegisterEntry("reg_1", UnionRegisterKind.HOLDING_REG, "হোল্ডিং-২১৪", "পুবাইল", "খঃ ৪১২ | দাগ ১০৪২", "মোঃ রফিকুল ইসলাম", "২৪.৭৯ শতক", "২০২৬-১১-১০", "হালনাগাদ"),
            UnionRegisterEntry("reg_2", UnionRegisterKind.DAKHILA_REG, "দাখিলা-৯০৮৭১", "পুবাইল", "খঃ ৪১২", "মোঃ রফিকুল ইসলাম", "৪৯৫ টাকা (১৪৩৩)", "২০২৬-০৭-১৫", "আদায়কৃত"),
            UnionRegisterEntry("reg_3", UnionRegisterKind.MUTATION_REF, "নামজারি-১১২/২৬", "পুবাইল", "দাগ ১০৪২", "মোঃ করিম উদ্দিন", "১২.০০ শতক", "২০২৬-১০-২০", "শুনানি অপেক্ষমাণ")
        )
        return MasterCadWorkspaceSnapshot(
            entities = entities,
            dimensions = dims,
            cogoPoints = cogoPts,
            historicalDeeds = listOf(sampleHistDeed),
            unionRegisters = registers
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MasterCadRepository? = null

        fun getInstance(context: Context): MasterCadRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MasterCadRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
