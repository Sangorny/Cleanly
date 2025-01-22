package com.cleanly

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

lateinit var auth: FirebaseAuth

fun createAccount(email: String, password: String, nick: String, context: Context, navController: NavHostController) {
    auth = FirebaseAuth.getInstance() // Asegúrate de inicializar FirebaseAuth

    val grupoId = "singrupo" // Grupo predeterminado

    if (!InputValidator.isEmailValid(email)) {
        Toast.makeText(context, "Email no válido. Formato example@domain.com", Toast.LENGTH_SHORT).show()
        return
    }
    if (!InputValidator.isPasswordValid(password)) {
        Toast.makeText(context, "Contraseña no válida.", Toast.LENGTH_SHORT).show()
        return
    }
    if (!InputValidator.isNicknameValid(nick)) {
        Toast.makeText(context, "Nick no válido.", Toast.LENGTH_SHORT).show()
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val uid = user?.uid ?: return@addOnCompleteListener

                // Actualizar perfil del usuario
                val profileUpdates = userProfileChangeRequest {
                    displayName = nick
                }
                user.updateProfile(profileUpdates)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            agregarUsuarioAGrupo(
                                db = FirebaseFirestore.getInstance(),
                                grupoId = grupoId,
                                uid = uid,
                                nombre = nick,
                                rol = "pendiente",
                                context = context
                            )
                            // Redirigir a la pantalla de crear o unirse a un grupo (GroupScreen)
                            navController.navigate("group_screen/$uid") {
                                popUpTo("register") { inclusive = true }  // Elimina la pantalla de registro de la pila
                                launchSingleTop = true // Evitar duplicados
                            }
                        }
                    }
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                Toast.makeText(context, "Error en el registro: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }


}

@Composable
fun CheckAuthentication(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firestore = FirebaseFirestore.getInstance()
                    val userId = currentUser.uid

                    // Verifica si el usuario pertenece a un grupo
                    firestore.collection("grupos")
                        .whereEqualTo("usuarios.$userId", true)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                navController.navigate("main_screen") {
                                    popUpTo("check_authentication") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate("group_screen/$userId") {
                                    popUpTo("check_authentication") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Error al verificar grupos.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(context, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                    navController.navigate("login") {
                        popUpTo("check_authentication") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        } else {
            navController.navigate("login") {
                popUpTo("check_authentication") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

fun agregarUsuarioAGrupo(
    db: FirebaseFirestore,
    grupoId: String,
    uid: String,
    nombre: String,
    rol: String,
    context: Context
) {
    val usuario = hashMapOf(
        "uid" to uid,
        "nombre" to nombre,
        "rol" to rol
    )

    db.collection("grupos").document(grupoId)
        .collection("usuarios")
        .document(uid) // Usa el UID como ID del documento
        .set(usuario)
        .addOnSuccessListener {
            Toast.makeText(context, "Usuario añadido", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al añadir usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
        }


}