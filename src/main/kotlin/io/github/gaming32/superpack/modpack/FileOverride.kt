package io.github.gaming32.superpack.modpack

import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class FileOverride(private val entry: ZipEntry) {
    val path: String by entry::name

    val size by entry::size

    val isDirectory get() = entry.isDirectory

    fun openInputStream(zipFile: ZipFile): InputStream = zipFile.getInputStream(entry)
}
