/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Aspect of Message that contains an XML document.
 */
public interface XmlKnob extends MessageKnob {
    /**
     * TODO: Add a version of this that will not invalidate the first part of the message
     * TODO: if the caller sets a flag indicating that it does not intend to modify the Document
     * TODO: Add a way to obtain the original document, before any undecorating
     * @return the parsed Document.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     */
    Document getDocument() throws SAXException, IOException;

    /**
     * set the Document.  Also invalidates the first MIME part.
     *
     * @param document the new Document.  Must not be null.
     */
    void setDocument(Document document);

    /**
     * Obtain the undecoration results for this Message, if it was undecorated.
     *
     * @return the ProcessorResult, or null if this Message has not been undecorated.
     */
    ProcessorResult getProcessorResult();

    /**
     * Store the undecoration results for this Message, if it has been undecorated.
     *
     * @param pr the results of undecorating this message, or null to remove any existing results.
     */
    void setProcessorResult(ProcessorResult pr);

    /**
     * Set the decorations that should be applied to this Message some time in the future.
     */
    void setDecorationRequirements(DecorationRequirements decorationRequirements);

    /**
     * Get the decorations that should be applied to this Message some time in the future,
     * or null if the message is not to be decorated.
     */
    DecorationRequirements getDecorationRequirements();

    /**
     * Get the decorations that should be applied to this Message some time in the future,
     * creating a new default set of decorations if there are no decorations pending.
     *
     * @return the current DecorationRequirements for this message, possibly newly created.  Never null.
     */
    DecorationRequirements getOrMakeDecorationRequirements();
}
