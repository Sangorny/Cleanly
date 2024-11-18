package com.cleanly

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
import com.cleanly.TareasActivity.Estadisticas
import com.cleanly.TareasActivity.TareasBD
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
        var email by remember { mutableStateOf(currentUser?.email ?: "") }
        var photoUrl by remember { mutableStateOf<Uri?>(null) }
        var estadisticas by remember { mutableStateOf(Estadisticas(0, 0)) }

        LaunchedEffect(Unit) {
            obtenerFotoPerfilDesdeFirestore(db) { uri ->
                photoUrl = uri
            }
            TareasBD.obtenerEstadisticas(
                db = db,
                onSuccess = { stats -> estadisticas = stats },
                onFailure = { /* Manejo de errores */ }
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                if (uri != null) {
                    photoUrl = uri
                    guardarFotoPerfilEnFirestore(db, uri)
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
            Image(
                painter = rememberImagePainter(data = photoUrl ?: R.drawable.default_avatar),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Button(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
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
                    .padding(vertical = 8.dp)
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
                    .padding(vertical = 8.dp)
                    .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (displayName.isNotBlank()) {
                        updateUserProfile(displayName, photoUrl, context)
                    } else {
                        Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Guardar cambios")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Correo de restablecimiento enviado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al enviar el correo de restablecimiento", Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Cambiar contraseña")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Estadísticas", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Tareas completadas: ${estadisticas.tareasCompletadas}", color = Color.White)
            Text(text = "Puntos obtenidos: ${estadisticas.puntosTotales}", color = Color.White)
        }
    }

    private fun guardarFotoPerfilEnFirestore(db: FirebaseFirestore, uri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("Usuarios").document(userId)
            userRef.set(mapOf("photoUrl" to uri.toString()), SetOptions.merge())
        }
    }

    private fun obtenerFotoPerfilDesdeFirestore(db: FirebaseFirestore, onSuccess: (Uri?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("Usuarios").document(userId)
            userRef.get()
                .addOnSuccessListener { document ->
                    val photoUrl = document.getString("photoUrl")
                    onSuccess(photoUrl?.let { Uri.parse(it) })
                }
                .addOnFailureListener {
                    onSuccess(null)
                }
        } else {
            onSuccess(null)
        }
    }

    private fun updateUserProfile(displayName: String, photoUrl: Uri?, context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .setPhotoUri(photoUrl)
            .build()

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }
}













