package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterCadRepository
import com.example.data.WorkstationRepository
import com.example.engine.DocumentAndReportEngine
import com.example.engine.ExportStreamWriter
import com.example.engine.FieldIoQualityAndDeedEngine
import com.example.model.DeedDraftDocument
import com.example.model.DeedTemplateType
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.TealSecondary

private data class ParchmentArchiveItem(
    val id: String,
    val title: String,
    val badge: String,
    val serialTag: String,
    val stampLabel: String,
    val bodyText: String
)

/**
 * Document Archiver UI for the About & Resource section.
 * Displays historical Bangladeshi legal deed templates and saved records inside a scrollable,
 * aged parchment / stamp-paper interface mirroring historical deeds.
 */
@Composable
fun DocumentArchiverScreen(
    onNavigateBack: () -> Unit,
    onOpenHistoricalDeedStudio: () -> Unit = {},
    onOpenDeedStudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val masterRepo = remember(context) { MasterCadRepository.getInstance(context) }
    val workRepo = remember(context) { WorkstationRepository.getInstance(context) }
    val masterSnap by masterRepo.snapshot.collectAsStateWithLifecycle()
    val workSnap by workRepo.snapshot.collectAsStateWithLifecycle()

    var filterKind by remember { mutableStateOf("সকল") }
    var search by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    val archiveItems = remember(masterSnap.historicalDeeds, workSnap.deedDrafts, filterKind, search) {
        val list = mutableListOf<ParchmentArchiveItem>()

        // 1. Saved Historical Deeds
        masterSnap.historicalDeeds.forEach { h ->
            list.add(
                ParchmentArchiveItem(
                    id = "hist_${h.id}",
                    title = h.title,
                    badge = "পুনর্গঠিত ঐতিহাসিক দলিল",
                    serialTag = "নং- ${h.deedNumber.ifBlank { "ARCH" }}",
                    stampLabel = h.stampValueLabel,
                    bodyText = FieldIoQualityAndDeedEngine.buildFaithfulHistoricalReproduction(h)
                )
            )
        }

        // 2. Saved Custom Deed Drafts
        workSnap.deedDrafts.forEach { d ->
            list.add(
                ParchmentArchiveItem(
                    id = "draft_${d.id}",
                    title = d.title.ifBlank { d.templateType.titleBn },
                    badge = "সংরক্ষিত দলিল খসড়া",
                    serialTag = "REF-${d.id.take(6).uppercase()}",
                    stampLabel = "১০০ টাকা (খসড়া)",
                    bodyText = DocumentAndReportEngine.buildDeedText(d)
                )
            )
        }

        // 3. All 12 Historical Bangladeshi Legal Deed Templates
        DeedTemplateType.entries.forEachIndexed { idx, tpl ->
            val sampleDoc = DeedDraftDocument(
                id = "arch_tpl_${idx + 1}",
                templateType = tpl,
                title = tpl.titleBn,
                firstPartyName = "হাজী ছমির উদ্দিন (নমুনা দাতা)",
                firstPartyFather = "মৃত করিম বক্স",
                firstPartyAddress = "গ্রাম: পুবাইল, গাজীপুর",
                secondPartyName = "মোঃ রফিকুল ইসলাম (নমুনা গ্রহীতা)",
                secondPartyFather = "আবদুল খালেক",
                secondPartyAddress = "গ্রাম: পুবাইল, গাজীপুর",
                district = "গাজীপুর",
                upazila = "গাজীপুর সদর",
                mouza = "পুবাইল",
                jlNumber = "৮৫",
                khatianType = "বিএস / আরএস",
                khatianNumber = "৪১২",
                dagNumber = "১০৪২",
                areaDecimal = "24.79",
                boundaryNorth = "দাগ ১০৪১ (রহিম উদ্দিন)",
                boundarySouth = "১২ ফুট রাস্তা",
                boundaryEast = "দাগ ১০৪৩",
                boundaryWest = "পুকুর পাড়"
            )
            list.add(
                ParchmentArchiveItem(
                    id = "tpl_${tpl.name}",
                    title = "${idx + 1}. ${tpl.titleBn} (${tpl.titleEn})",
                    badge = "বাংলাদেশি ঐতিহাসিক টেমপ্লেট",
                    serialTag = "TPL-${1950 + idx * 5}",
                    stampLabel = if (idx % 2 == 0) "১০০ টাকা নন-জুডিশিয়াল স্ট্যাম্প শৈলী" else "৫০ টাকা ঐতিহাসিক রাজস্ব পাতা",
                    bodyText = DocumentAndReportEngine.buildDeedText(sampleDoc)
                )
            )
        }

        list.filter { item ->
            val catOk = when (filterKind) {
                "সংরক্ষিত" -> item.id.startsWith("hist_") || item.id.startsWith("draft_")
                "টেমপ্লেট" -> item.id.startsWith("tpl_")
                else -> true
            }
            val qOk = search.isBlank() ||
                item.title.contains(search, true) ||
                item.bodyText.contains(search, true) ||
                item.badge.contains(search, true)
            catOk && qOk
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1080.dp)
        ) {
            WorkstationTopBar(
                titleBn = "দলিল ও নথি সংরক্ষণাগার (Document Archiver)",
                subtitleEn = "Scrollable Vintage Parchment Deed Templates • Historical Bangladeshi Stamp Styling",
                helpTitle = "Document Archiver গাইড",
                helpSteps = listOf(
                    "এখানে বাংলাদেশের সাফ-কবলা, বণ্টননামা, হেবা, বায়না, হলফনামা, আমমোক্তারনামাসহ সকল ঐতিহাসিক দলিলের টেমপ্লেট ও সংরক্ষিত নথি পুরাতন স্ট্যাম্প কাগজে দেখা যাবে।",
                    "যেকোনো দলিল সরাসরি PNG, PDF অথবা DOCX ফরম্যাটে রপ্তানি করতে পারবেন অথবা স্টুডিওতে গিয়ে সম্পাদনা করতে পারবেন।"
                ),
                onBack = onNavigateBack,
                backTestTag = "doc_archiver_back_btn"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("দলিল বা নথি খুঁজুন (যেমন: সাফ-কবলা, হেবা, বণ্টননামা)...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("archiver_search_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("সকল", "টেমপ্লেট", "সংরক্ষিত").forEach { f ->
                                FilterChip(selected = filterKind == f, onClick = { filterKind = f }, label = { Text(f) })
                            }
                            OutlinedButton(onClick = onOpenHistoricalDeedStudio) {
                                Text("পুরাতন স্ক্যান ও OCR")
                            }
                            OutlinedButton(onClick = onOpenDeedStudio) {
                                Text("নতুন দলিল তৈরি")
                            }
                        }
                    }
                }

                items(archiveItems, key = { it.id }) { arch ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VintageStampPaperCard(
                            headerTitle = arch.title,
                            stampBadge = "${arch.badge} • ${arch.stampLabel}",
                            serialNumber = arch.serialTag,
                            disclaimerText = DocumentAndReportEngine.LEGAL_DRAFT_DISCLAIMER_BN
                        ) {
                            Text(
                                text = arch.bodyText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF2B1C10)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                        title = arch.title,
                                        stampValueLabel = arch.stampLabel,
                                        serialTag = arch.serialTag,
                                        bodyText = arch.bodyText
                                    )
                                    val res = ExportStreamWriter.savePdfBitmap(context, bmp, "Archiver_${arch.serialTag}")
                                    toast = "ভিন্টেজ PDF সেভ: ${res.displayPath}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) { Text("PDF সেভ") }
                            OutlinedButton(
                                onClick = {
                                    val bmp = DocumentAndReportEngine.renderVintageLegalBitmap(
                                        title = arch.title,
                                        stampValueLabel = arch.stampLabel,
                                        serialTag = arch.serialTag,
                                        bodyText = arch.bodyText
                                    )
                                    val res = ExportStreamWriter.savePngBitmap(context, bmp, "Archiver_${arch.serialTag}")
                                    toast = "ভিন্টেজ PNG সেভ: ${res.displayPath}"
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("PNG সেভ") }
                            Button(
                                onClick = {
                                    val bytes = DocumentAndReportEngine.buildDocxBytes(arch.title, arch.bodyText)
                                    val res = DocumentAndReportEngine.saveBinaryDocument(context, "Archiver_${arch.serialTag}.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes)
                                    toast = "DOCX সেভ: ${res.displayPath}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier.weight(1f)
                            ) { Text("DOCX") }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        BottomSwipeToastBanner(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
