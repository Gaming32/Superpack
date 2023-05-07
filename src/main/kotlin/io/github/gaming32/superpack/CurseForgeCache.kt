package io.github.gaming32.superpack

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.github.gaming32.superpack.util.div
import io.github.gaming32.superpack.util.parseHexString
import io.github.gaming32.superpack.util.toHexString
import io.github.oshai.KotlinLogging
import java.io.IOException

private val logger = KotlinLogging.logger {}

object CurseForgeCache {
    private val path = CACHE_DIR / "curseforge.json"
    private val map = mutableMapOf<Int, MutableMap<Int, ByteArray>>().apply {
        try {
            path.reader().let(::JsonReader).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    reader.beginObject()
                    this[reader.nextName().toInt()] = mutableMapOf<Int, ByteArray>().apply {
                        while (reader.hasNext()) {
                            this[reader.nextName().toInt()] = reader.nextString().parseHexString()
                        }
                    }
                    reader.endObject()
                }
                reader.endObject()
            }
        } catch (e: IOException) {
            logger.info("Couldn't read CurseForge cache", e)
        } catch (e: Exception) {
            logger.error("Failed to read CurseForge cache", e)
        }
    }

    operator fun get(projectId: Int, fileId: Int) = map[projectId]?.get(fileId)

    operator fun set(projectId: Int, fileId: Int, hash: ByteArray) {
        map.getOrPut(projectId, ::mutableMapOf)[fileId] = hash
        save()
    }

    private fun save() = try {
        path.parentFile.mkdirs()
        path.writer().let(::JsonWriter).use { writer ->
            writer.beginObject()
            map.forEach { (projectId, files) ->
                writer.name(projectId.toString()).beginObject()
                files.forEach { (fileId, hash) ->
                    writer.name(fileId.toString()).value(hash.toHexString())
                }
                writer.endObject()
            }
            writer.endObject()
        }
        Unit
    } catch (e: Exception) {
        logger.error("Failed to save CurseForge cache", e)
    }
}
