/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import com.l7tech.common.mime.*;

import java.io.IOException;

/**
 * Aspect of a Message that contains an outer content type and at least one part's InputStream.
 */
public interface MimeKnob extends Knob {
    boolean isMultipart();

    PartIterator getParts() throws IOException;

    PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException;

    /**
     * @return the outer content type of the request, or a default.  never null.
     */
    ContentTypeHeader getOuterContentType() throws IOException;

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
