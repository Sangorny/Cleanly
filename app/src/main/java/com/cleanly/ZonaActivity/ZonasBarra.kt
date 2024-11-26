package com.cleanly.ZonaActivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZonasBarra(completedTasks: Int, totalTasks: Int) {
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = Color.Green
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Completadas: $completedTasks / $totalTasks",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
