package com.l7tech.skunkworks;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An OutputStream wrapper that synchronizes all calls.  Can be used to make a non-threadsafe OutputStream
 * implmentation (such as ByteArrayOutputStream) threadsafe.
 */
public class ConcurrentOutputStream extends FilterOutputStream {
    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.  OutputStream methods called
     * on this instance will be passed through to the delegate.
     * @param delegate delegate to which method calls should be forwarded.  Required.
     */
    public ConcurrentOutputStream(OutputStream delegate) {
        super(delegate);
        if (delegate == null) throw new NullPointerException("delegate");
    }

    public synchronized void write(int b) throws IOException {
        super.write(b);
    }

    public synchronized void write(byte b[]) throws IOException {
        super.write(b);
    }

    public synchronized void write(byte b[], int off, int len) throws IOException {
        super.write(b, off, len);
    }

    public synchronized void flush() throws IOException {
        super.flush();
    }

    public synchronized void close() throws IOException {
        super.close();
    }
}
