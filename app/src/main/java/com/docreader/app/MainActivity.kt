package com.docreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.docreader.app.data.repository.AuthRepository
import com.docreader.app.data.repository.DocsRepository
import com.docreader.app.data.repository.DriveRepository
import com.docreader.app.navigation.NavGraph
import com.docreader.app.session.SessionManager
import com.docreader.app.tts.GoogleCloudTtsEngine
import com.docreader.app.ui.theme.DocReaderTheme
import com.docreader.app.viewmodel.AuthViewModel
import com.docreader.app.viewmodel.DriveViewModel
import com.docreader.app.viewmodel.ReaderViewModel
import com.docreader.app.viewmodel.VoiceViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Shared HTTP client for all API calls
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val ttsApiKey by lazy {
        getString(R.string.google_cloud_tts_api_key)
    }

    private val authViewModel by lazy {
        ViewModelProvider(this, viewModelFactory {
            AuthViewModel(AuthRepository(this))
        })[AuthViewModel::class.java]
    }

    private val driveViewModel by lazy {
        ViewModelProvider(this, viewModelFactory {
            DriveViewModel(DriveRepository(httpClient))
        })[DriveViewModel::class.java]
    }

    private val readerViewModel by lazy {
        ViewModelProvider(this, viewModelFactory {
            ReaderViewModel(DocsRepository(httpClient))
        })[ReaderViewModel::class.java]
    }

    private val voiceViewModel by lazy {
        ViewModelProvider(this, viewModelFactory {
            VoiceViewModel(
                ttsEngine = GoogleCloudTtsEngine(httpClient, ttsApiKey),
                onLogout = {
                    runOnUiThread {
                        authViewModel.signOut()
                        // Navigation to login is handled by SessionManager's logout callback
                    }
                }
            )
        })[VoiceViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register logout callback — navigates to login screen
        SessionManager.setLogoutCallback {
            runOnUiThread {
                authViewModel.signOut()
            }
        }

        setContent {
            DocReaderTheme {
                NavGraph(
                    authViewModel = authViewModel,
                    driveViewModel = driveViewModel,
                    readerViewModel = readerViewModel,
                    voiceViewModel = voiceViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SessionManager.onAppForeground()
    }

    override fun onPause() {
        super.onPause()
        SessionManager.onAppBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        SessionManager.logout()
    }
}

/** Minimal factory helper to avoid boilerplate. */
private inline fun <reified VM : ViewModel> viewModelFactory(
    crossinline create: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
