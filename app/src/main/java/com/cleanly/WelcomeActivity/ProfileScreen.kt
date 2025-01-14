package com.cleanly.WelcomeActivity

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.cleanly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    navController: NavController,
    grupoId: String, // El grupo al que pertenece el usuario
    userId: String,  // El UID del usuario autenticado
    onProfileUpdated: (String, Uri?) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(R.drawable.default_avatar) }
    var isLoading by remember { mutableStateOf(true) }

    // Obtener los datos del usuario desde Firestore
    LaunchedEffect(Unit) {
        firestore.collection("grupos").document(grupoId)
            .collection("usuarios").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    displayName = document.getString("nombre") ?: ""
                    email = document.getString("email") ?: ""
                    selectedAvatar = document.getLong("avatar")?.toInt() ?: R.drawable.default_avatar // Cargar avatar
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ProfileContent(
            navController = navController,
            grupoId = grupoId,
            userId = userId,
            displayName = displayName,
            email = email,
            selectedAvatar = selectedAvatar,
            onProfileUpdated = onProfileUpdated
        )
    }
}

@Composable
fun ProfileContent(
    navController: NavController,
    grupoId: String,
    userId: String,
    displayName: String,
    email: String,
    selectedAvatar: Int,
    onProfileUpdated: (String, Uri?) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var updatedDisplayName by remember { mutableStateOf(displayName) }
    var updatedEmail by remember { mutableStateOf(email) }
    var selectedAvatarState by remember { mutableStateOf(selectedAvatar) }

    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cambiar avatar
                AvatarSection(
                    selectedAvatar = selectedAvatarState,
                    onAvatarSelected = { selectedAvatarState = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cambiar nombre
                Text("Nombre", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                BasicTextField(
                    value = updatedDisplayName,
                    onValueChange = { updatedDisplayName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.White, shape = CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para guardar cambios
                Button(onClick = {
                    saveProfileChanges(
                        grupoId = grupoId,
                        userId = userId,
                        displayName = updatedDisplayName,
                        avatar = selectedAvatarState,
                        firestore = firestore,
                        onSuccess = {
                            val newAvatarUri = Uri.parse("android.resource://com.cleanly/drawable/$selectedAvatarState")
                            onProfileUpdated(updatedDisplayName, newAvatarUri) // Callback para actualizar la barra superior
                            Toast.makeText(navController.context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(navController.context, "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                        }
                    )
                }) {
                    Text("Guardar cambios")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para restablecer contraseña
                Button(onClick = {
                    resetPassword(updatedEmail, navController.context)
                }) {
                    Text("Restablecer Contraseña")
                }
            }
        }
    )
}

@Composable
fun AvatarSection(selectedAvatar: Int, onAvatarSelected: (Int) -> Unit) {
    val avatars = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Selecciona un avatar", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            avatars.forEach { avatar ->
                AvatarOption(avatar = avatar) {
                    onAvatarSelected(avatar) // Cambiar avatar seleccionado
                }
            }
        }
    }
}

@Composable
fun AvatarOption(avatar: Int, onClick: () -> Unit) {
    Image(
        painter = painterResource(id = avatar),
        contentDescription = "Avatar",
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Gray)
            .clickable { onClick() }
    )
}

fun saveProfileChanges(
    grupoId: String,
    userId: String,
    displayName: String,
    avatar: Int,
    firestore: FirebaseFirestore,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val userData = mapOf(
        "nombre" to displayName,
        "avatar" to avatar // Guardar el avatar seleccionado
    )

    firestore.collection("grupos").document(grupoId)
        .collection("usuarios").document(userId)
        .update(userData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

fun resetPassword(email: String, context: Context) {
    if (email.isBlank()) {
        Toast.makeText(context, "Introduce un correo válido", Toast.LENGTH_SHORT).show()
        return
    }

    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Correo de restablecimiento enviado a $email", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
}
