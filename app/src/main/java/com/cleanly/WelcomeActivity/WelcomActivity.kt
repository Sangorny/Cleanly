package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cleanly.ProgramasActivity.GestionProgramar
import com.cleanly.ProgramasActivity.TaskSyncWorker
import com.cleanly.ui.theme.CleanlyTheme
import com.cleanly.work.ResetTasksWorker
import java.util.concurrent.TimeUnit


class WelcomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (1) Crear canal de notificaciones
        GestionProgramar.createNotificationChannel(this)

        // (2) Configurar WorkManager para tareas periódicas (cada hora)
        val taskSyncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(
            15, TimeUnit.MINUTES
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

        // Por último, configurar la interfaz de usuario:
        setContent {
            CleanlyTheme {
                AppNavigation()
            }
        }
    }
}