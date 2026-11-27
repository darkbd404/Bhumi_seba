package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppPreferences
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.model.UnifiedSurveyProject
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun LandDatabaseScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    val appPrefs = remember(context) { AppPreferences(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var unlocked by remember(snap.vaultPinHash) { mutableStateOf(snap.vaultPinHash.isBlank()) }
    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var activeFolder by remember { mutableIntStateOf(0) } // 0=Projects, 1=Deeds, 2=GPS/Div, 3=OCR/Reports, 4=Backup/PIN
    var search by remember { mutableStateOf("") }

    // New Project Form
    var projName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var mouza by remember { mutableStateOf("") }
    var khatian by remember { mutableStateOf("") }
    var dag by remember { mutableStateOf("") }
    var areaDec by remember { mutableStateOf("") }

    // Backup & Restore State
    var encPassphrase by remember { mutableStateOf("bhumi2026") }
    var backupText by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

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
                titleBn = "ব্যক্তিগত ভূমি ডাটাবেজ ও ভল্ট",
                subtitleEn = "Projects • Owner/Mouza Folders • AES-256 Encrypted Backup & PIN",
                helpTitle = "ডাটাবেজ ও সুরক্ষা ভল্ট গাইড",
                helpSteps = listOf(
                    "মালিক, মৌজা, খতিয়ান ও দাগ অনুযায়ী আপনার সব জরিপ প্রকল্প, দলিল, OCR নথি ও GPS ট্র্যাক অফলাইনে সংরক্ষিত থাকে।",
                    "নিরাপত্তার জন্য ভল্ট PIN সেট করতে পারবেন এবং AES-256 এনক্রিপ্টেড ব্যাকআপ তৈরি করে যেকোনো ডিভাইসে রিস্টোর করতে পারবেন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "vault_back_button"
            )

            if (!unlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .neuSurface(cornerRadius = 24.dp, elevation = 6.dp)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
                    Text("ভূমি ভল্ট লক করা আছে", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("ভল্ট PIN লিখুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("vault_unlock_pin_input")
                    )
                    Button(
                        onClick = {
                            if (repo.verifyVaultPin(pinInput)) {
                                unlocked = true
                                pinInput = ""
                            } else {
                                toast = "ভুল PIN! আবার চেষ্টা করুন।"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("vault_unlock_button")
                    ) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("আনলক করুন")
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "প্রকল্প (${snap.unifiedProjects.size})",
                        "দলিল (${snap.deedDrafts.size})",
                        "GPS ও বিভাজন (${snap.fieldTracks.size + snap.divisionRecords.size})",
                        "OCR ও রিপোর্ট (${snap.ocrDocuments.size + snap.reportConfigs.size})",
                        "ব্যাকআপ ও PIN"
                    )
                    tabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = activeFolder == idx,
                            onClick = { activeFolder = idx },
                            label = { Text(label) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeFolder != 4) {
                        item {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                placeholder = { Text("মালিক, মৌজা, খতিয়ান বা দাগ লিখে খুঁজুন...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    when (activeFolder) {
                        0 -> {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("নতুন সমন্বিত জরিপ প্রকল্প যুক্ত করুন", fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = projName, onValueChange = { projName = it }, label = { Text("প্রকল্পের নাম") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("মালিক / ক্লায়েন্ট") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = khatian, onValueChange = { khatian = it }, label = { Text("খতিয়ান") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = dag, onValueChange = { dag = it }, label = { Text("দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = areaDec, onValueChange = { areaDec = it }, label = { Text("শতাংশ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.8f))
                                    }
                                    Button(
                                        onClick = {
                                            if (projName.isNotBlank() || mouza.isNotBlank()) {
                                                repo.saveUnifiedProject(
                                                    UnifiedSurveyProject(
                                                        id = UUID.randomUUID().toString(),
                                                        projectName = projName.ifBlank { "$mouza — দাগ $dag" },
                                                        ownerName = ownerName,
                                                        mouza = mouza,
                                                        khatianNumbers = khatian,
                                                        dagNumbers = dag,
                                                        totalDecimalArea = areaDec.toDoubleOrNull() ?: 0.0
                                                    )
                                                )
                                                projName = ""; ownerName = ""; mouza = ""; khatian = ""; dag = ""; areaDec = ""
                                                toast = "প্রকল্প ভল্টে যুক্ত হয়েছে!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.fillMaxWidth().testTag("vault_add_project_btn")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("প্রকল্প সংরক্ষণ")
                                    }
                                }
                            }

                            val itemsFiltered = snap.unifiedProjects.filter {
                                search.isBlank() ||
                                    it.projectName.contains(search, true) ||
                                    it.ownerName.contains(search, true) ||
                                    it.mouza.contains(search, true) ||
                                    it.dagNumbers.contains(search, true)
                            }
                            items(itemsFiltered, key = { it.id }) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.projectName, fontWeight = FontWeight.Bold)
                                        Text("মালিক: ${p.ownerName.ifBlank { "-" }} • মৌজা: ${p.mouza.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                                        Text("খতিয়ান: ${p.khatianNumbers.ifBlank { "-" }} • দাগ: ${p.dagNumbers.ifBlank { "-" }} (${String.format(Locale.US, "%.2f", p.totalDecimalArea)} শতক)", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                    }
                                    IconButton(onClick = { repo.deleteUnifiedProject(p.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "প্রকল্প মুছুন")
                                    }
                                }
                            }
                        }

                        1 -> {
                            items(snap.deedDrafts.filter { search.isBlank() || it.title.contains(search, true) || it.mouza.contains(search, true) }, key = { it.id }) { d ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(d.title.ifBlank { d.templateType.titleBn }, fontWeight = FontWeight.Bold)
                                        Text("${d.firstPartyName} → ${d.secondPartyName} • মৌজা: ${d.mouza} • ${d.areaDecimal} শতক", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { repo.deleteDeedDraft(d.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "মুছুন")
                                    }
                                }
                            }
                        }

                        2 -> {
                            items(snap.fieldTracks, key = { it.id }) { trk ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                        .padding(12.dp)
                                ) {
                                    Text("GPS: ${trk.title}", fontWeight = FontWeight.Bold)
                                    Text("${trk.waypoints.size} পয়েন্ট • ${String.format(Locale.US, "%.2f ব.ফু", trk.areaSqFt)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            items(snap.divisionRecords, key = { it.id }) { div ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                        .padding(12.dp)
                                ) {
                                    Text("বিভাজন: ${div.title} (${div.parcels.size} প্লট)", fontWeight = FontWeight.Bold)
                                    Text("ব্যবহার্য: ${String.format(Locale.US, "%.1f ব.ফু", div.usableAreaSqFt)} • সংরক্ষণ ত্রুটি: ${String.format(Locale.US, "%.3f ব.ফু", div.conservationErrorSqFt)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        3 -> {
                            items(snap.ocrDocuments, key = { it.id }) { o ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                        .padding(12.dp)
                                ) {
                                    Text("OCR: ${o.title} ${if (o.verifiedByUser) "✓ যাচাইকৃত" else "(অযাচাইকৃত)"}", fontWeight = FontWeight.Bold)
                                    Text(o.extractedFields.joinToString(" | ") { "${it.labelBn}: ${it.value}" }, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            items(snap.reportConfigs, key = { it.id }) { r ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 16.dp, elevation = 3.dp)
                                        .padding(12.dp)
                                ) {
                                    Text("রিপোর্ট: ${r.title}", fontWeight = FontWeight.Bold)
                                    Text("${r.pageSize} ${r.orientation} • মৌজা: ${r.mouza} • ${r.areaSummaryText}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        else -> {
                            // Backup, Restore & PIN Security
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldPrimary)
                                        Text("AES-256-GCM এনক্রিপ্টেড ব্যাকআপ ও রিস্টোর", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedTextField(
                                        value = encPassphrase,
                                        onValueChange = { encPassphrase = it },
                                        label = { Text("এনক্রিপশন পাসফ্রেজ / কী") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val raw = repo.exportBackupJson(appPrefs)
                                                    val enc = DocumentAndReportEngine.encryptVaultJson(raw, encPassphrase.ifBlank { "bhumi2026" })
                                                    backupText = enc
                                                    val saved = DocumentAndReportEngine.saveBinaryDocument(
                                                        context,
                                                        "Bhumi_Vault_Backup_${System.currentTimeMillis()}.bhumienc",
                                                        "text/plain",
                                                        enc.toByteArray(Charsets.UTF_8)
                                                    )
                                                    toast = "এনক্রিপ্টেড ব্যাকআপ সেভ: ${saved.displayPath}"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier.weight(1f).testTag("export_encrypted_backup")
                                        ) {
                                            Text("এনক্রিপ্টেড ব্যাকআপ")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val payload = if (backupText.trim().startsWith("BHUMI_VAULT_V1|")) {
                                                        DocumentAndReportEngine.decryptVaultJson(backupText, encPassphrase)
                                                    } else {
                                                        backupText
                                                    }
                                                    if (payload == null) {
                                                        toast = "পাসফ্রেজ ভুল অথবা এনক্রিপ্টেড ডাটা নষ্ট।"
                                                    } else {
                                                        val (_, msg) = repo.restoreFromBackupJson(payload, appPrefs)
                                                        toast = msg
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("restore_backup_btn")
                                        ) {
                                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("রিস্টোর করুন")
                                        }
                                    }

                                    OutlinedTextField(
                                        value = backupText,
                                        onValueChange = { backupText = it },
                                        label = { Text("ব্যাকআপ JSON / BHUMI_VAULT_V1 ডাটা") },
                                        minLines = 3,
                                        maxLines = 6,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("ভল্ট গোপনীয়তা PIN লক", fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = newPinInput,
                                        onValueChange = { newPinInput = it },
                                        label = { Text("নতুন PIN (ফাঁকা রেখে সেভ করলে PIN বন্ধ হবে)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            repo.setVaultPin(newPinInput.trim())
                                            toast = if (newPinInput.isBlank()) "ভল্ট PIN লক বন্ধ করা হয়েছে" else "নতুন ভল্ট PIN সক্রিয় হয়েছে!"
                                            newPinInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (snap.vaultPinHash.isBlank()) "PIN লক চালু করুন" else "PIN আপডেট / বন্ধ করুন")
                                    }
                                }
                            }
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
