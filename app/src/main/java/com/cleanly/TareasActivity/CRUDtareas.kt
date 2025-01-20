package com.cleanly.TareasActivity

import android.content.Context
import android.util.Log
import android.widget.Toast
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
    val zona: String,
    val subzona: String = "Sin Subzona",
    var isChecked: Boolean = false
)

@Composable
fun CRUDTareas(
    db: FirebaseFirestore,
    taskList: List<Tarea>,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
    onList: () -> Unit,
    onEdit: () -> Unit,
    onTaskListUpdated: (List<Tarea>) -> Unit,
    groupId: String,
    zonaSeleccionada: String,
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


    // Función para agregar una tarea
    fun agregarTareaAFirestore(
        groupId: String,
        nombre: String,
        puntos: Int,
        zona: String,
        subzona: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("grupos").document(groupId) // Referencia al grupo
        val tarea = hashMapOf(
            "nombre" to nombre,
            "puntos" to puntos,
            "zona" to zona,
            "subzona" to subzona,
            "completadoPor" to "",
            "completadoEn" to null
        )

        // Verificar si la subcolección "mistareas" existe y agregar la tarea
        groupRef.collection("mistareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(context, "Tarea añadida correctamente", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al añadir tarea", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para cargar tareas
    fun cargarTareasDesdeFirestore(
        groupId: String, // Se incluye el groupId como parámetro
        zonaSeleccionada: String,
        onSuccess: (List<Tarea>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        // Validar que el groupId no esté vacío
        if (groupId.isEmpty()) {
            onFailure(IllegalArgumentException("El groupId no puede estar vacío"))
            return
        }

        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("grupos").document(groupId) // Referencia al grupo

        // Consultar la subcolección "mistareas" del grupo
        groupRef.collection("mistareas")
            .whereEqualTo("zona", zonaSeleccionada) // Filtrar por zona
            .get()
            .addOnSuccessListener { result ->
                // Convertir los documentos en objetos Tarea
                val listaTareas = result.mapNotNull { document ->
                    val nombre = document.getString("nombre") ?: return@mapNotNull null
                    val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                    val subzona = document.getString("subzona") ?: "Sin Subzona"
                    Tarea(nombre, puntos, zonaSeleccionada, subzona)
                }
                onSuccess(listaTareas) // Llamar al callback de éxito con las tareas cargadas
            }
            .addOnFailureListener { exception ->
                onFailure(exception) // Llamar al callback de error con la excepción
            }
    }

    // Llamar a cargar tareas al montar el componente
    LaunchedEffect(zonaSeleccionada, groupId) { // Agrega groupId como dependencia del efecto
        if (groupId.isNotEmpty()) { // Asegúrate de que groupId esté definido
            cargarTareasDesdeFirestore(
                groupId = groupId, // Pasa el groupId necesario
                zonaSeleccionada = zonaSeleccionada,
                onSuccess = { listaTareas ->
                    onTaskListUpdated(listaTareas) // Actualiza la lista de tareas
                },
                onFailure = { exception ->
                    Log.e("CargarTareas", "Error al cargar tareas: ${exception.message}")
                    // Maneja el error, por ejemplo, con un mensaje en pantalla
                }
            )
        } else {
            Log.e("CargarTareas", "El groupId no está definido")
        }
    }
    // Función para eliminar tareas
    fun eliminarTareasDeFirestore(
        groupId: String, // Se incluye el groupId como parámetro
        nombresDeTareas: List<String>,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("grupos").document(groupId) // Referencia al grupo

        nombresDeTareas.forEach { nombre ->
            groupRef.collection("mistareas")
                .whereEqualTo("nombre", nombre)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(
                            context,
                            "No se encontró la tarea para eliminar: $nombre",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        querySnapshot.documents.forEach { document ->
                            document.reference.delete()
                                .addOnSuccessListener {
                                    onSuccess()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al eliminar tarea: $nombre",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Error al eliminar tarea: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    val handleCreate = {
        if (taskName.isNotBlank() && taskPoints.isNotBlank()) {
            agregarTareaAFirestore(
                groupId = groupId, // Utilizar el groupId ya definido
                nombre = taskName,
                puntos = taskPoints.toInt(),
                zona = zonaSeleccionada,
                subzona = taskSubzona.ifBlank { "Sin Subzona" },
                context = context,
                onSuccess = {
                    showSnackbarMessage = "Tarea añadida correctamente"
                    showDialog = false
                    // Actualizar la lista en tiempo real
                    cargarTareasDesdeFirestore(
                        groupId = groupId,
                        zonaSeleccionada = zonaSeleccionada,
                        onSuccess = onTaskListUpdated,
                        onFailure = { exception ->
                            showSnackbarMessage = "Error al cargar tareas: ${exception.message}"
                        }
                    )
                }
            )
        } else {
            showSnackbarMessage = "Por favor completa todos los campos requeridos."
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
            showEditDialog = true
        }
        // Llamar a cargarTareasDesdeFirestore al finalizar la edición (después de actualizar Firestore)
        cargarTareasDesdeFirestore(
            groupId = groupId,
            zonaSeleccionada = zonaSeleccionada,
            onSuccess = onTaskListUpdated,
            onFailure = { exception ->
                showSnackbarMessage = "Error al cargar tareas: ${exception.message}"
            }
        )
    }

    val handleDelete = {
        val nombresTareasMarcadas = taskList
            .filter { tarea -> checkedStates[tarea.nombre] == true }
            .map { it.nombre }

        if (nombresTareasMarcadas.isNotEmpty()) {
            eliminarTareasDeFirestore(
                groupId = groupId,
                nombresDeTareas = nombresTareasMarcadas,
                context = context,
                onSuccess = {
                    showSnackbarMessage = "Tareas eliminadas correctamente"
                    checkedStates.clear()
                    // Actualizar la lista en tiempo real
                    cargarTareasDesdeFirestore(
                        groupId = groupId,
                        zonaSeleccionada = zonaSeleccionada,
                        onSuccess = onTaskListUpdated,
                        onFailure = { exception ->
                            showSnackbarMessage = "Error al cargar tareas: ${exception.message}"
                        }
                    )
                }
            )
        } else {
            showSnackbarMessage = "No hay tareas seleccionadas para eliminar"
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
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
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

            VisualBoton(
                onCreate = { showDialog = true }, // Activa el cuadro de diálogo
                onEdit = handleEdit,
                onDelete = handleDelete,
                onList = { /*cargarTareasDesdeFirestore(zonaSeleccionada, onTaskListUpdated)*/ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            taskList.forEach { tarea ->
                TaskRow(
                    task = tarea.nombre,
                    puntos = tarea.puntos,
                    subzona = tarea.subzona,
                    isChecked = checkedStates[tarea.nombre] ?: false,
                    onCheckedChange = { isChecked -> checkedStates[tarea.nombre] = isChecked }
                )
            }

            // Mostrar Snackbar
            showSnackbarMessage?.let { message ->
                LaunchedEffect(message) {
                    snackbarHostState.showSnackbar(message)
                    showSnackbarMessage = null
                }
            }

            // Cuadro de diálogo de agregar tarea
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
                                value = taskSubzona,
                                onValueChange = { taskSubzona = it },
                                label = { Text("Subzona") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = taskPoints,
                                onValueChange = { taskPoints = it },
                                label = { Text("Puntos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Prioridad:")
                            DropdownPrioridad(
                                selectedPriority = selectedPriority, // Estado actual de la prioridad
                                onPriorityChange = { newPriority ->
                                    selectedPriority = newPriority // Actualiza el estado
                                }
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
        }
    }
}

@Composable
fun TaskRow(
    task: String,
    puntos: Int,
    subzona: String?,
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

        Text(
            text = "$puntos puntos",
            style = TextStyle(fontSize = 12.sp, color = Color.White)
        )

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
fun DropdownPrioridad(
    selectedPriority: String,
    onPriorityChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) } // Controla si el menú está expandido o no
    val priorities = listOf("Baja", "Media", "Urgente") // Lista de prioridades predeterminadas

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = selectedPriority) // Muestra la prioridad seleccionada
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            priorities.forEach { priority ->
                DropdownMenuItem(
                    text = { Text(priority) },
                    onClick = {
                        onPriorityChange(priority) // Notifica el cambio al padre
                        expanded = false // Cierra el menú
                    }
                )
            }
        }
    }
}