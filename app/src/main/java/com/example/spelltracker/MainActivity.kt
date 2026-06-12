package com.example.spelltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.spelltracker.ui.nav.AppNavigation
import com.example.spelltracker.ui.theme.SpellTrackerTheme

/**
 * Единственная Activity в приложении. Внутри — Compose-Nav с
 * тремя экранами (Home / Spells / SpellDetail).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpellTrackerTheme {
                AppNavigation()
            }
        }
    }
}
