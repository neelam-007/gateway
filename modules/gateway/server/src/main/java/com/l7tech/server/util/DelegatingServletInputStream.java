/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import javax.servlet.ServletInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Extracted from {@link com.l7tech.server.admin.ws.AdminWebServiceFilter}.
*/
public class DelegatingServletInputStream extends ServletInputStream {
    private final InputStream is;

    public DelegatingServletInputStream(InputStream is) {
        this.is = is;
    }

    public int read() throws IOException {
        return is.read();
    }

    public int read(byte b[]) throws IOException {
        return is.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException {
        return is.read(b, off, len);
    }
}
