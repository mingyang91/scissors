package dev.famer.build

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Future

open class DownloadPythonTask : DefaultTask() {
    @TaskAction
    fun greet() {
        runBlocking {
            impl()
        }
        println("Run!!!!!!!!!!!!!!!!!!")
    }

    private suspend fun impl() {
        val client = HttpClient(CIO)

        val file = Files.createTempFile("python", "zip")
        val ch = FileChannel.open(file)

        client.get<HttpStatement>("https://www.python.org/ftp/python/3.10.0/python-3.10.0-embed-amd64.zip")
            .execute { response: HttpResponse ->
                val channel: ByteReadChannel = response.receive()
            }
    }
}