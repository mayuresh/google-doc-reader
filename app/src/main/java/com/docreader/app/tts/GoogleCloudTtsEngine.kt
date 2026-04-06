package com.docreader.app.tts

import android.util.Base64
import com.docreader.app.data.model.CURATED_VOICES
import com.docreader.app.data.model.TtsVoice
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val TTS_API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"

/**
 * Google Cloud TTS Neural2 engine.
 * NOT currently used — AndroidTtsEngine is the active engine (issue #2).
 * Kept here as a future upgrade path if a user wants premium Neural2 quality
 * and is willing to provide their own API key.
 *
 * To re-enable: wire this up in MainActivity instead of AndroidTtsEngine,
 * and restore the google_cloud_tts_api_key entry in secrets.xml.
 */
class GoogleCloudTtsEngine(
    private val httpClient: OkHttpClient,
    private val apiKey: String
) : TtsEngine {

    // Google Cloud TTS cannot play directly — it returns audio bytes.
    // This engine is not compatible with the speak()-suspends-until-done contract
    // without an audio player. Marked as unsupported until re-integrated with ExoPlayer.
    override suspend fun speak(text: String, voice: TtsVoice, speedRate: Float): Result<Unit> {
        return Result.failure(UnsupportedOperationException(
            "GoogleCloudTtsEngine requires an audio player. Use AndroidTtsEngine instead."
        ))
    }

    override fun getAvailableVoices(): List<TtsVoice> = CURATED_VOICES

    override fun stop() {}

    override fun shutdown() {}

    /** Synthesises text and returns raw MP3 bytes. Available for future use. */
    suspend fun synthesise(text: String, voice: TtsVoice, speedRate: Float): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val body = buildRequestJson(text, voice, speedRate).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$TTS_API_URL?key=$apiKey")
                .post(body)
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        RuntimeException("TTS API error ${response.code}: ${response.body?.string()}")
                    )
                }
                val json = JsonParser.parseString(response.body?.string()).asJsonObject
                val audioContent = json.get("audioContent")?.asString
                    ?: return@withContext Result.failure(RuntimeException("No audio in response"))
                Result.success(Base64.decode(audioContent, Base64.DEFAULT))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildRequestJson(text: String, voice: TtsVoice, speedRate: Float): JsonObject =
        JsonObject().apply {
            add("input", JsonObject().apply { addProperty("text", text) })
            add("voice", JsonObject().apply {
                addProperty("languageCode", voice.languageCode)
                addProperty("name", voice.name)
            })
            add("audioConfig", JsonObject().apply {
                addProperty("audioEncoding", "MP3")
                addProperty("speakingRate", speedRate.toDouble())
                addProperty("pitch", 0.0)
            })
        }
}
