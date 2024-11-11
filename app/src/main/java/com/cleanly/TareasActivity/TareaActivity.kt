package com.cleanly

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.cleanly.TareasActivity.CRUDTareas
import com.cleanly.TareasActivity.TareasBD
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn

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
        val taskList = remember { mutableStateListOf<Pair<String, Int>>() }
        val updateTaskList: (List<Pair<String, Int>>) -> Unit = { newList ->
            taskList.clear()
            taskList.addAll(newList)
        }

        TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            updateTaskList(listaTareas.map { it.nombre to it.puntos })
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        val photoUrl: Uri? = account?.photoUrl

        Column {
            TopBar(auth = auth, photoUrl = photoUrl) {
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
                onCreate = { reloadTaskList(db, updateTaskList) },
                onDelete = { reloadTaskList(db, updateTaskList) },
                onList = { reloadTaskList(db, updateTaskList) },
                onEdit = { reloadTaskList(db, updateTaskList) },
                onTaskListUpdated = updateTaskList
            )
        }
    }

    @Composable
    fun TopBar(auth: FirebaseAuth, photoUrl: Uri?, onLogout: () -> Unit) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = if (photoUrl != null) rememberImagePainter(photoUrl) else painterResource(id = R.drawable.default_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
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

    private fun reloadTaskList(
        db: FirebaseFirestore,
        updateTaskList: (List<Pair<String, Int>>) -> Unit
    ) {
        TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
            updateTaskList(listaTareas.map { it.nombre to it.puntos })
        }
    }
}






