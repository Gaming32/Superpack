package io.github.gaming32.superpack.modpack.curseforge

import com.google.gson.JsonParser
import io.github.gaming32.superpack.modpack.*
import io.github.gaming32.superpack.util.toFile
import java.util.zip.ZipFile

class CurseForgeModpack(private val zipFile: ZipFile) : Modpack {
    private val index by lazy {
        zipFile.getInputStream(zipFile.getEntry("manifest.json"))
            .reader()
            .use { JsonParser.parseReader(it) }
            .asJsonObject
    }

    override val type = ModpackType.CURSEFORGE

    override val name: String get() = index["name"].asString

    override val version: String get() = index["version"].asString

    override val description get() = null

    override val path = zipFile.name.toFile()

    override val allFiles by lazy { index["files"].asJsonArray.map { CurseForgeModpackFile(it.asJsonObject) } }

    override fun getOverrides(side: Side?): List<FileOverride> {
        if (side != null) {
            return listOf()
        }
        val root = "${index["overrides"].asJsonPrimitive.asString}/"
        return zipFile.entries()
            .asSequence()
            .filter { it.name.startsWith(root) }
            .map { ZipEntryFileOverride(zipFile, it) }
            .toList()
    }

    override fun close() = zipFile.close()
}
