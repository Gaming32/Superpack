package io.github.gaming32.superpack.modpack.modrinth

import io.github.gaming32.mrpacklib.packindex.PackFile
import io.github.gaming32.superpack.getCacheFilePath
import io.github.gaming32.superpack.modpack.ModpackFile
import io.github.gaming32.superpack.modpack.Side
import java.net.URL

class ModrinthModpackFile(private val file: PackFile) : ModpackFile {
    override val path: String by file::path

    override val size by file::fileSize

    override val cacheFile by lazy { getCacheFilePath(file.hashes.getValue("sha1")) }

    override val downloads: List<URL> by file::downloads

    override val hashes: Map<String, ByteArray> by file::hashes

    override fun getCompatibility(side: Side) = file.env[side.mrpack].superpack

    override fun toString() = file.toString()
}
