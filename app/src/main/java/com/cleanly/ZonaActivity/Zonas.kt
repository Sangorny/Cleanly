package com.cleanly


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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


//Parte de Zonas
@Composable
fun Zonas(
    groupId: String,
    onZoneClick: (String) -> Unit
) {
    val zones = listOf(
        "Aseo" to R.drawable.bano,
        "Cocina" to R.drawable.cocina,
        "Sala" to R.drawable.salon,
        "Dormitorio" to R.drawable.dormitorio,
        "Garaje/Trastero" to R.drawable.default1,
        "Exterior" to R.drawable.default5
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
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
                onZoneClick = onZoneClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ZoneGrid(
    zones: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    onZoneClick: (String) -> Unit
) {

    val customFontFamily = FontFamily(
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
                    .clickable { onZoneClick(zoneName) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )

                Text(
                    text = zoneName,
                    color = Color.Blue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = customFontFamily,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }
        }
    }
}
