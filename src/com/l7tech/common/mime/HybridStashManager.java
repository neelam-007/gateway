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
    private final int limit;
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream(limit);
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

    public long getSize(int ordinal) {
        long s = ramstash.getSize(ordinal);
        if (s != -1) return s;
        if (filestash != null) return filestash.getSize(ordinal);
        return -1;
    }

    public InputStream recall(int ordinal) throws IOException {
        InputStream ret = ramstash.recall(ordinal);
        if (ret != null) return ret;
        if (filestash != null) return filestash.recall(ordinal);
        return null;
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
