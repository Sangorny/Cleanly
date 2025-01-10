package com.cleanly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cleanly.ProgramasActivity.GestionProgramar
import com.cleanly.ProgramasActivity.TaskSyncWorker
import com.cleanly.ZonaActivity.ZonasActivity
import com.cleanly.ui.theme.CleanlyTheme
import java.util.concurrent.TimeUnit

class WelcomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creo aquí el canal de notificaciones para enviarlas

        GestionProgramar.createNotificationChannel(this)

        // Configuro el workmanager, para con periodicidad de 1 hora, revisar el workmanager, así reviso
        //de paso las tareas pendientes Urgentes, Normales y Bajas.
        val taskSyncWorkRequest = PeriodicWorkRequestBuilder<TaskSyncWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            taskSyncWorkRequest
        )
        setContent {
            CleanlyTheme {

                //Aquí cargo el MainScreen(), donde tengo la UI
                MainScreen(
                    onNavigateToTarea = {
                        val intent = Intent(this, TareaActivity::class.java)
                        startActivity(intent)
                    },
                    onNavigateToZonas = {
                        val intent = Intent(this, ZonasActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
            }
        }
    }
