/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        HexUtils.copyStream(in, baos);
        stash(ordinal, baos.toByteArray());
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
        try {
            return peek(ordinal);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a byte array
        }
    }

    public byte[] recallBytes(int ordinal) throws NoSuchPartException {
        if (stashed.size() <= ordinal)
            throw new NoSuchPartException("No part stashed with the ordinal " + ordinal);
        byte[] buf = (byte[])stashed.get(ordinal);
        if (buf == null)
            throw new NoSuchPartException("No part stashed with the ordinal " + ordinal);

        return buf;
    }

    public boolean peek(int ordinal) throws IOException {
        if (stashed.size() <= ordinal)
            return false;
        byte[] buf = (byte[])stashed.get(ordinal);
        if (buf == null)
            return false;
        return true;
    }

    public void close() {
        stashed.clear();
    }
}
