package com.cleanly

import android.content.Intent
import android.net.Uri
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
import com.cleanly.ProgramasActivity.programarTaskSync
import com.cleanly.ProgramasActivity.scheduleInitialReset
import com.cleanly.WelcomeActivity.WelcomeTopBar
import com.cleanly.WelcomeActivity.WelcomeDownBar
import com.cleanly.shared.Tarea
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MainScreen(
    onNavigateToTarea: ((Tarea) -> Unit)? = null,
    onNavigateToZonas: (() -> Unit)? = null,
    zonaSeleccionada: String? = null
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
    var isLoading by remember { mutableStateOf(true) }
    var navigationTriggered by remember { mutableStateOf(false) }
    val nombresUsuarios = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isAdmin by remember { mutableStateOf(false) }
    var workerProgramado by remember { mutableStateOf(false) }


    // Dentro de MainScreen
    val currentRoute = navController.currentBackStackEntry?.destination?.route

// Usar startsWith para manejar rutas con parámetros
    val isInGroupScreen = currentRoute?.startsWith("group_screen") == true

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
                                        val rol = userDoc.getString("rol")
                                        isAdmin = rol == "administrador"
                                        grupoDoc.reference.collection("usuarios").get()
                                            .addOnSuccessListener { usuariosSnapshot ->
                                                val nuevosNombres = mutableMapOf<String, String>()
                                                usuariosSnapshot.documents.forEach { usuarioDoc ->
                                                    val uid = usuarioDoc.id
                                                    val nombre = usuarioDoc.getString("nombre")
                                                    if (!uid.isNullOrEmpty() && !nombre.isNullOrEmpty()) {
                                                        nuevosNombres[uid] = nombre
                                                    }
                                                }
                                                nombresUsuarios.value = nuevosNombres.toMap()
                                            }
                                            .addOnFailureListener { exception ->
                                            }
                                    }
                                }
                                .addOnFailureListener { exception ->
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
                        grupoIdLoaded = true
                        isLoading = false
                    }
                } else {
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
            if (groupId == null || groupId == "singrupo") {
                Log.d("Navigation", "Redirigiendo a group_screen porque groupId es null o singrupo")
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

    LaunchedEffect(isAdmin, groupId) {
        groupId?.let { safeGroupId ->
            if (isAdmin && !workerProgramado) {
                scheduleInitialReset(context, isAdmin, safeGroupId)
                workerProgramado = true
            }
        }
    }

    LaunchedEffect(groupId) {
        groupId?.let { safeGroupId ->
            if (!workerProgramado) {
                programarTaskSync(context, safeGroupId)
                workerProgramado = true
            }
        }
    }
    // Interfaz principal si isLoading es false
    if (!isLoading) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        Scaffold(
            topBar = {
                groupId?.let { safeGroupId ->
                    if (groupId != null && currentRoute?.startsWith("group_screen") != true) {
                        WelcomeTopBar(
                            photoUrl = photoUrl,
                            displayName = displayName,
                            onProfileClick = { navController.navigate("profile") },
                            onGroupManagementClick = {
                                navController.navigate("group_management/$safeGroupId/${currentUser?.uid}")
                            },
                            onLogoutClick = {
                                auth.signOut()
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            },
            bottomBar = {
                groupId?.let { safeGroupId ->
                    if (groupId != null && currentRoute?.startsWith("group_screen") != true) {
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
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = if (groupId == "singrupo") "group_screen/{userId}" else "welcome",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("welcome") {
                    groupId?.let { safeGroupId ->
                        if (safeGroupId != "singrupo") {
                            Welcome(
                                navController = navController,
                                onTareaClick = { /* Aquí puedes manejar la lógica al hacer clic en una tarea */ },
                                groupId = safeGroupId,
                                nombresUsuarios = nombresUsuarios.value
                            )
                        } else {
                            navController.navigate("group_screen/${currentUser?.uid ?: ""}")
                        }
                    }
                }


                composable("group_screen/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    GroupScreen(navController = navController, userId = userId)
                }

                composable(
                    route = "group_management/{groupId}/{userId}",
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.StringType },
                        navArgument("userId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val groupIdParam = backStackEntry.arguments?.getString("groupId") ?: ""
                    val userIdParam = backStackEntry.arguments?.getString("userId") ?: ""

                    GroupManagementScreen(
                        navController = navController,
                        groupId = groupIdParam,
                        userId = userIdParam,
                        isAdmin = isAdmin,
                        onGroupLeft = {
                            groupId = null
                        }
                    )
                }

                composable("estadisticas") {
                    groupId?.let { safeGroupId ->
                        if (safeGroupId != "singrupo") {
                            EstadisticasScreen(
                                navController = navController,
                                groupId = safeGroupId,
                                nombresUsuarios = nombresUsuarios.value
                            )
                        } else {
                            navController.navigate("group_screen/${currentUser?.uid ?: ""}")
                        }
                    }
                }

                composable("programar") {
                    groupId?.let { safeGroupId ->
                        if (safeGroupId != "singrupo") {
                            ProgramarScreen(navController = navController, groupId = safeGroupId)
                        } else {
                            navController.navigate("group_screen/${currentUser?.uid ?: ""}")
                        }
                    }
                }

                composable("profile") {
                    val grupoId = ""
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

                composable("zonas") {
                    Zonas(
                        groupId = groupId.orEmpty(),
                        onZoneClick = { zoneName ->
                            val encodedZone = Uri.encode(zoneName)
                            val encodedGroupId = Uri.encode(groupId.orEmpty())
                            navController.navigate("tarea?zona=$encodedZone&groupId=$encodedGroupId")
                        }
                    )
                }

                composable(
                    route = "tarea?zona={zona}&groupId={groupId}",
                    arguments = listOf(
                        navArgument("zona") { type = NavType.StringType; defaultValue = "" },
                        navArgument("groupId") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val zona = backStackEntry.arguments?.getString("zona").orEmpty()
                    val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()

                    TareaScreen(
                        navController = navController,
                        zonaSeleccionada = zona,
                        groupId = groupId,
                        nombresUsuarios = nombresUsuarios.value,
                        isAdmin = isAdmin
                    )
                }
            }



        }
    }
}