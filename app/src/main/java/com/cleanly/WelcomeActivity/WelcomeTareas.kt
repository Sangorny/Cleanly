package com.cleanly

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cleanly.shared.Tarea

@Composable
fun MostrarTareasFiltradas(
    tareas: List<Tarea>,
    onTareaClick: (Tarea) -> Unit,
    mostrarAsignado: Boolean,
    nombresUsuarios: Map<String, String>,
    onAsignarTarea: ((Tarea) -> Unit)? = null,
    onCompletarTarea: ((Tarea) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tareas) { tarea ->
            val nombreUsuario = nombresUsuarios[tarea.usuario] ?: tarea.usuario ?: "Sin asignar"
            val nombreCompletadoPor = nombresUsuarios[tarea.completadoPor] ?: tarea.completadoPor ?: "No completado"

            TareaItem(
                tarea = tarea.copy(
                    usuario = nombreUsuario,
                    completadoPor = nombreCompletadoPor
                ),
                onClick = onTareaClick,
                mostrarAsignado = mostrarAsignado,
                onAsignarTarea = onAsignarTarea,
                onCompletarTarea = onCompletarTarea
            )
        }
    }
}

@Composable
fun TareaItem(
    tarea: Tarea,
    onClick: (Tarea) -> Unit,
    mostrarAsignado: Boolean = true,
    onCompletarTarea: ((Tarea) -> Unit)? = null,
    onAsignarTarea: ((Tarea) -> Unit)? = null
) {
    var mostrarDialogoCompletar by remember { mutableStateOf(false) }
    var mostrarDialogoAsignar by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.3f))
            .padding(16.dp)
            .clickable {
                if (onCompletarTarea != null) {
                    mostrarDialogoCompletar = true
                } else if (onAsignarTarea != null) {
                    mostrarDialogoAsignar = true
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tarea.nombre,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            if (mostrarAsignado && !tarea.usuario.isNullOrEmpty()) {
                // Usa el valor actual de tarea.usuario, que debería ser el nombre ya traducido
                Text(
                    text = tarea.usuario,
                    color = Color.Yellow
                )
            }
        }
    }

    if (mostrarDialogoCompletar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCompletar = false },
            confirmButton = {
                Button(onClick = {
                    onCompletarTarea?.invoke(tarea)
                    mostrarDialogoCompletar = false
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                Button(onClick = { mostrarDialogoCompletar = false }) {
                    Text("No")
                }
            },
            title = { Text("¿Completar tarea?") }
        )
    }

    if (mostrarDialogoAsignar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAsignar = false },
            confirmButton = {
                Button(onClick = {
                    onAsignarTarea?.invoke(tarea)
                    mostrarDialogoAsignar = false
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                Button(onClick = { mostrarDialogoAsignar = false }) {
                    Text("No")
                }
            },
            title = { Text("¿Asignarte la tarea?") }
        )
    }
}