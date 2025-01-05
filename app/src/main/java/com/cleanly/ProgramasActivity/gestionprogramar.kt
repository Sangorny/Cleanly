package com.cleanly.ProgramasActivity

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object GestionProgramar {

    private const val CHANNEL_ID = "TASK_NOTIFICATION_CHANNEL"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Notifications"
            val descriptionText = "Notifications for task reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReset(context: Context) {
        scheduleReset(context, 0, 0, DailyResetReceiver::class.java)
    }

    fun scheduleWeeklyReset(context: Context) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        scheduleReset(context, calendar.timeInMillis, AlarmManager.INTERVAL_DAY * 7, WeeklyResetReceiver::class.java)
    }

    fun scheduleMonthlyReset(context: Context) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        scheduleReset(context, calendar.timeInMillis, AlarmManager.INTERVAL_DAY * 30, MonthlyResetReceiver::class.java)
    }

    private fun scheduleReset(context: Context, startTime: Long, interval: Long, receiverClass: Class<out BroadcastReceiver>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            startTime,
            interval,
            pendingIntent
        )
    }

    fun manageNotifications(context: Context) {
        val firestore = FirebaseFirestore.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("MisTareas").get()
                .addOnSuccessListener { result ->
                    checkTasksAndNotify(context, result)
                }
        }
    }

    private fun checkTasksAndNotify(context: Context, result: QuerySnapshot) {
        for (document in result) {
            val nombre = document.getString("nombre") ?: ""
            val frecuencia = document.getString("frecuencia") ?: ""
            val prioridad = document.getString("prioridad") ?: "Baja"

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val midnight = calendar.timeInMillis

            when (prioridad) {
                "Baja" -> scheduleNotification(context, nombre, "Baja", midnight - 21600000) // 6 hours before midnight
                "Normal" -> {
                    scheduleNotification(context, nombre, "Normal", midnight - 21600000) // 6 hours before midnight
                    scheduleNotification(context, nombre, "Normal", midnight - 14400000) // 4 hours before midnight
                }
                "Urgente" -> {
                    var time = midnight - 18000000 // 5 hours before midnight
                    while (time <= midnight - 7200000) { // Until 2 hours before midnight
                        scheduleNotification(context, nombre, "Urgente", time)
                        time += 3600000 // Every hour
                    }
                }
            }
        }
    }

    private fun scheduleNotification(context: Context, taskName: String, priority: String, time: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskName", taskName)
            putExtra("priority", priority)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, taskName.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    public fun sendNotification(context: Context, taskName: String, priority: String) {
        val notificationText = "Tarea: $taskName — Prioridad: $priority"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio de Tarea")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(taskName.hashCode(), builder.build())
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GestionProgramar.initNotificationChannel(context) // Inicializar el canal
        val taskName = intent?.getStringExtra("taskName") ?: ""
        val priority = intent?.getStringExtra("priority") ?: ""
        GestionProgramar.sendNotification(context, taskName, priority)
    }
}

class DailyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GestionProgramar.manageNotifications(context)
    }
}

class WeeklyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GestionProgramar.manageNotifications(context)
    }
}

class MonthlyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GestionProgramar.manageNotifications(context)
    }
}
