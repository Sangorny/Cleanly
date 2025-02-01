package com.cleanly.EstadisticaActivity

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cleanly.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    navController: NavHostController,
    groupId: String,
    nombresUsuarios: Map<String, String>
) {
    val firestore = FirebaseFirestore.getInstance()
    var memberScores by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var victories by remember { mutableStateOf(0) }

    LaunchedEffect(groupId) {
        fetchMemberRanking(groupId, firestore) { scores ->
            memberScores = scores.map { (uid, score) ->
                val name = nombresUsuarios[uid] ?: "Usuario desconocido"
                name to score
            }
            isLoading = false

            if (scores.isNotEmpty()) {
                val winnerId = scores[0].first

                fetchVictories(groupId, firestore, winnerId) { victoriesCount ->
                    victories = victoriesCount
                }
            }
        }
        scheduleResetScores(groupId, firestore)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clasificación del Grupo", color = Color.White) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (memberScores.isNotEmpty()) {
                        Image(
                            painter = painterResource(id = R.drawable.copa),
                            contentDescription = "Copa",
                            modifier = Modifier.size(100.dp)
                        )

                        Text(
                            text = "Victorias: $victories",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    memberScores.forEachIndexed { index, (name, score) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF673AB7), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = name,
                                fontSize = 16.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "$score puntos",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

fun fetchVictories(groupId: String, firestore: FirebaseFirestore, winnerId: String, onResult: (Int) -> Unit) {
    val victoriesRef = firestore.collection("grupos").document(groupId).collection("victorias").document(winnerId)

    victoriesRef.get()
        .addOnSuccessListener { document ->
            val victories = document.getLong("count")?.toInt() ?: 0
            onResult(victories)
        }
        .addOnFailureListener {
            Log.e("EstadisticasScreen", "Error al obtener victorias: ${it.message}")
            onResult(0)
        }
}


fun scheduleResetScores(groupId: String, firestore: FirebaseFirestore) {
    val now = Calendar.getInstance()
    val resetTime = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (this.timeInMillis <= now.timeInMillis) {
            add(Calendar.DAY_OF_YEAR, 7)
        }
    }

    val delay = resetTime.timeInMillis - now.timeInMillis

    Log.d("scheduleResetScores", "Puntuaciones se resetearán en $delay ms")

    Handler(Looper.getMainLooper()).postDelayed({
        resetScores(groupId, firestore) { updatedScores, updatedVictories ->
            Log.d("scheduleResetScores", "Clasificación actualizada tras el reset")
            Log.d("scheduleResetScores", "Victorias: $updatedVictories")
        }
    }, delay)
}



fun fetchMemberRanking(
    groupId: String,
    firestore: FirebaseFirestore,
    onResult: (List<Pair<String, Int>>) -> Unit
) {
    val groupUsersRef = firestore.collection("grupos").document(groupId).collection("usuarios")
    val groupTasksRef = firestore.collection("grupos").document(groupId).collection("mistareas")

    val usersListener = groupUsersRef.addSnapshotListener { userSnapshots, error ->
        if (error != null) {
            Log.e("fetchMemberRanking", "Error en listener de usuarios: ${error.message}")
            return@addSnapshotListener
        }

        val validUserIds = userSnapshots?.documents?.map { it.id }?.toSet() ?: emptySet()
        val uidToNameMap = userSnapshots?.documents?.associate {
            it.id to (it.getString("nombre") ?: "Usuario desconocido")
        } ?: emptyMap()

        val tasksListener = groupTasksRef.addSnapshotListener { taskSnapshots, taskError ->
            if (taskError != null) {
                Log.e("fetchMemberRanking", "Error en listener de tareas: ${taskError.message}")
                return@addSnapshotListener
            }

            val uidToScoreMap = mutableMapOf<String, Int>()
            taskSnapshots?.documents?.forEach { taskDoc ->
                val uid = taskDoc.getString("usuario") ?: return@forEach
                val points = taskDoc.getLong("puntos")?.toInt() ?: 0

                if (validUserIds.contains(uid)) {
                    uidToScoreMap[uid] = uidToScoreMap.getOrDefault(uid, 0) + points
                } else {
                    taskDoc.reference.delete().addOnFailureListener { e ->
                        Log.e("fetchMemberRanking", "Error al eliminar tarea: ${e.message}")
                    }
                }
            }

            // Cambiar para usar el UID en lugar del nombre
            val ranking = uidToScoreMap.map { (uid, score) ->
                uid to score
            }.sortedByDescending { it.second }

            onResult(ranking)
        }
    }
}

fun resetScores(
    groupId: String,
    firestore: FirebaseFirestore,
    onRankingUpdated: (List<Pair<String, Int>>, Int) -> Unit
) {
    val groupUsersRef = firestore.collection("grupos").document(groupId).collection("usuarios")
    val groupTasksRef = firestore.collection("grupos").document(groupId).collection("mistareas")
    val victoriesRef = firestore.collection("grupos").document(groupId).collection("victorias")

    groupUsersRef.get().addOnSuccessListener { userSnapshots ->
        val userIds = userSnapshots.documents.map { it.id }

        fetchMemberRanking(groupId, firestore) { scores ->
            if (scores.isNotEmpty()) {
                val winnerId = scores[0].first

                // Sumar la victoria al ganador
                victoriesRef.document(winnerId).update("count", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d("resetScores", "Victoria sumada correctamente para $winnerId")

                        // Crear tareas para resetear puntos de usuarios y tareas
                        val resetUserTasks = userIds.map { userId ->
                            groupUsersRef.document(userId).update("puntos", 0)
                        }

                        val resetTaskPoints = groupTasksRef.get().continueWithTask { task ->
                            val resetTasks = task.result?.documents?.map { taskDoc ->
                                taskDoc.reference.update("puntos", 0)
                            } ?: emptyList()
                            Tasks.whenAll(resetTasks)  // Asegurarnos de esperar todas las actualizaciones de tareas
                        }

                        // Esperar a que todas las tareas se completen
                        Tasks.whenAll(resetUserTasks + resetTaskPoints).addOnSuccessListener {
                            Log.d("resetScores", "Reseteo completado correctamente")

                            // Leer el valor actualizado de victorias
                            victoriesRef.document(winnerId).get().addOnSuccessListener { document ->
                                val updatedVictories = document.getLong("count")?.toInt() ?: 0

                                // Actualizar la clasificación y las victorias en la UI
                                fetchMemberRanking(groupId, firestore) { updatedScores ->
                                    onRankingUpdated(updatedScores, updatedVictories)
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("resetScores", "Error durante el reseteo: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("resetScores", "Error al sumar victoria: ${e.message}")
                    }
            } else {
                Log.d("resetScores", "No hay usuarios con puntuación para procesar.")
            }
        }
    }.addOnFailureListener { e ->
        Log.e("resetScores", "Error al obtener usuarios del grupo: ${e.message}")
    }
}