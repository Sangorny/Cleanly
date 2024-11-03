package com.cleanly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VentanaPrincipal(taskList: List<Pair<String, Int>>) {
    val completedTasks = taskList.count { it.second > 0 }
    val totalTasks = taskList.size

    BasePantalla { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),  // Azul oscuro
                            Color(0xFF00E676)   // Verde
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Barra de progreso de tareas completadas en la parte superior
                BarraTareas(completedTasks, totalTasks)

                Spacer(modifier = Modifier.height(16.dp))

                // Encabezado de la lista de tareas
                Text(
                    text = "Tareas pendientes",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar cada tarea con sombreado gris claro
                taskList.forEach { (task, puntos) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nombre de la tarea en blanco
                            Text(
                                text = task,
                                style = TextStyle(fontSize = 18.sp, color = Color.White)
                            )
                            // Puntuación de la tarea en blanco y en negrita
                            Text(
                                text = "$puntos puntos",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}