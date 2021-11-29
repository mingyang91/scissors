package dev.famer.scissors.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.concurrent.fixedRateTimer

object Extractor {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
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
        }
    }

}


