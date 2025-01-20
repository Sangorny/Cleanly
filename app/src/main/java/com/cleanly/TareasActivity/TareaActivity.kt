package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cleanly.TareasActivity.CRUDTareas
import com.cleanly.TareasActivity.Tarea
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.firestore.FirebaseFirestore


class TareaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zonaSeleccionada = intent.getStringExtra("zona") ?: "" // Obtener la zona seleccionada

        setContent {
            CleanlyTheme {
                val navController = rememberNavController() // Crear NavController
                MainScreen(
                    onNavigateToTarea = { tarea ->
                        // Manejar navegación desde tareas (puede estar vacío si no es necesario)
                    },
                    onNavigateToZonas = {
                        // Navegación a ZonasActivity
                        navController.navigate("zonas")
                    },
                    zonaSeleccionada = zonaSeleccionada // Pasar la zona seleccionada
                )
            }
        }
    }
}


@Composable
fun TareaScreen(navController: NavHostController, groupId: String, zonaSeleccionada: String) {
    val db = FirebaseFirestore.getInstance()
    val taskList = remember { mutableStateListOf<Tarea>() }
    val updateTaskList: (List<Tarea>) -> Unit = { newList ->
        taskList.clear()
        taskList.addAll(newList)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Función local para cargar tareas desde Firestore
    fun cargarTareasDesdeFirestore(
        db: FirebaseFirestore,
        groupId: String,
        zonaSeleccionada: String,
        onSuccess: (List<Tarea>) -> Unit
    ) {
        db.collection("grupos")
            .document(groupId)
            .collection("tareas")
            .whereEqualTo("zona", zonaSeleccionada)
            .get()
            .addOnSuccessListener { result ->
                val listaTareas = result.mapNotNull { document ->
                    val nombre = document.getString("nombre") ?: return@mapNotNull null
                    val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                    val subzona = document.getString("subzona") ?: "Sin Subzona"
                    Tarea(nombre, puntos, zonaSeleccionada, subzona)
                }
                onSuccess(listaTareas)
            }
            .addOnFailureListener {
                // Manejar el error, si es necesario
            }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CRUDTareas(
                db = db,
                taskList = taskList,
                onCreate = {
                    cargarTareasDesdeFirestore(db, groupId, zonaSeleccionada) { listaTareas ->
                        updateTaskList(listaTareas)
                    }
                },
                onDelete = { reloadTaskList(db, groupId, zonaSeleccionada, updateTaskList) },
                onList = { reloadTaskList(db, groupId, zonaSeleccionada, updateTaskList) },
                onEdit = { reloadTaskList(db, groupId, zonaSeleccionada, updateTaskList) },
                onTaskListUpdated = updateTaskList,
                groupId = groupId,
                zonaSeleccionada = zonaSeleccionada
            )
        }
    }
}

// Función para recargar la lista de tareas
private fun reloadTaskList(
    db: FirebaseFirestore,
    groupId: String,
    zonaSeleccionada: String,
    updateTaskList: (List<Tarea>) -> Unit
) {
    db.collection("grupos")
        .document(groupId)
        .collection("tareas")
        .whereEqualTo("zona", zonaSeleccionada)
        .get()
        .addOnSuccessListener { result ->
            val listaTareas = result.mapNotNull { document ->
                val nombre = document.getString("nombre") ?: return@mapNotNull null
                val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                val subzona = document.getString("subzona") ?: "Sin Subzona"
                Tarea(nombre, puntos, zonaSeleccionada, subzona)
            }
            updateTaskList(listaTareas)
        }
        .addOnFailureListener {
            // Manejar el error, si es necesario
        }
}