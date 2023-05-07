package io.github.gaming32.superpack.modpack

import java.io.InputStream

interface FileOverride {
    val path: String

    val size: Long

    val isDirectory: Boolean

    fun openInputStream(): InputStream
}
