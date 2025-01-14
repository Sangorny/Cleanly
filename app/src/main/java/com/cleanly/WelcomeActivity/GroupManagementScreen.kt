package com.cleanly.WelcomeActivity

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

data class Group(
    val name: String = "",
    val id: String = "",
    val members: List<String> = emptyList(),
    val creator: String = "",
    val points: Map<String, Int> = emptyMap()
)

@Composable
fun GroupManagementScreen(context: Context, userId: String) {
    val firestore = FirebaseFirestore.getInstance()

    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var currentGroup by remember { mutableStateOf<Group?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showClassificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("grupos").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                groups = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)
                }
                currentGroup = groups.find { group ->
                    group.members.contains(userId)
                }
            }
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
                    Text("Gestión de Grupos", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    currentGroup?.let {
                        Text("Grupo actual: ${it.name}", fontSize = 20.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showClassificationDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Ver Clasificación", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                currentGroup?.let { group ->
                                    leaveGroup(context, userId, group.id)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Salir del Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        if (it.creator == userId) {
                            Button(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB00)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Modificar Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } ?: run {
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Crear Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showJoinDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Unirse a Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showCreateDialog) {
                CreateGroupDialog(
                    onDismiss = { showCreateDialog = false },
                    onCreate = { name ->
                        createGroup(context, name, userId)
                        showCreateDialog = false
                    }
                )
            }

            if (showJoinDialog) {
                JoinGroupDialog(
                    groups = groups,
                    onDismiss = { showJoinDialog = false },
                    onJoin = { id ->
                        joinGroup(context, id, userId)
                        showJoinDialog = false
                    }
                )
            }

            if (showEditDialog && currentGroup != null) {
                EditGroupDialog(
                    group = currentGroup!!,
                    onDismiss = { showEditDialog = false },
                    onEdit = { name ->
                        editGroup(context, currentGroup!!.name, name)
                        showEditDialog = false
                    },
                    onRemoveMember = { member ->
                        removeMember(context, currentGroup!!.name, member)
                    }
                )
            }

            if (showClassificationDialog && currentGroup != null) {
                ClassificationDialog(
                    group = currentGroup!!,
                    onDismiss = { showClassificationDialog = false }
                )
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
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del Grupo") })
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
fun JoinGroupDialog(groups: List<Group>, onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var selectedGroup by remember { mutableStateOf<Group?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unirse a Grupo") },
        text = {
            Column {
                LazyColumn {
                    items(groups) { group ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedGroup = group },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(group.name)
                            if (selectedGroup == group) Text("Seleccionado", color = Color.Green)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedGroup?.let { onJoin(it.id) }
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


fun joinGroup(context: Context, id: String, userId: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(id).get().addOnSuccessListener { doc ->
        if (doc.exists()) {
            firestore.collection("grupos").document(id)
                .update(
                    "members", FieldValue.arrayUnion(userId),
                    "points.$userId", 0
                )
                .addOnSuccessListener {
                    Toast.makeText(context, "Te has unido al grupo", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "ID de grupo no válido", Toast.LENGTH_SHORT).show()
        }
    }
}

fun generateUniqueId(): String {
    val charset = ('A'..'F') + ('0'..'9')
    return "#" + List(6) { charset.random() }.joinToString("")
}

fun editGroup(context: Context, oldName: String, newName: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(oldName).get().addOnSuccessListener { doc ->
        if (doc.exists()) {
            firestore.collection("grupos").document(newName)
                .set(Group(newName, doc.getString("id") ?: "", doc.get("members") as List<String>, doc.getString("creator")!!))
                .addOnSuccessListener {
                    firestore.collection("grupos").document(oldName).delete()
                    Toast.makeText(context, "Grupo actualizado", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

fun removeMember(context: Context, groupName: String, memberId: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupName)
        .update("members", FieldValue.arrayRemove(memberId))
        .addOnSuccessListener {
            Toast.makeText(context, "Miembro eliminado", Toast.LENGTH_SHORT).show()
        }
}

fun leaveGroup(context: Context, userId: String, groupId: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("grupos").document(groupId)
        .update("members", FieldValue.arrayRemove(userId))
        .addOnSuccessListener {
            Toast.makeText(context, "Has salido del grupo", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error al salir del grupo: ${it.message}", Toast.LENGTH_SHORT).show()
        }
}

@Composable
fun EditGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var name by remember { mutableStateOf(group.name) }
    var memberNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Cargar nombres de los miembros
    LaunchedEffect(group.members) {
        val usersCollection = firestore.collection("usuarios")
        val tempMemberNames = mutableMapOf<String, String>()

        group.members.forEach { memberId ->
            usersCollection.document(memberId).get().addOnSuccessListener { document ->
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
                Text(
                    text = "Código del Grupo: ${group.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Miembros del Grupo:")
                LazyColumn {
                    items(group.members) { memberId ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = memberNames[memberId] ?: "Cargando...",
                                fontSize = 14.sp
                            )
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
fun ClassificationDialog(group: Group, onDismiss: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    var memberNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Cargar los nombres de los usuarios
    LaunchedEffect(group.members) {
        val usersCollection = firestore.collection("usuarios")
        val tempMemberNames = mutableMapOf<String, String>()

        group.members.forEach { memberId ->
            usersCollection.document(memberId).get().addOnSuccessListener { document ->
                val userName = document.getString("nombre") ?: memberId
                tempMemberNames[memberId] = userName
                if (tempMemberNames.size == group.members.size) {
                    memberNames = tempMemberNames
                }
            }
        }
    }

    // Ordenar la clasificación
    val sortedPoints = group.points.toList().sortedByDescending { it.second }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clasificación del Grupo") },
        text = {
            Column {
                if (memberNames.isEmpty()) {
                    // Mostrar indicador de carga mientras se obtienen los nombres
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn {
                        items(sortedPoints) { (memberId, points) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = memberNames[memberId] ?: "Cargando...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$points puntos",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
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