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
import com.google.firebase.auth.FirebaseAuth
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
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(groupId, currentUserId) {
        fetchMemberRanking(groupId, firestore) { scores ->
            memberScores = scores.map { (uid, score) ->
                val name = nombresUsuarios[uid] ?: "Usuario desconocido"
                name to score
            }
            isLoading = false

            if (scores.isNotEmpty()) {
                fetchVictories(groupId, firestore, currentUserId) { victoriesCount ->
                    victories = victoriesCount
                }
            }
        }

        // Invoca el reset de puntuaciones correctamente con el contexto adecuado
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

fun fetchVictories(groupId: String, firestore: FirebaseFirestore, userId: String, onResult: (Int) -> Unit) {
    val victoriesRef = firestore.collection("grupos").document(groupId).collection("victorias").document(userId)

    victoriesRef.get().addOnSuccessListener { document ->
        val victories = document.getLong("count")?.toInt() ?: 0
        onResult(victories)
    }.addOnFailureListener { e ->
        Log.e("fetchVictories", "Error al obtener victorias: ${e.message}")
        onResult(0) // Devuelve 0 en caso de error
    }
}



fun scheduleResetScores(groupId: String, firestore: FirebaseFirestore) {
    val now = Calendar.getInstance()
    val resetTime = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (this.timeInMillis <= now.timeInMillis) {
            add(Calendar.DAY_OF_YEAR, 7)
        }
    }

    val delay = resetTime.timeInMillis - now.timeInMillis

    Log.d("scheduleResetScores", "Puntuaciones se resetearán en $delay ms")

    Handler(Looper.getMainLooper()).postDelayed({
        resetScores(groupId, firestore) {
            Log.d("scheduleResetScores", "Clasificación actualizada tras el reset")
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

    groupUsersRef.get().addOnSuccessListener { userSnapshots ->
        val validUserIds = userSnapshots.documents.map { it.id }.toSet()

        groupTasksRef.get().addOnSuccessListener { taskSnapshots ->
            val uidToScoreMap = mutableMapOf<String, Int>()

            taskSnapshots.documents.forEach { taskDoc ->
                val uid = taskDoc.getString("usuario") ?: return@forEach
                val points = taskDoc.getLong("puntos")?.toInt() ?: 0
                val completadoPor = taskDoc.getString("completadoPor") ?: ""

                // Verificar que la tarea esté completada antes de sumar los puntos
                if (completadoPor.isNotEmpty() && validUserIds.contains(uid)) {
                    uidToScoreMap[uid] = uidToScoreMap.getOrDefault(uid, 0) + points
                }
            }

            // Ordenar el ranking en orden descendente por puntuación
            val ranking = uidToScoreMap.map { (uid, score) -> uid to score }
                .sortedByDescending { it.second }

            onResult(ranking)
        }.addOnFailureListener { e ->
            Log.e("fetchMemberRanking", "Error al obtener tareas: ${e.message}")
        }
    }.addOnFailureListener { e ->
        Log.e("fetchMemberRanking", "Error al obtener usuarios: ${e.message}")
    }
}

private var resetInProgress = false

fun resetScores(
    groupId: String,
    firestore: FirebaseFirestore,
    onResetCompleted: () -> Unit = {}
) {
    val groupVictoriesRef = firestore.collection("grupos").document(groupId).collection("victorias")
    val groupTasksRef = firestore.collection("grupos").document(groupId).collection("mistareas")

    // Verificar si ya está en progreso para evitar duplicidades
    if (resetInProgress) {
        Log.d("resetScores", "Reseteo ya está en progreso. Cancelando ejecución duplicada.")
        return
    }

    resetInProgress = true // Marcar como en progreso

    // Obtener la clasificación actual
    fetchMemberRanking(groupId, firestore) { ranking ->
        if (ranking.isNotEmpty()) {
            val ganador = ranking.first().first

            // Incrementar la victoria solo una vez
            // Incrementar la victoria del usuario ganador en un documento personal dentro del grupo
            groupVictoriesRef.document(ganador).get().addOnSuccessListener { doc ->
                val currentVictories = doc.getLong("count")?.toInt() ?: 0
                val newVictories = currentVictories + 1

                groupVictoriesRef.document(ganador)
                    .set(mapOf("count" to newVictories))
                    .addOnSuccessListener {
                        Log.d("resetScores", "Victoria sumada correctamente para $ganador")
                    }
                    .addOnFailureListener { e ->
                        Log.e("resetScores", "Error al sumar victoria: ${e.message}")
                    }
            }


            // Resetear solo tareas completadas
            groupTasksRef.whereNotEqualTo("completadoPor", "")
                .get()
                .addOnSuccessListener { completedTasks ->
                    completedTasks.documents.forEach { taskDoc ->
                        taskDoc.reference.update("puntos", 0)
                    }
                    Log.d("resetScores", "Reseteo completado correctamente")
                    onResetCompleted()
                }
                .addOnFailureListener { e ->
                    Log.e("resetScores", "Error al resetear tareas: ${e.message}")
                }
                .addOnCompleteListener { task ->
                    resetInProgress = false // Resetear bandera al finalizar
                }
        }
    }
}