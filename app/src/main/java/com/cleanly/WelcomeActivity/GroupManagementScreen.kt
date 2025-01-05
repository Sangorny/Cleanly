package com.cleanly.WelcomeActivity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    val groupCollection = firestore.collection("grupos")

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid ?: ""

    var groupName by remember { mutableStateOf("") }
    var currentGroupName by remember { mutableStateOf("") }
    var isUserInGroup by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var groupMemberNames by remember { mutableStateOf<List<String>>(emptyList()) }
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

// Verificar si el usuario está dentro de un grupo y obtener los miembros
    LaunchedEffect(userId) {
        groupCollection.get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach { document ->
                val members = document.get("Miembros") as? List<String> ?: emptyList()
                if (members.contains(userId)) {
                    currentGroupName = document.id // Establecer el nombre del grupo al que pertenece
                    isUserInGroup = true
                    userPoints = document.get("puntos_de_usuario") as? Int ?: 0
                    groupMembers = members

                    // Recuperar los nombres de los miembros desde Firestore
                    val memberNames = mutableListOf<String>()
                    val usersCollection = FirebaseFirestore.getInstance().collection("usuarios")

                    members.forEach { memberId ->
                        usersCollection.document(memberId).get().addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("nombre") ?: "Desconocido"

                            // Log de depuración
                            Log.d("Firestore", "Miembro: $memberId - Nombre: $name")

                            memberNames.add(name)

                            // Cuando todos los miembros hayan sido procesados, actualizamos el estado
                            if (memberNames.size == members.size) {
                                groupMemberNames = memberNames
                            }
                        }.addOnFailureListener { exception ->
                            // Log de error en caso de fallo
                            Log.e("Firestore", "Error al obtener el nombre del miembro: $memberId", exception)
                        }
                    }
                }
            }
        }.addOnFailureListener { exception ->
            // Log de error en caso de fallo al obtener el grupo
            Log.e("Firestore", "Error al obtener los datos del grupo", exception)
        }
    }

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
                            onClick = { leaveGroup(context, userId, currentGroupName, { currentGroupName = "" }, { isUserInGroup = false }) },
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
                                        joinGroup(context, userId, group, { currentGroupName = group }, { isUserInGroup = true })
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
                            groupMemberNames.forEach { member ->
                                Text("Miembro: $member")  // Mostrar el nombre de cada miembro
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

fun leaveGroup(
    context: android.content.Context,
    userId: String,
    currentGroupName: String,
    updateGroupName: (String) -> Unit,
    updateIsUserInGroup: (Boolean) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("grupos")

    val groupRef = groupCollection.document(currentGroupName)
    groupRef.update("Miembros", FieldValue.arrayRemove(userId))
        .addOnSuccessListener {
            Toast.makeText(context, "Has dejado el grupo", Toast.LENGTH_SHORT).show()
            updateGroupName("") // Vaciar el nombre del grupo
            updateIsUserInGroup(false) // El usuario ya no está en el grupo
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error al salir del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun joinGroup(
    context: android.content.Context,
    userId: String,
    groupName: String,
    updateGroupName: (String) -> Unit,
    updateIsUserInGroup: (Boolean) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("grupos")

    val groupRef = groupCollection.document(groupName)
    groupRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            groupRef.update("Miembros", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    Toast.makeText(context, "Te has unido al grupo $groupName", Toast.LENGTH_SHORT).show()
                    updateGroupName(groupName)
                    updateIsUserInGroup(true)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al unirse al grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "El grupo no existe", Toast.LENGTH_SHORT).show()
        }
    }
}

fun createGroup(context: android.content.Context, groupName: String, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val groupCollection = firestore.collection("grupos")

    val newGroup = hashMapOf(
        "nombre" to groupName,
        "Miembros" to listOf(userId),
        "puntos_de_usuario" to 0
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