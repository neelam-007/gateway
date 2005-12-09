/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import java.io.*;

/**
 * A StashManager that stashes parts in RAM at first, but moves them into a FileStashManager if they exceed a certain size.
 */
public class HybridStashManager implements StashManager {
    private static final int DEFAULT_MAX_INITIAL_BUFFER = 8192;
    private static final String MAX_INITIAL_BUFFER_PROPERTY = HybridStashManager.class.getName() + ".maxInitialBuffer";
    private static final int MAX_INITIAL_BUFFER = Integer.getInteger(MAX_INITIAL_BUFFER_PROPERTY,
                                                                     DEFAULT_MAX_INITIAL_BUFFER).intValue();

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
     * @param dir     the directory in which to create stash files, should it be necessary; {@see FileStashManager}
     * @param unique  the unique filename prefix to use for created stash files; {@see FileStashManager}
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

    /** Get or create a file stash. */
    private FileStashManager getFilestash() throws IOException {
        if (filestash == null) {
            filestash = new FileStashManager(dir, unique);
        }
        return filestash;
    }

    public void stash(int ordinal, InputStream in) throws IOException {
        ramstash.unstash(ordinal);

        if (size > limit) {
            // already at limit, go right to file stash
            getFilestash().stash(ordinal, in);
            long partSize = getFilestash().getSize(ordinal);
            if (partSize < 0)
                throw new IOException("Unable to determine size of file that was just written");
            size += partSize;
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(initialBuffer);
        byte[] buff = new byte[4096];
        int got;
        while ((got = in.read(buff)) > 0) {
            baos.write(buff, 0, got);
            size += got;
            if (size > limit) {
                // reached limit.  move it to the filestash
                getFilestash().stash(ordinal, new SequenceInputStream(new ByteArrayInputStream(baos.toByteArray()), in));
                return;
            }
        }

        // didn't hit limit yet.  Move it to the ram stash
        ramstash.stash(ordinal, baos.toByteArray());
    }

    public void stash(int ordinal, byte[] in) throws IOException {
        ramstash.unstash(ordinal);
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
        ramstash.unstash(ordinal);
        if (filestash != null) filestash.unstash(ordinal);
    }

    public long getSize(int ordinal) {
        long s = ramstash.getSize(ordinal);
        if (s != -1) return s;
        if (filestash != null) return filestash.getSize(ordinal);
        return -1;
    }

    public InputStream recall(int ordinal) throws IOException, NoSuchPartException {
        InputStream ret = null;
        if (ramstash.peek(ordinal))
            ret = ramstash.recall(ordinal);
        if (ret != null) return ret;
        if (filestash != null) return filestash.recall(ordinal);
        return null;
    }

    public boolean isByteArrayAvailable(int ordinal) {
        return ramstash.isByteArrayAvailable(ordinal);
    }

    public byte[] recallBytes(int ordinal) throws NoSuchPartException {
        return ramstash.recallBytes(ordinal);
    }

    public boolean peek(int ordinal) throws IOException {
        boolean r = ramstash.peek(ordinal);
        if (r) return r;
        if (filestash != null) return filestash.peek(ordinal);
        return false;
    }

    public void close() {
        ramstash.close();
        if (filestash != null) {
            filestash.close();
            filestash = null;
        }
        size = 0;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
}
