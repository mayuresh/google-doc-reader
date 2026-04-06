package com.docreader.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docreader.app.data.model.CURATED_VOICES
import com.docreader.app.data.model.DEFAULT_VOICE
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
    val isLoading: Boolean = false,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val selectedVoice: TtsVoice = DEFAULT_VOICE,
    val availableVoices: List<TtsVoice> = CURATED_VOICES,
    val speedRate: Float = 1.0f,
    val showVoicePanel: Boolean = false,
    val showSleepTimerDialog: Boolean = false,
    val sleepTimerMinutesRemaining: Int? = null,   // null = no timer active
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

    // Caller must set this to play audio — injected to avoid Android context in ViewModel
    var audioPlayer: ((ByteArray) -> Unit)? = null

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
        _uiState.value = _uiState.value.copy(isPlaying = false, isLoading = false)
        SessionManager.resumeInactivityTimer()
    }

    fun stop() {
        playbackJob?.cancel()
        sleepTimerJob?.cancel()
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
        if (next < chunks.size) {
            playbackJob?.cancel()
            _uiState.value = _uiState.value.copy(currentChunkIndex = next)
            if (_uiState.value.isPlaying) startPlayback(next)
        }
    }

    fun skipBack() {
        val prev = (_uiState.value.currentChunkIndex - 1).coerceAtLeast(0)
        playbackJob?.cancel()
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
            // Timer hit zero — stop and logout
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
            var index = fromChunk
            while (index < chunks.size) {
                _uiState.value = _uiState.value.copy(isLoading = true, currentChunkIndex = index)
                val state = _uiState.value
                val result = ttsEngine.synthesise(chunks[index], state.selectedVoice, state.speedRate)
                result.fold(
                    onSuccess = { audioBytes ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        audioPlayer?.invoke(audioBytes)
                        // Wait for playback to finish before fetching next chunk
                        // AudioPlayer signals completion via awaitPlaybackComplete()
                        // For now we estimate duration: ~150 words/min at 1x speed
                        val words = chunks[index].split(" ").size
                        val durationMs = (words / (150f * state.speedRate) * 60_000f).toLong()
                        delay(durationMs)
                        index++
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPlaying = false,
                            error = "TTS error: ${e.message}"
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

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        sleepTimerJob?.cancel()
        ttsEngine.shutdown()
    }
}
