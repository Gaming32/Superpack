package io.github.gaming32.superpack.modpack

import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

interface Modpack : AutoCloseable {
    val type: ModpackType

    val name: String

    val version: String

    val description: String?

    val path: File

    @get:Throws(IOException::class)
    val allFiles: List<ModpackFile>

    @Throws(IOException::class)
    fun getAllFiles(side: Side) = allFiles.filter { it.getCompatibility(side) != Compatibility.UNSUPPORTED }

    @Throws(IOException::class)
    fun getAllFiles(side: Side, compatibility: Compatibility) =
        allFiles.filter { it.getCompatibility(side) == compatibility }

    fun getOverrides(side: Side? = null): List<FileOverride>

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun open(zipFile: ZipFile) = ModpackType.values()
            .firstOrNull { it.isPack(zipFile) }
            ?.makePack
            ?.invoke(zipFile)
            ?: throw IllegalArgumentException("Couldn't detect modpack type of ${zipFile.name}")
    }
}
