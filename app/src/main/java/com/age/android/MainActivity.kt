package com.age.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.age.android.navigation.AppNavigation
import com.age.android.ui.theme.AgeAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgeAndroidTheme {
                AppNavigation()
            }
        }
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        // Share intent handling will be wired to specific screens in future
        if (intent?.action == Intent.ACTION_SEND) {
            // Files shared to the app will be handled by the active screen
        }
    }
}
