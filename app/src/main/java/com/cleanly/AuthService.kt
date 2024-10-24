package com.cleanly

import android.util.Log
import android.widget.Toast
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest


private lateinit var auth: FirebaseAuth

fun initAuth() {
    auth = FirebaseAuth.getInstance()
}

fun signIn(email: String, password: String, navController: NavHostController) {
    if (email.isNotEmpty() && password.isNotEmpty()) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(navController.context, "Login exitoso", Toast.LENGTH_LONG).show()
                    navController.navigate("welcome") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                    Toast.makeText(navController.context, "Error en el login: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    } else {
        Toast.makeText(
            navController.context,
            "Por favor, completa todos los campos",
            Toast.LENGTH_SHORT
        ).show()
    }
}


fun createAccount(email: String, password: String, nick: String, navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    if (email.isNotEmpty() && password.length >= 6) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->  // Eliminar 'this' aquí
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = auth.currentUser

                    // Actualizar el perfil del usuario con el nick
                    val profileUpdates = userProfileChangeRequest {
                        displayName = nick
                    }

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Toast.makeText(navController.context, "Registro exitoso", Toast.LENGTH_LONG).show()
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        }
                } else {
                    val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(navController.context, "Error en el registro: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    } else {
        Toast.makeText(navController.context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
    }
}

fun signOut(navController: NavHostController) {
    FirebaseAuth.getInstance().signOut()
    navController.navigate("login") {
        popUpTo("welcome") { inclusive = true }
    }
}




