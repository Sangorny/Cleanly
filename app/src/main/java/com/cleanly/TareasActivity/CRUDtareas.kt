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
    val prioridad: String = "Baja",
    val usuario: String = "",
    var isChecked: Boolean = false,
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
    nombresUsuarios: Map<String, String>,
    isAdmin: Boolean
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
    var tareaParaAsignar by remember { mutableStateOf<Tarea?>(null) }
    var showAsignarDialog by remember { mutableStateOf(false) }
    var usuarioSeleccionado by remember { mutableStateOf("") }


    // Función para agregar una tarea
    fun agregarTareaAFirestore(
        groupId: String,
        nombre: String,
        puntos: Int,
        zona: String,
        subzona: String,
        prioridad: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("grupos").document(groupId)
        val tarea = hashMapOf(
            "nombre" to nombre,
            "puntos" to puntos,
            "zona" to zona,
            "subzona" to subzona,
            "prioridad" to prioridad,
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
        groupId: String,
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
        val groupRef = db.collection("grupos").document(groupId)

        // Consultar la subcolección "mistareas" del grupo
        groupRef.collection("mistareas")
            .whereEqualTo("zona", zonaSeleccionada)
            .get()
            .addOnSuccessListener { result ->
                // Convertir los documentos en objetos Tarea
                val listaTareas = result.mapNotNull { document ->
                    val nombre = document.getString("nombre") ?: return@mapNotNull null
                    val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                    val subzona = document.getString("subzona") ?: "Sin Subzona"
                    val prioridad = document.getString("prioridad") ?: "Baja"
                    val usuario = document.getString("usuario") ?: ""
                    Tarea(nombre, puntos, zonaSeleccionada, subzona, prioridad, usuario)
                }
                onSuccess(listaTareas)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Llamar a cargar tareas al montar el componente
    LaunchedEffect(zonaSeleccionada, groupId) {
        if (groupId.isNotEmpty()) {
            cargarTareasDesdeFirestore(
                groupId = groupId,
                zonaSeleccionada = zonaSeleccionada,
                onSuccess = { listaTareas ->
                    onTaskListUpdated(listaTareas)
                },
                onFailure = { exception ->
                    Log.e("CargarTareas", "Error al cargar tareas: ${exception.message}")
                }
            )
        } else {
            Log.e("CargarTareas", "El groupId no está definido")
        }
    }
    // Función para eliminar tareas
    fun eliminarTareasDeFirestore(
        groupId: String,
        nombresDeTareas: List<String>,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("grupos").document(groupId)

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

    fun asignarTarea(
        tarea: Tarea,
        usuarioId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("grupos")
            .document(groupId)
            .collection("mistareas")
            .whereEqualTo("nombre", tarea.nombre)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onFailure(Exception("No se encontró la tarea con el nombre '${tarea.nombre}'."))
                } else {
                    val documentRef = querySnapshot.documents.first().reference
                    documentRef.update("usuario", usuarioId)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onFailure(exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun showAsignarDialog(tarea: Tarea) {
        tareaParaAsignar = tarea
        showAsignarDialog = true
    }

    fun editarTareaEnFirestore(
        groupId: String,
        nombreOriginal: String,
        nuevoNombre: String,
        nuevosPuntos: Int,
        nuevaSubzona: String,
        nuevaPrioridad: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("grupos").document(groupId)

        groupRef.collection("mistareas")
            .whereEqualTo("nombre", nombreOriginal)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // No se encontró ningún documento con ese nombre
                    onFailure(Exception("No se encontró la tarea con el nombre $nombreOriginal"))
                } else {
                    // Suponiendo que el nombre es único, tomas el primer documento
                    val docRef = querySnapshot.documents.first().reference

                    // Haces el update de los campos
                    docRef.update(
                        mapOf(
                            "nombre" to nuevoNombre,
                            "puntos" to nuevosPuntos,
                            "subzona" to nuevaSubzona,
                            "prioridad" to nuevaPrioridad
                        )
                    )
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }


    val handleCreate = {
        if (taskName.isNotBlank() && taskPoints.isNotBlank()) {
            agregarTareaAFirestore(
                groupId = groupId,
                nombre = taskName,
                puntos = taskPoints.toInt(),
                zona = zonaSeleccionada,
                subzona = taskSubzona.ifBlank { "Sin Subzona" },
                prioridad = selectedPriority,
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

    val handleAsignar = {
        val tareasSeleccionadas = taskList.filter { tarea -> checkedStates[tarea.nombre] == true }
        if (tareasSeleccionadas.size != 1) {
            showSnackbarMessage = if (tareasSeleccionadas.isEmpty()) {
                "Selecciona una tarea para asignar"
            } else {
                "Solo puedes asignar una tarea a la vez"
            }
        } else {
            val tareaSeleccionada = tareasSeleccionadas.first()
            showAsignarDialog(tareaSeleccionada)
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

            if (isAdmin) {
                VisualBoton(
                    onCreate = { showDialog = true },
                    onEdit = handleEdit,
                    onDelete = handleDelete,
                    onList = {},
                    onAsignar = handleAsignar
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            taskList.forEach { tarea ->
                TaskRow(
                    task = tarea.nombre,
                    subzona = tarea.subzona,
                    prioridad = tarea.prioridad,
                    asignadoA = nombresUsuarios[tarea.usuario],
                    puntos = tarea.puntos,
                    isChecked = checkedStates[tarea.nombre] ?: false,
                    onCheckedChange = { isChecked ->
                        checkedStates[tarea.nombre] = isChecked
                    }
                )
            Spacer(modifier = Modifier.height(16.dp))
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
                                selectedPriority = selectedPriority,
                                onPriorityChange = { newPriority ->
                                    selectedPriority = newPriority
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
            if (showAsignarDialog && tareaParaAsignar != null) {
                AlertDialog(
                    onDismissRequest = { showAsignarDialog = false },
                    title = { Text("Asignar Tarea") },
                    text = {
                        Column {
                            Text("Selecciona un usuario para la tarea: ${tareaParaAsignar!!.nombre}")
                            Spacer(modifier = Modifier.height(8.dp))
                            DropdownUsuarios(
                                nombresUsuarios = nombresUsuarios,
                                selectedUser = usuarioSeleccionado,
                                onUserSelected = { usuarioSeleccionado = it }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (usuarioSeleccionado.isNotEmpty()) {
                                asignarTarea(
                                    tarea = tareaParaAsignar!!,
                                    usuarioId = usuarioSeleccionado,
                                    onSuccess = {
                                        showSnackbarMessage = "Tarea '${tareaParaAsignar!!.nombre}' asignada a ${nombresUsuarios[usuarioSeleccionado]}"
                                        showAsignarDialog = false
                                        // Ahora recargas la lista de tareas después de la asignación
                                        cargarTareasDesdeFirestore(
                                            groupId = groupId,
                                            zonaSeleccionada = zonaSeleccionada,
                                            onSuccess = onTaskListUpdated,
                                            onFailure = { exception ->
                                                showSnackbarMessage = "Error al cargar tareas: ${exception.message}"
                                            }
                                        )
                                    },
                                    onFailure = { exception ->
                                        showSnackbarMessage = "Error al asignar la tarea: ${exception.message}"
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Selecciona un usuario", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Asignar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showAsignarDialog = false }) {
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
                                selectedPriority = selectedPriority,
                                onPriorityChange = { newPriority ->
                                    selectedPriority = newPriority
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            // Llamada a la función de edición
                            editarTareaEnFirestore(
                                groupId = groupId,
                                nombreOriginal = nombreOriginal,
                                nuevoNombre = taskName,
                                nuevosPuntos = taskPoints.toInt(),
                                nuevaSubzona = taskSubzona,
                                nuevaPrioridad = selectedPriority,
                                onSuccess = {
                                    showSnackbarMessage = "Tarea editada correctamente"
                                    showEditDialog = false
                                    // Recargar la lista de tareas tras la edición
                                    cargarTareasDesdeFirestore(
                                        groupId = groupId,
                                        zonaSeleccionada = zonaSeleccionada,
                                        onSuccess = onTaskListUpdated,
                                        onFailure = { exception ->
                                            showSnackbarMessage = "Error al cargar tareas: ${exception.message}"
                                        }
                                    )
                                },
                                onFailure = { exception ->
                                    showSnackbarMessage = "Error al editar la tarea: ${exception.message}"
                                }
                            )
                        }) {
                            Text("Guardar cambios")
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
    subzona: String?,
    prioridad: String,
    asignadoA: String?,
    puntos: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Columna izquierda: Nombre y subzona
        Column(modifier = Modifier.weight(1f)) {
            // Nombre de la tarea
            Text(
                text = task,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )

            // Subzona
            if (!subzona.isNullOrEmpty() && subzona != "Sin Subzona") {
                Text(
                    text = "Subzona: $subzona",
                    style = TextStyle(fontSize = 12.sp, color = Color.White)
                )
            }
        }

        // Columna central: Gestión y prioridad
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nombre de quien lo gestiona
            if (!asignadoA.isNullOrEmpty()) {
                Text(
                    text = "$asignadoA",
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontSize = 14.sp, color = Color.Yellow)
                )
            }

            // Prioridad con colores
            Text(
                text = prioridad,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = when (prioridad) {
                        "Baja" -> Color.Green
                        "Media" -> Color(0xFFFFA500) // Naranja
                        "Urgente" -> Color.Red
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Columna derecha: Puntos y checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween, // Asegura el espacio entre los elementos
            modifier = Modifier.weight(1f)
        ) {
            // Puntos
            Text(
                text = "$puntos pts",
                style = TextStyle(fontSize = 12.sp, color = Color.White),
                modifier = Modifier.weight(1f) // Empuja el texto hacia la derecha
            )

            // Espacio entre puntos y checkbox
            Spacer(modifier = Modifier.width(8.dp)) // Ajusta el espacio según tu preferencia

            // Checkbox más pequeño
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Green,
                    uncheckedColor = Color.White
                ),
                modifier = Modifier.size(12.dp) // Tamaño reducido
            )
        }
    }
}

//Poder bajar en la ventana
@Composable
fun DropdownPrioridad(
    selectedPriority: String,
    onPriorityChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val priorities = listOf("Baja", "Media", "Urgente")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = selectedPriority)
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

@Composable
fun DropdownUsuarios(
    nombresUsuarios: Map<String, String>,
    selectedUser: String,
    onUserSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text = nombresUsuarios[selectedUser] ?: "Selecciona un usuario")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            nombresUsuarios.forEach { (uid, nombre) ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = {
                        onUserSelected(uid)
                        expanded = false
                    }
                )
            }
        }
    }
}