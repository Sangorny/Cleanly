package com.cleanly.TareasActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.foundation.lazy.items


data class Tarea(
    val nombre: String,
    val puntos: Int,
    val zona: String,
    val subzona: String = "Sin Subzona", // Campo opcional con un valor predeterminado
    var prioridad: String = "Baja",     // Campo de prioridad con valor predeterminado
    var isChecked: Boolean = false
)

@Composable
fun CRUDTareas(
    db: FirebaseFirestore,
    groupId: String, // ID del grupo para identificar la subcolección
    taskList: List<Tarea>,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
    onList: () -> Unit,
    onEdit: () -> Unit,
    onTaskListUpdated: (List<Tarea>) -> Unit,
    zonaSeleccionada: String
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
    var taskSubzona by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Baja") }

    val handleCreate = {
        if (taskName.isNotBlank() && taskPoints.isNotBlank()) {
            val puntos = taskPoints.toIntOrNull() ?: 0
            val subzona = taskSubzona.ifBlank { "Sin Subzona" }

            TareasBD.agregarTareaAFirestore(
                db = db,
                groupId = groupId, 
                nombre = taskName,
                puntos = puntos,
                zona = zonaSeleccionada,
                subzona = subzona,
                prioridad = selectedPriority,
                context = context,
                onSuccess = {
                    showSnackbarMessage = "Tarea añadida correctamente"
                    onCreate() // Refrescar tareas
                    taskName = ""
                    taskPoints = ""
                    taskSubzona = ""
                    selectedPriority = "Baja" // Reinicia los campos del formulario

                    // Recargar la lista de tareas desde Firestore
                    TareasBD.cargarTareasDesdeFirestore(db, groupId, zonaSeleccionada) { tareasRecargadas ->
                        onTaskListUpdated(tareasRecargadas)
                        showDialog = false
                    }
                }
            )
        } else {
            showSnackbarMessage = "Por favor completa todos los campos requeridos."
            showDialog = false
        }
    }

    val handleEdit = {
        val tareasMarcadas = taskList.filter { tarea -> checkedStates[tarea.nombre] == true }

        if (tareasMarcadas.size != 1) {
            showSnackbarMessage = if (tareasMarcadas.isEmpty()) {
                "Selecciona una tarea para editar"
            } else {
                "No puedes editar más de una tarea a la vez"
            }
        } else {
            val tareaSeleccionada = tareasMarcadas.first()
            nombreOriginal = tareaSeleccionada.nombre
            taskName = tareaSeleccionada.nombre
            taskPoints = tareaSeleccionada.puntos.toString()
            taskSubzona = tareaSeleccionada.subzona
            selectedPriority = tareaSeleccionada.prioridad
            showEditDialog = true
        }
    }

    val handleDelete = {
        val nombresTareasMarcadas = taskList.filter { tarea -> checkedStates[tarea.nombre] == true }
            .map { it.nombre }

        if (nombresTareasMarcadas.isNotEmpty()) {
            TareasBD.eliminarTareasDeFirestore(
                db = db,
                groupId = groupId,
                nombresDeTareas = nombresTareasMarcadas,
                context = context,
                onSuccess = {
                    showSnackbarMessage = "Tareas eliminadas correctamente"
                    checkedStates.clear()
                    TareasBD.cargarTareasDesdeFirestore(db, groupId, zonaSeleccionada) { tareasRecargadas ->
                        onTaskListUpdated(tareasRecargadas)
                    }
                    onDelete()
                },
                onFailure = {
                    showSnackbarMessage = "Error al eliminar tareas"
                }
            )
        } else {
            showSnackbarMessage = "No hay tareas seleccionadas para eliminar"
        }
    }

    val onList = {
        TareasBD.cargarTareasDesdeFirestore(db, groupId, zonaSeleccionada) { listaTareas ->
            onTaskListUpdated(listaTareas)
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
            Text(
                text = "$zonaSeleccionada",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            CRUDboton(
                onCreate = { showDialog = true },
                onEdit = handleEdit,
                onDelete = handleDelete,
                onList = onList
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                items(taskList) { tarea ->
                    TaskRow(
                        task = tarea.nombre,
                        puntos = tarea.puntos,
                        subzona = tarea.subzona,
                        prioridad = tarea.prioridad,
                        isChecked = checkedStates[tarea.nombre] ?: false,
                        onCheckedChange = { isChecked -> checkedStates[tarea.nombre] = isChecked }
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
                            TextField(
                                value = taskSubzona,
                                onValueChange = { taskSubzona = it },
                                label = { Text("Subzona") },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                            TextField(
                                value = taskPoints,
                                onValueChange = { taskPoints = it },
                                label = { Text("Puntos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Prioridad:")
                            DropdownPrioridad(
                                selectedPriority = selectedPriority,
                                onPriorityChange = { selectedPriority = it }
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
                            TextField(
                                value = taskPoints,
                                onValueChange = { taskPoints = it },
                                label = { Text("Puntos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            TextField(
                                value = taskSubzona,
                                onValueChange = { taskSubzona = it },
                                label = { Text("Subzona") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Prioridad:")
                            DropdownPrioridad(
                                selectedPriority = selectedPriority,
                                onPriorityChange = { selectedPriority = it }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = handleEdit) {
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
    subzona: String?, // Subzona opcional
    prioridad: String, // Añadir prioridad
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Columna con nombre de tarea y subzona
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )

            if (!subzona.isNullOrEmpty() && subzona != "Sin Subzona") {
                Text(
                    text = "Subzona: $subzona",
                    style = TextStyle(fontSize = 12.sp, color = Color.White)
                )
            }
        }

        // Texto de prioridad
        val prioridadColor = when (prioridad) {
            "Baja" -> Color.Green
            "Normal" -> Color.Yellow
            "Urgente" -> Color.Red
            else -> Color.Gray
        }

        Text(
            text = prioridad,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontSize = 12.sp, color = prioridadColor),
            modifier = Modifier.padding(horizontal = 16.dp) // Añadir más separación horizontal
        )


        Spacer(modifier = Modifier.width(20.dp))

        // Texto de puntos
        Text(
            text = "$puntos puntos",
            style = TextStyle(fontSize = 12.sp, color = Color.White),
            modifier = Modifier.padding(end = 8.dp)
        )

        // Checkbox
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.Green,
                uncheckedColor = Color.White
            )
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun DropdownPrioridad(selectedPriority: String, onPriorityChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val priorities = listOf("Baja", "Normal", "Urgente")

    Box(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedPriority, color = Color.Black)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            priorities.forEach { priority ->
                DropdownMenuItem(
                    text = { Text(priority) },
                    onClick = {
                        onPriorityChange(priority)
                        expanded = false
                    }
                )
            }
        }
    }
}

