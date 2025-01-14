package com.cleanly

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cleanly.shared.Tarea
import com.cleanly.shared.welcomeBD
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun Welcome(
    navController: NavHostController,
    onTareaClick: (Tarea) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val displayName = currentUser?.displayName ?: "Usuario"

    val tabTitles = listOf("Asignadas", "Pendientes", "Otros")
    var selectedTabIndex by remember { mutableStateOf(0) }

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

                        when (selectedTabIndex) {
                            0 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario == displayName && it.completadoPor.isNullOrEmpty() },
                                onTareaClick = onTareaClick,
                                mostrarAsignado = true
                            )
                            1 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario.isNullOrEmpty() && it.completadoPor.isNullOrEmpty() },
                                onTareaClick = onTareaClick,
                                mostrarAsignado = false
                            )
                            2 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario != displayName && !it.usuario.isNullOrEmpty() && it.completadoPor.isNullOrEmpty() },
                                onTareaClick = onTareaClick,
                                mostrarAsignado = true
                            )
                        }
                    }
                }
            }
        }
    )
}
