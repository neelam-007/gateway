/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * Users of MultipartMessage must provide it with a StashManager in which it can store multipart parts throughout
 * the duration of a request.  A StashManager can store the parts wherever it likes as long as it can stash an entire
 * InputStream on command, and provide a rewound view the InputStream when asked.
 * <p>
 * StashManagers may not be used by more than one thread simultaneously.
 */
public interface StashManager {
    /**
     * Stash the complete content of the specified InputStream into this StashManager instance keyed with the
     * specified ordinal number.
     *
     * @param ordinal  a unique small non-negative integer to identify this InputStream for later recall.
     * @param in  the InputStream to stash.  Will be drained, read all the way to EOF, before this method returns.
     * @throws IOException if there is a problem stashing this InputStream.
     */
    void stash(int ordinal, InputStream in) throws IOException;

    /**
     * Check the size of a previously-stashed InputStream in bytes, if this information is available.
     * Not all StashManagers may be capable of supplying this information.
     *
     * @param ordinal the ordinal that was used in a previous call to stash().
     * @return the size in bytes of the stashed InputStream; or,
     *         -1 if no such ordinal was stashed; or,
     *         -2 if a stream was stashed but its size cannot be determined.
     */
    long getSize(int ordinal);

    /**
     * Recall the content of the previously-stashed InputStream given its ordinal number, or null if no such
     * ordinal is currently stashed in this StashManager.
     *
     * @param ordinal the ordinal that was used in a previous call to stash().
     * @return an InputStream ready to play back the exact bytes that were previously stashed, followed by EOF.
     * @throws IOException if there is a problem producing the InputStream.
     */
    InputStream recall(int ordinal) throws IOException;

    /**
     * Check to see if an InputStream with the specified ordinal is currently available in the stash.
     *
     * @param ordinal the ordinal that may have been used in a previous call to stash()
     * @return true if recall(ordinal) would produce a non-null InputStream; otherwise, false
     * @throws IOException if there was a problem reading the stash, or if this information is unavailable
     */
    boolean peek(int ordinal) throws IOException;

    /**
     * Notify that all previously-stashed InputStream data can now be released.  After this, the behaviour of
     * future calls to stash() and recall() is undefined.
     */
    void close();
}
