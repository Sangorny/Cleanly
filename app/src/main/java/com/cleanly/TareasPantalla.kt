package com.cleanly.utils

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

object TareasPantalla {
    fun mostrarDialogoAgregarTarea(
        context: Context,
        db: FirebaseFirestore,
        onSuccess: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Agregar Nueva Tarea")

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val inputNombre = EditText(context).apply { hint = "Nombre de la tarea" }
        val inputPuntos = EditText(context).apply {
            hint = "Puntos (10-40)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(inputNombre)
        layout.addView(inputPuntos)
        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nombre = inputNombre.text.toString()
            val puntos = inputPuntos.text.toString().toIntOrNull() ?: 10
            if (nombre.isNotEmpty() && puntos in 10..40) {
                TareasBD.agregarTareaAFirestore(db, nombre, puntos, context, onSuccess)
            } else {
                Toast.makeText(context, "Introduce un nombre válido y puntos entre 10-40", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    fun createDefaultTasks(db: FirebaseFirestore, TAG: String) {
        val tareas = listOf(
            "Lavar los platos",
            "Sacar la basura",
            "Limpiar el baño",
            "Barrer el suelo",
            "Hacer la cama"
        ).shuffled()

        tareas.forEach { tarea ->
            val puntos = Random.nextInt(10, 41)
            val data = hashMapOf(
                "nombre" to tarea,
                "completadoPor" to "",
                "completadoEn" to null,
                "puntos" to puntos
            )
            db.collection("MisTareas")
                .add(data)
                .addOnSuccessListener { documentReference ->
                    println("Tarea añadida con ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    println("Error al añadir tarea: $e")
                }
        }
    }
}