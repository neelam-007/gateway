/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.mime.*;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class MessageAdapter implements Message {
    private static final Logger logger = Logger.getLogger(MessageAdapter.class.getName());
    private static int stashFileUnique = 1; // Used to make unique prefix for StashManager stash files
    protected MultipartMessage multipartMessage = null;
    private boolean firstPartBodyIsUpToDate = false; // if false, first part of multipartMessage might not be in sync with desired value ([un]decorated xml, say)
    private StashManager stashManager;
    private InputStream _inputStream;

    public MessageAdapter( TransportMetadata tm ) {
        _transportMetadata = tm;
    }

    public void setParameter( String name, Object value ) {
        if ( _params == Collections.EMPTY_MAP ) _params = new HashMap();
        _params.put( name, value );
    }

    public void setParameterIfEmpty( String name, Object value ) {
        Object temp = getParameter( name );
        if ( temp == null ) setParameter( name, value );
    }

    public Object getParameter( String name ) {
        Object value = doGetParameter(name);
        if ( value == null ) value = _params.get( name );
        if ( value instanceof Object[] )
            return ((Object[])value)[0];
        else
            return value;
    }

    public abstract Object doGetParameter(String name);

    public Object[] getParameterValues( String name ) {
        Object value = doGetParameter(name);
        if ( value == null ) value = _params.get(name);
        if ( value instanceof Object[] ) {
            return (Object[])value;
        } else if ( value == null ) {
            return null;
        } else {
            return new Object[] { value };
        }
    }

    public Iterator getParameterNames() {
        return _params.keySet().iterator();
    }

    public TransportMetadata getTransportMetadata() {
        return _transportMetadata;
    }

    protected synchronized InputStream getInputStream() {
        return _inputStream;
    }

    public synchronized void setInputStream( InputStream is ) {
        _inputStream = is;
    }

    public Collection getDeferredAssertions() {
        return _deferredAssertions.values();
    }

    public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {
        _deferredAssertions.put(owner, decoration);
    }

    public void removeDeferredAssertion(ServerAssertion owner) {
        _deferredAssertions.remove(owner);
    }

    public synchronized void runOnClose( Runnable runMe ) {
        if ( _runOnClose == Collections.EMPTY_LIST ) _runOnClose = new ArrayList();
        _runOnClose.add( runMe );
    }

    public void close() {
        Runnable runMe;
        Iterator i = _runOnClose.iterator();
        while ( i.hasNext() ) {
            runMe = (Runnable)i.next();
            runMe.run();
            i.remove();
        }
        if (multipartMessage != null)
            multipartMessage.close();
    }

    private transient List _runOnClose = Collections.EMPTY_LIST;

    protected TransportMetadata _transportMetadata;
    protected Map _params = new HashMap();
    protected Map _deferredAssertions = new LinkedHashMap();

    private static synchronized int getStashFileUnique() {
        return stashFileUnique++;
    }

    /**
     * Get or create a MultipartMessage for this message.  Requires that an InputStream be available.
     *
     * @throws IOException if the mainInputStream cannot be read or a multipart message is not in valid MIME format
     * @throws IOException if this message is multpart/related but does not have any parts
     * @throws IllegalStateException if no InputStream has been attached to this message
     */
    protected MultipartMessage getMultipartMessage() throws IOException {
        if (multipartMessage == null) {
            InputStream is = getInputStream();
            if (is == null)
                throw new IllegalStateException("No InputStream has been attached to this message.");

            String ctypeval = (String)getParameter(Message.PARAM_HTTP_CONTENT_TYPE);
            ContentTypeHeader ctype;
            try {
                ctype = ContentTypeHeader.parseValue(ctypeval);
            } catch (IOException e) {
                ctype = ContentTypeHeader.XML_DEFAULT;
                logger.warning("Incoming message had missing or invalid outer Content-Type header; assuming " + ctype.getValue());
            }
            try {
                multipartMessage = new MultipartMessage(getStashManager(), ctype, is);
            } catch (NoSuchPartException e) {
                throw new CausedIOException("Incoming message had an invalid MIME multipart format", e);
            }
        }
        return multipartMessage;
    }

    protected StashManager getStashManager() {
        if (stashManager == null) {
            stashManager = new HybridStashManager(ServerConfig.getInstance().getAttachmentDiskThreshold(),
                                                  ServerConfig.getInstance().getAttachmentDirectory(),
                                                  "att" + getStashFileUnique());
        }
        return stashManager;
    }

    /**
     * Notify that the first part's body may have changed, and that the first body part will need to be updated
     * before the next time the entire message body is reserialied.
     */
    protected void invalidateFirstBodyPart() {
        firstPartBodyIsUpToDate = false;
    }

    public boolean isMultipart() throws IOException {
        return getMultipartMessage().isMultipart();
    }

    public PartIterator getParts() throws IOException {
        return getMultipartMessage().iterator();
    }

    public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
        return getMultipartMessage().getPartByContentId(contentId);
    }

    public boolean isAdditionalUnreadPartsPossible() throws IOException {
        return getMultipartMessage().isMorePartsPossible();
    }

    /**
     * @return the outer content type of the request, or a default.  never null.
     */
    public ContentTypeHeader getOuterContentType() throws IOException {
        return getMultipartMessage().getOuterContentType();
    }

    /**
     * @return the entire length of the current message body including any applied decorations,
     *         all attachments, and any MIME boundaries; but not including any HTTP or other headers
     *         that would accompany this message over wire.
     * @throws org.xml.sax.SAXException if the SOAP part of this message was empty or was not well-formed XML
     * @throws java.io.IOException  if there was a problem reading from the message InputStream
     * @throws java.io.IOException  if there is a problem stashing the new SOAP part
     * @throws java.io.IOException  if a MIME syntax error was encountered reading a multipart message
     */
    public long getContentLength() throws IOException, SAXException {
        try {
            ensureFirstPartIsUpToDate();
            long len = 0;
            len = multipartMessage.getEntireMessageBodyLength();
            if (len < 0)
                throw new IllegalStateException("At least one multipart part length could not be determinated"); // can't happen
            return len;
        } catch (NoSuchPartException e) {
            throw new IllegalStateException("At least one multipart part's body has been lost"); // can't happen
        }
    }

    /**
     * @return an InputStream which will, when read, produce the entire current message body including any applied
     *         decorations, all attachments, and any MIME boundaries; but not including any HTTP or other headers
     *         that would accompany this message over wire.
     * @throws IOException if the main input stream could not be read, or a MIME syntax error was encountered.
     * @throws IOException if any multipart part bodies were read destructively and not replaced
     */
    public InputStream getEntireMessageBody() throws IOException, SAXException {
        try {
            ensureFirstPartIsUpToDate();
            return multipartMessage.getEntireMessageBodyAsInputStream(false);
        } catch (NoSuchPartException e) {
            throw new IOException("At least one multipart part's body has been lost");
        }
    }

    /**
     * Ensure that the first part is up-to-date with the decorated document.
     */
    private void ensureFirstPartIsUpToDate() throws IOException {
        if (firstPartBodyIsUpToDate)
            return;
        final byte[] bytes = getUpToDateFirstPartBodyBytes();
        final ContentTypeHeader ctype = getUpToDateFirstPartContentType();
        if (bytes != null && ctype != null)
            getMultipartMessage().getFirstPart().replaceBody(bytes, ctype);
        firstPartBodyIsUpToDate = true;
    }

    /**
     * Get the content type header that will be used if the first part's body is to be replaced before this
     * message is reserialized with getEntireMessageBody().  This will be called by ensureFirstPartIsUpToDate()
     * whenever the message is reserialized, if invalidateFirstBodyPart() has been called.
     *
     * @return the content type of the byte array to use for the body of the first part, or null to leave it as is.
     */
    protected ContentTypeHeader getUpToDateFirstPartContentType() throws IOException {
        return null;
    }

    /**
     * Get the byte array that will be used if the first part's body is to be replaced before this
     * message is reserialized with getEntireMessageBody().  This will be called by ensureFirstPartIsUpToDate()
     * whenever the message is reserialized, if invalidateFirstBodyPart() has been called.
     *
     * @return the byte array to use for the body of the first part, or null to leave it as is.
     */
    protected byte[] getUpToDateFirstPartBodyBytes() throws IOException {
        return null;
    }


}
