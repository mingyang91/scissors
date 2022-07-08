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
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlin.math.abs
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
        val out = file.resolveSibling(file.name + "_split").let {
            if (!it.exists()) it.createDirectory()
            it
        }
        val splitOut = out.resolve("正式报告含原始数据")
        cleanOutTarget(splitOut)
        val removeHandwriteOut = out.resolve("正式报告")
        cleanOutTarget(removeHandwriteOut)
        val firstAndLastOut = out.resolve("正文首页与末页")
        cleanOutTarget(firstAndLastOut)
        val firstOnlyOut = out.resolve("仅正文首页")
        cleanOutTarget(firstOnlyOut)
        val coverAndLastOut = out.resolve("封面与末页")
        cleanOutTarget(coverAndLastOut)
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
                        log("[${kind.index + 1}] 内容页")
                        accumulation.add(kind)
                    }
                    is PageKind.HandWrite -> {
                        log("[${kind.index + 1}] 手写页")
                        accumulation.add(kind)
                    }
                    is PageKind.Unknown -> {
                        log("[${kind.index + 1}] !!未识别页!!")
                        accumulation.add(kind)
                    }
                }
                // last page
                if (kind.index + 1 == count) emit(Pair(filename, accumulation.toList()))
            }
            .collect { (filename, pages) ->
                log("---共 ${pages.size} 页，下一份---")
                val outName = if (pages.filterIsInstance<PageKind.Unknown>().isEmpty()) {
                    "$filename.pdf"
                } else {
                    "!!!WARN!!! - $filename.pdf"
                }

                val completedFile = splitOut.resolve(outName)
                if (!completedFile.exists()) {
                    saveAll(completedFile, pages)
                } else log("[完整] $outName 已存在，跳过")

                val withoutHandwriteFile = removeHandwriteOut.resolve(outName)
                if (!withoutHandwriteFile.exists()) {
                    saveWithOutHandWrite(withoutHandwriteFile, pages)
                } else log("[无手写] $outName 已存在，跳过")

                val firstAndLastFile = firstAndLastOut.resolve(outName)
                if (!firstAndLastFile.exists()) {
                    saveFirstAndLast(firstAndLastFile, pages)
                } else log("[首页与末页] $outName 已存在，跳过")

                val firstOnlyFile = firstOnlyOut.resolve(outName)
                if (!firstOnlyFile.exists()) {
                    saveFirst(firstOnlyFile, pages)
                } else log("[仅首页] $outName 已存在，跳过")

                val coverAndLastFile = coverAndLastOut.resolve(outName)
                if (!coverAndLastFile.exists()) {
                    saveCoverAndLast(coverAndLastFile, pages)
                } else log("[封面与末页] $outName 已存在，跳过")
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

    private suspend fun classification(file: Path): Pair<Int, Flow<PageKind>> {
        val (count, flow) = extractAllImages(file)

        val clfFlow = flow.map { (index, path, page) ->
            val res = when (val clf = RPCUtils.clf(path)) {
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
                is Classification.Error -> {
                    logger.error("样本无法识别：" + clf.message)
                    PageKind.Unknown(index, page, clf.message)
                }
            }
            path.deleteIfExists()
            res
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

    private suspend fun saveWithOutHandWrite(target: Path, pages: List<PageKind>) {
        val doc = PDDocument()

        pages
            .forEach {
                if (it !is PageKind.HandWrite) {
                    doc.pages.add(it.page)
                }
            }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }

    private suspend fun saveAll(target: Path, pages: List<PageKind>) {
        val doc = PDDocument()

        pages
            .forEach { doc.pages.add(it.page) }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }

    private suspend fun saveFirstAndLast(target: Path, pages: List<PageKind>) {
        val doc = PDDocument()

        val dropped = pages.filterIsInstance<PageKind.Content>()
        listOf(dropped.drop(1).firstOrNull(), dropped.lastOrNull())
            .filterNotNull()
            .distinct()
            .forEach { doc.pages.add(it.page) }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }

    private suspend fun saveFirst(target: Path, pages: List<PageKind>) {
        val doc = PDDocument()

        val dropped = pages.filterIsInstance<PageKind.Content>()
        listOf(dropped.drop(1).firstOrNull())
            .filterNotNull()
            .forEach { doc.pages.add(it.page) }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }

    private suspend fun saveCoverAndLast(target: Path, pages: List<PageKind>) {
        val doc = PDDocument()

        val cover = pages.filterIsInstance<PageKind.Cover>()
        val dropped = pages.filterIsInstance<PageKind.Content>()
        listOf(cover.firstOrNull(), dropped.lastOrNull())
            .filterNotNull()
            .forEach { doc.pages.add(it.page) }

        withContext(Dispatchers.IO) {
            doc.save(target.toFile())
            doc.close()
        }
    }
}