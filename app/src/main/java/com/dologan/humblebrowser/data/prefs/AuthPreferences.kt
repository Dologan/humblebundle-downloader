package com.dologan.humblebrowser.data.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthPreferences(private val prefs: SharedPreferences) {

    private val _sessionCookie = MutableStateFlow(prefs.getString(KEY_SESSION_COOKIE, null))
    val sessionCookie: StateFlow<String?> = _sessionCookie.asStateFlow()

    fun getSessionCookie(): String? = prefs.getString(KEY_SESSION_COOKIE, null)

    fun setSessionCookie(cookie: String?) {
        prefs.edit().putString(KEY_SESSION_COOKIE, cookie).apply()
        _sessionCookie.value = cookie
    }

    fun isLoggedIn(): Boolean = getSessionCookie() != null

    fun clear() {
        prefs.edit().clear().apply()
        _sessionCookie.value = null
    }

    companion object {
        private const val KEY_SESSION_COOKIE = "session_cookie"
    }
}
