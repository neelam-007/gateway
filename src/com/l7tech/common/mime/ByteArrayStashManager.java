/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * A StashManager that can only be used for a single request, and which always buffers all InputStreams in memory.
 */
public class ByteArrayStashManager implements StashManager {
    private ArrayList stashed = new ArrayList(); // contains byte arrays

    public ByteArrayStashManager() {
    }

    public void stash(int ordinal, InputStream in) throws IOException {
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream(4096);
        try {
            HexUtils.copyStream(in, baos);
            stash(ordinal, baos.toByteArray());
        } finally {
            baos.close();
        }
    }

    /**
     * For callers that already possess the entire InputStream as a byte array, this method can be used
     * to stash the byte array directly without needing to copy it again.
     *
     * @param ordinal a small non-negative integer to identify this stream within the stash
     * @param in      the byte array to stash.  May be zero-length but must be non-null.
     */
    public void stash(int ordinal, byte[] in) {
        while (stashed.size() <= ordinal)
            stashed.add(null);
        stashed.set(ordinal, in);
    }

    public void unstash(int ordinal) {
        if (stashed.size() <= ordinal)
            return;
        stashed.set(ordinal, null);
    }

    public long getSize(int ordinal) {
        if (stashed.size() <= ordinal)
            return -1;
        byte[] buf = (byte[])stashed.get(ordinal);
        if (buf == null)
            return -1;
        return buf.length;
    }

    public InputStream recall(int ordinal) throws IOException, NoSuchPartException {
        return new ByteArrayInputStream(recallBytes(ordinal));
    }

    public boolean isByteArrayAvailable(int ordinal) {
        return peek(ordinal);
    }

    public byte[] recallBytes(int ordinal) throws NoSuchPartException {
        if (stashed.size() <= ordinal)
            throw new NoSuchPartException("No part stashed with the ordinal " + ordinal);
        byte[] buf = (byte[])stashed.get(ordinal);
        if (buf == null)
            throw new NoSuchPartException("No part stashed with the ordinal " + ordinal);

        return buf;
    }

    public boolean peek(int ordinal) {
        if (stashed.size() <= ordinal)
            return false;
        byte[] buf = (byte[])stashed.get(ordinal);
        return buf != null;
    }

    public int getMaxOrdinal() {
        return stashed.size();
    }

    public void close() {
        stashed.clear();
    }
}
