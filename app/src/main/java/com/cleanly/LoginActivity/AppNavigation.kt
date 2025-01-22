package com.cleanly

import MisTareasScreen
import android.util.Log
import android.widget.Toast
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
import com.cleanly.PerfilActivity.GroupManagementScreen
import com.cleanly.PerfilActivity.GroupScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ("splash")) {


        // Pantalla Splash
        composable("splash") {
            SplashScreen(navController = navController)
        }


        composable("register") {
            RegisterScreen(navController = navController)
        }

        composable("check_authentication") {
            CheckAuthentication(navController = navController)
        }

        // Pantalla Login
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

        composable(
            route = "main_screen?zonaSeleccionada={zonaSeleccionada}",
            arguments = listOf(
                navArgument("zonaSeleccionada") {
                    type = NavType.StringType
                    nullable = true // Permitir que sea null
                    defaultValue = null // Valor por defecto null
                }
            )
        ) { backStackEntry ->
            val zonaSeleccionada = backStackEntry.arguments?.getString("zonaSeleccionada")
            MainScreen(
                onNavigateToTarea = null, // Pasar null si no se necesita callback
                onNavigateToZonas = null, // Pasar null si no se necesita callback
                zonaSeleccionada = zonaSeleccionada
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
            if (userId.isNotEmpty()) {
                GroupScreen(navController = navController, userId = userId)
            } else {
                Log.e("NavHost", "userId no está disponible.")
            }
        }

        composable("group_management/{grupoId}") { backStackEntry ->
            val grupoIdArg = backStackEntry.arguments?.getString("groupId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            GroupManagementScreen(
                navController = navController,
                userId = userId,
                groupId = grupoIdArg
            )
        }

        /*// Navegación para Zonas
        composable(
            route = "zonas/{groupId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""

            if (groupId.isNotEmpty()) {
                Zonas(groupId = groupId) { zoneName ->
                    navController.navigate("tarea?zona=$zoneName&groupId=$groupId")
                }
            } else {
                Log.e("NavHost", "groupId está vacío. Verifica la navegación.")
                Toast.makeText(
                    LocalContext.current,
                    "Error al cargar las zonas: groupId no encontrado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }*/

       /* // Navegación para TareaScreen
        composable(
            route = "tarea?zona={zona}&groupId={groupId}",
            arguments = listOf(
                navArgument("zona") { type = NavType.StringType; defaultValue = "" },
                navArgument("groupId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val zona = backStackEntry.arguments?.getString("zona") ?: ""
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""

            TareaScreen(
                navController = navController,
                zonaSeleccionada = zona,
                groupId = groupId // Pasar el groupId
            )
        }*/

    }
}

