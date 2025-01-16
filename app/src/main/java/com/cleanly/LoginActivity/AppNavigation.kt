package com.cleanly

import MisTareasScreen
import android.util.Log
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cleanly.PerfilActivity.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.cleanly.PerfilActivity.GroupManagementScreen
import com.cleanly.PerfilActivity.GroupScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // Pantalla Splash
        composable("splash") {
            SplashScreen(navController = navController)
        }


        composable("register") {
            RegisterScreen(navController = navController)
        }


        // Pantalla Login
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate("waiting_screen") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Pantalla Welcome
        composable("welcome") {
            Welcome(
                navController = navController,
                onTareaClick = { tarea ->
                    navController.navigate("mis_tareas") {
                        popUpTo("welcome") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Otras pantallas
        composable("mis_tareas") {
            MisTareasScreen(navController)
        }

        composable("profile") { navBackStackEntry ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val grupoId = remember { mutableStateOf<String?>(null) }

            LaunchedEffect(userId) {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("usuarios").document(userId)

                userRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        grupoId.value = document.getString("grupoId") ?: "default_group_id"
                    }
                }
            }

            if (grupoId.value != null) {
                ProfileScreen(
                    navController = navController,
                    grupoId = grupoId.value!!,
                    userId = userId,
                    onProfileUpdated = { updatedDisplayName, updatedPhotoUrl -> }
                )
            } else {
                CircularProgressIndicator()
            }
        }

        // Pantalla GroupScreen
        composable("group_screen/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            GroupScreen(
                navController = navController,
                userId = userId,
                showTopBarAndBottomBar = false // No mostrar las barras en GroupScreen
            )
        }

        composable("group_management/{grupoId}") { backStackEntry ->
            val grupoIdArg = backStackEntry.arguments?.getString("grupoId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            GroupManagementScreen(
                navController = navController,
                context = LocalContext.current,
                userId = userId,
                grupoId = grupoIdArg
            )
        }


    }
}

