package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.engine.KmzDemarcationMetadata
import com.example.engine.SpatialParcelEngine
import com.example.model.ImportedSpatialParcel
import com.example.ui.theme.EmeraldPrimary
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MapCoordinateToolboxDialog(
    initialLat: Double,
    initialLon: Double,
    onFlyTo: (lat: Double, lon: Double) -> Unit,
    onAddPin: (lat: Double, lon: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var latText by remember(initialLat) { mutableStateOf(String.format(Locale.US, "%.6f", initialLat)) }
    var lonText by remember(initialLon) { mutableStateOf(String.format(Locale.US, "%.6f", initialLon)) }
    val lat = latText.toDoubleOrNull() ?: initialLat
    val lon = lonText.toDoubleOrNull() ?: initialLon
    val conv = remember(lat, lon) { SpatialParcelEngine.convertCoordinates(lat, lon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("স্থানাঙ্ক রূপান্তর (DD / DMS / UTM / BTM)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("অক্ষাংশ (Lat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("দ্রাঘিমা (Lon)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(shape = RoundedCornerShape(12.dp), color = EmeraldPrimary.copy(alpha = 0.1f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DMS: ${conv.dmsLat}, ${conv.dmsLon}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            String.format(Locale.US, "UTM Zone %dN: E %.2f m, N %.2f m", conv.utmZone, conv.utmEasting, conv.utmNorthing),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            String.format(Locale.US, "BTM (Bangladesh): E %.2f m, N %.2f m", conv.btmEasting, conv.btmNorthing),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onFlyTo(lat, lon); onDismiss() }) {
                    Text("যান")
                }
                Button(onClick = { onAddPin(lat, lon); onDismiss() }) {
                    Text("+ পিন বসান")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ") }
        }
    )
}

@Composable
fun KmzDemarcationExportDialog(
    hasMouzaOverlay: Boolean,
    scaleLabel: String,
    onExportKmz: (KmzDemarcationMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    var district by remember { mutableStateOf("") }
    var upazila by remember { mutableStateOf("") }
    var mouza by remember { mutableStateOf("") }
    var jlNo by remember { mutableStateOf("") }
    var dagNo by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var includeOverlay by remember { mutableStateOf(hasMouzaOverlay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("উন্নত KMZ ডিমার্কেশন রপ্তানি", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Google Earth KMZ-এ পলিগন, মৌজা ওভারলে (GroundOverlay) ও বাংলাদেশের দাগ-খতিয়ান তথ্য যুক্ত হবে:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("উপজেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = jlNo, onValueChange = { jlNo = it }, label = { Text("জেএল নং") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dagNo, onValueChange = { dagNo = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("মালিক/জরিপকার") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                if (hasMouzaOverlay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeOverlay, onCheckedChange = { includeOverlay = it })
                        Text("মৌজা নকশা GroundOverlay সংযুক্ত করুন", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExportKmz(
                        KmzDemarcationMetadata(
                            district = district.trim(),
                            upazila = upazila.trim(),
                            mouza = mouza.trim(),
                            jlNumber = jlNo.trim(),
                            dagNumber = dagNo.trim(),
                            ownerName = owner.trim(),
                            scaleLabel = scaleLabel,
                            includeOverlayImage = includeOverlay && hasMouzaOverlay
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_kmz_export_btn")
            ) {
                Icon(Icons.Rounded.ShareLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("KMZ সেভ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
fun GpsAveragingDialog(
    maxAccuracyM: Float,
    onMaxAccuracyChange: (Float) -> Unit,
    onUseAveragedPoint: (lat: Double, lon: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasPerm = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    var running by remember { mutableStateOf(hasPerm) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    val samples = remember { mutableStateListOf<Triple<Double, Double, Float>>() }
    var rejectedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        elapsedSec = 0
        while (elapsedSec < 10 && running) {
            delay(1000L)
            elapsedSec++
        }
        running = false
    }

    DisposableEffect(running, maxAccuracyM) {
        if (!running || !hasPerm) return@DisposableEffect onDispose {}
        val client = LocationServices.getFusedLocationProviderClient(context)
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 900L).build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                val acc = if (loc.hasAccuracy()) loc.accuracy else 35f
                if (acc <= maxAccuracyM) {
                    samples.add(Triple(loc.latitude, loc.longitude, acc))
                } else {
                    rejectedCount++
                }
            }
        }
        client.requestLocationUpdates(req, cb, Looper.getMainLooper())
        onDispose { client.removeLocationUpdates(cb) }
    }

    val avgLat = if (samples.isNotEmpty()) samples.map { it.first }.average() else null
    val avgLon = if (samples.isNotEmpty()) samples.map { it.second }.average() else null
    val avgAcc = if (samples.isNotEmpty()) samples.map { it.third.toDouble() }.average() else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("১০-সেকেন্ড GPS গড় ও নির্ভুলতা থ্রেশহোল্ড", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasPerm) {
                    Text(
                        "GPS ব্যবহার করতে ম্যাপের 'GPS' বাটনে ট্যাপ করে লোকেশন পারমিশন দিন।",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("গ্রহণযোগ্য সর্বোচ্চ ত্রুটি সীমা: ±${maxAccuracyM.toInt()} মিটার", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = maxAccuracyM,
                        onValueChange = onMaxAccuracyChange,
                        valueRange = 5f..50f
                    )
                    LinearProgressIndicator(
                        progress = { (elapsedSec / 10f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                    Text(
                        text = if (running) "স্যাম্পল সংগ্রহ চলছে... ($elapsedSec/10 সে.) • গৃহীত: ${samples.size} • বাতিল (দুর্বল সিগন্যাল): $rejectedCount"
                        else "সংগ্রহ সম্পন্ন • গৃহীত: ${samples.size} • বাতিল: $rejectedCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (avgLat != null && avgLon != null && avgAcc != null) {
                        Surface(shape = RoundedCornerShape(10.dp), color = EmeraldPrimary.copy(alpha = 0.12f)) {
                            Text(
                                text = String.format(Locale.US, "গড় অবস্থান:\nLat: %.7f\nLon: %.7f\nগড় নির্ভুলতা: ±%.1f মি.", avgLat, avgLon, avgAcc),
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasPerm && !running) {
                    OutlinedButton(onClick = { samples.clear(); rejectedCount = 0; running = true }) {
                        Text("আবার গড় করুন")
                    }
                }
                Button(
                    onClick = {
                        if (avgLat != null && avgLon != null) {
                            onUseAveragedPoint(avgLat, avgLon)
                            onDismiss()
                        }
                    },
                    enabled = avgLat != null && avgLon != null
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("পিন যোগ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportedParcelsSheet(
    parcels: List<ImportedSpatialParcel>,
    onSelectParcel: (ImportedSpatialParcel) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ইমপোর্টকৃত পার্সেল (${parcels.size}টি)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (parcels.isNotEmpty()) {
                    TextButton(onClick = { onClearAll(); onDismiss() }) {
                        Text("সব সরান", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                text = "স্যাটেলাইট ছবিতে নিজস্ব ভেক্টর সীমানা না থাকায়, আপনার KML/KMZ/GeoJSON/DXF ফাইলের পার্সেলে ট্যাপ করে স্ন্যাপ করুন:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(parcels, key = { it.id }) { parcel ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldPrimary.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectParcel(parcel)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(parcel.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${parcel.sourceFormat} • দাগ: ${parcel.dagNumber.ifBlank { "-" }} • ${parcel.pointsLatLon.size} বিন্দু",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.2f শতক", parcel.decimal),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
