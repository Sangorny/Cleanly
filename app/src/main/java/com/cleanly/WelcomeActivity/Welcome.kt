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
import com.cleanly.shared.welcomeBD.asignarTareaAlUsuario
import com.cleanly.shared.welcomeBD.completarTarea
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Tarea(
    val nombre: String,
    val usuario: String? = null,
    val completadoPor: String? = null
)

@Composable
fun Welcome(
    navController: NavHostController,
    onTareaClick: (Tarea) -> Unit,
    groupId: String,
    nombresUsuarios: Map<String, String>
) {


    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val displayName = remember(currentUser) { currentUser?.displayName ?: "Usuario" }
    val tabTitles = listOf("Asignadas", "Pendientes", "Otros")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var tareas by remember { mutableStateOf<List<Tarea>>(emptyList()) }




    LaunchedEffect(Unit) {
        welcomeBD.cargarTareasDesdeFirestore(
            db = FirebaseFirestore.getInstance(),
            groupId = groupId,
            onSuccess = { listaTareas ->
                tareas = listaTareas
            },
            onFailure = { exception ->
                tareas = emptyList()
            }
        )
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Tareas - $groupId",
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
                                tareas.filter { it.usuario == FirebaseAuth.getInstance().currentUser?.uid },
                                onTareaClick = { /* No se usa aquí */ },
                                mostrarAsignado = true,
                                nombresUsuarios = nombresUsuarios, // Pasar mapa de nombres
                                onCompletarTarea = { tarea ->
                                    completarTarea(
                                        db = FirebaseFirestore.getInstance(),
                                        groupId = groupId,
                                        tarea = tarea
                                    ) { success ->
                                        if (success) {
                                            val currentUID = FirebaseAuth.getInstance().currentUser?.uid
                                            tareas = tareas.map {
                                                if (it.nombre == tarea.nombre) it.copy(completadoPor = currentUID) else it
                                            }
                                        }
                                    }
                                }
                            )
                            1 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario.isNullOrEmpty() },
                                onTareaClick = { /* No se usa aquí */ },
                                mostrarAsignado = false,
                                nombresUsuarios = nombresUsuarios, // Pasar mapa de nombres
                                onAsignarTarea = { tarea ->
                                    asignarTareaAlUsuario(
                                        db = FirebaseFirestore.getInstance(),
                                        groupId = groupId,
                                        tarea = tarea
                                    ) { success ->
                                        if (success) {
                                            val currentUID = FirebaseAuth.getInstance().currentUser?.uid
                                            tareas = tareas.map {
                                                if (it.nombre == tarea.nombre) it.copy(usuario = currentUID) else it
                                            }
                                        }
                                    }
                                }
                            )
                            2 -> MostrarTareasFiltradas(
                                tareas.filter { it.usuario != FirebaseAuth.getInstance().currentUser?.uid && !it.usuario.isNullOrEmpty() },
                                onTareaClick = onTareaClick,
                                mostrarAsignado = true,
                                nombresUsuarios = nombresUsuarios // Pasar mapa de nombres
                            )
                        }


                    }
                }
            }
        }
    )
}