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
    private ArrayList stashed = new ArrayList(); // an array of ByteArrayOutputStreams

    public ByteArrayStashManager() {
    }

    public void stash(int ordinal, InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        HexUtils.copyStream(in, baos);
        while (stashed.size() <= ordinal)
            stashed.add(null);
        stashed.set(ordinal, baos.toByteArray());
    }

    public long getSize(int ordinal) {
        if (stashed.size() <= ordinal)
            return -1;
        byte[] buf = (byte[])stashed.get(ordinal);
        if (buf == null)
            return -1;
        return buf.length;
    }

    public InputStream recall(int ordinal) throws IOException {
        if (stashed.size() <= ordinal)
            return null;
        byte[] buf = (byte[])stashed.get(ordinal);
        if (buf == null)
            return null;

        return new ByteArrayInputStream(buf);
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
        stashed = new ArrayList();
    }
}
