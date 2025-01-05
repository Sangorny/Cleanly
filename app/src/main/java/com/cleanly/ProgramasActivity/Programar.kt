package com.cleanly.ProgramasActivity

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

data class Tarea(
    val nombre: String,
    val puntos: Int,
    val zona: String,
    val frecuencia: String = "", // Por defecto vacío si no está definido
    val subzona: String = "" // Asegúrate de incluir subzona
)

@Composable
fun ProgramarScreen(navController: NavHostController) {
    var todasLasTareas by remember { mutableStateOf<List<Tarea>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var tareaSeleccionada by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    val firestore = FirebaseFirestore.getInstance()

    // Pestañas
    val tabTitles = listOf("Sin Programar", "Diarias", "Semanales", "Mensuales")
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        cargarTodasLasTareasDesdeFirestore(
            db = firestore,
            onSuccess = { tareas ->
                todasLasTareas = tareas
                isLoading = false
            },
            onFailure = { isLoading = false }
        )
    }

    // Fondo aplicado a toda la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676)) // Gradiente azul a verde
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent, // Importante para mantener el fondo visible
            content = { paddingValues ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Cargando tareas...", color = Color.White)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Text(
                            text = "Programar Tareas",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Pestañas
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
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTabIndex == index) Color.White else Color.Gray
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val tareasFiltradas = when (selectedTabIndex) {
                            0 -> todasLasTareas.filter { it.frecuencia.isEmpty() }
                            1 -> todasLasTareas.filter { it.frecuencia == "Diaria" }
                            2 -> todasLasTareas.filter { it.frecuencia == "Semanal" }
                            3 -> todasLasTareas.filter { it.frecuencia == "Mensual" }
                            else -> emptyList()
                        }

                        MostrarTareasPorZona(
                            tareas = tareasFiltradas,
                            onProgramarTarea = { tarea ->
                                tareaSeleccionada = tarea
                                mostrarDialogo = true
                            }
                        )
                    }
                }
            }
        )

        if (mostrarDialogo && tareaSeleccionada != null) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("¿Quieres programar la tarea?") },
                text = {
                    Column {
                        Text("Selecciona una opción:")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            actualizarFrecuencia(
                                db = firestore,
                                tarea = tareaSeleccionada,
                                nuevaFrecuencia = "Diaria",
                                onCompletion = { tareas ->
                                    todasLasTareas = tareas
                                    mostrarDialogo = false
                                }
                            )
                        }) {
                            Text("Diaria")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            actualizarFrecuencia(
                                db = firestore,
                                tarea = tareaSeleccionada,
                                nuevaFrecuencia = "Semanal",
                                onCompletion = { tareas ->
                                    todasLasTareas = tareas
                                    mostrarDialogo = false
                                }
                            )
                        }) {
                            Text("Semanal")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            actualizarFrecuencia(
                                db = firestore,
                                tarea = tareaSeleccionada,
                                nuevaFrecuencia = "Mensual",
                                onCompletion = { tareas ->
                                    todasLasTareas = tareas
                                    mostrarDialogo = false
                                }
                            )
                        }) {
                            Text("Mensual")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            actualizarFrecuencia(
                                db = firestore,
                                tarea = tareaSeleccionada,
                                nuevaFrecuencia = null,
                                onCompletion = { tareas ->
                                    todasLasTareas = tareas
                                    mostrarDialogo = false
                                }
                            )
                        }) {
                            Text("Quitar programación")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { mostrarDialogo = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}


fun cargarTodasLasTareasDesdeFirestore(
    db: FirebaseFirestore,
    onSuccess: (List<Tarea>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("MisTareas")
        .get() // Ya no filtra por zona
        .addOnSuccessListener { result ->
            val listaTareas = result.mapNotNull { document ->
                val nombre = document.getString("nombre") ?: return@mapNotNull null
                val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                val zona = document.getString("zona") ?: "Sin Zona"
                val frecuencia = document.getString("frecuencia") ?: "" // Si no existe, asigna ""
                val subzona = document.getString("subzona") ?: "Sin Subzona" // Traemos la subzona
                Tarea(nombre, puntos, zona, frecuencia, subzona) // Asegúrate de que subzona esté en Tarea
            }
            onSuccess(listaTareas)
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

@Composable
fun MostrarTareasPorZona(
    tareas: List<Tarea>,
    onProgramarTarea: (Tarea) -> Unit // Agrega este parámetro
) {
    // Agrupa las tareas por zona
    val tareasPorZona = tareas.groupBy { it.zona ?: "Sin Zona" }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        tareasPorZona.forEach { (zona, tareasZona) ->
            item {
                Text(
                    text = zona,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            // Pasa onProgramarTarea a TareaItem
            items(tareasZona) { tarea ->
                TareaItem(tarea = tarea, onProgramarTarea = onProgramarTarea)
            }
        }
    }
}

@Composable
fun TareaItem(tarea: Tarea, onProgramarTarea: (Tarea) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000).copy(alpha = 0.1f))
            .padding(8.dp)
            .clickable { onProgramarTarea(tarea) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tarea.nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )

            // Mostrar la subzona solo si está definida y no es "Sin Subzona"
            if (!tarea.subzona.isNullOrBlank() && tarea.subzona != "Sin Subzona") {
                Text(
                    text = tarea.subzona,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

fun actualizarFrecuencia(db: FirebaseFirestore, tarea: Tarea?, nuevaFrecuencia: String?) {
    if (tarea == null) return

    db.collection("MisTareas")
        .whereEqualTo("nombre", tarea.nombre) // O usa el ID único si tienes uno
        .get()
        .addOnSuccessListener { result ->
            if (result.isEmpty) return@addOnSuccessListener

            val docRef = result.documents.first().reference
            docRef.update("frecuencia", nuevaFrecuencia)
                .addOnSuccessListener {
                    Log.d("Firestore", "Frecuencia actualizada a $nuevaFrecuencia")
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Error al actualizar la frecuencia", it)
                }
        }
}

fun actualizarFrecuencia(
    db: FirebaseFirestore,
    tarea: Tarea?,
    nuevaFrecuencia: String?,
    onCompletion: (List<Tarea>) -> Unit // Callback para recargar las tareas
) {
    if (tarea == null) return

    db.collection("MisTareas")
        .whereEqualTo("nombre", tarea.nombre) // Ajusta según tu esquema
        .get()
        .addOnSuccessListener { result ->
            if (result.isEmpty) return@addOnSuccessListener

            val docRef = result.documents.first().reference
            docRef.update("frecuencia", nuevaFrecuencia)
                .addOnSuccessListener {
                    // Recargar todas las tareas tras la actualización
                    cargarTodasLasTareasDesdeFirestore(db, onCompletion, {})
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Error al actualizar la frecuencia", it)
                }
        }
}