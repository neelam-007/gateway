/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import java.io.PushbackInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * An InputStream wrapper that will pass through to another InputStream until at most a certain number of bytes
 * have been read, at which point it will start throwing IOExceptions.
 */
public class ByteLimitInputStream extends PushbackInputStream {
    private long sizeLimit = 0;
    private long bytesRead = 0;

    public ByteLimitInputStream(InputStream in, int pushbackSize) {
        super(in, pushbackSize);
    }

    public ByteLimitInputStream(InputStream in) {
        super(in);
    }

    /**
     * change the current size limit.  Will close the stream and throw immediately if the limit has already been exceeded.
     */
    public void setSizeLimit(long newLimit) throws IOException {
        sizeLimit = newLimit;
        gotBytes(0);
    }

    private void gotBytes(long got) throws IOException {
        bytesRead += got;
        if (sizeLimit > 0 && bytesRead >= sizeLimit) {
            close();
            throw new IOException("Unable to read stream: the specified maximum data size limit would be exceeded");
        }
    }

    public int read() throws IOException {
        int b = super.read();
        gotBytes(1);
        return b;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int got = super.read(b, off, len);
        gotBytes(got);
        return got;
    }

    public long skip(long n) throws IOException {
        long got = super.skip(n);
        gotBytes(got);
        return got;
    }

    public void unread(byte[] b) throws IOException {
        super.unread(b);
        bytesRead -= b.length;
    }

    public void unread(byte[] b, int off, int len) throws IOException {
        super.unread(b, off, len);
        bytesRead -= len;
    }

    public void unread(int b) throws IOException {
        super.unread(b);
        bytesRead--;
    }

    public boolean markSupported() {
        return false;
    }
}
