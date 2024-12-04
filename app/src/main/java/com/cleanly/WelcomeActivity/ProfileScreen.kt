package com.cleanly.WelcomeActivity

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
import coil.compose.rememberImagePainter
import com.cleanly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class ProfileScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreenContent(this)
        }
    }

    @Composable
    fun ProfileScreenContent(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
        var email by remember { mutableStateOf(currentUser?.email ?: "") }
        var photoUrl by remember { mutableStateOf<Uri?>(currentUser?.photoUrl) }
        var tasksCompleted by remember { mutableStateOf(0) }
        var pointsAccumulated by remember { mutableStateOf(0) }
        var notificationsEnabled by remember { mutableStateOf(true) }

        tasksCompleted = 50  // Simulación de estadísticas
        pointsAccumulated = 120  // Simulación de estadísticas

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                if (uri != null) {
                    uploadImageToFirebase(uri, context) { updatedUri ->
                        photoUrl = updatedUri
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagen de perfil
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

            // Botón para verificar correo si no está verificado
            if (currentUser != null && !currentUser.isEmailVerified) {
                Button(onClick = { sendEmailVerification(context) }) {
                    Text("Verificar correo electrónico")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guardar cambios
            Button(onClick = {
                if (displayName.isNotBlank()) {
                    updateUserProfile(displayName, photoUrl, context)
                } else {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Guardar cambios")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cambiar contraseña
            Button(onClick = { resetPassword(email, context) }) {
                Text("Cambiar contraseña")
            }
        }
    }

    // Función para subir la imagen a Firebase Storage
    fun uploadImageToFirebase(uri: Uri, context: Context, onComplete: (Uri) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        // Crear un nombre único para el archivo
        val fileName = UUID.randomUUID().toString()
        val imageRef = storageRef.child("profile_pictures/$fileName.jpg")

        // Subir el archivo al Storage
        val uploadTask = imageRef.putFile(uri)

        // Monitorear el progreso de la subida
        uploadTask.addOnSuccessListener {
            // Una vez que la imagen se haya subido correctamente
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                // Cuando obtenemos la URL, la pasamos a Firestore
                updatePhotoUrlInFirestore(downloadUri, context)
                onComplete(downloadUri)  // Regresamos la URL al llamador
            }.addOnFailureListener { exception ->
                Toast.makeText(context, "Error al obtener la URL: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Error al subir la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // Función para actualizar la URL de la foto en Firestore
    fun updatePhotoUrlInFirestore(photoUrl: Uri, context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val userRef = FirebaseFirestore.getInstance().collection("Usuarios").document(user?.uid ?: "")

        userRef.update("photoUrl", photoUrl.toString())
            .addOnSuccessListener {
                Toast.makeText(context, "Foto de perfil actualizada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error al actualizar el perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Función para enviar el correo de verificación
    fun sendEmailVerification(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Correo de verificación enviado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al enviar correo de verificación", Toast.LENGTH_SHORT).show()
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

    // Función para actualizar el perfil en Firebase Authentication
    fun updateUserProfile(displayName: String, photoUrl: Uri?, context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .setPhotoUri(photoUrl)
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
}





