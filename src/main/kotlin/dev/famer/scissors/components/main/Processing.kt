package dev.famer.scissors.components.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.famer.state.ServeState

@Composable
fun Processing(logs: List<String>, state: ServeState.Processing) {
    val (filename, count, current) = state
    Column {
        Text("处理中： $filename", modifier = Modifier.padding(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = current.toFloat() / count,
                modifier = Modifier.padding(12.dp).weight(1f)
            )
            Text("${current}/${count}", modifier = Modifier.padding(12.dp))
        }
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(logs) { log ->
                Text(log, color = Color.LightGray)
            }
        }
    }
}