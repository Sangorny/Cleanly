package com.cleanly.TareasActivity

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

object TareasBD {

    fun agregarTareaAFirestore(
        db: FirebaseFirestore,
        nombre: String,
        puntos: Int,
        zona: String, // Agregar la zona como parámetro
        context: Context,
        onSuccess: () -> Unit
    ) {
        val tarea = hashMapOf(
            "nombre" to nombre,
            "puntos" to puntos,
            "zona" to zona, // Incluir la zona en la tarea
            "completadoPor" to "",
            "completadoEn" to null
        )
        db.collection("MisTareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(context, "Tarea añadida correctamente a la zona $zona", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al añadir tarea en la zona $zona", Toast.LENGTH_SHORT).show()
            }
    }

    fun cargarTareasDesdeFirestore(
        db: FirebaseFirestore,
        zonaSeleccionada: String,
        onSuccess: (List<Tarea>) -> Unit
    ) {
        db.collection("MisTareas")
            .whereEqualTo("zona", zonaSeleccionada) // Filtrar por la zona seleccionada
            .get()
            .addOnSuccessListener { result ->
                val listaTareas = result.mapNotNull { document ->
                    val nombre = document.getString("nombre") ?: return@mapNotNull null
                    val puntos = document.getLong("puntos")?.toInt() ?: return@mapNotNull null
                    Tarea(nombre, puntos, zonaSeleccionada) // Crea el objeto Tarea
                }
                onSuccess(listaTareas)
            }
            .addOnFailureListener {
                // Manejo del error
            }
    }

    fun eliminarTareasDeFirestore(
        db: FirebaseFirestore,
        nombresDeTareas: List<String>,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
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
        zona: String, // Agregar la zona si es necesario actualizarla también
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ) {
        db.collection("MisTareas")
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
                        "zona" to zona // Actualizar la zona si es necesario
                    )
                    db.collection("MisTareas").document(documentId)
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
