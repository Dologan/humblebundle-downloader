package com.dologan.humblebrowser.ui.auth

import androidx.lifecycle.ViewModel
import com.dologan.humblebrowser.data.prefs.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(authPreferences.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _showManualEntry = MutableStateFlow(false)
    val showManualEntry: StateFlow<Boolean> = _showManualEntry.asStateFlow()

    private val _manualCookieText = MutableStateFlow("")
    val manualCookieText: StateFlow<String> = _manualCookieText.asStateFlow()

    fun onCookieExtracted(cookie: String) {
        authPreferences.setSessionCookie(cookie)
        _isLoggedIn.value = true
    }

    fun toggleManualEntry() {
        _showManualEntry.value = !_showManualEntry.value
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
        _isLoggedIn.value = false
    }
}
