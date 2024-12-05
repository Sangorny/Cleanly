package com.cleanly

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
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        delay(3000) // Tiempo de espera opcional en el SplashScreen

        // Verificar si el usuario ya está autenticado
        if (auth.currentUser != null) {
            // Si está logueado, redirigir a Welcome, y limpiar la pila de navegación
            navController.navigate("welcome") {
                popUpTo("splash") { inclusive = true } // Limpiar la pantalla Splash de la pila
                launchSingleTop = true  // Evitar duplicados si ya está en la pila
            }
        } else {
            // Si no está logueado, redirigir a LoginScreen
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true } // Limpiar la pantalla Splash de la pila
                launchSingleTop = true  // Evitar duplicados si ya está en la pila
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




