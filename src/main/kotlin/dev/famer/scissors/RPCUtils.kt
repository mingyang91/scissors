package dev.famer.scissors

import dev.famer.scissors.models.Classification
import dev.famer.scissors.models.Span
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.util.Identity.decode
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.*
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

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

    suspend fun clf(file: Path): Classification {
        val res: Classification = client.get("http://localhost:9000/clf") {
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

        return Path.of(propertyFirst).resolve("ocr-model\\venv\\Scripts\\python.exe")
    }

    private fun locateEntryFile(): Path {
        val propertyFirst: String = System.getProperty("compose.application.resources.dir")
            ?: (System.getProperty("user.dir") + "\\resources\\windows-x64\\")

        return Path.of(propertyFirst).resolve("ocr-model\\main.py")
    }

    fun startModelService(): Process {
        val python = locatePython()
        val entry = locateEntryFile()
        val pb = ProcessBuilder()
            .directory(entry.parent.toFile())
            .command(python.toString(), entry.toString())
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        return pb.start()
    }

    suspend fun unzip(onProgress: (count: Int, current: Int) -> Unit) {
        val propertyFirst: String = System.getProperty("compose.application.resources.dir")
            ?: (System.getProperty("user.dir") + "\\resources\\windows-x64\\")
        val dstDir = Path.of(propertyFirst)
        val compressed = Path.of(propertyFirst).resolve("ocr-model.zip")

        if (!compressed.exists()) return // already unpacked

        ZipFile(compressed.toFile()).use { zipfile ->
            val entries = zipfile.entries().toList()
            entries
                .asFlow()
                .collectIndexed { index, entry ->
                    withContext(Dispatchers.IO) {
                        val newFile = newFile(dstDir.toFile(), entry)
                        if (entry.isDirectory) {
                            if (newFile.isDirectory) newFile.mkdirs()
                        } else {
                            val parent = newFile.parentFile
                            if (!parent.isDirectory) parent.mkdirs()

                            val inputStream = zipfile.getInputStream(entry)
                            val fos = FileOutputStream(newFile)
                            inputStream.transferTo(fos)
                            inputStream.close()
                            fos.close()
                        }
                    }
                    onProgress(entries.size, index + 1)
                }
        }
        compressed.deleteExisting()

    }

    suspend fun newFile(dstDir: File, entry: ZipEntry): File = withContext(Dispatchers.IO) {
        File(dstDir, entry.name)
    }
}
