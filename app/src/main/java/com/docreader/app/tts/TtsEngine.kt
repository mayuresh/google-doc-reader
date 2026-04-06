package com.docreader.app.tts

import com.docreader.app.data.model.TtsVoice

/** Abstraction over TTS engines — swap Google Cloud for another engine without touching UI. */
interface TtsEngine {
    /** Synthesise [text] with the given [voice] and return raw MP3 audio bytes. */
    suspend fun synthesise(text: String, voice: TtsVoice, speedRate: Float): Result<ByteArray>

    /** Release any resources held by this engine. */
    fun shutdown()
}

/** Splits a long text into chunks that fit within the TTS API limit (4000 chars). */
fun splitIntoChunks(text: String, maxChars: Int = 4000): List<String> {
    if (text.length <= maxChars) return listOf(text)

    val chunks = mutableListOf<String>()
    var start = 0

    while (start < text.length) {
        val end = minOf(start + maxChars, text.length)
        if (end == text.length) {
            chunks.add(text.substring(start))
            break
        }
        // Try to split at sentence boundary (. ! ?)
        val sentenceEnd = text.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'), end)
        val splitAt = if (sentenceEnd > start) sentenceEnd + 1 else end
        chunks.add(text.substring(start, splitAt).trim())
        start = splitAt
    }

    return chunks.filter { it.isNotBlank() }
}
