package com.docreader.app.tts

import android.util.Base64
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
 *
 * Each call synthesises one chunk of text and returns MP3 bytes.
 * Long documents should be pre-split using [splitIntoChunks] before calling this.
 *
 * Audio is returned as raw bytes — never written to disk (incognito requirement).
 * The caller (VoiceViewModel) feeds these bytes to ExoPlayer via ByteArrayDataSource.
 */
class GoogleCloudTtsEngine(
    private val httpClient: OkHttpClient,
    private val apiKey: String
) : TtsEngine {

    override suspend fun synthesise(
        text: String,
        voice: TtsVoice,
        speedRate: Float
    ): Result<ByteArray> = withContext(Dispatchers.IO) {

        val requestBody = buildRequestJson(text, voice, speedRate)
        val body = requestBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$TTS_API_URL?key=$apiKey")
            .post(body)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "unknown error"
                return@withContext Result.failure(
                    RuntimeException("TTS API error ${response.code}: $errorBody")
                )
            }
            val json = JsonParser.parseString(response.body?.string()).asJsonObject
            val audioContent = json.get("audioContent")?.asString
                ?: return@withContext Result.failure(RuntimeException("No audio in response"))

            val audioBytes = Base64.decode(audioContent, Base64.DEFAULT)
            Result.success(audioBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequestJson(text: String, voice: TtsVoice, speedRate: Float): JsonObject {
        return JsonObject().apply {
            add("input", JsonObject().apply {
                addProperty("text", text)
            })
            add("voice", JsonObject().apply {
                addProperty("languageCode", voice.languageCode)
                addProperty("name", voice.name)
            })
            add("audioConfig", JsonObject().apply {
                addProperty("audioEncoding", "MP3")
                addProperty("speakingRate", speedRate.toDouble())
                // Slight pitch adjustment for naturalness
                addProperty("pitch", 0.0)
                addProperty("effectsProfileId", "headphone-class-device")
            })
        }
    }

    override fun shutdown() {
        // OkHttpClient is shared — not closed here
    }
}
