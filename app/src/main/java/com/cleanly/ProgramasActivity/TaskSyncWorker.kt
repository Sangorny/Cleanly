package com.cleanly.ProgramasActivity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
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
        Log.d("TaskSyncWorker", "Worker iniciado...")

        // 1) Obtener el groupId desde inputData
        val groupId = inputData.getString("GROUP_ID") ?: return Result.failure().also {
            Log.e("TaskSyncWorker", "ERROR: GROUP_ID no recibido")
        }
        Log.d("TaskSyncWorker", "Recibido GROUP_ID: $groupId")

        val db = FirebaseFirestore.getInstance()

        // 2) Obtener tareas desde Firestore
        db.collection("grupos")
            .document(groupId)
            .collection("mistareas")
            .get()
            .addOnSuccessListener { result ->
                Log.d("TaskSyncWorker", "Tareas obtenidas: ${result.size()}")

                val now = Calendar.getInstance()
                val currentHour = now.get(Calendar.HOUR_OF_DAY)

                for (document in result) {
                    val nombreTarea = document.getString("nombre") ?: "Tarea sin nombre"
                    val prioridad = document.getString("prioridad") ?: "Baja"
                    val ultimaNotificacion = document.getTimestamp("ultimaNotificacion")?.toDate()?.time
                    // Obtenemos el campo completadoPor
                    val completadoPor = document.getString("completadoPor")

                    Log.d("TaskSyncWorker", "Procesando tarea: $nombreTarea - Prioridad: $prioridad")

                    // Solo procesamos la tarea si el campo completadoPor es nulo o está vacío
                    if (completadoPor.isNullOrEmpty()) {
                        when (prioridad) {
                            "Urgente" -> {
                                // Ejemplo: notificar si estamos entre las 8 y 23 y si ha pasado el intervalo (1 hora = 60*60*1000 milisegundos)
                                if (currentHour in 8..23 &&
                                    shouldSendNotification(ultimaNotificacion, 60 * 60 * 1000)) {

                                    Log.d("TaskSyncWorker", "Enviando notificación urgente para: $nombreTarea")
                                    enviarNotificacion(nombreTarea, prioridad)
                                    document.reference.update("ultimaNotificacion", com.google.firebase.Timestamp.now())
                                }
                            }
                            "Normal" -> {
                                // Ejemplo: notificar solo a las 18, 20 o 22 y si ha pasado el intervalo de 2 horas
                                if (currentHour in listOf(18, 20, 22) &&
                                    shouldSendNotification(ultimaNotificacion, 2 * 60 * 60 * 1000)) {

                                    Log.d("TaskSyncWorker", "Enviando notificación normal para: $nombreTarea")
                                    enviarNotificacion(nombreTarea, prioridad)
                                    document.reference.update("ultimaNotificacion", com.google.firebase.Timestamp.now())
                                }
                            }
                            "Baja" -> {
                                Log.d("TaskSyncWorker", "Tarea de baja prioridad, no se notifica.")
                            }
                        }
                    } else {
                        Log.d("TaskSyncWorker", "La tarea '$nombreTarea' ya fue completada (completadoPor: $completadoPor), no se notifica.")
                    }
                }

                // 4) Programar el siguiente Worker (en este ejemplo se reprograma a sí mismo en 15 minutos)
                programarProximoWorker(applicationContext, groupId)
            }
            .addOnFailureListener { exception ->
                Log.e("TaskSyncWorker", "Error al sincronizar tareas: ${exception.message}")
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
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 15) // Programa para dentro de 15 minutos
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        var delayMillis = now.timeInMillis - System.currentTimeMillis()
        if (delayMillis <= 0) {
            Log.e("TaskSyncWorker", "Error: delayMillis negativo ($delayMillis), ajustando a 15 minutos.")
            delayMillis = TimeUnit.MINUTES.toMillis(15) // Evita valores negativos
        }

        // Pasar el groupId como inputData
        val inputData = workDataOf("GROUP_ID" to groupId)

        val workRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "TaskSyncWorker_$groupId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d("TaskSyncWorker", "Siguiente notificación programada en ${delayMillis / 1000 / 60} minutos.")
    }
}