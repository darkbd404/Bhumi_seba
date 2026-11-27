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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.FormulaExpressionEvaluator
import com.example.model.CustomFieldType
import com.example.model.CustomFormFieldDef
import com.example.model.CustomFormTemplate
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.ui.graphics.Color
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID

@Composable
fun CustomBuilderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { WorkstationRepository.getInstance(context) }
    val snap by repo.snapshot.collectAsStateWithLifecycle()

    var selectedId by remember(snap.customTemplates) {
        mutableStateOf(snap.customTemplates.firstOrNull()?.id.orEmpty())
    }
    var workingTpl by remember(selectedId, snap.customTemplates) {
        mutableStateOf(
            snap.customTemplates.find { it.id == selectedId }
                ?: snap.customTemplates.firstOrNull()
                ?: CustomFormTemplate(id = UUID.randomUUID().toString(), title = "নতুন ফর্ম")
        )
    }
    val values = remember(workingTpl.id) {
        mutableStateMapOf<String, String>().apply {
            workingTpl.fields.forEach { put(it.key, it.defaultValue) }
        }
    }

    var newFieldKey by remember { mutableStateOf("x1") }
    var newFieldLabel by remember { mutableStateOf("দৈর্ঘ্য") }
    var newFieldType by remember { mutableStateOf(CustomFieldType.NUMBER) }
    var newFieldFormula by remember { mutableStateOf("") }
    var jsonText by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    val builderJson = remember { Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true } }

    // Evaluate all numeric variables and formulas dynamically
    val numericVars = remember(workingTpl.fields, values.toMap()) {
        val m = mutableMapOf<String, Double>()
        workingTpl.fields.filter { it.type == CustomFieldType.NUMBER }.forEach { f ->
            m[f.key] = (values[f.key] ?: f.defaultValue).toDoubleOrNull() ?: 0.0
        }
        workingTpl.fields.filter { it.type == CustomFieldType.FORMULA }.forEach { f ->
            val v = FormulaExpressionEvaluator.evaluate(f.formulaExpr, m)
            if (v != null) m[f.key] = v
        }
        m.toMap()
    }

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
                titleBn = "কাস্টম ফর্ম ও ফর্মুলা বিল্ডার",
                subtitleEn = "Dynamic Calculator Templates • Formula Engine • JSON Import/Export",
                helpTitle = "কাস্টম ফর্ম বিল্ডার গাইড",
                helpSteps = listOf(
                    "সংখ্যা, টেক্সট, ড্রপডাউন, তারিখ এবং ফর্মুলা (যেমন: (n+s)/2*e) ফিল্ড যোগ করে নিজস্ব জরিপ ক্যালকুলেটর তৈরি করুন।",
                    "সংরক্ষিত টেমপ্লেট JSON আকারে এক্সপোর্ট করে অন্য ডিভাইসে ইমপোর্ট করতে পারবেন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "custom_builder_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snap.customTemplates.forEach { tpl ->
                            FilterChip(
                                selected = workingTpl.id == tpl.id,
                                onClick = { selectedId = tpl.id; workingTpl = tpl },
                                label = { Text(tpl.title, maxLines = 1) }
                            )
                        }
                    }
                }

                // Live Runner
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = workingTpl.title,
                            onValueChange = { workingTpl = workingTpl.copy(title = it) },
                            label = { Text("টেমপ্লেটের নাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        workingTpl.fields.forEach { field ->
                            when (field.type) {
                                CustomFieldType.FORMULA -> {
                                    val calcVal = FormulaExpressionEvaluator.evaluate(field.formulaExpr, numericVars)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(EmeraldPrimary.copy(alpha = 0.08f))
                                            .padding(10.dp)
                                    ) {
                                        Text("${field.label} [${field.key}]", fontWeight = FontWeight.Bold)
                                        Text("সূত্র: ${field.formulaExpr}", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = if (calcVal != null) String.format(Locale.US, "ফলাফল = %.3f", calcVal) else "সূত্র ত্রুটি",
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                CustomFieldType.CHECKBOX -> {
                                    val checked = (values[field.key] ?: field.defaultValue).equals("true", true)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(field.label)
                                        Switch(checked = checked, onCheckedChange = { values[field.key] = it.toString() })
                                    }
                                }
                                CustomFieldType.DROPDOWN -> {
                                    val opts = field.optionsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    val cur = values[field.key] ?: opts.firstOrNull().orEmpty()
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(field.label, style = MaterialTheme.typography.labelMedium)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            opts.forEach { opt ->
                                                FilterChip(selected = cur == opt, onClick = { values[field.key] = opt }, label = { Text(opt) })
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    OutlinedTextField(
                                        value = values[field.key] ?: field.defaultValue,
                                        onValueChange = { values[field.key] = it },
                                        label = { Text("${field.label} (var: ${field.key})") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = if (field.type == CustomFieldType.NUMBER) KeyboardType.Decimal else KeyboardType.Text
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                repo.saveCustomTemplate(workingTpl.copy(updatedAtMillis = System.currentTimeMillis()))
                                toast = "কাস্টম টেমপ্লেট ভল্টে সেভ হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("save_custom_template_btn")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("টেমপ্লেট সেভ করুন")
                        }
                    }
                }

                // Add New Field
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("নতুন ফিল্ড / ফর্মুলা যোগ করুন", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = newFieldKey, onValueChange = { newFieldKey = it }, label = { Text("ভেরিয়েবল Key") }, singleLine = true, modifier = Modifier.weight(0.8f))
                            OutlinedTextField(value = newFieldLabel, onValueChange = { newFieldLabel = it }, label = { Text("ফিল্ড লেবেল") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CustomFieldType.entries.forEach { ft ->
                                FilterChip(selected = newFieldType == ft, onClick = { newFieldType = ft }, label = { Text(ft.labelBn) })
                            }
                        }
                        if (newFieldType == CustomFieldType.FORMULA || newFieldType == CustomFieldType.DROPDOWN) {
                            OutlinedTextField(
                                value = newFieldFormula,
                                onValueChange = { newFieldFormula = it },
                                label = {
                                    Text(if (newFieldType == CustomFieldType.FORMULA) "ফর্মুলা (যেমন: x1 * 2)" else "কমা দিয়ে অপশন লিখুন")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = {
                                val f = CustomFormFieldDef(
                                    id = UUID.randomUUID().toString(),
                                    key = newFieldKey.trim().ifBlank { "v${workingTpl.fields.size + 1}" },
                                    label = newFieldLabel.trim().ifBlank { "ফিল্ড" },
                                    type = newFieldType,
                                    defaultValue = if (newFieldType == CustomFieldType.NUMBER) "10" else "",
                                    optionsCsv = if (newFieldType == CustomFieldType.DROPDOWN) newFieldFormula else "",
                                    formulaExpr = if (newFieldType == CustomFieldType.FORMULA) newFieldFormula else ""
                                )
                                workingTpl = workingTpl.copy(fields = workingTpl.fields + f)
                                values[f.key] = f.defaultValue
                                toast = "নতুন ফিল্ড যুক্ত হয়েছে"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ফিল্ড যোগ করুন")
                        }
                    }
                }

                // JSON Import / Export
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("JSON টেমপ্লেট আমদানি / রপ্তানি", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val encoded = builderJson.encodeToString(workingTpl)
                                    jsonText = encoded
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "template_${workingTpl.id}.json", "application/json", encoded.toByteArray())
                                    toast = "JSON সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("JSON রপ্তানি") }
                            OutlinedButton(
                                onClick = {
                                    runCatching { builderJson.decodeFromString<CustomFormTemplate>(jsonText) }
                                        .onSuccess { imp ->
                                            val fresh = imp.copy(id = UUID.randomUUID().toString())
                                            repo.saveCustomTemplate(fresh)
                                            workingTpl = fresh
                                            toast = "JSON থেকে টেমপ্লেট লোড হয়েছে!"
                                        }
                                        .onFailure { toast = "সঠিক JSON ফরম্যাট দিন" }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("JSON ইমপোর্ট") }
                        }
                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = { jsonText = it },
                            label = { Text("JSON টেক্সট") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
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

@Composable
fun LegalLibraryScreen(
    onNavigateBack: () -> Unit,
    onOpenDocumentArchiver: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val articles = remember {
        listOf(
            "১. সিএস, এসএ, আরএস, বিএস ও বিআরএস জরিপের ধারাবাহিকতা" to
                "• CS (Cadastral Survey, ১৮৮৮-১৯৪০): প্রথম বিজ্ঞানসম্মত জরিপ ও মৌজা ম্যাপ (প্রায় সব এলাকা ১৬\"=১ মাইল)।\n" +
                "• SA (State Acquisition, ১৯৫৬-১৯৬৩): জমিদারি প্রথা উচ্ছেদ পরবর্তীতে তৈরি হাতে লেখা খতিয়ান (নতুন ম্যাপ হয়নি, CS ম্যাপ অনুসৃত)।\n" +
                "• RS (Revisional Survey): সংশোধনী জরিপ — নতুন নকশা ও দাগ সৃষ্টি হয়েছে।\n" +
                "• BS / সিটি জরিপ / BRS: আধুনিক জরিপ, কম্পিউটারাইজড খতিয়ান (অনলাইনে dlrms.land.gov.bd পোর্টালে সংরক্ষিত)।",
            "২. নামজারি (Mutation) ও ভূমি উন্নয়ন কর (খাজনা)" to
                "• জমি ক্রয় বা ওয়ারিশ সূত্রে প্রাপ্তির পর এসিল্যান্ড (AC Land) অফিসে নামজারি (IX ফর্ম/ই-নামজারি) করে নতুন জোত খতিয়ান খোলা জরুরি।\n" +
                "• সরকার নির্ধারিত অনলাইন ই-নামজারি ফি (সাধারণত ১,১৭০ টাকা) চালানে/অনলাইনে পরিশোধ হয়।\n" +
                "• প্রতি বাংলা সন শেষে ইউনিয়ন ভূমি অফিস বা ldtax.gov.bd পোর্টালে দাখিলা সংগ্রহ করতে হয়।",
            "৩. সরেজমিন সীমানা ও প্যান্টোগ্রাফ মাপ নীতি" to
                "• মৌজা ম্যাপ ১৬\"=১ মাইল স্কেলে ১ ইঞ্চি = ৩৩০ ফুট বা ৫ চেইন।\n" +
                "• পুরাতন কাগজের নকশা আদ্রতা ও ভাঁজের কারণে ২%-৪% সংকুচিত হতে পারে; তাই প্যান্টোগ্রাফে স্থায়ী আইল/সড়ক/মসজিদ কন্ট্রোল পয়েন্ট ব্যবহার করে স্কেল সংশোধন করুন।\n" +
                "• আদালতের বা সরকারি ডিমার্কেশনে সার্ভেয়ার ও প্রতিবেশীর উপস্থিতিতে চূড়ান্ত সীমানা নির্ধারিত হয়।",
            "৪. সিস্টেম আর্কিটেকচার ও বাহ্যিক ইন্টিগ্রেশন পরিকল্পনা" to
                "• অফলাইন-প্রথম ইঞ্জিন: শুলেস (Shoelace), বোউডিচ ট্র্যাভার্স, WGS84↔UTM (45N/46N), সাদারল্যান্ড-হজম্যান পলিগন বিভাজন, AES-256-GCM ভল্ট, OpenXML DOCX এবং DXF/KMZ/GPX/GeoJSON সম্পূর্ণ অফলাইনে Kotlin ইঞ্জিনে কাজ করে।\n" +
                "• স্বীকৃত ওপেন ফরম্যাট ও ঐচ্ছিক অ্যাড-অন:\n" +
                "  1) Google ML Kit / Tesseract Bangla OCR (on-device image-to-text)\n" +
                "  2) বাহ্যিক Bluetooth RTK/DGPS রিসিভার (NMEA-0183 GGA/RMC সরাসরি সাপোর্টেড)\n" +
                "  3) অনুমোদিত স্যাটেলাইট টাইল ও সরকারি DLRMS পোর্টাল ইন-অ্যাপ ব্রাউজার।"
        )
    }

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
                titleBn = "আইনি ও সরকারি তথ্য লাইব্রেরি",
                subtitleEn = "Offline Bangladesh Survey Guide • Law Notes • System Architecture",
                helpTitle = "তথ্য লাইব্রেরি সম্পর্কে",
                helpSteps = listOf(
                    "এখানে বাংলাদেশের ভূমি জরিপ আইন, পর্চা যাচাই, নামজারি এবং সিস্টেমের কারিগরি আর্কিটেকচার অফলাইনে পড়তে পারবেন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "legal_library_back_button"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LegalPrecisionNoticeBanner(
                        text = "দ্রষ্টব্য: এই অ্যাপ কোনো সরকারি দপ্তর নয়। চূড়ান্ত আইনি সিদ্ধান্ত, দলিল রেজিস্ট্রি বা পর্চা যাচাইয়ের জন্য সরকারি ভূমি অফিস ও বিজ্ঞ আইনজীবীর সহায়তা নিন।"
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ঐতিহাসিক দলিল সংরক্ষণাগার (Document Archiver)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            VintageStampPaperCard(
                                headerTitle = "বাংলাদেশি দলিল টেমপ্লেট — পুরাতন কাগজ শৈলী",
                                stampBadge = "১০০ টাকা স্ট্যাম্প আর্কাইভ",
                                serialNumber = "DOC-ARCHIVE",
                                disclaimerText = DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN
                            ) {
                                Text(
                                    text = "সাফ-কবলা, হেবা-বিল-এওয়াজ, বণ্টননামা, বায়নাপত্র, হলফনামা, আমমোক্তারনামা এবং জমি জরিপের তফসিলসহ ১২+ টেমপ্লেট পুরাতন দলিলের কাগজে সংরক্ষিত।",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Serif,
                                    color = Color(0xFF2B1C10)
                                )
                            }
                        }
                        Button(
                            onClick = onOpenDocumentArchiver,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_doc_archiver_from_resources")
                        ) {
                            Icon(Icons.Rounded.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Document Archiver খুলুন (সব দলিল দেখুন)")
                        }
                    }
                }

                items(articles) { (heading, body) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        Text(body, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
