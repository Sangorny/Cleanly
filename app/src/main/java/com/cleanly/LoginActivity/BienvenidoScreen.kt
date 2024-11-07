package com.cleanly

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var showWelcomePopup by remember { mutableStateOf(true) }
    val tasks = remember { mutableStateListOf("Lavar los platos", "Sacar la basura", "Limpiar el baño") }
    var completedTasks = remember { mutableStateOf(1) }

    val progress = if (tasks.isNotEmpty()) completedTasks.value.toFloat() / tasks.size else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val photoUrl = currentUser?.photoUrl
                        if (photoUrl != null) {
                            Image(
                                painter = rememberImagePainter(photoUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.default_avatar),
                                contentDescription = "Default Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = currentUser?.displayName ?: "Usuario", fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Configuración")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Perfil") },
                            onClick = { /* Acción para navegar al perfil */ }
                        )
                        DropdownMenuItem(
                            text = { Text("Idioma") },
                            onClick = { /* Acción para cambiar idioma */ }
                        )
                        DropdownMenuItem(
                            text = { Text("Notificaciones") },
                            onClick = { /* Acción para configurar notificaciones */ }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF00E676)
                            )
                        )
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Tareas pendientes",
                                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${completedTasks.value}/${tasks.size}", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = Color.Green
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            tasks.forEach { task ->
                                Text(text = task, style = TextStyle(fontSize = 16.sp, color = Color.Black))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { /* Acción para añadir tareas a otros usuarios */ }) {
                        Text("Añadir tarea a otros usuarios")
                    }
                }

                if (showWelcomePopup) {
                    AlertDialog(
                        onDismissRequest = { showWelcomePopup = false },
                        confirmButton = {
                            TextButton(onClick = { showWelcomePopup = false }) {
                                Text("Cerrar")
                            }
                        },
                        title = { Text("Bienvenid@ a Cleanly") },
                        text = { Text("Aquí podrás gestionar tus tareas de forma colaborativa.") }
                    )
                }
            }
        }
    )
}