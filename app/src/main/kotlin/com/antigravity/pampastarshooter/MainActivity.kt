package com.antigravity.pampastarshooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.antigravity.pampastarshooter.ui.PampaStarShooterApp
import com.antigravity.pampastarshooter.ui.theme.PampaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val container = (application as PampaApplication).container
        setContent {
            PampaTheme {
                PampaStarShooterApp(container = container)
            }
        }
    }
}

