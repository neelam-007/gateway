/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.io.IOException;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public interface XmlRequest extends Request, XmlMessage {
    /**
     * Returns a String containing the XML document of the <code>Request</code>.
     *
     * This method consumes any underlying <code>InputStream</code> if necessary
     * the first time it's called.  Subsequent calls will return the cached XML
     * document, leaving the InputStream unchanged.
     *
     * Note that SocketInputStreams are not resettable!
     *
     * @return a String containing the XML document of the Request.
     * @throws java.io.IOException
     */
    String getRequestXml() throws IOException;

    /**
     * Sets the String containing the XML document for this <code>Request</code>.
     *
     * Implementors <b>MUST</b> clear any cached data structures that are dependent on the
     * XML content of the request (e.g. DOM Documents) when this method is called.
     *
     * @param xml
     */
    void setRequestXml( String xml );

    public Map getRequestAttachments() throws IOException;

    public Part getRequestAttachment(int position) throws IOException;

    public String getMultipartBoundary();

    public MultipartMessageReader getMultipartReader();

    public Part getSoapPart() throws IOException;

    public boolean isMultipart();
}
