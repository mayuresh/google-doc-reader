package com.docreader.app.data.model

enum class VoiceGender { MALE, FEMALE }

data class TtsVoice(
    val name: String,           // e.g. "en-US-Neural2-A"
    val displayName: String,    // e.g. "Aria (US Female)"
    val languageCode: String,   // e.g. "en-US"
    val gender: VoiceGender
)

/** Curated list of high-quality Google Cloud TTS Neural2 voices. */
val CURATED_VOICES = listOf(
    TtsVoice("en-US-Neural2-A", "Aria (US, Female)", "en-US", VoiceGender.FEMALE),
    TtsVoice("en-US-Neural2-C", "Clara (US, Female)", "en-US", VoiceGender.FEMALE),
    TtsVoice("en-US-Neural2-D", "David (US, Male)", "en-US", VoiceGender.MALE),
    TtsVoice("en-US-Neural2-F", "Fiona (US, Female)", "en-US", VoiceGender.FEMALE),
    TtsVoice("en-US-Neural2-I", "Ian (US, Male)", "en-US", VoiceGender.MALE),
    TtsVoice("en-US-Neural2-J", "James (US, Male)", "en-US", VoiceGender.MALE),
    TtsVoice("en-GB-Neural2-A", "Alice (UK, Female)", "en-GB", VoiceGender.FEMALE),
    TtsVoice("en-GB-Neural2-B", "Ben (UK, Male)", "en-GB", VoiceGender.MALE),
    TtsVoice("en-GB-Neural2-D", "Diana (UK, Female)", "en-GB", VoiceGender.FEMALE),
    TtsVoice("en-AU-Neural2-A", "Ava (AU, Female)", "en-AU", VoiceGender.FEMALE),
    TtsVoice("en-AU-Neural2-B", "Blake (AU, Male)", "en-AU", VoiceGender.MALE),
    TtsVoice("en-IN-Neural2-A", "Ananya (IN, Female)", "en-IN", VoiceGender.FEMALE),
    TtsVoice("en-IN-Neural2-B", "Aryan (IN, Male)", "en-IN", VoiceGender.MALE),
)

val DEFAULT_VOICE = CURATED_VOICES.first()
