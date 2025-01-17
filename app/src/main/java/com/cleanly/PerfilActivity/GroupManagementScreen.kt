package com.cleanly.PerfilActivity

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class Group(
    val name: String = "",
    val id: String = "",
    val members: List<String> = emptyList(),
    val creator: String = "",
    val points: Map<String, Int> = emptyMap()
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    navController: NavHostController,
    userId: String,
    groupId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    var currentGroup by remember { mutableStateOf<Group?>(null) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var isLeavingGroup by remember { mutableStateOf(false) }

    if (isLeavingGroup) {
        LoadingScreen(isLoading = isLeavingGroup) {
            // No se necesita lógica adicional aquí
        }
    } else {
        LaunchedEffect(groupId) {
            firestore.collection("grupos").document(groupId).get()
                .addOnSuccessListener { groupDoc ->
                    currentGroup = groupDoc.toObject(Group::class.java)
                    fetchGroupMembers(groupId, firestore) { members ->
                        groupMembers = members
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Gestión de Grupos", color = Color.White) },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
                )
            },
            content = { paddingValues ->
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
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            currentGroup?.let {
                                Text("Grupo actual: ${it.name} (ID: ${it.id})", fontSize = 20.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { showMembersDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Listar usuarios del grupo", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        isLeavingGroup = true
                                        leaveGroupAndRedirectWithLoading(userId, groupId, navController, firestore) {
                                            isLeavingGroup = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Dejar el grupo", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                if (it.creator == userId) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { navController.navigate("edit_group/${it.id}") },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB00)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Modificar grupo", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } ?: run {
                                Text("No se encontró información del grupo", color = Color.White, fontSize = 16.sp)
                            }
                        }

                        if (showMembersDialog) {
                            MemberListDialog(
                                members = groupMembers,
                                onDismiss = { showMembersDialog = false }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MemberListDialog(members: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Miembros del grupo") },
        text = {
            Column {
                members.forEach { member ->
                    Text(member, fontSize = 16.sp, color = Color.Black)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

fun fetchGroupMembers(groupId: String, firestore: FirebaseFirestore, onResult: (List<String>) -> Unit) {
    firestore.collection("grupos").document(groupId).collection("usuarios").get()
        .addOnSuccessListener { querySnapshot ->
            val members = querySnapshot.documents.mapNotNull { it.getString("nombre") }
            onResult(members)
        }
        .addOnFailureListener { exception ->
            Log.e("GroupManagementScreen", "Error al cargar usuarios: ${exception.message}")
            onResult(emptyList())
        }
}


fun leaveGroupAndRedirectWithLoading(
    userId: String,
    groupId: String,
    navController: NavHostController,
    firestore: FirebaseFirestore,
    onComplete: () -> Unit
) {
    val groupRef = firestore.collection("grupos").document(groupId).collection("usuarios").document(userId)
    val noGroupRef = firestore.collection("grupos").document("singrupo").collection("usuarios").document(userId)

    groupRef.get()
        .addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data?.toMutableMap() ?: mutableMapOf()
                userData["rol"] = "pendiente"

                groupRef.delete()
                    .addOnSuccessListener {
                        noGroupRef.set(userData)
                            .addOnSuccessListener {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    navController.navigate("group_screen") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    onComplete()
                                }, 4000) // Retraso de 4 segundos
                            }
                            .addOnFailureListener { exception ->
                                Log.e("LeaveGroup", "Error al mover al grupo sin grupo: ${exception.message}")
                                onComplete()
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("LeaveGroup", "Error al eliminar del grupo: ${exception.message}")
                        onComplete()
                    }
            } else {
                Log.e("LeaveGroup", "El usuario no existe en el grupo actual")
                onComplete()
            }
        }
        .addOnFailureListener { exception ->
            Log.e("LeaveGroup", "Error al obtener datos del usuario: ${exception.message}")
            onComplete()
        }
}
