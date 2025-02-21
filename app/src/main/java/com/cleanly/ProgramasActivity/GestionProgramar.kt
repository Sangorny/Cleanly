package com.cleanly.ProgramasActivity


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build


object GestionProgramar {

    const val CHANNEL_ID = "TEST_NOTIFICATION_CHANNEL"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Test Notifications"
            val descriptionText = "Channel for test notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
