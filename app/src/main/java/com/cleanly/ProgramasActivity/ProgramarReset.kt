package com.cleanly.ProgramasActivity

import android.content.Context
import android.util.Log
import androidx.work.*
import com.cleanly.work.ResetTasksWorker
import java.lang.reflect.Array.set
import java.util.Calendar
import java.util.concurrent.TimeUnit


// Programamos el worker que se encargará de lanzar notificaciones Urgentes o Medias

fun programarTaskSync(context: Context, groupId: String) {
    val inputData = workDataOf(
        "GROUP_ID" to groupId
    )

    val workManager = WorkManager.getInstance(context) // 🔹 Obtén el WorkManager

    // 🔹 Cancela cualquier Worker anterior para este groupId antes de encolar uno nuevo
    workManager.cancelUniqueWork("TaskSyncWorker_$groupId")

    val taskSyncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(15, TimeUnit.MINUTES)
        .setInputData(inputData) // 🔹 Aquí enviamos el groupId al Worker
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    workManager.enqueueUniquePeriodicWork(
        "TaskSyncWorker_$groupId", // 🔹 Un identificador único por grupo
        ExistingPeriodicWorkPolicy.REPLACE, // 🔹 Reemplaza cualquier Worker en ejecución
        taskSyncWorkRequest
    )

    Log.d("TaskSyncWorker", "Worker programado para groupId: $groupId cada 15 minutos.")
}

fun scheduleInitialReset(context: Context, isAdmin: Boolean, groupId: String) {
    // Configurar el calendario para obtener las 23:59 del día en curso (o del siguiente si ya pasó)
    val now = Calendar.getInstance()
    val nextReset = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)) {
            // Si ya pasó las 23:59, programar para el día siguiente
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val delay = nextReset.timeInMillis - now.timeInMillis

    // Datos que se pasan al Worker
    val inputData = workDataOf(
        "isAdmin" to isAdmin,
        "groupId" to groupId
    )

    // Crear el OneTimeWorkRequest con el delay calculado
    val resetWorkRequest = OneTimeWorkRequestBuilder<ResetTasksWorker>()
        .setInputData(inputData)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()

    // Encolar el Worker de forma única para evitar duplicados
    WorkManager.getInstance(context).enqueueUniqueWork(
        "resetTareas",
        ExistingWorkPolicy.REPLACE,
        resetWorkRequest
    )
}
