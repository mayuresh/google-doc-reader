package com.docreader.app.data.repository

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.docreader.app.R
import com.docreader.app.session.SessionManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.tasks.await

/**
 * Handles the two-step Google authentication flow:
 *
 * Step 1 — Identity (Credential Manager)
 *   Proves who the user is. Returns a Google ID token and the user's display name/email.
 *
 * Step 2 — Authorization (Google Identity AuthorizationClient)
 *   Requests OAuth2 access token for Drive and Docs read-only scopes.
 *   May require the user to approve a consent screen (returns a PendingIntent if so).
 *   This access token is what we use in Authorization: Bearer headers.
 *
 * Why two steps? Credential Manager handles identity; scope-based access tokens
 * require the separate AuthorizationClient. Both are needed for Drive/Docs access.
 */
class AuthRepository(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    // Drive and Docs read-only OAuth2 scopes (PRD P4.1.2)
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.readonly")
    private val docsScope = Scope("https://www.googleapis.com/auth/documents.readonly")

    /**
     * Step 1: Sign in with Google via Credential Manager.
     * Returns identity info (display name, email) and the ID token.
     * Call [authorizeForDrive] next to get the access token.
     */
    suspend fun signIn(activityContext: Context): Result<SignInResult> {
        val webClientId = context.getString(R.string.google_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // show all accounts, not just previously used
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
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            Result.success(
                SignInResult(
                    idToken = credential.idToken,
                    displayName = credential.displayName,
                    email = credential.id
                )
            )
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Step 2: Request OAuth2 access token for Drive + Docs scopes.
     *
     * Returns [AuthorizationResult]:
     *  - [AuthorizationResult.Success] with the access token if already authorised
     *  - [AuthorizationResult.ConsentRequired] with a PendingIntent if the user
     *    must approve a consent screen (launch the intent and call [handleAuthorizationResult])
     */
    suspend fun authorizeForDrive(): AuthorizationResult {
        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(driveScope, docsScope))
            .build()

        return try {
            val authResult = Identity.getAuthorizationClient(context)
                .authorize(authRequest)
                .await()

            if (authResult.hasResolution()) {
                // User needs to approve the consent screen
                AuthorizationResult.ConsentRequired(authResult.pendingIntent!!.intentSender)
            } else {
                val token = authResult.accessToken
                    ?: return AuthorizationResult.Failure(RuntimeException("No access token returned"))
                AuthorizationResult.Success(token)
            }
        } catch (e: Exception) {
            AuthorizationResult.Failure(e)
        }
    }

    /**
     * Call this after the consent screen activity returns.
     * Extracts the access token from the authorization result intent.
     */
    fun handleAuthorizationResult(activityResult: ActivityResult): Result<String> {
        return try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(activityResult.data ?: Intent())
            val token = authResult.accessToken
                ?: return Result.failure(RuntimeException("No access token in result"))
            Result.success(token)
        } catch (e: Exception) {
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

sealed class AuthorizationResult {
    data class Success(val accessToken: String) : AuthorizationResult()
    data class ConsentRequired(val intentSender: android.content.IntentSender) : AuthorizationResult()
    data class Failure(val error: Throwable) : AuthorizationResult()
}
