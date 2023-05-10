package io.github.gaming32.superpack.util;

import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.LongConsumer;

/**
 * Calls {@code handler} with the current number of bytes read after each change.
 * Any {@link java.io.UncheckedIOException}s thrown in the handler will be rethrown
 * as {@link java.io.IOException}s.
 */
public class TrackingInputStream extends FilterInputStream {
    private final LongConsumer handler;
    private long read = 0;
    private long mark = 0;

    public TrackingInputStream(InputStream in, LongConsumer handler) {
        super(in);
        this.handler = handler;
    }

    public LongConsumer getHandler() {
        return handler;
    }

    public long getRead() {
        return read;
    }

    public long getMark() {
        return mark;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            read++;
            try {
                handler.accept(read);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        return result;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        len = in.read(b, off, len);
        if (len > 0) {
            read += len;
            try {
                handler.accept(read);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        n = in.skip(n);
        if (n > 0) {
            read += n;
            try {
                handler.accept(read);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        return n;
    }

    @Override
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
        mark = read;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        if (read != mark) {
            read = mark;
            try {
                handler.accept(read);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
    }
}
