package io.github.gaming32.superpack.modpack.curseforge

import com.google.gson.JsonObject
import io.github.gaming32.superpack.getCacheFilePath
import io.github.gaming32.superpack.modpack.Compatibility
import io.github.gaming32.superpack.modpack.ModpackFile
import io.github.gaming32.superpack.modpack.Side
import io.github.gaming32.superpack.util.getValue
import io.github.gaming32.superpack.util.parseHexString
import io.github.matyrobbrt.curseforgeapi.schemas.HashAlgo
import io.github.matyrobbrt.curseforgeapi.schemas.file.File
import io.github.matyrobbrt.curseforgeapi.schemas.mod.Mod
import java.net.URL
import java.util.concurrent.CompletableFuture

class CurseForgeModpackFile(json: JsonObject) : ModpackFile {
    private val modId = json["projectID"].asInt
    private val fileId = json["fileID"].asInt
    private val required = json["required"].asBoolean

    private val modFuture = CompletableFuture<Mod>()
    private val mod by modFuture

    private val fileFuture = CompletableFuture<File>()
    private val file by fileFuture

    init {
        CF_SEMAPHORE.acquire()
        CF_API.asyncHelper.getMod(modId).queue { modResp ->
            CF_SEMAPHORE.release()
            try {
                val mod = modResp.orElseThrow {
                    IllegalStateException("Couldn't find CF mod $modId")
                }
                modFuture.complete(mod)
            } catch (e: Exception) {
                modFuture.completeExceptionally(e)
            }
            mod.latestFiles
                .firstOrNull { file -> file.id == fileId }
                ?.let(fileFuture::complete)
                ?: run {
                    CF_SEMAPHORE.acquire()
                    CF_API.asyncHelper.getModFile(modId, fileId).queue { fileResp ->
                        CF_SEMAPHORE.release()
                        try {
                            val file = fileResp.orElseThrow {
                                IllegalStateException("Couldn't find CF file ($modId, $fileId)")
                            }
                            fileFuture.complete(file)
                        } catch (e: Exception) {
                            fileFuture.completeExceptionally(e)
                        }
                    }
                }
        }
    }

    override val path by lazy { "${CLASS_ID_TO_DIR[mod.classId]}/${file.fileName}" }

    override val size get() = file.fileLength.toLong()

    override val cacheFile by lazy {
        getCacheFilePath(file.hashes.first { it.algo == HashAlgo.SHA1 }.value.parseHexString())
    }

    override val downloads by lazy { file.downloadUrl?.let { listOf(URL(it)) } ?: listOf() }

    override val hashes by lazy { file.hashes.associate { it.algo.name.lowercase() to it.value.parseHexString() } }

    override fun getCompatibility(side: Side) = if (required) Compatibility.REQUIRED else Compatibility.OPTIONAL

    // Thanks to ATLauncher for this
    val browserDownloadUrl get() = if (mod.links.websiteUrl != null) {
        "${mod.links.websiteUrl}/download/$fileId"
    } else {
        "https://www.curseforge.com/minecraft/${CLASS_ID_TO_SLUG[mod.classId]}/${mod.slug}/download/$fileId"
    }

    companion object {
        private val CLASS_ID_TO_DIR = mapOf(
            6 to "mods",
            12 to "resourcepacks",
            17 to "saves"
        )

        private val CLASS_ID_TO_SLUG = mapOf(
            6 to "mc-mods",
            12 to "texture-packs",
            17 to "worlds"
        )
    }
}
