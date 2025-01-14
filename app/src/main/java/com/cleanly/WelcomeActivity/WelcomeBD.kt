package com.cleanly.shared

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Tarea(
    val nombre: String,
    val puntos: Int,
    val usuario: String? = null,
    val completadoPor: String? = null
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
                        usuario = document.getString("usuario"),
                        completadoPor = document.getString("completadoPor") // Incluir completadoPor
                    )
                }
                onComplete(tareas)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    fun asignarTareaAFirestore(
        db: FirebaseFirestore,
        tarea: Tarea,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val usuarioActual = currentUser?.displayName ?: "Usuario desconocido"

        db.collection("MisTareas")
            .whereEqualTo("nombre", tarea.nombre) // Filtrar por nombre de tarea
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onFailure()
                    return@addOnSuccessListener
                }
                val docRef = result.documents.first().reference
                docRef.update("usuario", usuarioActual) // Actualizar el campo "usuario"
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure() }
            }
            .addOnFailureListener { onFailure() }
    }

    fun actualizarCompletadoPor(
        db: FirebaseFirestore,
        tarea: Tarea,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val usuarioActual = currentUser?.displayName ?: "Usuario desconocido" // Obtener el nombre del usuario logueado

        db.collection("MisTareas")
            .whereEqualTo("nombre", tarea.nombre) // Filtrar por nombre de tarea
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onFailure()
                    return@addOnSuccessListener
                }
                val docRef = result.documents.first().reference
                docRef.update("completadoPor", usuarioActual) // Actualizar el campo "completadoPor"
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure() }
            }
            .addOnFailureListener { onFailure() }
    }



}

