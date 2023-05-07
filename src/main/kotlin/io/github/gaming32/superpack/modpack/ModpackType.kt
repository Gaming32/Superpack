package io.github.gaming32.superpack.modpack

import io.github.gaming32.superpack.modpack.modrinth.ModrinthModpack
import java.util.zip.ZipFile
import javax.swing.filechooser.FileNameExtensionFilter

enum class ModpackType(
    val isPack: ZipFile.() -> Boolean,
    val makePack: (ZipFile) -> Modpack,
    val fileFilter: FileNameExtensionFilter
) {
    MODRINTH(
        { getEntry("modrinth.index.json") != null }, ::ModrinthModpack,
        FileNameExtensionFilter("Modrinth Modpacks (*.mrpack)", "mrpack")
    ),
    CURSEFORGE(
        { getEntry("manifest.json") != null }, ::ModrinthModpack, // TODO: CurseForgeModpack
        FileNameExtensionFilter("CurseForge Modpacks (*.zip)", "zip")
    );

    companion object {
        val JOINED_FILE_FILTER = run {
            val extensions = ModpackType.values().flatMap { it.fileFilter.extensions.toList() }.toTypedArray()
            FileNameExtensionFilter(
                "Supported Modpacks (${extensions.joinToString(", ") { "*.$it" }})",
                *extensions
            )
        }
    }
}
