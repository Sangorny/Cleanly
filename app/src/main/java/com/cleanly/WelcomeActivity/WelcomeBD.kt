package com.cleanly.shared

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Tarea(
    val nombre: String,
    val usuario: String? = null,
    val completadoPor: String? = null // Nuevo campo agregado
)

object welcomeBD {
    /**
     * Cargar todas las tareas desde la subcolección "tareas" en el grupo especificado por groupId.
     */
    fun cargarTareasDesdeFirestore(
        db: FirebaseFirestore,
        groupId: String,
        onSuccess: (List<Tarea>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        // Referencia al grupo
        val groupRef = db.collection("grupos").document(groupId)

        // Consulta a la subcolección "tareas" sin filtros adicionales
        groupRef.collection("mistareas")
            .get()
            .addOnSuccessListener { result ->
                val listaTareas = result.mapNotNull { document ->
                    val nombre = document.getString("nombre") ?: return@mapNotNull null
                    val usuario = document.getString("usuario") ?: ""
                    val completadoPor =
                        document.getString("completadoPor") // Lee el campo completadoPor
                    Tarea(nombre, usuario, completadoPor)
                }
                onSuccess(listaTareas)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Asignar una tarea al usuario actual.
     */
    fun asignarTareaAlUsuario(
        db: FirebaseFirestore,
        groupId: String,
        tarea: Tarea,
        onResult: (Boolean) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val usuarioUID = currentUser?.uid ?: return onResult(false) // Obtiene el UID del usuario

        db.collection("grupos")
            .document(groupId)
            .collection("mistareas")
            .whereEqualTo("nombre", tarea.nombre)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onResult(false)
                    return@addOnSuccessListener
                }

                val docRef = result.documents.first().reference
                docRef.update("usuario", usuarioUID) // Actualiza el campo usuario con el UID
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Completar la tarea
     */
    fun completarTarea(
        db: FirebaseFirestore,
        groupId: String,
        tarea: Tarea,
        onResult: (Boolean) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val usuarioUID = currentUser?.uid ?: return onResult(false) // Obtén el UID del usuario

        db.collection("grupos")
            .document(groupId)
            .collection("mistareas")
            .whereEqualTo("nombre", tarea.nombre)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onResult(false)
                    return@addOnSuccessListener
                }

                val docRef = result.documents.first().reference
                docRef.update(
                    "completadoPor",
                    usuarioUID
                ) // Actualiza el campo completadoPor con el UID
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }
}