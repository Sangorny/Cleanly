package com.cleanly.PerfilActivity


import android.content.Context
import android.content.Intent
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
import com.cleanly.WelcomeActivity.WelcomeDownBar
import com.cleanly.WelcomeActivity.WelcomeTopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore



@Composable
fun GroupScreen(
    navController: NavHostController,
    userId: String,
    showTopBarAndBottomBar: Boolean // Recibimos el parámetro para controlar la visibilidad de las barras
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }  // Para la creación de un nuevo grupo
    var groupCode by remember { mutableStateOf("") }  // Para unirse a un grupo existente
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estado de carga
    var isLoading by remember { mutableStateOf(false) }

    // Crear un nuevo grupo
    fun handleCreateGroup() {
        if (groupName.isNotBlank()) {
            isLoading = true
            createGroup(context, groupName, userId) {
                isLoading = false
                Toast.makeText(context, "Grupo creado con éxito", Toast.LENGTH_SHORT).show()
                navController.navigate("welcome") {
                    popUpTo("group_screen/$userId") { inclusive = true } // Limpiar la pila de navegación
                    launchSingleTop = true
                }
            }
        } else {
            errorMessage = "El nombre del grupo no puede estar vacío."
        }
    }

    // Unirse a un grupo
    fun handleJoinGroup() {
        if (groupCode.isNotBlank()) {
            isLoading = true
            joinGroup(context, groupCode, userId) {
                isLoading = false
                Toast.makeText(context, "Te has unido al grupo", Toast.LENGTH_SHORT).show()
                navController.navigate("welcome") {
                    popUpTo("group_screen/$userId") { inclusive = true } // Limpiar la pila de navegación
                    launchSingleTop = true
                }
            }
        } else {
            errorMessage = "El código del grupo no puede estar vacío."
        }
    }

    Scaffold(
        topBar = {
            if (showTopBarAndBottomBar) {
                WelcomeTopBar(
                    photoUrl = null, // Puedes configurarlo según sea necesario
                    displayName = "Crear/Unirse a un Grupo", // O el nombre que desees mostrar
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
                        onClick = {
                            if (groupCode.isNotBlank()) {
                                isLoading = true
                                // Unirse al grupo
                                joinGroup(context, groupCode, userId) {
                                    isLoading = false
                                    Toast.makeText(context, "Te has unido al grupo", Toast.LENGTH_SHORT).show()
                                    navController.navigate("main") {
                                        popUpTo("group_screen/$userId") { inclusive = true } // Limpia la pila
                                        launchSingleTop = true // Evita duplicados
                                    }
                                }
                            } else {
                                errorMessage = "El código del grupo no puede estar vacío."
                            }
                        },
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


fun createGroup(context: Context, name: String, userId: String, onSuccess: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val uniqueId = generateUniqueId()  // Genera un ID único para el grupo

    val group = Group(name = name, id = uniqueId, members = listOf(userId), creator = userId, points = mapOf(userId to 0))

    firestore.collection("grupos").document(uniqueId).set(group)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error al crear el grupo", Toast.LENGTH_SHORT).show()
        }
}

fun joinGroup(context: Context, groupCode: String, userId: String, onSuccess: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    // Primero, buscamos el grupo con el código
    firestore.collection("grupos")
        .whereEqualTo("id", groupCode) // Buscamos el grupo con el código
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                // Si no encontramos el grupo con ese código
                Toast.makeText(context, "Código de grupo no válido.", Toast.LENGTH_SHORT).show()
            } else {
                // Obtenemos el primer grupo que coincide con el código
                val groupDoc = querySnapshot.documents.first()
                val groupId = groupDoc.id // ID del grupo

                // Ahora obtenemos el usuario desde el grupo "singrupo"
                firestore.collection("grupos").document("singrupo")
                    .collection("usuarios").document(userId)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        if (userSnapshot.exists()) {
                            // Tomamos los datos del usuario
                            val user = userSnapshot.data
                            val userName = user?.get("nombre") ?: "Desconocido"
                            val userRole = user?.get("rol") ?: "pendiente"

                            // Ahora que tenemos el usuario, lo agregamos al grupo correspondiente
                            val userRef = firestore.collection("grupos").document(groupId)
                                .collection("usuarios").document(userId)

                            // Añadimos el usuario a la subcolección 'usuarios' del grupo y actualizamos la lista de miembros
                            userRef.set(
                                hashMapOf(
                                    "uid" to userId,
                                    "nombre" to userName,
                                    "rol" to userRole
                                )
                            ).addOnSuccessListener {
                                // Actualizamos la lista de miembros en el grupo
                                firestore.collection("grupos").document(groupId)
                                    .update(
                                        "members", FieldValue.arrayUnion(userId), // Agregamos el UID a la lista de miembros
                                        "points.$userId", 0 // Inicializamos los puntos del nuevo miembro
                                    ).addOnSuccessListener {
                                        // Eliminar el usuario de la subcolección de 'singrupo'
                                        firestore.collection("grupos").document("singrupo")
                                            .collection("usuarios").document(userId)
                                            .delete()
                                            .addOnSuccessListener {
                                                // Llamamos al callback de éxito después de eliminar
                                                onSuccess()
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(context, "Error al eliminar usuario de 'singrupo': ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(context, "Error al actualizar grupo: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            // Si el usuario no existe en "singrupo"
                            Toast.makeText(context, "No se encontró al usuario en 'singrupo'.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Error al obtener datos del usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al verificar el código: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

fun generateUniqueId(): String {
    val charset = ('A'..'F') + ('0'..'9')  // Usamos los caracteres hexadecimales
    return "#" + List(6) { charset.random() }.joinToString("")  // Genera un ID de 6 caracteres
}

