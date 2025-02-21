package com.cleanly.WelcomeActivity

import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.cleanly.R


//Barra superior de navegación
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeTopBar(
    photoUrl: Uri?,
    displayName: String,
    onProfileClick: () -> Unit,
    onGroupManagementClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = rememberImagePainter(data = photoUrl ?: R.drawable.default_avatar),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayName,
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
                DropdownMenuItem(text = { Text("Perfil") }, onClick = onProfileClick)
                DropdownMenuItem(text = { Text("Grupo") }, onClick = onGroupManagementClick)
                DropdownMenuItem(text = { Text("Logout") }, onClick = onLogoutClick)
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF0D47A1))
    )
}