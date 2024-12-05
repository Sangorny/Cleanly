package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.ui.theme.CleanlyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar la interfaz de usuario con el tema y la navegación
        setContent {
            CleanlyTheme {
                AppNavigation()  // Usa AppNavigation que maneja todas las pantallas y navegación
            }
        }
    }
}


