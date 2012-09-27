package com.l7tech.external.assertions.ahttp.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * An InputStream backed by a list of (initially-unread) ByteBuffer instances.
 * <p/>
 * Reading the InputStream will drain the buffers.  When the last buffer is drained,
 * reads from the InputStream will return EOF.
 */
public class ByteBuffersInputStream extends InputStream {
    private final Queue<ByteBuffer> nextBuffers;
    private ByteBuffer currentBuffer;

    public ByteBuffersInputStream(List<ByteBuffer> buffers) {
        nextBuffers = new LinkedList<ByteBuffer>(buffers);
        currentBuffer = null;
    }

    @Override
    public int read() throws IOException {
        current();
        if (currentBuffer == null)
            return -1;
        if (!currentBuffer.hasRemaining()) {
            currentBuffer = null;
            return -1;
        }
        return currentBuffer.get();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int total = 0;
        for (;;) {
            int got = readCurrent(b, off, len);
            if (got <= 0)
                break;
            total += got;
            off += got;
            if (total >= len)
                break;
        }
        return total;
    }

    private int readCurrent(byte[] b, int off, int len) throws IOException {
        current();
        if (currentBuffer == null)
            return -1;
        if (!currentBuffer.hasRemaining()) {
            currentBuffer = null;
            return -1;
        }

        final int remaining = currentBuffer.remaining();
        if (remaining < len) {
            currentBuffer.get(b, off, remaining);
            return remaining;
        }

        currentBuffer.get(b, off, len);
        return len;
    }

    private void current() {
        if (currentBuffer == null) {
            if (!nextBuffers.isEmpty()) {
                currentBuffer = nextBuffers.remove();
            }
        }
    }
}
