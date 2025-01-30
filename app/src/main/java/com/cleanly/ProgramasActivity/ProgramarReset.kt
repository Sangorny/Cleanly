package com.cleanly.ProgramasActivity

import android.content.Context
import androidx.work.*
import com.cleanly.work.ResetTasksWorker
import java.util.concurrent.TimeUnit

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

fun programarTaskSync(context: Context, groupId: String) {
    // 1) Crear input data con el groupId
    val inputData = workDataOf("GROUP_ID" to groupId)

    // 2) Crear un WorkRequest periódico
    // (si quieres cada hora, cada 2 horas, etc.)
    val workRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(1, TimeUnit.HOURS)
        .setInputData(inputData)
        .build()

    // 3) Encolarlo como UniqueWork para no duplicar
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "TaskSyncWorker",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}