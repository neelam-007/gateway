/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Aspect of Message that contains an XML document.
 */
public interface XmlKnob extends Knob {
    /**
     * TODO: Add a version of this that will not invalidate the first part of the message
     * TODO: if the caller sets a flag indicating that it does not intend to modify the Document
     * @return the parsed Document.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     */
    Document getDocument() throws SAXException, IOException;

    /**
     *
     * @param document
     */
    void setDocument(Document document);
}
