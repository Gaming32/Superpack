package io.github.gaming32.superpack

import com.sun.jna.Platform
import io.github.gaming32.superpack.modpack.ModpackType
import io.github.gaming32.superpack.util.selectedSaveFile
import java.awt.Component
import java.io.File
import javax.swing.JFileChooser

object FileDialogs {
    private val FILE_CHOOSER = JFileChooser()
    private val OUTPUT_FILE_CHOOSER = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
        dialogTitle = "Select destination directory..."
        isAcceptAllFileFilterUsed = false
        resetChoosableFileFilters()
        when { // Use the directory containing .minecraft so that people can select it
            Platform.isWindows() -> File(System.getenv("APPDATA"))
            Platform.isMac() -> File(System.getProperty("user.home"), "Library/Application Support")
            else -> File(System.getProperty("user.home"))
        }.takeIf { it.isDirectory }?.let { currentDirectory = it }
    }

    @JvmStatic
    fun modpack(parent: Component?): File? = FILE_CHOOSER.run {
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        dialogTitle = "Select modpack..."
        isAcceptAllFileFilterUsed = false
        resetChoosableFileFilters()
        addChoosableFileFilter(ModpackType.JOINED_FILE_FILTER)
        ModpackType.values().forEach { addChoosableFileFilter(it.fileFilter) }
        if (showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            null
        } else {
            selectedFile
        }
    }

    @JvmStatic
    fun saveModpack(parent: Component?, type: ModpackType) = FILE_CHOOSER.run {
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        dialogTitle = null
        isAcceptAllFileFilterUsed = false
        resetChoosableFileFilters()
        addChoosableFileFilter(type.fileFilter)
        if (showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            null
        } else {
            selectedSaveFile
        }
    }

    @JvmStatic
    fun outputDir(parent: Component?) = OUTPUT_FILE_CHOOSER.run {
        if (showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            null
        } else {
            selectedFile
        }
    }
}
