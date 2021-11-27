package dev.famer.scissors.utils

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import java.nio.file.Paths
import java.time.Instant
import kotlin.concurrent.fixedRateTimer
import kotlin.coroutines.EmptyCoroutineContext

object Extractor {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val file = Paths.get("D:\\BaiduYunDownload\\检测报告.pdf")
            val f: Flow<Int> = callbackFlow<Unit> {
                val timer = fixedRateTimer("test", true, 0, 1000) {
                    trySend(Unit)
                }
                awaitClose { timer.cancel() }
            }
                .map { Instant.now().nano }
                .take(10)
                .shareIn(this, SharingStarted.Lazily)

            val f1: Deferred<Unit> = async<Unit> { f.collect { println(it) } }
            val f2: Deferred<Unit> = async<Unit> { f.collect { println(it) } }
            f1.await()
            f2.await()
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


