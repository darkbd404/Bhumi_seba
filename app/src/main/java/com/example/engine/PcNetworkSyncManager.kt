package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import com.example.model.CustomKhatianDocument
import com.example.model.DEFAULT_CLOUD_RELAY_URL
import com.example.model.DEFAULT_REMOTE_WEBSITE_URL
import com.example.model.DetectedNetworkType
import com.example.model.PcSyncMode
import com.example.model.PcSyncSettings
import com.example.model.SurveyorProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class SyncConnectionStatus {
    IDLE,
    CHECKING,
    LOCAL_CONNECTED,
    REMOTE_READY,
    CLOUD_RELAY_READY,
    NETWORK_MISMATCH_PROMPT,
    ERROR
}

data class PcNetworkSyncState(
    val detectedNetwork: DetectedNetworkType = DetectedNetworkType.LOCAL_WIFI,
    val activeMode: PcSyncMode = PcSyncMode.LOCAL_WIFI,
    val localIp: String? = null,
    val localUrl: String = "",
    val remoteUrl: String = DEFAULT_REMOTE_WEBSITE_URL,
    val cloudRelayUrl: String = DEFAULT_CLOUD_RELAY_URL,
    val useCloudRelay: Boolean = false,
    val activeTargetUrl: String = "",
    val syncStatus: SyncConnectionStatus = SyncConnectionStatus.IDLE,
    val statusLabel: String = "নেটওয়ার্ক যাচাই হচ্ছে...",
    val statusSubtext: String = "",
    val lastLatencyMs: Long? = null,
    val lastSyncedAtMillis: Long? = null,
    val lastSyncUrl: String = "",
    val lastError: String? = null
)

data class RemoteProbeResult(
    val reachable: Boolean,
    val responseCode: Int,
    val latencyMs: Long,
    val resolvedUrl: String,
    val message: String
)

data class RemoteSyncResult(
    val syncedOverHttp: Boolean,
    val shareableSyncUrl: String,
    val statusMessage: String
)

@Serializable
data class CrossNetworkSyncPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val surveyorProfile: SurveyorProfile = SurveyorProfile(),
    val khatians: List<CustomKhatianDocument> = emptyList()
)

/**
 * Networking utility class that detects local Wi-Fi vs cellular/remote networks and
 * toggles between local IP communication and public cloud-relay / custom website endpoints
 * (e.g. https://www.salam.whf.bz/) for cross-network Khatian sync and PC downloads.
 */
object PcNetworkSyncManager {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(PcNetworkSyncState())
    val state: StateFlow<PcNetworkSyncState> = _state.asStateFlow()

    fun normalizeEndpointUrl(rawUrl: String, fallback: String = DEFAULT_REMOTE_WEBSITE_URL): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return fallback
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return if (withScheme.endsWith("/") || withScheme.contains("?") || withScheme.contains("#")) {
            withScheme
        } else {
            "$withScheme/"
        }
    }

    fun detectNetworkType(context: Context): DetectedNetworkType {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching DetectedNetworkType.LOCAL_WIFI
            val activeNetwork = cm.activeNetwork
            val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            if (caps != null) {
                return@runCatching when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> DetectedNetworkType.LOCAL_WIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> DetectedNetworkType.CELLULAR_REMOTE
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> DetectedNetworkType.CELLULAR_REMOTE
                    else -> DetectedNetworkType.OFFLINE
                }
            }
            if (LocalPcWebServer.findBestLocalIpv4() != null) {
                DetectedNetworkType.LOCAL_WIFI
            } else {
                DetectedNetworkType.OFFLINE
            }
        }.getOrElse {
            if (LocalPcWebServer.findBestLocalIpv4() != null) {
                DetectedNetworkType.LOCAL_WIFI
            } else {
                DetectedNetworkType.OFFLINE
            }
        }
    }

    fun refreshState(
        context: Context,
        settings: PcSyncSettings,
        serverState: PcServerState
    ): PcNetworkSyncState {
        val detected = detectNetworkType(context)
        val localIp = LocalPcWebServer.findBestLocalIpv4()
        val remoteUrl = normalizeEndpointUrl(settings.customRemoteUrl, DEFAULT_REMOTE_WEBSITE_URL)
        val relayUrl = normalizeEndpointUrl(settings.cloudRelayUrl, DEFAULT_CLOUD_RELAY_URL)
        val effectiveRemote = if (settings.useCloudRelay) relayUrl else remoteUrl

        val effectiveMode = if (settings.autoSwitchToRemoteOnCellular && detected == DetectedNetworkType.CELLULAR_REMOTE) {
            PcSyncMode.REMOTE_ACCESS
        } else {
            settings.mode
        }

        val activeTarget = when (effectiveMode) {
            PcSyncMode.LOCAL_WIFI -> serverState.lanUrl.ifBlank {
                if (localIp != null) "http://$localIp:${serverState.port}" else ""
            }
            PcSyncMode.REMOTE_ACCESS -> effectiveRemote
        }

        val (status, label, subtext) = when {
            effectiveMode == PcSyncMode.REMOTE_ACCESS && detected == DetectedNetworkType.OFFLINE -> Triple(
                SyncConnectionStatus.ERROR,
                "অফলাইন — ইন্টারনেট নেই",
                "রিমোট অ্যাক্সেসের জন্য ফোনে ডাটা বা Wi-Fi চালু করুন"
            )
            effectiveMode == PcSyncMode.REMOTE_ACCESS && settings.useCloudRelay -> Triple(
                SyncConnectionStatus.CLOUD_RELAY_READY,
                "ক্লাউড-রিলে মোড সক্রিয়",
                "যেকোনো নেটওয়ার্ক থেকে PC-তে খতিয়ান ও ডাউনলোড চলবে ($relayUrl)"
            )
            effectiveMode == PcSyncMode.REMOTE_ACCESS -> Triple(
                SyncConnectionStatus.REMOTE_READY,
                "রিমোট ওয়েবসাইট মোড সংযুক্ত",
                "যেকোনো নেটওয়ার্কে চলবে — PC ব্রাউজারে $remoteUrl খুলুন"
            )
            detected == DetectedNetworkType.CELLULAR_REMOTE -> Triple(
                SyncConnectionStatus.NETWORK_MISMATCH_PROMPT,
                "মোবাইল ডাটা সনাক্ত হয়েছে (ভিন্ন নেটওয়ার্ক)",
                "ফোনে ডাটা ও PC-তে Wi-Fi থাকলে নিচ থেকে রিমোট অ্যাক্সেসে সুইচ করুন"
            )
            serverState.isRunning && serverState.lanUrl.isNotBlank() -> Triple(
                SyncConnectionStatus.LOCAL_CONNECTED,
                "লোকাল Wi-Fi সার্ভার সংযুক্ত",
                "একই Wi-Fi / Hotspot-এ সিঙ্ক সক্রিয় (${serverState.lanUrl})"
            )
            detected == DetectedNetworkType.LOCAL_WIFI -> Triple(
                SyncConnectionStatus.IDLE,
                "লোকাল Wi-Fi প্রস্তুত (${localIp ?: "Hotspot"})",
                "লোকাল সার্ভার চালু করুন অথবা দূরে থাকলে রিমোট অ্যাক্সেস ব্যবহার করুন"
            )
            else -> Triple(
                SyncConnectionStatus.ERROR,
                "নেটওয়ার্ক সংযুক্ত নয়",
                "Wi-Fi/ডাটা চালু করুন অথবা অফলাইন HTML ফাইল ব্যবহার করুন"
            )
        }

        val updated = _state.value.copy(
            detectedNetwork = detected,
            activeMode = effectiveMode,
            localIp = localIp,
            localUrl = serverState.lanUrl,
            remoteUrl = remoteUrl,
            cloudRelayUrl = relayUrl,
            useCloudRelay = settings.useCloudRelay,
            activeTargetUrl = activeTarget,
            syncStatus = status,
            statusLabel = label,
            statusSubtext = subtext
        )
        _state.value = updated
        return updated
    }

    fun buildCrossNetworkSyncUrl(
        baseUrl: String,
        profile: SurveyorProfile,
        khatians: List<CustomKhatianDocument>
    ): String {
        val normalized = normalizeEndpointUrl(baseUrl, DEFAULT_REMOTE_WEBSITE_URL)
        val trimmedKhatians = khatians.take(15).map { doc ->
            doc.copy(referenceDocPath = null)
        }
        val payload = CrossNetworkSyncPayload(
            exportedAtMillis = System.currentTimeMillis(),
            surveyorProfile = profile.copy(photoUri = null),
            khatians = trimmedKhatians
        )
        val rawJson = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        val encoded = Base64.encodeToString(rawJson, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val baseWithoutHash = normalized.substringBefore("#")
        return "$baseWithoutHash#sync=$encoded"
    }

    suspend fun probeRemoteEndpoint(rawUrl: String): RemoteProbeResult = withContext(Dispatchers.IO) {
        val target = normalizeEndpointUrl(rawUrl, DEFAULT_REMOTE_WEBSITE_URL)
        _state.update { it.copy(syncStatus = SyncConnectionStatus.CHECKING, statusLabel = "রিমোট সার্ভার যাচাই হচ্ছে...", lastError = null) }
        val start = System.currentTimeMillis()
        runCatching {
            val conn = (URL(target).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BhumiSebaDigital/1.0")
            }
            var code = conn.responseCode
            conn.disconnect()
            if (code == 405 || code == 501) {
                val getConn = (URL(target).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "BhumiSebaDigital/1.0")
                }
                code = getConn.responseCode
                getConn.disconnect()
            }
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val ok = code in 200..399
            val msg = if (ok) "রিমোট সার্ভার অনলাইন (HTTP $code • ${elapsed}ms)"
            else "সার্ভার সাড়া দিয়েছে: HTTP $code (${elapsed}ms)"
            _state.update {
                it.copy(
                    syncStatus = if (ok) SyncConnectionStatus.REMOTE_READY else SyncConnectionStatus.ERROR,
                    statusLabel = msg,
                    lastLatencyMs = elapsed,
                    lastError = if (ok) null else msg
                )
            }
            RemoteProbeResult(ok, code, elapsed, target, msg)
        }.getOrElse { err ->
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val errMsg = "রিমোট সংযোগ পাওয়া যায়নি: ${err.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}"
            _state.update {
                it.copy(
                    syncStatus = SyncConnectionStatus.ERROR,
                    statusLabel = errMsg,
                    lastLatencyMs = elapsed,
                    lastError = errMsg
                )
            }
            RemoteProbeResult(false, -1, elapsed, target, errMsg)
        }
    }

    suspend fun syncKhatiansToRemoteEndpoint(
        endpointUrl: String,
        profile: SurveyorProfile,
        khatians: List<CustomKhatianDocument>
    ): RemoteSyncResult = withContext(Dispatchers.IO) {
        val shareableUrl = buildCrossNetworkSyncUrl(endpointUrl, profile, khatians)
        val payloadJson = json.encodeToString(
            CrossNetworkSyncPayload(
                exportedAtMillis = System.currentTimeMillis(),
                surveyorProfile = profile.copy(photoUri = null),
                khatians = khatians.take(15).map { it.copy(referenceDocPath = null) }
            )
        )

        val uploaded = runCatching {
            val normalized = normalizeEndpointUrl(endpointUrl, DEFAULT_REMOTE_WEBSITE_URL)
            val postUrl = if (normalized.endsWith("/")) "${normalized}api/sync" else "$normalized/api/sync"
            val conn = (URL(postUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "BhumiSebaDigital/1.0")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payloadJson)
                writer.flush()
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)

        val now = System.currentTimeMillis()
        _state.update {
            it.copy(
                lastSyncedAtMillis = now,
                lastSyncUrl = shareableUrl,
                syncStatus = if (uploaded) SyncConnectionStatus.CLOUD_RELAY_READY else SyncConnectionStatus.REMOTE_READY,
                statusLabel = if (uploaded) {
                    "ক্লাউড-রিলেতে খতিয়ান সিঙ্ক সম্পন্ন"
                } else {
                    "ক্রস-নেটওয়ার্ক সিঙ্ক লিংক প্রস্তুত"
                }
            )
        }

        RemoteSyncResult(
            syncedOverHttp = uploaded,
            shareableSyncUrl = shareableUrl,
            statusMessage = if (uploaded) {
                "ক্লাউড-রিলে সার্ভারে খতিয়ান সিঙ্ক হয়েছে — PC থেকে সরাসরি ডাউনলোড করতে পারবেন"
            } else {
                "যেকোনো নেটওয়ার্কের জন্য সিঙ্ক লিংক তৈরি হয়েছে — PC ব্রাউজারে খুলুন"
            }
        )
    }
}
