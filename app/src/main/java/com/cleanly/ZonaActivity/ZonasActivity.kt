package com.cleanly.ZonaActivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.MainScreen
import com.cleanly.Welcome
import com.cleanly.ui.theme.CleanlyTheme

class ZonasActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CleanlyTheme {
                // Llama al composable Welcome y define la lógica de navegación
                Welcome()
            }
            }
        }
    }
