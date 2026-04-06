package com.docreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.docreader.app.data.repository.AuthRepository
import com.docreader.app.data.repository.DocsRepository
import com.docreader.app.data.repository.DriveRepository
import com.docreader.app.navigation.NavGraph
import com.docreader.app.session.SessionManager
import com.docreader.app.tts.GoogleCloudTtsEngine
import com.docreader.app.ui.theme.DocReaderTheme
import com.docreader.app.viewmodel.AuthState
import com.docreader.app.viewmodel.AuthViewModel
import com.docreader.app.viewmodel.DriveViewModel
import com.docreader.app.viewmodel.ReaderViewModel
import com.docreader.app.viewmodel.VoiceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val ttsApiKey by lazy { getString(R.string.google_cloud_tts_api_key) }

    private val authViewModel by lazy {
        ViewModelProvider(this, simpleFactory { AuthViewModel(AuthRepository(this)) })[AuthViewModel::class.java]
    }

    private val driveViewModel by lazy {
        ViewModelProvider(this, simpleFactory { DriveViewModel(DriveRepository(httpClient)) })[DriveViewModel::class.java]
    }

    private val readerViewModel by lazy {
        ViewModelProvider(this, simpleFactory { ReaderViewModel(DocsRepository(httpClient)) })[ReaderViewModel::class.java]
    }

    private val voiceViewModel by lazy {
        ViewModelProvider(this, simpleFactory {
            VoiceViewModel(
                ttsEngine = GoogleCloudTtsEngine(httpClient, ttsApiKey),
                onLogout = { runOnUiThread { authViewModel.signOut() } }
            )
        })[VoiceViewModel::class.java]
    }

    /**
     * Launched when the Google consent screen returns.
     * Passes the result back to AuthViewModel to complete sign-in.
     */
    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        authViewModel.handleConsentResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Watch for NeedsConsent state and launch the consent screen
        lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                if (state is AuthState.NeedsConsent) {
                    consentLauncher.launch(
                        IntentSenderRequest.Builder(state.intentSender).build()
                    )
                }
            }
        }

        // When session expires, sign out (navigates back to login via NavGraph)
        SessionManager.setLogoutCallback {
            runOnUiThread { authViewModel.signOut() }
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

private inline fun <reified VM : ViewModel> simpleFactory(
    crossinline create: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
