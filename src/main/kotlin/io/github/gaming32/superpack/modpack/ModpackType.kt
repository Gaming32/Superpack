package io.github.gaming32.superpack.modpack

import io.github.gaming32.superpack.modpack.curseforge.CurseForgeModpack
import io.github.gaming32.superpack.modpack.modrinth.ModrinthModpack
import java.util.zip.ZipFile
import javax.swing.filechooser.FileNameExtensionFilter

enum class ModpackType(
    val isPack: ZipFile.() -> Boolean,
    val makePack: (ZipFile) -> Modpack,
    val fileFilter: FileNameExtensionFilter,
    val supportsSides: Boolean,
    val secondaryHash: HashTypePair
) {
    MODRINTH(
        { getEntry("modrinth.index.json") != null }, ::ModrinthModpack,
        FileNameExtensionFilter("Modrinth Modpacks (*.mrpack)", "mrpack"),
        true, HashTypePair("sha512", "SHA-512")
    ),
    CURSEFORGE(
        { getEntry("manifest.json") != null }, ::CurseForgeModpack,
        FileNameExtensionFilter("CurseForge Modpacks (*.zip)", "zip"),
        false, HashTypePair("md5", "MD5")
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

    data class HashTypePair(val apiId: String, val algorithm: String)
}
