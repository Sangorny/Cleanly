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
                    val context = navController.context
                    val intent = Intent(context, TareaActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
        }

        // Pantalla de registro
        composable("register") {
            RegisterScreen(navController = navController)
        }
    }
}