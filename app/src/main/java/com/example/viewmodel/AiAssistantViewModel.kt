package com.example.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URLEncoder

data class SearchChatItem(
    val id: Long,
    val query: String,
    val searchUrl: String
)

data class GoogleChatUiState(
    val history: List<SearchChatItem> = emptyList(),
    val activeItem: SearchChatItem? = null,
    val pageProgress: Int = 0,
    val isLoadingPage: Boolean = false
)

class AiAssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleChatUiState())
    val uiState: StateFlow<GoogleChatUiState> = _uiState.asStateFlow()

    fun searchInChat(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val url = "https://www.google.com/search?q=$encoded"
        val item = SearchChatItem(
            id = System.currentTimeMillis(),
            query = trimmed,
            searchUrl = url
        )
        _uiState.update { state ->
            val deduped = state.history.filterNot { it.query.equals(trimmed, ignoreCase = true) }
            state.copy(
                history = (deduped + item).takeLast(12),
                activeItem = item,
                isLoadingPage = true,
                pageProgress = 10
            )
        }
    }

    fun selectHistoryItem(item: SearchChatItem) {
        _uiState.update {
            it.copy(
                activeItem = item,
                isLoadingPage = true,
                pageProgress = 10
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

    fun clearChat() {
        _uiState.value = GoogleChatUiState()
    }
}
