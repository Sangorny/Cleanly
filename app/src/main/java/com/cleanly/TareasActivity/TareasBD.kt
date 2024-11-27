package com.cleanly.TareasActivity

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Estadisticas(
    val tareasCompletadas: Int = 0,
    val puntosTotales: Int = 0
) {
    // Constructor sin argumentos necesario para Firebase
    constructor() : this(0, 0)
}

object TareasBD {

    // Función para agregar una nueva tarea en Firestore
    fun agregarTareaAFirestore(
        db: FirebaseFirestore,
        nombre: String,
        puntos: Int,
        context: Context,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val tarea = hashMapOf(
            "nombre" to nombre,
            "puntos" to puntos,
            "completadoPor" to null,
            "completadoEn" to null
        )

        db.collection("MisTareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(context, "Tarea añadida correctamente", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al añadir tarea", Toast.LENGTH_SHORT).show()
                onFailure()
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
    }

    fun actualizarEstadisticas(
        db: FirebaseFirestore,
        puntosGanados: Int,
        onSuccess: (Estadisticas) -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val estadisticasRef = db.collection("Estadisticas").document("global")

        estadisticasRef.get()
            .addOnSuccessListener { document ->
                val estadisticas = document.toObject(Estadisticas::class.java)
                val nuevasEstadisticas = estadisticas?.copy(
                    tareasCompletadas = (estadisticas.tareasCompletadas + 1),
                    puntosTotales = (estadisticas.puntosTotales + puntosGanados)
                ) ?: Estadisticas(tareasCompletadas = 1, puntosTotales = puntosGanados)

                estadisticasRef.set(nuevasEstadisticas)
                    .addOnSuccessListener {
                        onSuccess(nuevasEstadisticas)
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            null,
                            "Error al actualizar estadísticas: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onFailure()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    null,
                    "Error al obtener estadísticas: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onFailure()
            }
    }

    fun obtenerEstadisticas(
        db: FirebaseFirestore,
        onSuccess: (Estadisticas) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("Estadisticas").document("global")
            .get()
            .addOnSuccessListener { document ->
                val estadisticas = document.toObject(Estadisticas::class.java)
                if (estadisticas != null) {
                    onSuccess(estadisticas)
                } else {
                    onFailure(Exception("Estadísticas no encontradas"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun marcarTareaComoCompletada(
        db: FirebaseFirestore,
        tareaNombre: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        db.collection("MisTareas")
            .whereEqualTo("nombre", tareaNombre)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val tarea = querySnapshot.documents.first()
                    db.collection("MisTareas").document(tarea.id)
                        .update("completadoPor", "usuario", "completadoEn", System.currentTimeMillis())
                        .addOnSuccessListener {
                            val puntos = tarea.getLong("puntos")?.toInt() ?: 0
                            actualizarEstadisticas(db, puntos, onSuccess = {
                                Toast.makeText(null, "Estadísticas actualizadas", Toast.LENGTH_SHORT).show()
                            })
                            onSuccess()
                        }
                        .addOnFailureListener {
                            Toast.makeText(null, "Error al completar tarea", Toast.LENGTH_SHORT).show()
                            onFailure()
                        }
                } else {
                    Toast.makeText(null, "Tarea no encontrada", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
            }
            .addOnFailureListener {
                Toast.makeText(null, "Error al marcar tarea", Toast.LENGTH_SHORT).show()
                onFailure()
            }
    }

    fun eliminarTareasDeFirestore(
        db: FirebaseFirestore,
        nombresDeTareas: List<String>,
        context: Context,
        onSuccess: () -> Unit = {},
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
        context: Context,
        onSuccess: () -> Unit = {},
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
                            onFailure()
                        }
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    fun guardarFotoPerfilEnFirestore(db: FirebaseFirestore, uri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("Usuarios").document(userId)
            userRef.update("photoUrl", uri.toString())
                .addOnSuccessListener {
                    Log.d("Firestore", "Foto de perfil actualizada en Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error al actualizar la foto de perfil.", e)
                }
        }
    }


    fun obtenerFotoPerfilDesdeFirestore(db: FirebaseFirestore, onSuccess: (Uri?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("Usuarios").document(userId)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val photoUrl = document.getString("photoUrl")
                        onSuccess(photoUrl?.let { Uri.parse(it) })
                    } else {
                        onSuccess(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error al obtener la foto de perfil.", e)
                    onSuccess(null)
                }
        } else {
            onSuccess(null)
        }
    }

}










