package dev.famer.scissors

import dev.famer.scissors.models.Span
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import java.io.File

object OCRUtils {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
    suspend fun rpc(file: File): List<Span> {
        val res: List<Span> = client.get("http://localhost:9000") {
            parameter("path", file.toString())
        }
        return res
    }
}