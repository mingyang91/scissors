package dev.famer.scissors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.name

object PDFUtils {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    suspend fun extractAllImages(file: Path): Pair<Int, Flow<Pair<Int, Path>>> {
        val pdf = load(file)
        val dir = createFolder(file.name)
        val flow = flowOf(*pdf.pages.toList().toTypedArray())
            .onCompletion { e ->
                if (e != null) logger.error("PDF process error", e)
                close(pdf)
            }
            .withIndex()
            .map { page ->
                withContext(Dispatchers.IO) {
                    val resources = page.value.resources
                    val obj = resources.xObjectNames
                        .map(resources::getXObject)
                        .filterIsInstance<PDImageXObject>()
                        .take(1)[0]
                    Pair(page.index, obj)
                }
            }
            .map { pair ->
                Pair(pair.first, saveImage(pair.second, dir))
            }
        return Pair(pdf.pages.count, flow)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun load(file: Path): PDDocument = withContext(Dispatchers.IO) {
        Loader.loadPDF(file.toFile())
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun close(pdf: PDDocument) = withContext(Dispatchers.IO) {
        pdf.close()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun createFolder(prefix: String) = withContext(Dispatchers.IO) {
        Files.createTempDirectory(prefix)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun saveImage(imgObj: PDImageXObject, folder: Path): Path = withContext(Dispatchers.IO) {
        val file = Files.createTempFile(folder, "", ".png")
        ImageIO.write(imgObj.image, "png", file.toFile())
        file
    }
}