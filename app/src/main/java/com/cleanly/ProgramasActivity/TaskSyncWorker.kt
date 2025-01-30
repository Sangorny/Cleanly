package com.cleanly.ProgramasActivity


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TaskSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // 1) Obtenemos el groupId desde inputData
        val groupId = inputData.getString("GROUP_ID")
            ?: return Result.failure()  // Si no viene, marcamos error o algo similar

        val db = FirebaseFirestore.getInstance()


        db.collection("grupos")
            .document(groupId)
            .collection("mistareas")
            .get()
            .addOnSuccessListener { result ->
                val now = Calendar.getInstance()
                val currentHour = now.get(Calendar.HOUR_OF_DAY)

                for (document in result) {
                    val nombreTarea = document.getString("nombre") ?: "Tarea sin nombre"
                    val prioridad = document.getString("prioridad") ?: "Baja"
                    val ultimaNotificacion = document.getTimestamp("ultimaNotificacion")?.toDate()?.time

                    // Lógica para priorizar las notificaciones
                    when (prioridad) {
                        "Urgente" -> {
                            if (currentHour in 8..23 &&
                                shouldSendNotification(ultimaNotificacion, 15 * 60 * 1000)) {

                                enviarNotificacion(nombreTarea, prioridad)
                                document.reference.update("ultimaNotificacion", com.google.firebase.Timestamp.now())
                            }
                        }
                        "Normal" -> {
                            if (currentHour in listOf(18, 20, 22) &&
                                shouldSendNotification(ultimaNotificacion, 2 * 60 * 60 * 1000)) {

                                enviarNotificacion(nombreTarea, prioridad)
                                document.reference.update("ultimaNotificacion", com.google.firebase.Timestamp.now())
                            }
                        }
                        "Baja" -> {
                            // No hacer nada para prioridad Baja
                        }
                    }
                }

                // Programar el siguiente Worker, reenviándole el groupId
                programarProximoWorker(applicationContext, groupId)
            }
            .addOnFailureListener { exception ->
                Log.e("TaskSyncWorker", "Error al sincronizar: ${exception.message}")
            }

        return Result.success()
    }

    private fun shouldSendNotification(ultimaNotificacion: Long?, intervalo: Long): Boolean {
        val now = System.currentTimeMillis()
        return ultimaNotificacion == null || now - ultimaNotificacion >= intervalo
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

    private fun programarProximoWorker(context: Context, groupId: String) {
        // 1) Calcula la próxima hora
        val now = Calendar.getInstance()
        now.set(Calendar.MINUTE, 30)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        val delayMillis = now.timeInMillis - System.currentTimeMillis()

        // 2) Pasamos el groupId también al siguiente Worker
        val inputData = workDataOf("GROUP_ID" to groupId)

        val workRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d("TaskSyncWorker", "Siguiente notificación programada en ${delayMillis / 1000 / 60} minutos.")
    }
}