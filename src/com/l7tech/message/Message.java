/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.mime.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author alex
 */
public interface Message {

    static final String PREFIX             = "com.l7tech.message";
    static final String PREFIX_HTTP        = PREFIX + ".http";
    static final String PREFIX_HTTP_HEADER = "header";

    public static final String PARAM_HTTP_CONTENT_TYPE      = PREFIX_HTTP_HEADER + "." + MimeUtil.CONTENT_TYPE;
    public static final String PARAM_HTTP_CONTENT_LENGTH    = PREFIX_HTTP_HEADER + "." + MimeUtil.CONTENT_LENGTH;
    public static final String PARAM_HTTP_DATE              = PREFIX_HTTP_HEADER + ".Date";

    TransportMetadata getTransportMetadata();
    Iterator getParameterNames();
    void setParameter( String name, Object value );
    void setParameterIfEmpty( String name, Object value );

    /**
     * Returns the value of a parameter, or the first value in a multivalued parameter if it has multiple values.
     */
    Object getParameter( String name );

    /**
     * Returns the array of values for a parameter, or an array with one element if it has one value.
     */
    Object[] getParameterValues( String name );

    /**
     * Obtain the ordered list of ServerAssertion instances that should be applied to this message after
     * policy tree traversal has completed.
     *
     * @return an ordered Collection of ServerAssertion instances.  May be null.
     */
    Collection getDeferredAssertions();

    /**
     * Schedule a deferred ServerAssertion to apply to the message after policy tree traversal has finished.
     * Each real, policy-embedded ServerAssertion instance may schedule at most one deferred assertion per Message.
     *
     * @param owner       The real, policy-embedded ServerAssertion that wants to apply this deferred decoration
     * @param decoration  The assertion to append to the list of deferred decorations.
     */
    void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration);

    /**
     * Cancel a deferred ServerAssertion, perhaps because its owner assertion (or some ancestor) was eventually
     * falsified.
     *
     * @param owner The real, policy-embedded ServerAssertion whose deferred assertion (if any) should be canceled.
     */
    void removeDeferredAssertion(ServerAssertion owner);

    /**
     * Get the outer content type header for this request.  If this is a single-part message, this is the only
     * content type and is the same as getFirstPart().getContentType().  If this is a multi-part message,
     * this will (currently) always be a "multipart/related" content type with a "boundary" parameter defining
     * the multipart boundary.
     *
     * @return the outer content type header for this request.  Never null.
     */
    ContentTypeHeader getOuterContentType() throws IOException;

    /**
     * Initialize or reinitialize this Message with the specified InputStream, whose bytes are to be interpreted
     * according to the specified outer Content-Type.  Once initialized, this Message takes ownership of the
     * InputStream but does not close it.
     *
     * @param messageBody       the InputStream that will be read as needed to discover message parts.  Not null.
     * @param outerContentType  the content type to use to interpret this InputStream.  Not null.
     *                          See {@link ContentTypeHeader} for some default value.
     * @throws IOException      if the Content Type was multipart, but the opening multipart boundary was not found.
     * @throws IOException      if there was a problem reading from the messageBody InputStream.
     */
    void initialize( InputStream messageBody, ContentTypeHeader outerContentType ) throws IOException;

    /** @return true if initialize() has been successfully called on this Message */
    boolean isInitialized();

    /** Adds a Runnable to a list of operations to be run when the message is closed (i.e. closing sockets or database connections) */
    void runOnClose( Runnable runMe );

    /**
     * Check if this is a multipart message.  Identical to getOuterContentType().isMultipart().
     *
     * @return true if this is a multipart message.
     * @throws IOException
     */
    boolean isMultipart() throws IOException;

    /**
     * Get the first part of this message, if it's been initialized.
     *
     * @return PartInfo describing the first part.  Never null.
     * @throws IllegalStateException if this message has not been initialized 
     */
    PartInfo getFirstPart();

    /**
     * Obtain an iterator that can be used to lazily iterate some or all parts in the MultipartMessage.
     * The iterator can be abandoned at any time, in which case any still-unread parts will be left in the main InputStream
     * (as long as they hadn't already needed to be read due to other method calls on Message or PartInfo).
     * <p>
     * It is not safe to call any Message or PartInfo methods whatsoever if any destroyAsRead InputStreams are open
     * on a PartInfo.
     * <p>
     * Note that, differing from {@link java.util.Iterator}, this PartIterator might throw NoSuchPartException
     * from next() even if hasNext() returned true, if the input message was not properly terminated.
     *
     * @return a {@link PartIterator} ready to iterate all parts of this message from beginning to end.  Never null.
     */
    public PartIterator getParts() throws IOException;

    /**
     * Get the specified PartInfo from this message by Content-ID.  If the specified Content-ID has not already
     * been seen, this may require reading, stashing, and parsing the rest of the message InputStream all
     * the way up to and including the closing delimiter of the multipart message in order to rule out the
     * existence of an attachment with this Content-ID.
     *
     * @param contentId   the Content-ID to look for, without any enclosing angle brackets.  May not be null.
     * @return the PartInfo describing the MIME part with the specified Content-ID.  Never null.
     * @throws NoSuchPartException if the entire message was examined and no part with that Content-ID was found.
     * @throws IOException if there was a problem reading the message stream
     */
    public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException;

    /**
     * @return the entire length of the current message body including any applied decorations,
     *         all attachments, and any MIME boundaries; but not including any HTTP or other headers
     *         that would accompany this message over wire.
     * @throws org.xml.sax.SAXException if the SOAP part of this message was empty or was not well-formed XML
     * @throws java.io.IOException  if there was a problem reading from the message InputStream
     * @throws java.io.IOException  if there is a problem stashing the new SOAP part
     * @throws java.io.IOException  if a MIME syntax error was encountered reading a multipart message
     */
    long getContentLength() throws IOException, SAXException;

    /**
     * @return an InputStream which will, when read, produce the entire current message body including any applied
     *         decorations, all attachments, and any MIME boundaries; but not including any HTTP or other headers
     *         that would accompany this message over wire.
     * @throws IOException if the main input stream could not be read, or a MIME syntax error was encountered.
     * @throws IllegalStateException if this Message is not attached to an InputStream
     */
    InputStream getEntireMessageBody() throws IOException, SAXException;

    /** Indicates that the message is done and any resources that were opened during the course of the message can now be closed. */
    void close();
}
