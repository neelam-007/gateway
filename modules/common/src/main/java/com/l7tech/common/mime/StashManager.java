/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.Closeable;

/**
 * Users of MimeBody must provide it with a StashManager in which it can store multipart parts throughout
 * the duration of a request.  A StashManager can store the parts wherever it likes as long as it can stash an entire
 * InputStream on command, and provide a rewound view the InputStream when asked.
 * <p>
 * StashManagers may not be used by more than one thread simultaneously.
 */
public interface StashManager extends Closeable {
    /**
     * Stash the complete content of the specified InputStream into this StashManager instance keyed with the
     * specified ordinal number.
     * <p>
     * If you already have a byte array, the stash(int, byte[]) method will save you wrapping it in a ByteArrayInputStream.
     *
     * @param ordinal  a unique small non-negative integer to identify this InputStream for later recall.
     * @param in  the InputStream to stash.  Will be drained, read all the way to EOF, before this method returns,
     *                                       but will not be closed by this method.
     * @throws IOException if there is a problem stashing this InputStream.
     *                     Implementations are required to save and rethrow the IOException so that it may be recall()'ed later.
     */
    void stash(int ordinal, InputStream in) throws IOException;

    /**
     * For callers that already possess the entire content as a byte array, this method can be used
     * to stash the byte array directly without needing to wrap it in a ByteArrayInputStream.
     * <p>
     * Prefer this stash(int, byte[]) method, but <i>only if you already have a byte array</i>.  Otherwise, it is
     * recommended to feed the InputStream into the stash(int, InputStream) method instead.
     *
     * @param ordinal a small non-negative integer to identify this stream within the stash
     * @param in      the byte array to stash.  May be zero-length but must be non-null.
     * @throws IOException if there is a problem stashing this byte array.
     *                     Implementations are required to save and rethrow the IOException so that it may be recall()'ed later.
     */
    void stash(int ordinal, byte[] in) throws IOException;

    /**
     * Free all resources used by the specified previously-stashed InputStream.  After this call, recall(ordinal)
     * will return null and peek(ordinal) will return false.  If no such ordinal is currently stashed, this
     * method takes no action.
     *
     * @param ordinal the ordinal that will be unstashed.
     */
    void unstash(int ordinal);

    /**
     * Check the size of a previously-stashed InputStream in bytes, if this information is available.
     * All StashManagers are required to keep track of this information for InputStreams that have been fully stashed.
     *
     * @param ordinal the ordinal that was used in a previous call to stash().
     * @return the size in bytes of the stashed InputStream; or,
     *         -1 if no such ordinal was stashed
     */
    long getSize(int ordinal);

    /**
     * Recall the content of the previously-stashed InputStream given its ordinal number.
     *
     * @param ordinal the ordinal that was used in a previous call to stash().
     * @return an InputStream ready to play back the exact bytes that were previously stashed, followed by EOF.  Never null.
     * @throws IOException if there is a problem producing the InputStream,
     *                     or if an IOException was thrown by stash() for this ordinal.
     * @throws NoSuchPartException if no InputStream is currently stashed using this ordinal
     */
    InputStream recall(int ordinal) throws IOException, NoSuchPartException;

    /**
     * Check if recallBytes() on the specified ordinal would be able to return a byte array.  The general
     * contract is that a StashManager will only return a byte array if it already has one available; otherwise,
     * it should require you to recall the InputStream instead.
     *
     * @param ordinal the ordinal that was used in a previous call to stash()
     * @return true if a call to {@link #recallBytes} would succeed; otherwise, false
     */
    boolean isByteArrayAvailable(int ordinal);

    /**
     * Get the specified previously-stashed part as a byte array, if possible.  The general contract is that
     * a StashManager will only return a byte array if it already has one available; otherwise, it
     * should require you to recall the InputStream instead.
     * <p>
     * Because this is only allowed to work if a byte array is already available, this method does not
     * need to throw IOException.
     *
     * @param ordinal the ordinal that was used in a previous call to stash()
     * @return the byte array content of the stashed part.  Never null.
     * @throws NoSuchPartException if no byte array is currently on hand for this ordinal
     */
    byte[] recallBytes(int ordinal) throws NoSuchPartException;

    /**
     * Check to see if an InputStream with the specified ordinal is currently available in the stash.
     *
     * @param ordinal the ordinal that may have been used in a previous call to stash()
     * @return true if recall(ordinal) would produce a non-null InputStream; otherwise, false
     */
    boolean peek(int ordinal);

    /**
     * Return a number such that no ordinal with a higher number has been stashed into this StashManager instance.
     * This does not imply that this ordinal (or any ordinal) would currently exist if you called peek, nor that
     * this ordinal has ever even been stashed into this instance: just that nothing higher has ever been stashed.
     *
     * @return a number such that nothing higher has been stashed into this StashManager instance.
     */
    int getMaxOrdinal();

    /**
     * Notify that all previously-stashed InputStream data can now be released.  After this, the behaviour of
     * future calls to stash() and recall() is undefined.
     */
    void close();
}
