package com.cleanly

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // SplashScreen como pantalla inicial
        composable("splash") {
            SplashScreen(navController = navController)
        }

        // Pantalla de login
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    // Cambiamos la navegación a Welcome en lugar de TareaActivity
                    navController.navigate("welcome") {
                        // Limpiamos la pila de navegación y no queremos volver atrás
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de registro
        composable("register") {
            RegisterScreen(navController = navController)
        }

        // Pantalla de Welcome (pantalla principal después del login)
        composable("welcome") {
            Welcome()
        }
    }
}
