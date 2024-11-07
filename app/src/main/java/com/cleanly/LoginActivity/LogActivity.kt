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
        auth.signOut()

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
                goToTareaActivity(context)
            }
        }

        if (currentUser == null) {
            AppNavigation()
        }
    }

    private fun goToTareaActivity(context: Context) {
        val intent = Intent(context, TareaActivity::class.java)
        context.startActivity(intent)
        finish()
    }
}
