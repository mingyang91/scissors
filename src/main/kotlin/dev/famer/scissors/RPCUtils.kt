package dev.famer.scissors

import dev.famer.scissors.models.Span
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import java.nio.file.Path

object RPCUtils {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
    suspend fun ocr(file: Path): List<Span> {
        val res: List<Span> = client.get("http://localhost:9000/ocr") {
            parameter("path", file.toString())
        }
        return res
    }

    suspend fun clf(file: Path): String {
        val res: String = client.get("http://localhost:9000/clf") {
            parameter("path", file.toString())
        }
        return res
    }

    suspend fun health(): Boolean {
        val res: String = client.get("http://localhost:9000/healt")
        return res.uppercase() == "OK"
    }

    private fun locatePython(): Path {
        val propertyFirst: String = System.getProperty("compose.application.resources.dir")
            ?: (System.getProperty("user.dir") + "\\resources\\windows-x64\\")

        return Path.of(propertyFirst).resolve("python/python.exe")
    }

    fun startModelService(): Process {
        val file = locatePython()
        val pb = ProcessBuilder()
            .command(file.toString(), "--version")
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        return pb.start()
    }
}