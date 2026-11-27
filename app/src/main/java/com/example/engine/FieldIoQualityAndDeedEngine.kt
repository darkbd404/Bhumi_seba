package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.example.model.CadDimensionEntity
import com.example.model.CadEntity
import com.example.model.CadEntityType
import com.example.model.CadLayerConfig
import com.example.model.CogoPoint
import com.example.model.GeoWaypoint
import com.example.model.HistoricalDeedRecord
import com.example.model.PolygonVertex
import com.example.model.QualityControlAuditReport
import com.example.model.QualityControlCheckResult
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Module 06, 11, 16 & 17 Engine:
 * - Old Paper Deed Recreation (Cleanup/Deskew preview + Bangla/English OCR parser + Faithful Reproduction vs Editable Modern Draft)
 * - Multi-Format Field-to-CAD Importer (CSV, PNEZD, PENZD, NEZ, ENZ, GPX, NMEA, LandXML, DXF, SVG, GeoJSON, KML) + Auto Code Linework
 * - Survey Quality Control & Uncertainty Audit Dashboard
 * - Universal File Compatibility Matrix + Master Project Bundle ZIP Exporter
 */
object FieldIoQualityAndDeedEngine {

    // -------------------- Module 06: Historical Deed Cleanup, OCR & Dual Recreation --------------------

    fun enhanceOldDeedBitmap(
        source: Bitmap,
        deskewDeg: Float = 0f,
        contrast: Float = 1.35f,
        removeStainsBw: Boolean = true
    ): Bitmap {
        val m = Matrix().apply { if (abs(deskewDeg) > 0.05f) postRotate(deskewDeg) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
        val out = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = ColorMatrix().apply {
            if (removeStainsBw) setSaturation(0f)
            val t = (-0.5f * contrast + 0.5f) * 255f + 16f
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, t,
                        0f, contrast, 0f, 0f, t,
                        0f, 0f, contrast, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        canvas.drawBitmap(rotated, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        if (rotated !== source) rotated.recycle()
        return out
    }

    fun parseHistoricalDeedText(rawText: String, existing: HistoricalDeedRecord = HistoricalDeedRecord(id = UUID.randomUUID().toString())): HistoricalDeedRecord {
        val norm = DocumentAndReportEngine.bengaliDigitsToEnglish(rawText)
        fun grab(pattern: String): String {
            val m = Regex(pattern, RegexOption.IGNORE_CASE).find(norm)
            return m?.groupValues?.getOrNull(1)?.trim().orEmpty()
        }
        return existing.copy(
            ocrRawText = rawText,
            sellerName = grab("""(?:দাতা|বিক্রেতা|Seller|First Party|১ম পক্ষ)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.sellerName },
            buyerName = grab("""(?:গ্রহীতা|ক্রেতা|Buyer|Second Party|২য় পক্ষ)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.buyerName },
            sellerFather = grab("""(?:পিতা|স্বামী|Father)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.sellerFather },
            deedNumber = grab("""(?:দলিল\s*নং|Deed\s*No\.?)\s*[:\-]?\s*([A-Za-z0-9/\-]+)""").ifBlank { existing.deedNumber },
            registrationDate = grab("""(?:তারিখ|Date)\s*[:\-]?\s*([0-9./\-]+)""").ifBlank { existing.registrationDate },
            registrationOffice = grab("""(?:সাব-রেজিস্ট্রি|অফিস|Office)\s*[:\-]?\s*([^\n;]+)""").ifBlank { existing.registrationOffice },
            mouza = grab("""(?:মৌজা|Mouza)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.mouza },
            jlNumber = grab("""(?:জে\.?এল\.?|JL)\s*(?:নং|No\.?)?\s*[:\-]?\s*([0-9]+)""").ifBlank { existing.jlNumber },
            khatianNumber = grab("""(?:খতিয়ান|Khatian)\s*(?:নং|No\.?)?\s*[:\-]?\s*([0-9/,\-]+)""").ifBlank { existing.khatianNumber },
            dagNumber = grab("""(?:দাগ|Dag)\s*(?:নং|No\.?)?\s*[:\-]?\s*([0-9/,\-]+)""").ifBlank { existing.dagNumber },
            areaDecimal = grab("""(?:পরিমাণ|Area|শতাংশ)\s*[:\-]?\s*([0-9.]+)""").ifBlank { existing.areaDecimal },
            boundaryNorth = grab("""(?:উত্তরে|উঃ|North)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.boundaryNorth },
            boundarySouth = grab("""(?:দক্ষিণে|দঃ|South)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.boundarySouth },
            boundaryEast = grab("""(?:পূর্বে|পূঃ|East)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.boundaryEast },
            boundaryWest = grab("""(?:পশ্চিমে|পঃ|West)\s*[:\-]?\s*([^\n,;]+)""").ifBlank { existing.boundaryWest },
            previousDeedRef = grab("""(?:পূর্ব\s*দলিল|বায়া\s*দলিল|Previous\s*Deed)\s*[:\-]?\s*([^\n;]+)""").ifBlank { existing.previousDeedRef },
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /** Mode 1: Faithful Historical Reproduction preserving traditional phrasing & layout */
    fun buildFaithfulHistoricalReproduction(r: HistoricalDeedRecord): String {
        return """
        ============================================================
        [ঐতিহাসিক পুনর্গঠিত অনুলিপি — মূল দলিলের পাঠ অনুসরণে]
        শিরোনাম: ${r.title.ifBlank { r.templateType.titleBn }}
        মূল দলিল নং: ${r.deedNumber.ifBlank { "অপাঠ্য/অজানা" }}  |  তারিখ: ${r.registrationDate.ifBlank { "-" }}
        রেজিস্ট্রি অফিস: ${r.registrationOffice.ifBlank { "সংশ্লিষ্ট সাব-রেজিস্ট্রি অফিস" }}
        ============================================================
        শ্রীশ্রী/বিসমিল্লাহির রাহমানির রাহিম — এতদ্বারা অত্র ${r.templateType.titleBn} পত্র লিখিতং...

        ১. দাতা / বিক্রেতা পক্ষ:
           নাম: ${r.sellerName.ifBlank { ".................." }} | পিতা/স্বামী: ${r.sellerFather.ifBlank { ".................." }}
           ঠিকানা: ${r.sellerAddress.ifBlank { r.mouza.ifBlank { ".................." } }}

        ২. গ্রহীতা / ক্রেতা পক্ষ:
           নাম: ${r.buyerName.ifBlank { ".................." }} | পিতা/স্বামী: ${r.buyerFather.ifBlank { ".................." }}
           ঠিকানা: ${r.buyerAddress.ifBlank { r.mouza.ifBlank { ".................." } }}

        ৩. কস্য তফসিল লিখিত ভূমি:
           মৌজা: ${r.mouza.ifBlank { "-" }}, জে.এল. নং: ${r.jlNumber.ifBlank { "-" }}
           খতিয়ান নং: ${r.khatianNumber.ifBlank { "-" }}, দাগ নং: ${r.dagNumber.ifBlank { "-" }}
           জমির পরিমাণ: ${r.areaDecimal.ifBlank { "-" }} শতাংশ (অংশ: ${r.shareFraction})
           বায়া/পূর্ব দলিল সূত্র: ${r.previousDeedRef.ifBlank { "বর্ণিত নাই" }}

        ৪. চৌহদ্দি বিবরণ:
           উত্তরে: ${r.boundaryNorth.ifBlank { "-" }}   |   দক্ষিণে: ${r.boundarySouth.ifBlank { "-" }}
           পূর্বে: ${r.boundaryEast.ifBlank { "-" }}   |   পশ্চিমে: ${r.boundaryWest.ifBlank { "-" }}

        ৫. মূল পাঠ / শর্তাবলি (সংশোধিত পাঠ):
        ${r.customArchivalTerms.ifBlank { r.ocrRawText.ifBlank { "মূল পুরাতন দলিলের শর্ত ও দখল হস্তান্তর বিবরণী অনুসারে পুনর্লিখিত।" } }}

        ৬. সাক্ষী ও টিপসই/স্বাক্ষর স্থান:
           দাতার স্বাক্ষর/টিপসই: ______________   গ্রহীতার স্বাক্ষর: ______________
        [অস্বীকৃতি: এটি স্ক্যান করা পুরাতন দলিল হতে ডিজিটাল পুনর্গঠন; আসল সরকারি রেজিস্ট্রিকৃত দলিল বা সার্টিফায়েড নকল নয়।]
        """.trimIndent()
    }

    /** Mode 2: Newly Drafted Modern Editable Legal Document */
    fun buildModernEditableLegalDraft(r: HistoricalDeedRecord): String {
        return """
        ============================================================
        নতুন আধুনিক আইনি খসড়া (পুনঃলিখিত — সম্পাদনাযোগ্য)
        ${r.title}
        ============================================================
        দলিল সূত্র (পূর্ব রেফারেন্স): দলিল নং ${r.deedNumber.ifBlank { "-" }}, তারিখ: ${r.registrationDate.ifBlank { "-" }}

        ১. প্রথম পক্ষ (দাতা/বিক্রেতা): ${r.sellerName.ifBlank { "........" }}, পিতা: ${r.sellerFather.ifBlank { "........" }}, ঠিকানা: ${r.sellerAddress.ifBlank { "........" }}
        ২. দ্বিতীয় পক্ষ (গ্রহীতা/ক্রেতা): ${r.buyerName.ifBlank { "........" }}, পিতা: ${r.buyerFather.ifBlank { "........" }}, ঠিকানা: ${r.buyerAddress.ifBlank { "........" }}

        ৩. আধুনিক তফসিল সারসংক্ষেপ:
           • মৌজা ও জে.এল.: ${r.mouza.ifBlank { "-" }} (JL: ${r.jlNumber.ifBlank { "-" }})
           • খতিয়ান ও দাগ: খতিয়ান ${r.khatianNumber.ifBlank { "-" }}, দাগ ${r.dagNumber.ifBlank { "-" }}
           • পরিমাণ ও হিস্যা: ${r.areaDecimal.ifBlank { "-" }} শতাংশ (হিস্যা: ${r.shareFraction})
           • চৌহদ্দি: উঃ ${r.boundaryNorth.ifBlank { "-" }} | দঃ ${r.boundarySouth.ifBlank { "-" }} | পূঃ ${r.boundaryEast.ifBlank { "-" }} | পঃ ${r.boundaryWest.ifBlank { "-" }}

        ৪. শর্তাবলি:
        ${r.customArchivalTerms.ifBlank { "উভয় পক্ষ সজ্ঞানে, সুস্থ মস্তিষ্কে ও সাক্ষীগণের উপস্থিতিতে পূর্ববর্তী দলিল ও খতিয়ান যাচাইপূর্বক নিষ্কণ্টক মালিকানা হস্তান্তরে সম্মত হইলেন।" }}

        [আইনি সতর্কতা: এটি আনসার্টিফায়েড ডিজিটাল খসড়া; সাব-রেজিস্ট্রি অফিসে দাখিলের পূর্বে বিজ্ঞ আইনজীবী দ্বারা যাচাই বাধ্যতামূলক।]
        """.trimIndent()
    }

    // -------------------- Module 11: Multi-Format Field-to-CAD Importer --------------------

    enum class PointFileFormat(val labelBn: String) {
        PNEZD("PNEZD (Pt, N, E, Z, Desc)"),
        PENZD("PENZD (Pt, E, N, Z, Desc)"),
        NEZ("NEZ (N, E, Z)"),
        ENZ("ENZ (E, N, Z)"),
        GPX("GPX Track/Waypoints"),
        NMEA("NMEA-0183 ($*GGA)"),
        LANDXML("LandXML (<CgPoints>)"),
        DXF("AutoCAD DXF (LINE/LWPOLYLINE/POINT)"),
        SVG("SVG Vector (<polygon>/<polyline>)"),
        GEOJSON("GeoJSON FeatureCollection"),
        KML("Google Earth KML")
    }

    data class FieldImportResult(
        val formatDetected: PointFileFormat,
        val points: List<CogoPoint>,
        val entities: List<CadEntity>,
        val summaryNote: String
    )

    fun importFieldOrVectorText(rawText: String, preferredFormat: PointFileFormat? = null): FieldImportResult {
        val trimmed = rawText.trim()
        val format = preferredFormat ?: when {
            trimmed.contains("ENTITIES") && trimmed.contains("SECTION") -> PointFileFormat.DXF
            trimmed.contains("<LandXML", ignoreCase = true) || trimmed.contains("<CgPoints", ignoreCase = true) -> PointFileFormat.LANDXML
            trimmed.contains("<gpx", ignoreCase = true) -> PointFileFormat.GPX
            trimmed.contains("<kml", ignoreCase = true) -> PointFileFormat.KML
            trimmed.contains("<svg", ignoreCase = true) -> PointFileFormat.SVG
            trimmed.startsWith("{") && trimmed.contains("FeatureCollection", ignoreCase = true) -> PointFileFormat.GEOJSON
            trimmed.contains("GGA,") -> PointFileFormat.NMEA
            else -> PointFileFormat.PNEZD
        }

        val points = mutableListOf<CogoPoint>()
        val entities = mutableListOf<CadEntity>()

        when (format) {
            PointFileFormat.LANDXML -> {
                val cgRegex = Regex("""<CgPoint[^>]*name="([^"]*)"[^>]*>([^<]+)</CgPoint>""", RegexOption.IGNORE_CASE)
                var idx = 1
                cgRegex.findAll(trimmed).forEach { m ->
                    val name = m.groupValues[1].ifBlank { "PT$idx" }
                    val nums = m.groupValues[2].trim().split(Regex("""\s+""")).mapNotNull { it.toDoubleOrNull() }
                    if (nums.size >= 2) {
                        points.add(CogoPoint("lx_$idx", idx, name, nums[0], nums[1], nums.getOrElse(2) { 0.0 }, "LandXML", "LandXML"))
                        idx++
                    }
                }
            }
            PointFileFormat.DXF -> {
                val lines = trimmed.lines().map { it.trim() }
                var i = 0
                var currentPts = mutableListOf<PolygonVertex>()
                var inPoly = false
                var pendingX: Double? = null
                while (i + 1 < lines.size) {
                    val code = lines[i]
                    val value = lines[i + 1]
                    if (code == "0") {
                        if (inPoly && currentPts.size >= 2) {
                            entities.add(CadEntity("dxf_${entities.size}", CadEntityType.POLYLINE, points = currentPts.toList(), closed = currentPts.size >= 3))
                        }
                        currentPts = mutableListOf()
                        inPoly = value == "LWPOLYLINE" || value == "POLYLINE" || value == "LINE"
                        pendingX = null
                    } else if (inPoly) {
                        if (code == "10") pendingX = value.toDoubleOrNull()
                        if (code == "20" && pendingX != null) {
                            val py = value.toDoubleOrNull()
                            if (py != null) {
                                currentPts.add(PolygonVertex(pendingX, py))
                                points.add(CogoPoint("dxf_pt_${points.size}", points.size + 1, "DXF", py, pendingX, 0.0, "DXF Vertex", "DXF"))
                            }
                            pendingX = null
                        }
                    }
                    i += 2
                }
                if (inPoly && currentPts.size >= 2) {
                    entities.add(CadEntity("dxf_${entities.size}", CadEntityType.POLYLINE, points = currentPts.toList(), closed = currentPts.size >= 3))
                }
            }
            PointFileFormat.SVG -> {
                val polyRegex = Regex("""points="([^"]+)"""", RegexOption.IGNORE_CASE)
                polyRegex.findAll(trimmed).forEachIndexed { idx, m ->
                    val coords = m.groupValues[1].trim().split(Regex("""\s+""")).mapNotNull { pair ->
                        val xy = pair.split(",").mapNotNull { it.toDoubleOrNull() }
                        if (xy.size >= 2) PolygonVertex(xy[0], xy[1]) else null
                    }
                    if (coords.size >= 2) {
                        entities.add(CadEntity("svg_$idx", CadEntityType.POLYGON, points = coords, closed = true))
                        coords.forEach { v ->
                            points.add(CogoPoint("svg_p_${points.size}", points.size + 1, "SVG", v.y, v.x, 0.0, "SVG", "SVG"))
                        }
                    }
                }
            }
            PointFileFormat.NMEA -> {
                val parsed = GisCoordinateEngine.parseNmeaLog(trimmed)
                parsed.waypoints.forEachIndexed { idx, wp ->
                    val utm = GisCoordinateEngine.latLonToUtm(wp.lat, wp.lon)
                    points.add(CogoPoint("nmea_$idx", idx + 1, "GPS", utm.northing * 3.28084, utm.easting * 3.28084, wp.altitudeM * 3.28084, wp.fixType, "NMEA"))
                }
            }
            PointFileFormat.GPX -> {
                val trkRegex = Regex("""lat="([-0-9.]+)"\s+lon="([-0-9.]+)"""", RegexOption.IGNORE_CASE)
                trkRegex.findAll(trimmed).forEachIndexed { idx, m ->
                    val lat = m.groupValues[1].toDoubleOrNull() ?: return@forEachIndexed
                    val lon = m.groupValues[2].toDoubleOrNull() ?: return@forEachIndexed
                    val utm = GisCoordinateEngine.latLonToUtm(lat, lon)
                    points.add(CogoPoint("gpx_$idx", idx + 1, "GPX", utm.northing * 3.28084, utm.easting * 3.28084, 0.0, "GPX", "GPX"))
                }
            }
            PointFileFormat.KML -> {
                val cRegex = Regex("""<coordinates>([\s\S]*?)</coordinates>""", RegexOption.IGNORE_CASE)
                cRegex.findAll(trimmed).forEach { m ->
                    m.groupValues[1].trim().split(Regex("""\s+""")).forEachIndexed { idx, tok ->
                        val parts = tok.split(",").mapNotNull { it.toDoubleOrNull() }
                        if (parts.size >= 2) {
                            val utm = GisCoordinateEngine.latLonToUtm(parts[1], parts[0])
                            points.add(CogoPoint("kml_$idx", points.size + 1, "KML", utm.northing * 3.28084, utm.easting * 3.28084, parts.getOrElse(2) { 0.0 } * 3.28084, "KML", "KML"))
                        }
                    }
                }
            }
            PointFileFormat.GEOJSON -> {
                val pairRegex = Regex("""\[\s*([-0-9.]+)\s*,\s*([-0-9.]+)\s*]""")
                pairRegex.findAll(trimmed).forEachIndexed { idx, m ->
                    val lon = m.groupValues[1].toDoubleOrNull() ?: return@forEachIndexed
                    val lat = m.groupValues[2].toDoubleOrNull() ?: return@forEachIndexed
                    val utm = GisCoordinateEngine.latLonToUtm(lat, lon)
                    points.add(CogoPoint("gj_$idx", idx + 1, "GJ", utm.northing * 3.28084, utm.easting * 3.28084, 0.0, "GeoJSON", "GeoJSON"))
                }
            }
            else -> {
                // CSV / TXT / PNEZD / PENZD / NEZ / ENZ
                trimmed.lines().forEachIndexed { lineIdx, rawLine ->
                    val line = rawLine.trim()
                    if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
                    val cols = line.split(Regex("""[,\t;]+""")).map { it.trim() }
                    if (cols.size < 2) return@forEachIndexed
                    when (format) {
                        PointFileFormat.PNEZD -> {
                            val ptNo = cols[0].toIntOrNull() ?: (points.size + 1)
                            val n = cols.getOrNull(1)?.toDoubleOrNull() ?: return@forEachIndexed
                            val e = cols.getOrNull(2)?.toDoubleOrNull() ?: return@forEachIndexed
                            val z = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                            val desc = cols.getOrNull(4).orEmpty().ifBlank { "PT" }
                            points.add(CogoPoint("pt_$lineIdx", ptNo, desc, n, e, z, desc, "PNEZD"))
                        }
                        PointFileFormat.PENZD -> {
                            val ptNo = cols[0].toIntOrNull() ?: (points.size + 1)
                            val e = cols.getOrNull(1)?.toDoubleOrNull() ?: return@forEachIndexed
                            val n = cols.getOrNull(2)?.toDoubleOrNull() ?: return@forEachIndexed
                            val z = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                            val desc = cols.getOrNull(4).orEmpty().ifBlank { "PT" }
                            points.add(CogoPoint("pt_$lineIdx", ptNo, desc, n, e, z, desc, "PENZD"))
                        }
                        PointFileFormat.NEZ -> {
                            val n = cols[0].toDoubleOrNull() ?: return@forEachIndexed
                            val e = cols[1].toDoubleOrNull() ?: return@forEachIndexed
                            val z = cols.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                            points.add(CogoPoint("pt_$lineIdx", points.size + 1, "NEZ", n, e, z, "", "NEZ"))
                        }
                        else -> {
                            val e = cols[0].toDoubleOrNull() ?: return@forEachIndexed
                            val n = cols[1].toDoubleOrNull() ?: return@forEachIndexed
                            val z = cols.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                            points.add(CogoPoint("pt_$lineIdx", points.size + 1, "ENZ", n, e, z, "", "ENZ"))
                        }
                    }
                }
            }
        }

        // Auto-connect linework by field code (BDY, RD, BLD, etc.) when entities weren't explicitly decoded
        if (entities.isEmpty() && points.size >= 2) {
            val grouped = points.groupBy { it.code.uppercase().ifBlank { "BDY" } }
            grouped.forEach { (code, pts) ->
                if (pts.size >= 2) {
                    val layerId = when {
                        code.contains("RD") || code.contains("ROAD") -> "layer_road"
                        code.contains("BLD") -> "layer_building"
                        code.contains("WTR") || code.contains("POND") -> "layer_water"
                        else -> "layer_plot_bdy"
                    }
                    entities.add(
                        CadEntity(
                            id = "auto_code_$code",
                            type = if (pts.size >= 3 && (code.contains("BDY") || code.contains("BLD") || code.contains("PLOT"))) CadEntityType.POLYGON else CadEntityType.POLYLINE,
                            layerId = layerId,
                            points = pts.map { PolygonVertex(it.eastFt, it.northFt) },
                            closed = pts.size >= 3 && !code.contains("RD")
                        )
                    )
                }
            }
        }

        return FieldImportResult(
            formatDetected = format,
            points = points,
            entities = entities,
            summaryNote = "${format.labelBn}: ${points.size}টি পয়েন্ট ও ${entities.size}টি CAD লাইনওয়ার্ক ইমপোর্ট হয়েছে।"
        )
    }

    // -------------------- Module 16: Survey Quality Control Audit --------------------

    fun runSurveyQualityAudit(
        entities: List<CadEntity>,
        measuredAreaDecimal: Double,
        deedAreaDecimal: Double,
        khatianAreaDecimal: Double,
        traverseMisclosureFt: Double,
        traversePerimeterFt: Double,
        georefRmsePx: Double,
        gpsAccuracyM: Double
    ): QualityControlAuditReport {
        val checks = mutableListOf<QualityControlCheckResult>()
        val topo = CadAndDimensionEngine.analyzeAndCleanTopology(entities)

        // 1. Boundary closure & self-intersection
        val closureStatus = if (topo.openGapsCount == 0 && topo.selfIntersections == 0) "PASS" else "FAIL"
        checks.add(
            QualityControlCheckResult(
                categoryBn = "সীমানা ক্লোজার ও টপোলজি",
                status = closureStatus,
                measuredValue = "গ্যাপ: ${topo.openGapsCount}, সেলফ-ইন্টারসেকশন: ${topo.selfIntersections}, ডুপ্লিকেট: ${topo.duplicateEntityCount}",
                thresholdOrRef = "০ গ্যাপ, ০ ইন্টারসেকশন",
                recommendationBn = if (closureStatus == "PASS") "সকল সীমানা সঠিকভাবে ক্লোজ আছে।" else "CAD Modify/Repair হতে Close Boundary ও Duplicate Cleanup চালান।"
            )
        )

        // 2. Traverse Precision
        val ratio = if (traverseMisclosureFt > 1e-6 && traversePerimeterFt > 1e-6) (traversePerimeterFt / traverseMisclosureFt).toInt() else 99999
        val travStatus = when {
            traversePerimeterFt <= 0.0 -> "WARN"
            ratio >= 5000 -> "PASS"
            ratio >= 1500 -> "WARN"
            else -> "FAIL"
        }
        checks.add(
            QualityControlCheckResult(
                categoryBn = "ট্র্যাভার্স মিসক্লোজার ও কৌণিক নির্ভুলতা",
                status = travStatus,
                measuredValue = if (traversePerimeterFt > 0.0) "ভুল: ${String.format(Locale.US, "%.3f", traverseMisclosureFt)} ft (1:$ratio)" else "ট্র্যাভার্স ডাটা নেই",
                thresholdOrRef = "সর্বনিম্ন 1:3000 (শহুরে 1:5000)",
                recommendationBn = if (travStatus == "PASS") "গ্রহণযোগ্য ট্র্যাভার্স নির্ভুলতা।" else "Bowditch বা Least-Squares সমন্বয় ব্যবহার করুন।"
            )
        )

        // 3. Deed vs Khatian vs Measured Discrepancy
        val diffDeed = measuredAreaDecimal - deedAreaDecimal
        val diffKh = measuredAreaDecimal - khatianAreaDecimal
        val maxDiff = maxOf(abs(diffDeed), abs(diffKh))
        val areaStatus = when {
            maxDiff <= 0.25 -> "PASS"
            maxDiff <= 1.0 -> "WARN"
            else -> "FAIL"
        }
        checks.add(
            QualityControlCheckResult(
                categoryBn = "দলিল বনাম খতিয়ান বনাম বাস্তব পরিমাপ",
                status = areaStatus,
                measuredValue = String.format(Locale.US, "মাপ %.2f | খতিয়ান %.2f (%+.2f) | দলিল %.2f (%+.2f)", measuredAreaDecimal, khatianAreaDecimal, diffKh, deedAreaDecimal, diffDeed),
                thresholdOrRef = "±০.২৫ শতাংশের মধ্যে গ্রহণযোগ্য",
                recommendationBn = if (areaStatus == "PASS") "রেকর্ড ও বাস্তব পরিমাপ সামঞ্জস্যপূর্ণ।" else "সাবেক-হাল দাগ, প্যান্টোগ্রাফ ও প্রতিবেশী দাগের দখল যাচাই করুন।"
            )
        )

        // 4. Georeference RMSE & Map Scale Distortion
        val geoStatus = when {
            georefRmsePx <= 2.5 -> "PASS"
            georefRmsePx <= 6.0 -> "WARN"
            else -> "FAIL"
        }
        checks.add(
            QualityControlCheckResult(
                categoryBn = "মৌজা ম্যাপ ক্যালিব্রেশন RMSE",
                status = geoStatus,
                measuredValue = String.format(Locale.US, "RMSE: %.2f px", georefRmsePx),
                thresholdOrRef = "≤ ২.৫ px",
                recommendationBn = if (geoStatus == "PASS") "ক্যালিব্রেশন নির্ভুল আছে।" else "অতিরিক্ত কন্ট্রোল পয়েন্ট ও TPS ট্রান্সফর্ম প্রয়োগ করুন।"
            )
        )

        // 5. GPS Accuracy Uncertainty
        val gpsStatus = when {
            gpsAccuracyM <= 1.0 -> "PASS"
            gpsAccuracyM <= 5.0 -> "WARN"
            else -> "FAIL"
        }
        checks.add(
            QualityControlCheckResult(
                categoryBn = "GPS / GNSS অনিশ্চয়তা সতর্কতা",
                status = gpsStatus,
                measuredValue = String.format(Locale.US, "±%.2f মিটার", gpsAccuracyM),
                thresholdOrRef = "RTK ≤ 0.1m | সাধারণ ফোন GPS ~3–8m",
                recommendationBn = "মোবাইল GPS আইনি সীমানা নির্ধারণে আনুমানিক; চূড়ান্ত সীমানায় Total Station / RTK ব্যবহার করুন।"
            )
        )

        val passPts = checks.sumOf { if (it.status == "PASS") 20 else if (it.status == "WARN") 12 else 4 }
        return QualityControlAuditReport(
            overallScorePct = passPts.coerceIn(0, 100),
            checks = checks,
            discrepancySummaryBn = String.format(
                Locale.US,
                "সার্ভে স্কোর: %d%% | পরিমাপ: %.2f শতক | খতিয়ান পার্থক্য: %+.2f | দলিল পার্থক্য: %+.2f",
                passPts, measuredAreaDecimal, diffKh, diffDeed
            )
        )
    }

    // -------------------- Module 17: Universal File Compatibility Matrix & Master Project ZIP --------------------

    data class FormatSupportInfo(
        val extension: String,
        val titleBn: String,
        val nativeInApp: Boolean,
        val statusLabelBn: String,
        val guidanceBn: String
    )

    val FILE_COMPATIBILITY_MATRIX = listOf(
        FormatSupportInfo("DXF", "AutoCAD ASCII Vector", true, "সম্পূর্ণ Read/Write", "Layer, LWPOLYLINE, CIRCLE, ARC, DIMENSION, TEXT"),
        FormatSupportInfo("SVG", "Scalable Vector Graphics", true, "সম্পূর্ণ Read/Write", "Polygon, Polyline, Text সহ CAD স্কেলে রপ্তানি"),
        FormatSupportInfo("SHP (ZIP)", "ESRI Shapefile (.shp/.shx/.dbf/.prj)", true, "সম্পূর্ণ Export", "WGS84 প্রজেকশনসহ QGIS/ArcGIS রেডি"),
        FormatSupportInfo("GeoJSON", "RFC 7946 Spatial JSON", true, "সম্পূর্ণ Read/Write", "QGIS, Mapbox, Leaflet ও ওয়েব GIS"),
        FormatSupportInfo("KML / KMZ", "Google Earth GIS", true, "সম্পূর্ণ Read/Write", "Google Earth Pro ও মোবাইল ম্যাপ"),
        FormatSupportInfo("GPX", "GPS Exchange Format", true, "সম্পূর্ণ Read/Write", "ফিল্ড ট্র্যাক ও ওয়েপয়েন্ট"),
        FormatSupportInfo("LandXML", "Survey Civil XML (<CgPoints>)", true, "সম্পূর্ণ Import/Parse", "Total Station ও Civil 3D পয়েন্ট ফাইল"),
        FormatSupportInfo("PNEZD / PENZD / NEZ / ENZ / CSV / TXT", "Survey Point Tables", true, "সম্পূর্ণ Import/Export", "কোড অনুযায়ী অটো CAD লাইনওয়ার্কসহ"),
        FormatSupportInfo("NMEA-0183", "GNSS Raw Receiver Stream", true, "সম্পূর্ণ Parse", "RTK Fix/Float ও HDOP বিশ্লেষণসহ"),
        FormatSupportInfo("PDF / DOCX / XLSX / PNG / JPG", "Office & Print Formats", true, "সম্পূর্ণ Read/Write", "A0–A4 প্রিন্ট, ভিন্টেজ দলিল, Excel"),
        FormatSupportInfo("DWG / DWT / DGN", "Proprietary Closed Binary CAD", false, "রূপান্তর প্রয়োজন (DXF ব্যবহার করুন)", "AutoCAD/ODA/LibreCAD-এ DXF R2000 সেভ করে আমদানি করুন — ভুয়া বাইনারি পার্সার নেই"),
        FormatSupportInfo("GeoTIFF / GPKG / GML / RTF", "Extended Raster/Spatial Containers", false, "গাইডেড বিকল্প", "GeoTIFF-কে PNG/JPG+GCP ও GPKG/GML-কে GeoJSON/Shapefile রূপে ব্যবহার করুন")
    )

    fun buildMasterProjectBundleZip(
        projectTitle: String,
        entities: List<CadEntity>,
        dimensions: List<CadDimensionEntity>,
        layers: List<CadLayerConfig>,
        cogoPoints: List<CogoPoint>,
        deedDraftText: String,
        scheduleText: String
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            fun putText(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            fun putBytes(name: String, data: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }

            putText("README.txt", "Project: $projectTitle\nGenerated by Bhumi Seba Digital Workstation (Uncertified Survey Draft Bundle)\n")
            putText("cad_drawing.dxf", CadAndDimensionEngine.exportCadToDxf(projectTitle, entities, dimensions, layers))
            putText("cad_drawing.svg", CadAndDimensionEngine.exportCadToSvg(projectTitle, entities, dimensions, layers))
            val csvRows = cogoPoints.map { listOf(it.pointNumber.toString(), it.northFt.toString(), it.eastFt.toString(), it.elevationFt.toString(), it.code) }
            putText("survey_points_pnezd.csv", UniversalExportEngine.buildCsvText(listOf("Point", "North", "East", "Elev", "Code"), csvRows))
            putBytes("survey_points.xlsx", UniversalExportEngine.buildXlsxBytes("SurveyPoints", listOf("Point", "North", "East", "Elev", "Code"), csvRows))
            putBytes("legal_schedule.docx", DocumentAndReportEngine.buildDocxBytes(projectTitle, scheduleText))
            putBytes("deed_draft_vintage.docx", DocumentAndReportEngine.buildDocxBytes("$projectTitle Deed Draft", deedDraftText))
            val wps = cogoPoints.mapIndexed { idx, p ->
                val (lat, lon) = GisCoordinateEngine.utmToLatLon(UtmCoordinate(46, 'N', 236000.0 + p.eastFt * 0.3048, 2635000.0 + p.northFt * 0.3048))
                GeoWaypoint(p.id, "P${p.pointNumber}", lat, lon)
            }
            putText("gis_layer.geojson", GisCoordinateEngine.exportGeoJson(projectTitle, wps))
            putText("gis_layer.kml", GisCoordinateEngine.exportKml(projectTitle, wps))
            putBytes("shapefile_bundle.zip", UniversalExportEngine.buildShapefileZipBytes("bhumi_plot", wps))
        }
        return baos.toByteArray()
    }
}
