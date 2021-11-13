package dev.famer.scissors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import javax.imageio.ImageIO

object PDFUtils {
    suspend fun extractAllImages(file: File): Flow<Pair<Int, File>> {
        val pdf = load(file)
        val manus = pdf.pages
            .flatMapIndexed { index, page ->
                val resources = page.resources
                resources.xObjectNames
                    .map(resources::getXObject)
                    .filterIsInstance<PDImageXObject>()
                    .map { Pair(index, it) }
            }
        return flowOf(*manus.toList().toTypedArray())
            .map { pair ->
                Pair(pair.first, saveImage(pair.second))
            }

    }

    private suspend fun load(file: File): PDDocument = withContext(Dispatchers.IO) {
        Loader.loadPDF(file)
    }

    private suspend fun saveImage(imgObj: PDImageXObject): File = withContext(Dispatchers.IO) {
        val file = File.createTempFile("pdf", ".png")
        ImageIO.write(imgObj.image, "png", file)
        file
    }
}