package com.l7tech.skunkworks.async;

import java.io.FilterInputStream;
import java.io.PushbackInputStream;
import java.io.IOException;

/**
 * Not thread-safe in the slightest!
 */
class ContentLengthLimitedInputStream extends FilterInputStream {
    private long left;
    private final PushbackInputStream pbis;

    public ContentLengthLimitedInputStream(PushbackInputStream pbis, long contentLength) {
        super(pbis);
        this.pbis = pbis;
        this.left = contentLength;
    }

    public int read() throws IOException {
        assert left > 0;
        if (left <= 0) return -1; // Done

        // The next byte, if any, belongs to the message
        int num = super.read();

        if (num == -1) {
            left = 0;
            return num;
        }

        left--;
        return num;
    }

    public int read(byte callerBuf[], int callerOffset, int callerLen) throws IOException {
        assert left > 0;
        if (left <= 0) return -1;

        int want = (int)Math.min(callerLen, left);

        int got = super.read(callerBuf, callerOffset, want);
        if (got == 0) {
            return 0;
        } else if (got < 0) {
            // Done
            left = 0;
            return got;
        } else if (got > left) {
            throw new IOException("Wrapped stream read " + got + " bytes; asked for maximum " + want);
        }

        if (got <= left) {
            left -= got;
            return got;
        }

        // I'm done; someone else gets the rest
        assert got > left;
        pbis.unread(callerBuf, (int)(callerOffset + left), (int)(got - left));
        left = 0;
        return got;
    }

    public void close() throws IOException {
        left = 0;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }
}
