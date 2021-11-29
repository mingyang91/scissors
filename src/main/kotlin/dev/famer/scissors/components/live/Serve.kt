import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import dev.famer.scissors.PDFUtils
import dev.famer.scissors.components.serve.Done
import dev.famer.scissors.components.serve.Prepare
import dev.famer.scissors.components.serve.Processing
import dev.famer.scissors.components.serve.Starting
import dev.famer.state.ServeState
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("Serve")

@Composable
@Preview
fun Serve(onCloseRequest: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    var serveState by remember { mutableStateOf<ServeState>(ServeState.Starting) }
    var title by remember { mutableStateOf("") }

    fun show(text: String) {
        if (logs.size > 20) {
            logs.removeFirst()
        }
        logs.add(text)
    }

    fun onDropHandler(files: List<File>): Unit {
        if (files.size > 1) {
            show("一次仅能处理一份文件")
            serveState = ServeState.Starting
            return
        }
        val file = files.first()
        serveState = ServeState.Before(file.name)
        show("处理开始")
        coroutineScope.launch {
            PDFUtils.split(
                file = file.toPath(),
                onProcessing = { filename, count, index ->
                    serveState = ServeState.Processing(filename, count, index)
                },
                onDone = { filename, count ->
                    serveState = ServeState.Done(filename, count)
                },
                log = ::show
            )
        }
            .invokeOnCompletion {
                if (it != null) logger.error("切分异常中断", it)
                show("处理结束")
                serveState = ServeState.Done(file.name, 0)
            }
    }
    Window(
        title = title,
        onCloseRequest = onCloseRequest) {
        MaterialTheme {
            Column {
                when (serveState) {
                    is ServeState.Starting -> {
                        title = "欢迎使用 Scissors PDF 切分软件"
                        Starting(::onDropHandler)
                    }
                    is ServeState.Before -> {
                        val state = serveState as ServeState.Before
                        title = "Scissors: 准备处理文件 ${state.filename}"
                        Prepare(state)
                    }
                    is ServeState.Processing -> {
                        val state = serveState as ServeState.Processing
                        title = "Scissors: 文件处理中 (${state.current}/${state.pageCount}) ${state.filename}"
                        Processing(logs, state)
                    }
                    is ServeState.Done -> {
                        val state = serveState as ServeState.Done
                        title = "Scissors: 文件处理完毕 ${state.filename}"
                        Done(state, ::onDropHandler)
                    }
                }
            }
        }
    }
}