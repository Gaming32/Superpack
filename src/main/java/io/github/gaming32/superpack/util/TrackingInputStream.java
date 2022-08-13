package io.github.gaming32.superpack.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

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
            handler.accept(read);
        }
        return result;
    };

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        len = in.read(b, off, len);
        if (len != 0) {
            read += len;
            handler.accept(read);
        }
        return len;
    };

    @Override
    public long skip(long n) throws IOException {
        n = in.skip(n);
        if (n != 0) {
            read += n;
            handler.accept(read);
        }
        return n;
    };

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
            handler.accept(read);
        }
    }
}
