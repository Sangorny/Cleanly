package com.cleanly

import android.content.Intent
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun Zonas(onZoneClick: (String) -> Unit) {
    val zones = remember {
        mutableStateListOf(
            "Aseo" to R.drawable.bano,
            "Cocina" to R.drawable.cocina,
            "Sala" to R.drawable.salon,
            "Dormitorio" to R.drawable.dormitorio
        )
    }
    val showDialog = remember { mutableStateOf(false) }
    val newZoneName = remember { mutableStateOf("") }
    val selectedImage = remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current // Obtén el contexto una vez aquí

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

            // Llama a ZoneGrid pasando el contexto
            ZoneGrid(
                zones = zones,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onZoneClick = { zoneName ->
                    val intent = Intent(context, TareaActivity::class.java)
                    intent.putExtra("zona", zoneName)
                    context.startActivity(intent)
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
                        TextField(
                            value = newZoneName.value,
                            onValueChange = { newZoneName.value = it },
                            placeholder = { Text("Nombre de la zona") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val defaultImages = listOf(
                                R.drawable.default1,
                                R.drawable.default2,
                                R.drawable.default3,
                                R.drawable.default4,
                                R.drawable.default5
                            )
                            items(defaultImages) { image ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedImage.value == image) Color.LightGray else Color.Transparent)
                                        .clickable { selectedImage.value = image },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = image),
                                        contentDescription = null
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
                    Button(onClick = { showDialog.value = false }) {
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
    onZoneClick: (String) -> Unit,
    onAddZoneClick: () -> Unit
) {
    // Define una familia de fuentes personalizada
    val customFontFamily = FontFamily(
        Font(R.font.bernadette) // Cambia a tu fuente personalizada
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
                    .clickable { onZoneClick(zoneName) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )

                // Texto con tipo de fuente, color personalizado y ajustado hacia arriba
                Text(
                    text = zoneName,
                    color = Color.Blue, // Cambia el color del texto
                    fontSize = 18.sp, // Ajusta el tamaño del texto
                    fontWeight = FontWeight.Bold, // Define el grosor del texto
                    fontFamily = customFontFamily, // Usa la fuente personalizada
                    modifier = Modifier
                        .align(Alignment.TopCenter) // Centra el texto horizontalmente y lo empuja hacia arriba
                        .padding(top = 6.dp) // Ajusta la posición hacia arriba
                )
            }
        }

        // Botón para agregar nueva zona
        item {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onAddZoneClick() },
                contentAlignment = Alignment.Center // Centra todo dentro del Box
            ) {
                // Imagen que ocupa todo el espacio del Box
                Image(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Agregar zona",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Texto encima de la imagen
                Text(
                    text = "Agregar",
                    color = Color.Blue, // Asegúrate de usar un color visible sobre la imagen
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, // Define el grosor del texto
                    fontFamily = customFontFamily, // Usa la fuente personalizada
                    modifier = Modifier
                        .align(Alignment.TopCenter) // Centra el texto horizontalmente y lo empuja hacia arriba
                        .padding(top = 6.dp) // Ajusta la posición hacia arriba
                )
            }
        }
    }
}
