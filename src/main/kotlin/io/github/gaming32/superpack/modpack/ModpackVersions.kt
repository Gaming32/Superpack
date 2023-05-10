package io.github.gaming32.superpack.modpack

data class ModpackVersions(
    val minecraft: String,
    val forge: String? = null,
    val fabricLoader: String? = null,
    val quiltLoader: String? = null
)
