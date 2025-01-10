package com.cleanly.ProgramasActivity


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore

class TaskSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()

        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                val now = System.currentTimeMillis()

                for (document in result) {
                    val nombreTarea = document.getString("nombre") ?: "Tarea sin nombre"
                    val prioridad = document.getString("prioridad") ?: "Baja"
                    val ultimaNotificacion = document.getTimestamp("ultimaNotificacion")?.toDate()?.time

                    // Determina el intervalo en milisegundos según la prioridad
                    val intervalo = when (prioridad) {
                        "Urgente" -> 60 * 60 * 1000 // 1 hora
                        "Normal" -> 2 * 60 * 60 * 1000 // 2 horas
                        "Baja" -> 4 * 60 * 60 * 1000 // 4 horas
                        else -> 0
                    }

                    // Si no tiene ultimaNotificacion o ya pasó el intervalo, enviar notificación
                    if (ultimaNotificacion == null || now - ultimaNotificacion >= intervalo) {
                        enviarNotificacion(nombreTarea, prioridad)

                        // Actualiza la última notificación en Firebase
                        document.reference.update("ultimaNotificacion", com.google.firebase.Timestamp.now())
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TaskSyncWorker", "Error al sincronizar: ${exception.message}")
            }

        return Result.success()
    }

    private fun enviarNotificacion(nombreTarea: String, prioridad: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("TaskSyncWorker", "Permiso para notificaciones no concedido")
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, GestionProgramar.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio de Tarea")
            .setContentText("Tarea: $nombreTarea - Prioridad: $prioridad")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(System.currentTimeMillis().toInt(), notification)
    }
}