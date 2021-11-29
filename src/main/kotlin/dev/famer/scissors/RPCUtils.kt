package dev.famer.scissors

import dev.famer.scissors.models.Classification
import dev.famer.scissors.models.Span
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object RPCUtils {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        val res: String = client.get("http://localhost:9000/health")
        return res.uppercase() == "\"OK\""
    }

    private fun modelPath(): Path {
        return Path.of(System.getenv("LOCALAPPDATA"))
            .resolve("scissors")
            .resolve("ocr-model")
    }

    private fun locatePython(): Path {
        return modelPath().resolve("venv\\Scripts\\python.exe")
    }

    private fun locateEntryFile(): Path {
        return modelPath().resolve("main.py")
    }

    suspend fun startModelService(): Process? {
        val python = locatePython()
        val entry = locateEntryFile()
        val pb = ProcessBuilder()
            .directory(entry.parent.toFile())
            .command(python.toString(), entry.toString())
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = pb.start()
        for (attempt in 1..300) {
            try {
                if (health()) return process
            } catch (e: Throwable) {
                logger.warn("等待模型服务启动，第 $attempt 次。 ${e.message}")
            }
            delay(1000)
        }

        return null
    }

    suspend fun unzip(onProgress: (count: Int, current: Int) -> Unit) {
        val propertyFirst: String = System.getProperty("compose.application.resources.dir")
            ?: (System.getProperty("user.dir") + "\\resources\\windows-x64\\")

        val compressed = Path.of(propertyFirst).resolve("ocr-model.zip")

        val dstDir = modelPath()

        if (dstDir.exists()) { // already unpacked
            logger.info("already exists, skip")
            return
        }
        dstDir.createDirectories()

        logger.info("before unpacked")
        ZipFile(compressed.toFile()).use { zipfile ->
            val entries = zipfile.entries().toList()
            logger.info("Total files: ${entries.size}")
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
                    logger.info("Unpack file: ${entry.name}")
                    onProgress(entries.size, index + 1)
                }
        }
        logger.info("Unpacked completed, delete zip file")
    }

    suspend fun newFile(dstDir: File, entry: ZipEntry): File = withContext(Dispatchers.IO) {
        File(dstDir, entry.name)
    }
}
