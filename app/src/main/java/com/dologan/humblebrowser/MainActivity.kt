package com.dologan.humblebrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dologan.humblebrowser.ui.HumbleBrowserNavHost
import com.dologan.humblebrowser.ui.theme.HumbleBrowserTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HumbleBrowserTheme {
                HumbleBrowserNavHost()
            }
        }
    }
}
