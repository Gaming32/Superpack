package io.github.gaming32.superpack.modpack

import io.github.gaming32.superpack.modpack.modrinth.ModrinthModpack
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

interface Modpack : AutoCloseable {
    /**
     * Contract: if [isSided] is `true`, then all [Side] arguments are ignored.
     */
    val isSided: Boolean

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
        fun open(zipFile: ZipFile) = when {
            zipFile.getEntry("modrinth.index.json") != null -> ModrinthModpack(zipFile)
//            zipFile.getEntry("manifest.json") != null -> CurseForgeModpack(zipFile)
            else -> throw IllegalArgumentException("Couldn't detect modpack type of ${zipFile.name}")
        }
    }
}
