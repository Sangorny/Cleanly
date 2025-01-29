package com.cleanly.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar



class ResetTasksWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // 🔹 1) Obtener si el usuario es admin y su grupo
            val isAdmin = inputData.getBoolean("isAdmin", false)
            val groupId = inputData.getString("groupId") ?: ""

            if (!isAdmin) {
                Log.d("ResetTasksWorker", "El usuario no es administrador, no se resetean tareas.")
                return Result.success()
            }

            if (groupId.isEmpty()) {
                Log.e("ResetTasksWorker", "Error: groupId es nulo o vacío.")
                return Result.failure()
            }

            // 🔹 2) Conectar a Firestore y obtener la colección de tareas del grupo
            val db = FirebaseFirestore.getInstance()
            val tasksRef = db.collection("grupos").document(groupId).collection("mistareas")

            // 🔹 3) Resetear tareas DIARIAS
            val dailyTasks = tasksRef.whereEqualTo("frecuencia", "Diaria").get().await()
            for (task in dailyTasks.documents) {
                val dataToUpdate = mapOf("completadoPor" to "", "completadoEn" to null)
                task.reference.update(dataToUpdate).await()
            }

            // 🔹 4) Resetear tareas SEMANALES (si es domingo)
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SUNDAY) {
                val weeklyTasks = tasksRef.whereEqualTo("frecuencia", "Semanal").get().await()
                for (task in weeklyTasks.documents) {
                    val dataToUpdate = mapOf("completadoPor" to "", "completadoEn" to null,"usuario" to null)
                    task.reference.update(dataToUpdate).await()
                }
            }

            // 🔹 5) Resetear tareas MENSUALES (si es fin de mes)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            if (dayOfMonth == maxDayOfMonth) {
                val monthlyTasks = tasksRef.whereEqualTo("frecuencia", "Mensual").get().await()
                for (task in monthlyTasks.documents) {
                    val dataToUpdate = mapOf("completadoPor" to "", "completadoEn" to null)
                    task.reference.update(dataToUpdate).await()
                }
            }

            Log.d("ResetTasksWorker", "Reseteo completado con éxito para el grupo: $groupId")
            return Result.success()

        } catch (e: Exception) {
            Log.e("ResetTasksWorker", "Error reseteando tareas: ${e.message}")
            return Result.retry() // Si falla, intenta de nuevo
        }
    }
}