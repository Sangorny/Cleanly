package com.cleanly.TareasActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CRUDTareas(
    db: FirebaseFirestore,
    taskList: List<Tarea>,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
    onList: () -> Unit,
    onEdit: () -> Unit,
    onTaskListUpdated: (List<Tarea>) -> Unit,
    onTaskCompleted: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbarMessage by remember { mutableStateOf<String?>(null) }
    val checkedStates = remember { mutableStateMapOf<String, Boolean>() }
    var showDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var taskPoints by remember { mutableStateOf("") }
    var nombreOriginal by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Primera fila de botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Button(onClick = { showDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("Crear Tarea")
                }
                Button(onClick = onList, modifier = Modifier.weight(1f)) {
                    Text("Listar Tareas")
                }
            }

            // Segunda fila de botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Editar Tarea")
                }
                Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Borrar Tarea")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título "Lista de Tareas"
            Text(
                text = "Lista de Tareas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Lista de tareas con scroll si excede el tamaño de la pantalla
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                taskList.forEach { tarea ->
                    TaskRow(
                        task = tarea.nombre,
                        puntos = tarea.puntos,
                        isChecked = checkedStates[tarea.nombre] ?: false,
                        onCheckedChange = { isChecked -> checkedStates[tarea.nombre] = isChecked }
                    )
                }
            }

            // Botón "Completar Tareas" en la parte inferior y centrado
            Button(
                onClick = {
                    val tareasCompletadas = taskList.filter { tarea -> checkedStates[tarea.nombre] == true }
                    if (tareasCompletadas.isNotEmpty()) {
                        tareasCompletadas.forEach { tarea ->
                            TareasBD.marcarTareaComoCompletada(
                                db = db,
                                tareaNombre = tarea.nombre,
                                onSuccess = {
                                    showSnackbarMessage = "Tarea completada: ${tarea.nombre}"
                                    onTaskCompleted()
                                    checkedStates[tarea.nombre] = false
                                },
                                onFailure = {
                                    showSnackbarMessage = "Error al completar tarea: ${tarea.nombre}"
                                }
                            )
                        }
                    } else {
                        showSnackbarMessage = "No hay tareas seleccionadas para completar"
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            ) {
                Text("Completar Tareas")
            }

            // Mostrar Snackbar
            showSnackbarMessage?.let { message ->
                LaunchedEffect(message) {
                    snackbarHostState.showSnackbar(message)
                    showSnackbarMessage = null
                }
            }

            // Diálogo para Crear Tarea
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Añadir Nueva Tarea") },
                    text = {
                        Column {
                            TextField(
                                value = taskName,
                                onValueChange = { taskName = it },
                                label = { Text("Nombre de la Tarea") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = taskPoints,
                                onValueChange = { taskPoints = it },
                                label = { Text("Puntos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val puntos = taskPoints.toIntOrNull() ?: 0
                            TareasBD.agregarTareaAFirestore(
                                db = db,
                                nombre = taskName,
                                puntos = puntos,
                                context = context,
                                onSuccess = {
                                    showSnackbarMessage = "Tarea añadida correctamente"
                                    onCreate()
                                    taskName = ""
                                    taskPoints = ""
                                    TareasBD.cargarTareasDesdeFirestore(db) { tareasRecargadas ->
                                        onTaskListUpdated(tareasRecargadas)
                                        showDialog = false
                                    }
                                }
                            )
                        }) {
                            Text("Añadir")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo para Editar Tarea
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Editar Tarea") },
                    text = {
                        Column {
                            TextField(
                                value = taskName,
                                onValueChange = { taskName = it },
                                label = { Text("Nombre de la Tarea") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = taskPoints,
                                onValueChange = { taskPoints = it },
                                label = { Text("Puntos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val puntos = taskPoints.toIntOrNull() ?: 0
                            TareasBD.actualizarTareaEnFirestore(
                                db = db,
                                nombreOriginal = nombreOriginal,
                                nuevoNombre = taskName,
                                nuevosPuntos = puntos,
                                context = context,
                                onSuccess = {
                                    showSnackbarMessage = "Tarea actualizada correctamente"
                                    showEditDialog = false
                                    onList()
                                },
                                onFailure = {
                                    showSnackbarMessage = "Error al actualizar la tarea"
                                    showEditDialog = false
                                }
                            )
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showEditDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TaskRow(
    task: String,
    puntos: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = task,
            style = TextStyle(fontSize = 16.sp, color = Color.White),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$puntos puntos",
            style = TextStyle(fontSize = 16.sp, color = Color.White),
            modifier = Modifier.weight(0.5f)
        )

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.Green,
                uncheckedColor = Color.White
            ),
            modifier = Modifier.weight(0.2f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}


