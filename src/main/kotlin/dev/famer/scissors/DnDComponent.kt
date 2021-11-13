package dev.famer.scissors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.text.toLowerCase
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import javax.swing.JPanel

@Composable
fun DnDComponent(onDrop: (List<File>) -> Unit) {
    val files = remember { mutableStateListOf<File>() }

    SwingPanel(factory = {
        JPanel().apply {
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

                override fun dragOver(dtde: DropTargetDragEvent?) {
                    super.dragOver(dtde)
                    onDrop(files.toList())
                }
            }
        }
    })
}

