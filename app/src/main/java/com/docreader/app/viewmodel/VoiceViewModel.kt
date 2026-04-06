package com.docreader.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docreader.app.data.model.DocContent
import com.docreader.app.data.model.TtsVoice
import com.docreader.app.data.model.toPlainText
import com.docreader.app.session.SessionManager
import com.docreader.app.tts.TtsEngine
import com.docreader.app.tts.splitIntoChunks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SleepTimerOption(val label: String, val minutes: Int) {
    FIFTEEN("15 min", 15),
    THIRTY("30 min", 30),
    FORTY_FIVE("45 min", 45),
    SIXTY("60 min", 60),
    CUSTOM("Custom", -1)
}

data class VoiceUiState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,       // true while engine initialises on first play
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val availableVoices: List<TtsVoice> = emptyList(),
    val selectedVoice: TtsVoice? = null,  // null until engine loads voices
    val speedRate: Float = 1.0f,
    val showVoicePanel: Boolean = false,
    val showSleepTimerDialog: Boolean = false,
    val sleepTimerMinutesRemaining: Int? = null,
    val error: String? = null
)

class VoiceViewModel(
    private val ttsEngine: TtsEngine,
    private val onLogout: () -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState

    private var chunks: List<String> = emptyList()
    private var playbackJob: Job? = null
    private var sleepTimerJob: Job? = null

    fun prepare(doc: DocContent) {
        val text = doc.toPlainText()
        chunks = splitIntoChunks(text)
        _uiState.value = _uiState.value.copy(
            totalChunks = chunks.size,
            currentChunkIndex = 0
        )
    }

    fun play() {
        if (chunks.isEmpty()) return
        SessionManager.pauseInactivityTimer()
        _uiState.value = _uiState.value.copy(isPlaying = true, error = null)
        startPlayback(_uiState.value.currentChunkIndex)
    }

    fun pause() {
        playbackJob?.cancel()
        ttsEngine.stop()
        _uiState.value = _uiState.value.copy(isPlaying = false, isLoading = false)
        SessionManager.resumeInactivityTimer()
    }

    fun stop() {
        playbackJob?.cancel()
        sleepTimerJob?.cancel()
        ttsEngine.stop()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            isLoading = false,
            currentChunkIndex = 0,
            sleepTimerMinutesRemaining = null
        )
        SessionManager.resumeInactivityTimer()
    }

    fun skipForward() {
        val next = _uiState.value.currentChunkIndex + 1
        if (next >= chunks.size) return
        playbackJob?.cancel()
        ttsEngine.stop()
        _uiState.value = _uiState.value.copy(currentChunkIndex = next)
        if (_uiState.value.isPlaying) startPlayback(next)
    }

    fun skipBack() {
        val prev = (_uiState.value.currentChunkIndex - 1).coerceAtLeast(0)
        playbackJob?.cancel()
        ttsEngine.stop()
        _uiState.value = _uiState.value.copy(currentChunkIndex = prev)
        if (_uiState.value.isPlaying) startPlayback(prev)
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(speedRate = speed)
    }

    fun selectVoice(voice: TtsVoice) {
        _uiState.value = _uiState.value.copy(selectedVoice = voice)
    }

    fun toggleVoicePanel() {
        _uiState.value = _uiState.value.copy(showVoicePanel = !_uiState.value.showVoicePanel)
    }

    fun showSleepTimerDialog() {
        _uiState.value = _uiState.value.copy(showSleepTimerDialog = true)
    }

    fun dismissSleepTimerDialog() {
        _uiState.value = _uiState.value.copy(showSleepTimerDialog = false)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            sleepTimerMinutesRemaining = minutes,
            showSleepTimerDialog = false
        )
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerMinutesRemaining = remaining)
            }
            stop()
            onLogout()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(sleepTimerMinutesRemaining = null)
    }

    private fun startPlayback(fromChunk: Int) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            // Load voices on first play (engine may not be initialised yet)
            if (_uiState.value.availableVoices.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Trigger engine init by speaking an empty string, then load voices
                ttsEngine.speak(" ", _uiState.value.selectedVoice ?: fallbackVoice(), 1.0f)
                val voices = ttsEngine.getAvailableVoices()
                val defaultVoice = voices.firstOrNull() ?: fallbackVoice()
                _uiState.value = _uiState.value.copy(
                    availableVoices = voices,
                    selectedVoice = _uiState.value.selectedVoice ?: defaultVoice,
                    isLoading = false
                )
            }

            var index = fromChunk
            while (index < chunks.size) {
                val state = _uiState.value
                val voice = state.selectedVoice ?: fallbackVoice()

                _uiState.value = state.copy(currentChunkIndex = index)

                // speak() suspends until the chunk finishes — no timers needed
                val result = ttsEngine.speak(chunks[index], voice, state.speedRate)

                result.fold(
                    onSuccess = { index++ },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            error = "Voice error: ${e.message}"
                        )
                        SessionManager.resumeInactivityTimer()
                        return@launch
                    }
                )
            }

            // Reached end of document
            _uiState.value = _uiState.value.copy(isPlaying = false, currentChunkIndex = 0)
            SessionManager.resumeInactivityTimer()
        }
    }

    private fun fallbackVoice() = TtsVoice(
        name = "", displayName = "Default", languageCode = "en-US", gender = null
    )

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        sleepTimerJob?.cancel()
        ttsEngine.shutdown()
    }
}
