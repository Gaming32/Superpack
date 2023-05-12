package io.github.gaming32.superpack

import com.sun.jna.Platform
import io.github.gaming32.superpack.modpack.ModpackType
import io.github.gaming32.superpack.util.getDownloadsFolder
import io.github.gaming32.superpack.util.toFile
import jnafilechooser.api.JnaFileChooser
import java.awt.Window
import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter

object FileDialogs {
    private val FILE_CHOOSER = JnaFileChooser(getDownloadsFolder())
    private val OUTPUT_FILE_CHOOSER = JnaFileChooser(
        when { // Use the directory containing .minecraft so that people can select it
            Platform.isWindows() -> System.getenv("APPDATA").toFile()
            Platform.isMac() -> File(System.getProperty("user.home"), "Library/Application Support")
            else -> System.getProperty("user.home").toFile()
        }.takeIf { it.isDirectory }
    ).apply {
        mode = JnaFileChooser.Mode.Directories
        isMultiSelectionEnabled = false
        title = "Select destination directory..."
    }

    @JvmStatic
    fun modpack(parent: Window?): File? = FILE_CHOOSER.run {
        mode = JnaFileChooser.Mode.Files
        isMultiSelectionEnabled = false
        title = "Select modpack..."
        addFilter(ModpackType.JOINED_FILE_FILTER)
        ModpackType.values().forEach { addFilter(it.fileFilter) }
        if (showOpenDialog(parent)) {
            null
        } else {
            selectedFile
        }
    }

    @JvmStatic
    fun saveModpack(parent: Window?, type: ModpackType) = FILE_CHOOSER.run {
        mode = JnaFileChooser.Mode.Files
        isMultiSelectionEnabled = false
        title = null
        addFilter(type.fileFilter)
        if (showSaveDialog(parent)) {
            null
        } else {
            selectedFile
        }
    }

    @JvmStatic
    fun outputDir(parent: Window?) = OUTPUT_FILE_CHOOSER.run {
        if (showOpenDialog(parent)) {
            null
        } else {
            selectedFile
        }
    }

    private var JnaFileChooser.title: String?
        get() = throw UnsupportedOperationException()
        set(value) = setTitle(value ?: "")

    private fun JnaFileChooser.addFilter(filter: FileNameExtensionFilter) =
        addFilter(filter.description, *filter.extensions)
}
