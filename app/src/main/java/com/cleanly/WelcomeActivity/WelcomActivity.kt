package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cleanly.ProgramasActivity.GestionProgramar
import com.cleanly.ProgramasActivity.TaskSyncWorker
import com.cleanly.ui.theme.CleanlyTheme
import java.util.concurrent.TimeUnit

class WelcomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear canal de notificaciones
        GestionProgramar.createNotificationChannel(this)

        // Configurar WorkManager para tareas periódicas
        val taskSyncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            taskSyncWorkRequest
        )

        // Configurar la interfaz de usuario
        setContent {
            CleanlyTheme {
                AppNavigation() // Llamar a tu NavHost desde aquí
            }
        }
    }
}