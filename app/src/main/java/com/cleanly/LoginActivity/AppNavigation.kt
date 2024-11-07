package com.cleanly

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                signInWithGoogle = { /* Lógica para Google Sign-In */ },
                onLoginSuccess = {
                    val context = navController.context
                    val intent = Intent(context, TareaActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
    }
}
