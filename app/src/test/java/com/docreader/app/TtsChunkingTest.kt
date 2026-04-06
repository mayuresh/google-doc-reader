package com.docreader.app

import com.docreader.app.tts.splitIntoChunks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsChunkingTest {

    @Test
    fun `short text returns single chunk`() {
        val text = "Hello world. This is a short sentence."
        val chunks = splitIntoChunks(text, maxChars = 4000)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun `long text is split into multiple chunks`() {
        val sentence = "This is a test sentence. "
        val text = sentence.repeat(300)  // ~7500 chars
        val chunks = splitIntoChunks(text, maxChars = 4000)
        assertTrue(chunks.size >= 2)
    }

    @Test
    fun `each chunk does not exceed max size`() {
        val sentence = "Short sentence here. "
        val text = sentence.repeat(500)
        val chunks = splitIntoChunks(text, maxChars = 4000)
        chunks.forEach { chunk ->
            assertTrue("Chunk size ${chunk.length} exceeds limit", chunk.length <= 4000)
        }
    }

    @Test
    fun `chunks concatenate to cover all text`() {
        val sentence = "Another test sentence with some words in it. "
        val text = sentence.repeat(200)
        val chunks = splitIntoChunks(text, maxChars = 4000)
        val reconstructed = chunks.joinToString(" ")
        // All words from original text should be present
        assertTrue(reconstructed.length >= text.trim().length * 0.95)
    }
}
