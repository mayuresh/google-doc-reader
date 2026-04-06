# DocReader

A Kindle-style reader for your Google Docs. Browse your Drive, open any document, and listen with natural-sounding voice narration — all in an incognito session that leaves no trace.

---

## Features

- **Google Sign-In** — secure OAuth2 login via Android Credential Manager
- **Drive browser** — navigate folders or search across all your docs
- **Kindle-style reader** — clean reading view with adjustable font size, font family, and background theme (light / sepia / dark)
- **Scroll or paginated mode** — switch anytime while reading
- **Voice reading** — powered by Google Cloud TTS Neural2 for natural-sounding narration
- **Playback controls** — play, pause, skip, speed control (0.5×–2×), curated voice selection
- **Sleep timer** — auto-logout after 15 / 30 / 45 / 60 min, or a custom duration (Audible-style)
- **Incognito session** — nothing is stored on-device; all data is cleared on sign-out, app close, or 5 minutes of inactivity

---

## Screenshots

*Coming soon*

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| Auth | Android Credential Manager + Google Identity |
| Drive & Docs | Google Drive API v3 / Google Docs API v1 (REST) |
| Voice | Google Cloud Text-to-Speech API — Neural2 voices |
| Audio playback | Media3 ExoPlayer (in-memory, no files written) |
| Networking | OkHttp + Retrofit |
| Min Android | 10 (API 29) |

---

## Setup

### 1. Clone the repo

```bash
git clone https://github.com/mayuresh/google-doc-reader.git
cd google-doc-reader
```

### 2. Google Cloud Console (one-time)

1. Go to [console.cloud.google.com](https://console.cloud.google.com) and create a project
2. Enable these APIs:
   - Google Drive API
   - Google Docs API
   - Cloud Text-to-Speech API
3. Create an **OAuth 2.0 Web Client ID** under Credentials
4. Register an **Android OAuth Client ID** using your app's SHA-1 fingerprint:
   ```bash
   keytool -keystore ~/.android/debug.keystore -list -v \
     -alias androiddebugkey -storepass android -keypass android
   ```
5. Create an **API Key** and restrict it to the Cloud Text-to-Speech API

### 3. Add secrets

Copy the example file and fill in your values:

```bash
cp app/src/main/res/values/secrets.xml.example \
   app/src/main/res/values/secrets.xml
```

Edit `secrets.xml`:
```xml
<string name="google_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
<string name="google_cloud_tts_api_key">YOUR_TTS_API_KEY</string>
```

`secrets.xml` is gitignored and will never be committed.

### 4. Open in Android Studio

Open the project root in Android Studio, let Gradle sync, then run on a device or emulator (API 29+).

---

## Project Structure

```
app/src/main/java/com/docreader/app/
├── session/          SessionManager — in-memory auth token, inactivity timer
├── data/
│   ├── model/        DriveItem, DocContent, TtsVoice, ReaderSettings
│   └── repository/   Auth, Drive, Docs repositories
├── tts/              TtsEngine interface + GoogleCloudTtsEngine
├── viewmodel/        AuthViewModel, DriveViewModel, ReaderViewModel, VoiceViewModel
├── ui/
│   ├── screens/      LoginScreen, DriveScreen, ReaderScreen
│   └── components/   VoiceControlsPanel, SleepTimerDialog, ReaderSettingsPanel
└── navigation/       NavGraph
```

---

## Privacy

This app is designed to be fully incognito:

- No local database, no SharedPreferences, no files written to disk
- Auth token is held in memory only and cleared immediately on sign-out
- Session auto-expires after 5 minutes of inactivity or when the sleep timer fires
- No reading history is retained between sessions

---

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Unit tests
./gradlew test
```

---

## Roadmap

- [ ] True paginated reading (measured page breaks)
- [ ] Full voice list browsing (all Neural2 voices)
- [ ] Background audio playback service
- [ ] Upgrade TTS engine path (ElevenLabs or Google Studio voices)

---

## License

Personal use only. Not distributed on the Play Store.
