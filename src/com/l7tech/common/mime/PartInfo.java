/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulates a MIME part, including metadata (map of name=>MimeHeader, ordinal position within the
 * set of attachments starting from zero, a content length in bytes, and a validated flag), and sometimes
 * including the actual attachment content (when it is small enough to fit in a String).
 * <p>
 * The content held by PartInfo is not believed to contain the MIME separator.
 */
public interface PartInfo {
    /** @return the specified MIME header (ie, "Content-Disposition") or null if this Part did not include it. */
    public MimeHeader getHeader(String name);

    /** @return the ordinal position of this Part in the message.  The SOAP part is always ordinal zero. */
    public int getPosition();

    /**
     * Get the part's body content as a byte array.
     * TODO do we need this?
     * @return the content of this part as a byte array.  Never null.
     * @deprecated use getInputStream() instead.
     */
    public byte[] getContent() throws IOException;

    /**
     * Obtain this multipart part's content as an InputStream.  This method can be called more than once on the same
     * PartInfo as long as destroyAsRead continues to be false.  Subsequent calls will receive an InputStream
     * that will play back the entire attachment body data.
     * <p>
     * If destroyAsRead is true, the caller must consume the returned InputStream before calling getInputStream()
     * on other PartInfos from the MultipartMessage that produced this PartInfo, or any methods in the
     * MultipartMessage.
     *
     * @param destroyAsRead if false, the InputStream will be stashed and subsequent callers will be able to
     *                      obtain it and read it from the beginning again.  If true, the InputStream's contents
     *                      might not be saved and subsequent calls to getInputStream() on the same PartInfo will
     *                      fail.
     * @return the content of this part as an InputStream.  Never null.  Will return EOF at the end of the body part.
     * @throws IOException if this PartInfo's InputStream has already been destructively read.
     * @throws IOException if there is a problem retrieving a stashed InputStream.
     */
    public InputStream getInputStream(boolean destroyAsRead) throws IOException;

    /** @return the MimeHeaders describing this Part.  Never null. */
    public MimeHeaders getHeaders();

    /**
     * Get the content-length of this Part, if known.  If the Part has already been read and its actual size is
     * known, that will be returned; otherwise, if there's a Content-Length: header, its value will be returned;
     * otherwize, -1 will be returned.
     *
     * @return the content length known or declared for this Part, or -1 if this information is not available.
     */
    public long getContentLength();

    /** @return the ContentTypeHeader, or a default value.  Never null. */
    public ContentTypeHeader getContentType();

    /** @return the Content-ID value with any enclosing &lt; &gt; characters removed; or null if there isn't one. */
    public String getContentId();

    // TODO do we need this?
    public boolean isValidated();

    // TODO do we need this?
    public void setValidated(boolean validated);
}
