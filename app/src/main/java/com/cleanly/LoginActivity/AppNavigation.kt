package com.cleanly

import MisTareasScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cleanly.WelcomeActivity.GroupManagementScreen
import com.cleanly.WelcomeActivity.ProfileScreen
import com.google.firebase.auth.FirebaseAuth


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
            Welcome(
                navController = navController,
                onTareaClick = { tarea ->
                    // Lógica para manejar el clic en una tarea
                    navController.navigate("mis_tareas") {
                        popUpTo("welcome") {
                            inclusive = false
                        } // Navegar sin eliminar "welcome" de la pila
                        launchSingleTop = true // Evitar duplicados
                    }
                }
            )
        }

        // Otras pantallas que quieras agregar pueden tener la barra de navegación también
        composable("mis_tareas") {
            MisTareasScreen(navController)  // Asegúrate de tener la barra aquí
        }

        // Pantalla Profile con el callback onProfileUpdated
        composable("profile") {
            ProfileScreen(
                navController = navController,
                onProfileUpdated = { updatedDisplayName, updatedPhotoUrl ->
                    // Aquí puedes manejar cómo se actualizan los datos en MainScreen o AppNavigation
                }
            )
        }

        // Pantalla de gestión de grupos
        composable("group_management") {
            val context = LocalContext.current
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: ""
            GroupManagementScreen(context = context, userId = userId)
        }
    }
}