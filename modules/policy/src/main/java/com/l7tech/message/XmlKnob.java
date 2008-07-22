/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.xml.ElementCursor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Aspect of Message that contains an XML document.
 * <p>
 * TODO investigate employing {@link org.apache.xerces.dom.NodeImpl#isReadOnly()} 
 */
public interface XmlKnob extends MessageKnob {
    /**
     * Get a read-only cursor view of the current working document.  This will use an existing Tarari RaxDocument
     * if one is available; otherwise, it will use a DOM cursor.
     * <p/>
     * The returned cursor, and any duplicates made by the caller, are only guaranteed to remain valid as long as
     * no changes are made to this Message through other means.  In particular, calling {@link #getDocumentWritable}
     * or using the {@link MimeKnob} should be assumed to render any cursors invalid, and using such invalid
     * cursors may cause misbehavior up to and including runtime exceptions.
     *
     * @return an ElementCursor, which will be pointed at an element in the document but may have been left in
     *         an unexpected position by the last user of this cursor -- use moveToDocumentRoot() on the returned
     *         cursor (or a duplicate) to be sure of where it is pointed.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if there is a problem reading from or writing to a stash
     */
    ElementCursor getElementCursor() throws SAXException, IOException;

    /**
     * Get a read-only reference to the current working Document.  There is currently no way to enforce
     * that the returned Document is not modified; callers are expected to keep their word and avoid changing
     * the document in any way.
     * <p/>
     * The actual Document instance returned by this method is guaranteed to refer to the same underlying
     * object as would be returned by a later call to {@link #getDocumentWritable}, assuming no other major changes
     * have been made to the content of the Message in the meantime (ie, a call to @{link #setDocument} or changing
     * the MIME part out from underneath it with {@link MimeKnob#getFirstPart()}.setBytes()).
     * <p/>
     * Thus, a caller can initially call {@link #getDocumentReadOnly} to get a read-only document,
     * but then upgrade their returned Document reference to be writable
     * simply by calling getDocumentWritable() in void context.
     * <p/>
     * TODO use {@link #getElementCursor} instead, for read-only access that will work quickly with Tarari
     *
     * @see #getDocumentWritable for the method to use if you have any chance of modifying the Document
     * @return the current working Document.  Caller must not modify this in any way.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if there is a problem reading from or writing to a stash
     */
    Document getDocumentReadOnly() throws SAXException, IOException;

    /**
     * Get the writable instance of the current working Document.  If the original Document has not been saved yet,
     * a clone of the working Document will be saved at this point.  Also, the underlying MIME bytestream will be marked
     * as out-of-date.
     * <p>
     * The actual Document instance returned by this method is guaranteed to refer to the same underlying
     * object as would have been returned by an earlier call to {@link #getDocumentReadOnly}, assuming no other
     * major changes have been made to the content of the Message in the meantime (ie, a call to {@link #setDocument}).
     * <p>
     * Thus, a caller can initially call {@link #getDocumentReadOnly} to get a read-only document,
     * but then upgrade their returned Document reference to be writable
     * by calling getDocumentWritable().  If there is any doubt, the caller can double-check that the return value
     * strongly equals (ie, using ==) the earlier return value of getDocument().
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
     * @throws UnsupportedOperationException if originalDocumentSupport is not enabled on this Message
     */
    Document getOriginalDocument() throws SAXException, IOException;

    /**
     * set the Document.  Also invalidates the first MIME part.
     *
     * @param document the new Document.  Must not be null.
     */
    void setDocument(Document document);

    /**
     * Indicate whether this XmlKnob holds an already-parsed DOM tree.
     */
    boolean isDomParsed();

    /**
     * Indicate whether the policy that will be processing this message has any assertions that rely so heavily on
     * Tarari that it's worth discarding an already-parsed DOM tree.
     */
    void setTarariWanted(boolean pref);

    /**
     * Indicates whether the policy that will be processing this message has any assertions that rely so heavily on 
     * Tarari that it's worth discarding an already-parsed DOM tree.
     */
    boolean isTarariWanted();
}
