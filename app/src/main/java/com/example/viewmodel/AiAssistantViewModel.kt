package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.RetrievalConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAssistantVM"

enum class AiGroundingMode(val labelBn: String) {
    LAND_SEARCH("ভূমি আইন ও সেবা (Google Search)"),
    NEARBY_OFFICES("নিকটস্থ ভূমি অফিস (Google Maps)")
}

data class GroundingSourceItem(
    val title: String,
    val uri: String,
    val isMapsSource: Boolean,
    val placeId: String? = null
)

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(
        val answerText: String,
        val sources: List<GroundingSourceItem>,
        val searchEntryHtml: String?
    ) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiAssistantViewModel : ViewModel() {

    private val searchModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "আপনি বাংলাদেশের ভূমি সেবা, মৌজা ম্যাপ (CS, SA, RS, BS), খতিয়ান, নামজারি (E-Mutation), " +
                        "ভূমি উন্নয়ন কর, ওয়ারিশ ও জমি পরিমাপ বিষয়ে দক্ষ সহায়ক। উত্তর সর্বদা পরিষ্কার, সঠিক এবং সহজ বাংলায় দিন।"
                )
            }
        )
    }

    private val _mode = MutableStateFlow(AiGroundingMode.LAND_SEARCH)
    val mode: StateFlow<AiGroundingMode> = _mode.asStateFlow()

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun setMode(newMode: AiGroundingMode) {
        _mode.value = newMode
    }

    fun askQuestion(
        query: String,
        userLat: Double? = null,
        userLon: Double? = null
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val currentMode = _mode.value
                val response = withTimeout(60_000L) {
                    if (currentMode == AiGroundingMode.NEARBY_OFFICES) {
                        val lat = userLat ?: 23.8103
                        val lon = userLon ?: 90.4125
                        val mapsModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                            modelName = "gemini-3.8-flash",
                            toolConfig = ToolConfig(
                                retrievalConfig = RetrievalConfig.builder()
                                    .setLatLng(LatLng(latitude = lat, longitude = lon))
                                    .setLanguageCode("bn")
                                    .build()
                            ),
                            tools = listOf(Tool.googleMaps()),
                            systemInstruction = content {
                                text(
                                    "আপনি বাংলাদেশের উপজেলা ভূমি অফিস, এসিল্যান্ড অফিস, সাব-রেজিস্ট্রি অফিস এবং সেটেলমেন্ট অফিস খুঁজে দেওয়ার সহায়ক।"
                                )
                            }
                        )
                        mapsModel.generateContent(trimmed)
                    } else {
                        searchModel.generateContent(trimmed)
                    }
                }

                val text = response.text
                if (text.isNullOrBlank()) {
                    _uiState.value = AiUiState.Error("মডেল থেকে কোনো উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।")
                    return@launch
                }

                val metadata = response.candidates.firstOrNull()?.groundingMetadata
                val searchHtml = metadata?.searchEntryPoint?.renderedContent
                val sources = mutableListOf<GroundingSourceItem>()

                metadata?.groundingChunks?.forEach { chunk ->
                    chunk.web?.let { web ->
                        val title = web.title
                        val uri = web.uri
                        if (!title.isNullOrBlank() && !uri.isNullOrBlank()) {
                            sources.add(
                                GroundingSourceItem(
                                    title = title,
                                    uri = uri,
                                    isMapsSource = false
                                )
                            )
                        }
                    }
                    chunk.maps?.let { maps ->
                        val title = maps.title
                        val uri = maps.uri
                        if (!title.isNullOrBlank() && !uri.isNullOrBlank()) {
                            sources.add(
                                GroundingSourceItem(
                                    title = title,
                                    uri = uri,
                                    isMapsSource = true,
                                    placeId = maps.placeId
                                )
                            )
                        }
                    }
                }

                _uiState.value = AiUiState.Success(
                    answerText = text,
                    sources = sources.distinctBy { it.uri },
                    searchEntryHtml = searchHtml
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI request timed out", e)
                _uiState.value = AiUiState.Error("উত্তর আসতে সময় বেশি লেগেছে (Timeout)। আবার চেষ্টা করুন।")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _uiState.value = AiUiState.Error(
                    e.localizedMessage ?: "AI সংযোগে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
                )
            }
        }
    }
}
