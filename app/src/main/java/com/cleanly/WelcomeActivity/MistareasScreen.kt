import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.cleanly.WelcomeActivity.WelcomeBarra

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Tareas") })
        },
        bottomBar = {
            WelcomeBarra { selectedScreen ->
                when (selectedScreen) {
                    "Mis Tareas" -> navController.navigate("mis_tareas")
                    "Zonas" -> navController.navigate("zonas")
                    "Estadísticas" -> navController.navigate("estadisticas")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Contenido de la pantalla de Mis Tareas
            Text("Contenido de las Mis Tareas")
        }
    }
}
