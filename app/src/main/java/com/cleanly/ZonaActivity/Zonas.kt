package com.cleanly.ZonaActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanly.R

@Composable
fun ZonasActivity(onZoneClick: () -> Unit) { // Recibe una función callback
    Text(text = "Zonas", style = MaterialTheme.typography.headlineMedium)
    val zones = remember {
        mutableStateListOf(
            "Baño" to R.drawable.bano,
            "Cocina" to R.drawable.cocina,
            "Sala" to R.drawable.salon,
            "Dormitorio" to R.drawable.dormitorio
        )
    }
    val showDialog = remember { mutableStateOf(false) }
    val newZoneName = remember { mutableStateOf("") }
    val selectedImage = remember { mutableStateOf<Int?>(null) }

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

            ZoneGrid(
                zones = zones,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onZoneClick = {
                    // Llama al callback cuando se haga clic en una zona
                    onZoneClick()
                },
                onAddZoneClick = { showDialog.value = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text(text = "Nueva Zona", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(text = "Introduce el nombre de la nueva zona:")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newZoneName.value,
                            onValueChange = { newZoneName.value = it },
                            placeholder = { Text("Nombre de la zona") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Selecciona una imagen:")
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(150.dp)
                        ) {
                            val defaultImages = listOf(
                                R.drawable.default1,
                                R.drawable.default2,
                                R.drawable.default3,
                                R.drawable.default4,
                                R.drawable.default5
                            )

                            items(defaultImages) { imageRes ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedImage.value == imageRes) Color.LightGray else Color.Transparent)
                                        .clickable { selectedImage.value = imageRes },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = imageRes),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newZoneName.value.isNotBlank() && selectedImage.value != null) {
                                zones.add(newZoneName.value.trim() to selectedImage.value!!)
                                newZoneName.value = ""
                                selectedImage.value = null
                                showDialog.value = false
                            }
                        }
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            newZoneName.value = ""
                            selectedImage.value = null
                            showDialog.value = false
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ZoneGrid(
    zones: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    onZoneClick: () -> Unit,
    onAddZoneClick: () -> Unit
) {
    val CustomBlue = Color(0xFF02A9FF)

    val BernadetteFontFamily = FontFamily(
        Font(R.font.bernadette)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(zones) { (zoneName, imageRes) ->
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onZoneClick() }, // Redirige al hacer clic
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                Text(
                    text = zoneName,
                    color = CustomBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        fontFamily = BernadetteFontFamily
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }
        }

        // Cuadro "Agregar"
        item {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onAddZoneClick() }, // Muestra el diálogo
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                Text(
                    text = "Agregar",
                    color = CustomBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        fontFamily = BernadetteFontFamily
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }
        }
    }
}

