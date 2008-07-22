/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An filter InputStream that invokes a Runnable on end-of-file.
 */
public class EofCallbackInputStream extends FilterInputStream {
    private Runnable endOfFileCallback = null;

    /**
     * Create a new InputStream that will pass through access to the specified InputStream, but will
     * invoke the endOfFileCallback once (and only once) when the passed-through stream reaches EOF.
     * Callback invocation takes place just before the EOF signal is delivered to the stream consumer.
     * <p>
     * The callback will not be invoked if the wrapped stream returns a zero-length read instead of -1,
     * or if the stream is closed.
     * The callback will be invoked at most once.  No callback or other special action will occur
     * if the wrapped stream throws IOException.  Not threadsafe.
     *
     * @param in                   the InputStream to wrap.  May not be null.
     * @param endOfFileCallback    the callback to invoke when in is closed or reached EOF.  May not be null.
     */
    public EofCallbackInputStream(InputStream in, Runnable endOfFileCallback) {
        super(in);
        if (in == null || endOfFileCallback == null) throw new NullPointerException();
        this.endOfFileCallback = endOfFileCallback;
    }

    private void doCallback() {
        if (endOfFileCallback != null) {
            Runnable r = endOfFileCallback;
            endOfFileCallback = null;
            r.run();
        }
    }

    public int read(byte b[]) throws IOException {
        int result = super.read(b);
        if (result < 0)
            doCallback();
        return result;
    }

    public int read(byte b[], int off, int len) throws IOException {
        final int r = super.read(b, off, len);
        if (r < 0)
            doCallback();
        return r;
    }

    public int read() throws IOException {
        final int r = super.read();
        if (r < 0)
            doCallback();
        return r;
    }

    public void close() throws IOException {
        super.close();
        doCallback();
    }
}
