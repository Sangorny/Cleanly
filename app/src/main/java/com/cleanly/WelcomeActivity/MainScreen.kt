package com.cleanly

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cleanly.EstadisticaActivity.EstadisticasScreen
import com.cleanly.ProgramasActivity.ProgramarScreen
import com.cleanly.PerfilActivity.GroupManagementScreen
import com.cleanly.PerfilActivity.GroupScreen
import com.cleanly.PerfilActivity.LoadingScreen
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
    var groupId by remember { mutableStateOf<String?>(null) }
    var grupoIdLoaded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) } // Controla la pantalla de carga
    var navigationTriggered by remember { mutableStateOf(false) }

    // Mostrar pantalla de carga mientras isLoading es true
    if (isLoading) {
        LoadingScreen(isLoading = isLoading) { /* No hace falta lógica extra aquí */ }
    }

    // Lógica para cargar grupos
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUser.reload()?.addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    val db = FirebaseFirestore.getInstance()
                    val gruposRef = db.collection("grupos")
                    var gruposProcesados = 0

                    gruposRef.get().addOnSuccessListener { querySnapshot ->
                        val totalGrupos = querySnapshot.size()

                        if (totalGrupos == 0) {
                            groupId = null
                            grupoIdLoaded = true
                            isLoading = false
                            Toast.makeText(
                                context,
                                "No perteneces a ningún grupo.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }

                        querySnapshot.documents.forEach { grupoDoc ->
                            val userDocRef =
                                grupoDoc.reference.collection("usuarios").document(currentUser.uid)
                            userDocRef.get()
                                .addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        groupId = grupoDoc.getString("id") ?: grupoDoc.id
                                        Log.d(
                                            "MainScreen",
                                            "Usuario encontrado en el grupo: $groupId"
                                        )
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(
                                        "MainScreen",
                                        "Error al buscar usuario en grupo: ${exception.message}"
                                    )
                                }
                                .addOnCompleteListener {
                                    gruposProcesados++
                                    if (gruposProcesados == totalGrupos) {
                                        grupoIdLoaded = true
                                        isLoading = false
                                    }
                                }
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("MainScreen", "Error al cargar grupos: ${exception.message}")
                        grupoIdLoaded = true
                        isLoading = false
                    }
                } else {
                    Log.e(
                        "MainScreen",
                        "Error al recargar usuario: ${reloadTask.exception?.message}"
                    )
                    grupoIdLoaded = true
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    // Lógica de navegación
    LaunchedEffect(grupoIdLoaded, navigationTriggered) {
        if (grupoIdLoaded && !navigationTriggered) {
            navigationTriggered = true
            if (groupId == null) {
                Log.d("Navigation", "Redirigiendo a group_screen porque groupId es null")
                currentUser?.let {
                    navController.navigate("group_screen/${it.uid}") {
                        popUpTo("main_screen") { inclusive = true }
                        launchSingleTop = true
                    }
                } ?: Log.e("Navigation", "Usuario no autenticado. No se puede redirigir.")
            } else {
                Log.d("Navigation", "Redirigiendo a welcome con groupId: $groupId")
                navController.navigate("welcome") {
                    popUpTo("main_screen") { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (navigationTriggered) {
            Log.d("Navigation", "Navegación ya fue activada. Evitando redirección repetida.")
        }
    }

    // Interfaz principal si isLoading es false
    if (!isLoading) {
        Scaffold(
            topBar = {
                if (groupId != null) {
                    WelcomeTopBar(
                        photoUrl = photoUrl,
                        displayName = displayName,
                        onProfileClick = { navController.navigate("profile") },
                        onGroupManagementClick = {
                            if (!groupId.isNullOrEmpty() && currentUser?.uid != null) {
                                navController.navigate("group_management/$groupId/${currentUser.uid}")
                            } else {
                                Toast.makeText(
                                    context,
                                    "Datos insuficientes para gestionar el grupo.",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                if (groupId != null) {
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
                    GroupScreen(
                        navController = navController,
                        userId = userId,
                        showTopBarAndBottomBar = false
                    )
                }

                // Ruta hacia GroupManagementScreen
                composable(
                    route = "group_management/{groupId}/{userId}",
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.StringType },
                        navArgument("userId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""

                    if (groupId.isNotEmpty() && userId.isNotEmpty()) {
                        GroupManagementScreen(
                            navController = navController,
                            groupId = groupId,
                            userId = userId
                        )
                    } else {
                        Log.e("NavHost", "groupId o userId están vacíos. Verifica la navegación.")
                        Toast.makeText(
                            context,
                            "Error al cargar la gestión del grupo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                composable("profile") {
                    val grupoId =
                        "id_del_grupo_actual" // Obtén el grupo al que pertenece el usuario
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
                        TareaScreen(
                            navController = navController,
                            zonaSeleccionada = zonaSeleccionada
                        )
                    }
                }
            }
        }
    }
}

