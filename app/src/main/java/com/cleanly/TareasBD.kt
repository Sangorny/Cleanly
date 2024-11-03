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

    fun eliminarTareasDeFirestore(
        db: FirebaseFirestore,
        nombresDeTareas: List<String>,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {} // Aquí cambiamos a una lambda sin parámetros
    ) {
        val collectionRef = db.collection("MisTareas")

        nombresDeTareas.forEach { nombre ->
            collectionRef.whereEqualTo("nombre", nombre)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(context, "No se encontró la tarea para eliminar: $nombre", Toast.LENGTH_SHORT).show()
                    } else {
                        querySnapshot.documents.forEach { document ->
                            collectionRef.document(document.id)
                                .delete()
                                .addOnSuccessListener {
                                    onSuccess()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al eliminar tarea: $nombre", Toast.LENGTH_SHORT).show()
                                    onFailure()
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    onFailure()
                }
        }
    }

    fun actualizarTareaEnFirestore(
        db: FirebaseFirestore,
        nombreOriginal: String,
        nuevoNombre: String,
        nuevosPuntos: Int,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {} // onFailure sin parámetros
    ) {
        db.collection("MisTareas")
            .whereEqualTo("nombre", nombreOriginal)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(context, "No se encontró la tarea original para actualizar", Toast.LENGTH_SHORT).show()
                    onFailure()
                } else {
                    // Actualizar el primer documento encontrado con el nombre original
                    val documentId = querySnapshot.documents.first().id
                    val tareaActualizada = hashMapOf(
                        "nombre" to nuevoNombre,
                        "puntos" to nuevosPuntos
                    )
                    db.collection("MisTareas").document(documentId)
                        .update(tareaActualizada as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Tarea actualizada correctamente", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al actualizar tarea", Toast.LENGTH_SHORT).show()
                            onFailure() // Llamamos a onFailure sin pasar parámetros
                        }
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }
}