package com.cleanly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cleanly.R
data class Tarea(
    val nombre: String,
    val puntos: Int
)
class TareasAdapter(private val listaTareas: List<Tarea>) : RecyclerView.Adapter<TareasAdapter.TareaViewHolder>() {

    // Clase interna con referencia a las vistas
    class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreTarea: TextView = itemView.findViewById(R.id.tareaNombre)
        val puntosTarea: TextView = itemView.findViewById(R.id.tareaPuntos)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tareaActual = listaTareas[position]
        holder.nombreTarea.text = tareaActual.nombre
        holder.puntosTarea.text = "Puntos: ${tareaActual.puntos}"
    }
    override fun getItemCount() = listaTareas.size
}