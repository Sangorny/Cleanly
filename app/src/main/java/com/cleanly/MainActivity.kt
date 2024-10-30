package com.cleanly

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cleanly.utils.TareasBD
import com.cleanly.utils.TareasPantalla
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TareasAdapter
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Firebase.firestore
        recyclerView = findViewById(R.id.recyclerViewTareas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val btnAgregarTarea: Button = findViewById(R.id.btnAgregarTarea)

        btnAgregarTarea.setOnClickListener {
            TareasPantalla.mostrarDialogoAgregarTarea(this, db) {
                cargarTareasDesdeFirestore()
            }
        }

        TareasBD.checkIfDefaultTasksExist(db, TAG) {
            TareasPantalla.createDefaultTasks(db, TAG)
        }

        cargarTareasDesdeFirestore()
    }

    private fun cargarTareasDesdeFirestore() {
        TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            adapter = TareasAdapter(listaTareas)
            recyclerView.adapter = adapter
        }
    }
    private fun trabajarConTareasMarcadas() {
        val tareasMarcadas = adapter.obtenerTareasMarcadas()

    }
}