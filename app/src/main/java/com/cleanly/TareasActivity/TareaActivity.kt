package com.cleanly.TareasActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import coil.compose.rememberImagePainter
import com.cleanly.MainActivity
import com.cleanly.ProfileScreen
import com.cleanly.R
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
class TareaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        setContent {
            CleanlyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TareaActivityContent(auth = auth, db = db)
                }
            }
        }
    }

    @Composable
    fun TareaActivityContent(auth: FirebaseAuth, db: FirebaseFirestore) {
        val taskList = remember { mutableStateListOf<Tarea>() }
        val updateTaskList: (List<Tarea>) -> Unit = { newList ->
            taskList.clear()
            taskList.addAll(newList)
        }
        var photoUrl by remember { mutableStateOf<Uri?>(null) }

        // Cargar foto de perfil desde Firestore
        LaunchedEffect(Unit) {
            obtenerFotoPerfilDesdeFirestore(db) { uri ->
                photoUrl = uri
            }
            TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                updateTaskList(listaTareas)
            }
        }

        Column {
            TopBar(auth = auth, db = db, photoUrl = photoUrl) {
                auth.signOut()
                val intent = Intent(this@TareaActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            CRUDTareas(
                db = db,
                taskList = taskList,
                onCreate = { TareasBD.cargarTareasDesdeFirestore(db, updateTaskList) },
                onDelete = { TareasBD.cargarTareasDesdeFirestore(db, updateTaskList) },
                onList = { TareasBD.cargarTareasDesdeFirestore(db, updateTaskList) },
                onEdit = { TareasBD.cargarTareasDesdeFirestore(db, updateTaskList) },
                onTaskListUpdated = updateTaskList,
                onTaskCompleted = {
                    TareasBD.actualizarEstadisticas(db, puntosGanados = 10)
                }
            )
        }
    }

    @Composable
    fun TopBar(auth: FirebaseAuth, db: FirebaseFirestore, photoUrl: Uri?, onLogout: () -> Unit) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = if (photoUrl != null) rememberImagePainter(photoUrl) else painterResource(id = R.drawable.default_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = auth.currentUser?.displayName ?: "Usuario",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            },
            actions = {
                var expanded by remember { mutableStateOf(false) }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = Color.White)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.Gray)
                ) {
                    DropdownMenuItem(
                        text = { Text("Perfil") },
                        onClick = {
                            expanded = false
                            val intent = Intent(this@TareaActivity, ProfileScreen::class.java)
                            startActivity(intent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Notificaciones") },
                        onClick = { /* Acción para Notificaciones */ }
                    )
                    DropdownMenuItem(
                        text = { Text("Grupo") },
                        onClick = { /* Acción para Grupo */ }
                    )
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            expanded = false
                            onLogout()
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
        )
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
}


