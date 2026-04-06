package com.docreader.app.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.docreader.app.data.model.TtsVoice
import com.docreader.app.data.model.VoiceGender
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * On-device TTS engine using Android's TextToSpeech API.
 *
 * On Pixel 7a (Google TTS engine), this uses neural on-device voices —
 * no internet required, no API key, no central billing account.
 *
 * Initialization is lazy: the TTS engine is started on the first speak() call
 * and reused for all subsequent calls.
 *
 * speak() bridges Android's callback API to a coroutine that suspends until
 * the utterance is fully complete, so VoiceViewModel can loop chunks cleanly.
 */
class AndroidTtsEngine(private val context: Context) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var initResult: Result<Unit>? = null  // null = not yet initialised

    /**
     * Initialises the TTS engine if not already done.
     * Safe to call multiple times — only initialises once.
     */
    private suspend fun ensureInitialised(): Result<Unit> {
        initResult?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            tts = TextToSpeech(context) { status ->
                val result = if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("TTS engine failed to initialise (status=$status)"))
                }
                initResult = result
                continuation.resume(result)
            }
        }
    }

    /**
     * Speaks [text] and suspends until the utterance finishes.
     * Coroutine cancellation calls tts.stop() immediately.
     */
    override suspend fun speak(text: String, voice: TtsVoice, speedRate: Float): Result<Unit> {
        val init = ensureInitialised()
        if (init.isFailure) return init

        val engine = tts ?: return Result.failure(RuntimeException("TTS engine not available"))

        // Apply the selected voice
        engine.voices
            ?.find { it.name == voice.name }
            ?.let { engine.voice = it }
            ?: engine.setLanguage(Locale.forLanguageTag(voice.languageCode))

        engine.setSpeechRate(speedRate)

        val utteranceId = UUID.randomUUID().toString()

        return suspendCancellableCoroutine { continuation ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uid: String?) {}

                override fun onDone(uid: String?) {
                    if (uid == utteranceId) {
                        continuation.resume(Result.success(Unit))
                    }
                }

                @Deprecated("Deprecated in API 21", ReplaceWith("onError(utteranceId, errorCode)"))
                override fun onError(uid: String?) {
                    if (uid == utteranceId) {
                        continuation.resume(Result.failure(RuntimeException("TTS error on utterance $uid")))
                    }
                }

                override fun onError(uid: String?, errorCode: Int) {
                    if (uid == utteranceId) {
                        continuation.resume(Result.failure(RuntimeException("TTS error $errorCode")))
                    }
                }
            })

            engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)

            continuation.invokeOnCancellation { engine.stop() }
        }
    }

    /**
     * Returns high-quality English voices available on this device.
     * Filters to on-device only (no network required) and high quality.
     * Falls back to a single default voice if none match the filter.
     */
    override fun getAvailableVoices(): List<TtsVoice> {
        val engine = tts ?: return listOf(defaultVoice())

        val highQualityVoices = engine.voices
            ?.filter { voice ->
                !voice.isNetworkConnectionRequired &&
                voice.quality >= Voice.QUALITY_HIGH &&
                voice.locale.language == "en" &&
                !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            ?.sortedWith(compareBy({ it.locale.country }, { it.name }))
            ?.mapIndexed { index, voice -> voice.toTtsVoice(index) }
            ?.distinctBy { it.displayName }  // deduplicate by display name

        return if (highQualityVoices.isNullOrEmpty()) listOf(defaultVoice())
        else highQualityVoices
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initResult = null
    }

    private fun defaultVoice() = TtsVoice(
        name = "",
        displayName = "Default voice",
        languageCode = "en-US",
        gender = null
    )
}

/** Maps an Android Voice to our TtsVoice model with a readable display name. */
private fun Voice.toTtsVoice(index: Int): TtsVoice {
    val country = when (locale.country.uppercase()) {
        "US" -> "US"
        "GB" -> "UK"
        "AU" -> "Australia"
        "IN" -> "India"
        "CA" -> "Canada"
        "IE" -> "Ireland"
        "NZ" -> "New Zealand"
        "ZA" -> "South Africa"
        else -> locale.displayCountry.ifBlank { locale.country }
    }

    // Google TTS voice names encode gender: "f" = female, "m" = male
    // e.g. en-us-x-sfg-local (female), en-us-x-sfm-local (male)
    val gender = when {
        name.contains("-sfg-") || name.contains("-tpf-") || name.contains("-iob-") -> VoiceGender.FEMALE
        name.contains("-sfm-") || name.contains("-tpm-") || name.contains("-iom-") -> VoiceGender.MALE
        else -> null
    }

    val genderLabel = when (gender) {
        VoiceGender.FEMALE -> "F"
        VoiceGender.MALE -> "M"
        null -> ""
    }

    val displayName = if (genderLabel.isNotEmpty()) "English ($country, $genderLabel)"
                      else "English ($country)"

    return TtsVoice(
        name = name,
        displayName = displayName,
        languageCode = locale.toLanguageTag(),
        gender = gender
    )
}
