package com.cleanly.TareasActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

data class Tarea(
    val nombre: String,
    val puntos: Int,
    var isChecked: Boolean = false
)

@Composable
fun CRUDTareas(
    db: FirebaseFirestore,
    taskList: List<Pair<String, Int>>,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
    onList: () -> Unit,
    onEdit: () -> Unit,
    onTaskListUpdated: (List<Pair<String, Int>>) -> Unit
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
    val handleCreate = {
        if (taskName.isNotBlank() && taskPoints.isNotBlank()) {
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
                        onTaskListUpdated(tareasRecargadas.map { it.nombre to it.puntos })
                        showDialog = false
                    }
                }
            )
        } else {
            showDialog = false
        }
    }
    val handleEdit = {
        val tareasMarcadas = taskList.filter { tarea -> checkedStates[tarea.first] == true }

        if (tareasMarcadas.size != 1) {
            showSnackbarMessage = if (tareasMarcadas.isEmpty()) {
                "Selecciona una tarea para editar"
            } else {
                "No puedes editar más de una tarea a la vez"
            }
        } else {
            val tareaSeleccionada = tareasMarcadas.first()
            nombreOriginal = tareaSeleccionada.first
            taskName = tareaSeleccionada.first
            taskPoints = tareaSeleccionada.second.toString()
            showEditDialog = true
        }
    }
    val handleDelete = {
        val tareasMarcadasCount = checkedStates.values.count { it }
        val nombresTareasMarcadas = taskList
            .filter { tarea -> checkedStates[tarea.first] == true }
            .map { it.first }

        if (nombresTareasMarcadas.isNotEmpty()) {
            TareasBD.eliminarTareasDeFirestore(
                db = db,
                nombresDeTareas = nombresTareasMarcadas,
                context = context,
                onSuccess = {
                    showSnackbarMessage = "$tareasMarcadasCount tareas borradas correctamente"
                    checkedStates.clear()
                    TareasBD.cargarTareasDesdeFirestore(db) { tareasRecargadas ->
                        onTaskListUpdated(tareasRecargadas.map { it.nombre to it.puntos })
                    }
                    onDelete()
                },
                onFailure = {
                    showSnackbarMessage = "Error al borrar tareas"
                }
            )
        } else {
            showSnackbarMessage = "No hay tareas seleccionadas para borrar"
        }
    }

    val onList = {
        TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            onTaskListUpdated(listaTareas.map { it.nombre to it.puntos })
            showSnackbarMessage = "Lista de tareas actualizada"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),
                            Color(0xFF00E676)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CRUDboton(
                onCreate = { showDialog = true },
                onEdit = handleEdit,
                onDelete = handleDelete,
                onList = onList
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lista de Tareas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            taskList.forEach { (task, puntos) ->
                TaskRow(
                    task = task,
                    puntos = puntos,
                    isChecked = checkedStates[task] ?: false,
                    onCheckedChange = { isChecked -> checkedStates[task] = isChecked }
                )
            }
        }

        showSnackbarMessage?.let { message ->
            LaunchedEffect(message) {
                snackbarHostState.showSnackbar(message)
                showSnackbarMessage = null
            }
        }
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
                    Button(onClick = handleCreate) {
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

        // Diálogo para editar la tarea seleccionada
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
