/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that persistently returns EOF.
 */
public class EmptyInputStream extends InputStream {
    public int read() throws IOException {
        return -1;
    }

    public int available() throws IOException {
        return 0;
    }
}
