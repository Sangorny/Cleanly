package com.cleanly

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
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar del usuario a la izquierda
                        if (currentUser?.photoUrl != null) {
                            Image(
                                painter = rememberImagePainter(currentUser.photoUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.default_avatar),
                                contentDescription = "Avatar predeterminado",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mostrar el nick del usuario
                        Text(text = currentUser?.displayName ?: "Usuario")
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
                                Color(0xFF0D47A1),  // Azul oscuro
                                Color(0xFF00E676)  // Verde
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Bienvenido al menú principal", style = MaterialTheme.typography.headlineMedium)
            }
        }
    )
}
