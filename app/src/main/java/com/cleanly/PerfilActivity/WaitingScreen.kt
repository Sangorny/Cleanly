package com.cleanly.PerfilActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cleanly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun WaitingScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = navController.context

    LaunchedEffect(Unit) {
        delay(2000) // Tiempo de espera opcional en el WaitingScreen

        if (currentUser != null) {
            val userId = currentUser.uid

            // Verificamos si el usuario está en 'singrupo'
            val db = FirebaseFirestore.getInstance()
            val usuariosRef = db.collection("grupos").document("singrupo")
                .collection("usuarios")

            usuariosRef.whereEqualTo("uid", userId).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        // Si el usuario no está en 'singrupo', significa que ya tiene un grupo
                        navController.navigate("welcome") {
                            popUpTo("waiting_screen") { inclusive = true }
                        }
                    } else {
                        // Si está en 'singrupo', redirigimos al usuario a 'GroupScreen' para que cree o se una a un grupo
                        navController.navigate("group_screen/$userId") {
                            popUpTo("waiting_screen") { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    // En caso de error al verificar, podemos manejarlo y redirigir a login
                    navController.navigate("login") {
                        popUpTo("waiting_screen") { inclusive = true }
                    }
                }
        } else {
            // Si no hay usuario autenticado, redirigimos a login
            navController.navigate("login") {
                popUpTo("waiting_screen") { inclusive = true }
            }
        }
    }

    // Interfaz de usuario del WaitingScreen
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
        CircularProgressIndicator() // El indicador de carga mientras se realiza la verificación
    }
}
