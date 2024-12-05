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
import com.google.firebase.auth.UserProfileChangeRequest

// Ahora ProfileScreen es un Composable
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl) }
    var selectedAvatar by remember { mutableStateOf(photoUrl ?: R.drawable.default_avatar) }

    // Lista de avatares predefinidos
    val avatars = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4
    )

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                    )
                )
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagen de perfil
            Image(
                painter = rememberImagePainter(data = selectedAvatar),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar los avatares disponibles
            Text(text = "Selecciona un avatar", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                avatars.forEach { avatar ->
                    AvatarOption(avatar = avatar, onClick = { selectedAvatar = avatar })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre
            Text(text = "Nombre", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            BasicTextField(
                value = displayName,
                onValueChange = { displayName = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Correo electrónico
            Text(text = "Correo electrónico", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            BasicTextField(
                value = email,
                onValueChange = { email = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guardar cambios
            Button(onClick = {
                if (displayName.isNotBlank()) {
                    updateUserProfile(displayName, selectedAvatar as Int, navController.context)
                } else {
                    Toast.makeText(navController.context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Guardar cambios")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cambiar contraseña
            Button(onClick = { resetPassword(email, navController.context) }) {
                Text("Cambiar contraseña")
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

// Función para actualizar el perfil en Firebase Authentication
fun updateUserProfile(displayName: String, photoUrl: Int, context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName(displayName)
        .setPhotoUri(Uri.parse("android.resource://com.cleanly/drawable/$photoUrl"))
        .build()

    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            // Recargamos el usuario después de la actualización
            user.reload().addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al recargar el perfil", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
        }
    }
}

// Función para restablecer la contraseña
fun resetPassword(email: String, context: Context) {
    FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Toast.makeText(context, "Correo de restablecimiento de contraseña enviado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al enviar correo de restablecimiento", Toast.LENGTH_SHORT).show()
        }
    }
}