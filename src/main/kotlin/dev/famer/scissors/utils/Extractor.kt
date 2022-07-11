package dev.famer.scissors.utils

import dev.famer.scissors.PDFUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

object Extractor {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        PDFUtils.extractAllImages(
            Path.of(
                """D:\复旦大学软件\文档切割\易老师文档样本""",
                "2022个剂0707",
                "20220708114415.pdf"
            )
        )
            .second
            .last()
        println("=========================")
        println("||done!                ||")
        println("=========================")
        delay(600.seconds)
    }

}


