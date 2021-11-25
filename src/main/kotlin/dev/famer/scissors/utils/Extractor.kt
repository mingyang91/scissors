package dev.famer.scissors.utils

import dev.famer.scissors.PDFUtils
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

object Extractor {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val file = Paths.get("D:\\BaiduYunDownload\\检测报告.pdf")
//            PDFUtils.split(file)
//            PDFUtils.extractAllImages(file)
//                .second
//                .take(10)
//                .collect { value ->
//                    println(value)
//                }
        }
    }

}


