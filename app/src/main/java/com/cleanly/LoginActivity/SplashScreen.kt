package com.cleanly

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    // Verificar autenticación del usuario en el inicio
    val auth = FirebaseAuth.getInstance()
    LaunchedEffect(Unit) {
        delay(3000) // Tiempo de espera opcional en el SplashScreen

        if (auth.currentUser != null) {
            // Usuario autenticado, iniciar TareaActivity
            val intent = Intent(navController.context, TareaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            navController.context.startActivity(intent)
        } else {
            // Usuario no autenticado, ir a LoginScreen
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Interfaz de usuario del SplashScreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1),
                        Color(0xFF00E676)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Logo de Cleanly",
            modifier = Modifier.size(300.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
    }
}


