/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import javax.servlet.ServletInputStream;
import java.io.InputStream;
import java.io.IOException;

public class DelegatingServletInputStream extends ServletInputStream {
    private final InputStream is;

    public DelegatingServletInputStream(InputStream is) {
        this.is = is;
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte b[]) throws IOException {
        return is.read(b);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return is.read(b, off, len);
    }
}
