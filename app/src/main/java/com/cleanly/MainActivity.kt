package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.cleanly.ui.theme.CleanlyTheme
import com.cleanly.utils.TareasBD
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firestore
        val db = Firebase.firestore

        setContent {
            CleanlyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Estado de la lista de tareas
                    val taskList = remember { mutableStateListOf<Pair<String, Int>>() }

                    // Función para actualizar la lista de tareas
                    val updateTaskList: (List<Pair<String, Int>>) -> Unit = { newList ->
                        taskList.clear()
                        taskList.addAll(newList)
                    }

                    // Cargar tareas inicialmente desde Firestore
                    TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                        updateTaskList(listaTareas.map { it.nombre to it.puntos })
                    }

                    // Invocar la pantalla CRUDTareas con los eventos necesarios
                    CRUDTareas(
                        db = db,
                        taskList = taskList,
                        onCreate = {
                            TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                                updateTaskList(listaTareas.map { it.nombre to it.puntos })
                            }
                        },
                        onDelete = {
                            TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                                updateTaskList(listaTareas.map { it.nombre to it.puntos })
                            }
                        },
                        onList = {
                            TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                                updateTaskList(listaTareas.map { it.nombre to it.puntos })
                            }
                        },
                        onEdit = {
                            TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                                updateTaskList(listaTareas.map { it.nombre to it.puntos })
                            }
                        },
                        onTaskListUpdated = updateTaskList
                    )
                }
            }
        }
    }
}