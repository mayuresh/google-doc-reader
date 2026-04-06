package com.docreader.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docreader.app.data.model.DocContent
import com.docreader.app.data.model.ReadingMode
import com.docreader.app.data.model.ReaderSettings
import com.docreader.app.data.repository.DocsRepository
import com.docreader.app.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val docContent: DocContent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val settings: ReaderSettings = ReaderSettings(),
    val showSettings: Boolean = false
)

class ReaderViewModel(private val docsRepository: DocsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState

    fun loadDocument(docId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, docContent = null)
        viewModelScope.launch {
            SessionManager.resetInactivityTimer()
            docsRepository.getDocument(docId).fold(
                onSuccess = { doc ->
                    _uiState.value = _uiState.value.copy(docContent = doc, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load document"
                    )
                }
            )
        }
    }

    fun onUserInteraction() {
        SessionManager.resetInactivityTimer()
    }

    fun increaseFontSize() {
        val current = _uiState.value.settings
        if (current.fontSize < ReaderSettings.FONT_SIZE_MAX) {
            _uiState.value = _uiState.value.copy(
                settings = current.copy(fontSize = current.fontSize + 1)
            )
        }
    }

    fun decreaseFontSize() {
        val current = _uiState.value.settings
        if (current.fontSize > ReaderSettings.FONT_SIZE_MIN) {
            _uiState.value = _uiState.value.copy(
                settings = current.copy(fontSize = current.fontSize - 1)
            )
        }
    }

    fun updateSettings(settings: ReaderSettings) {
        _uiState.value = _uiState.value.copy(settings = settings)
    }

    fun toggleReadingMode() {
        val current = _uiState.value.settings
        val newMode = if (current.readingMode == ReadingMode.SCROLL) ReadingMode.PAGINATED else ReadingMode.SCROLL
        _uiState.value = _uiState.value.copy(settings = current.copy(readingMode = newMode))
    }

    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(showSettings = !_uiState.value.showSettings)
    }
}
