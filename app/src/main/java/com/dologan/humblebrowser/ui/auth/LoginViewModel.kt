package com.dologan.humblebrowser.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dologan.humblebrowser.data.prefs.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    /** One-shot event: emitted only when the user actively completes a login. */
    private val _loginComplete = MutableSharedFlow<Unit>()
    val loginComplete: SharedFlow<Unit> = _loginComplete.asSharedFlow()

    /** Whether we currently have a stored cookie (used by Navigation for startDestination). */
    fun hasStoredSession(): Boolean = authPreferences.isLoggedIn()

    private val _manualCookieText = MutableStateFlow("")
    val manualCookieText: StateFlow<String> = _manualCookieText.asStateFlow()

    fun onCookieExtracted(cookie: String) {
        authPreferences.setSessionCookie(cookie)
        viewModelScope.launch {
            _loginComplete.emit(Unit)
        }
    }

    fun onManualCookieChanged(text: String) {
        _manualCookieText.value = text
    }

    fun submitManualCookie() {
        val cookie = _manualCookieText.value.trim()
        if (cookie.isNotEmpty()) {
            onCookieExtracted(cookie)
        }
    }

    fun logout() {
        authPreferences.clear()
    }
}
