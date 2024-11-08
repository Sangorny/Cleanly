package com.cleanly

import android.content.Intent
import android.os.Bundle
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.cleanly.TareasActivity.CRUDTareas
import com.cleanly.TareasActivity.TareasBD
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

@OptIn(ExperimentalMaterial3Api::class)
class TareaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        setContent {
            CleanlyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val taskList = remember { mutableStateListOf<Pair<String, Int>>() }
                    val updateTaskList: (List<Pair<String, Int>>) -> Unit = { newList ->
                        taskList.clear()
                        taskList.addAll(newList)
                    }

                    TareasBD.cargarTareasDesdeFirestore(db) { listaTareas ->
                        updateTaskList(listaTareas.map { it.nombre to it.puntos })
                    }

                    val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
                    val photoUrl: Uri? = account?.photoUrl

                    Column {
                        // Barra superior con el nombre del usuario, avatar, y menú desplegable
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Mostrar la imagen de perfil de Google si está disponible
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                }
                            },
                            actions = {
                                var expanded by remember { mutableStateOf(false) }

                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menú",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.Gray) // Fondo gris del menú
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Perfil") },
                                        onClick = { /* Acción para Perfil */ }
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
                                            auth.signOut()  // Cerrar sesión de Firebase
                                            expanded = false
                                            // Navegar de vuelta a la pantalla de inicio de sesión
                                            val intent = Intent(this@TareaActivity, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.background(Color(0xFF0D47A1)),
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = Color(0xFF0D47A1)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Contenido principal con la lista de tareas y funciones CRUD
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
            }
        }
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
