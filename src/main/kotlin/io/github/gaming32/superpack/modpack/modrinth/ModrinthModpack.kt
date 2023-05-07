package io.github.gaming32.superpack.modpack.modrinth

import io.github.gaming32.mrpacklib.Mrpack
import io.github.gaming32.superpack.modpack.Modpack
import io.github.gaming32.superpack.modpack.Side
import io.github.gaming32.superpack.modpack.ZipEntryFileOverride
import java.io.File
import java.util.zip.ZipFile

class ModrinthModpack(private val zipFile: ZipFile) : Modpack {
    private val mrpack = Mrpack(zipFile)

    override fun close() = mrpack.close()

    override val isSided get() = true

    override val name: String get() = mrpack.packIndex.name

    override val version: String get() = mrpack.packIndex.versionId

    override val description: String? get() = mrpack.packIndex.summary

    override val path = File(zipFile.name)

    override val allFiles by lazy { mrpack.allFiles.map(::ModrinthModpackFile) }

    override fun getOverrides(side: Side?) =
        mrpack.getOverrides(side?.mrpack).map { ZipEntryFileOverride(zipFile, it) }

    override fun toString() = mrpack.toString()
}
