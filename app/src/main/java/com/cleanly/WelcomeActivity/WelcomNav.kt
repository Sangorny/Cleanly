package com.cleanly.WelcomeActivity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BotonesNavegacion() {
    BottomAppBar(
        modifier = Modifier.height(70.dp),
        containerColor = Color.Transparent,
        content = {
            Button(
                onClick = { /* Navegación a Inicio */ },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Inicio", color = Color.White)
            }

            Button(
                onClick = { /* Navegación a Tareas */ },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Tareas", color = Color.White)
            }
        }
    )
}
