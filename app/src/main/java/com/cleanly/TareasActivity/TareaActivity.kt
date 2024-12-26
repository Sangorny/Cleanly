package com.cleanly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cleanly.TareasActivity.CRUDTareas
import com.cleanly.TareasActivity.TareasBD
import com.cleanly.WelcomeActivity.WelcomeBarra
import com.cleanly.ZonaActivity.ZonasActivity
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



@OptIn(ExperimentalMaterial3Api::class)
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
fun TareaScreen(navController: NavHostController, zonaSeleccionada: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val taskList = remember { mutableStateListOf<Pair<String, Int>>() }
    val updateTaskList: (List<Pair<String, Int>>) -> Unit = { newList ->
        taskList.clear()
        taskList.addAll(newList)
    }

    LaunchedEffect(zonaSeleccionada) {
        TareasBD.cargarTareasDesdeFirestore(db, zonaSeleccionada) { listaTareas ->
            updateTaskList(listaTareas.map { it.nombre to it.puntos })
        }
    }

    Scaffold(
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
                    TareasBD.agregarTareaAFirestore(
                        db = db,
                        nombre = "Nueva tarea",
                        puntos = 50,
                        zona = zonaSeleccionada,
                        context = context
                    ) {
                        TareasBD.cargarTareasDesdeFirestore(db, zonaSeleccionada) { listaTareas ->
                            updateTaskList(listaTareas.map { it.nombre to it.puntos })
                        }
                    }
                },
                onDelete = { reloadTaskList(db, zonaSeleccionada, updateTaskList) },
                onList = { reloadTaskList(db, zonaSeleccionada, updateTaskList) },
                onEdit = { reloadTaskList(db, zonaSeleccionada, updateTaskList) },
                onTaskListUpdated = updateTaskList,
                zonaSeleccionada = zonaSeleccionada
            )
        }
    }
}

private fun reloadTaskList(
    db: FirebaseFirestore,
    zonaSeleccionada: String,
    updateTaskList: (List<Pair<String, Int>>) -> Unit
) {
    TareasBD.cargarTareasDesdeFirestore(db, zonaSeleccionada) { listaTareas ->
        updateTaskList(listaTareas.map { it.nombre to it.puntos })
    }
}
