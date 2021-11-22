import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.famer.scissors.PDFUtils
import dev.famer.scissors.RPCUtils
import dev.famer.scissors.components.main.Done
import dev.famer.scissors.components.main.Prepare
import dev.famer.scissors.components.main.Processing
import dev.famer.scissors.components.main.Starting
import dev.famer.scissors.models.Span
import dev.famer.state.MainState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

private val logger = LoggerFactory.getLogger("Main")

@Composable
@Preview
fun App(onCloseRequest: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    var mainState by remember { mutableStateOf<MainState>(MainState.Starting) }
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
            mainState = MainState.Starting
            return
        }
        val file = files.first()
        mainState = MainState.Before(file.name)
        show("处理开始")
        coroutineScope.launch {
            val (count, stream) = PDFUtils.extractAllImages(file.toPath())
            mainState = MainState.Processing(file.name, count, 0)
            flow {
                var accumulation: MutableList<Path> = mutableListOf()
                stream
                    .collect { (pageNumber, pageFile) ->
                        mainState = MainState.Processing(file.name, count, pageNumber)
                        val clf = RPCUtils.clf(pageFile)
                        if (clf == "\"cover\"") {
                            val spans = RPCUtils.ocr(pageFile)
                            val newLog = spans.map(Span::text).joinToString()
                            show(newLog)
                            emit(accumulation.toList())
                            accumulation = mutableListOf(pageFile)
                        } else {
                            show("内容页，跳过")
                            accumulation.add(pageFile)
                        }
                    }
                emit(accumulation.toList())
            }
                .collect {
                    show("---共 ${it.size} 页，下一份---")
                }

            mainState = MainState.Done(file.name, count)
        }
            .invokeOnCompletion {
                if (it != null) logger.error("切分异常中断", it)
                show("处理结束")
                mainState = MainState.Done(file.name, 0)
            }

    }
    Window(
        title = title,
        onCloseRequest = onCloseRequest) {
        MaterialTheme {
            Column {
                when (mainState) {
                    is MainState.Starting -> {
                        title = "欢迎使用 Scissors PDF 切分软件"
                        Starting(::onDropHandler)
                    }
                    is MainState.Before -> {
                        val state = mainState as MainState.Before
                        title = "Scissors: 准备处理文件 ${state.filename}"
                        Prepare(state)
                    }
                    is MainState.Processing -> {
                        val state = mainState as MainState.Processing
                        title = "Scissors: 文件处理中 (${state.current}/${state.pageCount}) ${state.filename}"
                        Processing(logs, state)
                    }
                    is MainState.Done -> {
                        val state = mainState as MainState.Done
                        title = "Scissors: 文件处理完毕 ${state.filename}"
                        Done(state, ::onDropHandler)
                    }
                }
            }
        }
    }
}

fun main() {
//    val process = RPCUtils.startModelService()

    application {
        App(onCloseRequest = {
            exitApplication()
//            process.destory()
        })
    }
}

