package com.cleanly


import com.cleanly.shared.welcomeBD
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import com.google.firebase.firestore.FirebaseFirestore
import com.cleanly.shared.Tarea

@Composable
fun Welcome(onTareaClick: (Tarea) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val tabTitles = listOf("Asignadas", "Pendientes", "De Otros")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var tareas by remember { mutableStateOf<List<Tarea>>(emptyList()) }

    LaunchedEffect(Unit) {
        welcomeBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            tareas = listaTareas ?: emptyList() // Manejar posibles `null`
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

            when (selectedTabIndex) {
                0 -> MostrarTareasFiltradas(
                    tareas.filter { it.usuario == "Antonio" },
                    onTareaClick = onTareaClick
                )
                1 -> MostrarTareasFiltradas(
                    tareas.filter { it.usuario.isNullOrEmpty() },
                    onTareaClick = onTareaClick
                )
                2 -> MostrarTareasFiltradas(
                    tareas.filter { it.usuario != "Antonio" && !it.usuario.isNullOrEmpty() },
                    onTareaClick = onTareaClick
                )
            }
        }
    }
}

@Composable
fun MostrarTareasFiltradas(tareas: List<Tarea>, onTareaClick: (Tarea) -> Unit) {
    LazyColumn {
        items(tareas) { tarea ->
            TareaItem(
                tarea = tarea,
                onClick = onTareaClick // Manejar clics aquí
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
            .clickable { onClick(tarea) } // Manejar clic en la tarea
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
    onNavigateToTarea: (Tarea) -> Unit, // Aceptar Tarea, no String
    onNavigateToZonas: () -> Unit
) {
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
                "Mis Tareas" -> Welcome(
                    onTareaClick = onNavigateToTarea // Pasa el callback esperado
                )
                "Zonas" -> Zonas { zoneName ->
                    onNavigateToZonas()
                }
            }
        }
    }
}




