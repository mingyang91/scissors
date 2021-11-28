package dev.famer.scissors.components.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.famer.scissors.DnDComponent
import dev.famer.state.ServeState
import java.io.File

@Composable
fun Done(state: ServeState.Done, onDropHandler: (files: List<File>) -> Unit) {
    Column {
        Text(
            text = "Tips: 将需要切分的 PDF 文件拖拽至下面区域并释放鼠标。(文件已处理完毕 ${state.filename})",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        DnDComponent(onDropHandler)
    }
}