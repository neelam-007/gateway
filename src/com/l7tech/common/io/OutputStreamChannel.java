/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;

/**
 * <code>OutputStream</code> to <code>ReadableByteChannel</code> adapter.
 *
 * @author emil
 * @version Oct 22, 2004
 */
public final class OutputStreamChannel implements WritableByteChannel {
    private boolean closed = false;
    private OutputStream out;

    public OutputStreamChannel(OutputStream out) {
        if (out == null) {
            throw  new IllegalArgumentException();
        }
        this.out = out;
    }

    /**
     * @throws IOException {@inheritDoc}
     */
    public void close() throws IOException {
        if (!closed) {
            out.close();
            closed = true;
        }
    }

    /**
     * @return {@inheritDoc}
     */
    public boolean isOpen() {
        return !closed;
    }

    /**
     * @param src {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen())
            throw new ClosedChannelException();
        byte[] buffer;
        int len;
        if (src.hasArray()) {
            buffer = src.array();
            len = buffer.length;
            src.limit(0);
        } else {
            len = src.remaining();
            buffer = new byte[len];
            src.get(buffer);
        }
        out.write(buffer);
        return len;
    }
}
