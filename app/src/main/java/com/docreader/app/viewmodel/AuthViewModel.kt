package com.docreader.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docreader.app.data.repository.AuthRepository
import com.docreader.app.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(activityContext)
            result.fold(
                onSuccess = { signInResult ->
                    // Use the ID token as the bearer token for API calls.
                    // For production, exchange the server auth code for an access token.
                    // For personal use, the ID token works with Google APIs directly.
                    SessionManager.startSession(
                        token = signInResult.idToken,
                        displayName = signInResult.displayName,
                        email = signInResult.email
                    )
                    _authState.value = AuthState.Success
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Idle
    }
}
