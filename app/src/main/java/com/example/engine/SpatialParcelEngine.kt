package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import com.example.model.ImportedSpatialParcel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class KmzDemarcationMetadata(
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val dagNumber: String = "",
    val ownerName: String = "",
    val scaleLabel: String = "1:3,960",
    val includeOverlayImage: Boolean = true
)

data class CoordinateConversions(
    val lat: Double,
    val lon: Double,
    val dmsLat: String,
    val dmsLon: String,
    val utmZone: Int,
    val utmEasting: Double,
    val utmNorthing: Double,
    val btmEasting: Double,
    val btmNorthing: Double
)

object SpatialParcelEngine {

    private const val EARTH_RADIUS_M = 6378137.0

    fun geodesicAreaSqMeters(pointsLatLon: List<Pair<Double, Double>>): Double {
        if (pointsLatLon.size < 3) return 0.0
        var area = 0.0
        for (i in pointsLatLon.indices) {
            val p1 = pointsLatLon[i]
            val p2 = pointsLatLon[(i + 1) % pointsLatLon.size]
            area += Math.toRadians(p2.second - p1.second) *
                (2.0 + sin(Math.toRadians(p1.first)) + sin(Math.toRadians(p2.first)))
        }
        return abs(area * EARTH_RADIUS_M * EARTH_RADIUS_M / 2.0)
    }

    fun haversineMeters(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val dLat = Math.toRadians(b.first - a.first)
        val dLon = Math.toRadians(b.second - a.second)
        val la1 = Math.toRadians(a.first)
        val la2 = Math.toRadians(b.first)
        val s1 = sin(dLat / 2.0)
        val s2 = sin(dLon / 2.0)
        val h = s1 * s1 + cos(la1) * cos(la2) * s2 * s2
        return 2.0 * 6371000.0 * atan2(sqrt(h), sqrt((1.0 - h).coerceAtLeast(0.0)))
    }

    fun parseSpatialFileBytes(fileName: String, bytes: ByteArray): List<ImportedSpatialParcel> {
        val lower = fileName.lowercase(Locale.US)
        return when {
            lower.endsWith(".kmz") -> parseKmz(bytes)
            lower.endsWith(".kml") -> parseKml(String(bytes, StandardCharsets.UTF_8))
            lower.endsWith(".geojson") || lower.endsWith(".json") -> parseGeoJson(String(bytes, StandardCharsets.UTF_8))
            lower.endsWith(".dxf") -> parseDxf(String(bytes, StandardCharsets.UTF_8))
            else -> {
                val txt = String(bytes, StandardCharsets.UTF_8).trimStart()
                when {
                    txt.startsWith("{") -> parseGeoJson(txt)
                    txt.startsWith("<") -> parseKml(txt)
                    txt.contains("SECTION") && txt.contains("ENTITIES") -> parseDxf(txt)
                    else -> emptyList()
                }
            }
        }
    }

    fun parseGeoJson(jsonText: String): List<ImportedSpatialParcel> = runCatching {
        val out = ArrayList<ImportedSpatialParcel>()
        val root = JSONObject(jsonText)
        val features = when (root.optString("type")) {
            "FeatureCollection" -> root.optJSONArray("features") ?: JSONArray()
            "Feature" -> JSONArray().apply { put(root) }
            else -> JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "Feature")
                    put("properties", JSONObject())
                    put("geometry", root)
                })
            }
        }
        for (i in 0 until features.length()) {
            val feat = features.optJSONObject(i) ?: continue
            val props = feat.optJSONObject("properties")
            val name = props?.optString("name", "")?.ifBlank { props.optString("NAME", "") } ?: ""
            val dag = props?.optString("dag", "")?.ifBlank { props.optString("DAG_NO", props.optString("plot_no", "")) } ?: ""
            val owner = props?.optString("owner", "")?.ifBlank { props.optString("OWNER", "") } ?: ""
            val geom = feat.optJSONObject("geometry") ?: continue
            val type = geom.optString("type")
            val coords = geom.optJSONArray("coordinates") ?: continue

            fun addRing(ringArr: JSONArray?, idxSuffix: String) {
                if (ringArr == null || ringArr.length() < 3) return
                val pts = ArrayList<Pair<Double, Double>>(ringArr.length())
                for (k in 0 until ringArr.length()) {
                    val c = ringArr.optJSONArray(k) ?: continue
                    if (c.length() >= 2) {
                        val lon = c.optDouble(0)
                        val lat = c.optDouble(1)
                        if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                            pts.add(lat to lon)
                        }
                    }
                }
                val cleaned = if (pts.size > 3 && pts.first() == pts.last()) pts.dropLast(1) else pts
                if (cleaned.size >= 3) {
                    val sqFt = geodesicAreaSqMeters(cleaned) * LandCalculators.SQ_FT_PER_SQ_METER
                    out.add(
                        ImportedSpatialParcel(
                            id = "geojson_${i}_$idxSuffix",
                            name = name.ifBlank { "প্লট #${out.size + 1}" },
                            dagNumber = dag,
                            ownerName = owner,
                            pointsLatLon = cleaned,
                            areaSqFt = sqFt,
                            decimal = sqFt / LandCalculators.SQ_FT_PER_DECIMAL,
                            sourceFormat = "GeoJSON"
                        )
                    )
                }
            }

            if (type == "Polygon") {
                addRing(coords.optJSONArray(0), "0")
            } else if (type == "MultiPolygon") {
                for (p in 0 until coords.length()) {
                    val poly = coords.optJSONArray(p)
                    addRing(poly?.optJSONArray(0), "$p")
                }
            }
        }
        out
    }.getOrDefault(emptyList())

    fun parseKml(kmlText: String): List<ImportedSpatialParcel> {
        val out = ArrayList<ImportedSpatialParcel>()
        val placemarkRx = Regex("""<Placemark\b[^>]*>([\s\S]*?)</Placemark>""", RegexOption.IGNORE_CASE)
        val nameRx = Regex("""<name>([\s\S]*?)</name>""", RegexOption.IGNORE_CASE)
        val coordRx = Regex("""<coordinates>([\s\S]*?)</coordinates>""", RegexOption.IGNORE_CASE)

        val blocks = placemarkRx.findAll(kmlText).toList()
        if (blocks.isEmpty()) {
            coordRx.findAll(kmlText).forEachIndexed { idx, m ->
                val pts = parseKmlCoordString(m.groupValues[1])
                if (pts.size >= 3) {
                    val sqFt = geodesicAreaSqMeters(pts) * LandCalculators.SQ_FT_PER_SQ_METER
                    out.add(
                        ImportedSpatialParcel(
                            id = "kml_$idx",
                            name = "KML প্লট #${idx + 1}",
                            dagNumber = "${idx + 1}",
                            ownerName = "",
                            pointsLatLon = pts,
                            areaSqFt = sqFt,
                            decimal = sqFt / LandCalculators.SQ_FT_PER_DECIMAL,
                            sourceFormat = "KML"
                        )
                    )
                }
            }
            return out
        }

        blocks.forEachIndexed { idx, pm ->
            val block = pm.groupValues[1]
            val pName = nameRx.find(block)?.groupValues?.get(1)?.trim() ?: "KML প্লট #${idx + 1}"
            val coordsMatch = coordRx.find(block) ?: return@forEachIndexed
            val pts = parseKmlCoordString(coordsMatch.groupValues[1])
            if (pts.size >= 3) {
                val sqFt = geodesicAreaSqMeters(pts) * LandCalculators.SQ_FT_PER_SQ_METER
                out.add(
                    ImportedSpatialParcel(
                        id = "kml_$idx",
                        name = pName,
                        dagNumber = Regex("""\d+""").find(pName)?.value ?: "",
                        ownerName = "",
                        pointsLatLon = pts,
                        areaSqFt = sqFt,
                        decimal = sqFt / LandCalculators.SQ_FT_PER_DECIMAL,
                        sourceFormat = "KML"
                    )
                )
            }
        }
        return out
    }

    private fun parseKmlCoordString(raw: String): List<Pair<Double, Double>> {
        val pts = raw.trim().split(Regex("""\s+""")).mapNotNull { token ->
            val parts = token.split(',')
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull()
                val lat = parts[1].toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    lat to lon
                } else null
            } else null
        }
        return if (pts.size > 3 && pts.first() == pts.last()) pts.dropLast(1) else pts
    }

    fun parseKmz(kmzBytes: ByteArray): List<ImportedSpatialParcel> {
        val zis = ZipInputStream(ByteArrayInputStream(kmzBytes))
        var entry: ZipEntry? = zis.nextEntry
        val all = ArrayList<ImportedSpatialParcel>()
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                val kmlBytes = zis.readBytes()
                all += parseKml(String(kmlBytes, StandardCharsets.UTF_8)).map {
                    it.copy(sourceFormat = "KMZ")
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        return all
    }

    /**
     * Parses ASCII DXF LWPOLYLINE / POLYLINE vertices.
     * Supports geographic lon/lat OR BTM/UTM meters (auto-converts BTM/UTM around Bangladesh if x > 180).
     */
    fun parseDxf(dxfText: String): List<ImportedSpatialParcel> {
        val lines = dxfText.lines().map { it.trim() }
        val out = ArrayList<ImportedSpatialParcel>()
        var i = 0
        while (i + 1 < lines.size) {
            if (lines[i] == "0" && lines[i + 1] == "LWPOLYLINE") {
                i += 2
                val rawPts = ArrayList<Pair<Double, Double>>()
                var curX: Double? = null
                while (i + 1 < lines.size && lines[i] != "0") {
                    val code = lines[i]
                    val valStr = lines[i + 1]
                    if (code == "10") {
                        curX = valStr.toDoubleOrNull()
                    } else if (code == "20" && curX != null) {
                        val y = valStr.toDoubleOrNull()
                        if (y != null) rawPts.add(curX to y)
                        curX = null
                    }
                    i += 2
                }
                if (rawPts.size >= 3) {
                    val isLonLat = rawPts.all { it.first in -180.0..180.0 && it.second in -90.0..90.0 }
                    val latLonPts = if (isLonLat) {
                        rawPts.map { it.second to it.first }
                    } else {
                        // Assume BTM meters around Bangladesh (Central Meridian 90°E, False Easting 500000, False Northing -2000000)
                        rawPts.map { (e, n) -> btmMetersToLatLon(e, n) }
                    }
                    val sqFt = geodesicAreaSqMeters(latLonPts) * LandCalculators.SQ_FT_PER_SQ_METER
                    out.add(
                        ImportedSpatialParcel(
                            id = "dxf_${out.size}",
                            name = "DXF প্লট #${out.size + 1}",
                            dagNumber = "${out.size + 1}",
                            ownerName = "",
                            pointsLatLon = latLonPts,
                            areaSqFt = sqFt,
                            decimal = sqFt / LandCalculators.SQ_FT_PER_DECIMAL,
                            sourceFormat = "DXF"
                        )
                    )
                }
            } else {
                i++
            }
        }
        return out
    }

    fun isPointInLatLonPolygon(lat: Double, lon: Double, poly: List<Pair<Double, Double>>): Boolean {
        if (poly.size < 3) return false
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val yi = poly[i].first
            val xi = poly[i].second
            val yj = poly[j].first
            val xj = poly[j].second
            val intersect = ((yi > lat) != (yj > lat)) &&
                (lon < (xj - xi) * (lat - yi) / ((yj - yi).let { if (abs(it) < 1e-12) 1e-12 else it }) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun findParcelAt(lat: Double, lon: Double, parcels: List<ImportedSpatialParcel>): ImportedSpatialParcel? {
        return parcels.firstOrNull { isPointInLatLonPolygon(lat, lon, it.pointsLatLon) }
    }

    /**
     * Road Width & Setback calculation in feet from polygon vertices / point to road line.
     */
    fun computeRoadSetbackFt(
        plotPointsLatLon: List<Pair<Double, Double>>,
        roadALatLon: Pair<Double, Double>,
        roadBLatLon: Pair<Double, Double>
    ): Double {
        if (plotPointsLatLon.isEmpty()) return 0.0
        val refLat = plotPointsLatLon.map { it.first }.average()
        val mPerDegLat = 111132.92
        val mPerDegLon = 111412.84 * cos(Math.toRadians(refLat))
        val ax = roadALatLon.second * mPerDegLon
        val ay = roadALatLon.first * mPerDegLat
        val bx = roadBLatLon.second * mPerDegLon
        val by = roadBLatLon.first * mPerDegLat
        val abx = bx - ax
        val aby = by - ay
        val abSq = (abx * abx + aby * aby).coerceAtLeast(1e-9)

        var minDistM = Double.MAX_VALUE
        plotPointsLatLon.forEach { (la, lo) ->
            val px = lo * mPerDegLon
            val py = la * mPerDegLat
            val t = (((px - ax) * abx + (py - ay) * aby) / abSq).coerceIn(0.0, 1.0)
            val d = hypot(px - (ax + t * abx), py - (ay + t * aby))
            if (d < minDistM) minDistM = d
        }
        return if (minDistM == Double.MAX_VALUE) 0.0 else minDistM * 3.280839895
    }

    /**
     * Builds KMZ (Google Earth archive) containing optional georeferenced Mouza GroundOverlay
     * + Polygon Placemark + Bangladesh demarcation metadata HTML description balloon.
     */
    fun buildEnhancedKmzBytes(
        pointsLatLon: List<Pair<Double, Double>>,
        metadata: KmzDemarcationMetadata,
        overlayBitmap: Bitmap? = null,
        overlayBoundsNSEW: DoubleArray? = null // [north, south, east, west]
    ): ByteArray {
        val sqM = geodesicAreaSqMeters(pointsLatLon)
        val area = LandCalculators.fromSqFeet(sqM * LandCalculators.SQ_FT_PER_SQ_METER)
        val coordText = buildString {
            val closed = if (pointsLatLon.size >= 3) pointsLatLon + pointsLatLon.first() else pointsLatLon
            closed.forEach { (la, lo) ->
                append(String.format(Locale.US, "%.7f,%.7f,0 ", lo, la))
            }
        }.trim()

        val hasOverlay = metadata.includeOverlayImage && overlayBitmap != null && overlayBoundsNSEW != null && overlayBoundsNSEW.size == 4
        val kml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
            append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append('\n')
            append("  <Document>\n")
            append("    <name>মৌজা দাগ ${metadata.dagNumber.ifBlank { "জরিপ" }}</name>\n")
            append("""    <Style id="plotStyle"><LineStyle><color>ff4fd5ff</color><width>3</width></LineStyle><PolyStyle><color>55589d0f</color></PolyStyle></Style>""").append('\n')
            if (hasOverlay && overlayBoundsNSEW != null) {
                append("    <GroundOverlay>\n")
                append("      <name>Mouza Sheet Overlay (${metadata.scaleLabel})</name>\n")
                append("      <color>bfffffff</color>\n")
                append("      <Icon><href>files/mouza_overlay.png</href></Icon>\n")
                append("      <LatLonBox>\n")
                append(String.format(Locale.US, "        <north>%.7f</north>\n", overlayBoundsNSEW[0]))
                append(String.format(Locale.US, "        <south>%.7f</south>\n", overlayBoundsNSEW[1]))
                append(String.format(Locale.US, "        <east>%.7f</east>\n", overlayBoundsNSEW[2]))
                append(String.format(Locale.US, "        <west>%.7f</west>\n", overlayBoundsNSEW[3]))
                append("      </LatLonBox>\n")
                append("    </GroundOverlay>\n")
            }
            if (pointsLatLon.size >= 3) {
                append("    <Placemark>\n")
                append("      <name>দাগ নং: ${metadata.dagNumber.ifBlank { "-" }} (${String.format(Locale.US, "%.2f", area.decimal)} শতক)</name>\n")
                append("      <styleUrl>#plotStyle</styleUrl>\n")
                append("      <description><![CDATA[\n")
                append("        <b>জেলা:</b> ${metadata.district.ifBlank { "-" }}<br/>\n")
                append("        <b>উপজেলা:</b> ${metadata.upazila.ifBlank { "-" }}<br/>\n")
                append("        <b>মৌজা ও জেএল নং:</b> ${metadata.mouza.ifBlank { "-" }} (JL: ${metadata.jlNumber.ifBlank { "-" }})<br/>\n")
                append("        <b>দাগ নং:</b> ${metadata.dagNumber.ifBlank { "-" }}<br/>\n")
                append("        <b>মালিক/জরিপকার:</b> ${metadata.ownerName.ifBlank { "-" }}<br/>\n")
                append(String.format(Locale.US, "        <b>পরিমাণ:</b> %.3f শতক (%.3f কাঠা / %.0f বর্গফুট)<br/>\n", area.decimal, area.katha, area.sqFeet))
                append("        <b>মৌজা স্কেল:</b> ${metadata.scaleLabel}<br/>\n")
                append("        <i>দ্রষ্টব্য: এটি সার্ভে প্রাক্কলন; সরকারি সীমানা নির্ধারণের চূড়ান্ত বিকল্প নয়।</i>\n")
                append("      ]]></description>\n")
                append("      <Polygon><outerBoundaryIs><LinearRing><coordinates>$coordText</coordinates></LinearRing></outerBoundaryIs></Polygon>\n")
                append("    </Placemark>\n")
            }
            append("  </Document>\n</kml>\n")
        }

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("doc.kml"))
            zos.write(kml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            if (hasOverlay && overlayBitmap != null) {
                zos.putNextEntry(ZipEntry("files/mouza_overlay.png"))
                overlayBitmap.compress(Bitmap.CompressFormat.PNG, 90, zos)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    fun exportKmzToFile(context: Context, kmzBytes: ByteArray, dagHint: String): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "KmzExports").apply { mkdirs() }
        val safeDag = dagHint.replace(Regex("""[^A-Za-z0-9_-]"""), "_").ifBlank { "Plot" }
        val out = File(dir, "Mouza_${safeDag}_${System.currentTimeMillis()}.kmz")
        FileOutputStream(out).use { it.write(kmzBytes) }
        return out
    }

    fun convertCoordinates(lat: Double, lon: Double): CoordinateConversions {
        val dmsLat = formatDms(lat, isLat = true)
        val dmsLon = formatDms(lon, isLat = false)
        val utmZone = (floor((lon + 180.0) / 6.0).toInt() + 1).coerceIn(1, 60)
        val lon0Utm = (utmZone - 1) * 6.0 - 180.0 + 3.0
        val (utmE, utmN) = projectTransverseMercator(lat, lon, lon0Deg = lon0Utm, falseE = 500000.0, falseN = 0.0, k0 = 0.9996)
        // Bangladesh Transverse Mercator (BTM): Central Meridian 90.0°E, False Easting 500000, False Northing -2000000, k0 = 0.9996
        val (btmE, btmN) = projectTransverseMercator(lat, lon, lon0Deg = 90.0, falseE = 500000.0, falseN = -2000000.0, k0 = 0.9996)
        return CoordinateConversions(
            lat = lat,
            lon = lon,
            dmsLat = dmsLat,
            dmsLon = dmsLon,
            utmZone = utmZone,
            utmEasting = utmE,
            utmNorthing = utmN,
            btmEasting = btmE,
            btmNorthing = btmN
        )
    }

    private fun formatDms(deg: Double, isLat: Boolean): String {
        val hemi = if (isLat) if (deg >= 0) "N" else "S" else if (deg >= 0) "E" else "W"
        val absVal = abs(deg)
        val d = absVal.toInt()
        val mFull = (absVal - d) * 60.0
        val m = mFull.toInt()
        val s = (mFull - m) * 60.0
        return String.format(Locale.US, "%d° %02d' %05.2f\" %s", d, m, s, hemi)
    }

    private fun projectTransverseMercator(
        latDeg: Double,
        lonDeg: Double,
        lon0Deg: Double,
        falseE: Double,
        falseN: Double,
        k0: Double
    ): Pair<Double, Double> {
        val a = 6378137.0
        val f = 1.0 / 298.257223563
        val e2 = 2 * f - f * f
        val ep2 = e2 / (1.0 - e2)
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)
        val lon0 = Math.toRadians(lon0Deg)

        val n = a / sqrt(1.0 - e2 * sin(lat).pow(2))
        val t = tan(lat).pow(2)
        val c = ep2 * cos(lat).pow(2)
        val A = cos(lat) * (lon - lon0)

        val m = a * (
            (1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0) * lat -
                (3.0 * e2 / 8.0 + 3.0 * e2 * e2 / 32.0) * sin(2 * lat) +
                (15.0 * e2 * e2 / 256.0) * sin(4 * lat)
            )
        val easting = falseE + k0 * n * (A + (1.0 - t + c) * A.pow(3) / 6.0)
        val northing = falseN + k0 * (m + n * tan(lat) * (A * A / 2.0 + (5.0 - t + 9.0 * c + 4.0 * c * c) * A.pow(4) / 24.0))
        return easting to northing
    }

    private fun btmMetersToLatLon(easting: Double, northing: Double): Pair<Double, Double> {
        val latApprox = 23.8 + (northing - 635000.0) / 111132.92
        val lonApprox = 90.0 + (easting - 500000.0) / (111412.84 * cos(Math.toRadians(latApprox)))
        return latApprox.coerceIn(-85.0, 85.0) to lonApprox.coerceIn(-180.0, 180.0)
    }
}
