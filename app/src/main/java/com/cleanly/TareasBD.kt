package com.cleanly.utils

import android.content.Context
import android.widget.Toast
import com.cleanly.Tarea
import com.google.firebase.firestore.FirebaseFirestore

object TareasBD {

    fun agregarTareaAFirestore(
        db: FirebaseFirestore,
        nombre: String,
        puntos: Int,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val tarea = hashMapOf(
            "nombre" to nombre,
            "completadoPor" to "",
            "completadoEn" to null,
            "puntos" to puntos
        )
        db.collection("MisTareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(context, "Tarea añadida correctamente", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al añadir tarea", Toast.LENGTH_SHORT).show()
            }
    }

    fun cargarTareasDesdeFirestore(
        db: FirebaseFirestore,
        onSuccess: (List<Tarea>) -> Unit
    ) {
        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                val listaTareas = result.mapNotNull { document ->
                    val nombre = document.getString("nombre") ?: return@mapNotNull null
                    val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                    Tarea(nombre, puntos)
                }
                onSuccess(listaTareas)
            }
            .addOnFailureListener {
                // Manejo de errores si se requiere
            }
    }

    fun checkIfDefaultTasksExist(
        db: FirebaseFirestore,
        TAG: String,
        onDefaultTasksNeeded: () -> Unit
    ) {
        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onDefaultTasksNeeded()
                }
            }
            .addOnFailureListener { e ->
                println("Error al verificar las tareas: $e")
            }
    }
}