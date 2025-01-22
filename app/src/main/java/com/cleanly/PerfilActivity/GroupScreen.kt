package com.cleanly.PerfilActivity


import android.content.Context
import android.content.Intent
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
import com.cleanly.WelcomActivity
import com.google.firebase.firestore.FirebaseFirestore



@Composable
fun GroupScreen(
    navController: NavHostController,
    userId: String,
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
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676)) // Fondo degradado
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Crear o Unirse a un Grupo",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White // Texto blanco
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Creación de grupo
                        TextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Nombre del Grupo", color = Color.Black) }, // Texto blanco
                            placeholder = { Text("Introduce nombre grupo", color = Color.Gray) },
                            modifier = Modifier
                                .width(260.dp)
                                .background(Color.Transparent)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { handleCreateGroup() },
                            modifier = Modifier
                                .width(160.dp)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)), // Nuevo color del botón
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Crear Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Unirse a un grupo
                        TextField(
                            value = groupCode,
                            onValueChange = { groupCode = it },
                            label = { Text("Código del Grupo", color = Color.Black) },
                            placeholder = { Text("Introduce Código", color = Color.Gray) },
                            isError = errorMessage != null,
                            modifier = Modifier
                                .width(200.dp)
                                .background(Color.Transparent)
                        )

                        errorMessage?.let {
                            Text(text = it, color = Color.Red, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { handleJoinGroup() },
                            modifier = Modifier
                                .width(160.dp)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)), // Otro color del botón
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Unirse Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
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

            // Mover al usuario al nuevo grupo
            firestore.collection(GROUPS_COLLECTION).document(SINGLE_GROUP)
                .collection(USERS_SUBCOLLECTION).document(userId)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    if (userSnapshot.exists()) {
                        // Copia los datos existentes del usuario
                        val userData = userSnapshot.data?.toMutableMap() ?: mutableMapOf()

                        // Asigna el rol de administrador
                        userData["rol"] = "administrador"

                        // Añade al usuario al nuevo grupo
                        firestore.collection(GROUPS_COLLECTION).document(uniqueId)
                            .collection(USERS_SUBCOLLECTION).document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                // Elimina al usuario del grupo 'singrupo'
                                firestore.collection(GROUPS_COLLECTION).document(SINGLE_GROUP)
                                    .collection(USERS_SUBCOLLECTION).document(userId)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d("createGroup", "Usuario movido al nuevo grupo $uniqueId como administrador")
                                        onSuccess() // Notifica que la operación fue exitosa
                                    }
                                    .addOnFailureListener { exception ->
                                        handleFirestoreError(context, "createGroup", "Error al eliminar usuario de 'singrupo'", exception)
                                    }
                            }
                            .addOnFailureListener { exception ->
                                handleFirestoreError(context, "createGroup", "Error al mover usuario al nuevo grupo", exception)
                            }
                    } else {
                        handleFirestoreError(context, "createGroup", "No se encontró al usuario en 'singrupo'", null)
                    }
                }
                .addOnFailureListener { exception ->
                    handleFirestoreError(context, "createGroup", "Error al obtener datos del usuario", exception)
                }
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