package com.cleanly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth

class LogActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            CleanlyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CheckAuthenticationAndNavigate()
                }
            }
        }
    }

    @Composable
    fun CheckAuthenticationAndNavigate() {
        val currentUser = auth.currentUser
        val context = LocalContext.current

        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                // Si el usuario está autenticado, redirigir a la WelcomActivity
                goToWelcomActivity(context)
            }
        }

        if (currentUser == null) {
            // Si el usuario no está autenticado, cargar la navegación para el inicio de sesión
            AppNavigation()
        }
    }

    private fun goToWelcomActivity(context: Context) {
        val intent = Intent(context, WelcomActivity::class.java)
        context.startActivity(intent)
        finish()
    }
}
