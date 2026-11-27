package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ChainOfTitleEntry
import com.example.model.KhatianCustomField
import com.example.model.KhatianDagEntry
import com.example.model.KhatianOneClickAnalysis
import com.example.model.KhatianOwnerEntry
import com.example.ui.components.VintageStampPaperCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuSurface
import com.example.viewmodel.CustomKhatianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val KHATIAN_TYPES = listOf(
    "আরএস (RS) খতিয়ান",
    "বিএস (BS) খতিয়ান",
    "বিআরএস (BRS) খতিয়ান",
    "সিএস (CS) খতিয়ান",
    "এসএ (SA) খতিয়ান",
    "নামজারি খতিয়ান",
    "কাস্টম পর্চা"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomKhatianScreen(
    savedKhatianId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CustomKhatianViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val doc = state.document
    val neu = LocalNeuPalette.current

    var showRefExpanded by remember { mutableStateOf(true) }
    var refLargeView by remember { mutableStateOf(false) }
    var showSavedSheet by remember { mutableStateOf(false) }
    var editingOwner by remember { mutableStateOf<KhatianOwnerEntry?>(null) }
    var showOwnerDialog by remember { mutableStateOf(false) }
    var editingDag by remember { mutableStateOf<KhatianDagEntry?>(null) }
    var showDagDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showCustomFieldDialog by remember { mutableStateOf(false) }
    var showChainDialog by remember { mutableStateOf(false) }

    val refDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openReferenceDocumentUri(uri)
            showRefExpanded = true
        }
    }

    LaunchedEffect(savedKhatianId) {
        if (!savedKhatianId.isNullOrBlank()) {
            viewModel.loadCustomKhatianById(savedKhatianId)
        }
    }

    BackHandler(enabled = showSavedSheet || showOwnerDialog || showDagDialog || showOcrDialog || showCustomFieldDialog || showChainDialog) {
        when {
            showOwnerDialog -> showOwnerDialog = false
            showDagDialog -> showDagDialog = false
            showSavedSheet -> showSavedSheet = false
            showOcrDialog -> showOcrDialog = false
            showCustomFieldDialog -> showCustomFieldDialog = false
            showChainDialog -> showChainDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(neu.canvas)
    ) {
        // 1. Top Header Bar
        Surface(
            color = neu.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("khatian_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "কাস্টম খতিয়ান তৈরি",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (doc.khatianNumber.isNotBlank()) {
                            "${doc.khatianType} • নং ${doc.khatianNumber}"
                        } else {
                            "অরিজিনাল দেখে হাতে খতিয়ান লিখুন ও সেভ করুন"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.createNewBlankKhatian() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("khatian_new_button")
                ) {
                    Icon(Icons.Rounded.NoteAdd, contentDescription = "নতুন খতিয়ান")
                }

                IconButton(
                    onClick = { showSavedSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("khatian_saved_list_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (state.savedKhatians.isNotEmpty()) {
                                Badge { Text("${state.savedKhatians.size}") }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = "সংরক্ষিত খতিয়ান")
                    }
                }

                IconButton(
                    onClick = { refDocPicker.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("khatian_upload_ref_button")
                ) {
                    Icon(
                        Icons.Rounded.UploadFile,
                        contentDescription = "অরিজিনাল খতিয়ান আপলোড",
                        tint = EmeraldPrimary
                    )
                }

                IconButton(
                    onClick = { showOcrDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("khatian_ocr_autofill_button")
                ) {
                    Icon(
                        Icons.Rounded.CenterFocusStrong,
                        contentDescription = "OCR টেক্সট অটো-ফিল",
                        tint = TealSecondary
                    )
                }
            }
        }

        if (state.isLoadingReference || state.isExporting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Status Feedback Pill
        AnimatedVisibility(visible = !state.statusMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearStatusMessage() }) {
                        Text("ঠিক আছে")
                    }
                }
            }
        }

        // 2. Original Khatian Reference Viewer (Collapsible & Pinch-Zoomable)
        if (state.referenceBitmap != null) {
            OriginalReferenceViewerCard(
                bitmap = state.referenceBitmap!!,
                docName = doc.referenceDocName.ifBlank { "অরিজিনাল খতিয়ান" },
                pageCount = state.referencePageCount,
                currentPage = state.referenceCurrentPage,
                expanded = showRefExpanded,
                largeView = refLargeView,
                onToggleExpanded = { showRefExpanded = !showRefExpanded },
                onToggleLarge = { refLargeView = !refLargeView },
                onPrevPage = { viewModel.selectReferencePage(state.referenceCurrentPage - 1) },
                onNextPage = { viewModel.selectReferencePage(state.referenceCurrentPage + 1) },
                onChangeFile = { refDocPicker.launch(arrayOf("application/pdf", "image/*")) }
            )
        } else {
            Surface(
                onClick = { refDocPicker.launch(arrayOf("application/pdf", "image/*")) },
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = TealSecondary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অরিজিনাল খতিয়ান ছবি / PDF ওপেন করুন",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "উপরে অরিজিনাল দেখে নিচে কাস্টম খতিয়ান সাজিয়ে লিখতে পারবেন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Scrollable Form Body
        val totalDec = remember(doc.dags) { doc.dags.sumOf { it.thisKhatianDecimal } }
        val totalAcre = totalDec / 100.0
        val totalKatha = totalDec * 0.605
        val totalSqFt = totalDec * 435.6

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 900.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section A: Mouza & Header Fields
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "১. মৌজা ও খতিয়ানের পরিচিতি",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(KHATIAN_TYPES) { typeName ->
                            FilterChip(
                                selected = doc.khatianType == typeName,
                                onClick = { viewModel.updateHeaderFields(khatianType = typeName) },
                                label = { Text(typeName) }
                            )
                        }
                    }

                    if (doc.khatianType !in KHATIAN_TYPES.take(6)) {
                        OutlinedTextField(
                            value = doc.khatianType,
                            onValueChange = { viewModel.updateHeaderFields(khatianType = it) },
                            label = { Text("কাস্টম রেকর্ড / পর্চার ধরণ নাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doc.khatianNumber,
                            onValueChange = { viewModel.updateHeaderFields(khatianNumber = it) },
                            label = { Text("খতিয়ান নং *") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("khatian_no_input")
                        )
                        OutlinedTextField(
                            value = doc.jlNumber,
                            onValueChange = { viewModel.updateHeaderFields(jlNumber = it) },
                            label = { Text("জে.এল. নং") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doc.unionName,
                            onValueChange = { viewModel.updateHeaderFields(unionName = it) },
                            label = { Text("ইউনিয়ন / পৌরসভা") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = doc.mouzaName,
                            onValueChange = { viewModel.updateHeaderFields(mouzaName = it) },
                            label = { Text("মৌজার নাম") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("khatian_mouza_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doc.upazila,
                            onValueChange = { viewModel.updateHeaderFields(upazila = it) },
                            label = { Text("উপজেলা / থানা") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = doc.district,
                            onValueChange = { viewModel.updateHeaderFields(district = it) },
                            label = { Text("জেলা") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doc.division,
                            onValueChange = { viewModel.updateHeaderFields(division = it) },
                            label = { Text("বিভাগ") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doc.resaNumber,
                            onValueChange = { viewModel.updateHeaderFields(resaNumber = it) },
                            label = { Text("রেঃ সাঃ নং") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = doc.tauziNumber,
                            onValueChange = { viewModel.updateHeaderFields(tauziNumber = it) },
                            label = { Text("তৌজি নং") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section B: Owners
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "২. মালিক / প্রজা ও অংশ (${doc.owners.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        FilledTonalButton(
                            onClick = {
                                editingOwner = null
                                showOwnerDialog = true
                            },
                            modifier = Modifier.testTag("khatian_add_owner_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("মালিক যোগ")
                        }
                    }

                    if (doc.owners.isEmpty()) {
                        Text(
                            text = "এখনো কোনো মালিক যোগ করা হয়নি — '+ মালিক যোগ' বাটনে চাপুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        doc.owners.forEachIndexed { idx, owner ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${idx + 1}. ${owner.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val details = listOfNotNull(
                                            owner.fatherOrHusband.takeIf { it.isNotBlank() }?.let { "পিতা/স্বামী: $it" },
                                            owner.address.takeIf { it.isNotBlank() }?.let { "সাং: $it" }
                                        ).joinToString(" • ")
                                        if (details.isNotBlank()) {
                                            Text(
                                                text = details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val shareDecimal = totalDec * owner.shareRatio
                                        Text(
                                            text = "অংশ: ${owner.shareText} (${String.format(Locale.US, "%.1f", owner.shareRatio * 100)}%) • প্রাপ্য: ${String.format(Locale.US, "%.2f", shareDecimal)} শতক",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TealSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingOwner = owner
                                            showOwnerDialog = true
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Owner", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeOwner(owner.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Owner",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val analysis = remember(doc.owners, doc.dags) { viewModel.computeOneClickAnalysis() }
                    Surface(
                        color = if (analysis.isSixteenAnaExact) EmeraldPrimary.copy(alpha = 0.08f) else Color(0xFFD97706).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (analysis.isSixteenAnaExact) "✓ এক ক্লিকে ১৬-আনা হিস্যা বিশ্লেষণ: নির্ভুল" else "⚠ হিস্যা যাচাই: অসমতা / ঘাটতি",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analysis.isSixteenAnaExact) EmeraldPrimary else Color(0xFFB45309)
                                )
                                Text(
                                    text = "মোট: ${String.format(Locale.US, "%.4f", analysis.totalOwnerRatio)} / ১.০০০",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = analysis.discrepancyNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section C: Dags & Land Area
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "৩. দাগ ও জমির পরিমাণ (${doc.dags.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        FilledTonalButton(
                            onClick = {
                                editingDag = null
                                showDagDialog = true
                            },
                            modifier = Modifier.testTag("khatian_add_dag_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("দাগ যোগ")
                        }
                    }

                    if (doc.dags.isEmpty()) {
                        Text(
                            text = "দাগ নম্বর, জমির শ্রেণী ও শতাংশ যুক্ত করতে '+ দাগ যোগ' বাটনে চাপুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        doc.dags.forEach { dag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "দাগ নং: ${dag.dagNumber} • শ্রেণী: ${dag.landClass}${if (dag.revenue.isNotBlank()) " • রাজস্ব: ${dag.revenue}" else ""}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "দাগে মোট: ${String.format(Locale.US, "%.4f", dag.totalDecimal / 100.0)} একর (${String.format(Locale.US, "%.2f", dag.totalDecimal)} শতক) • অংশ: ${String.format(Locale.US, "%.3f", dag.thisKhatianShare)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "অত্র খতিয়ানে: ${String.format(Locale.US, "%.4f", dag.thisKhatianDecimal / 100.0)} একর (${String.format(Locale.US, "%.2f", dag.thisKhatianDecimal)} শতক • ${String.format(Locale.US, "%.2f", (dag.thisKhatianDecimal * 435.6) / 720.0)} কাঠা)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingDag = dag
                                            showDagDialog = true
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Dag", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeDag(dag.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Dag",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section D: Khajna, Notes & Summary
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "৪. খাজনা, মন্তব্য ও সারসংক্ষেপ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    OutlinedTextField(
                        value = doc.totalKhajna,
                        onValueChange = { viewModel.updateHeaderFields(totalKhajna = it) },
                        label = { Text("খাজনা / ভূমি উন্নয়ন কর (টাকা)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = doc.notes,
                        onValueChange = { viewModel.updateHeaderFields(notes = it) },
                        label = { Text("মন্তব্য / পূর্ব খতিয়ান ও বিশেষ নোট") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = doc.possessionNote,
                        onValueChange = { viewModel.updateHeaderFields(possessionNote = it) },
                        label = { Text("দখলদারিত্ব ও সীমানা নোট (Possession Note)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "অতিরিক্ত কাস্টম ফিল্ড (${doc.customFields.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showCustomFieldDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ ফিল্ড যোগ")
                        }
                    }
                    doc.customFields.forEach { cf ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cf.label}: ${cf.value}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removeCustomField(cf.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মালিকানার ধারাবাহিকতা / বায়া রেকর্ড (${doc.chainOfTitle.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showChainDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ বায়া যোগ")
                        }
                    }
                    doc.chainOfTitle.forEach { cot ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${cot.stage}: খতিয়ান নং ${cot.khatianNo.ifBlank { "-" }} • দাগ নং ${cot.dagNo.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val meta = listOfNotNull(
                                        cot.deedOrCaseNo.takeIf { it.isNotBlank() }?.let { "দলিল/কেস: $it" },
                                        cot.yearStr.takeIf { it.isNotBlank() }?.let { "সাল: $it" },
                                        cot.partiesSummary.takeIf { it.isNotBlank() }?.let { "পক্ষ: $it" },
                                        if (cot.areaDecimal > 0.0) "${cot.areaDecimal} শতক" else null
                                    ).joinToString(" | ")
                                    if (meta.isNotBlank()) {
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { viewModel.removeChainOfTitleEntry(cot.id) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "মোট জমি: ${String.format(Locale.US, "%.2f", totalDec)} শতাংশ (${String.format(Locale.US, "%.4f", totalAcre)} একর)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "সমান: ${String.format(Locale.US, "%.2f", totalKatha)} কাঠা • ${String.format(Locale.US, "%.0f", totalSqFt)} বর্গফুট",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PNG বা PDF সেভ করলে প্রাচীন খতিয়ান কাগজে আপনার নাম, ছবি, মোবাইল ও Gmail স্বয়ংক্রিয়ভাবে যুক্ত হবে।",
                                style = MaterialTheme.typography.labelSmall,
                                color = SurveyGold
                            )
                        }
                    }
                }
            }

            // Section E: Live Vintage Parchment Khatian Preview
            item {
                VintageStampPaperCard(
                    headerTitle = "${doc.khatianType.ifBlank { "আরএস (RS) খতিয়ান" }} — খতিয়ান নং: ${doc.khatianNumber.ifBlank { "-" }}",
                    stampBadge = "প্রাচীন খতিয়ান পর্চা শৈলী (খসড়া)",
                    serialNumber = "মৌজা: ${doc.mouzaName.ifBlank { "-" }} (JL ${doc.jlNumber.ifBlank { "-" }})",
                    disclaimerText = "অস্বীকৃতি: এটি হাতে প্রস্তুতকৃত ডিজিটাল কাস্টম খতিয়ান (অনুকরণ); সরকারি সার্টিফায়েড পর্চা নয়।"
                ) {
                    Text(
                        text = "জেলা: ${doc.district.ifBlank { "-" }} | উপজেলা: ${doc.upazila.ifBlank { "-" }} | তৌজি: ${doc.tauziNumber.ifBlank { "-" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2B1C10)
                    )
                    Text(
                        text = "মালিক (${doc.owners.size}): " + if (doc.owners.isEmpty()) "যোগ করা হয়নি" else doc.owners.joinToString("; ") { "${it.name} (${it.shareText})" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2B1C10)
                    )
                    Text(
                        text = "দাগসমূহ (${doc.dags.size}): " + if (doc.dags.isEmpty()) "যোগ করা হয়নি" else doc.dags.joinToString("; ") { "দাগ ${it.dagNumber} (${it.landClass}: ${String.format(Locale.US, "%.2f", it.thisKhatianDecimal)} শতক)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2B1C10)
                    )
                    Text(
                        text = "মোট জমি: ${String.format(Locale.US, "%.2f", totalDec)} শতাংশ (${String.format(Locale.US, "%.4f", totalAcre)} একর) • খাজনা: ${doc.totalKhajna.ifBlank { "-" }}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF174E3D)
                    )
                }
            }
        }

        // 4. Docked Bottom Action Bar
        Surface(
            color = neu.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.saveCustomKhatian() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("khatian_save_data_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ডাটা সেভ", maxLines = 1)
                }

                Button(
                    onClick = { viewModel.exportKhatian(asPdf = false) },
                    enabled = !state.isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("khatian_export_png_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PNG সেভ", maxLines = 1)
                }

                Button(
                    onClick = { viewModel.exportKhatian(asPdf = true) },
                    enabled = !state.isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("khatian_export_pdf_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("PDF সেভ", maxLines = 1)
                }
            }
        }
    }

    if (showOwnerDialog) {
        OwnerEntryDialog(
            initial = editingOwner,
            onDismiss = { showOwnerDialog = false },
            onSave = { name, father, addr, shareText, shareRatio ->
                viewModel.addOrUpdateOwner(
                    editId = editingOwner?.id,
                    name = name,
                    fatherOrHusband = father,
                    address = addr,
                    shareText = shareText,
                    shareRatio = shareRatio
                )
                showOwnerDialog = false
            }
        )
    }

    if (showDagDialog) {
        DagEntryDialog(
            initial = editingDag,
            onDismiss = { showDagDialog = false },
            onSave = { dagNo, landClass, totalDec, share, explicitDec, remarks, revenue ->
                viewModel.addOrUpdateDag(
                    editId = editingDag?.id,
                    dagNumber = dagNo,
                    landClass = landClass,
                    totalDecimal = totalDec,
                    thisKhatianShare = share,
                    explicitThisKhatianDecimal = explicitDec,
                    remarks = remarks,
                    revenue = revenue
                )
                showDagDialog = false
            }
        )
    }

    if (showSavedSheet) {
        ModalBottomSheet(onDismissRequest = { showSavedSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "সংরক্ষিত কাস্টম খতিয়ান (${state.savedKhatians.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.savedKhatians.isEmpty()) {
                    Text(
                        text = "এখনো কোনো কাস্টম খতিয়ান সেভ করা হয়নি।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.savedKhatians, key = { it.id }) { item ->
                            val dateStr = remember(item.updatedAtMillis) {
                                SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(item.updatedAtMillis))
                            }
                            Surface(
                                onClick = {
                                    viewModel.loadCustomKhatian(item)
                                    showSavedSheet = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, tint = EmeraldPrimary)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "খতিয়ান নং: ${item.khatianNumber.ifBlank { "-" }} (${item.khatianType})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "মৌজা: ${item.mouzaName.ifBlank { "-" }} • মালিক ${item.owners.size} • দাগ ${item.dags.size} • $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSavedKhatian(item.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOcrDialog) {
        OcrAutoFillDialog(
            onDismiss = { showOcrDialog = false },
            onApply = { rawText ->
                viewModel.applyOcrTextToHeaderAndDags(rawText)
                showOcrDialog = false
            }
        )
    }

    if (showCustomFieldDialog) {
        AddCustomFieldDialog(
            onDismiss = { showCustomFieldDialog = false },
            onAdd = { label, value ->
                viewModel.addCustomField(label, value)
                showCustomFieldDialog = false
            }
        )
    }

    if (showChainDialog) {
        AddChainOfTitleDialog(
            onDismiss = { showChainDialog = false },
            onAdd = { stage, khatianNo, dagNo, deedNo, year, parties, dec, note ->
                viewModel.addChainOfTitleEntry(stage, khatianNo, dagNo, deedNo, year, parties, dec, note)
                showChainDialog = false
            }
        )
    }
}

@Composable
private fun OriginalReferenceViewerCard(
    bitmap: android.graphics.Bitmap,
    docName: String,
    pageCount: Int,
    currentPage: Int,
    expanded: Boolean,
    largeView: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleLarge: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onChangeFile: () -> Unit
) {
    var zoomScale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(bitmap) { mutableFloatStateOf(0f) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = docName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (pageCount > 1) {
                    IconButton(
                        onClick = onPrevPage,
                        enabled = currentPage > 0,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous page")
                    }
                    Text("${currentPage + 1}/$pageCount", style = MaterialTheme.typography.labelSmall)
                    IconButton(
                        onClick = onNextPage,
                        enabled = currentPage < pageCount - 1,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next page")
                    }
                }
                if (expanded) {
                    IconButton(
                        onClick = {
                            zoomScale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Rounded.CenterFocusStrong, contentDescription = "Reset zoom", modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = onToggleLarge) {
                        Text(if (largeView) "ছোট" else "বড়")
                    }
                }
                TextButton(onClick = onChangeFile) {
                    Text("বদলান")
                }
                IconButton(onClick = onToggleExpanded, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Toggle Reference Viewer"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (largeView) 300.dp else 190.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFECE9DF))
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextZoom = (zoomScale * zoom).coerceIn(1f, 6f)
                                zoomScale = nextZoom
                                if (nextZoom > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Original Khatian Reference",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerEntryDialog(
    initial: KhatianOwnerEntry?,
    onDismiss: () -> Unit,
    onSave: (name: String, father: String, addr: String, shareText: String, shareRatio: Double) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var father by remember { mutableStateOf(initial?.fatherOrHusband ?: "") }
    var addr by remember { mutableStateOf(initial?.address ?: "") }
    var shareText by remember { mutableStateOf(initial?.shareText ?: "১.০০০ (১৬ আনা)") }
    var shareRatioStr by remember {
        mutableStateOf(initial?.shareRatio?.let { String.format(Locale.US, "%.4f", it) } ?: "1.0")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "নতুন মালিক / প্রজা যোগ" else "মালিকের তথ্য এডিট") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("মালিকের নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = father,
                    onValueChange = { father = it },
                    label = { Text("পিতা / স্বামীর নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = addr,
                    onValueChange = { addr = it },
                    label = { Text("ঠিকানা / সাং") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("সহজ অংশ বাছাই:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(
                        "১৬ আনা (১.০)" to "1.0",
                        "৮ আনা (০.৫)" to "0.5",
                        "৪ আনা (০.২৫)" to "0.25",
                        "২ আনা (০.১২৫)" to "0.125"
                    )
                    items(presets) { (label, ratioVal) ->
                        FilterChip(
                            selected = shareRatioStr == ratioVal,
                            onClick = {
                                shareRatioStr = ratioVal
                                shareText = label
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shareRatioStr,
                        onValueChange = { shareRatioStr = it },
                        label = { Text("অংশ (০–১.০)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = shareText,
                        onValueChange = { shareText = it },
                        label = { Text("আনা / লেখা") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ratio = shareRatioStr.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
                    onSave(name, father, addr, shareText, ratio)
                },
                enabled = name.isNotBlank()
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun DagEntryDialog(
    initial: KhatianDagEntry?,
    onDismiss: () -> Unit,
    onSave: (dagNo: String, landClass: String, totalDec: Double, share: Double, explicitDec: Double?, remarks: String, revenue: String) -> Unit
) {
    var dagNo by remember { mutableStateOf(initial?.dagNumber ?: "") }
    var landClass by remember { mutableStateOf(initial?.landClass ?: "নাল") }
    var totalAcreStr by remember {
        mutableStateOf(if (initial != null && initial.totalDecimal > 0.0) String.format(Locale.US, "%.4f", initial.totalDecimal / 100.0) else "")
    }
    var totalDecStr by remember {
        mutableStateOf(if (initial != null && initial.totalDecimal > 0.0) String.format(Locale.US, "%.2f", initial.totalDecimal) else "")
    }
    var shareStr by remember {
        mutableStateOf(if (initial != null) String.format(Locale.US, "%.4f", initial.thisKhatianShare) else "1.0")
    }
    var explicitDecStr by remember {
        mutableStateOf(
            if (initial != null && kotlin.math.abs(initial.thisKhatianDecimal - initial.totalDecimal * initial.thisKhatianShare) > 0.01) {
                String.format(Locale.US, "%.2f", initial.thisKhatianDecimal)
            } else ""
        )
    }
    var revenue by remember { mutableStateOf(initial?.revenue ?: "") }
    var remarks by remember { mutableStateOf(initial?.remarks ?: "") }

    val landClasses = listOf("নাল", "ভিটা", "বাড়ী", "পুকুর", "বাগান", "ডোবা", "বাঁশঝাড়", "চালা", "রাস্তা", "পতিত")

    // Parse effective total decimal, auto-adjusting if user typed 4-digit khatian notation like 4800 (which means 48.00 decimal / 0.4800 acre)
    fun parseInputDecimal(raw: String): Double {
        val clean = raw.trim()
        val num = clean.toDoubleOrNull() ?: return 0.0
        return if (!clean.contains('.') && num >= 1000.0 && num <= 9999.0) {
            num / 100.0 // e.g. 4800 -> 48.00 decimal
        } else {
            num
        }
    }

    val parsedDec = parseInputDecimal(totalDecStr)
    val parsedShare = shareStr.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
    val khatianDec = explicitDecStr.toDoubleOrNull() ?: (parsedDec * parsedShare)
    val khatianAcre = khatianDec / 100.0
    val khatianKatha = (khatianDec * 435.6) / 720.0
    val khatianSqFt = khatianDec * 435.6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "নতুন দাগ ও জমি যোগ" else "দাগের তথ্য এডিট") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dagNo,
                        onValueChange = { dagNo = it },
                        label = { Text("দাগ নং *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = revenue,
                        onValueChange = { revenue = it },
                        label = { Text("রাজস্ব (খাজনা)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("জমির শ্রেণী (৫ নং কলাম):", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(landClasses) { cls ->
                        FilterChip(
                            selected = landClass == cls,
                            onClick = { landClass = cls },
                            label = { Text(cls) }
                        )
                    }
                }
                OutlinedTextField(
                    value = landClass,
                    onValueChange = { landClass = it },
                    label = { Text("কাস্টম শ্রেণী (কৃষি / অকৃষি)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("দাগের মোট জমি (৬ নং কলাম):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalAcreStr,
                        onValueChange = {
                            totalAcreStr = it
                            val ac = it.toDoubleOrNull()
                            if (ac != null) {
                                totalDecStr = String.format(Locale.US, "%.2f", ac * 100.0)
                            }
                        },
                        label = { Text("৬(ক) একর") },
                        placeholder = { Text("০.৪৮০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalDecStr,
                        onValueChange = {
                            totalDecStr = it
                            val dec = parseInputDecimal(it)
                            if (dec > 0.0) {
                                totalAcreStr = String.format(Locale.US, "%.4f", dec / 100.0)
                            }
                        },
                        label = { Text("৬(খ) শতাংশ") },
                        placeholder = { Text("৪৮.০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!totalDecStr.contains('.') && (totalDecStr.toDoubleOrNull() ?: 0.0) >= 1000.0) {
                    Text(
                        text = "💡 খতিয়ানের ৪-সংখ্যার নোটেশন শনাক্ত: ${parsedDec} শতক (${String.format(Locale.US, "%.4f", parsedDec / 100.0)} একর)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shareStr,
                        onValueChange = { shareStr = it },
                        label = { Text("৭. অত্র অংশ (০–১)") },
                        placeholder = { Text("১.০০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = explicitDecStr,
                        onValueChange = { explicitDecStr = it },
                        label = { Text("৮. সরাসরি শতক (ঐচ্ছিক)") },
                        placeholder = { Text(if (parsedDec > 0.0) String.format(Locale.US, "%.2f", parsedDec * parsedShare) else "") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live authentic Form 5462/5463 breakdown
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "অত্র খতিয়ানের প্রাপ্যতা (৮ নং কলাম):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.4f", khatianAcre)} একর • ${String.format(Locale.US, "%.2f", khatianDec)} শতক (${String.format(Locale.US, "%.2f", khatianKatha)} কাঠা)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "বর্গফুট: ${String.format(Locale.US, "%,.0f", khatianSqFt)} ব.ফু. (১ শতক = ৪৩৫.৬ ব.ফু.)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("৯. দখল বিষয়ক বা বিশেষ মন্তব্য") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDec = parseInputDecimal(totalDecStr)
                    val share = shareStr.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
                    val explicit = explicitDecStr.toDoubleOrNull()
                    onSave(dagNo, landClass, finalDec, share, explicit, remarks, revenue)
                },
                enabled = dagNo.isNotBlank()
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun OcrAutoFillDialog(
    onDismiss: () -> Unit,
    onApply: (rawText: String) -> Unit
) {
    var ocrText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OCR টেক্সট থেকে খতিয়ান অটো-ফিল", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "খতিয়ানের স্ক্যান করা বা ফটো থেকে প্রাপ্ত বাংলা টেক্সট এখানে পেস্ট করুন। মৌজা, জেএল, খতিয়ান নং, দাগ ও মালিকানা স্বয়ংক্রিয়ভাবে পার্স হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = ocrText,
                    onValueChange = { ocrText = it },
                    label = { Text("OCR টেক্সট পেস্ট করুন") },
                    placeholder = { Text("মৌজা: পুবাইল, জেএল: ৪২, খতিয়ান: ১০৫, দাগ: ১৪২, মালিক: মোঃ করিম...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(ocrText) },
                enabled = ocrText.isNotBlank()
            ) {
                Text("অটো-পার্স ও যোগ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun AddCustomFieldDialog(
    onDismiss: () -> Unit,
    onAdd: (label: String, value: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("অতিরিক্ত কাস্টম ফিল্ড যোগ", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("ফিল্ডের নাম / কলাম (যেমন: সাবেক দাগ বা জরিপকারী)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("তথ্য / মান") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(label, value) },
                enabled = label.isNotBlank() && value.isNotBlank()
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun AddChainOfTitleDialog(
    onDismiss: () -> Unit,
    onAdd: (stage: String, khatianNo: String, dagNo: String, deedNo: String, year: String, parties: String, dec: Double, note: String) -> Unit
) {
    val stages = listOf("সিএস (CS)", "এসএ (SA)", "আরএস (RS)", "বিএস (BS)", "অন্যান্য বায়া")
    var selectedStage by remember { mutableStateOf(stages.first()) }
    var khatianNo by remember { mutableStateOf("") }
    var dagNo by remember { mutableStateOf("") }
    var deedNo by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("") }
    var parties by remember { mutableStateOf("") }
    var decStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বায়া রেকর্ড / মালিকানার ধারাবাহিকতা", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("জরিপ পর্যায় / বায়া স্তর:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(stages) { stg ->
                        FilterChip(
                            selected = selectedStage == stg,
                            onClick = { selectedStage = stg },
                            label = { Text(stg) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = khatianNo,
                        onValueChange = { khatianNo = it },
                        label = { Text("খতিয়ান নং") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dagNo,
                        onValueChange = { dagNo = it },
                        label = { Text("দাগ নং") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deedNo,
                        onValueChange = { deedNo = it },
                        label = { Text("দলিল / কেস নং") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("বছর / তারিখ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = parties,
                    onValueChange = { parties = it },
                    label = { Text("দাতা / গ্রহীতা / পক্ষগণ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = decStr,
                        onValueChange = { decStr = it },
                        label = { Text("জমির পরিমাণ (শতক)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("সংক্ষিপ্ত নোট") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dec = decStr.toDoubleOrNull() ?: 0.0
                    onAdd(selectedStage, khatianNo, dagNo, deedNo, yearStr, parties, dec, note)
                },
                enabled = khatianNo.isNotBlank() || dagNo.isNotBlank() || deedNo.isNotBlank()
            ) {
                Text("বায়া সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

