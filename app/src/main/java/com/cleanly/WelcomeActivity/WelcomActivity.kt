package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.ui.theme.CleanlyTheme


class WelcomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar la interfaz de usuario
        setContent {
            CleanlyTheme {
                AppNavigation() // Llamar a tu NavHost desde aquí
            }
        }
    }
}