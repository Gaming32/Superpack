package io.github.gaming32.superpack.util;

import java.awt.Component;
import java.awt.Frame;
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
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.LongStream;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.gson.stream.JsonWriter;

import io.github.gaming32.superpack.SuperpackMain;

public final class GeneralUtil {
    private static final String[] SIZE_UNITS = {
        "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"
    };
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,###.##");
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final URL GITHUB_MARKDOWN_URL;

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
        final Frame ownerFrame = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, owner);
        showErrorMessage(owner, message, ownerFrame != null ? ownerFrame.getTitle() : SuperpackMain.APP_NAME);
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String message, String title) {
        owner.getLogger().error(message);
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void onlyShowErrorMessage(Component owner, String message) {
        final Frame ownerFrame = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, owner);
        onlyShowErrorMessage(owner, message, ownerFrame != null ? ownerFrame.getTitle() : SuperpackMain.APP_NAME);
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
        final URLConnection cnxn = GITHUB_MARKDOWN_URL.openConnection();
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
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
}
