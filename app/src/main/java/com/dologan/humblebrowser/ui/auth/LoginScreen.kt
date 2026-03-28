package com.dologan.humblebrowser.ui.auth

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val showManualEntry by viewModel.showManualEntry.collectAsState()
    val manualCookieText by viewModel.manualCookieText.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    // Navigate only from a side-effect, never directly in the composition body
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to Humble Bundle") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!showManualEntry) {
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
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?,
                                ) {
                                    isLoading = true
                                    checkForSessionCookie(url, viewModel)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    checkForSessionCookie(url, viewModel)
                                }
                            }

                            loadUrl("https://www.humblebundle.com/login")
                        }
                    },
                )
            }

            // Manual cookie entry section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.toggleManualEntry() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (showManualEntry) "Use WebView Login" else "Advanced: Paste Cookie Manually")
                }

                AnimatedVisibility(visible = showManualEntry) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Copy the _simpleauth_sess cookie value from your browser and paste it below.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "In Chrome/Firefox: open humblebundle.com, then DevTools → Application → Cookies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualCookieText,
                            onValueChange = { viewModel.onManualCookieChanged(it) },
                            label = { Text("_simpleauth_sess value") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.submitManualCookie() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = manualCookieText.isNotBlank(),
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }
        }
    }
}

private fun checkForSessionCookie(url: String?, viewModel: LoginViewModel) {
    val cookies = CookieManager.getInstance().getCookie(url ?: return) ?: return
    val sessionCookie = cookies.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("_simpleauth_sess=") }
        ?.substringAfter("_simpleauth_sess=")

    if (!sessionCookie.isNullOrBlank()) {
        viewModel.onCookieExtracted(sessionCookie)
    }
}
