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
import kotlin.random.Random

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
    val publicTunnelUrl: String = "",
    val tunnelStatus: TunnelStatus = TunnelStatus.STOPPED,
    val remoteUrl: String = "",
    val cloudRelayUrl: String = DEFAULT_CLOUD_RELAY_URL,
    val relayRoomCode: String = "",
    val useCloudRelay: Boolean = true,
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
    val relayTopicUrl: String,
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
 * manages:
 * 1. Local IP-based HTTP server (same Wi-Fi / Hotspot),
 * 2. Live Public Internet Reverse Tunnel (lhr.life or pinggy.link) for ANY network,
 * 3. Public Cloud-Relay Topic Sync (ntfy.sh) for cross-network real-time sync,
 * 4. Optional custom user website URL (e.g. https://www.salam.whf.bz/ if hosted).
 */
object PcNetworkSyncManager {

    private const val PREF_FILE = "bhumi_network_relay_prefs"
    private const val KEY_ROOM_CODE = "relay_room_code"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(PcNetworkSyncState())
    val state: StateFlow<PcNetworkSyncState> = _state.asStateFlow()

    fun ensureRelayRoomCode(context: Context, preferred: String = ""): String {
        val cleaned = sanitizeRoomCode(preferred)
        if (cleaned.isNotBlank()) return cleaned
        return runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            val existing = sanitizeRoomCode(prefs.getString(KEY_ROOM_CODE, "") ?: "")
            if (existing.isNotBlank()) {
                existing
            } else {
                val chars = "abcdefghjkmnpqrstuvwxyz23456789"
                val suffix = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
                val generated = "bhumi-seba-$suffix"
                prefs.edit().putString(KEY_ROOM_CODE, generated).apply()
                generated
            }
        }.getOrElse { "bhumi-seba-relay1" }
    }

    fun sanitizeRoomCode(raw: String): String {
        return raw.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_-]"), "")
            .take(40)
    }

    fun normalizeEndpointUrl(rawUrl: String, fallback: String = DEFAULT_CLOUD_RELAY_URL): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return fallback
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    fun buildCloudRelayTopicUrl(relayBaseUrl: String, roomCode: String): String {
        val base = normalizeEndpointUrl(relayBaseUrl, DEFAULT_CLOUD_RELAY_URL).trimEnd('/')
        val room = sanitizeRoomCode(roomCode).ifBlank { "bhumi-seba-relay1" }
        return "$base/$room"
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
        serverState: PcServerState,
        tunnelState: PublicTunnelState = PublicPcTunnelManager.state.value
    ): PcNetworkSyncState {
        val detected = detectNetworkType(context)
        val localIp = LocalPcWebServer.findBestLocalIpv4()
        val roomCode = ensureRelayRoomCode(context, settings.relayRoomCode)
        val relayBase = normalizeEndpointUrl(settings.cloudRelayUrl, DEFAULT_CLOUD_RELAY_URL)
        val customUrl = if (settings.customRemoteUrl.isNotBlank()) {
            normalizeEndpointUrl(settings.customRemoteUrl, DEFAULT_REMOTE_WEBSITE_URL)
        } else {
            ""
        }

        val effectiveMode = if (settings.autoSwitchToRemoteOnCellular && detected == DetectedNetworkType.CELLULAR_REMOTE) {
            PcSyncMode.REMOTE_ACCESS
        } else {
            settings.mode
        }

        val tunnelPublic = tunnelState.publicUrl
        val activeTarget = when {
            effectiveMode == PcSyncMode.LOCAL_WIFI -> serverState.lanUrl.ifBlank {
                if (localIp != null) "http://$localIp:${serverState.port}" else ""
            }
            tunnelState.status == TunnelStatus.CONNECTED && tunnelPublic.isNotBlank() -> {
                "$tunnelPublic/#relay=$roomCode"
            }
            customUrl.isNotBlank() -> "$customUrl/#relay=$roomCode"
            else -> buildCloudRelayTopicUrl(relayBase, roomCode)
        }

        val (status, label, subtext) = when {
            effectiveMode == PcSyncMode.REMOTE_ACCESS && detected == DetectedNetworkType.OFFLINE -> Triple(
                SyncConnectionStatus.ERROR,
                "অফলাইন — ইন্টারনেট নেই",
                "যেকোনো নেটওয়ার্কের রিমোট অ্যাক্সেসের জন্য ফোনে ডাটা বা Wi-Fi চালু করুন"
            )
            tunnelState.status == TunnelStatus.CONNECTED && tunnelPublic.isNotBlank() -> Triple(
                SyncConnectionStatus.REMOTE_READY,
                "পাবলিক ইন্টারনেট URL চালু (${tunnelState.providerName})",
                "দূরে থাকলেও চলবে — PC ব্রাউজারে $tunnelPublic খুলুন"
            )
            tunnelState.status == TunnelStatus.CONNECTING -> Triple(
                SyncConnectionStatus.CHECKING,
                "যেকোনো নেটওয়ার্কের জন্য পাবলিক URL তৈরি হচ্ছে...",
                "কিছুক্ষণ অপেক্ষা করুন — সরাসরি https:// লিংক তৈরি হচ্ছে"
            )
            effectiveMode == PcSyncMode.REMOTE_ACCESS -> Triple(
                SyncConnectionStatus.CLOUD_RELAY_READY,
                "রিমোট ও ক্লাউড-রিলে মোড প্রস্তুত",
                "পাবলিক লিংক চালু করুন অথবা ক্লাউড কোড ($roomCode) দিয়ে ভিন্ন নেটওয়ার্কের PC সিঙ্ক করুন"
            )
            detected == DetectedNetworkType.CELLULAR_REMOTE -> Triple(
                SyncConnectionStatus.NETWORK_MISMATCH_PROMPT,
                "ফোনে মোবাইল ডাটা চালু আছে (ভিন্ন নেটওয়ার্ক)",
                "PC বাসার Wi-Fi-তে থাকলে নিচ থেকে রিমোট/পাবলিক URL চালু করুন"
            )
            serverState.isRunning && serverState.lanUrl.isNotBlank() -> Triple(
                SyncConnectionStatus.LOCAL_CONNECTED,
                "লোকাল Wi-Fi সার্ভার সংযুক্ত",
                "একই Wi-Fi / Hotspot-এ চালু: ${serverState.lanUrl}"
            )
            detected == DetectedNetworkType.LOCAL_WIFI -> Triple(
                SyncConnectionStatus.IDLE,
                "লোকাল Wi-Fi প্রস্তুত (${localIp ?: "Hotspot"})",
                "কাছে থাকলে Wi-Fi সার্ভার, আর দূরে থাকলে রিমোট অ্যাক্সেস চালু করুন"
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
            publicTunnelUrl = tunnelPublic,
            tunnelStatus = tunnelState.status,
            remoteUrl = customUrl,
            cloudRelayUrl = relayBase,
            relayRoomCode = roomCode,
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
        khatians: List<CustomKhatianDocument>,
        relayRoomCode: String = ""
    ): String {
        val normalized = normalizeEndpointUrl(baseUrl, DEFAULT_CLOUD_RELAY_URL)
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
        val baseWithoutHash = normalized.substringBefore("#").let {
            if (it.endsWith("/") || it.contains("?")) it else "$it/"
        }
        val relaySuffix = sanitizeRoomCode(relayRoomCode).takeIf { it.isNotBlank() }?.let { "&relay=$it" } ?: ""
        return "${baseWithoutHash}#sync=$encoded$relaySuffix"
    }

    suspend fun probeRemoteEndpoint(rawUrl: String): RemoteProbeResult = withContext(Dispatchers.IO) {
        val normalized = normalizeEndpointUrl(rawUrl, DEFAULT_CLOUD_RELAY_URL)
        val target = if (normalized.equals(DEFAULT_CLOUD_RELAY_URL, ignoreCase = true)) {
            "$normalized/v1/health"
        } else {
            normalized
        }
        _state.update { it.copy(syncStatus = SyncConnectionStatus.CHECKING, statusLabel = "সংযোগ যাচাই হচ্ছে...", lastError = null) }
        val start = System.currentTimeMillis()
        runCatching {
            val conn = (URL(target).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BhumiSebaDigital/1.0")
            }
            val code = conn.responseCode
            conn.disconnect()
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val ok = code in 200..399
            val msg = if (ok) "ক্লাউড/সার্ভার অনলাইন (HTTP $code • ${elapsed}ms)"
            else "সার্ভার সাড়া দিয়েছে: HTTP $code (${elapsed}ms)"
            _state.update {
                it.copy(
                    syncStatus = if (ok) SyncConnectionStatus.CLOUD_RELAY_READY else SyncConnectionStatus.ERROR,
                    statusLabel = msg,
                    lastLatencyMs = elapsed,
                    lastError = if (ok) null else msg
                )
            }
            RemoteProbeResult(ok, code, elapsed, target, msg)
        }.getOrElse { err ->
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val errMsg = "সংযোগ ব্যর্থ: ${err.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}"
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
        context: Context,
        settings: PcSyncSettings,
        profile: SurveyorProfile,
        khatians: List<CustomKhatianDocument>,
        publicTunnelUrl: String = PublicPcTunnelManager.state.value.publicUrl
    ): RemoteSyncResult = withContext(Dispatchers.IO) {
        val roomCode = ensureRelayRoomCode(context, settings.relayRoomCode)
        val relayTopicUrl = buildCloudRelayTopicUrl(settings.cloudRelayUrl, roomCode)
        val payload = CrossNetworkSyncPayload(
            exportedAtMillis = System.currentTimeMillis(),
            surveyorProfile = profile.copy(photoUri = null),
            khatians = khatians.take(15).map { it.copy(referenceDocPath = null) }
        )
        val payloadJson = json.encodeToString(payload)

        // Update local server cache as well
        LocalPcWebServer.updateSyncPayload(payloadJson)

        var relayUploaded = false
        if (settings.useCloudRelay || publicTunnelUrl.isBlank()) {
            relayUploaded = runCatching {
                val conn = (URL(relayTopicUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("X-Title", "BhumiSebaSync")
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
        }

        val shareableUrl = when {
            publicTunnelUrl.isNotBlank() -> buildCrossNetworkSyncUrl(
                baseUrl = publicTunnelUrl,
                profile = profile,
                khatians = khatians,
                relayRoomCode = roomCode
            )
            settings.customRemoteUrl.isNotBlank() -> buildCrossNetworkSyncUrl(
                baseUrl = settings.customRemoteUrl,
                profile = profile,
                khatians = khatians,
                relayRoomCode = roomCode
            )
            else -> relayTopicUrl
        }

        val now = System.currentTimeMillis()
        _state.update {
            it.copy(
                lastSyncedAtMillis = now,
                lastSyncUrl = shareableUrl,
                relayRoomCode = roomCode,
                syncStatus = if (relayUploaded || publicTunnelUrl.isNotBlank()) {
                    SyncConnectionStatus.CLOUD_RELAY_READY
                } else {
                    SyncConnectionStatus.REMOTE_READY
                },
                statusLabel = if (relayUploaded) {
                    "ক্লাউড-রিলেতে খতিয়ান সিঙ্ক সম্পন্ন ($roomCode)"
                } else {
                    "ক্রস-নেটওয়ার্ক সিঙ্ক লিংক প্রস্তুত"
                }
            )
        }

        val msg = when {
            publicTunnelUrl.isNotBlank() && relayUploaded ->
                "পাবলিক URL ও ক্লাউড-রিলে ($roomCode) দুটোতেই সিঙ্ক সম্পন্ন হয়েছে!"
            relayUploaded ->
                "ক্লাউড-রিলেতে সিঙ্ক হয়েছে! যেকোনো নেটওয়ার্কের PC-তে কোড '$roomCode' দিন।"
            publicTunnelUrl.isNotBlank() ->
                "পাবলিক ইন্টারনেট URL প্রস্তুত — যেকোনো নেটওয়ার্কের PC ব্রাউজারে খুলুন।"
            else ->
                "সিঙ্ক লিংক তৈরি হয়েছে — ইন্টারনেট চালু করে পুনরায় চেষ্টা করুন।"
        }

        RemoteSyncResult(
            syncedOverHttp = relayUploaded || publicTunnelUrl.isNotBlank(),
            relayTopicUrl = relayTopicUrl,
            shareableSyncUrl = shareableUrl,
            statusMessage = msg
        )
    }
}
