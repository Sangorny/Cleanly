package com.cleanly

import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import com.cleanly.PerfilActivity.GroupScreen
import com.cleanly.PerfilActivity.ProfileScreen
import com.cleanly.WelcomeActivity.WelcomeTopBar
import com.cleanly.WelcomeActivity.WelcomeDownBar
import com.cleanly.shared.Tarea
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    val userId = currentUser?.uid ?: ""
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "Usuario") }
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl) }
    var grupoId by remember { mutableStateOf<String?>(null) }
    var grupoIdLoaded by remember { mutableStateOf(false) }
    var navigationTriggered by remember { mutableStateOf(false) }


    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUser.reload()?.addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    val db = FirebaseFirestore.getInstance()
                    val gruposRef = db.collection("grupos")
                    var gruposProcesados = 0 // Contador para rastrear documentos procesados

                    gruposRef.get().addOnSuccessListener { querySnapshot ->
                        val totalGrupos = querySnapshot.size() // Total de documentos en la colección

                        if (totalGrupos == 0) {
                            // Si no hay grupos, marcar como cargado
                            grupoId = null
                            grupoIdLoaded = true
                            Toast.makeText(context, "No perteneces a ningún grupo.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        querySnapshot.documents.forEach { grupoDoc ->
                            val userDocRef = grupoDoc.reference.collection("usuarios").document(currentUser.uid)
                            userDocRef.get()
                                .addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        grupoId = grupoDoc.getString("id") ?: grupoDoc.id
                                        Log.d("MainScreen", "Usuario encontrado en el grupo: $grupoId")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("MainScreen", "Error al buscar usuario en grupo: ${exception.message}")
                                }
                                .addOnCompleteListener {
                                    gruposProcesados++
                                    if (gruposProcesados == totalGrupos) {
                                        // Marcar como cargado solo cuando todos los grupos hayan sido procesados
                                        grupoIdLoaded = true
                                        if (grupoId == null) {
                                            Toast.makeText(context, "No perteneces a ningún grupo.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("MainScreen", "Error al cargar grupos: ${exception.message}")
                        grupoIdLoaded = true
                    }
                } else {
                    Log.e("MainScreen", "Error al recargar usuario: ${reloadTask.exception?.message}")
                    grupoIdLoaded = true
                }
            }
        }
    }

    LaunchedEffect(grupoIdLoaded, navigationTriggered) {
        if (grupoIdLoaded && !navigationTriggered) {
            if (grupoId == null) {
                Log.d("Navigation", "Redirigiendo a group_screen porque grupoId es null")
                navController.navigate("group_screen/${currentUser?.uid}") {
                    popUpTo("main_screen") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                Log.d("Navigation", "Redirigiendo a welcome con grupoId: $grupoId")
                navController.navigate("welcome") {
                    popUpTo("main_screen") { inclusive = true }
                    launchSingleTop = true
                }
            }
            navigationTriggered = true
        }
    }

    Scaffold(
        topBar = {
            if (grupoId != null) {
                WelcomeTopBar(
                    photoUrl = photoUrl,
                    displayName = displayName,
                    onProfileClick = { navController.navigate("profile") },
                    onGroupManagementClick = {
                        // Si el usuario tiene un grupo, va a la pantalla de gestión del grupo
                        if (grupoId != null) {
                            navController.navigate("group_management/$grupoId")
                        }
                    },
                    onLogoutClick = {
                        auth.signOut()
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
        },
        bottomBar = {
            if (grupoId != null) {
                WelcomeDownBar { selectedScreen ->
                    when (selectedScreen) {
                        "Mis Tareas" -> navController.navigate("welcome")
                        "Zonas" -> navController.navigate("zonas")
                        "Estadísticas" -> navController.navigate("estadisticas")
                        "Programar" -> navController.navigate("programar")
                    }
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

            composable("group_screen/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                GroupScreen(navController = navController, userId = userId, showTopBarAndBottomBar = false)
            }

            // Ruta hacia GroupManagementScreen
            composable("group_management/{grupoId}") { backStackEntry ->
                val grupoIdArg = backStackEntry.arguments?.getString("grupoId") ?: ""
                GroupManagementScreen(
                    navController = navController,
                    context = LocalContext.current,
                    userId = userId,
                    grupoId = grupoIdArg
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