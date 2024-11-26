package com.cleanly


import com.cleanly.shared.welcomeBD
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanly.WelcomeActivity.WelcomeBarra
import com.cleanly.ZonaActivity.ZonasActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.cleanly.shared.Tarea

@Composable
fun Welcome() {
    Text(text = "Mis Tareas", style = MaterialTheme.typography.headlineMedium)
    val db = FirebaseFirestore.getInstance()
    val tabTitles = listOf("Asignadas", "Pendientes", "De Otros")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var tareas by remember { mutableStateOf<List<Tarea>>(emptyList()) }

    // Cargar las tareas desde Firebase al inicio
    LaunchedEffect(Unit) {
        welcomeBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            tareas = listaTareas
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                0 -> MostrarTareasFiltradas(tareas.filter { it.usuario == "Antonio" }) // Asignadas
                1 -> MostrarTareasFiltradas(tareas.filter { it.usuario.isNullOrEmpty() })      // Pendientes
                2 -> MostrarTareasFiltradas(tareas.filter { it.usuario != "Antonio" && !it.usuario.isNullOrEmpty()}) // De Otros
            }
        }
    }
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
                "Mis Tareas" -> Welcome()       // Llama al composable de las tareas
                "Zonas" -> ZonasActivity()    // Llama al composable de Zonas

            }
        }
    }
}



