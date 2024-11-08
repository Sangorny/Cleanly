package com.cleanly

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    navController: NavHostController,
    signInWithGoogle: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<Uri?>(null) } // Estado para la URL de la foto

    val auth = FirebaseAuth.getInstance()
    val context = navController.context

    LaunchedEffect(Unit) {
        // Recuperar la cuenta de Google después de iniciar sesión
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)
        photoUrl = account?.photoUrl // Actualizar el estado con la URL de la foto
    }

    Column(
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
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mostrar la imagen de perfil de Google si está disponible, o un avatar predeterminado
        photoUrl?.let {
            Image(
                painter = rememberImagePainter(data = it),
                contentDescription = "Foto de perfil de Google",
                modifier = Modifier.size(100.dp)
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.default_avatar), // Imagen predeterminada
            contentDescription = "Avatar predeterminado",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

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
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!InputValidator.isEmailValid(email)) {
                    Toast.makeText(navController.context, "El formato del email es incorrecto", Toast.LENGTH_SHORT).show()
                } else if (!InputValidator.isPasswordValid(password)) {
                    Toast.makeText(navController.context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                val errorMessage = task.exception?.localizedMessage ?: "Error en el inicio de sesión"
                                Toast.makeText(navController.context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier
                .width(240.dp)
                .height(48.dp)
        ) {
            Text("Iniciar sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { signInWithGoogle() },
            modifier = Modifier
                .width(240.dp)
                .height(48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_google),
                contentDescription = "Google Icon",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar sesión con Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¿No tienes cuenta? Regístrate",
            modifier = Modifier.clickable {
                navController.navigate("register")
            }
        )
    }
}





