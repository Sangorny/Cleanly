package com.cleanly

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

@Composable
fun RegisterScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nick by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Repite la contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nick,
            onValueChange = { nick = it },
            label = { Text("Nick") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (password == confirmPassword) {
                createAccount(email, password, nick, context)
            } else {
                Toast.makeText(
                    context,
                    "Las contraseñas no coinciden",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }) {
            Text("Registrarse")
        }
    }
}

fun createAccount(email: String, password: String, nick: String, context: Context) {
    val auth = FirebaseAuth.getInstance()

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
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_LONG).show()
                            // Puedes redirigir a otra pantalla o realizar una acción después de éxito
                            // Ejemplo:
                            // navController.navigate("welcome")
                        }
                    }
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                Toast.makeText(context, "Error en el registro: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
}
