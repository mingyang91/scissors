// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.io.path.readText
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.nio.file.Files
import javax.imageio.ImageIO

@Composable
@Preview
fun App() {
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
        SwingPanel(background = Color.White, factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                dropTarget = object : DropTarget() {
                    override fun dragEnter(dtde: DropTargetDragEvent?) {
                        super.dragEnter(dtde)
                        val list = dtde?.transferable?.getTransferData(DataFlavor.javaFileListFlavor)
                        if (list is List<*>) {
                            list.filterIsInstance<File>()
                                .forEach { it ->
                                    val pdf = Loader.loadPDF(it)
                                    pdf.pages.map { page ->
                                        val resources = page.resources
                                        resources.xObjectNames
                                            .map(resources::getXObject)
                                            .filterIsInstance<PDImageXObject>()
                                            .toList()
                                            .forEach { pd ->
                                                val file = File.createTempFile("pdf", ".png")
                                                ImageIO.write(pd.image, "png", file)
                                                println(file.toString())
                                            }
                                    }
                                    println(it)
                                }
                        }

                    }
                }
            }
        })

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
