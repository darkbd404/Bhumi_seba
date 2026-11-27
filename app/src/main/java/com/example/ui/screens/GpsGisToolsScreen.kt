package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.GisCoordinateEngine
import com.example.engine.LandCalculators
import com.example.engine.UtmCoordinate
import com.example.model.FieldSurveyTrack
import com.example.model.GeoWaypoint
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import java.util.Locale
import java.util.UUID

@Composable
fun GpsGisToolsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Field GPS & NMEA, 1 = WGS84/UTM/DMS Converter
    var toast by remember { mutableStateOf<String?>(null) }

    var trackTitle by remember { mutableStateOf("ফিল্ড জরিপ ট্র্যাক ০১") }
    var waypoints by remember {
        mutableStateOf(
            listOf(
                GeoWaypoint("gp1", "কোণ ১", 23.81030, 90.41250, 11.0, 4.5, 1.1, 1.6, "Sample Ref"),
                GeoWaypoint("gp2", "কোণ ২", 23.81030, 90.41285, 11.2, 4.2, 1.1, 1.5, "Sample Ref"),
                GeoWaypoint("gp3", "কোণ ৩", 23.81000, 90.41285, 11.1, 4.0, 1.0, 1.5, "Sample Ref"),
                GeoWaypoint("gp4", "কোণ ৪", 23.81000, 90.41250, 11.0, 4.5, 1.1, 1.6, "Sample Ref")
            )
        )
    }
    var nmeaInput by remember {
        mutableStateOf(
            "\$GNGGA,061500.00,2348.6180,N,09024.7500,E,4,12,0.7,11.5,M,-46.2,M,,*61\n" +
                "\$GNGGA,061510.00,2348.6180,N,09024.7720,E,4,12,0.7,11.6,M,-46.2,M,,*62\n" +
                "\$GNGGA,061520.00,2348.6000,N,09024.7720,E,4,12,0.8,11.5,M,-46.2,M,,*63\n" +
                "\$GNGGA,061530.00,2348.6000,N,09024.7500,E,4,12,0.8,11.4,M,-46.2,M,,*64"
        )
    }

    // Coordinate converter state
    var latText by remember { mutableStateOf("23.810300") }
    var lonText by remember { mutableStateOf("90.412500") }
    var utmZoneText by remember { mutableStateOf("46") }
    var utmEastingText by remember { mutableStateOf("236500.0") }
    var utmNorthingText by remember { mutableStateOf("2635600.0") }

    @SuppressLint("MissingPermission")
    fun captureDeviceGpsPoint() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm?.getLastKnownLocation(it) }.getOrNull() }
            .minByOrNull { it.accuracy }
        if (loc != null) {
            val next = GeoWaypoint(
                id = UUID.randomUUID().toString(),
                label = "GPS P${waypoints.size + 1}",
                lat = loc.latitude,
                lon = loc.longitude,
                altitudeM = loc.altitude,
                accuracyM = loc.accuracy.toDouble(),
                hdop = (loc.accuracy / 3.5).coerceAtLeast(0.8),
                fixType = "Device GPS (±${loc.accuracy.toInt()}m)"
            )
            waypoints = waypoints + next
            toast = String.format(Locale.US, "GPS পয়েন্ট যুক্ত হয়েছে: %.6f, %.6f (±%.1fm)", loc.latitude, loc.longitude, loc.accuracy)
        } else {
            toast = "ডিভাইসে সাম্প্রতিক GPS সিগন্যাল পাওয়া যায়নি — বাইরে খোলা আকাশের নিচে চেষ্টা করুন বা NMEA পেস্ট করুন।"
        }
    }

    val locPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) captureDeviceGpsPoint() else toast = "লোকেশন অনুমতি না দেওয়ায় GPS পয়েন্ট নেওয়া যায়নি।"
    }

    val (areaSqFt, perimFt) = remember(waypoints) { GisCoordinateEngine.computeGeodesicPolygonSqFt(waypoints) }
    val topology = remember(waypoints) { GisCoordinateEngine.validateTopology(waypoints) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
        ) {
            WorkstationTopBar(
                titleBn = "GPS ফিল্ড সার্ভে ও GIS টুলস",
                subtitleEn = "GNSS/NMEA/RTK • Bearing • WGS84↔UTM↔DMS • KML/KMZ/GPX/GeoJSON/DXF",
                helpTitle = "GPS ও GIS নির্দেশিকা",
                helpSteps = listOf(
                    "জমির প্রতিটি কোণায় দাঁড়িয়ে 'GPS পয়েন্ট নিন' চাপুন অথবা বাহ্যিক RTK GNSS রিসিভারের NMEA (\$GPGGA/\$GNGGA) লগ পেস্ট করুন।",
                    "টপোলজি ভ্যালিডেটর সীমানায় ক্রস-লাইন (Self-Intersection) ও GPS নির্ভুলতা ত্রুটি পরীক্ষা করে।",
                    "ডাটা সংরক্ষণ করে GeoJSON, KML, KMZ, GPX অথবা AutoCAD DXF ফরম্যাটে রপ্তানি করতে পারবেন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "gps_gis_back_button"
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text("GPS ট্র্যাক ও NMEA/RTK") },
                    leadingIcon = { Icon(Icons.Rounded.GpsFixed, contentDescription = null) },
                    modifier = Modifier.testTag("tab_gps_field")
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("WGS84 ↔ UTM ↔ DMS") },
                    leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                    modifier = Modifier.testTag("tab_gis_coords")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        text = "অস্বীকৃতি: ফোনের সাধারণ GPS পরিমাপে ±৩ থেকে ±১০ মিটার ত্রুটি থাকতে পারে। এটি চূড়ান্ত আইনি বা সরকারি জরিপের বিকল্প নয়; নিখুঁত কাজের জন্য DGPS/RTK বা সরেজমিন ফিতা জরিপ যাচাই করুন।"
                    )
                }

                if (activeTab == 0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = trackTitle,
                                onValueChange = { trackTitle = it },
                                label = { Text("সার্ভে ট্র্যাকের নাম") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (hasFine) {
                                            captureDeviceGpsPoint()
                                        } else {
                                            locPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("capture_gps_point")
                                ) {
                                    Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("GPS পয়েন্ট নিন")
                                }
                                OutlinedButton(
                                    onClick = { waypoints = emptyList() },
                                    modifier = Modifier.weight(0.6f)
                                ) {
                                    Text("রিসেট")
                                }
                            }

                            // Results Summary & Topology Check
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "ক্ষেত্রফল: %.3f শতাংশ (%.2f ব.ফু) • পরিসীমা: %.1f ফু",
                                    areaSqFt / LandCalculators.SQ_FT_PER_DECIMAL,
                                    areaSqFt,
                                    perimFt
                                ),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = if (topology.isValid) EmeraldPrimary else Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (topology.hasSelfIntersection) {
                                        "টপোলজি সতর্কতা: সীমানা রেখা ক্রস করেছে!"
                                    } else {
                                        "টপোলজি যাচাই: ${topology.vertexCount}টি পয়েন্ট — বৈধ বদ্ধ বহুভুজ"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (topology.hasSelfIntersection) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (waypoints.size >= 2) {
                                val leg0 = GisCoordinateEngine.forwardBearingDeg(
                                    waypoints[0].lat, waypoints[0].lon, waypoints[1].lat, waypoints[1].lon
                                )
                                val back0 = GisCoordinateEngine.backBearingDeg(leg0)
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "প্রথম বাহুর বিয়ারিং (P1→P2): %.1f° | ব্যাক বিয়ারিং: %.1f°",
                                        leg0, back0
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TealSecondary
                                )
                            }

                            // Spatial Exports Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    enabled = waypoints.size >= 2,
                                    onClick = {
                                        repo.saveFieldTrack(
                                            FieldSurveyTrack(
                                                id = "trk_${System.currentTimeMillis()}",
                                                title = trackTitle,
                                                mode = "Field Survey",
                                                waypoints = waypoints,
                                                areaSqFt = areaSqFt,
                                                perimeterFt = perimFt
                                            )
                                        )
                                        toast = "ট্র্যাক ব্যক্তিগত ডাটাবেজে সংরক্ষিত হয়েছে!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ডাটাবেজে সেভ")
                                }
                                OutlinedButton(enabled = waypoints.size >= 2, onClick = {
                                    val txt = GisCoordinateEngine.exportGeoJson(trackTitle, waypoints)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "track_${System.currentTimeMillis()}.geojson", "application/geo+json", txt.toByteArray())
                                    toast = "GeoJSON সেভ: ${res.displayPath}"
                                }) { Text("GeoJSON") }

                                OutlinedButton(enabled = waypoints.size >= 2, onClick = {
                                    val txt = GisCoordinateEngine.exportKml(trackTitle, waypoints)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "track_${System.currentTimeMillis()}.kml", "application/vnd.google-earth.kml+xml", txt.toByteArray())
                                    toast = "KML সেভ: ${res.displayPath}"
                                }) { Text("KML") }

                                OutlinedButton(enabled = waypoints.size >= 2, onClick = {
                                    val bytes = GisCoordinateEngine.exportKmzBytes(trackTitle, waypoints)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "track_${System.currentTimeMillis()}.kmz", "application/vnd.google-earth.kmz", bytes)
                                    toast = "KMZ সেভ: ${res.displayPath}"
                                }) { Text("KMZ") }

                                OutlinedButton(enabled = waypoints.size >= 2, onClick = {
                                    val txt = GisCoordinateEngine.exportGpx(trackTitle, waypoints)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "track_${System.currentTimeMillis()}.gpx", "application/gpx+xml", txt.toByteArray())
                                    toast = "GPX সেভ: ${res.displayPath}"
                                }) { Text("GPX") }

                                OutlinedButton(enabled = waypoints.size >= 3, onClick = {
                                    val utms = waypoints.map {
                                        val u = GisCoordinateEngine.latLonToUtm(it.lat, it.lon, 46)
                                        u.easting * 3.28084 to u.northing * 3.28084
                                    }
                                    val txt = GisCoordinateEngine.exportPlanarDxf(trackTitle, utms)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "track_${System.currentTimeMillis()}.dxf", "application/dxf", txt.toByteArray())
                                    toast = "DXF (UTM Feet) সেভ: ${res.displayPath}"
                                }) { Text("DXF") }
                            }
                        }
                    }

                    // NMEA-0183 / RTK Parser Box
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("NMEA-0183 / RTK GNSS লগ ইমপোর্ট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = nmeaInput,
                                onValueChange = { nmeaInput = it },
                                label = { Text("NMEA বাক্য পেস্ট করুন (\$GPGGA / \$GNGGA / \$GPRMC)") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth().testTag("nmea_input_box")
                            )
                            Button(
                                onClick = {
                                    val parsed = GisCoordinateEngine.parseNmeaLog(nmeaInput)
                                    if (parsed.waypoints.isNotEmpty()) {
                                        waypoints = parsed.waypoints
                                        toast = "${parsed.validFixes}টি ফিক্স লোড হয়েছে (${parsed.rtkFixCount}টি RTK)"
                                    } else {
                                        toast = "কোনো বৈধ NMEA GGA/RMC বাক্য পাওয়া যায়নি।"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier.fillMaxWidth().testTag("parse_nmea_button")
                            ) {
                                Text("NMEA পার্স করে মাপে যোগ করুন")
                            }
                        }
                    }

                    itemsIndexed(waypoints, key = { idx, w -> "${w.id}_$idx" }) { idx, wp ->
                        val nextWp = if (waypoints.size >= 2) waypoints[(idx + 1) % waypoints.size] else null
                        val legBearing = nextWp?.let { GisCoordinateEngine.forwardBearingDeg(wp.lat, wp.lon, it.lat, it.lon) }
                        val legFt = nextWp?.let { GisCoordinateEngine.haversineMeters(wp.lat, wp.lon, it.lat, it.lon) * 3.28084 }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${wp.label} • ${wp.fixType}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = String.format(Locale.US, "Lat: %.6f, Lon: %.6f | ±%.1fm", wp.lat, wp.lon, wp.accuracyM),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (legFt != null && legBearing != null) {
                                    Text(
                                        text = String.format(Locale.US, "পরের কোণা: %.1f ফু @ %.1f° (Back %.1f°)", legFt, legBearing, GisCoordinateEngine.backBearingDeg(legBearing)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                            IconButton(onClick = { waypoints = waypoints.filterIndexed { i, _ -> i != idx } }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "পয়েন্ট মুছুন")
                            }
                        }
                    }
                } else {
                    // WGS84 <-> UTM Zone 45N/46N <-> DMS Converter
                    item {
                        val lat = latText.toDoubleOrNull() ?: 23.8103
                        val lon = lonText.toDoubleOrNull() ?: 90.4125
                        val utm = remember(lat, lon) { GisCoordinateEngine.latLonToUtm(lat, lon) }
                        val dmsLat = remember(lat) { GisCoordinateEngine.toDms(lat, true).format() }
                        val dmsLon = remember(lon) { GisCoordinateEngine.toDms(lon, false).format() }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("WGS84 Lat/Lon → UTM ও DMS রূপান্তর", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = latText,
                                    onValueChange = { latText = it },
                                    label = { Text("Latitude (°)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = lonText,
                                    onValueChange = { lonText = it },
                                    label = { Text("Longitude (°)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "UTM জোন: %d%c | Easting: %.2f m | Northing: %.2f m", utm.zone, utm.hemisphere, utm.easting, utm.northing),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text("DMS ফরম্যাট: $dmsLat , $dmsLon", style = MaterialTheme.typography.bodySmall)
                            Text("নোট: বাংলাদেশ সাধারণত UTM Zone 45N ও Zone 46N-এ অবস্থিত (BTM/WGS84 মানদণ্ড)।", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("বিপরীত রূপান্তর: UTM → WGS84", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = utmZoneText,
                                    onValueChange = { utmZoneText = it },
                                    label = { Text("জোন (45/46)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                                OutlinedTextField(
                                    value = utmEastingText,
                                    onValueChange = { utmEastingText = it },
                                    label = { Text("Easting (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = utmNorthingText,
                                    onValueChange = { utmNorthingText = it },
                                    label = { Text("Northing (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val inv = remember(utmZoneText, utmEastingText, utmNorthingText) {
                                val z = (utmZoneText.toIntOrNull() ?: 46).coerceIn(1, 60)
                                val e = utmEastingText.toDoubleOrNull() ?: 236500.0
                                val n = utmNorthingText.toDoubleOrNull() ?: 2635600.0
                                GisCoordinateEngine.utmToLatLon(UtmCoordinate(z, 'N', e, n))
                            }
                            Text(
                                text = String.format(Locale.US, "ফলাফল: Lat %.6f°, Lon %.6f°", inv.first, inv.second),
                                fontWeight = FontWeight.Bold,
                                color = TealSecondary
                            )
                        }
                    }
                }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
