/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

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

    PartIterator getParts() throws IOException;

    PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException;

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
