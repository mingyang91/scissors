package dev.famer.scissors

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JPanel

@Composable
fun DnDComponent(onDrop: (List<File>) -> Unit) {
    val files = remember { mutableStateListOf<File>() }

    SwingPanel(
        background = Color.White,
        modifier = Modifier.size(270.dp, 90.dp),
        factory = {
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            dropTarget = object : DropTarget() {
                override fun dragEnter(dtde: DropTargetDragEvent?) {
                    super.dragEnter(dtde)
                    val list = dtde?.transferable?.getTransferData(DataFlavor.javaFileListFlavor)
                    if (list is List<*>) {
                        files.clear()

                        val pdfFileList = list.filterIsInstance<File>()
                            .filter { it.extension.lowercase() == "pdf" }

                        files.addAll(pdfFileList)
                    }
                }

                override fun drop(dtde: DropTargetDropEvent?) {
                    super.drop(dtde)
                    onDrop(files.toList())
                }
            }
        }
    })
}

