package com.cleanly

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.cleanly.R

@Composable
fun Welcome() {
    // Lista dinámica de zonas
    val zones = remember { mutableStateListOf("Baño", "Cocina", "Sala", "Dormitorio") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1),
                        Color(0xFF00E676)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Zonas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid dinámico para las zonas
            ZoneGrid(
                zones = zones,
                modifier = Modifier
                    .weight(1f) // Ocupa el espacio restante
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para agregar nuevas zonas
            AddZoneButton {
                zones.add("Nueva Zona ${zones.size + 1}")
            }
        }
    }
}

@Composable
fun ZoneGrid(
    zones: List<String>,
    modifier: Modifier = Modifier
) {
    // Mapear zonas con sus imágenes
    val zoneImages = mapOf(
        "Baño" to R.drawable.cocina,
        "Cocina" to R.drawable.cocina,
        "Sala" to R.drawable.cocina,
        "Dormitorio" to R.drawable.cocina
    )
    val CustomBlue = Color(0xFF02A9FF)

    val BernadetteFontFamily = FontFamily(
        Font(R.font.bernadette) // Apunta al archivo en `res/font/bernadette.ttf`
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(zones) { zone ->
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Imagen asociada a la zona
                zoneImages[zone]?.let { imageRes ->
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Imagen de $zone",
                        modifier = Modifier
                            .fillMaxSize() // La imagen ocupa todo el recuadro
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                // Texto superpuesto en la parte superior
                Text(
                    text = zone,
                    color = CustomBlue, // Texto azul
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        fontFamily = BernadetteFontFamily // Usa la fuente Bernadette
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter) // Posición del texto
                        .padding(top = 6.dp) // Espaciado superior
                )
            }
        }
    }
}

@Composable
fun AddZoneButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Agregar Zona",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 16.sp
        )
    }
}
