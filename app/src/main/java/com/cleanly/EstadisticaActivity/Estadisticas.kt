package com.cleanly.EstadisticaActivity

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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    navController: NavHostController,
    groupId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    var memberScores by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var victories by remember { mutableStateOf(0) }

    LaunchedEffect(groupId) {
        Log.d("EstadisticasScreen", "Cargando datos para groupId: $groupId")
        fetchMemberRanking(groupId, firestore) { scores ->
            memberScores = scores
            isLoading = false
            if (scores.isNotEmpty() && scores[0].second > 0) {
                fetchVictories(groupId, firestore, scores[0].first) { victoriesCount ->
                    victories = victoriesCount
                }
            }
            Log.d("EstadisticasScreen", "Clasificación cargada: $memberScores")
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
                    // Copa e información de victorias
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

                    if (memberScores.isNotEmpty()) {
                        memberScores.forEachIndexed { index, (name, score) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Número de clasificación
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

                                // Nombre del miembro
                                Text(
                                    text = name,
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Puntos del miembro
                                Text(
                                    text = "$score puntos",
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No se encontraron miembros en este grupo.",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun fetchVictories(groupId: String, firestore: FirebaseFirestore, winnerName: String, onResult: (Int) -> Unit) {
    val victoriesRef = firestore.collection("grupos").document(groupId).collection("victorias").document(winnerName)

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
    val nextSunday = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now.timeInMillis) {
            add(Calendar.WEEK_OF_YEAR, 1)
        }
    }

    val delay = nextSunday.timeInMillis - now.timeInMillis

    Timer().schedule(object : TimerTask() {
        override fun run() {
            resetScores(groupId, firestore)
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
    val groupMetaRef = firestore.collection("grupos").document(groupId)

    groupUsersRef.get()
        .addOnSuccessListener { userSnapshots ->
            val uidToNameMap = userSnapshots.documents.associate {
                it.id to (it.getString("nombre") ?: "Usuario desconocido")
            }

            groupTasksRef.get()
                .addOnSuccessListener { taskSnapshots ->
                    val uidToScoreMap = mutableMapOf<String, Int>()

                    taskSnapshots.documents.forEach { taskDoc ->
                        val uid = taskDoc.getString("usuario") ?: return@forEach
                        val points = taskDoc.getLong("puntos")?.toInt() ?: 0

                        uidToScoreMap[uid] = uidToScoreMap.getOrDefault(uid, 0) + points
                    }

                    val ranking = uidToScoreMap.mapNotNull { (uid, score) ->
                        val name = uidToNameMap[uid] ?: "Usuario desconocido"
                        name to score
                    }.sortedByDescending { it.second }

                    onResult(ranking)

                    // Reset scores if it's Sunday 23:59
                    val currentTime = System.currentTimeMillis()
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = currentTime
                    }
                    val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    val isResetTime = calendar.get(Calendar.HOUR_OF_DAY) == 23 && calendar.get(Calendar.MINUTE) == 59

                    if (isSunday && isResetTime) {
                        val topScorerUid = uidToScoreMap.maxByOrNull { it.value }?.key
                        if (topScorerUid != null) {
                            groupUsersRef.document(topScorerUid)
                                .update("victorias", FieldValue.increment(1))
                                .addOnSuccessListener {
                                    Log.d("fetchMemberRanking", "Victoria añadida para el usuario: $topScorerUid")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("fetchMemberRanking", "Error al actualizar victorias: ${exception.message}")
                                }
                        }

                        // Reset scores in the database
                        groupTasksRef.get().addOnSuccessListener { taskDocs ->
                            for (task in taskDocs) {
                                task.reference.update("puntos", 0)
                                    .addOnSuccessListener {
                                        Log.d("fetchMemberRanking", "Puntuación reiniciada para la tarea: ${task.id}")
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("fetchMemberRanking", "Error al reiniciar puntuación: ${exception.message}")
                                    }
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("fetchMemberRanking", "Error al cargar tareas: ${exception.message}")
                    onResult(emptyList())
                }
        }
        .addOnFailureListener { exception ->
            Log.e("fetchMemberRanking", "Error al cargar usuarios: ${exception.message}")
            onResult(emptyList())
        }
}


fun resetScores(groupId: String, firestore: FirebaseFirestore) {
    val groupUsersRef = firestore.collection("grupos").document(groupId).collection("usuarios")

    groupUsersRef.get().addOnSuccessListener { userSnapshots ->
        userSnapshots.documents.forEach { userDoc ->
            val userId = userDoc.id
            val userRef = groupUsersRef.document(userId)
            userRef.update("puntos", 0)
        }
    }.addOnFailureListener {
        Log.e("EstadisticasScreen", "Error al resetear puntuaciones: ${it.message}")
    }
}
