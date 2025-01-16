package com.cleanly.PerfilActivity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.cleanly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    grupoId: String,
    userId: String,
    onProfileUpdated: (String, Uri?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var photoUrl by remember { mutableStateOf<Uri?>(currentUser?.photoUrl) }
    var tasksCompleted by remember { mutableStateOf(0) }
    var pointsAccumulated by remember { mutableStateOf(0) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Simula estadísticas (puedes reemplazar con datos reales)
    tasksCompleted = 50
    pointsAccumulated = 120

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                updateProfilePicture(uri) { updatedUri ->
                    photoUrl = updatedUri
                    onProfileUpdated(displayName, updatedUri)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Perfil de Usuario", color = Color.White) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = rememberImagePainter(data = photoUrl ?: R.drawable.default_avatar),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { launcher.launch("image/*") }) {
                    Text("Cambiar foto de perfil")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Nombre", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                BasicTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Correo electrónico", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                BasicTextField(
                    value = email,
                    onValueChange = { email = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    if (displayName.isNotBlank()) {
                        updateUserProfile(displayName, photoUrl) {
                            onProfileUpdated(displayName, photoUrl)
                            Toast.makeText(navController.context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Guardar cambios")
                }
            }
        }
    )
}

private fun updateUserProfile(displayName: String, photoUrl: Uri?, onComplete: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName(displayName)
        .setPhotoUri(photoUrl)
        .build()

    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onComplete()
        }
    }
}

private fun updateProfilePicture(uri: Uri, onComplete: (Uri) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setPhotoUri(uri)
        .build()

    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onComplete(uri)
        }
    }
}


    private fun sendEmailVerification(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Correo de verificación enviado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al enviar correo de verificación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPassword(email: String, context: Context) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Correo de restablecimiento de contraseña enviado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al enviar correo de restablecimiento", Toast.LENGTH_SHORT).show()
            }
        }
    }