package com.cleanly.WelcomeActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.cleanly.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class GroupManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Inicializamos el NavController
            val navController = rememberNavController()

            // Aquí configuramos el NavHost con las pantallas
            NavHost(navController = navController, startDestination = "group_management") {
                composable("group_management") {
                    GroupManagementScreen(navController = navController)
                }
                composable("profile") {
                    ProfileScreen(
                        navController = navController,
                        onProfileUpdated = { updatedName, updatedPhoto ->
                            // Aquí puedes manejar la lógica cuando el perfil se actualiza
                            handleProfileUpdated(updatedName, updatedPhoto)
                        }
                    )
                }
            }
        }


    }
    private fun handleProfileUpdated(updatedName: String, updatedPhoto: Uri?) {
        // Lógica para manejar los cambios en el perfil
        // Por ejemplo: puedes actualizar un estado compartido o informar al backend
        println("Perfil actualizado: $updatedName con foto $updatedPhoto")
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("groups")

    // Obtener el usuario actual
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid ?: ""

    // Estados para manejar el grupo y el estado del usuario
    var groupName by remember { mutableStateOf("") }
    var currentGroupName by remember { mutableStateOf("") }
    var isUserInGroup by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var userPoints by remember { mutableStateOf(0) }
    var availableGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Cargar grupos disponibles
    LaunchedEffect(Unit) {
        groupCollection.get().addOnSuccessListener { snapshot ->
            availableGroups = snapshot.map { it.id }
        }
    }

    // Verificar si el usuario está dentro de un grupo
    LaunchedEffect(Unit) {
        groupCollection.document("default_group").get().addOnSuccessListener { document ->
            if (document.exists()) {
                groupMembers = document.get("members") as? List<String> ?: emptyList()
                userPoints = document.get("user_points") as? Int ?: 0
                isUserInGroup = groupMembers.contains(userId)
                currentGroupName = if (isUserInGroup) "default_group" else ""
            }
        }
    }

    // Scaffold con la barra de navegación
    Scaffold(
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF00E676)
                            )
                        )
                    )
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Grupo: $currentGroupName", fontSize = 24.sp, modifier = Modifier.padding(8.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isUserInGroup) {
                        Text("Puntos: $userPoints", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botones siempre visibles
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showMembersDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver miembros")
                        }

                        Button(
                            onClick = { leaveGroup(context, userId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dejar el grupo")
                        }

                        Button(
                            onClick = { showCreateGroupDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Crear un grupo")
                        }

                        Button(
                            onClick = { showJoinGroupDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unirse a un grupo")
                        }
                    }
                }
            }

            // Mostrar el diálogo para unirse a un grupo
            if (showJoinGroupDialog) {
                AlertDialog(
                    onDismissRequest = { showJoinGroupDialog = false },
                    title = { Text("Selecciona un grupo") },
                    text = {
                        Column {
                            availableGroups.forEach { group ->
                                Button(
                                    onClick = {
                                        joinGroup(context, userId, group)
                                        currentGroupName = group
                                        showJoinGroupDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Text("Unirse a $group")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showJoinGroupDialog = false }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Mostrar el diálogo para crear un nuevo grupo
            if (showCreateGroupDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateGroupDialog = false },
                    title = { Text("Crear un nuevo grupo") },
                    text = {
                        Column {
                            TextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                label = { Text("Nombre del grupo") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (groupName.isNotBlank()) {
                                    createGroup(context, groupName, userId)
                                    showCreateGroupDialog = false
                                    groupName = ""
                                } else {
                                    Toast.makeText(context, "Por favor ingresa el nombre del grupo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Crear")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showCreateGroupDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Mostrar el diálogo para mostrar los miembros del grupo
            if (showMembersDialog) {
                AlertDialog(
                    onDismissRequest = { showMembersDialog = false },
                    title = { Text("Miembros del grupo") },
                    text = {
                        Column {
                            groupMembers.forEach { member ->
                                Text(member)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showMembersDialog = false }
                        ) {
                            Text("Cerrar")
                        }
                    }
                )
            }
        }
    )
}

fun leaveGroup(context: android.content.Context, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("groups")

    groupCollection.document("default_group")
        .update("members", FieldValue.arrayRemove(userId))
        .addOnSuccessListener {
            Toast.makeText(context, "Has dejado el grupo", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error al salir del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun joinGroup(context: android.content.Context, userId: String, groupName: String) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("groups")

    groupCollection.document(groupName)
        .update("members", FieldValue.arrayUnion(userId))
        .addOnSuccessListener {
            Toast.makeText(context, "Te has unido al grupo", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error al unirse al grupo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun createGroup(context: android.content.Context, groupName: String, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("groups")

    val newGroup = hashMapOf(
        "name" to groupName,
        "members" to listOf(userId),
        "user_points" to 0
    )

    groupCollection.document(groupName)
        .set(newGroup)
        .addOnSuccessListener {
            Toast.makeText(context, "Grupo creado con éxito", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error al crear el grupo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}