package com.docreader.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.docreader.app.R
import com.docreader.app.session.SessionManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class AuthRepository(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Launches Google Sign-In via Credential Manager.
     * Returns the OAuth access token on success.
     *
     * Note: Credential Manager returns an ID token. To call Drive/Docs APIs we need
     * an access token. The caller (AuthViewModel) handles the server-side auth code
     * exchange if needed, or uses the ID token to authenticate with Google APIs
     * via the Authorization header.
     *
     * For Drive/Docs REST API calls we use the access token from the Google Sign-In
     * result. The Credential Manager flow with requestServerAuthCode returns a code
     * that must be exchanged for an access token at your backend. For a personal-use
     * app with no backend, we use GoogleAuthUtil to get the token directly on-device.
     */
    suspend fun signIn(activityContext: Context): Result<SignInResult> {
        val webClientId = context.getString(R.string.google_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken
            val displayName = googleIdTokenCredential.displayName
            val email = googleIdTokenCredential.id

            Result.success(SignInResult(idToken = idToken, displayName = displayName, email = email))
        } catch (e: GetCredentialException) {
            Result.failure(e)
        }
    }

    fun signOut() {
        SessionManager.logout()
    }
}

data class SignInResult(
    val idToken: String,
    val displayName: String?,
    val email: String?
)
