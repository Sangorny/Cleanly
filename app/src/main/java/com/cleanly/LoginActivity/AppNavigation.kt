package com.cleanly


import RegisterScreen
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cleanly.PerfilActivity.GroupScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ("splash")) {

        //Pantalla de Carga
        composable("splash") {
            SplashScreen(navController = navController)
        }

        //Pantalla de Registro
        composable("register") {
            RegisterScreen(navController = navController)
        }
        // Pantalla de Login
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate("main_screen?zonaSeleccionada=null") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        //Pantalla de MainScreen
        composable(
            route = "main_screen?zonaSeleccionada={zonaSeleccionada}",
            arguments = listOf(
                navArgument("zonaSeleccionada") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val zonaSeleccionada = backStackEntry.arguments?.getString("zonaSeleccionada")
            MainScreen(
                onNavigateToTarea = null,
                onNavigateToZonas = null,
                zonaSeleccionada = zonaSeleccionada
            )
        }
        // Pantalla de Usuario
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
            if (userId.isNotEmpty()) {
                GroupScreen(navController = navController, userId = userId)
            } else {
                Log.e("NavHost", "userId no está disponible.")
            }
        }


    }
}