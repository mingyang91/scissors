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
}