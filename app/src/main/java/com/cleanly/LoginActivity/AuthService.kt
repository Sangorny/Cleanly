package com.cleanly

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

lateinit var auth: FirebaseAuth

fun createAccount(email: String, password: String, nick: String, context: Context, navController: NavHostController) {
    auth = FirebaseAuth.getInstance()

    val grupoId = "singrupo"

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

                            navController.navigate("group_screen/$uid") {
                                popUpTo("register") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                Toast.makeText(context, "Error en el registro: $errorMessage", Toast.LENGTH_LONG).show()
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
        .document(uid)
        .set(usuario)
        .addOnSuccessListener {
            Toast.makeText(context, "Usuario añadido", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error al añadir usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}

//Verifica que tenga un email valido
fun forgotPassword(email: String, context: Context) {

    if (!InputValidator.isEmailValid(email)) {
        Toast.makeText(context, "Email no válido", Toast.LENGTH_SHORT).show()
        return
    }
    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                    context,
                    "Se ha enviado un correo para restablecer la contraseña",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        }
}

//Funcion para hacer update a la pass
fun updatePassword(newPassword: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        user.updatePassword(newPassword).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.localizedMessage)
            }
        }
    } else {
        onComplete(false, "Usuario no autenticado")
    }
}