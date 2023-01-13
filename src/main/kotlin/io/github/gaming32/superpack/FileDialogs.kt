package io.github.gaming32.superpack

import io.github.gaming32.superpack.util.selectedSaveFile
import java.awt.Component
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object FileDialogs {
    private val FILE_CHOOSER = JFileChooser()

    @JvmStatic
    fun mrpack(parent: Component?): File? {
        FILE_CHOOSER.fileSelectionMode = JFileChooser.FILES_ONLY
        FILE_CHOOSER.isMultiSelectionEnabled = false
        FILE_CHOOSER.dialogTitle = "Select modpack..."
        FILE_CHOOSER.isAcceptAllFileFilterUsed = false
        FILE_CHOOSER.resetChoosableFileFilters()
        FILE_CHOOSER.addChoosableFileFilter(FileNameExtensionFilter("Modrinth Modpacks (*.mrpack)", "mrpack"))
        return if (FILE_CHOOSER.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            null
        } else {
            FILE_CHOOSER.selectedFile
        }
    }

    @JvmStatic
    fun saveMrpack(parent: Component?): File? {
        FILE_CHOOSER.fileSelectionMode = JFileChooser.FILES_ONLY
        FILE_CHOOSER.isMultiSelectionEnabled = false
        FILE_CHOOSER.dialogTitle = null
        FILE_CHOOSER.isAcceptAllFileFilterUsed = false
        FILE_CHOOSER.resetChoosableFileFilters()
        FILE_CHOOSER.addChoosableFileFilter(FileNameExtensionFilter("Modrinth Modpacks (*.mrpack)", "mrpack"))
        return if (FILE_CHOOSER.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            null
        } else {
            FILE_CHOOSER.selectedSaveFile
        }
    }

    @JvmStatic
    fun outputDir(parent: Component?): File? {
        FILE_CHOOSER.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        FILE_CHOOSER.isMultiSelectionEnabled = false
        FILE_CHOOSER.dialogTitle = "Select destination directory..."
        FILE_CHOOSER.isAcceptAllFileFilterUsed = false
        FILE_CHOOSER.resetChoosableFileFilters()
        return if (FILE_CHOOSER.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            null
        } else {
            FILE_CHOOSER.selectedFile
        }
    }
}
