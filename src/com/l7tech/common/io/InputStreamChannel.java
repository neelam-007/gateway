/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * <code>InputStream</code> to <code>ReadableByteChannel</code> adapter.
 *
 * @author emil
 * @version Oct 22, 2004
 */
public final class InputStreamChannel implements ReadableByteChannel {
    private boolean closed = false;
    private final InputStream in;

    public InputStreamChannel(InputStream in) {
        if (in == null) {
            throw  new IllegalArgumentException();
        }
        this.in = in;
    }

    /**
     * @throws IOException {@inheritDoc}
     */
    public void close() throws IOException {
        if (!closed) {
            in.close();
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
     * @param dst {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen())
            throw new ClosedChannelException();

        byte[] buffer = new byte[dst.remaining()];
        int readBytes = in.read(buffer);

        if (readBytes > 0)
            dst.put(buffer, 0, readBytes);

        return readBytes;
    }
}
