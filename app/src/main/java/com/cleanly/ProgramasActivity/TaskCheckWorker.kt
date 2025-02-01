package com.cleanly.ProgramasActivity

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore

class TaskCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // 1) Obtener groupId desde inputData
        val groupId = inputData.getString("GROUP_ID") ?: run {
            Log.e("TaskCheckWorker", "ERROR: GROUP_ID no recibido")
            return Result.failure()
        }
        Log.d("TaskCheckWorker", "Revisando tareas para groupId: $groupId")

        val db = FirebaseFirestore.getInstance()

        // 2) Consultar la colección de tareas
        db.collection("grupos")
            .document(groupId)
            .collection("mistareas")
            .get()
            .addOnSuccessListener { result ->
                Log.d("TaskCheckWorker", "Tareas obtenidas: ${result.size()}")

                for (document in result) {
                    // Suponiendo que la tarea se considera completada si el campo "completadoPor" no es nulo o vacío.
                    val completadoPor = document.getString("completadoPor")
                    if (!completadoPor.isNullOrEmpty()) {
                        // Actualizamos el campo "estado" a "completada" para que no se notifique
                        document.reference.update("estado", "completada")
                            .addOnSuccessListener {
                                Log.d("TaskCheckWorker", "Tarea '${document.getString("nombre")}' marcada como completada")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("TaskCheckWorker", "Error actualizando tarea: ${exception.message}")
                            }
                    } else {
                        // Opcional: Podrías actualizar el estado de las tareas incompletas a "pendiente" (o dejarlo sin cambios)
                        document.reference.update("estado", "pendiente")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TaskCheckWorker", "Error al obtener tareas: ${exception.message}")
            }

        return Result.success()
    }
}