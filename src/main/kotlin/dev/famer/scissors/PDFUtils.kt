package dev.famer.scissors

import dev.famer.scissors.models.Classification
import dev.famer.scissors.models.PageKind
import dev.famer.scissors.models.Point
import dev.famer.scissors.models.Span
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.LoggerFactory
import kotlin.math.abs
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlin.math.max
import kotlin.math.min

object PDFUtils {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    data class Page(val index: Int, val image: Path, val page: PDPage)

    private val ColonLike = setOf(":", ";", "；", "：", ",", "，", ".", "。")

    suspend fun split(file: Path,
                      onProcessing: (filename: String, count: Int, index: Int) -> Unit,
                      onDone: (filename: String, count: Int) -> Unit,
                      log: (String) -> Unit) {
        val preface = load(locatePreface())
        val (count, stream) = classification(file)
        val splitOut = file.resolveSibling("切分插页")
        cleanOutTarget(splitOut)
        val removeHandwriteOut = file.resolveSibling("移除手写页")
        cleanOutTarget(removeHandwriteOut)
        onProcessing(file.name, count, 0)
        var filename = ""
        var accumulation: MutableList<PageKind> = mutableListOf()
        stream
            .onCompletion { preface.close() }
            .transform { kind ->
                onProcessing(file.name, count, kind.index)
                when(kind) {
                    is PageKind.Cover -> {
                        if (accumulation.isNotEmpty()) emit(Pair(filename, accumulation.toList()))

                        log("[${kind.index + 1}] 首页，创建新文档")
                        val texts = kind.spans.map(Span::text)
                        val code = texts.firstOrNull { it.startsWith("FYS") } ?: "未识别编号${kind.index}"
                        val clientTitleSpan = kind.spans.firstOrNull { it.text.contains("委托单位") }
                        var client: String = "未识别单位${kind.index}"
                        if (clientTitleSpan != null) {
                            if (clientTitleSpan.text.length > 6) {
                                val dropped = clientTitleSpan.text.drop("委托单位".length)
                                client = if (dropped.startsWith(":") || dropped.startsWith("：")) dropped.drop(1)
                                else dropped
                            } else {
                                val (min, max) = getRowRange(clientTitleSpan.areas)
                                val find = kind.spans.firstOrNull { span ->
                                    span != clientTitleSpan &&
                                    span.areas.all { p -> p.x > min.x && p.x < max.x && p.y > min.y && p.y < max.y }
                                }

                                if (find != null) {
                                    client = find.text
                                }
                            }

                            if (ColonLike.any(client::startsWith)) {
                                client = client.drop(1)
                            }
                            client = client.trim()
                        }
                        filename = "${code}-${client}"
                        log("文件：$filename")
                        accumulation = mutableListOf(kind)
                    }
                    is PageKind.Content -> {
                        log("[${kind.index + 1}] 内容页，附加至文档")
                        accumulation.add(kind)
                    }
                    is PageKind.HandWrite -> {
                        log("[${kind.index + 1}] 手写页")
                        accumulation.add(kind)
                    }
                }
                // last page
                if (kind.index + 1 == count) emit(Pair(filename, accumulation.toList()))
            }
            .collect { (filename, pages) ->
                log("---共 ${pages.size} 页，下一份---")
                val completedFile = splitOut.resolve("$filename.pdf")
                if (!completedFile.exists()) {
                    saveAll(completedFile, preface.pages.toList(), pages)
                } else log("$filename 已存在，跳过")

                val withoutHandwriteFile = removeHandwriteOut.resolve("$filename.pdf")
                if (!withoutHandwriteFile.exists()) {
                    saveWithOutHandWrite(withoutHandwriteFile, preface.pages.toList(), pages)
                }
                else log("$filename 已存在，跳过")
            }

        onDone(file.name, count)
    }

    fun getRowRange(area: List<Point>): Pair<Point, Point> {
        val p1 = area.get(0)
        val p2 = area.get(2)
        val xDistance = abs(p1.x - p2.x)
        val yDistance = abs(p1.y - p2.y)
        val xMax = max(p1.x, p2.x) + xDistance * 0.2f
        val xMin = min(p1.x, p2.x) - xDistance * 0.2f
        val yMax = max(p1.y, p2.y) + yDistance * 0.2f
        val yMin = min(p1.y, p2.y) - yDistance * 0.2f
        return if (xDistance > yDistance) {
            Pair(Point(0f, yMin), Point(Float.MAX_VALUE, yMax))
        } else {
            Pair(Point(xMin, 0f), Point(xMax, Float.MAX_VALUE))
        }
    }

    suspend fun cleanOutTarget(dir: Path) = withContext(Dispatchers.IO) {
        if (dir.exists()) {
            dir.listDirectoryEntries()
                .forEach { it.deleteExisting() }
            dir.deleteExisting()
        }

        dir.createDirectory()
    }

    fun locatePreface(): Path {
        val propertyFirst: String = System.getProperty("compose.application.resources.dir")
            ?: (System.getProperty("user.dir") + "\\resources\\windows-x64\\")

        return Path.of(propertyFirst).resolve("preface.pdf")
    }


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

    suspend fun saveWithOutHandWrite(target: Path, preface: List<PDPage>, pages: List<PageKind>) {
        val doc = PDDocument()

        pages
            .flatMap {
                when (it) {
                    is PageKind.Cover -> {
                        listOf(it.page) + preface
                    }
                    is PageKind.Content -> {
                        listOf(it.page)
                    }
                    is PageKind.HandWrite -> {
                        emptyList()
                    }
                }
            }
            .forEach { doc.pages.add(it) }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }

    suspend fun saveAll(target: Path, preface: List<PDPage>, pages: List<PageKind>) {
        val doc = PDDocument()

        pages
            .flatMap {
                when (it) {
                    is PageKind.Cover -> {
                        listOf(it.page) + preface
                    }
                    else -> {
                        listOf(it.page)
                    }
                }
            }
            .forEach { doc.pages.add(it) }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }
}