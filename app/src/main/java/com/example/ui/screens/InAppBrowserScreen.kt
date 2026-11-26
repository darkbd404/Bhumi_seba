package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.engine.ExportStreamWriter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.neuClickable

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppBrowserScreen(
    initialUrl: String,
    initialTitle: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf(initialTitle) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        fileChooserCallback?.onReceiveValue(uris.toTypedArray())
        fileChooserCallback = null
    }

    // Back button navigates inside WebView history first; pops out cleanly when at first page
    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onNavigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas)
    ) {
        // In-App Browser Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("browser_close_button")
                    .neuClickable(cornerRadius = 12.dp, elevation = 4.dp, onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Browser")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = currentUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = { webViewRef?.goBack() },
                enabled = canGoBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back Page")
            }
            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = canGoForward,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward Page")
            }
            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = EmeraldPrimary
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportMultipleWindows(false)

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank()) currentTitle = title
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            fileChooserCallback?.onReceiveValue(null)
                            fileChooserCallback = filePathCallback
                            fileChooserLauncher.launch(arrayOf("*/*"))
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (!url.isNullOrBlank()) currentUrl = url
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!url.isNullOrBlank()) currentUrl = url
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }

                        // Keep ALL http/https browsing strictly inside this in-app browser
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val reqUrl = request?.url?.toString() ?: return false
                            return if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                false // Load directly inside this WebView
                            } else {
                                true
                            }
                        }
                    }

                    addJavascriptInterface(
                        WebViewDownloadBridge(ctx.applicationContext),
                        "AndroidBridge"
                    )

                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            if (url.startsWith("data:", ignoreCase = true)) {
                                val fallbackName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                    .ifBlank { "BhumiSeba_${System.currentTimeMillis()}" }
                                WebViewDownloadBridge(ctx.applicationContext)
                                    .saveBase64File(url, fallbackName, mimetype.ifBlank { "application/octet-stream" })
                                return@setDownloadListener
                            }
                            val req = DownloadManager.Request(Uri.parse(url)).apply {
                                setMimeType(mimetype)
                                addRequestHeader("User-Agent", userAgent)
                                setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                                setDescription("ভূমি সেবা ডিজিটাল ডাউনলোড")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    URLUtil.guessFileName(url, contentDisposition, mimetype)
                                )
                            }
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(req)
                            Toast.makeText(ctx, "ডাউনলোড শুরু হয়েছে...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "ডাউনলোড করা যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    }

                    loadUrl(initialUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

private class WebViewDownloadBridge(
    private val appContext: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun saveBase64File(base64OrDataUrl: String, fileName: String, mimeType: String) {
        try {
            val cleanB64 = base64OrDataUrl.substringAfter("base64,", base64OrDataUrl)
            val bytes = Base64.decode(cleanB64, Base64.DEFAULT)
            val safeName = fileName.ifBlank { "BhumiSeba_${System.currentTimeMillis()}.bin" }
            val safeMime = mimeType.ifBlank {
                if (safeName.endsWith(".pdf", ignoreCase = true)) "application/pdf"
                else if (safeName.endsWith(".png", ignoreCase = true)) "image/png"
                else "application/octet-stream"
            }
            val savedPath = ExportStreamWriter.saveBinaryFileToDownloads(
                context = appContext,
                bytes = bytes,
                fileName = safeName,
                mimeType = safeMime
            )
            mainHandler.post {
                Toast.makeText(appContext, "সেভ হয়েছে: $savedPath", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            mainHandler.post {
                Toast.makeText(appContext, "ফাইল সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
