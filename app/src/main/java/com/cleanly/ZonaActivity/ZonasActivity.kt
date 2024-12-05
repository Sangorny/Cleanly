package com.cleanly.ZonaActivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.cleanly.TareaActivity
import com.cleanly.ui.theme.CleanlyTheme
import com.cleanly.Welcome
import com.cleanly.shared.Tarea

class ZonasActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CleanlyTheme {
                val navController = rememberNavController()

                Welcome(
                    navController = navController,
                    onTareaClick = { tarea ->
                        // Lógica para manejar clics en tareas desde ZonasActivity
                        val intent = Intent(this, TareaActivity::class.java)
                        intent.putExtra("tarea_nombre", tarea.nombre)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
