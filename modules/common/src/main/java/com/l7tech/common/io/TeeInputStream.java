/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>TeeInputStream</code> is a {@link FilterInputStream } which sends all
 * of its input to its underlying {@link InputStream }, as well as to an
 * {@link OutputStream}.
 * @author emil
 * @version Nov 5, 2004
 */
public class TeeInputStream extends FilterInputStream {
    private final OutputStream out;

    public TeeInputStream(InputStream in, OutputStream out) {
        super(in);
        this.out = out;
        // die early with npe
        this.in.getClass();
        this.out.getClass();
    }

    public int read() throws IOException {
        final int i = super.read();
        if (i < 0) {
            out.flush();
        } else {
            out.write(i);
        }
        return i;
    }

    public int read(byte b[], int off, int len) throws IOException {
        final int i = super.read(b, off, len);
        if (i <= 0) {
            out.flush();
        } else {
            out.write(b, off, i);
        }
        return i;
    }

    public void close() throws IOException {
        try {
            out.close();
        } finally {
            super.close();
        }
    }
}