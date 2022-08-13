package io.github.gaming32.superpack.util;

import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.stream.LongStream;

import javax.swing.JOptionPane;

public final class GeneralUtil {
    private GeneralUtil() {
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, Throwable t) {
        showErrorMessage(owner, "Error", t);
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String logMessage, Throwable t) {
        owner.getLogger().error(logMessage, t);
        JOptionPane.showMessageDialog(owner, t.getMessage(), t.getClass().getName(), JOptionPane.ERROR_MESSAGE);
    }

    public static <T extends Frame & HasLogger> void showErrorMessage(T owner, String message) {
        showErrorMessage(owner, message, owner.getTitle());
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String message, String title) {
        owner.getLogger().error(message);
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

    private static final String[] UNITS = {
        "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"
    };
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###.##");
    public static String getHumanFileSize(long size) {
        int divisor = 0;
        while (size >>> divisor > 2048L && divisor / 10 < UNITS.length) {
            divisor += 10;
        }
        final String unit = UNITS[divisor / 10];
        return FORMAT.format((double)size / (1L << divisor)) + " " + unit;
    }
}
