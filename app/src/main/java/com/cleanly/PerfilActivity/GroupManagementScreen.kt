package com.cleanly.PerfilActivity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.cleanly.MainActivity
import com.cleanly.WelcomeActivity.WelcomeTopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

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
    userId: String
) {
    val context = LocalContext.current // Obtén el contexto al principio
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "Usuario") }
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl) }
    var selectedAvatar by remember { mutableStateOf<Uri?>(photoUrl) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WelcomeTopBar(
                photoUrl = photoUrl,
                displayName = displayName,
                onProfileClick = { navController.navigate("profile") },
                onGroupManagementClick = { /* Lógica de grupo */ },
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Gestión de Grupos", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Selecciona un avatar")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Lista de avatares predefinidos
                    val avatars = listOf(
                        Uri.parse("android.resource://com.cleanly/drawable/avatar_1"),
                        Uri.parse("android.resource://com.cleanly/drawable/avatar_2"),
                        Uri.parse("android.resource://com.cleanly/drawable/avatar_3"),
                        Uri.parse("android.resource://com.cleanly/drawable/avatar_4")
                    )

                    avatars.forEach { avatar ->
                        Image(
                            painter = rememberImagePainter(data = avatar),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedAvatar = avatar
                                    photoUrl = avatar // Actualizar dinámicamente en la barra superior
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Nombre", fontWeight = FontWeight.Bold)
                TextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de usuario") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            // Guardar cambios en Firebase
                            val updates = hashMapOf<String, Any>(
                                "displayName" to displayName,
                                "photoUrl" to selectedAvatar.toString()
                            )

                            firestore.collection("usuarios").document(userId)
                                .update(updates)
                                .addOnSuccessListener {
                                    currentUser?.updateProfile(
                                        userProfileChangeRequest {
                                            this.displayName = displayName
                                            this.photoUri = selectedAvatar
                                        }
                                    )?.addOnCompleteListener {
                                        isSaving = false
                                        Log.d("ProfileUpdate", "Perfil actualizado correctamente")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    isSaving = false
                                    Log.e("ProfileUpdate", "Error al actualizar: ${exception.message}")
                                }
                        }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Guardar Cambios")
                    }
                }
            }
        }
    )
}


fun createGroup(context: Context, name: String, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val uniqueId = generateUniqueId()

    val group = Group(name = name, id = uniqueId, members = listOf(userId), creator = userId, points = mapOf(userId to 0))

    firestore.collection("grupos").document(uniqueId).set(group)
        .addOnSuccessListener {
            Toast.makeText(context, "Grupo creado con éxito. ID: $uniqueId", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error al crear el grupo", Toast.LENGTH_SHORT).show()
        }
}

fun joinGroup(context: Context, code: String, userId: String, onSuccess: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos")
        .whereEqualTo("id", code) // Buscar el grupo por el código
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                Toast.makeText(context, "Código de grupo no válido.", Toast.LENGTH_SHORT).show()
            } else {
                val groupDoc = querySnapshot.documents.first()
                val groupId = groupDoc.id

                // Referencia al usuario en 'singrupo'
                val singrupoUserRef = firestore.collection("grupos").document("singrupo")
                    .collection("usuarios").document(userId)

                // Obtener los datos del usuario en 'singrupo'
                singrupoUserRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val userData = documentSnapshot.data ?: mapOf()

                            // Agregar el usuario al nuevo grupo en la subcolección 'usuarios'
                            firestore.collection("grupos").document(groupId)
                                .collection("usuarios").document(userId)
                                .set(userData + ("rol" to "gestor")) // Actualizar el rol a 'gestor'
                                .addOnSuccessListener {
                                    // Eliminar al usuario de 'singrupo'
                                    singrupoUserRef.delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Usuario movido y asignado como gestor.", Toast.LENGTH_SHORT).show()
                                            onSuccess()
                                        }
                                        .addOnFailureListener { exception ->
                                            Toast.makeText(context, "Error al borrar de 'singrupo': ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(context, "Error al agregar usuario al grupo: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Usuario no encontrado en 'singrupo'.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Error al obtener datos del usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al buscar el grupo: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

fun generateUniqueId(): String {
    val charset = ('A'..'F') + ('0'..'9')
    return "#" + List(6) { charset.random() }.joinToString("")
}

fun editGroup(context: Context, groupId: String, newName: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupId).update("name", newName)
        .addOnSuccessListener {
            Toast.makeText(context, "Grupo actualizado con éxito.", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al actualizar el grupo: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

fun removeMember(context: Context, groupId: String, memberId: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupId)
        .update("members", FieldValue.arrayRemove(memberId))
        .addOnSuccessListener {
            Toast.makeText(context, "Miembro eliminado del grupo.", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al eliminar miembro: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

fun leaveGroup(context: Context, userId: String, groupId: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupId)
        .update("members", FieldValue.arrayRemove(userId))
        .addOnSuccessListener {
            Toast.makeText(context, "Has salido del grupo.", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al salir del grupo: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

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

fun fetchPointsForGroupFromTasks(
    firestore: FirebaseFirestore,
    groupId: String,
    onSuccess: (Map<String, Int>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    firestore.collection("MisTareas").get()
        .addOnSuccessListener { snapshot ->
            val pointsMap = mutableMapOf<String, Int>()

            snapshot.documents.forEach { document ->
                val userName = document.getString("completadoPor") ?: return@forEach
                val points = document.getLong("puntos")?.toInt() ?: 0

                pointsMap[userName] = pointsMap.getOrDefault(userName, 0) + points
            }

            firestore.collection("grupos").document(groupId).get()
                .addOnSuccessListener { groupSnapshot ->
                    val members = groupSnapshot.get("members") as? List<String> ?: emptyList()
                    fetchUserNames(
                        memberIds = members,
                        onSuccess = { userNames ->
                            val mappedPoints = mapUserNamesToIds(userNames, pointsMap).toMutableMap()
                            members.forEach { memberId ->
                                mappedPoints.putIfAbsent(memberId, 0)
                            }
                            onSuccess(mappedPoints)
                        }
                        ,
                        onFailure = {
                            onFailure(it)
                        }
                    )
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    userId: String
) {
    val context = LocalContext.current
    var groupCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unirse a Grupo") },
        text = {
            Column {
                Text("Introduce el código del grupo:")
                TextField(
                    value = groupCode,
                    onValueChange = { groupCode = it },
                    placeholder = { Text("Código del grupo") },
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (groupCode.isNotBlank()) {
                    onJoin(groupCode)
                } else {
                    errorMessage = "El código no puede estar vacío."
                }
            }) {
                Text("Unirse")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Grupo") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Grupo") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }) {
                Text("Crear")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var memberNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(group.members) {
        val firestore = FirebaseFirestore.getInstance()
        val tempMemberNames = mutableMapOf<String, String>()

        group.members.forEach { memberId ->
            firestore.collection("usuarios").document(memberId).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("nombre") ?: memberId
                    tempMemberNames[memberId] = userName
                    if (tempMemberNames.size == group.members.size) {
                        memberNames = tempMemberNames
                    }
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Grupo") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Grupo") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Miembros del Grupo:")
                LazyColumn {
                    items(group.members) { memberId ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(memberNames[memberId] ?: "Cargando...", fontSize = 14.sp)
                            Button(onClick = { onRemoveMember(memberId) }) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onEdit(name) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ClassificationDialog(
    group: Group,
    members: Map<String, String>,
    points: Map<String, Int>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clasificación del Grupo") },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando...", color = Color.Gray)
                }
            } else {
                if (group.members.isEmpty()) {
                    Text("No se encontraron miembros en el grupo.", color = Color.Gray)
                } else {
                    LazyColumn {
                        items(group.members) { memberId ->
                            val memberName = members[memberId] ?: "Desconocido"
                            val memberPoints = points[memberId] ?: 0

                            println("DEBUG: Member: $memberId, Name: $memberName, Points: $memberPoints")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(memberName)
                                Text("$memberPoints puntos")
                            }
                        }
                    }

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

fun mapUserNamesToIds(
    userNames: Map<String, String>, // Mapa de ID -> Nombre
    pointsMap: Map<String, Int>
): Map<String, Int> {
    val mappedPoints = mutableMapOf<String, Int>()

    pointsMap.forEach { (name, points) ->
        val userId = userNames.entries.find { it.value == name }?.key
        if (userId != null) {
            mappedPoints[userId] = points
            println("DEBUG: Final points map with user IDs: $mappedPoints")

        }
    }

    return mappedPoints
}

