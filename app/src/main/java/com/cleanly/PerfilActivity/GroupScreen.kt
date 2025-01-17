package com.cleanly.PerfilActivity


import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cleanly.MainActivity
import com.cleanly.WelcomActivity
import com.cleanly.WelcomeActivity.WelcomeDownBar
import com.cleanly.WelcomeActivity.WelcomeTopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore



@Composable
fun GroupScreen(
    navController: NavHostController,
    userId: String,
    showTopBarAndBottomBar: Boolean
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var groupCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Validación de campos vacíos
    fun validateInput(input: String, fieldName: String): Boolean {
        return if (input.isBlank()) {
            errorMessage = "$fieldName no puede estar vacío."
            false
        } else {
            errorMessage = null
            true
        }
    }

    fun handleCreateGroup() {
        if (validateInput(groupName, "Nombre del Grupo")) {
            isLoading = true
            createGroup(context, groupName, userId) {
                isLoading = false
                Toast.makeText(context, "Grupo creado con éxito", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, WelcomActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    fun handleJoinGroup() {
        if (validateInput(groupCode, "Código del Grupo")) {
            isLoading = true
            joinGroup(context, groupCode, userId) {
                isLoading = false
                Toast.makeText(context, "Te has unido al grupo", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, WelcomActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        topBar = {
            if (showTopBarAndBottomBar) {
                WelcomeTopBar(
                    photoUrl = null,
                    displayName = "Crear/Unirse a un Grupo",
                    onProfileClick = { /* Acción de perfil */ },
                    onGroupManagementClick = { /* Acción de gestión de grupos */ },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
        },
        bottomBar = {
            if (showTopBarAndBottomBar) {
                WelcomeDownBar { selectedScreen ->
                    when (selectedScreen) {
                        "Mis Tareas" -> navController.navigate("welcome")
                        "Zonas" -> navController.navigate("zonas")
                        "Estadísticas" -> navController.navigate("estadisticas")
                        "Programar" -> navController.navigate("programar")
                    }
                }
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Crear o Unirse a un Grupo", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Creación de grupo
                    TextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Nombre del Grupo") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ingrese el nombre del grupo") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { handleCreateGroup() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Crear Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Unirse a un grupo
                    TextField(
                        value = groupCode,
                        onValueChange = { groupCode = it },
                        label = { Text("Código del Grupo") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ingrese el código del grupo") },
                        isError = errorMessage != null
                    )

                    errorMessage?.let {
                        Text(text = it, color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { handleJoinGroup() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Unirse a Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}


const val GROUPS_COLLECTION = "grupos"
const val USERS_SUBCOLLECTION = "usuarios"
const val SINGLE_GROUP = "singrupo"

// Función para manejar errores de forma centralizada
fun handleFirestoreError(context: Context, tag: String, message: String?, exception: Exception?) {
    val errorMsg = message ?: "Error desconocido"
    Log.e(tag, errorMsg, exception)
    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
}

// Crear un grupo
fun createGroup(context: Context, name: String, userId: String, onSuccess: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val uniqueId = generateUniqueId() // Genera un ID único para el grupo

    val group = Group(
        name = name,
        id = uniqueId,
        members = listOf(userId),
        creator = userId,
        points = mapOf(userId to 0)
    )

    firestore.collection(GROUPS_COLLECTION).document(uniqueId).set(group)
        .addOnSuccessListener {
            Log.d("createGroup", "Grupo creado con éxito: $uniqueId")
            onSuccess()
        }
        .addOnFailureListener { exception ->
            handleFirestoreError(context, "createGroup", "Error al crear el grupo", exception)
        }
}

// Unirse a un grupo
fun joinGroup(context: Context, groupCode: String, userId: String, onSuccess: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection(GROUPS_COLLECTION)
        .whereEqualTo("id", groupCode)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                handleFirestoreError(context, "joinGroup", "Código de grupo no válido", null)
                return@addOnSuccessListener
            }

            val groupDoc = querySnapshot.documents.first()
            val groupId = groupDoc.id

            // Mueve al usuario de 'singrupo' al grupo objetivo
            moveUserToGroup(context, firestore, groupId, userId, onSuccess)
        }
        .addOnFailureListener { exception ->
            handleFirestoreError(context, "joinGroup", "Error al verificar el grupo", exception)
        }
}

// Función para mover al usuario de un grupo a otro
fun moveUserToGroup(
    context: Context,
    firestore: FirebaseFirestore,
    groupId: String,
    userId: String,
    onSuccess: () -> Unit
) {
    firestore.collection(GROUPS_COLLECTION).document(SINGLE_GROUP)
        .collection(USERS_SUBCOLLECTION).document(userId)
        .get()
        .addOnSuccessListener { userSnapshot ->
            if (userSnapshot.exists()) {
                // Copia los datos existentes del usuario
                val userData = userSnapshot.data?.toMutableMap() ?: mutableMapOf()

                // Actualiza el campo "rol" a "gestor"
                userData["rol"] = "gestor"

                // Añade el usuario al grupo objetivo
                firestore.collection(GROUPS_COLLECTION).document(groupId)
                    .collection(USERS_SUBCOLLECTION).document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        // Elimina al usuario de 'singrupo'
                        firestore.collection(GROUPS_COLLECTION).document(SINGLE_GROUP)
                            .collection(USERS_SUBCOLLECTION).document(userId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("moveUserToGroup", "Usuario movido correctamente al grupo $groupId con rol 'gestor'")
                                onSuccess()
                            }
                            .addOnFailureListener { exception ->
                                handleFirestoreError(context, "moveUserToGroup", "Error al eliminar usuario de 'singrupo'", exception)
                            }
                    }
                    .addOnFailureListener { exception ->
                        handleFirestoreError(context, "moveUserToGroup", "Error al agregar usuario al grupo", exception)
                    }
            } else {
                handleFirestoreError(context, "moveUserToGroup", "No se encontró al usuario en 'singrupo'", null)
            }
        }
        .addOnFailureListener { exception ->
            handleFirestoreError(context, "moveUserToGroup", "Error al obtener datos del usuario", exception)
        }
}

// Generar un ID único
fun generateUniqueId(): String {
    val charset = ('A'..'F') + ('0'..'9') // Usamos caracteres hexadecimales
    return "#" + List(6) { charset.random() }.joinToString("")
}

