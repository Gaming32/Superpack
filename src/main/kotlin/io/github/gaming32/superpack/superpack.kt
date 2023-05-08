package io.github.gaming32.superpack

import com.jthemedetecor.OsThemeDetector
import com.sun.jna.Platform
import io.github.gaming32.superpack.themes.Theme
import io.github.gaming32.superpack.themes.Themes
import io.github.gaming32.superpack.util.div
import io.github.gaming32.superpack.util.showErrorMessage
import io.github.gaming32.superpack.util.toFile
import io.github.gaming32.superpack.util.toHexString
import io.github.oshai.KotlinLogging
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.swing.SwingUtilities

private val logger = KotlinLogging.logger {}

const val APP_NAME = "Superpack"
const val MODRINTH_API_ROOT = "https://api.modrinth.com/v2/"

@JvmField
val DATA_DIR = getDataDir()

@JvmField
val SETTINGS_FILE = DATA_DIR / "settings.json"

@JvmField
val MYPACKS_FILE = DATA_DIR / "mypacks.json"

@JvmField
val CACHE_DIR = DATA_DIR / "cache"

@JvmField
val DOWNLOAD_CACHE_DIR = CACHE_DIR / "downloadCache"

@JvmField
val ICON_CACHE_DIR = CACHE_DIR / "iconCache"

fun main(args: Array<String>) {
    logger.debug("If you see this, you're in debug mode :)")
    try {
        FileReader(SETTINGS_FILE, Charsets.UTF_8).use { SuperpackSettings.INSTANCE.copyFromRead(it) }
    } catch (e: Exception) {
        logger.warn("Failed to load settings, using defaults", e)
        SuperpackSettings.INSTANCE.copyFrom(SuperpackSettings())
    }
    if (SuperpackSettings.INSTANCE.theme == null) {
        logger.warn("Configured theme was unknown. Falling back to {}.", Themes.DEFAULT.id)
        SuperpackSettings.INSTANCE.theme = Themes.DEFAULT
    }
    saveSettings()
    try {
        FileReader(MYPACKS_FILE, Charsets.UTF_8).use { MyPacks.INSTANCE.copyFromRead(it) }
    } catch (e: Exception) {
        logger.warn("Failed to load My Packs, using defaults", e)
        MyPacks.INSTANCE.copyFrom(MyPacks())
    }
    MyPacks.INSTANCE.removeMissing()
    saveMyPacks()
    setTheme(SuperpackSettings.INSTANCE.theme)
    SwingUtilities.invokeLater {
        val mainFrame = SuperpackMainFrame(OsThemeDetector.getDetector())
        mainFrame.isVisible = true
        if (args.isNotEmpty()) {
            try {
                mainFrame.openInstallPack(args[0].toFile())
            } catch (e: Exception) {
                showErrorMessage(mainFrame, "Failed to open file automatically", e)
            }
        }
    }
}

private fun getDataDir() = if (Platform.isWindows()) {
    File(System.getenv("APPDATA"), ".superpack")
} else {
    val home = System.getProperty("user.dir")
    if (Platform.isMac()) {
        File(home, "Library/Application Support/superpack")
    } else {
        File(home, ".superpack")
    }
}

fun saveSettings() = try {
    FileWriter(SETTINGS_FILE, Charsets.UTF_8).use { SuperpackSettings.INSTANCE.write(it) }
} catch (e: Exception) {
    logger.error("Failed to save settings", e)
}

fun saveMyPacks() = try {
    FileWriter(MYPACKS_FILE, Charsets.UTF_8).use { MyPacks.INSTANCE.write(it) }
} catch (e: Exception) {
    logger.error("Failed to save My Packs", e)
}

fun getCacheFilePath(sha1: ByteArray): File {
    val hash = sha1.toHexString()
    val path = hash.substring(0, 2) + '/' +
        hash.substring(2, 4) + '/' +
        hash.substring(4)
    return DOWNLOAD_CACHE_DIR / path
}

fun getCacheFile(sha1: ByteArray, fileSize: Long): File? {
    val file = getCacheFilePath(sha1)
    return if (file.exists() && file.length() == fileSize) file else null
}

fun isThemeDark(): Boolean {
    val theme = SuperpackSettings.INSTANCE.theme
    return if (theme.isAffectedBySystem) OsThemeDetector.getDetector().isDark else theme.isDark
}

fun setTheme(theme: Theme) {
    logger.info("Setting theme: {}", theme.id)
    SuperpackSettings.INSTANCE.theme = theme
    saveSettings()
    theme.apply(isThemeDark())
}
