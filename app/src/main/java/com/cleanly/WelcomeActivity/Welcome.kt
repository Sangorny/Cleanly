package com.cleanly

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.cleanly.WelcomeActivity.GroupManagementActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cleanly.shared.Tarea
import com.cleanly.ZonaActivity.ZonasActivity
import com.cleanly.WelcomeActivity.ProfileScreen
import com.cleanly.WelcomeActivity.WelcomeBarra
import com.cleanly.shared.welcomeBD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Welcome(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    // Verifica si el usuario está logueado y recupera la información de la cuenta
    val displayName = currentUser?.displayName ?: "Usuario"
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl) }

    // Cargar las tareas desde Firebase al inicio
    var tareas by remember { mutableStateOf<List<Tarea>>(emptyList()) }
    LaunchedEffect(Unit) {
        welcomeBD.cargarTareasDesdeFirestore(FirebaseFirestore.getInstance()) { listaTareas ->
            tareas = listaTareas
        }
    }

    // Recargar el perfil cuando se inicie sesión
    LaunchedEffect(currentUser) {
        currentUser?.reload()?.addOnCompleteListener {
            photoUrl = currentUser.photoUrl
        }
    }

    // Lista de títulos de pestañas
    val tabTitles = listOf("Asignadas", "Pendientes", "De Otros")

    // Índice de la pestaña seleccionada (inicialmente en la primera pestaña)
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Scaffold para contener el topBar, contenido y la barra inferior
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Avatar del usuario
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

                        // Nombre del usuario
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
                        DropdownMenuItem(
                            text = { Text("Perfil") },
                            onClick = {
                                expanded = false
                                // Usamos NavController para navegar a la pantalla ProfileScreen
                                navController.navigate("profile") // Este es el cambio principal
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Grupo") },
                            onClick = {
                                expanded = false
                                val intent = Intent(context, GroupManagementActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                expanded = false
                                auth.signOut()
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1)),
            )
        },
        bottomBar = {
            WelcomeBarra { selectedScreen ->
                when (selectedScreen) {
                    "Mis Tareas" -> navController.navigate("mis_tareas")
                    "Zonas" -> navController.navigate("zonas")
                    "Estadísticas" -> navController.navigate("estadisticas")
                }
            }
        },
        content = { paddingValues ->
            // Contenido de la pantalla de bienvenida
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "Tareas",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar tareas filtradas según la pestaña seleccionada
                when (selectedTabIndex) {
                    0 -> MostrarTareasFiltradas(tareas.filter { it.usuario == "Antonio" }) // Asignadas
                    1 -> MostrarTareasFiltradas(tareas.filter { it.usuario.isNullOrEmpty() }) // Pendientes
                    2 -> MostrarTareasFiltradas(tareas.filter { it.usuario != "Antonio" && !it.usuario.isNullOrEmpty() }) // De Otros
                }
            }
        }
    )
}


@Composable
fun MostrarTareasFiltradas(tareasFiltradas: List<Tarea>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tareasFiltradas) { tarea ->
            TareaItem(tarea)
        }
    }
}

@Composable
fun TareaItem(tarea: Tarea) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.3f))
            .padding(16.dp)
            .clip(MaterialTheme.shapes.medium)
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
fun MainScreen() {
    var currentScreen by remember { mutableStateOf("Mis Tareas") }

    val navController = rememberNavController() // Crear el NavHostController

    Scaffold(
        bottomBar = {
            WelcomeBarra { selectedScreen ->
                currentScreen = selectedScreen
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "Mis Tareas" -> Welcome(navController) // Pasar el navController
                "Zonas" -> ZonasActivity() // Llama al composable de Zonas
            }
        }
    }
}
