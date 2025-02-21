package com.cleanly.PerfilActivity

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


data class Group(
    val name: String = "",
    val id: String = "",
    val members: List<String> = emptyList(),
    val creator: String = "",
    val points: Map<String, Int> = emptyMap()
)

// Pantalla para manejar los grupos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    navController: NavHostController,
    userId: String,
    groupId: String,
    onGroupLeft: () -> Unit,
    isAdmin: Boolean
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
                                Text("${group.name} - ${groupId}", fontSize = 25.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                if (isAdmin==true){

                                    Text("Administrador", fontSize = 20.sp, color = Color.Yellow)
                                    Spacer(modifier = Modifier.height(60.dp))
                                }
                                else
                                {
                                    Text("Gestor", fontSize = 20.sp, color = Color.Yellow)
                                    Spacer(modifier = Modifier.height(60.dp))
                                }

                                Button(
                                    onClick = {

                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = android.content.ClipData.newPlainText("Group ID", groupId)
                                        clipboardManager.setPrimaryClip(clipData)

                                        Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(
                                        0xFF000000
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
                                isAdmin = isAdmin,
                                onConfirm = {
                                    isLeavingGroup = true
                                    leaveGroupAndRedirect(
                                        navController = navController,
                                        userId = userId,
                                        groupId = groupId,
                                        firestore = firestore,
                                        isAdmin = isAdmin,
                                        onGroupLeft = onGroupLeft,
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
fun ConfirmationDialog(
    isAdmin: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirmación")
        },
        text = {
            if (isAdmin) {
                Text(
                    "¿Estás seguro de que quieres abandonar el grupo?\n" +
                            "Al hacerlo, se eliminará todo el grupo y todos los usuarios quedarán sin grupo."
                )
            } else {
                Text("¿Estás seguro de que quieres abandonar el grupo?")
            }
        },
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

// Dejar el grupo
fun leaveGroupAndRedirect(
    navController: NavHostController,
    userId: String,
    groupId: String,
    firestore: FirebaseFirestore,
    isAdmin: Boolean,
    onGroupLeft: () -> Unit,
    onComplete: () -> Unit
) {
    val tasksRef = firestore.collection("grupos")
        .document(groupId)
        .collection("mistareas")

    if (!isAdmin) {
        // -------------------------------------------------
        // FLUJO PARA USUARIO NO ADMIN (Gestor, por ejemplo)
        // 1) Primero desasigna las tareas que tenga este user
        // 2) Luego lo quita del grupo y lo mete en 'singrupo'
        // -------------------------------------------------

        // 1) Desasignar tareas
        tasksRef.whereEqualTo("usuario", userId).get()
            .addOnSuccessListener { snapshot ->
                val taskDocs = snapshot.documents
                if (taskDocs.isEmpty()) {
                    // No hay tareas asignadas a este usuario
                    // Continuar con el flujo de salida
                    removeUserFromGroup(
                        navController, userId, groupId, firestore,
                        onGroupLeft, onComplete
                    )
                } else {
                    // Hay tareas asignadas que debemos desasignar (usuario = "")
                    var pending = taskDocs.size
                    for (doc in taskDocs) {
                        doc.reference.update("usuario", "")
                            .addOnSuccessListener {
                                pending--
                                if (pending == 0) {
                                    // Ya se desasignaron todas las tareas
                                    removeUserFromGroup(
                                        navController, userId, groupId, firestore,
                                        onGroupLeft, onComplete
                                    )
                                }
                            }
                            .addOnFailureListener { ex ->
                                Log.e("LeaveGroup", "Error al desasignar tarea: ${ex.message}")
                                pending--
                                if (pending == 0) {
                                    removeUserFromGroup(
                                        navController, userId, groupId, firestore,
                                        onGroupLeft, onComplete
                                    )
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { ex ->
                Log.e("LeaveGroup", "Error al buscar tareas del usuario: ${ex.message}")
                // Aunque falle la consulta de tareas, intentar seguir con el proceso
                removeUserFromGroup(
                    navController, userId, groupId, firestore,
                    onGroupLeft, onComplete
                )
            }

    } else {
        // -------------------------------------------------
        // FLUJO PARA USUARIO ADMIN
        // (Se disuelve el grupo y se mueven todos a 'singrupo')
        // -------------------------------------------------
        val groupRef = firestore.collection("grupos").document(groupId)
        val groupUsersRef = groupRef.collection("usuarios")

        // Obtenemos todos los usuarios del grupo
        groupUsersRef.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents
                if (documents.isEmpty()) {
                    // Si no hay usuarios, solo borramos el grupo
                    deleteGroupAndMoveAdmin(
                        navController,
                        userId,
                        groupId,
                        firestore,
                        onGroupLeft,
                        onComplete
                    )
                } else {
                    // Mover uno a uno a singrupo
                    var remaining = documents.size
                    for (doc in documents) {
                        val userDocId = doc.id
                        val userData = doc.data?.toMutableMap() ?: mutableMapOf()
                        userData["rol"] = "pendiente"

                        val sinGrupoRef = firestore.collection("grupos")
                            .document("singrupo")
                            .collection("usuarios")
                            .document(userDocId)

                        // Copiamos el usuario a singrupo
                        sinGrupoRef.set(userData)
                            .addOnSuccessListener {
                                // Borramos el usuario del grupo actual
                                doc.reference.delete()
                                    .addOnSuccessListener {
                                        remaining--
                                        // Cuando hayamos movido a TODOS
                                        if (remaining == 0) {
                                            // Eliminamos el documento (padre) del grupo
                                            deleteGroupAndMoveAdmin(
                                                navController,
                                                userId,
                                                groupId,
                                                firestore,
                                                onGroupLeft,
                                                onComplete
                                            )
                                        }
                                    }
                                    .addOnFailureListener { delEx ->
                                        Log.e("LeaveGroup", "Error al eliminar user del grupo: ${delEx.message}")
                                        remaining--
                                        if (remaining == 0) {
                                            deleteGroupAndMoveAdmin(
                                                navController,
                                                userId,
                                                groupId,
                                                firestore,
                                                onGroupLeft,
                                                onComplete
                                            )
                                        }
                                    }
                            }
                            .addOnFailureListener { setEx ->
                                Log.e("LeaveGroup", "Error al mover user a 'singrupo': ${setEx.message}")
                                remaining--
                                if (remaining == 0) {
                                    deleteGroupAndMoveAdmin(
                                        navController,
                                        userId,
                                        groupId,
                                        firestore,
                                        onGroupLeft,
                                        onComplete
                                    )
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { ex ->
                Log.e("LeaveGroup", "Error al obtener usuarios del grupo: ${ex.message}")
                onComplete()
            }
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

private fun deleteGroupAndMoveAdmin(
    navController: NavHostController,
    userId: String,
    groupId: String,
    firestore: FirebaseFirestore,
    onGroupLeft: () -> Unit,
    onComplete: () -> Unit
) {
    val groupRef = firestore.collection("grupos").document(groupId)
    val adminNoGroupRef = firestore.collection("grupos")
        .document("singrupo")
        .collection("usuarios")
        .document(userId)

    // 1) Primero borramos el doc del grupo
    groupRef.delete()
        .addOnSuccessListener {
            // 2) Por si acaso, poner al admin en singrupo (si no se ha movido ya)
            // Podemos simplemente asegurarnos de que su rol sea "pendiente".
            // O, si ya se movió durante el bucle, esto no hará daño.
            val userData = mapOf("rol" to "pendiente")
            adminNoGroupRef.set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    // 3) Notificamos que ya se salió del grupo
                    onGroupLeft()

                    // 4) Navegamos
                    navController.navigate("group_screen/$userId") {
                        popUpTo("group_management_screen") { inclusive = true }
                    }

                    // 5) Terminamos
                    onComplete()
                }
                .addOnFailureListener { ex ->
                    Log.e("LeaveGroup", "Error al pasar admin a singrupo: ${ex.message}")
                    onComplete()
                }
        }
        .addOnFailureListener { ex ->
            Log.e("LeaveGroup", "Error al eliminar el grupo: ${ex.message}")
            onComplete()
        }
}

private fun removeUserFromGroup(
    navController: NavHostController,
    userId: String,
    groupId: String,
    firestore: FirebaseFirestore,
    onGroupLeft: () -> Unit,
    onComplete: () -> Unit
) {
    val groupUserRef = firestore.collection("grupos")
        .document(groupId)
        .collection("usuarios")
        .document(userId)

    val noGroupRef = firestore.collection("grupos")
        .document("singrupo")
        .collection("usuarios")
        .document(userId)

    groupUserRef.get()
        .addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data?.toMutableMap() ?: mutableMapOf()
                userData["rol"] = "pendiente"

                // Borrar del grupo actual
                groupUserRef.delete()
                    .addOnSuccessListener {
                        // Añadir a singrupo
                        noGroupRef.set(userData)
                            .addOnSuccessListener {
                                onGroupLeft() // Notificar que se abandonó
                                navController.navigate("group_screen/$userId") {
                                    popUpTo("group_management_screen") { inclusive = true }
                                }
                                onComplete()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("LeaveGroup", "Error al mover al grupo singrupo: ${exception.message}")
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