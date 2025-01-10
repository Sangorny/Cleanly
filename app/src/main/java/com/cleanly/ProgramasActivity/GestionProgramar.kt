package com.cleanly.ProgramasActivity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

object GestionProgramar {

    const val CHANNEL_ID = "TEST_NOTIFICATION_CHANNEL"

    // Crea el canal de notificaciones
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Test Notifications"
            val descriptionText = "Channel for test notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Programa notificaciones basadas en la prioridad
    fun programarNotificacionesPorPrioridad(context: Context) {
        val db = FirebaseFirestore.getInstance()
        db.collection("MisTareas")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val nombreTarea = document.getString("nombre") ?: "Tarea sin nombre"
                    val prioridad = document.getString("prioridad") ?: "Baja"

                    when (prioridad) {
                        "Urgente" -> scheduleNotification(context, nombreTarea, 30, TimeUnit.MINUTES)
                        "Normal" -> scheduleNotification(context, nombreTarea, 2, TimeUnit.HOURS)
                        "Baja" -> scheduleNotification(context, nombreTarea, 4, TimeUnit.HOURS)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GestionProgramar", "Error al cargar tareas: ${exception.message}")
            }
    }

    // Programa una notificación usando WorkManager
    private fun scheduleNotification(context: Context, taskName: String, interval: Long, timeUnit: TimeUnit) {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(interval, timeUnit)
            .setInputData(workDataOf("taskName" to taskName)) // Pasar datos a Worker
            .addTag(taskName) // Tag para identificar la tarea
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            taskName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        Log.d("GestionProgramar", "Notificación programada para $taskName con intervalo de $interval ${timeUnit.toString().lowercase()}.")
    }
}

// Clase NotificationWorker para enviar notificaciones
class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val taskName = inputData.getString("taskName") ?: "Tarea sin nombre"

        // Verificar permiso antes de enviar la notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("NotificationWorker", "Permiso para notificaciones no concedido")
            return Result.failure()
        }

        // Enviar notificación
        val notification = NotificationCompat.Builder(applicationContext, GestionProgramar.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio de tarea")
            .setContentText("Tarea: $taskName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(System.currentTimeMillis().toInt(), notification)

        return Result.success()
    }
}
