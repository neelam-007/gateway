/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that ignores all output.
 */
public class NullOutputStream extends OutputStream {
    public NullOutputStream() {
    }

    public void close() throws IOException {
    }

    public void flush() throws IOException {
    }

    public void write(int b) throws IOException {
    }

    public void write(byte b[]) throws IOException {
    }

    public void write(byte b[], int off, int len) throws IOException {
    }
}
