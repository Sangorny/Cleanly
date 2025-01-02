package com.cleanly.TareasActivity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CRUDboton(
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onList: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
            Text("Agregar", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
            Text("Borrar", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
            Text("Editar", fontSize = 16.sp)
        }

    }
}
