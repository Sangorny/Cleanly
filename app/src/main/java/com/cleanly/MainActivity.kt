package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa Firebase Auth
        initAuth()

        setContent {
            CleanlyTheme {
                val navController = rememberNavController()

                // Verificación de la autenticación del usuario en el lanzamiento
                LaunchedEffect(Unit) {
                    val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        // Navega a la pantalla de bienvenida si el usuario está autenticado
                        navController.navigate("welcome") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        // Navega a la pantalla de login si no hay usuario autenticado
                        navController.navigate("login")
                    }
                }

                // Configura la navegación de la aplicación
                AppNavigation(navController)
            }
        }
    }
}




