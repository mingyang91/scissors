package dev.famer.scissors

import dev.famer.scissors.models.Classification
import dev.famer.scissors.models.Span
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
        install(ContentNegotiation) {
            json()
        }
    }
    suspend fun ocr(file: Path): List<Span> {
        val res: List<Span> = client.get("http://localhost:9000/ocr") {
            parameter("path", file.toString())
        }.body()
        return res
    }

    suspend fun clf(file: Path): Classification {
        val res: Classification = client.get("http://localhost:9000/clf") {
            parameter("path", file.toString())
        }.body()
        return res
    }

    suspend fun health(): Boolean {
        val res: String = client.get("http://localhost:9000/health").body()
        return res.uppercase() == "OK"
    }

    private fun modelPath(): Path {
        return Path.of(System.getenv("LOCALAPPDATA"))
            .resolve("scissors")
            .resolve("ocr-model")
    }

    private fun locatePython(): Path {
        return modelPath().resolve("conda\\python.exe")
    }

    private fun locateEntryFile(): Path {
        return modelPath().resolve("main.py")
    }

    private fun condaEnv(): List<String>  {
        val condaPath = modelPath().resolve("conda")
        val paths = listOf(
            condaPath,
            condaPath.resolve("Library").resolve("mingw-w64").resolve("bin"),
            condaPath.resolve("Library").resolve("usr").resolve("bin"),
            condaPath.resolve("Library").resolve("bin"),
            condaPath.resolve("Scripts"),
        )
        return paths.map { it.toString() }
    }

    suspend fun startModelService(): Process? {
        val python = locatePython()
        val entry = locateEntryFile()
        val pb = ProcessBuilder()
            .directory(entry.parent.toFile())
            .command(python.toString(), entry.toString())

        // Re-constructor conda environment variable
        val env = pb.environment()
        env.set("PATH", (condaEnv() + env.get("PATH")).filterNotNull().joinToString(";"))

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = withContext(Dispatchers.IO) { pb.start() }
        for (attempt in 1..300) {
            try {
                if (!process.isAlive) return null
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
        withContext(Dispatchers.IO) { dstDir.createDirectories() }

        logger.info("before unpacked")
        withContext(Dispatchers.IO) { ZipFile(compressed.toFile()) }.use { zipfile ->
            val entries = zipfile.entries().toList()
            logger.info("Total files: ${entries.size}")
            entries
                .asFlow()
                .withIndex()
                .flatMapMerge { (index, entry) ->
                    flow {
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
                        emit(index)
                    }.flowOn(Dispatchers.IO)
                }
                .collect { onProgress(entries.size, it + 1) }
        }
        logger.info("Unpacked completed, delete zip file")
    }

    suspend fun newFile(dstDir: File, entry: ZipEntry): File = withContext(Dispatchers.IO) {
        File(dstDir, entry.name)
    }
}
