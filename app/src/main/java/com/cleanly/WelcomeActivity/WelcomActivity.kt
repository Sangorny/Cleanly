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
import java.util.concurrent.TimeUnit

class WelcomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (1) Crear canal de notificaciones
        GestionProgramar.createNotificationChannel(this)

        // (2) Programar el Worker de sincronización de tareas (cada 60 minutos)
        val taskSyncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(
            60, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            taskSyncWorkRequest
        )



        // (4) Configurar la interfaz de usuario:
        setContent {
            CleanlyTheme {
                MainScreen()
            }
        }
    }
}