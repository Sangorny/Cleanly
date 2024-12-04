package com.cleanly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cleanly.ui.theme.CleanlyTheme

class WelcomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanlyTheme {
                MainScreen(
                    onNavigateToTarea = {
                        // Navegar a TareaActivity sin pasar datos
                        val intent = Intent(this, TareaActivity::class.java)
                        startActivity(intent)
                    },
                    onNavigateToZonas = {
                        val intent = Intent(this, ZonasActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
            }
        }
    }
