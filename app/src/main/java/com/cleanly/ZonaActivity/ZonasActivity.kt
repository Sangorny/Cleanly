package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.ui.theme.CleanlyTheme

class ZonasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanlyTheme {
                Zonas(
                    onZoneClick = { zoneName ->
                        // Manejar clics en una zona aquí
                    }
                )
            }
        }
    }
}
