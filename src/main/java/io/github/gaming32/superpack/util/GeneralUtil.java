package io.github.gaming32.superpack.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.LongStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;
import com.sun.jna.Platform;

import io.github.gaming32.superpack.Superpack;

public final class GeneralUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralUtil.class);

    public static final int THUMBNAIL_SIZE = 64;

    private static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    });
    private static final String[] SIZE_UNITS = {
        "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"
    };
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,###.##");
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final URL GITHUB_MARKDOWN_URL;
    private static final SoftCacheMap<String, Image> IMAGE_CACHE = new SoftCacheMap<>();

    static {
        try {
            GITHUB_MARKDOWN_URL = new URL("https://api.github.com/markdown");
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GeneralUtil() {
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, Throwable t) {
        showErrorMessage(owner, "Error", t);
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String logMessage, Throwable t) {
        owner.getLogger().error(logMessage, t);
        JOptionPane.showMessageDialog(owner, t.getMessage(), t.getClass().getName(), JOptionPane.ERROR_MESSAGE);
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String message) {
        showErrorMessage(owner, message, getTitle(owner));
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String message, String title) {
        owner.getLogger().error(message);
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void onlyShowErrorMessage(Component owner, String message) {
        onlyShowErrorMessage(owner, message, getTitle(owner));
    }

    public static void onlyShowErrorMessage(Component owner, String message, String title) {
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static long getDirectorySize(Path dir) throws IOException {
        try (LongStream stream = Files.walk(dir).filter(Files::isRegularFile).mapToLong(value -> {
            try {
                return Files.size(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })) {
            return stream.sum();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static void rmdir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return; // Doesn't exist, so we have nothing to do
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String getHumanFileSize(long size) {
        int divisor = 0;
        while (size >>> divisor > 2048L && divisor / 10 < SIZE_UNITS.length) {
            divisor += 10;
        }
        final String unit = SIZE_UNITS[divisor / 10];
        return SIZE_FORMAT.format((double)size / (1L << divisor)) + " " + unit;
    }

    public static String getHumanFileSizeExtended(long size) {
        return getHumanFileSize(size) + " (" + size + " bytes)";
    }

    public static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int)value;
    }

    public static void addDocumentListener(JTextField textField, Consumer<DocumentEvent> listener) {
        // I wish I started this project in Kotlin :/
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.accept(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.accept(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.accept(e);
            }
        });
    }

    public static String renderMarkdown(String markdown) throws IOException {
        final URLConnection cnxn = SimpleHttp.request(GITHUB_MARKDOWN_URL);
        cnxn.setDoOutput(true);
        cnxn.setDoInput(true);
        try (
            Writer writer = new OutputStreamWriter(cnxn.getOutputStream(), StandardCharsets.UTF_8);
            // Writer writer = new FileWriter("debug.json");
            JsonWriter jsonWriter = new JsonWriter(writer);
        ) {
            jsonWriter.beginObject();
            jsonWriter.name("text");
            jsonWriter.value(markdown);
            jsonWriter.endObject();
        }
        final StringBuilder result = new StringBuilder();
        final byte[] buf = new byte[8192];
        int n;
        try (InputStream is = cnxn.getInputStream()) {
            while ((n = is.read(buf)) != -1) {
                result.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            try (InputStream is = ((HttpURLConnection)cnxn).getErrorStream()) {
                while ((n = is.read(buf)) != -1) {
                    result.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                }
            } catch (Exception e2) {
                throw e;
            }
            throw new IOException(result.toString(), e);
        }
        return result.toString();
    }

    public static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static String toHexString(byte[] arr) {
        StringBuilder result = new StringBuilder(arr.length << 1);
        for (byte v : arr) {
            result.append(HEX_CHARS[(v & 0xff) >> 4]);
            result.append(HEX_CHARS[v & 0xf]);
        }
        return result.toString();
    }

    public static <T> Optional<T> findFirst(Iterable<T> iterable) {
        return findFirst(iterable.iterator());
    }

    public static <T> Optional<T> findFirst(Iterator<T> iterator) {
        return Optional.ofNullable(iterator.hasNext() ? iterator.next() : null);
    }

    public static void callAction(AbstractButton button) {
        final ButtonModel model = button.getModel();
        final boolean armed = model.isArmed();
        final boolean pressed = model.isPressed();
        if (!armed) model.setArmed(true);
        model.setPressed(!pressed);
        model.setPressed(pressed);
        if (!armed) model.setArmed(false);
    }

    public static String getTitle(Component comp) {
        final Frame ownerFrame = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, comp);
        return ownerFrame != null ? ownerFrame.getTitle() : Superpack.APP_NAME;
    }

    public static void readAndDiscard(InputStream is) throws IOException {
        byte[] buf = new byte[8192];
        while (is.read(buf) != -1);
    }

    public static MessageDigest getSha1() {
        final MessageDigest digest = SHA1.get();
        digest.reset();
        return digest;
    }

    public static byte[] sha1(String input) {
        return sha1(input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha1(byte[] input) {
        return getSha1().digest(input);
    }

    /**
     * Computes the SHA-1 hash of the specified input stream, closing it afterwards.
     */
    public static byte[] sha1(InputStream is) throws IOException {
        final MessageDigest digest = getSha1();
        try (InputStream is2 = new DigestInputStream(is, digest)) {
            readAndDiscard(is2);
            return digest.digest();
        }
    }

    public static String getIconCacheKey(String iconUrl) {
        final int lastSlash = iconUrl.lastIndexOf('/');
        final String lastPathPart = iconUrl.substring(lastSlash + 1);
        if (lastPathPart.startsWith("icon.")) {
            // This is the project ID. This is ok to use, because if the project ever has its icon updated, the hash will then be used.
            return
                iconUrl.substring(iconUrl.lastIndexOf('/', lastSlash - 1) + 1, lastSlash) +
                iconUrl.substring(iconUrl.lastIndexOf('.'));
        }
        return lastPathPart; // Use the icon hash
    }

    public static void loadProjectIcon(URL iconUrl, Consumer<Image> completionHandler) {
        loadProjectIcon(iconUrl).thenAccept(image -> SwingUtilities.invokeLater(() -> completionHandler.accept(image)));
    }

    public static CompletableFuture<Image> loadProjectIcon(URL iconUrl) {
        // Swing has its own method of downloading and caching images like this, however that lacks
        // paralellism and blocks while it loads the images.
        return IMAGE_CACHE.getFuture(iconUrl.toExternalForm(), strUrl -> {
            final String cacheKey = getIconCacheKey(strUrl);
            final File iconCache = new File(Superpack.ICON_CACHE_DIR, cacheKey);
            Image image;
            try {
                if (iconCache.exists()) {
                    return ImageIO.read(iconCache);
                }
                image = ImageIO.read(iconUrl);
                if (image == null) return null;
                image = image.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
            } catch (Exception e) {
                LOGGER.error("Error loading icon " + strUrl, e);
                return null;
            }
            try {
                iconCache.getParentFile().mkdirs();
                final BufferedImage bimage = toBufferedImage(image, BufferedImage.TYPE_INT_ARGB);
                ImageIO.write(
                    toBufferedImage(bimage, approximateImageType(bimage)),
                    cacheKey.substring(cacheKey.lastIndexOf('.') + 1),
                    iconCache
                );
            } catch (Exception e) {
                LOGGER.error("Error caching icon " + cacheKey, e);
            }
            return image;
        });
    }

    public static int approximateImageType(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB && image.getType() != BufferedImage.TYPE_INT_RGB) {
            return image.getType();
        }
        final Raster data = image.getTile(0, 0); // This is the entire image data
        final int[] pixel = new int[data.getNumBands()];
        int type = BufferedImage.TYPE_BYTE_GRAY;
        for (int x = 0, mx = data.getWidth(); x < mx; x++) {
            for (int y = 0, my = data.getHeight(); y < my; y++) {
                data.getPixel(x, y, pixel);
                if (
                    type == BufferedImage.TYPE_BYTE_GRAY &&
                    (pixel[0] != pixel[1] ||
                     pixel[1] != pixel[2] ||
                     pixel[0] != pixel[2])
                ) {
                    type = BufferedImage.TYPE_INT_RGB;
                }
                if (pixel.length > 3 && pixel[3] != 255) {
                    return BufferedImage.TYPE_INT_ARGB;
                }
            }
        }
        return type;
    }

    public static BufferedImage toBufferedImage(Image image, int type) {
        final BufferedImage result = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        final Graphics g = result.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * @implNote This is implemented with <a href="https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function">FNV-1a</a>
     */
    public static long longHash(byte[] input) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : input) {
            hash = (hash ^ b) * 0x100000001b3L;
        }
        return hash != 0L ? hash : -1L;
    }

    public static <T extends Component & HasLogger> void browseFileDirectory(T parent, File file) {
        try {
            if (Platform.isWindows()) {
                // AWT's browseFileDirectory doesn't work on Windows, so we need to use the win32 API ourselves
                // Run in a thread since Explorer blocks if it needs to open a new window
                new Thread(() -> WindowsUtil.browseFileDirectory(file), "Windows-BrowseFileDirectory").start();
                return;
            }
            Desktop.getDesktop().browseFileDirectory(file);
        } catch (UnsupportedOperationException e) {
            GeneralUtil.onlyShowErrorMessage(parent, "This option is unsupported on your platform");
        } catch (Exception e) {
            GeneralUtil.showErrorMessage(parent, "Failed to browse file", e);
        }
    }

    public static File appendExtension(File file, FileFilter filter) {
        if (!(filter instanceof FileNameExtensionFilter)) return file;
        final String[] extensions = ((FileNameExtensionFilter)filter).getExtensions();
        if (extensions.length == 0) return file;
        if (file.getPath().indexOf('.') != -1) return file;
        return new File(file.getPath() + '.' + extensions[0]);
    }

    public static File getSelectedSaveFile(JFileChooser fileChooser) {
        return appendExtension(fileChooser.getSelectedFile(), fileChooser.getFileFilter());
    }
}
