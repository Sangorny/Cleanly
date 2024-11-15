package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.ui.theme.CleanlyTheme

class WelcomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CleanlyTheme {
                // Llama al composable principal `Welcome` sin parámetros
                Welcome()
            }
        }
    }
}