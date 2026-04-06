package com.docreader.app.tts

import com.docreader.app.data.model.TtsVoice

/**
 * Abstraction over TTS engines.
 *
 * speak() suspends until the chunk has finished playing.
 * This lets VoiceViewModel simply loop through chunks with sequential calls,
 * without needing timers, ByteArrays, or an audio player.
 */
interface TtsEngine {

    /**
     * Speaks [text] with the given [voice] and [speedRate].
     * Suspends until speech is fully complete (or fails).
     * Cancelling the coroutine must stop playback immediately.
     */
    suspend fun speak(text: String, voice: TtsVoice, speedRate: Float): Result<Unit>

    /**
     * Returns high-quality voices available on this engine.
     * Called after the engine is initialised.
     */
    fun getAvailableVoices(): List<TtsVoice>

    /** Stops any ongoing speech immediately. */
    fun stop()

    /** Releases engine resources. Call from ViewModel.onCleared(). */
    fun shutdown()
}

/**
 * Splits text into chunks at sentence/paragraph boundaries.
 * Used for progress tracking (Section X of Y) and skip forward/back.
 * Android TTS can handle long text natively but we chunk for UX control.
 */
fun splitIntoChunks(text: String, maxChars: Int = 4000): List<String> {
    if (text.length <= maxChars) return listOf(text).filter { it.isNotBlank() }

    val chunks = mutableListOf<String>()
    var start = 0

    while (start < text.length) {
        val end = minOf(start + maxChars, text.length)
        if (end == text.length) {
            chunks.add(text.substring(start))
            break
        }
        val splitAt = text.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'), end)
            .takeIf { it > start }?.plus(1) ?: end
        chunks.add(text.substring(start, splitAt).trim())
        start = splitAt
    }

    return chunks.filter { it.isNotBlank() }
}
