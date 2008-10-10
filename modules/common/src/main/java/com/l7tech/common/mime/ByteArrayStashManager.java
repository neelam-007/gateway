/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.IOUtils;
import com.l7tech.util.BufferPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * A StashManager that can only be used for a single request, and which always buffers all InputStreams in memory.
 */
public class ByteArrayStashManager implements StashManager {

    //- PUBLIC

    public ByteArrayStashManager() {
    }

    public void stash(int ordinal, InputStream in) throws IOException {
        BufferPoolByteArrayOutputStream baos = null;
        try {
            while (thrown.size() <= ordinal) thrown.add(null);
            baos = new BufferPoolByteArrayOutputStream(4096);
            IOUtils.copyStream(in, baos);

            int length = baos.size();
            byte[] data = baos.detachPooledByteArray();

            stash(ordinal, data, 0, length);
        } catch (IOException e) {
            thrown.set(ordinal, e);
            throw e;
        } finally {
            if (baos != null) baos.close();
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
        stash(ordinal, in, 0, in != null ? in.length : 0);
    }

    /**
     * For callers that already possess the entire InputStream as a byte array, this method can be used
     * to stash the byte array directly without needing to copy it again.
     *
     * @param ordinal a small non-negative integer to identify this stream within the stash
     * @param in      the byte array to stash.  May be zero-length but must be non-null.
     * @param offset  the offset into the byte array where data starts
     * @param length  the length of data in the byte array
     */
    public void stash(int ordinal, byte[] in, int offset, int length) {
        while (stashed.size() <= ordinal) stashed.add(null);
        while (thrown.size() <= ordinal) thrown.add(null);

        final StashInfo removed;
        if (in != null)
            removed = stashed.set(ordinal, new StashInfo(in, offset, length));
        else
            removed = stashed.set(ordinal, null);

        thrown.set(ordinal, null);

        if (removed != null)
            removed.release();
    }

    public void unstash(int ordinal) {
        if (stashed.size() <= ordinal || ordinal < 0)
            return;
        StashInfo removed = stashed.set(ordinal, null);
        if (removed != null)
            removed.release();
        if (thrown.size() <= ordinal || ordinal < 0)
            thrown.set(ordinal, null);
    }

    public long getSize(int ordinal) {
        try {
            StashInfo stashInfo = getStashInfo(ordinal);
            return stashInfo.length;
        }
        catch(NoSuchPartException nspe) {
            return -1;
        }
    }

    public InputStream recall(int ordinal) throws IOException, NoSuchPartException {
        rethrowStashIOException(ordinal);
        StashInfo stashInfo = getStashInfo(ordinal);
        return new ByteArrayInputStream(stashInfo.data, stashInfo.offset, stashInfo.length);
    }

    // re-throws the IOException that was thrown during stash(), if any
    private void rethrowStashIOException(int ordinal) throws IOException {
        if (thrown.size() <= ordinal || ordinal < 0)
            return;

        IOException ioex = thrown.get(ordinal);
        if (ioex != null) throw ioex;
    }

    public boolean isByteArrayAvailable(int ordinal) {
        return peek(ordinal);
    }

    public byte[] recallBytes(int ordinal) throws NoSuchPartException {
        StashInfo stashInfo = getStashInfo(ordinal);

        if (stashInfo.isFull()) {
            stashInfo.markShared();
        } else {
            byte newbuf[] = new byte[stashInfo.length];
            if (newbuf.length > 0)
                System.arraycopy(stashInfo.data, stashInfo.offset, newbuf, 0, newbuf.length);
            stash(ordinal, newbuf);
            stashInfo = getStashInfo(ordinal);
            stashInfo.markShared();
        }

        return stashInfo.data;
    }

    public boolean peek(int ordinal) {
        if (stashed.size() <= ordinal || ordinal < 0)
            return false;
        StashInfo stashInfo = stashed.get(ordinal);
        return stashInfo != null;
    }

    public int getMaxOrdinal() {
        return stashed.size();
    }

    public void close() {
        ArrayList<StashInfo> removed = new ArrayList<StashInfo>(stashed);
        stashed.clear();
        for (StashInfo stashInfo : removed) {
            if (stashInfo != null)
                stashInfo.release();
        }
    }

    //- PRIVATE

    private final ArrayList<StashInfo> stashed = new ArrayList<StashInfo>();
    private ArrayList<IOException> thrown = new ArrayList<IOException>();

    private StashInfo getStashInfo(int ordinal) throws NoSuchPartException {
        if (stashed.size() <= ordinal || ordinal < 0)
            throw new NoSuchPartException("No part stashed with the ordinal " + ordinal);

        StashInfo stashInfo = stashed.get(ordinal);
        if (stashInfo == null)
            throw new NoSuchPartException("No part stashed with the ordinal " + ordinal);

        return stashInfo;
    }

    private static final class StashInfo {
        private final byte[] data;
        private final int offset;
        private final int length;
        private boolean shared;

        StashInfo(byte[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
            this.shared = false;
        }

        boolean isFull() {
            return data != null && data.length == length && offset == 0;
        }

        boolean isShared() {
            return shared;
        }

        void markShared() {
            shared = true;
        }

        void release() {
            if (!isShared())
                BufferPool.returnBuffer(data);
        }
    }

}
