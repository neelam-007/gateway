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
     * Get a read-only view of the current working Document.  There is currently no way to enforce
     * that the returned Document is not modified; callers are expected to keep their word.
     *
     * @see #getDocumentWritable for the method to use if you have any chance of modifying the Document
     * @return the current working Document.  Caller must not modify this in any way.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if there is a problem reading from or writing to a stash
     */
    Document getDocumentReadOnly() throws SAXException, IOException;

    /**
     * Get a read-write copy of the current working Document.  If the original Document has not been saved yet,
     * a clone of the working Document will be saved at this point.  Also, the underlying MIME bytestream will be marked
     * as dirty.
     *
     * @see #getDocumentReadOnly for the read-only, faster version of this call
     * @return the current working Document.  Caller may modify this.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if there is a problem reading from or writing to a stash
     */
    Document getDocumentWritable() throws SAXException, IOException;

    /**
     * Get a read-only view of the original Document from when the MIME bytestream was first parsed.  There is
     * currently no way to enforce that the returned Document is not modified; callers are expected to keep their
     * word.
     * <p>
     * This might not be enabled unless {@link Message#setEnableOriginalDocument()} has been called.
     *
     * @see #getDocumentWritable for a way to get a writable version of the current working Document, instead
     * @return the original Document as it came from the MIME bytestream.  Caller must not modify this in any way.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if there is a problem reading from or writing to a stash
     */
    Document getOriginalDocument() throws SAXException, IOException;

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
