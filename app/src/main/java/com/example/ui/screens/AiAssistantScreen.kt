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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
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
    var lastRequestedNonce by remember { mutableStateOf(-1L) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    LaunchedEffect(state.searchNonce) {
        val wv = webViewRef
        if (wv != null && lastRequestedNonce != state.searchNonce) {
            lastRequestedNonce = state.searchNonce
            wv.loadUrl(state.searchUrl)
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
        // Slim Header (keeps maximum vertical room for Google Search page)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("ai_back_button")
                    .neuClickable(cornerRadius = 12.dp, elevation = 4.dp, onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = if (state.currentQuery.isBlank()) "Google সার্চ" else state.currentQuery,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { viewModel.resetToGoogleHome() },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Rounded.Home, contentDescription = "Google Home", modifier = Modifier.size(18.dp))
            }
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
                        webViewRef?.url ?: state.searchUrl,
                        state.currentQuery.ifBlank { "Google সার্চ" }
                    )
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = "Full Browser", modifier = Modifier.size(18.dp))
            }
        }

        if (state.isLoadingPage) {
            LinearProgressIndicator(
                progress = { (state.pageProgress / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = EmeraldPrimary
            )
        }

        // Full-Height Embedded Google Search WebView
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .neuSurface(cornerRadius = 18.dp, elevation = 4.dp)
                .clip(RoundedCornerShape(18.dp))
        ) {
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
                        lastRequestedNonce = state.searchNonce
                        loadUrl(state.searchUrl)
                    }
                },
                update = { wv ->
                    if (lastRequestedNonce != state.searchNonce) {
                        lastRequestedNonce = state.searchNonce
                        wv.loadUrl(state.searchUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("google_search_chat_webview")
            )
        }

        // Single Clean Input Box Only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                placeholder = { Text("যা ইচ্ছা লিখে Google-এ খুঁজুন...") },
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
