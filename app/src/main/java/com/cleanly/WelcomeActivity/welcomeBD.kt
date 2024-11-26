package com.cleanly.shared

import com.google.firebase.firestore.FirebaseFirestore

data class Tarea(
    val nombre: String,
    val puntos: Int,
    val usuario: String? // El usuario puede ser null si no está asignado
)

object welcomeBD {
    fun cargarTareasDesdeFirestore(
        db: FirebaseFirestore,
        onComplete: (List<Tarea>) -> Unit
    ) {
        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                val tareas = result.map { document ->
                    Tarea(
                        nombre = document.getString("nombre") ?: "Tarea Sin Nombre",
                        puntos = document.getLong("puntos")?.toInt() ?: 0,
                        usuario = document.getString("usuario") // Puede ser null si no está asignado
                    )
                }
                onComplete(tareas)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
}
