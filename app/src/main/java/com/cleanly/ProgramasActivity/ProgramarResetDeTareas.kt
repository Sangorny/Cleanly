package com.cleanly.ProgramasActivity

import android.content.Context
import androidx.work.*
import com.cleanly.work.ResetTasksWorker
import java.util.concurrent.TimeUnit

fun programarResetDeTareas(context: Context, isAdmin: Boolean, groupId: String) {
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