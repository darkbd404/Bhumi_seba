package com.example.engine

import android.content.Context
import com.example.model.SurveyorProfile
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

enum class TunnelStatus {
    STOPPED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class PublicTunnelState(
    val status: TunnelStatus = TunnelStatus.STOPPED,
    val publicUrl: String = "",
    val providerName: String = "",
    val statusMessage: String = "পাবলিক ইন্টারনেট টানেল বন্ধ আছে",
    val errorMessage: String? = null,
    val startedAtMillis: Long? = null
)

/**
 * Creates a live, zero-config outbound reverse HTTPS tunnel from the phone's LocalPcWebServer
 * to public tunnel endpoints (localhost.run on port 22 with automatic Pinggy port-443 fallback),
 * producing a real clickable https://... URL that works from ANY network (e.g. Phone on Mobile Data
 * far away, PC on Home Wi-Fi) without needing personal web hosting.
 */
object PublicPcTunnelManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PublicTunnelState())
    val state: StateFlow<PublicTunnelState> = _state.asStateFlow()

    private val running = AtomicBoolean(false)

    @Volatile
    private var tunnelJob: Job? = null

    @Volatile
    private var activeSession: Session? = null

    @Volatile
    private var activeChannel: ChannelShell? = null

    private data class TunnelProvider(
        val name: String,
        val user: String,
        val host: String,
        val port: Int,
        val remoteForwardPort: Int
    )

    private val providers = listOf(
        TunnelProvider(
            name = "localhost.run",
            user = "nokey",
            host = "localhost.run",
            port = 22,
            remoteForwardPort = 80
        ),
        TunnelProvider(
            name = "Pinggy (443)",
            user = "free",
            host = "a.pinggy.io",
            port = 443,
            remoteForwardPort = 0
        )
    )

    private val ansiRegex = Regex("""\u001B\[[;?0-9]*[A-Za-z]|\r""")
    private val tunnelUrlRegex = Regex(
        """https://[a-zA-Z0-9.-]+\.(?:lhr\.life|lhr\.rocks|pinggy\.link|pinggy\.io|pinggy\.online)"""
    )

    internal fun extractPublicTunnelUrl(rawOutput: String): String? {
        val cleaned = ansiRegex.replace(rawOutput, " ")
        return tunnelUrlRegex.find(cleaned)?.value
    }

    fun start(context: Context, profile: SurveyorProfile = SurveyorProfile()) {
        val appCtx = context.applicationContext
        val cur = _state.value
        if (running.get() && (cur.status == TunnelStatus.CONNECTING || cur.status == TunnelStatus.CONNECTED)) {
            return
        }

        tunnelJob?.cancel()
        closeResources()
        running.set(true)

        _state.value = PublicTunnelState(
            status = TunnelStatus.CONNECTING,
            statusMessage = "যেকোনো নেটওয়ার্কের জন্য পাবলিক HTTPS লিংক তৈরি হচ্ছে...",
            errorMessage = null
        )

        tunnelJob = scope.launch {
            try {
                // Ensure local HTTP server is running first
                if (!LocalPcWebServer.state.value.isRunning) {
                    LocalPcWebServer.start(appCtx, profile)
                }
                var localPort = 0
                for (i in 0 until 40) {
                    val st = LocalPcWebServer.state.value
                    if (st.isRunning && st.port > 0) {
                        localPort = st.port
                        break
                    }
                    delay(100)
                }
                if (localPort <= 0) {
                    running.set(false)
                    _state.value = PublicTunnelState(
                        status = TunnelStatus.ERROR,
                        statusMessage = "লোকাল সার্ভার চালু করা যায়নি",
                        errorMessage = "পোর্ট পাওয়া যায়নি"
                    )
                    return@launch
                }

                var lastErr: String? = null
                for (provider in providers) {
                    if (!running.get() || !isActive) break
                    val connected = tryConnectProvider(provider, localPort)
                    if (connected) {
                        return@launch
                    } else {
                        lastErr = "${provider.name} সংযোগ ব্যর্থ"
                        closeResources()
                    }
                }

                if (running.get()) {
                    running.set(false)
                    _state.value = PublicTunnelState(
                        status = TunnelStatus.ERROR,
                        statusMessage = "পাবলিক টানেল তৈরি হয়নি — নিচের ক্লাউড-রিলে কোড ব্যবহার করুন",
                        errorMessage = lastErr ?: "ফোনের নেটওয়ার্ক টানেল ব্লক করেছে"
                    )
                }
            } catch (_: CancellationException) {
                // Stopped intentionally
            } catch (e: Exception) {
                if (running.get()) {
                    running.set(false)
                    _state.value = PublicTunnelState(
                        status = TunnelStatus.ERROR,
                        statusMessage = "পাবলিক টানেল ত্রুটি — ক্লাউড-রিলে ব্যবহার করুন",
                        errorMessage = e.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"
                    )
                }
            }
        }
    }

    private suspend fun tryConnectProvider(
        provider: TunnelProvider,
        localPort: Int
    ): Boolean {
        _state.update {
            it.copy(
                status = TunnelStatus.CONNECTING,
                providerName = provider.name,
                statusMessage = "${provider.name} দিয়ে পাবলিক ইন্টারনেট URL তৈরি হচ্ছে..."
            )
        }

        return runCatching {
            val jsch = JSch()
            // Generate ephemeral in-memory RSA identity so SSH auth succeeds cleanly
            val kp = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048)
            val prvOut = ByteArrayOutputStream()
            val pubOut = ByteArrayOutputStream()
            kp.writePrivateKey(prvOut)
            kp.writePublicKey(pubOut, "bhumi-seba-tunnel")
            kp.dispose()
            jsch.addIdentity("bhumi-ephemeral", prvOut.toByteArray(), pubOut.toByteArray(), null)

            val session = jsch.getSession(provider.user, provider.host, provider.port).apply {
                setConfig("StrictHostKeyChecking", "no")
                setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password,none")
                setServerAliveInterval(20_000)
                setServerAliveCountMax(3)
                userInfo = object : UserInfo {
                    override fun getPassphrase(): String? = null
                    override fun getPassword(): String = ""
                    override fun promptPassword(message: String?): Boolean = true
                    override fun promptPassphrase(message: String?): Boolean = false
                    override fun promptYesNo(message: String?): Boolean = true
                    override fun showMessage(message: String?) {}
                }
                connect(9000)
            }
            activeSession = session

            session.setPortForwardingR(provider.remoteForwardPort, "127.0.0.1", localPort)
            val channel = (session.openChannel("shell") as ChannelShell).apply {
                setPty(true)
            }
            activeChannel = channel
            val input = channel.inputStream
            channel.connect(7000)

            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val deadline = System.currentTimeMillis() + 10_000L
            var foundUrl: String? = null

            while (running.get() && session.isConnected && System.currentTimeMillis() < deadline) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    val candidate = extractPublicTunnelUrl(line)
                    if (!candidate.isNullOrBlank()) {
                        foundUrl = candidate
                        break
                    }
                } else {
                    delay(120)
                }
            }

            if (foundUrl.isNullOrBlank()) {
                return false
            }

            _state.value = PublicTunnelState(
                status = TunnelStatus.CONNECTED,
                publicUrl = foundUrl,
                providerName = provider.name,
                statusMessage = "পাবলিক ইন্টারনেট URL চালু (${provider.name}) — যেকোনো নেটওয়ার্কে চলবে",
                errorMessage = null,
                startedAtMillis = System.currentTimeMillis()
            )

            // Keep reading & keeping session alive until stopped or remote drops
            while (running.get() && session.isConnected && !channel.isClosed) {
                while (reader.ready()) {
                    reader.readLine() ?: break
                }
                delay(500)
            }

            if (running.get()) {
                running.set(false)
                _state.value = PublicTunnelState(
                    status = TunnelStatus.STOPPED,
                    statusMessage = "পাবলিক টানেল সেশন শেষ হয়েছে — প্রয়োজনে পুনরায় চালু করুন"
                )
            }
            true
        }.getOrDefault(false)
    }

    fun stop() {
        running.set(false)
        tunnelJob?.cancel()
        tunnelJob = null
        closeResources()
        _state.value = PublicTunnelState(
            status = TunnelStatus.STOPPED,
            statusMessage = "পাবলিক ইন্টারনেট টানেল বন্ধ আছে"
        )
    }

    private fun closeResources() {
        runCatching { activeChannel?.disconnect() }
        activeChannel = null
        runCatching { activeSession?.disconnect() }
        activeSession = null
    }
}
