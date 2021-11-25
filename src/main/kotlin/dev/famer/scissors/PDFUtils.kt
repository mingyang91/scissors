package dev.famer.scissors

import dev.famer.scissors.models.Classification
import dev.famer.scissors.models.PageKind
import dev.famer.scissors.models.Span
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.name

object PDFUtils {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    data class Page(val index: Int, val image: Path, val page: PDPage)

    suspend fun extractAllImages(file: Path): Pair<Int, Flow<Page>> {
        val pdf = load(file)
        val dir = createFolder(file.name)
        val flow = pdf.pages
            .asFlow()
            .onCompletion { e ->
                if (e != null) logger.error("PDF process error", e)
                close(pdf)
            }
            .withIndex()
            .map { pageWithIndex ->
                withContext(Dispatchers.IO) {
                    val resources = pageWithIndex.value.resources
                    val obj = resources.xObjectNames
                        .map(resources::getXObject)
                        .filterIsInstance<PDImageXObject>()
                        .take(1)[0]
                    Pair(pageWithIndex.index, obj)
                    Page(pageWithIndex.index, saveImage(obj, dir),pageWithIndex.value)
                }
            }

        return Pair(pdf.pages.count, flow)
    }

    suspend fun classification(file: Path): Pair<Int, Flow<PageKind>> {
        val (count, flow) = extractAllImages(file)

        val clfFlow = flow.map { (index, path, page) ->
            when (RPCUtils.clf(path)) {
                is Classification.Cover -> {
                    val spans = RPCUtils.ocr(path)
                    PageKind.Cover(index, page, spans)
                }
                is Classification.Content -> {
                    PageKind.Content(index, page)
                }
                is Classification.HandWrite -> {
                    PageKind.HandWrite(index, page)
                }
            }
        }

        return Pair(count, clfFlow)
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

    suspend fun save(target: Path, pages: List<PDPage>) {
        val doc = PDDocument()
        pages.forEach {
            doc.pages.add(it)
        }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
        }
    }
}