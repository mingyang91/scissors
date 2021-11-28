package dev.famer.scissors.components.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.famer.state.ServeState

@Preview
@Composable
fun Prepare(state: ServeState.Before) {
    Column {
        Text("准备中: " + state.filename)
        CircularProgressIndicator(modifier = Modifier.size(100.dp))
    }
}