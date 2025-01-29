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
    groupId: String,
    onGroupLeft: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var currentGroup by remember { mutableStateOf<Group?>(null) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var isLeavingGroup by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Cargar datos del grupo
    LaunchedEffect(groupId) {
        if (groupId == "singrupo") {
            navController.navigate("group_screen/$userId") {
                popUpTo("main_screen") { inclusive = true }
            }
        } else {
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
    }

    // Mostrar la interfaz
    if (isLeavingGroup) {
        LoadingScreen(isLoading = isLeavingGroup)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gestión de Grupos", color = Color.White) },
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
                            currentGroup?.let { group ->
                                Text("Grupo actual: ${group.name} - ID:${groupId}", fontSize = 20.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {

                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = android.content.ClipData.newPlainText("Group ID", groupId)
                                        clipboardManager.setPrimaryClip(clipData)

                                        Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(
                                        0xFFE7C60D
                                    )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Copiar ID del grupo", color = Color.White)
                                }

                                Spacer(modifier = Modifier.height(16.dp))


                                Button(
                                    onClick = { showMembersDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Listar usuarios del grupo", color = Color.White)
                                }

                                Spacer(modifier = Modifier.height(16.dp))



                                Button(
                                    onClick = { showConfirmationDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Dejar el grupo", color = Color.White)
                                }
                            } ?: run {
                                Text("No se encontró información del grupo", fontSize = 16.sp, color = Color.White)
                            }
                        }

                        if (showMembersDialog) {
                            MemberListDialog(
                                members = groupMembers,
                                onDismiss = { showMembersDialog = false }
                            )
                        }

                        if (showConfirmationDialog) {
                            ConfirmationDialog(
                                onConfirm = {
                                    isLeavingGroup = true
                                    leaveGroupAndRedirect(
                                        navController = navController,
                                        userId = userId,
                                        groupId = groupId,
                                        firestore = firestore,
                                        onGroupLeft = onGroupLeft, // ¡Aquí se pasa el callback!
                                        onComplete = {
                                            isLeavingGroup = false
                                        }
                                    )
                                },
                                onDismiss = { showConfirmationDialog = false }
                            )
                        }
                    }
                }
            }
        )
    }
}


@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmación") },
        text = { Text("¿Estas seguro de abandonar el grupo?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Sí")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}


@Composable
fun MemberListDialog(members: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Miembros del grupo") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, Color.LightGray)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                members.forEach { member ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.White, Color.LightGray)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = member,
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
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

fun leaveGroupAndRedirect(
    navController: NavHostController,
    userId: String,
    groupId: String,
    firestore: FirebaseFirestore,
    onGroupLeft: () -> Unit,
    onComplete: () -> Unit
) {
    val groupRef = firestore.collection("grupos").document(groupId).collection("usuarios").document(userId)
    val noGroupRef = firestore.collection("grupos").document("singrupo").collection("usuarios").document(userId)

    // Variables de estado local
    var currentGroup: Group? by mutableStateOf(null) // Estado del grupo actual
    var groupMembers: List<String> by mutableStateOf(emptyList()) // Estado de los miembros del grupo

    groupRef.get()
        .addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data?.toMutableMap() ?: mutableMapOf()
                userData["rol"] = "pendiente"

                groupRef.delete()
                    .addOnSuccessListener {
                        noGroupRef.set(userData)
                            .addOnSuccessListener {
                                currentGroup = null
                                groupMembers = emptyList()
                                onGroupLeft() // Notificar a MainScreen que el grupo se abandonó
                                navController.navigate("group_screen/$userId") {
                                    popUpTo("group_management_screen") { inclusive = true }
                                }
                                onComplete()
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

@Composable
fun LoadingScreen(isLoading: Boolean, message: String = "Cargando...") {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, color = Color.White)
            }
        }
    }
}