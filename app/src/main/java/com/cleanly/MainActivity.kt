package com.cleanly


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.ui.theme.CleanlyTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().setLanguageCode("es")
        setContent {
            CleanlyTheme {
                AppNavigation()
            }
        }
    }
}
