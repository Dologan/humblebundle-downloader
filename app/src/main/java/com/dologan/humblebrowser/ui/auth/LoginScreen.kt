package com.dologan.humblebrowser.ui.auth

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import java.net.URLDecoder

/** Which login method the user has chosen. */
private enum class LoginMethod { CHOOSE, WEBVIEW, MANUAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val manualCookieText by viewModel.manualCookieText.collectAsState()
    var loginMethod by remember { mutableStateOf(LoginMethod.CHOOSE) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onLoginSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to Humble Bundle") },
            )
        },
    ) { padding ->
        when (loginMethod) {
            LoginMethod.CHOOSE -> {
                // ── Method picker ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        text = "Choose how to connect your account",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: WebView
                    Button(
                        onClick = { loginMethod = LoginMethod.WEBVIEW },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Sign in via Browser")
                    }
                    Text(
                        text = "Opens the Humble Bundle login page inside the app. Supports SSO and 2FA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 2: Manual cookie
                    OutlinedButton(
                        onClick = { loginMethod = LoginMethod.MANUAL },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Paste Session Cookie")
                    }
                    Text(
                        text = "If the browser login doesn't work, you can copy your session cookie from a desktop browser and paste it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            LoginMethod.WEBVIEW -> {
                // ── WebView login ─────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                CookieManager.getInstance().apply {
                                    setAcceptCookie(true)
                                    removeAllCookies(null)
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // Only check for the cookie once the user has left the
                                        // login / signup pages (i.e. after a successful login).
                                        if (url != null && !isLoginPage(url)) {
                                            extractSessionCookie(url, viewModel)
                                        }
                                    }
                                }

                                loadUrl("https://www.humblebundle.com/login")
                            }
                        },
                    )

                    OutlinedButton(
                        onClick = { loginMethod = LoginMethod.CHOOSE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text("Back to login options")
                    }
                }
            }

            LoginMethod.MANUAL -> {
                // ── Manual cookie entry ───────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Paste your session cookie",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "1. Open humblebundle.com in a desktop browser and log in.\n" +
                            "2. Open DevTools (F12) → Application → Cookies → humblebundle.com\n" +
                            "3. Find the _simpleauth_sess cookie and copy its Value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = manualCookieText,
                        onValueChange = { viewModel.onManualCookieChanged(it) },
                        label = { Text("_simpleauth_sess value") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 3,
                    )
                    Button(
                        onClick = { viewModel.submitManualCookie() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualCookieText.isNotBlank(),
                    ) {
                        Text("Sign In")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { loginMethod = LoginMethod.CHOOSE },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Back to login options")
                    }
                }
            }
        }
    }
}

/** Returns true if [url] is a HB login / signup page (cookie is not yet valid). */
private fun isLoginPage(url: String): Boolean {
    val path = Uri.parse(url).path?.lowercase() ?: return false
    return path.startsWith("/login") || path.startsWith("/signup") || path.startsWith("/processlogin")
}

/**
 * Extracts _simpleauth_sess from CookieManager for the given URL.
 * URL-decodes the value because CookieManager may percent-encode it.
 */
private fun extractSessionCookie(url: String, viewModel: LoginViewModel) {
    val cookies = CookieManager.getInstance().getCookie(url) ?: return
    val raw = cookies.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("_simpleauth_sess=") }
        ?.substringAfter("_simpleauth_sess=")
        ?: return

    if (raw.isBlank()) return

    // CookieManager sometimes returns a URL-encoded value
    val decoded = try {
        URLDecoder.decode(raw, "UTF-8")
    } catch (_: Exception) {
        raw
    }

    viewModel.onCookieExtracted(decoded)
}
