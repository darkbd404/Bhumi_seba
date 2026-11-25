package com.example.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URLEncoder

data class GoogleChatUiState(
    val currentQuery: String = "",
    val searchUrl: String = "https://www.google.com/",
    val searchNonce: Long = 0L,
    val pageProgress: Int = 0,
    val isLoadingPage: Boolean = true
)

class AiAssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleChatUiState())
    val uiState: StateFlow<GoogleChatUiState> = _uiState.asStateFlow()

    fun searchInChat(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val url = "https://www.google.com/search?q=$encoded"
        _uiState.update {
            it.copy(
                currentQuery = trimmed,
                searchUrl = url,
                searchNonce = System.currentTimeMillis(),
                isLoadingPage = true,
                pageProgress = 12
            )
        }
    }

    fun updatePageProgress(progress: Int) {
        _uiState.update {
            it.copy(
                pageProgress = progress.coerceIn(0, 100),
                isLoadingPage = progress in 1..94
            )
        }
    }

    fun resetToGoogleHome() {
        _uiState.value = GoogleChatUiState(
            searchUrl = "https://www.google.com/",
            searchNonce = System.currentTimeMillis()
        )
    }
}
