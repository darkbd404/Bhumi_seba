package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.AiGroundingMode
import com.example.viewmodel.AiUiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AiAssistantScreen(
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    viewModel: AiAssistantViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    var questionText by remember { mutableStateOf("") }

    val suggestions = remember(mode) {
        if (mode == AiGroundingMode.LAND_SEARCH) {
            listOf(
                "ই-নামজারি করতে কী কী কাগজপত্র ও কত ফি লাগে?",
                "সিএস, এসএ, আরএস ও বিএস খতিয়ানের পার্থক্য কী?",
                "অনলাইনে জমির খাজনা (LD Tax) দেওয়ার নিয়ম কী?",
                "মৌজা ম্যাপে ১৬ ইঞ্চি = ১ মাইল স্কেল কীভাবে মাপতে হয়?"
            )
        } else {
            listOf(
                "ঢাকার সাভার উপজেলা ভূমি অফিস ও এসিল্যান্ড অফিস কোথায়?",
                "গাজীপুর সদর সাব-রেজিস্ট্রি অফিসের ঠিকানা দিন",
                "কুমিল্লা জেলা সেটেলমেন্ট ও রেকর্ড রুম কোথায় অবস্থিত?"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .testTag("ai_back_button")
                    .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI ভূমি ও অফিস সহায়ক",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "রিয়েল-টাইম তথ্য ও নিকটস্থ ভূমি অফিস খুঁজুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Grounding Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeuPillChip(
                text = AiGroundingMode.LAND_SEARCH.labelBn,
                selected = mode == AiGroundingMode.LAND_SEARCH,
                icon = Icons.Rounded.Public,
                testTag = "ai_mode_search",
                onClick = { viewModel.setMode(AiGroundingMode.LAND_SEARCH) }
            )
            NeuPillChip(
                text = AiGroundingMode.NEARBY_OFFICES.labelBn,
                selected = mode == AiGroundingMode.NEARBY_OFFICES,
                icon = Icons.Rounded.LocationOn,
                testTag = "ai_mode_maps",
                onClick = { viewModel.setMode(AiGroundingMode.NEARBY_OFFICES) }
            )
        }

        // Content Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "প্রশ্ন বাছুন অথবা নিচে লিখুন:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuClickable(cornerRadius = 16.dp, elevation = 4.dp) {
                                    questionText = s
                                    viewModel.askQuestion(s)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(s, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            when (val st = uiState) {
                is AiUiState.Idle -> Unit
                is AiUiState.Loading -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 20.dp, elevation = 5.dp)
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Text("সর্বশেষ তথ্য যাচাই করে উত্তর তৈরি হচ্ছে...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is AiUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(
                                    cornerRadius = 20.dp,
                                    elevation = 5.dp,
                                    backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ত্রুটি হয়েছে",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(st.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is AiUiState.Success -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neuSurface(cornerRadius = 22.dp, elevation = 6.dp)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "সহায়কের উত্তর",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = st.answerText,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            if (st.sources.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "তথ্যসূত্র ও লিংক:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                st.sources.forEach { src ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(EmeraldPrimary.copy(alpha = 0.09f))
                                            .clickable { onOpenUrlInApp(src.uri, src.title) }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (src.isMapsSource) Icons.Rounded.LocationOn else Icons.Rounded.Public,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = src.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (src.isMapsSource) {
                                            // Exact Google Maps attribution text per guidelines
                                            Text(
                                                text = "Google Maps",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF5E5E5E),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = !st.searchEntryHtml.isNullOrBlank()) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            loadDataWithBaseURL(null, st.searchEntryHtml.orEmpty(), "text/html", "UTF-8", null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                placeholder = { Text("ভূমি বিষয়ক প্রশ্ন লিখুন...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    keyboard?.hide()
                    viewModel.askQuestion(questionText)
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_question_input")
            )
            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.askQuestion(questionText)
                },
                enabled = questionText.isNotBlank() && uiState !is AiUiState.Loading,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
            }
        }
    }
}
