/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An outputstream that duplicates all output to two different streams.
 */
public class TeeOutputStream extends FilterOutputStream {
    final OutputStream out2;

    public TeeOutputStream(OutputStream out, OutputStream out2) {
        super(out);
        this.out2 = out2;
    }

    public void write(int b) throws IOException {
        super.write(b);
        out2.write(b);
    }

    public void write(byte b[]) throws IOException {
        super.write(b);
        out2.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        super.write(b, off, len);
        out2.write(b, off, len);
    }

    public void close() throws IOException {
        super.close();
        out2.close();
    }

    public void flush() throws IOException {
        super.flush();
        out2.flush();
    }
}
