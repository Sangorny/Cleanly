package com.cleanly.ProgramasActivity

import android.content.Context
import android.util.Log
import androidx.work.*
import com.cleanly.work.ResetTasksWorker
import java.util.concurrent.TimeUnit


//Programamos el worker que se encarga de resetear las tareas que tengan programada una ejecución.

fun programarReset(context: Context, isAdmin: Boolean, groupId: String) {
    val inputData = workDataOf(
        "isAdmin" to isAdmin,
        "groupId" to groupId
    )

    val resetWork = PeriodicWorkRequestBuilder<ResetTasksWorker>(1, TimeUnit.DAYS)
        .setInputData(inputData) // 🔹 Aquí enviamos los datos al Worker
        .setInitialDelay(1, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "resetTareas_$groupId", // 🔹 Evita conflictos entre diferentes grupos
        ExistingPeriodicWorkPolicy.KEEP,
        resetWork
    )
}

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

// Programamos el Worker que se encargará de revisar las tareas cada 45 minutos

fun programarTaskCheck(context: Context, groupId: String) {
    val inputData = workDataOf("GROUP_ID" to groupId)

    val taskCheckWorkRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(45, TimeUnit.MINUTES)
        .setInputData(inputData)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "TaskCheckWorker_$groupId", // Identificador único para evitar duplicados
        ExistingPeriodicWorkPolicy.REPLACE,
        taskCheckWorkRequest
    )
    Log.d("TaskWorker", "TaskCheckWorker programado para groupId: $groupId cada 45 minutos.")
}