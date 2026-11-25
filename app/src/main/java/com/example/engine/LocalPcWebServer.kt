package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import com.example.model.PC_SERVER_DEFAULT_PORT
import com.example.model.SurveyorProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

data class PcServerState(
    val isRunning: Boolean = false,
    val port: Int = PC_SERVER_DEFAULT_PORT,
    val lanUrl: String = "",
    val loopbackUrl: String = "",
    val networkHint: String = "",
    val errorMessage: String? = null
)

object LocalPcWebServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PcServerState())
    val state: StateFlow<PcServerState> = _state.asStateFlow()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var cachedHtmlBytes: ByteArray = ByteArray(0)

    @Volatile
    private var cachedIconBytes: ByteArray = ByteArray(0)

    @Volatile
    private var cachedAvatarBytes: ByteArray = ByteArray(0)

    @Volatile
    private var cachedBanners: List<ByteArray> = emptyList()

    private fun decodeDrawablePngBytes(ctx: Context, resId: Int, maxDim: Int = 900, quality: Int = 88, asJpeg: Boolean = false): ByteArray {
        return runCatching {
            val raw = BitmapFactory.decodeResource(ctx.resources, resId) ?: return ByteArray(0)
            val scaled = if (raw.width > maxDim || raw.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
                Bitmap.createScaledBitmap(raw, (raw.width * ratio).toInt().coerceAtLeast(1), (raw.height * ratio).toInt().coerceAtLeast(1), true).also {
                    if (it !== raw) raw.recycle()
                }
            } else raw
            ByteArrayOutputStream().use { out ->
                scaled.compress(if (asJpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, quality, out)
                scaled.recycle()
                out.toByteArray()
            }
        }.getOrDefault(ByteArray(0))
    }

    fun start(context: Context, profile: SurveyorProfile = SurveyorProfile()) {
        val appCtx = context.applicationContext
        val html = DesktopAppExporter.buildStandaloneDesktopHtml(appCtx, profile)
        cachedHtmlBytes = html.toByteArray(Charsets.UTF_8)

        if (cachedIconBytes.isEmpty()) {
            cachedIconBytes = decodeDrawablePngBytes(appCtx, R.drawable.ic_launcher_art, maxDim = 512, quality = 95, asJpeg = false)
        }
        cachedAvatarBytes = runCatching {
            val uriStr = profile.photoUri
            if (!uriStr.isNullOrBlank()) {
                appCtx.contentResolver.openInputStream(Uri.parse(uriStr))?.use { ins ->
                    val bmp = BitmapFactory.decodeStream(ins)
                    if (bmp != null) {
                        ByteArrayOutputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                            bmp.recycle()
                            return@runCatching out.toByteArray()
                        }
                    }
                }
            }
            decodeDrawablePngBytes(appCtx, R.drawable.img_dev_avatar, maxDim = 512, quality = 88, asJpeg = true)
        }.getOrElse {
            decodeDrawablePngBytes(appCtx, R.drawable.img_dev_avatar, maxDim = 512, quality = 88, asJpeg = true)
        }

        if (cachedBanners.isEmpty()) {
            cachedBanners = listOf(
                R.drawable.img_banner_paddy,
                R.drawable.img_banner_survey,
                R.drawable.img_banner_mouza,
                R.drawable.img_banner_delta,
                R.drawable.img_land_hero
            ).map { res -> decodeDrawablePngBytes(appCtx, res, maxDim = 1280, quality = 82, asJpeg = true) }
        }

        if (_state.value.isRunning && serverSocket != null && serverSocket?.isClosed == false) {
            refreshNetworkUrl()
            return
        }

        scope.launch {
            try {
                var bound: ServerSocket? = null
                var chosenPort = PC_SERVER_DEFAULT_PORT
                for (candidate in PC_SERVER_DEFAULT_PORT..(PC_SERVER_DEFAULT_PORT + 12)) {
                    val attempt = runCatching {
                        ServerSocket(candidate).apply { reuseAddress = true }
                    }.getOrNull()
                    if (attempt != null) {
                        bound = attempt
                        chosenPort = candidate
                        break
                    }
                }
                if (bound == null) {
                    _state.update {
                        it.copy(
                            isRunning = false,
                            errorMessage = "পোর্ট খালি পাওয়া যায়নি — অফলাইন PC ফাইল ডাউনলোড ব্যবহার করুন"
                        )
                    }
                    return@launch
                }

                serverSocket = bound
                val ip = findBestLocalIpv4()
                val lan = if (ip != null) "http://$ip:$chosenPort" else "http://127.0.0.1:$chosenPort"
                val loopback = "http://127.0.0.1:$chosenPort"
                val hint = if (ip != null) {
                    "ফোন ও PC একই Wi-Fi বা ফোনের Hotspot-এ যুক্ত করে PC Chrome/Edge-এ $lan খুলুন"
                } else {
                    "Wi-Fi বা ফোনের Hotspot চালু করে আবার চাপুন, অথবা অফলাইন .html ফাইল PC-তে পাঠান"
                }

                _state.value = PcServerState(
                    isRunning = true,
                    port = chosenPort,
                    lanUrl = lan,
                    loopbackUrl = loopback,
                    networkHint = hint,
                    errorMessage = null
                )

                while (!bound.isClosed) {
                    val client = runCatching { bound.accept() }.getOrNull() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                if (serverSocket != null) {
                    _state.update {
                        it.copy(
                            isRunning = false,
                            errorMessage = "সার্ভার ত্রুটি: ${e.localizedMessage ?: "বন্ধ"}"
                        )
                    }
                }
            }
        }
    }

    fun refreshNetworkUrl() {
        val st = _state.value
        if (!st.isRunning) return
        val ip = findBestLocalIpv4()
        val lan = if (ip != null) "http://$ip:${st.port}" else st.loopbackUrl
        val hint = if (ip != null) {
            "ফোন ও PC একই Wi-Fi বা ফোনের Hotspot-এ যুক্ত করে PC Chrome/Edge-এ $lan খুলুন"
        } else {
            "Wi-Fi বা ফোনের Hotspot চালু করুন অথবা নিচের অফলাইন .html ফাইল PC-তে পাঠান"
        }
        _state.update { it.copy(lanUrl = lan, networkHint = hint) }
    }

    fun stop() {
        val old = serverSocket
        serverSocket = null
        runCatching { old?.close() }
        _state.value = PcServerState(isRunning = false)
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            runCatching {
                s.soTimeout = 6000
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val requestLine = reader.readLine() ?: return
                val path = requestLine.split(" ").getOrNull(1)?.substringBefore("?") ?: "/"
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }

                val out = s.getOutputStream()
                when {
                    path == "/manifest.webmanifest" -> {
                        val manifest = """
                            {
                              "name": "ভূমি সেবা ডিজিটাল",
                              "short_name": "ভূমি সেবা",
                              "start_url": "/",
                              "display": "standalone",
                              "background_color": "#E9EEF2",
                              "theme_color": "#0f6b54",
                              "icons": [
                                { "src": "/icon.png", "sizes": "512x512", "type": "image/png" }
                              ]
                            }
                        """.trimIndent().toByteArray(Charsets.UTF_8)
                        writeHttpResponse(out, "200 OK", "application/manifest+json; charset=utf-8", manifest)
                    }
                    path == "/icon.png" && cachedIconBytes.isNotEmpty() -> {
                        writeHttpResponse(out, "200 OK", "image/png", cachedIconBytes)
                    }
                    path == "/avatar.jpg" && cachedAvatarBytes.isNotEmpty() -> {
                        writeHttpResponse(out, "200 OK", "image/jpeg", cachedAvatarBytes)
                    }
                    path.startsWith("/banner/") -> {
                        val idx = path.removePrefix("/banner/").toIntOrNull() ?: -1
                        val bytes = cachedBanners.getOrNull(idx)
                        if (bytes != null && bytes.isNotEmpty()) {
                            writeHttpResponse(out, "200 OK", "image/jpeg", bytes)
                        } else {
                            writeHttpResponse(out, "404 Not Found", "text/plain", "Not Found".toByteArray())
                        }
                    }
                    else -> {
                        writeHttpResponse(out, "200 OK", "text/html; charset=utf-8", cachedHtmlBytes)
                    }
                }
                out.flush()
            }
        }
    }

    private fun writeHttpResponse(
        out: java.io.OutputStream,
        status: String,
        contentType: String,
        body: ByteArray
    ) {
        val headers = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.US_ASCII)
        out.write(headers)
        out.write(body)
    }

    internal fun findBestLocalIpv4(): String? {
        val candidates = mutableListOf<Pair<Int, String>>()
        runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (ifaces.hasMoreElements()) {
                val ni = ifaces.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                val name = ni.name.lowercase()
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        val priority = when {
                            name.startsWith("wlan") || name.startsWith("ap") -> 0
                            name.startsWith("rndis") || name.startsWith("eth") -> 1
                            addr.isSiteLocalAddress -> 2
                            else -> 3
                        }
                        candidates.add(priority to ip)
                    }
                }
            }
        }
        return candidates.minByOrNull { it.first }?.second
    }
}
