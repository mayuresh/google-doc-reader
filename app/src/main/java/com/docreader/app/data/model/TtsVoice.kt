package com.docreader.app.data.model

enum class VoiceGender { MALE, FEMALE }

data class TtsVoice(
    val name: String,           // Android voice name, e.g. "en-us-x-sfg-local"
    val displayName: String,    // Human-readable, e.g. "English (US, F)"
    val languageCode: String,   // BCP-47 tag, e.g. "en-US"
    val gender: VoiceGender?    // null if not determinable from the voice name
)

/**
 * Curated fallback list for Google Cloud TTS Neural2 (GoogleCloudTtsEngine).
 * Not used by AndroidTtsEngine — that engine queries voices from the device at runtime.
 */
val CURATED_VOICES = listOf(
    TtsVoice("en-US-Neural2-A", "Aria (US, F)", "en-US", VoiceGender.FEMALE),
    TtsVoice("en-US-Neural2-C", "Clara (US, F)", "en-US", VoiceGender.FEMALE),
    TtsVoice("en-US-Neural2-D", "David (US, M)", "en-US", VoiceGender.MALE),
    TtsVoice("en-US-Neural2-I", "Ian (US, M)", "en-US", VoiceGender.MALE),
    TtsVoice("en-GB-Neural2-A", "Alice (UK, F)", "en-GB", VoiceGender.FEMALE),
    TtsVoice("en-GB-Neural2-B", "Ben (UK, M)", "en-GB", VoiceGender.MALE),
    TtsVoice("en-AU-Neural2-A", "Ava (AU, F)", "en-AU", VoiceGender.FEMALE),
    TtsVoice("en-IN-Neural2-A", "Ananya (IN, F)", "en-IN", VoiceGender.FEMALE),
)

val DEFAULT_VOICE = CURATED_VOICES.first()
