package dev.famer.scissors.components.serve

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.famer.scissors.DnDComponent
import java.io.File

@Composable
fun Starting(onDropHandler: (files: List<File>) -> Unit) {
    Column {
        Text(
            text = "Tips: 请将需要处理的检验报告拖拽至下面区域并释放鼠标",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        DnDComponent(onDropHandler)
    }
}