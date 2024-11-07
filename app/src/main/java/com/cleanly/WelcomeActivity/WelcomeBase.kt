package com.cleanly.WelcomeActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasePantalla(
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = {
            BotonesNavegacion()
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .background(Color.Transparent)
                    .then(Modifier.padding(0.dp))
            ) {
                content(Modifier)
            }
        }
    )
}
