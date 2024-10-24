package com.cleanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.cleanly.ui.theme.CleanlyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initAuth()

        setContent {
            CleanlyTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}
