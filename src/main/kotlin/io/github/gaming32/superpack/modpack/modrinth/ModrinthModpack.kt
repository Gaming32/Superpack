package io.github.gaming32.superpack.modpack.modrinth

import io.github.gaming32.mrpacklib.Mrpack
import io.github.gaming32.superpack.modpack.FileOverride
import io.github.gaming32.superpack.modpack.Modpack
import io.github.gaming32.superpack.modpack.ModpackType
import io.github.gaming32.superpack.modpack.Side
import io.github.gaming32.superpack.util.toFile
import java.util.zip.ZipFile

class ModrinthModpack(override val zipFile: ZipFile) : Modpack {
    private val mrpack = Mrpack(zipFile)

    override val type = ModpackType.MODRINTH

    override val name: String get() = mrpack.packIndex.name

    override val version: String get() = mrpack.packIndex.versionId

    override val description: String? get() = mrpack.packIndex.summary

    override val path = zipFile.name.toFile()

    override val versions by lazy { mrpack.packIndex.dependencies.toModpackVersions() }

    override val allFiles by lazy { mrpack.allFiles.map(::ModrinthModpackFile) }

    override fun getOverrides(side: Side?) = mrpack.getOverrides(side?.mrpack).map(::FileOverride)

    override fun close() = mrpack.close()

    override fun toString() = mrpack.toString()
}
