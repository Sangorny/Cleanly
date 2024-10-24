package com.cleanly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(navController: NavHostController) {
    // Fondo degradado en toda la aplicación
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1), // Azul oscuro
                        Color(0xFF00E676)  // Verde
                    )
                )
            )
    ) {
        // Definición del NavHost, pasando el `navController` correctamente a cada pantalla
        NavHost(navController = navController, startDestination = "login") {
            // Pantalla de login
            composable("login") {
                LoginScreen(navController = navController)  // Pasando el navController
            }
            // Pantalla de registro
            composable("register") {
                RegisterScreen(navController = navController)  // Pasando el navController
            }
            // Pantalla de bienvenida
            composable("welcome") {
                WelcomeScreen(navController = navController)  // Pasando el navController
            }
        }
    }
}


