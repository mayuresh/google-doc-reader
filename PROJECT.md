# PROJECT.md — DocReader Android App
# ========================================
# PURPOSE OF THIS FILE:
# This file contains everything specific to THIS project.
# It is read by Claude Code at the start of every session to establish context.
# Claude has no memory between sessions — this file IS the memory.
# ========================================


---


# SECTION P1 — Project identity

Project name:    DocReader
Owner / company: Personal (Mayuresh)
Description:     Android app that lets you read Google Docs in a Kindle-style reader
                 interface. Supports folder browsing, search, adjustable typography,
                 pagination or scroll mode, and Google Cloud TTS Neural2 voice reading
                 with playback controls and a sleep timer. Fully incognito — no session
                 persistence, no reading history.

Primary users:   Personal use only (single user)

Project started: 2026-04-06
Current status:  Active development — initial scaffolding


---


# SECTION P2 — Technology stack

Language:        Kotlin
Framework:       Android (native)
UI:              Jetpack Compose + Material3
Min SDK:         29 (Android 10)
Target SDK:      35 (Android 15)
Compile SDK:     35

Key libraries:
  - Jetpack Compose BOM 2024.10.00
  - Navigation Compose 2.8.3
  - ViewModel + StateFlow (lifecycle-viewmodel-compose 2.8.6)
  - Kotlin Coroutines 1.8.1
  - Credential Manager (androidx.credentials 1.3.0) — Google Sign-In
  - Google Identity 1.1.1 — modern Google Sign-In
  - Retrofit 2.11.0 + OkHttp 4.12.0 — REST API calls
  - Gson (converter-gson) — JSON parsing
  - Media3 ExoPlayer 1.4.1 — TTS audio playback

External APIs:
  - Google Drive API v3 (REST) — list files and folders
  - Google Docs API v1 (REST) — fetch document content
  - Google Cloud Text-to-Speech API (REST) — Neural2 voice synthesis

Authentication:
  - Google OAuth2 via Credential Manager
  - Token stored IN MEMORY ONLY — no SharedPreferences, no database
  - Session expires on: app close, 5-min inactivity (foreground), background (if voice not playing)

No local database — fully incognito, zero persistence


---


# SECTION P3 — Key commands

Install dependencies: (managed by Gradle — no manual step)
Build debug APK:      ./gradlew assembleDebug
Build release APK:    ./gradlew assembleRelease
Install on device:    ./gradlew installDebug
Run unit tests:       ./gradlew test
Run instrumented:     ./gradlew connectedAndroidTest
Lint:                 ./gradlew lint
Clean:                ./gradlew clean

On Windows use:       gradlew.bat <task>  (or .\gradlew <task> in PowerShell)


---


# SECTION P4 — Repository and issue tracking

GitHub repo URL:  https://github.com/mayuresh/google-doc-reader
Main branch:      main
Issue tracker:    GitHub Issues (disabled until repo is created — use P9 log)

Branch naming convention:
  feature/<issue-number>-<short-description>
  fix/<issue-number>-<short-description>
  docs/<issue-number>-<short-description>
  refactor/<issue-number>-<short-description>


---


# SECTION P5 — Environment variables and secrets

All secrets are stored in app/google-services.json and local.properties — never committed.

Required setup (one-time, done in Google Cloud Console):
  GOOGLE_OAUTH_CLIENT_ID        — Web client ID from Google Cloud Console OAuth credentials
  GOOGLE_CLOUD_TTS_API_KEY      — API key with Cloud Text-to-Speech API enabled

These values go into:
  app/src/main/res/values/secrets.xml   (gitignored)
  local.properties                       (gitignored)

File app/src/main/res/values/secrets.xml.example is committed with placeholder values.

Note: No Firebase needed. Google Sign-In uses Credential Manager with a Web OAuth client ID.
SHA-1 of debug keystore must be registered in Google Cloud Console.


---


# SECTION P6 — Documentation

| File                      | Status  | Last updated  |
|---------------------------|---------|---------------|
| docs/index.html           | NEEDED  | n/a           |
| docs/requirements.html    | NEEDED  | n/a           |
| docs/architecture.html    | NEEDED  | n/a           |
| docs/changelog.html       | NEEDED  | n/a           |
| docs/runbook.html         | NEEDED  | n/a           |


---


# SECTION P7 — Architecture overview

UI Layer (Jetpack Compose screens):
  LoginScreen → DriveScreen → ReaderScreen
  Each screen has a corresponding ViewModel.

Navigation:
  NavGraph.kt manages navigation between screens.
  Login → Drive (after auth) → Reader (after selecting a doc).
  Back from Reader returns to Drive. Back from Drive logs out.

Session Layer:
  SessionManager (singleton object) holds the auth token and Google account info
  in memory. Manages inactivity timer (5 min). Clears all state on logout.
  All repositories depend on SessionManager for the current access token.

Data Layer:
  AuthRepository    — handles Google Sign-In flow, token retrieval
  DriveRepository   — lists files/folders in Google Drive (no shared docs)
  DocsRepository    — fetches and parses Google Doc content into structured model
  TtsRepository     — calls Google Cloud TTS REST API, returns audio bytes

TTS Layer:
  TtsEngine (interface) — abstraction for future engine swap
  GoogleCloudTtsEngine  — implementation using Google Cloud TTS Neural2 REST API
  Text is chunked into ~4000 char segments (sentence-aware) before API calls.
  Audio is played via Media3 ExoPlayer from memory (no files written to disk).

Models:
  DriveItem     — file or folder in Drive (id, name, mimeType, isFolder)
  DocContent    — parsed Google Doc (title, list of DocBlock: heading/paragraph/list)
  TtsVoice      — voice name, language, gender (curated list of Neural2 voices)
  ReaderSettings— font size, font type, background theme, pagination mode

No Room database. No SharedPreferences. Everything in memory.


---


# SECTION P8 — Known constraints and quirks

- Google Sign-In uses Credential Manager (new API, not deprecated play-services-auth).
  Requires a Web OAuth 2.0 client ID (not Android client ID) for server auth code flow.
  SHA-1 of the debug keystore must be registered in Google Cloud Console.

- Google Cloud TTS Neural2 has a 5000 byte (~4000 char) limit per API call.
  Long documents must be chunked. Chunking is done at sentence boundaries.

- TTS audio is streamed from memory using ExoPlayer's ByteArrayDataSource.
  No audio files are written to disk (incognito requirement).

- The 5-minute inactivity timer resets on ANY user interaction (scroll, tap, etc.)
  It starts when the app goes to background (if voice is not playing).
  In voice read mode: sleep timer governs logout, not the 5-min inactivity timer.

- Google Drive API: only shows docs owned by the signed-in user (not shared).
  Query filter: 'me' in owners AND mimeType = 'application/vnd.google-apps.document'

- Pagination mode and scroll mode can be toggled mid-document.
  Reading position is preserved on toggle (scroll offset to page number mapping).

- Debug keystore SHA-1 command:
  keytool -keystore ~/.android/debug.keystore -list -v -alias androiddebugkey -storepass android -keypass android


---


# SECTION P9 — Work log (used in place of GitHub Issues until repo is created)

[2026-04-06 00:00] Initial project setup — scaffold Android project structure,
                   build files, core architecture, all screens and ViewModels,
                   TTS engine, session management, Drive/Docs/TTS repositories.


---


# END OF PROJECT.md
# Last reviewed: 2026-04-06
