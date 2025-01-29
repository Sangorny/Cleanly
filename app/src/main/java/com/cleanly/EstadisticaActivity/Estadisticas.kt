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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        set(Calendar.HOUR_OF_DAY, 11)
        set(Calendar.MINUTE, 22)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now.timeInMillis) {
            add(Calendar.DAY_OF_YEAR, 1) // Si ya pasó, lo programa para el siguiente día
        }
    }

    val delay = resetTime.timeInMillis - now.timeInMillis

    Log.d("scheduleResetScores", "Puntuaciones se resetearán en $delay ms")

    Handler(Looper.getMainLooper()).postDelayed({
        resetScores(groupId, firestore)
    }, delay)
}


fun fetchMemberRanking(
    groupId: String,
    firestore: FirebaseFirestore,
    onResult: (List<Pair<String, Int>>) -> Unit
) {
    val groupUsersRef = firestore.collection("grupos").document(groupId).collection("usuarios")
    val groupTasksRef = firestore.collection("grupos").document(groupId).collection("mistareas")

    groupUsersRef.get()
        .addOnSuccessListener { userSnapshots ->
            val validUserIds = userSnapshots.documents.map { it.id }.toSet() // Obtener solo los IDs de usuarios activos
            val uidToNameMap = userSnapshots.documents.associate {
                it.id to (it.getString("nombre") ?: "Usuario desconocido")
            }

            groupTasksRef.get()
                .addOnSuccessListener { taskSnapshots ->
                    val uidToScoreMap = mutableMapOf<String, Int>()

                    taskSnapshots.documents.forEach { taskDoc ->
                        val uid = taskDoc.getString("usuario") ?: return@forEach
                        val points = taskDoc.getLong("puntos")?.toInt() ?: 0

                        // **Solo incluir si el usuario sigue en el grupo**
                        if (validUserIds.contains(uid)) {
                            uidToScoreMap[uid] = uidToScoreMap.getOrDefault(uid, 0) + points
                        } else {
                            // **Si el usuario ya no está, eliminar su tarea**
                            taskDoc.reference.delete()
                        }
                    }

                    val ranking = uidToScoreMap.mapNotNull { (uid, score) ->
                        val name = uidToNameMap[uid] ?: "Usuario desconocido"
                        name to score
                    }.sortedByDescending { it.second }

                    onResult(ranking)
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
    val victoriesRef = firestore.collection("grupos").document(groupId).collection("victorias")

    groupUsersRef.get().addOnSuccessListener { userSnapshots ->
        var topUserId: String? = null
        var maxPoints = -1

        // Encontrar al usuario con más puntos
        userSnapshots.documents.forEach { userDoc ->
            val userId = userDoc.id
            val points = userDoc.getLong("puntos")?.toInt() ?: 0

            if (points > maxPoints) {
                maxPoints = points
                topUserId = userId
            }
        }

        // Si hay un ganador, incrementar su contador de victorias ANTES de resetear puntos
        topUserId?.let { winnerId ->
            val winnerDocRef = victoriesRef.document(winnerId)

            winnerDocRef.get().addOnSuccessListener { document ->
                val currentVictories = document.getLong("count")?.toInt() ?: 0

                // Incrementar victorias
                winnerDocRef.set(mapOf("count" to (currentVictories + 1)), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("resetScores", "Victoria añadida para usuario: $winnerId")

                        // Ahora sí, resetear las puntuaciones
                        userSnapshots.documents.forEach { userDoc ->
                            userDoc.reference.update("puntos", 0)
                                .addOnSuccessListener {
                                    Log.d("resetScores", "Puntuación reseteada para: ${userDoc.id}")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("resetScores", "Error al resetear puntuación: ${exception.message}")
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("resetScores", "Error al actualizar victorias: ${exception.message}")
                    }
            }
        }
    }.addOnFailureListener {
        Log.e("resetScores", "Error al resetear puntuaciones: ${it.message}")
    }
}