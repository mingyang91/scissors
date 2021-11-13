import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.famer.scissors.DnDComponent
import dev.famer.scissors.OCRUtils
import dev.famer.scissors.PDFUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.nio.file.Path

@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("Hello, World!") }
    val resourceRoot = {
        val propertyFirst: String = System.getProperty("compose.application.resources.dir")
            ?: (System.getProperty("user.dir") + "\\resources\\windows-x64\\")

        val file = Path.of(propertyFirst).resolve("python/python.exe")
        val pb = ProcessBuilder()
            .command(file.toString(), "--version")
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        pb.start()
    }
    resourceRoot()


    MaterialTheme {
        DnDComponent { files ->
            coroutineScope.launch {
                flowOf(*files.toTypedArray())
                    .flatMapConcat { PDFUtils.extractAllImages(it) }
                    .map { pair ->
                        Pair(pair.first, OCRUtils.rpc(pair.second))
                    }
                    .collect { value ->
                        println(value)
                    }
            }
        }

        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
