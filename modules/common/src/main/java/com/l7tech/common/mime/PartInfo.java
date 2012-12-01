/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulates a MIME part and its metadata, and provides a way to
 * access the part's body content (wherever it might currently be stored).
 * <p>
 * Changing the content transfer encoding for a part will not automatically
 * re-encode the contents. To re-encode, the data should be read, the encoding
 * changed and the data written.
 */
public interface PartInfo {
    /** @return the specified MIME header (ie, "Content-Disposition") or null if this Part did not include it. */
    MimeHeader getHeader(String name);

    /** @return the ordinal position of this Part in the message.  The SOAP part is always ordinal zero. */
    int getPosition();

    /**
     * Obtain this multipart part's content as an InputStream.  This method can be called more than once on the same
     * PartInfo as long as destroyAsRead continues to be false.  Subsequent calls will receive an InputStream
     * that will play back the entire attachment body data.
     * <p>
     * If destroyAsRead is true, the caller must consume the returned InputStream before calling getInputStream()
     * on other PartInfos from the MimeBody that produced this PartInfo, or any methods in the
     * MimeBody.
     * <p>
     * If the parts content transfer encoding requires decoding this is performed automatically, this means that
     * in some cases the data read from the stream can be more or less than the content length of the part.
     *
     * @param destroyAsRead if false, the InputStream will be stashed and subsequent callers will be able to
     *                      obtain it and read it from the beginning again.  If true, the InputStream's contents
     *                      might not be saved and subsequent calls to getInputStream() on the same PartInfo will
     *                      fail with NoSuchPartException.
     * @return the content of this part as an InputStream.  Never null.  Will return EOF at the end of the body part.
     * @throws NoSuchPartException if this PartInfo's InputStream has already been destructively read.
     * @throws IOException if there is a problem retrieving a stashed InputStream.
     */
    InputStream getInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException;

    /**
     * Get this part's body as a byte array, but only if it is already available internally in that form.
     * Use this method only as an optimization in cases where you would always have slurped the entire InputStream
     * into a byte array anyway.
     * <p>
     * If the parts content transfer encoding requires decoding the bytes are always considered unavailable.
     *
     * @return the body bytes of this part, if they are already available as a byte array, or null if they are not
     *         available in that form.
     *        <b>Note:</b> any returned byte array is the live data, not a copy, and should <b>not be
     *           modified</b> in any way.
     */
    byte[] getBytesIfAlreadyAvailable();

    /**
     * Get this part's body as a byte array, creating a new byte array if necessary.
     * <p/>
     * If a byte array is already available internally, this method will just return that.
     * Otherwise, this method will create a new byte array and return it as long as the {@link #getActualContentLength}
     * is maxSize or smaller.
     * <p/>
     * <b>Note:</b> this method is intended to be used for debugging and logging purposes.  Production code should
     * generally avoid assuming that part bodies are small enough to fit in memory -- even the XML part.
     * Only use this method if your only alternative would be to just make a new byte array yourself.
     * <p>
     * If the parts content transfer encoding requires decoding this is performed automatically, this means that the
     * actual size of the data could be greater than the given maximum.
     *
     * @param maxSize  Mamimum size if a new byte array needs to be created.  Ignored if a byte array is already
     *                 available.
     * @return a byte array containing all bytes from the current part, or null if the part size exceeds maxSize.
     *        <b>Note:</b> the returned byte array should be assumed to be the live data, not a copy, and should <b>not be
     *           modified</b> in any way.
     * @throws IOException if there is an IOException while reading the part to produce a new byte array.
     * @throws NoSuchPartException if this PartInfo's InputStream has already been destructively read.
     */
    byte[] getBytesIfAvailableOrSmallerThan(int maxSize) throws IOException, NoSuchPartException;

    /**
     * Completely replace the body content of this multipart part.  This may require reading and discarding the
     * old content, and will result in the new content being stashed.
     * <p>
     * The Content-Length will be updated with the new length.
     * <p>
     * If the parts content transfer encoding requires encoding this is performed automatically on the given
     * data.
     *
     * @param newBody         the new body content to substitute.  May be empty but not null.
     * @throws IOException    if there is a problem reading past the original part body in the main InputStream; or,
     *                        if there is a problem stashing the new part body
     */
    void setBodyBytes(byte[] newBody) throws IOException;

    /**
     * Completely replace the body content of this multipart part.  This may require reading and discarding the
     * old content, and will result in the new content being stashed.
     * <p>
     * The Content-Length will be updated with the new length.
     * <p>
     * If the parts content transfer encoding requires encoding this is performed automatically on the given
     * data.
     *
     * @param newBody         the new body content to substitute.  May be empty but not null.
     * @param offset          the offset of the data in newBody
     * @param length          the length of the data in newBody
     * @throws IOException    if there is a problem reading past the original part body in the main InputStream; or,
     *                        if there is a problem stashing the new part body
     */
    void setBodyBytes(byte[] newBody, int offset, int length) throws IOException;

    /**
     * Replace the Content-Type, perhaps due to a change in body content.
     *
     * @param newContentType  the content type.  Must not be null.
     */
    void setContentType(ContentTypeHeader newContentType);

    /** @return the MimeHeaders describing this Part.  Never null. */
    MimeHeaders getHeaders();

    /**
     * Get the content-length of this Part, if known.  If the Part has already been read and its actual size is
     * known, that will be returned; otherwise, if there's a Content-Length: header, its value will be returned;
     * otherwize, if the Part's body has not yet been read, -1 will be returned.
     * <p>
     * To force the body to be read if it hasn't been already, use {@link #getActualContentLength} instead.
     * <p>
     * The length is for the encoded data, so if the part has a content transfer encoding that requires encoding
     * the size of data that can be accessed from the part may be larger or smaller than this value.
     *
     * @return the content length known or declared for this Part, or -1 if this information is not available.
     */
    long getContentLength();

    /**
     * Get the actual length of this Part's body in bytes.  Any length declared in a Content-Length: header will
     * be ignored.  This will read and stash the entire body, if necessary, in order to obtain an accurate answer.
     * The Content-Length header will be updated with the new, accurate information.
     * <p>
     * The length is for the encoded data, so if the part has a content transfer encoding that requires encoding
     * the size of data that can be accessed from the part may be larger or smaller than this value.
     *
     * @return The length of this part in bytes.  Always nonnegative, and always accurate.
     * @throws IOException  if the main InputStream could not be read; or,
     *                      if there was a problem recalling from the stash
     * @throws NoSuchPartException if this part's body has already been destructively read
     */
    long getActualContentLength() throws IOException, NoSuchPartException;

    /** @return the ContentTypeHeader, or a default value.  Never null. */
    ContentTypeHeader getContentType();

    /**
     * Get the Content-ID value, optionally stripping enclosing angle brackets.
     *
     * @param stripAngleBrackets true to strip angle brackets, or false to get the raw value.
     * @return the Content-ID value, possibly with any enclosing &lt; &gt; characters removed; or null if there isn't one.
     */
    String getContentId(boolean stripAngleBrackets);

    /**
     * @return true if this PartInfo was previously tagged as valid with setValidated(true).
     *         This flag is not used by artInfo itself in any way.
     */
    boolean isValidated();

    /**
     * @param validated true if you would like to flag this attachment as valid.
     *        This flag is not used by PartInfo itself in any way. 
     */
    void setValidated(boolean validated);

    /**
     * See if it will be possible to call getInputStream() on this PartInfo and get back a non-null InputStream.
     * <p/>
     * For a simple part (eg, not some more complex situation like one backing a DOM tree),
     * this is equivalent to {@link #isBodyStashed()} || !{@link #isBodyRead()}.
     *
     * @return true if {@link #getInputStream(boolean)} would return an InputStream (perhaps meaning this Part's body has been stashed,
     *         or is currently waiting to be read); false if it would throw {@link NoSuchPartException} (perhaps because it has already been read destructively).
     */
    boolean isBodyAvailable();

    /**
     * Check if this body has been stashed to the stash manager.
     *
     * @return true if our stash manager has an InputStream available for this part's ordinal.
     */
    boolean isBodyStashed();

    /**
     * Check if the body of this part has been read from the source InputStream.
     *
     * @return false if the body has not yet been read.  True if it has been read (it may or may not have been stashed,
     * depending on whether it was read destructively).
     */
    boolean isBodyRead();
}
