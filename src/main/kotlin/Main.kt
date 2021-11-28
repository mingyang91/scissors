import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.application
import dev.famer.scissors.RPCUtils
import dev.famer.state.LiveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Scissors")

@Composable
@Preview
fun App(onCloseRequest: () -> Unit) {
    var liveState by remember { mutableStateOf<LiveState>(LiveState.Start) }
    var modelProcess by remember { mutableStateOf<Process?>(null) }

    fun close() {
        if (modelProcess != null && modelProcess!!.isAlive) {
            modelProcess!!.destroy()
        }
        onCloseRequest()
    }
    LaunchedEffect(1) {
        try {
            RPCUtils.unzip(
                onProgress = { count, current ->
                    logger.info("初始化：${current}/$count")
                    liveState = LiveState.Initializing(current, count)
                }
            )
            modelProcess = RPCUtils.startModelService()
            logger.info("初始化完成, 模型服务已启动 PID: ${modelProcess!!.pid()}")
            liveState = LiveState.Serve
        } catch (e: Throwable) {
            liveState = LiveState.Error(e.message ?: "未知错误")
        }
    }
    when (liveState) {
        is LiveState.Start -> {
            Dialog({}, resizable = false) {
                Text("软件启动中")
            }
        }
        is LiveState.Initializing -> {
            val s = liveState as LiveState.Initializing
            Initialization(s.current, s.total)
        }
        is LiveState.Serve -> {
            Serve(::close)
        }
        is LiveState.Error -> {
            Dialog(::close, resizable = false) {
                Text("软件初始化遇到错误，请重新安装软件\n${(liveState as LiveState.Error).message}")
            }
        }
    }
}

fun main() {
    logger.info("软件启动")

    application {
        App(onCloseRequest = {
            exitApplication()
        })
    }
}

