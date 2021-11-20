import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.famer.scissors.DnDComponent
import dev.famer.scissors.PDFUtils
import dev.famer.scissors.RPCUtils
import dev.famer.scissors.models.Span
import dev.famer.state.MainState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("Main")
@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("Hello, World!") }
    val logs = remember { mutableStateListOf<String>() }
    var mainState by remember { mutableStateOf<MainState>(MainState.Starting) }

    fun show(text: String) {
        if (logs.size > 20) {
            logs.removeFirst()
        }
        logs.add(text)
    }

    fun onDropHandler(files: List<File>): Unit {
        if (files.size > 1) {
            show("一次仅能处理一份文件")
            mainState = MainState.Starting
            return
        }
        val file = files.first()
        mainState = MainState.Before(file.name)
        show("处理开始")
        coroutineScope.launch {
            val (count, flow) = PDFUtils.extractAllImages(file.toPath())
            mainState = MainState.Processing(count, 0)
            flow
                .map { pair ->
                    val clf = RPCUtils.clf(pair.second)
                    if (clf == "\"cover\"") {
                        Pair(pair.first, RPCUtils.ocr(pair.second))
                    } else {
                        Pair(pair.first, emptyList())
                    }
                }
                .collect { value ->
                    mainState = MainState.Processing(count, value.first)
                    val newLog = value.second.map(Span::text).joinToString()
                    if (newLog.isEmpty()) {
                        show("内容页，跳过")
                    } else {
                        show(newLog)
                    }
                }
            mainState = MainState.Done(count)
        }
            .invokeOnCompletion {
                if (it != null) logger.error("切分异常中断", it)
                show("处理结束")
                mainState = MainState.Done(0)
            }

    }

    MaterialTheme {
        Column {
            DnDComponent(::onDropHandler)

            when(mainState) {
                is MainState.Processing -> {
                    val (count, current) = mainState as MainState.Processing
                    Row {
                        Text("${current}/${count}")
                        LinearProgressIndicator(progress = current.toFloat() / count)
                    }
                }
                else -> {}
            }

            Button(onClick = {
                text = "Hello, Desktop!"
            }) {
                Text(text)
            }

            LazyColumn {
                items(logs) { log ->
                    Text(log)
                }
            }
        }
    }
}

fun main() {
//    val process = RPCUtils.startModelService()

    application {
        Window(onCloseRequest = {
//            process.destroy()
            exitApplication()
        }) {
            App()
        }
    }
}
