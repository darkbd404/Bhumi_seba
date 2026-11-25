package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.KhatianRecord
import com.example.model.MouzaPlot
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SurveyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatianLookupBottomSheet(
    khatianDocName: String,
    khatianPreviewBitmap: Bitmap?,
    khatianPageCount: Int,
    khatianCurrentPage: Int,
    isLoadingKhatian: Boolean,
    khatianRecords: List<KhatianRecord>,
    taggedPlots: List<MouzaPlot> = emptyList(),
    onUploadKhatianClick: () -> Unit,
    onSelectKhatianPage: (Int) -> Unit,
    onAddKhatianRecord: (khatianNo: String, dagNumbersCsv: String, ownerName: String, areaSummary: String) -> Unit,
    onDeleteKhatianRecord: (String) -> Unit,
    onSelectDagForMap: (khatianNo: String, dagNo: String) -> Unit,
    onFocusExistingPlot: ((MouzaPlot) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var newKhatianNo by remember { mutableStateOf("") }
    var newDagCsv by remember { mutableStateOf("") }
    var newOwner by remember { mutableStateOf("") }
    var newArea by remember { mutableStateOf("") }
    var showFullScreenKhatian by remember { mutableStateOf(false) }

    val query = searchQuery.trim()
    val filteredRecords = remember(khatianRecords, query) {
        if (query.isEmpty()) khatianRecords
        else khatianRecords.filter { rec ->
            rec.khatianNumber.contains(query, ignoreCase = true) ||
                rec.ownerName.contains(query, ignoreCase = true) ||
                rec.dagNumbers.any { it.contains(query, ignoreCase = true) }
        }
    }
    val filteredTaggedPlots = remember(taggedPlots, query) {
        taggedPlots.filter { p ->
            (p.dagNumber.isNotBlank() || p.khatianNumber.isNotBlank()) && (
                query.isEmpty() ||
                    p.dagNumber.contains(query, ignoreCase = true) ||
                    p.khatianNumber.contains(query, ignoreCase = true)
                )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "খতিয়ান ও দাগ নং অনুসন্ধান",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "খতিয়ান PDF/ছবি আপলোড করে দাগ নং ক্লিক করলে ম্যাপে সরাসরি খোঁজা যাবে",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onUploadKhatianClick,
                    modifier = Modifier.testTag("upload_khatian_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (khatianPreviewBitmap == null) "খতিয়ান নিন" else "বদলান", maxLines = 1)
                }
            }

            if (isLoadingKhatian) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
            }

            // Interactive Preview of Uploaded Khatian File
            if (khatianPreviewBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.08f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = khatianDocName.ifBlank { "আপলোডকৃত খতিয়ান" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (khatianPageCount > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onSelectKhatianPage(khatianCurrentPage - 1) },
                                    enabled = khatianCurrentPage > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev Khatian Page", modifier = Modifier.size(18.dp))
                                }
                                Text("${khatianCurrentPage + 1}/$khatianPageCount", style = MaterialTheme.typography.labelSmall)
                                IconButton(
                                    onClick = { onSelectKhatianPage(khatianCurrentPage + 1) },
                                    enabled = khatianCurrentPage < khatianPageCount - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Khatian Page", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        IconButton(
                            onClick = { showFullScreenKhatian = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Fullscreen, contentDescription = "Zoom Khatian", tint = EmeraldPrimary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable { showFullScreenKhatian = true }
                    ) {
                        Image(
                            bitmap = khatianPreviewBitmap.asImageBitmap(),
                            contentDescription = "Uploaded Khatian Sheet",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC16222B))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("ট্যাপ করে বড় ও জুম করুন", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Search Bar + Add Dag/Khatian Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("খতিয়ান বা দাগ নং খুঁজুন...", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("khatian_search_input")
                )
                OutlinedButton(
                    onClick = { showAddForm = !showAddForm },
                    modifier = Modifier.testTag("toggle_add_khatian_entry"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)
                ) {
                    Icon(if (showAddForm) Icons.Rounded.Close else Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("নোট", maxLines = 1)
                }
            }

            // Add Khatian + Dag Numbers Form
            AnimatedVisibility(
                visible = showAddForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurveyGold.copy(alpha = 0.10f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newKhatianNo,
                            onValueChange = { newKhatianNo = it },
                            label = { Text("খতিয়ান নং") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newDagCsv,
                            onValueChange = { newDagCsv = it },
                            label = { Text("দাগ নং (যেমন: ৪৫২, ৪৫৩)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.6f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newOwner,
                            onValueChange = { newOwner = it },
                            label = { Text("মালিক (ঐচ্ছিক)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newArea,
                            onValueChange = { newArea = it },
                            label = { Text("পরিমাণ") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Button(
                        onClick = {
                            if (newKhatianNo.isNotBlank() || newDagCsv.isNotBlank()) {
                                onAddKhatianRecord(
                                    newKhatianNo.trim(),
                                    newDagCsv.trim(),
                                    newOwner.trim(),
                                    newArea.trim()
                                )
                                newKhatianNo = ""
                                newDagCsv = ""
                                newOwner = ""
                                newArea = ""
                                showAddForm = false
                            }
                        },
                        enabled = newKhatianNo.isNotBlank() || newDagCsv.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("save_khatian_entry_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("খতিয়ান ও দাগ তালিকাভুক্ত করুন")
                    }
                }
            }

            // Matching Plots already tagged on the Mouza Map
            if (filteredTaggedPlots.isNotEmpty() && onFocusExistingPlot != null) {
                Text(
                    text = "নকশায় চিহ্নিত ম্যাচিং দাগ (ক্লিক করে জুম করুন):",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredTaggedPlots.forEach { plot ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(plot.color.copy(alpha = 0.16f))
                                .clickable {
                                    onFocusExistingPlot(plot)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = buildString {
                                    if (plot.dagNumber.isNotBlank()) append("দাগ ${plot.dagNumber}") else append("দাগ #${plot.plotIndex}")
                                    if (plot.khatianNumber.isNotBlank()) append(" • খ:${plot.khatianNumber}")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = plot.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Uploaded / Listed Khatian-Dag Entries
            if (filteredRecords.isEmpty()) {
                Text(
                    text = if (khatianRecords.isEmpty()) {
                        "উপরের 'নোট' বাটনে খতিয়ান ও দাগ নম্বর লিখুন — তারপর যেকোনো দাগ নম্বরে ক্লিক করে মৌজা PDF/ম্যাপে খুঁজুন।"
                    } else {
                        "\"$query\" অনুযায়ী কোনো খতিয়ান বা দাগ পাওয়া যায়নি।"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { rec ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = buildString {
                                            if (rec.khatianNumber.isNotBlank()) append("খতিয়ান: ${rec.khatianNumber}")
                                            if (rec.ownerName.isNotBlank()) {
                                                if (isNotEmpty()) append(" • ")
                                                append(rec.ownerName)
                                            }
                                            if (isEmpty()) append("খতিয়ান এন্ট্রি")
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (rec.areaSummary.isNotBlank()) {
                                        Text(
                                            text = "পরিমাণ: ${rec.areaSummary}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteKhatianRecord(rec.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Entry", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }

                            if (rec.dagNumbers.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rec.dagNumbers.forEach { dag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(EmeraldPrimary)
                                                .clickable {
                                                    onSelectDagForMap(rec.khatianNumber, dag)
                                                    onDismiss()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "দাগ $dag খুঁজুন",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    // Full-screen Zoomable Khatian Viewer Dialog
    if (showFullScreenKhatian && khatianPreviewBitmap != null) {
        var kScale by remember { mutableFloatStateOf(1f) }
        var kOffX by remember { mutableFloatStateOf(0f) }
        var kOffY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { showFullScreenKhatian = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E171D))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = khatianDocName.ifBlank { "খতিয়ান জুম ভিউ" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (khatianPageCount > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSelectKhatianPage(khatianCurrentPage - 1) },
                                enabled = khatianCurrentPage > 0
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                            }
                            Text("${khatianCurrentPage + 1}/$khatianPageCount", color = Color.White)
                            IconButton(
                                onClick = { onSelectKhatianPage(khatianCurrentPage + 1) },
                                enabled = khatianCurrentPage < khatianPageCount - 1
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next", tint = Color.White)
                            }
                        }
                    }
                    Button(
                        onClick = { showFullScreenKhatian = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ম্যাপে ফিরুন")
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextS = (kScale * zoom).coerceIn(1f, 8f)
                                kScale = nextS
                                if (nextS <= 1.02f) {
                                    kOffX = 0f; kOffY = 0f
                                } else {
                                    kOffX += pan.x
                                    kOffY += pan.y
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = khatianPreviewBitmap.asImageBitmap(),
                        contentDescription = "Zoomable Khatian Document",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = kScale
                                scaleY = kScale
                                translationX = kOffX
                                translationY = kOffY
                            }
                    )
                }
            }
        }
    }
}
