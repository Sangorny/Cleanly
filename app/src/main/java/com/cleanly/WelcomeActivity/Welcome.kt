package com.cleanly

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.material.TopAppBar
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.cleanly.WelcomeActivity.GroupManagementActivity
import com.cleanly.WelcomeActivity.GroupManagementScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cleanly.shared.Tarea
import com.cleanly.ZonaActivity.ZonasActivity
import com.cleanly.WelcomeActivity.ProfileScreen
import com.cleanly.WelcomeActivity.WelcomeBarra
import com.cleanly.shared.welcomeBD


@Composable
fun Welcome(
    navController: NavHostController,
    onTareaClick: (Tarea) -> Unit // Callback para manejar clics en tareas
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    // Información del usuario
    val displayName = currentUser?.displayName ?: "Usuario"
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl) }

    // Lista de pestañas
    val tabTitles = listOf("Asignadas", "Pendientes", "De Otros")
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Tareas desde Firebase
    var tareas by remember { mutableStateOf<List<Tarea>>(emptyList()) }
    LaunchedEffect(Unit) {
        welcomeBD.cargarTareasDesdeFirestore(FirebaseFirestore.getInstance()) { listaTareas ->
            tareas = listaTareas ?: emptyList()
        }
    }

    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                            )
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = "Tareas",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color(0xFF0D47A1),
                            contentColor = Color.White
                        ) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTabIndex == index) Color.White else Color.Gray
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mostrar tareas filtradas según la pestaña seleccionada
                        when (selectedTabIndex) {
                            0 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario == "Antonio" },
                                onTareaClick = onTareaClick // Pasa el callback aquí
                            )
                            1 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario.isNullOrEmpty() },
                                onTareaClick = onTareaClick // Pasa el callback aquí
                            )
                            2 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario != "Antonio" && !it.usuario.isNullOrEmpty() },
                                onTareaClick = onTareaClick // Pasa el callback aquí
                            )
                        }
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeTopBar(
    photoUrl: Uri?,
    displayName: String,
    onProfileClick: () -> Unit,
    onGroupManagementClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = rememberImagePainter(data = photoUrl ?: R.drawable.default_avatar),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayName,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = Color.White)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.Gray)
            ) {
                DropdownMenuItem(text = { Text("Perfil") }, onClick = onProfileClick)
                DropdownMenuItem(text = { Text("Grupo") }, onClick = onGroupManagementClick)
                DropdownMenuItem(text = { Text("Logout") }, onClick = onLogoutClick)
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
    )
}

@Composable
fun MostrarTareasFiltradas(tareas: List<Tarea>, onTareaClick: (Tarea) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tareas) { tarea ->
            TareaItem(
                tarea = tarea,
                onClick = onTareaClick // Llama al callback aquí
            )
        }
    }
}

@Composable
fun TareaItem(tarea: Tarea, onClick: (Tarea) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.3f))
            .padding(16.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick(tarea) } // Llama al callback al hacer clic
    ) {
        Column {
            Text(
                text = tarea.nombre,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            if (tarea.usuario != null) {
                Text(
                    text = "Asignado a: ${tarea.usuario}",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
fun MainScreen(
    onNavigateToTarea: (Tarea) -> Unit,
    onNavigateToZonas: () -> Unit
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
                onGroupManagementClick = {
                    navController.navigate("group_management")
                },
                onLogoutClick = {
                    auth.signOut()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }
            )
        },
        bottomBar = {
            WelcomeBarra { selectedScreen ->
                when (selectedScreen) {
                    "Mis Tareas" -> navController.navigate("welcome")
                    "Zonas" -> navController.navigate("zonas")
                    "Estadísticas" -> navController.navigate("estadisticas")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "welcome",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("welcome") {
                Welcome(
                    navController = navController,
                    onTareaClick = onNavigateToTarea
                )
            }
            composable("zonas") {
                Zonas { zoneName ->
                    onNavigateToZonas()
                }
            }
            composable("group_management") {
                GroupManagementScreen(navController = navController)
            }
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    onProfileUpdated = { updatedDisplayName, updatedPhotoUrl ->
                        displayName = updatedDisplayName
                        photoUrl = updatedPhotoUrl
                    }
                )
            }
        }
    }
}



