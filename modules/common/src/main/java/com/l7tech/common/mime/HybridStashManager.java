/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.util.SyspropUtil;
import com.l7tech.util.BufferPool;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;

import java.io.*;

/**
 * A StashManager that stashes parts in RAM at first, but moves them into a FileStashManager if they exceed a certain size.
 */
public class HybridStashManager implements StashManager {
    private static final int DEFAULT_MAX_INITIAL_BUFFER = 8192;
    private static final String MAX_INITIAL_BUFFER_PROPERTY = HybridStashManager.class.getName() + ".maxInitialBuffer";
    private static final int MAX_INITIAL_BUFFER = SyspropUtil.getInteger(MAX_INITIAL_BUFFER_PROPERTY, DEFAULT_MAX_INITIAL_BUFFER);

    private final int limit;
    private final int initialBuffer;
    private final File dir;
    private final String unique;
    private final ByteArrayStashManager ramstash = new ByteArrayStashManager();
    private FileStashManager filestash = null;
    private long size = 0; // our current total stash size

    /**
     * Create a new HybridStashManager that will store stashed InputStreams in RAM, but move them to disk if their
     * totalled sizes exceed the specified size limit.
     *
     * @param limit   the maximum total number of bytes, for all attachments stashed, to store in RAM.
     * @param dir     the directory in which to create stash files, should it be necessary; {@link FileStashManager}
     * @param unique  the unique filename prefix to use for created stash files; {@link FileStashManager}
     */
    public HybridStashManager(int limit, File dir, String unique) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        if (dir == null) throw new NullPointerException("directory must be supplied");
        if (unique == null || unique.length() < 1) throw new IllegalArgumentException("unique filename prefix missing or empty");
        this.limit = limit;
        this.dir = dir;
        this.unique = unique;

        // Avoid over-allocating since allocating huge buffers up front is expensive and not needed unless the message
        // turns out to be very large
        if (limit < 512)
            initialBuffer = limit;
        else if (limit > 32768)
            initialBuffer = MAX_INITIAL_BUFFER;
        else
            initialBuffer = limit / 4;
    }

    /**
     * Get or create a file stash.  Used internally, but also exposed as package-private for testing purposes.
     */
    FileStashManager getFilestash() throws IOException {
        if (filestash == null) {
            filestash = new FileStashManager(dir, unique);
        }
        return filestash;
    }

    public void stash(int ordinal, InputStream in) throws IOException {
        unstash(ordinal);

        if (size > limit) {
            // already at limit, go right to file stash
            getFilestash().stash(ordinal, in);
            long partSize = getFilestash().getSize(ordinal);
            if (partSize < 0)
                throw new IOException("Unable to determine size of file that was just written");
            size += partSize;
            return;
        }

        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream(initialBuffer);
        byte[] buff = null;
        try {
            buff = BufferPool.getBuffer(16384);
            int got;
            long newSize = size;
            while ((got = in.read(buff)) > 0) {
                baos.write(buff, 0, got);
                newSize += got;
                if (newSize > limit) {
                    // reached limit.  move it to the filestash
                    getFilestash().stash(ordinal, new SequenceInputStream(new ByteArrayInputStream(baos.getPooledByteArray(), 0, baos.size()), in));
                    size += getFilestash().getSize(ordinal);
                    return;
                }
            }

            // didn't hit limit yet.  Move it to the ram stash
            size = newSize;
            int length = baos.size();
            byte[] data = baos.detachPooledByteArray();
            ramstash.stash(ordinal, data, 0, length);
        } finally {
            baos.close();
            BufferPool.returnBuffer(buff);
        }
    }

    public void stash(int ordinal, byte[] in) throws IOException {
        unstash(ordinal);
        size += in.length;

        if (size  > limit) {
            getFilestash().stash(ordinal, in);
            return;
        }

        ramstash.stash(ordinal, in);
    }

    /**
     * Unstash the specified ordinal.
     * This does not affect the size limit -- once the limit is reached, file mode won't be turned off. 
     */
    public void unstash(int ordinal) {
        if (ramstash.peek(ordinal)) {
            long oldSize = ramstash.getSize(ordinal);
            if (oldSize > 0) size -= oldSize;
        } else if (filestash != null && filestash.peek(ordinal)) {
            long oldSize = filestash.getSize(ordinal);
            if (oldSize > 0) size -= oldSize;
        }
        ramstash.unstash(ordinal);
        if (filestash != null) filestash.unstash(ordinal);
    }

    public long getSize(int ordinal) {
        long s = ramstash.getSize(ordinal);
        if (s != -1) return s;
        if (filestash != null) return filestash.getSize(ordinal);
        return -1;
    }

    /**
     * Package-private test method that returns our current accounting size limit
     *
     * @return current total number of bytes we believe we are holding
     */
    long getCurrentTotalSize() {
        return size;
    }

    /**
     * Package-private test method that grants access to our byte array stash manager
     *
     * @return the ramstash.  Never null.
     */
    ByteArrayStashManager getRamStash() {
        return ramstash;
    }

    public InputStream recall(int ordinal) throws IOException, NoSuchPartException {
        InputStream ret = null;
        try {
            ret = ramstash.recall(ordinal);
        } catch (NoSuchPartException e) {
            /* fall back to file stash; we do want the IOException thrown, if any */
        }

        if (ret == null && filestash != null) ret = filestash.recall(ordinal);

        return ret;
    }

    public boolean isByteArrayAvailable(int ordinal) {
        return ramstash.isByteArrayAvailable(ordinal);
    }

    public byte[] recallBytes(int ordinal) throws NoSuchPartException {
        return ramstash.recallBytes(ordinal);
    }

    public boolean peek(int ordinal) {
        return ramstash.peek(ordinal) || (filestash != null && filestash.peek(ordinal));
    }

    public int getMaxOrdinal() {
        if (filestash == null) return ramstash.getMaxOrdinal();
        return Math.max(ramstash.getMaxOrdinal(), filestash.getMaxOrdinal());
    }

    public void close() {
        ramstash.close();
        if (filestash != null) {
            filestash.close();
            filestash = null;
        }
        size = 0;
    }
}
