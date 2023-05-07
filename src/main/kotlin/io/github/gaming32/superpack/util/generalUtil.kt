package io.github.gaming32.superpack.util

import com.google.gson.stream.JsonWriter
import com.sun.jna.Platform
import io.github.gaming32.mrpacklib.util.GsonHelper
import io.github.gaming32.superpack.APP_NAME
import io.github.gaming32.superpack.ICON_CACHE_DIR
import io.github.gaming32.superpack.util.SimpleHttp.request
import io.github.oshai.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.Util
import java.awt.*
import java.awt.image.BufferedImage
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

const val THUMBNAIL_SIZE = 64

private val SHA1 = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-1") }
private val SIZE_UNITS = listOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
private val SIZE_FORMAT = DecimalFormat("#,###.##")
private val HEX_CHARS = "0123456789abcdef".toCharArray()
private val GITHUB_MARKDOWN_URL = URL("https://api.github.com/markdown")
private val IMAGE_CACHE = SoftCacheMap<String, Image?>()

interface HasLogger {
    val logger: Logger
}

fun getLogger(): Logger = LoggerFactory.getLogger(Util.getCallingClass())

fun <T> showErrorMessage(owner: T, t: Throwable) where T : Component, T : HasLogger =
    showErrorMessage(owner, "Error", t)

fun <T> showErrorMessage(owner: T, logMessage: String, t: Throwable) where T : Component, T : HasLogger {
    owner.logger.error(logMessage, t)
    JOptionPane.showMessageDialog(owner, t.message, t.javaClass.name, JOptionPane.ERROR_MESSAGE)
}

fun <T> showErrorMessage(owner: T, message: String) where T : Component, T : HasLogger =
    showErrorMessage(owner, message, owner.getTitle())

fun <T> showErrorMessage(owner: T, message: String, title: String) where T : Component, T : HasLogger {
    owner.logger.error(message)
    JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE)
}

fun onlyShowErrorMessage(owner: Component, message: String) = onlyShowErrorMessage(owner, message, owner.getTitle())

fun onlyShowErrorMessage(owner: Component, message: String, title: String) =
    JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE)

@OptIn(ExperimentalPathApi::class)
@Throws(IOException::class)
fun getDirectorySize(dir: Path) = dir.walk()
    .filter(Path::isRegularFile)
    .map(Path::fileSize)
    .sum()

@OptIn(ExperimentalPathApi::class)
@Throws(IOException::class)
fun rmdir(dir: Path) {
    if (!dir.isDirectory()) return  // Doesn't exist, so we have nothing to do
    dir.visitFileTree {
        onVisitFile { file, _ ->
            file.deleteExisting()
            FileVisitResult.CONTINUE
        }
        onPostVisitDirectory { directory, _ ->
            directory.deleteExisting()
            FileVisitResult.CONTINUE
        }
    }
}

fun getHumanFileSize(size: Long): String {
    var divisor = 0
    while (size ushr divisor > 2048L && divisor / 10 < SIZE_UNITS.size) {
        divisor += 10
    }
    val unit = SIZE_UNITS[divisor / 10]
    return SIZE_FORMAT.format(size.toDouble() / (1L shl divisor)) + " " + unit
}

fun getHumanFileSizeExtended(size: Long) = String.format("%s (%,d bytes)", getHumanFileSize(size), size)

fun Long.toIntClamped() = when {
    this > Int.MAX_VALUE -> Int.MAX_VALUE
    this < Int.MIN_VALUE -> Int.MIN_VALUE
    else -> toInt()
}

fun JTextField.addDocumentListener(listener: DocumentEvent.() -> Unit) =
    document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = e.listener()
        override fun removeUpdate(e: DocumentEvent) = e.listener()
        override fun changedUpdate(e: DocumentEvent) = e.listener()
    })

@Throws(IOException::class)
fun renderMarkdown(markdown: String): String {
    val cnxn = GITHUB_MARKDOWN_URL.request()
    cnxn.doOutput = true
    cnxn.doInput = true
    OutputStreamWriter(cnxn.getOutputStream(), Charsets.UTF_8).use { writer ->
        JsonWriter(writer).use { jsonWriter ->
            jsonWriter.beginObject()
            jsonWriter.name("text")
            jsonWriter.value(markdown)
            jsonWriter.endObject()
        }
    }
    try {
        return cnxn.getInputStream().use { it.reader(Charsets.UTF_8).readText() }
    } catch (e: IOException) {
        throw IOException((cnxn as HttpURLConnection).errorStream.use { it.reader(Charsets.UTF_8).readText() }, e)
    }
}

fun String.capitalize() =
    if (isEmpty()) this else this[0].uppercaseChar().toString() + substring(1).lowercase()

fun ByteArray.toHexString(): String {
    val result = StringBuilder(size shl 1)
    for (v in this) {
        result.append(HEX_CHARS[v.toInt() and 0xff shr 4])
        result.append(HEX_CHARS[v.toInt() and 0xf])
    }
    return result.toString()
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.parseHexString(): ByteArray = GsonHelper.fromHexString(this)

fun <T : Any> Iterable<T>.findFirst() = iterator().findFirst()

fun <T : Any> Iterator<T>.findFirst() = Optional.ofNullable(if (hasNext()) next() else null)

fun AbstractButton.callAction() {
    val model = model
    val armed = model.isArmed
    val pressed = model.isPressed
    if (!armed) model.isArmed = true
    model.isPressed = !pressed
    model.isPressed = pressed
    if (!armed) model.isArmed = false
}

fun Component.getTitle() =
    (SwingUtilities.getAncestorOfClass(Frame::class.java, this) as? Frame)?.title ?: APP_NAME

@Throws(IOException::class)
fun readAndDiscard(inp: InputStream) {
    val buf = ByteArray(8192)
    while (inp.read(buf) != -1) {
        // Intentionally empty
    }
}

fun getSha1(): MessageDigest {
    val digest = SHA1.get()
    digest.reset()
    return digest
}

fun String.sha1() = toByteArray(Charsets.UTF_8).sha1()

fun ByteArray.sha1(): ByteArray = getSha1().digest(this)

/**
 * Computes the SHA-1 hash of the specified input stream, closing it afterward.
 */
@Throws(IOException::class)
fun InputStream.sha1(): ByteArray {
    val digest = getSha1()
    DigestInputStream(this, digest).use { is2 ->
        readAndDiscard(is2)
        return digest.digest()
    }
}

fun getIconCacheKey(iconUrl: String): String {
    val lastSlash = iconUrl.lastIndexOf('/')
    val lastPathPart = iconUrl.substring(lastSlash + 1)
    return if (lastPathPart.startsWith("icon.")) {
        // This is the project ID. This is ok to use, because if the project ever has its icon updated, the hash will then be used.
        iconUrl.substring(iconUrl.lastIndexOf('/', lastSlash - 1) + 1, lastSlash) +
            iconUrl.substring(iconUrl.lastIndexOf('.'))
    } else {
        // Use the icon hash
        lastPathPart
    }
}

fun loadProjectIcon(iconUrl: URL, completionHandler: (Image?) -> Unit) {
    loadProjectIcon(iconUrl).thenAccept { SwingUtilities.invokeLater { completionHandler(it) } }
}

fun loadProjectIcon(iconUrl: URL): CompletableFuture<Image?> =
    // Swing has its own method of downloading and caching images like this, however that lacks
    // parallelism and blocks while it loads the images.
    IMAGE_CACHE.getFuture(iconUrl.toExternalForm()) { strUrl ->
        val cacheKey = getIconCacheKey(strUrl)
        val iconCache = ICON_CACHE_DIR / cacheKey
        var image: Image?
        try {
            if (iconCache.exists()) {
                return@getFuture ImageIO.read(iconCache)
            }
            image = ImageIO.read(iconUrl)
            if (image == null) return@getFuture null
            image = image.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH)
        } catch (e: Exception) {
            logger.error("Error loading icon $strUrl", e)
            return@getFuture null
        }
        try {
            iconCache.parentFile.mkdirs()
            val bimage = image.toBufferedImage(BufferedImage.TYPE_INT_ARGB)
            ImageIO.write(
                bimage.toBufferedImage(approximateImageType(bimage)),
                cacheKey.substring(cacheKey.lastIndexOf('.') + 1),
                iconCache
            )
        } catch (e: Exception) {
            logger.error("Error caching icon $cacheKey", e)
        }
        image
    }

fun approximateImageType(image: BufferedImage): Int {
    if (image.type != BufferedImage.TYPE_INT_ARGB && image.type != BufferedImage.TYPE_INT_RGB) {
        return image.type
    }
    val data = image.getTile(0, 0) // This is the entire image data
    val pixel = IntArray(data.numBands)
    var type = BufferedImage.TYPE_BYTE_GRAY
    var x = 0
    val mx = data.width
    while (x < mx) {
        var y = 0
        val my = data.height
        while (y < my) {
            data.getPixel(x, y, pixel)
            if (type == BufferedImage.TYPE_BYTE_GRAY && (pixel[0] != pixel[1] || pixel[1] != pixel[2])) {
                type = BufferedImage.TYPE_INT_RGB
            }
            if (pixel.size > 3 && pixel[3] != 255) {
                return BufferedImage.TYPE_INT_ARGB
            }
            y++
        }
        x++
    }
    return type
}

fun Image.toBufferedImage(type: Int): BufferedImage {
    val result = BufferedImage(getWidth(null), getHeight(null), type)
    val g = result.graphics
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return result
}

/**
 * @implNote This is implemented with [FNV-1a](https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function)
 */
fun ByteArray.longHash(): Long {
    var hash = -0x340d631b7bdddcdbL
    for (b in this) {
        hash = (hash xor b.toLong()) * 0x100000001b3L
    }
    return if (hash != 0L) hash else -1L
}

fun <T> browseFileDirectory(parent: T, file: File) where T : Component, T : HasLogger {
    try {
        if (Platform.isWindows()) {
            // AWT's browseFileDirectory doesn't work on Windows, so we need to use the win32 API ourselves
            // Run in a thread since Explorer blocks if it needs to open a new window
            thread { WindowsUtil.browseFileDirectory(file) }
            return
        }
        Desktop.getDesktop().browseFileDirectory(file)
    } catch (e: UnsupportedOperationException) {
        onlyShowErrorMessage(parent, "This option is unsupported on your platform")
    } catch (e: Exception) {
        showErrorMessage(parent, "Failed to browse file", e)
    }
}

fun appendExtension(file: File, filter: FileFilter): File {
    if (filter !is FileNameExtensionFilter) return file
    val extensions = filter.extensions
    if (extensions.isEmpty()) return file
    return if (file.path.indexOf('.') != -1) file else File(file.path + '.' + extensions[0])
}

val JFileChooser.selectedSaveFile get() = appendExtension(selectedFile, fileFilter)

@Suppress("NOTHING_TO_INLINE")
inline operator fun File.div(other: String) = File(this, other)

operator fun <V> Future<V>.getValue(thisRef: Any?, property: KProperty<*>): V = get()

inline fun <T, A, B> build(value: T, a: T.() -> A, b: A.() -> B) = b(a(value))

inline fun <T, A, B, C> build(value: T, a: T.() -> A, b: A.() -> B, c: B.() -> C) = c(b(a(value)))
