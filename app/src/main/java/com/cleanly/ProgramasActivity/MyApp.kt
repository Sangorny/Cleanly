package com.cleanly

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cleanly.ProgramasActivity.GestionProgramar
import com.cleanly.ProgramasActivity.TaskSyncWorker
import com.cleanly.work.ResetTasksWorker
import java.util.concurrent.TimeUnit

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // (1) Crear canal de notificaciones
        GestionProgramar.createNotificationChannel(this)

        // (2) Configurar WorkManager para tareas periódicas (cada hora)
        val taskSyncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            taskSyncWorkRequest
        )

        // (3) Configurar un segundo Worker para cada 24 horas (ResetTasksWorker)
        val resetTasksWorkRequest = PeriodicWorkRequestBuilder<ResetTasksWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ResetTasksDaily",
            ExistingPeriodicWorkPolicy.KEEP,
            resetTasksWorkRequest
        )

        // Ahora ya tienes todo lo que hacías en WelcomActivity (canal + Worker cada hora)
        // además del Worker diario para "resetear" tareas.
    }
}
