package com.docreader.app.viewmodel

import android.content.Context
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docreader.app.data.repository.AuthRepository
import com.docreader.app.data.repository.AuthorizationResult
import com.docreader.app.data.repository.SignInResult
import com.docreader.app.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object SigningIn : AuthState()
    // Sign-in succeeded; waiting for user to approve Drive scope consent screen
    data class NeedsConsent(val intentSender: IntentSender) : AuthState()
    data object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Held between step 1 and step 2 — not persisted anywhere
    private var pendingSignInResult: SignInResult? = null

    /**
     * Step 1: Launches Google account picker.
     * On success, immediately proceeds to Step 2 (authorize Drive scopes).
     */
    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.SigningIn

            val signInResult = authRepository.signIn(activityContext)
            signInResult.fold(
                onSuccess = { result ->
                    pendingSignInResult = result
                    authorizeForDrive(result)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(
                        if (e.message?.contains("cancel", ignoreCase = true) == true)
                            "Sign-in cancelled."
                        else
                            "Sign-in failed: ${e.message}"
                    )
                }
            )
        }
    }

    /**
     * Step 2: Request OAuth2 access token for Drive + Docs scopes.
     * If the user hasn't approved these scopes before, emits NeedsConsent so
     * the Activity can launch the consent screen intent.
     */
    private suspend fun authorizeForDrive(signInResult: SignInResult) {
        when (val authResult = authRepository.authorizeForDrive()) {
            is AuthorizationResult.Success -> {
                startSession(signInResult, authResult.accessToken)
            }
            is AuthorizationResult.ConsentRequired -> {
                // Activity will launch the consent screen; we wait for the result
                _authState.value = AuthState.NeedsConsent(authResult.intentSender)
            }
            is AuthorizationResult.Failure -> {
                _authState.value = AuthState.Error(
                    "Could not get Drive access: ${authResult.error.message}"
                )
            }
        }
    }

    /**
     * Called by the Activity after the user returns from the consent screen.
     * Extracts the access token and completes the sign-in.
     */
    fun handleConsentResult(activityResult: ActivityResult) {
        viewModelScope.launch {
            val tokenResult = authRepository.handleAuthorizationResult(activityResult)
            tokenResult.fold(
                onSuccess = { token ->
                    val signIn = pendingSignInResult
                    if (signIn != null) {
                        startSession(signIn, token)
                    } else {
                        _authState.value = AuthState.Error("Session data lost. Please sign in again.")
                    }
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error("Authorization failed: ${e.message}")
                }
            )
        }
    }

    private fun startSession(signInResult: SignInResult, accessToken: String) {
        SessionManager.startSession(
            token = accessToken,
            displayName = signInResult.displayName,
            email = signInResult.email
        )
        pendingSignInResult = null
        _authState.value = AuthState.Success
    }

    fun signOut() {
        authRepository.signOut()
        pendingSignInResult = null
        _authState.value = AuthState.Idle
    }
}
