package com.cleanly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(navController: NavHostController) {

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

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
        // LaunchedEffect para manejar la navegación según el estado del usuario
        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                // Si el usuario está autenticado, navegar a la pantalla de bienvenida
                navController.navigate("welcome") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                // Si no está autenticado, navegar a la pantalla de login
                navController.navigate("login") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }

        // Definición del NavHost, pasando el `navController` correctamente a cada pantalla
        NavHost(navController = navController, startDestination = "login") {
            // Pantalla de login
            composable("login") {
                LoginScreen(navController = navController)
            }
            // Pantalla de registro
            composable("register") {
                RegisterScreen(navController = navController)
            }
            // Pantalla de bienvenida
            composable("welcome") {
                WelcomeScreen(navController = navController)
            }
        }
    }
}



