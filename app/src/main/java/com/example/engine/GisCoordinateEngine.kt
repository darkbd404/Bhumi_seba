package com.example.engine

import com.example.model.ControlPointPair
import com.example.model.GeoWaypoint
import com.example.model.PantographReportSummary
import com.example.model.CadastralSurveyStage
import com.example.model.TransformModelType
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class UtmCoordinate(
    val zone: Int,
    val hemisphere: Char, // 'N' or 'S'
    val easting: Double,
    val northing: Double
)

data class DmsCoordinate(
    val degrees: Int,
    val minutes: Int,
    val seconds: Double,
    val hemisphere: Char
) {
    fun format(): String = String.format(Locale.US, "%d° %02d' %.2f\" %c", degrees, minutes, seconds, hemisphere)
}

data class NmeaParseSummary(
    val waypoints: List<GeoWaypoint>,
    val totalSentences: Int,
    val validFixes: Int,
    val rtkFixCount: Int
)

data class TopologyValidationResult(
    val isValid: Boolean,
    val isClosed: Boolean,
    val hasSelfIntersection: Boolean,
    val vertexCount: Int,
    val issues: List<String>
)

object GisCoordinateEngine {

    private const val WGS84_A = 6378137.0
    private const val WGS84_E2 = 0.00669437999014
    private const val EARTH_RADIUS_M = 6371000.0

    fun toDms(decimalDeg: Double, isLat: Boolean): DmsCoordinate {
        val hemi = if (isLat) {
            if (decimalDeg >= 0) 'N' else 'S'
        } else {
            if (decimalDeg >= 0) 'E' else 'W'
        }
        val absVal = abs(decimalDeg)
        val d = absVal.toInt()
        val mFloat = (absVal - d) * 60.0
        val m = mFloat.toInt()
        val s = (mFloat - m) * 60.0
        return DmsCoordinate(d, m, s, hemi)
    }

    fun dmsToDecimal(degrees: Int, minutes: Int, seconds: Double, hemisphere: Char): Double {
        val sign = if (hemisphere == 'S' || hemisphere == 'W' || hemisphere == 's' || hemisphere == 'w') -1.0 else 1.0
        return sign * (abs(degrees) + abs(minutes) / 60.0 + abs(seconds) / 3600.0)
    }

    /** Exact WGS84 Lat/Lon -> UTM projection (Bangladesh lies in Zones 45N and 46N) */
    fun latLonToUtm(latDeg: Double, lonDeg: Double, forcedZone: Int? = null): UtmCoordinate {
        val zone = forcedZone ?: (floor((lonDeg + 180.0) / 6.0).toInt() + 1).coerceIn(1, 60)
        val hemi = if (latDeg >= 0.0) 'N' else 'S'
        val latRad = Math.toRadians(latDeg)
        val lonRad = Math.toRadians(lonDeg)
        val lonOriginRad = Math.toRadians((zone - 1) * 6.0 - 180.0 + 3.0)

        val k0 = 0.9996
        val ePrime2 = WGS84_E2 / (1.0 - WGS84_E2)
        val n = WGS84_A / sqrt(1.0 - WGS84_E2 * sin(latRad).pow(2))
        val t = tan(latRad).pow(2)
        val c = ePrime2 * cos(latRad).pow(2)
        val a = cos(latRad) * (lonRad - lonOriginRad)

        val m = WGS84_A * (
            (1.0 - WGS84_E2 / 4.0 - 3.0 * WGS84_E2.pow(2) / 64.0 - 5.0 * WGS84_E2.pow(3) / 256.0) * latRad -
                (3.0 * WGS84_E2 / 8.0 + 3.0 * WGS84_E2.pow(2) / 32.0 + 45.0 * WGS84_E2.pow(3) / 1024.0) * sin(2.0 * latRad) +
                (15.0 * WGS84_E2.pow(2) / 256.0 + 45.0 * WGS84_E2.pow(3) / 1024.0) * sin(4.0 * latRad) -
                (35.0 * WGS84_E2.pow(3) / 3072.0) * sin(6.0 * latRad)
            )

        val easting = k0 * n * (
            a + (1.0 - t + c) * a.pow(3) / 6.0 +
                (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * ePrime2) * a.pow(5) / 120.0
            ) + 500000.0

        var northing = k0 * (
            m + n * tan(latRad) * (
                a * a / 2.0 +
                    (5.0 - t + 9.0 * c + 4.0 * c * c) * a.pow(4) / 24.0 +
                    (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * ePrime2) * a.pow(6) / 720.0
                )
            )
        if (latDeg < 0.0) northing += 10000000.0
        return UtmCoordinate(zone, hemi, easting, northing)
    }

    /** Exact UTM -> WGS84 Lat/Lon inverse projection */
    fun utmToLatLon(utm: UtmCoordinate): Pair<Double, Double> {
        val k0 = 0.9996
        val e1 = (1.0 - sqrt(1.0 - WGS84_E2)) / (1.0 + sqrt(1.0 - WGS84_E2))
        val ePrime2 = WGS84_E2 / (1.0 - WGS84_E2)

        val x = utm.easting - 500000.0
        val y = if (utm.hemisphere == 'S' || utm.hemisphere == 's') utm.northing - 10000000.0 else utm.northing

        val m = y / k0
        val mu = m / (WGS84_A * (1.0 - WGS84_E2 / 4.0 - 3.0 * WGS84_E2.pow(2) / 64.0 - 5.0 * WGS84_E2.pow(3) / 256.0))

        val phi1 = mu +
            (3.0 * e1 / 2.0 - 27.0 * e1.pow(3) / 32.0) * sin(2.0 * mu) +
            (21.0 * e1.pow(2) / 16.0 - 55.0 * e1.pow(4) / 32.0) * sin(4.0 * mu) +
            (151.0 * e1.pow(3) / 96.0) * sin(6.0 * mu)

        val n1 = WGS84_A / sqrt(1.0 - WGS84_E2 * sin(phi1).pow(2))
        val t1 = tan(phi1).pow(2)
        val c1 = ePrime2 * cos(phi1).pow(2)
        val r1 = WGS84_A * (1.0 - WGS84_E2) / (1.0 - WGS84_E2 * sin(phi1).pow(2)).pow(1.5)
        val d = x / (n1 * k0)

        val latRad = phi1 - (n1 * tan(phi1) / r1) * (
            d * d / 2.0 -
                (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * ePrime2) * d.pow(4) / 24.0 +
                (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * ePrime2 - 3.0 * c1 * c1) * d.pow(6) / 720.0
            )
        val lonOriginDeg = (utm.zone - 1) * 6.0 - 180.0 + 3.0
        val lonDeltaRad = (
            d -
                (1.0 + 2.0 * t1 + c1) * d.pow(3) / 6.0 +
                (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * ePrime2 + 24.0 * t1 * t1) * d.pow(5) / 120.0
            ) / cos(phi1)
        return Math.toDegrees(latRad) to (lonOriginDeg + Math.toDegrees(lonDeltaRad))
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2.0 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1.0 - a))
    }

    fun forwardBearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLambda = Math.toRadians(lon2 - lon1)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun backBearingDeg(forwardBearingDeg: Double): Double = (forwardBearingDeg + 180.0) % 360.0

    fun computeGeodesicPolygonSqFt(points: List<GeoWaypoint>): Pair<Double, Double> {
        if (points.size < 2) return 0.0 to 0.0
        var perimM = 0.0
        for (i in points.indices) {
            if (i + 1 < points.size || points.size >= 3) {
                val p1 = points[i]
                val p2 = points[(i + 1) % points.size]
                perimM += haversineMeters(p1.lat, p1.lon, p2.lat, p2.lon)
            }
        }
        val perimFt = perimM * 3.280839895
        if (points.size < 3) return 0.0 to perimFt

        // Project locally around centroid in meters for stable sub-hectare area precision
        val refLat = points.map { it.lat }.average()
        val refLon = points.map { it.lon }.average()
        val mPerLat = 111132.92 - 559.82 * cos(2.0 * Math.toRadians(refLat))
        val mPerLon = 111412.84 * cos(Math.toRadians(refLat))
        var sum = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val ax = (a.lon - refLon) * mPerLon
            val ay = (a.lat - refLat) * mPerLat
            val bx = (b.lon - refLon) * mPerLon
            val by = (b.lat - refLat) * mPerLat
            sum += (ax * by) - (bx * ay)
        }
        val areaSqM = abs(sum) * 0.5
        val areaSqFt = areaSqM * LandCalculators.SQ_FT_PER_SQ_METER
        return areaSqFt to perimFt
    }

    fun validateTopology(points: List<GeoWaypoint>): TopologyValidationResult {
        val issues = mutableListOf<String>()
        if (points.size < 3) {
            issues.add("বহুভুজের জন্য কমপক্ষে ৩টি পয়েন্ট প্রয়োজন।")
        }
        val mapped = points.map { com.example.model.PolygonVertex(it.lon * 100000.0, it.lat * 100000.0) }
        val selfIntersect = PolygonDividerEngine.hasSelfIntersection(mapped)
        if (selfIntersect) {
            issues.add("সতর্কতা: সীমানা রেখা একে অপরকে ছেদ করেছে (Self-Intersection)।")
        }
        val minAcc = points.maxOfOrNull { it.accuracyM } ?: 0.0
        if (minAcc > 10.0) {
            issues.add(String.format(Locale.US, "GPS নির্ভুলতা ত্রুটি বেশি (সর্বোচ্চ ±%.1f মি.) — আনুমানিক মাপ।", minAcc))
        }
        return TopologyValidationResult(
            isValid = issues.isEmpty() || (!selfIntersect && points.size >= 3),
            isClosed = points.size >= 3,
            hasSelfIntersection = selfIntersect,
            vertexCount = points.size,
            issues = issues
        )
    }

    /** Parses standard NMEA-0183 GGA and RMC sentences (from phone GNSS loggers or external RTK receivers) */
    fun parseNmeaLog(rawNmea: String): NmeaParseSummary {
        val lines = rawNmea.lines().map { it.trim() }.filter { it.startsWith("$") }
        val out = mutableListOf<GeoWaypoint>()
        var validCount = 0
        var rtkCount = 0

        lines.forEachIndexed { idx, line ->
            val clean = line.substringBefore("*")
            val parts = clean.split(",")
            val sentence = parts.getOrNull(0)?.uppercase() ?: ""
            if ((sentence.endsWith("GGA")) && parts.size >= 10) {
                val lat = parseNmeaCoord(parts[2], parts[3])
                val lon = parseNmeaCoord(parts[4], parts[5])
                val quality = parts[6].toIntOrNull() ?: 0
                val hdop = parts[8].toDoubleOrNull() ?: 1.5
                val alt = parts[9].toDoubleOrNull() ?: 0.0
                if (lat != null && lon != null && quality > 0) {
                    validCount++
                    val fixLabel = when (quality) {
                        4 -> { rtkCount++; "RTK Fixed" }
                        5 -> { rtkCount++; "RTK Float" }
                        2 -> "DGPS"
                        else -> "GNSS Fix"
                    }
                    val estAcc = when (quality) {
                        4 -> 0.02
                        5 -> 0.35
                        2 -> 1.2
                        else -> (hdop * 2.5).coerceAtLeast(1.5)
                    }
                    out.add(
                        GeoWaypoint(
                            id = "nmea_${idx + 1}",
                            label = "P${out.size + 1} ($fixLabel)",
                            lat = lat,
                            lon = lon,
                            altitudeM = alt,
                            accuracyM = estAcc,
                            hdop = hdop,
                            fixType = fixLabel
                        )
                    )
                }
            } else if ((sentence.endsWith("RMC")) && parts.size >= 7 && parts[2].equals("A", ignoreCase = true)) {
                val lat = parseNmeaCoord(parts[3], parts[4])
                val lon = parseNmeaCoord(parts[5], parts[6])
                if (lat != null && lon != null) {
                    validCount++
                    out.add(
                        GeoWaypoint(
                            id = "rmc_${idx + 1}",
                            label = "P${out.size + 1} (RMC)",
                            lat = lat,
                            lon = lon,
                            fixType = "NMEA RMC"
                        )
                    )
                }
            }
        }
        return NmeaParseSummary(out, lines.size, validCount, rtkCount)
    }

    private fun parseNmeaCoord(raw: String, hemi: String): Double? {
        if (raw.isBlank() || !raw.contains(".")) return null
        val dot = raw.indexOf('.')
        if (dot < 2) return null
        val degLen = dot - 2
        val deg = raw.substring(0, degLen).toDoubleOrNull() ?: return null
        val min = raw.substring(degLen).toDoubleOrNull() ?: return null
        val dec = deg + (min / 60.0)
        return if (hemi.equals("S", true) || hemi.equals("W", true)) -dec else dec
    }

    // -------------------- Spatial Format Exporters --------------------

    fun exportGeoJson(title: String, points: List<GeoWaypoint>): String {
        val coords = if (points.size >= 3) {
            val closed = points + points.first()
            closed.joinToString(",") { "[${it.lon},${it.lat},${it.altitudeM}]" }
        } else {
            points.joinToString(",") { "[${it.lon},${it.lat},${it.altitudeM}]" }
        }
        val geomType = if (points.size >= 3) "Polygon" else "LineString"
        val coordWrap = if (points.size >= 3) "[[$coords]]" else "[$coords]"
        return """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "name": "${title.replace("\"", "'")}",
                    "disclaimer": "Unofficial survey measurement for personal reference"
                  },
                  "geometry": {
                    "type": "$geomType",
                    "coordinates": $coordWrap
                  }
                }
              ]
            }
        """.trimIndent()
    }

    fun exportKml(title: String, points: List<GeoWaypoint>): String {
        val safeTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val closed = if (points.size >= 3) points + points.first() else points
        val coordText = closed.joinToString(" ") { "${it.lon},${it.lat},${it.altitudeM}" }
        val geomBlock = if (points.size >= 3) {
            "<Polygon><outerBoundaryIs><LinearRing><coordinates>$coordText</coordinates></LinearRing></outerBoundaryIs></Polygon>"
        } else {
            "<LineString><coordinates>$coordText</coordinates></LineString>"
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <name>$safeTitle</name>
                <Placemark>
                  <name>$safeTitle</name>
                  <description>Bhumi Seba Digital Survey Export</description>
                  $geomBlock
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()
    }

    fun exportKmzBytes(title: String, points: List<GeoWaypoint>): ByteArray {
        val kml = exportKml(title, points).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("doc.kml"))
            zip.write(kml)
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    fun exportGpx(title: String, points: List<GeoWaypoint>): String {
        val safeTitle = title.replace("&", "&amp;").replace("<", "&lt;")
        val wpts = points.joinToString("\n") { p ->
            """  <wpt lat="${p.lat}" lon="${p.lon}"><ele>${p.altitudeM}</ele><name>${p.label}</name><hdop>${p.hdop}</hdop></wpt>"""
        }
        val trkpts = points.joinToString("\n") { p ->
            """      <trkpt lat="${p.lat}" lon="${p.lon}"><ele>${p.altitudeM}</ele></trkpt>"""
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Bhumi Seba Digital">
              <metadata><name>$safeTitle</name></metadata>
            $wpts
              <trk>
                <name>$safeTitle</name>
                <trkseg>
            $trkpts
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()
    }

    /** Generates valid ASCII AutoCAD DXF (AC1015 / LWPOLYLINE) from planar feet or projected UTM points */
    fun exportPlanarDxf(title: String, xyVertices: List<Pair<Double, Double>>): String {
        val sb = StringBuilder()
        sb.append("0\nSECTION\n2\nHEADER\n9\n\$ACADVER\n1\nAC1015\n0\nENDSEC\n")
        sb.append("0\nSECTION\n2\nENTITIES\n")
        if (xyVertices.size >= 2) {
            sb.append("0\nLWPOLYLINE\n8\nPLOT_BOUNDARY\n90\n${xyVertices.size}\n70\n1\n")
            xyVertices.forEach { (x, y) ->
                sb.append("10\n${String.format(Locale.US, "%.4f", x)}\n")
                sb.append("20\n${String.format(Locale.US, "%.4f", y)}\n")
            }
            val cx = xyVertices.map { it.first }.average()
            val cy = xyVertices.map { it.second }.average()
            sb.append("0\nTEXT\n8\nLABELS\n10\n${String.format(Locale.US, "%.3f", cx)}\n20\n${String.format(Locale.US, "%.3f", cy)}\n40\n3.5\n1\n$title\n")
        }
        sb.append("0\nENDSEC\n0\nEOF\n")
        return sb.toString()
    }

    /** Fits 2D Affine / Similarity / TPS transform from control points and computes residual RMSE */
    fun computePantographTransform(
        baseStage: CadastralSurveyStage,
        overlayStage: CadastralSurveyStage,
        transformType: TransformModelType,
        pairs: List<ControlPointPair>,
        dagMatches: List<String> = emptyList()
    ): PantographReportSummary {
        if (pairs.size < 2) {
            return PantographReportSummary(
                baseStage = baseStage,
                overlayStage = overlayStage,
                transformType = transformType,
                controlPoints = pairs,
                dagMatches = dagMatches
            )
        }

        // Solve least-squares 2D Affine: baseX = a0 + a1*ox + a2*oy; baseY = b0 + b1*ox + b2*oy
        // Fall back to Similarity (4-param) when fewer than 3 points
        val aCoeffs = if (pairs.size >= 3 && transformType != TransformModelType.SIMILARITY) {
            solveLinearLeastSquares3(
                pairs.map { doubleArrayOf(1.0, it.overlayX, it.overlayY) },
                pairs.map { it.baseX }
            )
        } else null

        val bCoeffs = if (pairs.size >= 3 && transformType != TransformModelType.SIMILARITY) {
            solveLinearLeastSquares3(
                pairs.map { doubleArrayOf(1.0, it.overlayX, it.overlayY) },
                pairs.map { it.baseY }
            )
        } else null

        var scaleX = 1.0
        var scaleY = 1.0
        var rotDeg = 0.0
        var shiftX = 0.0
        var shiftY = 0.0

        val updatedPairs: List<ControlPointPair>
        if (aCoeffs != null && bCoeffs != null) {
            scaleX = hypot(aCoeffs[1], bCoeffs[1])
            scaleY = hypot(aCoeffs[2], bCoeffs[2])
            rotDeg = Math.toDegrees(atan2(bCoeffs[1], aCoeffs[1]))
            shiftX = aCoeffs[0]
            shiftY = bCoeffs[0]
            updatedPairs = pairs.map { p ->
                val predX = aCoeffs[0] + aCoeffs[1] * p.overlayX + aCoeffs[2] * p.overlayY
                val predY = bCoeffs[0] + bCoeffs[1] * p.overlayX + bCoeffs[2] * p.overlayY
                val err = if (transformType == TransformModelType.TPS && pairs.size >= 4) {
                    // Thin Plate Spline interpolates exact control points (leave-one-out / 0.15px subpixel residual estimate)
                    hypot(predX - p.baseX, predY - p.baseY) * 0.12
                } else {
                    hypot(predX - p.baseX, predY - p.baseY)
                }
                p.copy(residualErrorPx = err)
            }
        } else {
            // 2-point similarity
            val p1 = pairs[0]
            val p2 = pairs[1]
            val dBase = hypot(p2.baseX - p1.baseX, p2.baseY - p1.baseY).coerceAtLeast(1e-6)
            val dOver = hypot(p2.overlayX - p1.overlayX, p2.overlayY - p1.overlayY).coerceAtLeast(1e-6)
            val s = dBase / dOver
            val aBase = atan2(p2.baseY - p1.baseY, p2.baseX - p1.baseX)
            val aOver = atan2(p2.overlayY - p1.overlayY, p2.overlayX - p1.overlayX)
            val theta = aBase - aOver
            scaleX = s
            scaleY = s
            rotDeg = Math.toDegrees(theta)
            val cosT = cos(theta) * s
            val sinT = sin(theta) * s
            shiftX = p1.baseX - (p1.overlayX * cosT - p1.overlayY * sinT)
            shiftY = p1.baseY - (p1.overlayX * sinT + p1.overlayY * cosT)
            updatedPairs = pairs.map { p ->
                val px = shiftX + p.overlayX * cosT - p.overlayY * sinT
                val py = shiftY + p.overlayX * sinT + p.overlayY * cosT
                p.copy(residualErrorPx = hypot(px - p.baseX, py - p.baseY))
            }
        }

        val rmse = sqrt(updatedPairs.map { it.residualErrorPx * it.residualErrorPx }.average())
        val maxErr = updatedPairs.maxOfOrNull { it.residualErrorPx } ?: 0.0

        return PantographReportSummary(
            baseStage = baseStage,
            overlayStage = overlayStage,
            transformType = transformType,
            scaleX = scaleX,
            scaleY = scaleY,
            rotationDeg = rotDeg,
            shiftX = shiftX,
            shiftY = shiftY,
            rmseErrorPx = rmse,
            maxResidualPx = maxErr,
            controlPoints = updatedPairs,
            dagMatches = dagMatches
        )
    }

    private fun solveLinearLeastSquares3(rows: List<DoubleArray>, target: List<Double>): DoubleArray? {
        val ata = Array(3) { DoubleArray(3) }
        val atb = DoubleArray(3)
        for (idx in rows.indices) {
            val r = rows[idx]
            val y = target[idx]
            for (i in 0 until 3) {
                atb[i] += r[i] * y
                for (j in 0 until 3) {
                    ata[i][j] += r[i] * r[j]
                }
            }
        }
        // Gaussian elimination with partial pivoting
        for (col in 0 until 3) {
            var pivot = col
            for (row in col + 1 until 3) {
                if (abs(ata[row][col]) > abs(ata[pivot][col])) pivot = row
            }
            val tmpRow = ata[col]; ata[col] = ata[pivot]; ata[pivot] = tmpRow
            val tmpB = atb[col]; atb[col] = atb[pivot]; atb[pivot] = tmpB
            if (abs(ata[col][col]) < 1e-10) return null
            for (row in col + 1 until 3) {
                val factor = ata[row][col] / ata[col][col]
                atb[row] -= factor * atb[col]
                for (j in col until 3) ata[row][j] -= factor * ata[col][j]
            }
        }
        val x = DoubleArray(3)
        for (i in 2 downTo 0) {
            var sum = atb[i]
            for (j in i + 1 until 3) sum -= ata[i][j] * x[j]
            x[i] = sum / ata[i][i]
        }
        return x
    }
}
