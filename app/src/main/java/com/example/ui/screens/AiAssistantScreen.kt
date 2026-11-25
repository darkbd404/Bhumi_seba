package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NeuPillChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface
import com.example.viewmodel.AiAssistantViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AiAssistantScreen(
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    viewModel: AiAssistantViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    var questionText by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    val quickQueries = remember {
        listOf(
            "ই-নামজারি করার নিয়ম ও খরচ বাংলাদেশ",
            "সিএস এসএ আরএস বিএস খতিয়ান পার্থক্য",
            "অনলাইনে জমির খাজনা দেওয়ার নিয়ম",
            "উপজেলা ভূমি অফিস ও এসিল্যান্ড অফিস ঠিকানা",
            "১৬ ইঞ্চি সমান ১ মাইল মৌজা স্কেল হিসাব"
        )
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    LaunchedEffect(state.activeItem?.id) {
        val active = state.activeItem ?: return@LaunchedEffect
        val wv = webViewRef
        if (wv != null && loadedUrl != active.searchUrl) {
            loadedUrl = active.searchUrl
            wv.loadUrl(active.searchUrl)
        }
    }

    fun submitQuery(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        keyboard?.hide()
        questionText = ""
        viewModel.searchInChat(trimmed)
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = "গুগল ভূমি ও তথ্য সার্চ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "যেকোনো বিষয় লিখে সার্চ করুন — চ্যাটেই উত্তর দেখুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (state.activeItem != null) {
                IconButton(
                    onClick = {
                        loadedUrl = null
                        webViewRef = null
                        viewModel.clearChat()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Clear Search",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Previous Search History Strip (acts like chat turns)
        if (state.history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.history.asReversed().forEach { item ->
                    NeuPillChip(
                        text = item.query,
                        selected = state.activeItem?.id == item.id,
                        icon = Icons.Rounded.Search,
                        onClick = {
                            loadedUrl = null
                            viewModel.selectHistoryItem(item)
                        }
                    )
                }
            }
        }

        // Main Chat Response / Google Search Result Area
        val active = state.activeItem
        if (active == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 22.dp, elevation = 6.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TravelExplore,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "সরাসরি Google Search সহায়ক",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "নিচের বক্সে যা ইচ্ছা লিখে পাঠান — সরাসরি গুগলের ফলাফল এই চ্যাট স্ক্রিনেই দেখা যাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "জনপ্রিয় অনুসন্ধান (ট্যাপ করলেই সার্চ হবে):",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                quickQueries.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuClickable(cornerRadius = 16.dp, elevation = 4.dp) {
                                submitQuery(q)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(q, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User Prompt Chat Bubble
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                            .background(EmeraldPrimary)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = active.query,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Embedded Google Search Response Container inside the Chat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .neuSurface(cornerRadius = 20.dp, elevation = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    // Mini response browser bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EmeraldPrimary.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TravelExplore,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Google Search রেজাল্ট",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back Page", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { webViewRef?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward Page", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    onOpenUrlInApp(
                                        webViewRef?.url ?: active.searchUrl,
                                        active.query
                                    )
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = "Full View", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (state.isLoadingPage) {
                        LinearProgressIndicator(
                            progress = { (state.pageProgress / 100f).coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = EmeraldPrimary
                        )
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        viewModel.updatePageProgress(newProgress)
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val targetUrl = request?.url?.toString() ?: return false
                                        return if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                            false
                                        } else {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                            }
                                            true
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        viewModel.updatePageProgress(100)
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }
                                }
                                webViewRef = this
                                loadedUrl = active.searchUrl
                                loadUrl(active.searchUrl)
                            }
                        },
                        update = { wv ->
                            if (loadedUrl != active.searchUrl) {
                                loadedUrl = active.searchUrl
                                wv.loadUrl(active.searchUrl)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("google_search_chat_webview")
                    )
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
                placeholder = { Text("যা ইচ্ছা লিখে গুগলে খুঁজুন...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    submitQuery(questionText)
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_question_input")
            )
            Button(
                onClick = { submitQuery(questionText) },
                enabled = questionText.isNotBlank(),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Search")
            }
        }
    }
}
