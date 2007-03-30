/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.message;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;

import java.io.IOException;
import java.io.InputStream;

/**
 * Aspect of a Message that contains an outer content type and at least one part's InputStream.
 */
public interface MimeKnob extends MessageKnob {
    /**
     * Check if this is a multipart message or not.  This should almost never be necessary.
     *
     * @return true if this message is multipart, or false if it is single-part
     */
    boolean isMultipart();

    /**
     * Obtain an iterator that can be used to lazily iterate some or all parts in this MimeBody.
     * The iterator can be abandoned at any time, in which case any still-unread parts will be left in the main InputStream
     * (as long as they hadn't already needed to be read due to other method calls on MimeBody or PartInfo).
     * <p>
     * Since this iterator is roughly equivalent to just calling {@link #getPart(int)}
     * while there are more parts possible, it is safe to call other MimeKnob and {@link PartInfo} methods
     * while iterating.  The usual caveats regarding PartInfo methods apply though: specifically, it is not safe to
     * call any MimeKnob or PartInfo methods whatsoever if any destroyAsRead InputStreams are open
     * on a PartInfo.
     * <p>
     * Note that, differing from {@link java.util.Iterator}, this PartIterator might throw NoSuchPartException
     * from next() even if hasNext() returned true, if the input message was not properly terminated.
     *
     * @return a {@link PartIterator} ready to iterate all parts of this message from beginning to end.  Never null.
     * @throws java.io.IOException  if there is a problem reading enough of the message to start the iterator
     */
    PartIterator getParts() throws IOException;

    /**
     * Get the specified PartInfo from this message by ordinal position, with the first part as part #0.
     * Note that getting the parts out of order is supported but will involve reading and stashing the
     * intervening parts in the current StashManager.
     *
     * @param num   the ordinal number of the part within this multipart message, where the first part is Part #0
     * @return the PartInfo describing the specified MIME part.  Never null.
     * @throws NoSuchPartException if the specified part does not exist in this message
     * @throws IOException if there was a problem reading the message stream
     */
    PartInfo getPart(int num) throws IOException, NoSuchPartException;

    /**
     * Get the specified PartInfo from this message by Content-ID.  If the specified Content-ID has not already
     * been found, this may require reading, stashing, and parsing the rest of the message InputStream all
     * the way up to and including the closing delimiter of the multipart message in order to rule out the
     * existence of an attachment with this Content-ID.
     *
     * @param contentId   the Content-ID to look for, without any enclosing angle brackets.  May not be null.
     * @return the PartInfo describing the MIME part with the specified Content-ID.  Never null.
     * @throws NoSuchPartException if the entire message was examined and no part with that Content-ID was found.
     * @throws IOException if there was a problem reading the message stream
     */
    PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException;

    /**
     * Set the maximum size of the message body that the underlying MimeBody should be able to process.
     * This can be used to abort processing early if a too-large message is encountered.  This will have no
     * effect at all if all parts have already been read and stashed.  If it is detected that the size limit has
     * been reached or exceeded during stream processing, further reads from the main message body input stream
     * will be aborted.
     * <p/>
     * If a lower (but non-zero) limit has already been set, this call will not raise it again.
     *
     * @param sizeLimit the new size limit to enforce, or zero to turn off size limit enforcement.
     * @throws IOException if this limit has already been exceeded, but it wasn't too late to cancel the read of the
     *                     rest of the message.
     */
    void setContentLengthLimit(long sizeLimit) throws IOException;

    /**
     * @return the outer content type of the request, or a default.  never null.
     */
    ContentTypeHeader getOuterContentType() throws IOException;

    /**
     * @return the length of the entire message body.  This might involve reading and stashing the entire message, including all attachments!
     * @throws IOException if there was a problem reading from the message stream
     */
    long getContentLength() throws IOException;


    /**
     * When streaming mime parts, only include those that are marked as valid.
     *
     * <p>This does not effect the first part (which is always included).</p>
     */
    void setStreamValidatedPartsOnly();

    /**
     * @return an InputStream that will produce the entire message body, including attachments, if any.
     * @throws IOException if there was a problem reading from the message stream
     * @throws NoSuchPartException if any part's body is unavailable, e.g. because it was read destructively
     */
    InputStream getEntireMessageBodyAsInputStream() throws IOException, NoSuchPartException;

    /**
     * Get the PartInfo describing the first part of the message.  For single-part messages this is the
     * psuedopart describing the entire message body.  For multipart messages this is the very first part,
     * after the preamble; what the SOAP with attachments specification calls the "root part".
     *
     * @return the first PartInfo of the MIME message.  Never null.
     * @throws IOException if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     */
    PartInfo getFirstPart() throws IOException;
}
