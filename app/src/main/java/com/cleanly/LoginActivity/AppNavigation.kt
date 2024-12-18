package com.cleanly

import MisTareasScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cleanly.WelcomeActivity.GroupManagementScreen
import com.cleanly.WelcomeActivity.ProfileScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // Pantalla Splash
        composable("splash") {
            SplashScreen(navController = navController)
        }

        // Pantalla Login
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    // Asegúrate de que no se agregue otra instancia de Welcome
                    navController.navigate("welcome") {
                        // Limpia la pila de navegación y no queremos volver atrás a Login
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true // Asegúrate de no duplicar la pantalla
                    }
                }
            )
        }

        // Pantalla de Registro
        composable("register") {
            RegisterScreen(navController = navController)
        }

        // Pantalla Welcome
        composable("welcome") {
            Welcome(navController)
        }
        // Otras pantallas que quieras agregar pueden tener la barra de navegación también
        composable("mis_tareas") {
            MisTareasScreen(navController)  // Asegúrate de tener la barra aquí
        }

        composable("profile") {
            ProfileScreen(navController)
        }

        composable("group_management") {
            GroupManagementScreen(navController)
        }

    }
}

