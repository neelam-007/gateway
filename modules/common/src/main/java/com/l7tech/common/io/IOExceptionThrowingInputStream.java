/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that will persistently throw the specified IOException.  Can be used by an Enumeration
 * implementation to report an IO error through a SequenceInputStream that is in the middle of enumerating it.
 */
public class IOExceptionThrowingInputStream extends InputStream {
    private final IOException exception;

    /** Create an InputStream that will throw the specified IOException for all IO operations. */
    public IOExceptionThrowingInputStream(IOException exception) {
        if (exception == null) throw new NullPointerException();
        this.exception = exception;
    }

    public int read() throws IOException {
        throw exception;
    }

    public int available() throws IOException {
        throw exception;
    }

    public void close() throws IOException {
        throw exception;
    }

    public synchronized void reset() throws IOException {
        throw exception;
    }

    public boolean markSupported() {
        return false;
    }

    public synchronized void mark(int readlimit) {
        // noop
    }

    public long skip(long n) throws IOException {
        throw exception;
    }

    public int read(byte b[]) throws IOException {
        throw exception;
    }

    public int read(byte b[], int off, int len) throws IOException {
        throw exception;
    }
}
