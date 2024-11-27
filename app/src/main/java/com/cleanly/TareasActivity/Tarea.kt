package com.cleanly.TareasActivity

data class Tarea(
    val nombre: String,
    val puntos: Int,
    var completadoPor: String? = null,
    var completadoEn: String? = null
)
