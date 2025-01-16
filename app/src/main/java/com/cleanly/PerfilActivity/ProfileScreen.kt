package com.cleanly.PerfilActivity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cleanly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    grupoId: String,
    userId: String,
    onProfileUpdated: (String, Uri?) -> Unit
) {
    val drawableImages = listOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4
    )
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var photoUrl by remember { mutableStateOf<Int>(drawableImages.first()) }




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
                    painter = painterResource(id = photoUrl), // Usa directamente el ID del recurso
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    items(drawableImages.size) { index ->
                        val imageRes = drawableImages[index]
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (photoUrl == imageRes) Color.Green else Color.Gray) // Resalta el avatar seleccionado
                                .clickable {
                                    // Actualiza el recurso seleccionado
                                    photoUrl = imageRes
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
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
                    if (displayName.isNotBlank() && email.isNotBlank()) {
                        val photoUri = Uri.parse("android.resource://${navController.context.packageName}/$photoUrl")

                        updateUserProfile(displayName, photoUri, email, userId) {
                            onProfileUpdated(displayName, photoUri)
                            Toast.makeText(navController.context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(navController.context, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Guardar cambios")
                }
            }
        }
    )
}


private fun updateUserProfile(
    displayName: String,
    photoUri: Uri,
    email: String,
    userId: String,
    onComplete: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    // Actualiza Firebase Authentication
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName(displayName)
        .setPhotoUri(photoUri)
        .build()

    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FirebaseAuthUpdate", "Perfil actualizado correctamente en Firebase Auth")
            // Busca el grupo del usuario y luego actualiza Firestore
            findGroupForUser(
                db = FirebaseFirestore.getInstance(),
                userId = userId,
                onGroupFound = { groupId ->
                    updateUserInFirestore(groupId, userId, displayName, email, photoUri)
                    onComplete()
                },
                onFailure = { exception ->
                    Log.e("Firestore", "Error al buscar grupo para el usuario: ${exception.message}")
                }
            )
        } else {
            Log.e("FirebaseAuthUpdate", "Error al actualizar perfil en Firebase Auth")
        }
    }

    // Actualiza el correo electrónico también
    user?.updateEmail(email)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FirebaseAuthUpdate", "Correo actualizado correctamente en Firebase Auth")
        } else {
            Log.e("FirebaseAuthUpdate", "Error al actualizar correo en Firebase Auth")
        }
    }
}

private fun updateUserInFirestore(
    grupoId: String,
    userId: String,
    displayName: String,
    email: String,
    photoUri: Uri
) {
    val firestore = FirebaseFirestore.getInstance()
    val userRef = firestore.collection("grupos").document(grupoId).collection("usuarios").document(userId)

    val updates = mapOf(
        "nombre" to displayName,
        "email" to email,
        "photoUrl" to photoUri.toString(),  // Guarda la URI como String
        "uid" to userId
    )

    userRef.set(updates, SetOptions.merge())
        .addOnSuccessListener {
            Log.d("FirestoreUpdate", "Usuario actualizado correctamente en el grupo $grupoId")
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreUpdate", "Error al actualizar usuario en Firestore: ${e.message}")
        }
}

private fun findGroupForUser(
    db: FirebaseFirestore,
    userId: String,
    onGroupFound: (String) -> Unit,  // Callback que recibe el grupo encontrado
    onFailure: (Exception) -> Unit   // Callback para manejar errores
) {
    // Consulta todos los grupos
    db.collection("grupos")
        .get()
        .addOnSuccessListener { querySnapshot ->
            for (group in querySnapshot.documents) {
                val groupId = group.id // ID del grupo actual
                val userRef = db.collection("grupos").document(groupId).collection("usuarios").document(userId)

                // Verifica si el usuario existe en este grupo
                userRef.get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            // Llama a onGroupFound con el grupo encontrado
                            onGroupFound(groupId)
                            return@addOnSuccessListener  // No es necesario seguir buscando más grupos
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error al buscar usuario en grupo $groupId: ${exception.message}")
                    }
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
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