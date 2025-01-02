package com.cleanly.ProgramasActivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxSize


class ProgramarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController() // Crea un NavController para la navegación interna
            ProgramarScreen(navController = navController)
        }
    }
}