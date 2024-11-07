package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.cleanly.TareasActivity.CRUDTareas
import com.cleanly.ui.theme.CleanlyTheme
import com.cleanly.TareasActivity.TareasBD
import com.google.firebase.firestore.FirebaseFirestore

class TareaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FirebaseFirestore.getInstance()

        setContent {
            CleanlyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val taskList = remember { mutableStateListOf<Pair<String, Int>>() }

                    val updateTaskList: (List<Pair<String, Int>>) -> Unit = { newList ->
                        taskList.clear()
                        taskList.addAll(newList)
                    }

                    TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                        updateTaskList(listaTareas.map { it.nombre to it.puntos })
                    }

                    CRUDTareas(
                        db = db,
                        taskList = taskList,
                        onCreate = { reloadTaskList(db, updateTaskList) },
                        onDelete = { reloadTaskList(db, updateTaskList) },
                        onList = { reloadTaskList(db, updateTaskList) },
                        onEdit = { reloadTaskList(db, updateTaskList) },
                        onTaskListUpdated = updateTaskList
                    )
                }
            }
        }
    }

    private fun reloadTaskList(
        db: FirebaseFirestore,
        updateTaskList: (List<Pair<String, Int>>) -> Unit
    ) {
        TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            updateTaskList(listaTareas.map { it.nombre to it.puntos })
        }
    }
}
