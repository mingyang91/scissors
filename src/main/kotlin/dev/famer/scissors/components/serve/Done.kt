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
import dev.famer.state.ServeState
import java.io.File

@Composable
fun Done(state: ServeState.Done, onDropHandler: (files: List<File>) -> Unit) {
    Column {
        Text(
            text = "您的检验报告已处理完毕-${state.filename}，请至该文件所在目录查看处理结果",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        DnDComponent(onDropHandler)
    }
}