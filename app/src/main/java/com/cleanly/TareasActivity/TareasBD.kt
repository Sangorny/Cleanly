package com.cleanly.TareasActivity

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

object TareasBD {


    fun agregarTareaAFirestore(
        db: FirebaseFirestore,
        groupId: String,
        nombre: String,
        puntos: Int,
        zona: String,
        subzona: String,
        prioridad: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        // Verificar datos antes de enviarlos a Firestore
        if (nombre.isBlank() || zona.isBlank() || prioridad.isBlank()) {
            Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear mapa de la tarea
        val tarea = hashMapOf(
            "nombre" to nombre,
            "puntos" to puntos,
            "zona" to zona,
            "subzona" to if (subzona.isBlank()) null else subzona,
            "prioridad" to prioridad,
            "completadoPor" to null,
            "completadoEn" to null
        )

        // Verificar si el documento del grupo existe
        val groupRef = db.collection("grupos").document(groupId)
        groupRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                groupRef.set(mapOf("createdAt" to System.currentTimeMillis()))
                    .addOnSuccessListener {
                        // Añadir tarea a la subcolección "mistareas"
                        groupRef.collection("mistareas")
                            .add(tarea)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Tarea añadida correctamente", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al añadir tarea", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al inicializar el grupo", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Añadir tarea directamente si el documento del grupo ya existe
                groupRef.collection("mistareas")
                    .add(tarea)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Tarea añadida correctamente", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al añadir tarea", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al verificar el grupo", Toast.LENGTH_SHORT).show()
        }
    }


    fun cargarTareasDesdeFirestore(
        db: FirebaseFirestore,
        groupId: String, // Identificador del grupo
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
                    val prioridad = document.getString("prioridad") ?: "Baja"
                    Tarea(nombre, puntos, zonaSeleccionada, subzona, prioridad)
                }
                onSuccess(listaTareas)
            }
            .addOnFailureListener {
                // Manejo del error
            }
    }

    fun eliminarTareasDeFirestore(
        db: FirebaseFirestore,
        groupId: String, // Identificador del grupo
        nombresDeTareas: List<String>,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ) {
        val collectionRef = db.collection("grupos").document(groupId).collection("tareas")

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
        groupId: String, // Identificador del grupo
        nombreOriginal: String,
        nuevoNombre: String,
        nuevosPuntos: Int,
        zona: String,
        nuevaSubzona: String,
        nuevaPrioridad: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ) {
        db.collection("grupos")
            .document(groupId)
            .collection("tareas")
            .whereEqualTo("nombre", nombreOriginal)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(context, "No se encontró la tarea original para actualizar", Toast.LENGTH_SHORT).show()
                    onFailure()
                } else {
                    val documentId = querySnapshot.documents.first().id
                    val tareaActualizada = hashMapOf(
                        "nombre" to nuevoNombre,
                        "puntos" to nuevosPuntos,
                        "zona" to zona,
                        "subzona" to nuevaSubzona,
                        "prioridad" to nuevaPrioridad
                    )
                    db.collection("grupos").document(groupId).collection("tareas").document(documentId)
                        .update(tareaActualizada as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Tarea actualizada correctamente", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al actualizar tarea", Toast.LENGTH_SHORT).show()
                            onFailure()
                        }
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }
}