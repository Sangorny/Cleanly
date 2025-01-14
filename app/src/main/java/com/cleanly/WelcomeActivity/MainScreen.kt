package com.cleanly

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cleanly.EstadisticaActivity.EstadisticasScreen
import com.cleanly.ProgramasActivity.ProgramarScreen
import com.cleanly.PerfilActivity.GroupManagementScreen
import com.cleanly.WelcomeActivity.ProfileScreen
import com.cleanly.WelcomeActivity.WelcomeTopBar
import com.cleanly.WelcomeActivity.WelcomeDownBar
import com.cleanly.shared.Tarea
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainScreen(
    onNavigateToTarea: (Tarea) -> Unit,
    onNavigateToZonas: () -> Unit,
    zonaSeleccionada: String? = null // Parámetro opcional
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Estados para manejar el nombre y foto del usuario
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "Usuario") }
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl) }

    // Actualiza el estado del usuario cuando se realizan cambios
    LaunchedEffect(currentUser) {
        currentUser?.reload()?.addOnCompleteListener {
            if (it.isSuccessful) {
                displayName = currentUser?.displayName ?: "Usuario"
                photoUrl = currentUser?.photoUrl
            }
        }
    }

    Scaffold(
        topBar = {
            WelcomeTopBar(
                photoUrl = photoUrl,
                displayName = displayName,
                onProfileClick = { navController.navigate("profile") },
                onGroupManagementClick = { navController.navigate("group_management") },
                onLogoutClick = {
                    auth.signOut()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }
            )
        },
        bottomBar = {
            WelcomeDownBar { selectedScreen ->
                when (selectedScreen) {
                    "Mis Tareas" -> navController.navigate("welcome")
                    "Zonas" -> navController.navigate("zonas")
                    "Estadísticas" -> navController.navigate("estadisticas")
                    "Programar" -> navController.navigate("programar")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (zonaSeleccionada != null) "tarea" else "welcome",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("welcome") {
                Welcome(
                    navController = navController,
                    onTareaClick = onNavigateToTarea
                )
            }
            composable("zonas") {
                Zonas { onNavigateToZonas() }
            }
            composable("group_management") {
                val userId = auth.currentUser?.uid ?: ""
                GroupManagementScreen(
                    navController = navController, // Si tu función requiere un controlador de navegación
                    userId = userId
                )
            }
            composable("profile") {
                val grupoId = "id_del_grupo_actual" // Obtén el grupo al que pertenece el usuario
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                ProfileScreen(
                    navController = navController,
                    grupoId = grupoId,
                    userId = userId,
                    onProfileUpdated = { updatedDisplayName, updatedPhotoUrl ->
                        displayName = updatedDisplayName
                        photoUrl = updatedPhotoUrl
                    }
                )
            }
            composable("estadisticas") {
                EstadisticasScreen(navController = navController)
            }
            composable("programar") {
                ProgramarScreen(navController = navController)
            }
            composable("tarea") {
                if (zonaSeleccionada != null) {
                    TareaScreen(navController = navController, zonaSeleccionada = zonaSeleccionada)
                }
            }
        }
    }
}
