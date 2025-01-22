package com.cleanly.EstadisticaActivity

import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    navController: NavHostController,
    groupId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    var groupRanking by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Efecto lanzado al cargar la pantalla
    LaunchedEffect(groupId) {
        if (groupId.isNotEmpty()) {
            Log.d("EstadisticasScreen", "Cargando clasificación para groupId: $groupId")
            fetchMemberRanking(groupId, firestore) { ranking ->
                groupRanking = ranking
                isLoading = false
            }
        } else {
            Log.e("EstadisticasScreen", "groupId está vacío. No se puede cargar la clasificación.")
            isLoading = false
        }
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
                    if (groupRanking.isNotEmpty()) {
                        groupRanking.forEachIndexed { index, (name, points) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Posición en la clasificación
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

                                // Información del miembro
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$points puntos",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
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

// Función para obtener la clasificación de los miembros del grupo
fun fetchMemberRanking(
    groupId: String,
    firestore: FirebaseFirestore,
    onResult: (List<Pair<String, Int>>) -> Unit
) {
    firestore.collection("grupos").document(groupId).collection("usuarios").get()
        .addOnSuccessListener { userSnapshots ->
            val users = userSnapshots.documents.mapNotNull { doc ->
                val userName = doc.getString("nombre") ?: "Sin nombre"
                val points = doc.getLong("puntos")?.toInt() ?: 0
                userName to points
            }
            onResult(users.sortedByDescending { it.second })
        }
        .addOnFailureListener { exception ->
            Log.e("EstadisticasScreen", "Error al cargar la clasificación: ${exception.message}")
            onResult(emptyList())
        }
}
