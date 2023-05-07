package io.github.gaming32.superpack.modpack

import java.io.File
import java.net.URL

interface ModpackFile {
    val path: String

    val size: Long

    val cacheFile: File

    val downloads: List<URL>

    val hashes: Map<String, ByteArray>

    fun getCompatibility(side: Side): Compatibility
}
