package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cleanly.ProgramasActivity.GestionProgramar
import com.cleanly.ProgramasActivity.TaskSyncWorker
import com.cleanly.ProgramasActivity.scheduleInitialReset
import com.cleanly.work.ResetTasksWorker
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

        // (3) Programar el Worker de reset de tareas EXACTAMENTE a las 23:59
        // Aquí se deben disponer los parámetros 'isAdmin' y 'groupId'
        // Puedes obtenerlos de tu lógica de sesión o configuración
        val isAdmin = /* Obtén el valor (por ejemplo, desde SharedPreferences o tu lógica de login) */ true
        val groupId = /* Obtén el groupId correspondiente */ "tu_group_id"

        if (isAdmin && groupId.isNotEmpty()) {
            scheduleInitialReset(this, isAdmin, groupId)
        }


        // (4) Configurar la interfaz de usuario:
        setContent {
            CleanlyTheme {
                MainScreen()
            }
        }
    }
}