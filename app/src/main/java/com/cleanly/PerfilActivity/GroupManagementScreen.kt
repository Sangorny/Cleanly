package com.cleanly.PerfilActivity

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class Group(
    val name: String = "",
    val id: String = "",
    val members: List<String> = emptyList(),
    val creator: String = "",
    val points: Map<String, Int> = emptyMap()
)
@Composable
fun GroupManagementScreen(
    navController: NavHostController,
    context: Context,
    userId: String,
    grupoId: String // Recibimos el grupoId como parámetro
) {
    val firestore = FirebaseFirestore.getInstance()

    var currentGroup by remember { mutableStateOf<Group?>(null) }
    var groupMembers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Obtener la información del grupo usando el grupoId recibido
    LaunchedEffect(grupoId) {
        firestore.collection("grupos").document(grupoId).get()
            .addOnSuccessListener { groupDoc ->
                currentGroup = groupDoc.toObject(Group::class.java)

                currentGroup?.let { group ->
                    fetchGroupMembers(groupId = group.id, onSuccess = { members ->
                        fetchUserNames(memberIds = members, onSuccess = { userNames ->
                            groupMembers = userNames
                            isLoading = false
                        }, onFailure = {
                            isLoading = false
                        })
                    }, onFailure = {
                        isLoading = false
                    })
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Gestión de Grupos", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    currentGroup?.let {
                        Text("Grupo actual: ${it.name}", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón para listar usuarios del grupo
                        Button(
                            onClick = {
                                // Acción para listar los miembros del grupo
                                navController.navigate("group_members/${it.id}")
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Listar usuarios del grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón para dejar el grupo
                        Button(
                            onClick = {
                                // Acción para dejar el grupo
                                leaveGroup(context, userId, it.id) {
                                    navController.navigate("group_screen/$userId") // Redirigir a la pantalla de creación/unión de grupo
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Dejar el grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        // Si el usuario es el creador del grupo, habilitar opciones de edición
                        if (it.creator == userId) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Lógica para editar el grupo (si es el creador)
                                    navController.navigate("edit_group/${it.id}")
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB00)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Modificar grupo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } ?: run {
                        // Si el usuario no está en ningún grupo, redirigir a la pantalla de creación/unión de grupo
                        Button(
                            onClick = { navController.navigate("group_screen/$userId") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Unirse a un grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

// Función para eliminar al usuario del grupo
fun leaveGroup(context: Context, userId: String, groupId: String, onComplete: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupId)
        .update("members", FieldValue.arrayRemove(userId))
        .addOnSuccessListener {
            Toast.makeText(context, "Has salido del grupo.", Toast.LENGTH_SHORT).show()
            onComplete()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al salir del grupo: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

// Función para obtener los miembros del grupo
fun fetchGroupMembers(groupId: String, onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupId).get()
        .addOnSuccessListener { document ->
            val members = document.get("members") as? List<String> ?: emptyList()
            onSuccess(members)
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

// Función para obtener los nombres de los usuarios a partir de sus IDs
fun fetchUserNames(memberIds: List<String>, onSuccess: (Map<String, String>) -> Unit, onFailure: (Exception) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val userNames = mutableMapOf<String, String>()

    memberIds.forEach { userId ->
        firestore.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("nombre") ?: "Desconocido"
                userNames[userId] = name

                if (userNames.size == memberIds.size) {
                    onSuccess(userNames)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}