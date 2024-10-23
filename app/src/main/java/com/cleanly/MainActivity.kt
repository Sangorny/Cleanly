package com.cleanly

import android.os.Bundle
import android.text.InputType
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cleanly.ui.theme.CleanlyTheme
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.cleanly.TareasAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TareasAdapter
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Iniciar BBDD
        db = Firebase.firestore
        // Verificar si las tareas por defecto están creadas
        checkIfDefaultTasksExist()
        // Cargar datos de BBDD!
        recyclerView = findViewById(R.id.recyclerViewTareas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Pop-Up que muestra al pulsar el botón de tareas
        val btnAgregarTarea: Button = findViewById(R.id.btnAgregarTarea)
        btnAgregarTarea.setOnClickListener {
            mostrarDialogoAgregarTarea()
        }
        cargarTareasDesdeFirestore()
    }

// -------- METODOS -----------

    // METODO para mostrar el dialogo de la tarea
    private fun mostrarDialogoAgregarTarea() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar Nueva Tarea")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val inputNombre = EditText(this)
        inputNombre.hint = "Nombre de la tarea"
        layout.addView(inputNombre)
        val inputPuntos = EditText(this)
        inputPuntos.hint = "Puntos (10-40)"
        inputPuntos.inputType = InputType.TYPE_CLASS_NUMBER
        layout.addView(inputPuntos)
        builder.setView(layout)

        // Botón que guarda los datos y llama a otro método para guardar
        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nombre = inputNombre.text.toString()
            val puntos = inputPuntos.text.toString().toIntOrNull() ?: 10

            if (nombre.isNotEmpty() && puntos in 10..40) {
                agregarTareaAFirestore(nombre, puntos)
            } else {
                Toast.makeText(this, "Introduce un nombre válido y puntos entre 10-40", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        // Botón de Cancelar
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
    // METODO que agrega a BBDD
    private fun agregarTareaAFirestore(nombre: String, puntos: Int) {
        val tarea = hashMapOf(
            "nombre" to nombre,
            "completadoPor" to "",  // Vacío por defecto
            "completadoEn" to null,  // Nulo por defecto
            "puntos" to puntos
        )
        db.collection("MisTareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea añadida correctamente", Toast.LENGTH_SHORT).show()
                cargarTareasDesdeFirestore()  // Actualizar la lista de tareas
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al añadir tarea", Toast.LENGTH_SHORT).show()
            }
    }
    // MÉTODO que carga las tareas desde la BBDD
    private fun cargarTareasDesdeFirestore() {
        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                val listaTareas = mutableListOf<Tarea>()
                for (document in result) {
                    val nombre = document.getString("nombre") ?: "Sin nombre"
                    val puntos = document.getLong("puntos")?.toInt() ?: 0
                    listaTareas.add(Tarea(nombre, puntos))
                }
                adapter = TareasAdapter(listaTareas)
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
            }
    }
    // MÉTODO para verificar si las tareas ya fueron creadas
    private fun checkIfDefaultTasksExist() {
        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    createDefaultTasks()
                } else {
                    Log.d(TAG, "Las tareas por defecto ya existen.")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error al verificar las tareas.", exception)
            }
    }
    // METODO creamos 5 tareas aleatorias
    private fun createDefaultTasks() {
        val tareas = listOf(
            "Lavar los platos",
            "Sacar la basura",
            "Limpiar el baño",
            "Barrer el suelo",
            "Hacer la cama"
        ).shuffled()
        for (tarea in tareas) {
            val puntos = (10..40).random()
            val data = hashMapOf(
                "nombre" to tarea,
                "completadoPor" to "",
                "completadoEn" to null,
                "puntos" to puntos
            )
            db.collection("MisTareas")
                .add(data)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Tarea añadida con ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al añadir tarea", e)
                }
        }
    }
}