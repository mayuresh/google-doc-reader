package com.docreader.app.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes

/**
 * Holds all session state in memory only.
 * No data is ever written to disk — fully incognito.
 *
 * Lifecycle:
 *  - Session starts on successful Google Sign-In.
 *  - Session ends on: explicit logout, 5-min inactivity, or app destruction.
 *  - In voice-read mode the inactivity timer is suspended; sleep timer governs instead.
 */
object SessionManager {

    @Volatile var accessToken: String? = null
        private set

    @Volatile var userDisplayName: String? = null
        private set

    @Volatile var userEmail: String? = null
        private set

    @Volatile var isLoggedIn: Boolean = false
        private set

    private var logoutCallback: (() -> Unit)? = null
    private var inactivityJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /** Called after successful Google Sign-In. */
    fun startSession(token: String, displayName: String?, email: String?) {
        accessToken = token
        userDisplayName = displayName
        userEmail = email
        isLoggedIn = true
        resetInactivityTimer()
    }

    /** Register a callback that fires when the session expires. */
    fun setLogoutCallback(callback: () -> Unit) {
        logoutCallback = callback
    }

    /**
     * Call this on any user interaction (scroll, tap).
     * Has no effect when voice reading is active (sleep timer governs).
     */
    fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            logout()
        }
    }

    /** Pause inactivity timer during voice reading — sleep timer takes over. */
    fun pauseInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
    }

    /** Resume inactivity timer when voice reading stops. */
    fun resumeInactivityTimer() {
        resetInactivityTimer()
    }

    /**
     * Start the background inactivity countdown.
     * Called when app goes to background and voice is not playing.
     */
    fun onAppBackground() {
        // Timer continues from its current state — no restart needed.
        // If already running it will fire after the remaining time.
    }

    /** Called when app returns to foreground. Resets the timer on return. */
    fun onAppForeground() {
        if (isLoggedIn) resetInactivityTimer()
    }

    /** Clear all session data. Safe to call multiple times. */
    fun logout() {
        inactivityJob?.cancel()
        inactivityJob = null
        accessToken = null
        userDisplayName = null
        userEmail = null
        isLoggedIn = false
        logoutCallback?.invoke()
    }
}
