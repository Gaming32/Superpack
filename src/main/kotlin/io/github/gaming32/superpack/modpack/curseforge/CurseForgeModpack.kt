package io.github.gaming32.superpack.modpack.curseforge

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.gaming32.superpack.modpack.*
import io.github.gaming32.superpack.util.splitOnce
import io.github.gaming32.superpack.util.toFile
import java.util.zip.ZipFile

class CurseForgeModpack(override val zipFile: ZipFile) : Modpack {
    val manifest: JsonObject by lazy {
        zipFile.getInputStream(zipFile.getEntry("manifest.json"))
            .reader()
            .use { JsonParser.parseReader(it) }
            .asJsonObject
    }

    override val type = ModpackType.CURSEFORGE

    override val name: String get() = manifest["name"].asString

    override val version: String get() = manifest["version"].asString

    override val description get() = null

    override val path = zipFile.name.toFile()

    override val versions by lazy {
        val root = manifest["minecraft"].asJsonObject
        val minecraft = root["version"].asString
        val loaders = root["modLoaders"].asJsonArray
            .asSequence()
            .map { it.asJsonObject["id"].asString }
            .associate { it.splitOnce('-') }
        ModpackVersions(minecraft, loaders["forge"], loaders["fabric"], loaders["quilt"])
    }

    override val allFiles by lazy { manifest["files"].asJsonArray.map { CurseForgeModpackFile(it.asJsonObject) } }

    override fun getOverrides(side: Side?): List<FileOverride> {
        if (side != null) {
            return listOf()
        }
        val root = "${manifest["overrides"].asString}/"
        return zipFile.entries()
            .asSequence()
            .filter { it.name.startsWith(root) }
            .map(::FileOverride)
            .toList()
    }

    override fun close() = zipFile.close()
}
