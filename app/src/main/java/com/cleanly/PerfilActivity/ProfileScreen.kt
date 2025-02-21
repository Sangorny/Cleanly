package com.cleanly.PerfilActivity

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cleanly.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import com.cleanly.updatePassword
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

    // Estados para cambiar la contraseña
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Perfil de Usuario", color = Color.White) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
            )
        },
        content = { padding ->
            // Se agrega scroll al contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF00E676))
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Foto de perfil
                Image(
                    painter = painterResource(id = photoUrl),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selección de avatar mediante LazyRow
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
                                .background(if (photoUrl == imageRes) Color.Green else Color.Gray)
                                .clickable { photoUrl = imageRes },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo para el nombre
                Text(
                    text = "Nombre",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
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

                // Campo para el correo electrónico
                Text(
                    text = "Correo electrónico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
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

                Spacer(modifier = Modifier.height(32.dp))

                // Sección para cambiar la contraseña
                Text(
                    text = "Cambiar Contraseña",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo para la contraseña actual (para reautenticarse)
                Text(
                    text = "Contraseña actual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                BasicTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo para la nueva contraseña
                Text(
                    text = "Nueva contraseña",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                BasicTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo para confirmar la nueva contraseña
                Text(
                    text = "Confirmar contraseña",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                BasicTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                            Toast.makeText(navController.context, "Por favor, rellene todos los campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(navController.context, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // Reautenticar al usuario con la contraseña actual
                        reauthenticateUser(currentPassword, navController.context) { reAuthSuccess, reAuthError ->
                            if (reAuthSuccess) {
                                // Si la reautenticación es exitosa, actualizar la contraseña mediante AuthService
                                updatePassword(newPassword, navController.context) { success, errorMessage ->
                                    if (success) {
                                        Toast.makeText(navController.context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                    } else {
                                        Toast.makeText(
                                            navController.context,
                                            "Error al actualizar contraseña: $errorMessage",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(navController.context, "Error al reautenticar: $reAuthError", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Actualizar Contraseña")
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

    // Actualizar el perfil en Firebase Authentication
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName(displayName)
        .setPhotoUri(photoUri)
        .build()

    // Actualizar Firestore
    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FirebaseAuthUpdate", "Perfil actualizado correctamente en Firebase Auth")
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

    // Actualizar también el correo electrónico
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
        "photoUrl" to photoUri.toString(),
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
    onGroupFound: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("grupos")
        .get()
        .addOnSuccessListener { querySnapshot ->
            for (group in querySnapshot.documents) {
                val groupId = group.id
                val userRef = db.collection("grupos").document(groupId).collection("usuarios").document(userId)
                userRef.get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            onGroupFound(groupId)
                            return@addOnSuccessListener
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

/**
 * Función para reautenticar al usuario.
 * Se utiliza la contraseña actual para crear una credencial y reautenticar al usuario.
 */
private fun reauthenticateUser(
    currentPassword: String,
    context: android.content.Context,
    onComplete: (Boolean, String?) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null && user.email != null) {
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.localizedMessage)
            }
        }
    } else {
        onComplete(false, "Usuario no autenticado")
    }
}