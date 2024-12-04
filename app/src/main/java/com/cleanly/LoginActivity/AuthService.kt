package com.cleanly

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest

lateinit var auth: FirebaseAuth

// Este método ahora está fuera del flujo composable y se ejecuta en un servicio o clase fuera de UI.
fun createAccount(email: String, password: String, nick: String, context: Context, onSuccess: () -> Unit) {
    if (!InputValidator.isEmailValid(email)) {
        Toast.makeText(context, "Email no válido. Formato example@domain.com", Toast.LENGTH_SHORT).show()
        return
    }
    if (!InputValidator.isPasswordValid(password)) {
        Toast.makeText(context, "Contraseña no válida. Debe contener al menos 8 caracteres y solo letras o números.", Toast.LENGTH_SHORT).show()
        return
    }
    if (!InputValidator.isNicknameValid(nick)) {
        Toast.makeText(context, "Nick no válido. Debe ser alfanumérico y tener hasta 12 caracteres.", Toast.LENGTH_SHORT).show()
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser

                val profileUpdates = userProfileChangeRequest {
                    displayName = nick
                }

                user?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            onSuccess()
                        }
                    }
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                Toast.makeText(context, "Error en el registro: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
}

// Para el login
fun signIn(email: String, password: String, context: Context, onSuccess: () -> Unit) {
    if (!InputValidator.isEmailValid(email) || !InputValidator.isPasswordValid(password)) {
        Toast.makeText(context, "Credenciales no válidas", Toast.LENGTH_SHORT).show()
        return
    }

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                Toast.makeText(context, "Error en el login: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
}


