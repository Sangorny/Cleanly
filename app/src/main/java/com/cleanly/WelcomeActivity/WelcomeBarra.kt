package com.cleanly


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun WelcomeBarra(onNavigate: (String) -> Unit) {
    val selectedItem = remember { mutableStateOf(0) }
    val items = listOf("Mis Tareas", "Zonas", "Estadísticas")
    val icons = listOf(Icons.Default.Home, Icons.Default.List, Icons.Default.ShowChart)

    NavigationBar(
        containerColor = Color(0xFF0D47A1),
        contentColor = Color.White
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItem.value == index,
                onClick = {
                    selectedItem.value = index
                    onNavigate(item) // Navegar según el nombre de la sección
                },
                icon = {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = item
                    )
                },
                label = {
                    Text(
                        text = item,
                        color = if (selectedItem.value == index) Color.White else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
