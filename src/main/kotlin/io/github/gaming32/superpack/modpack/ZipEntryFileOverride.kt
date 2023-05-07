package io.github.gaming32.superpack.modpack

import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipEntryFileOverride(private val zipFile: ZipFile, private val entry: ZipEntry) : FileOverride {
    override val path: String by entry::name

    override val size by entry::size

    override val isDirectory get() = entry.isDirectory

    override fun openInputStream(): InputStream = zipFile.getInputStream(entry)
}
