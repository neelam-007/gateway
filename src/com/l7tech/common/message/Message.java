/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.common.xml.MessageNotSoapException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an abstract Message in the system.  This can be a request or a reply; over HTTP or JMS or transport
 * not yet determined; using SOAP, MIME, XML, or not yet set.  Any message at all.
 * <p>
 * All Messages and MessageFacets should be assumed <em>not</em> to be threadsafe.
 */
public final class Message {
    private MessageFacet rootFacet;
    private boolean enableOriginalDocument = false; // enable this to enable XmlKnob.getOriginalDocument().
                                                    // This is off by default since only certain messages need this.

    /**
     * Create a Message with no facets.
     */
    public Message() {
    }

    /**
     * Create a Message pre-initialized with a MIME facet attached to the specified InputStream.
     *
     * @param sm  the StashManager to use for stashing MIME parts temporarily.  Must not be null.
     * @param outerContentType  the content type of the body InputStream.  Must not be null.
     * @param body an InputStream positioned at the first byte of body content for this Message.
     * @throws NoSuchPartException if the message is multipart/related but contains no initial boundary
     * @throws IOException if there is a problem reading the initial boundary from a multipart/related body
     */
    public Message(StashManager sm,
                   ContentTypeHeader outerContentType,
                   InputStream body)
            throws NoSuchPartException, IOException
    {
        initialize(sm, outerContentType, body);
    }

    /**
     * Create a Message pre-initialized with a Document.
     *
     * @param doc the Document to use.  Must not be null.
     */
    public Message(Document doc) {
        initialize(doc);
    }

    /**
     * Initialize, or re-initialize, a Message with a MIME facet attached to the specified InputStream.
     * <p>
     * With the exception of {@link HttpRequestKnob} and {@link HttpResponseKnob}s, which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param sm  the StashManager to use for stashing MIME parts temporarily.  Must not be null.
     * @param outerContentType  the content type of the body InputStream.  Must not be null.
     * @param body an InputStream positioned at the first byte of body content for this Message.
     * @throws NoSuchPartException if the message is multipart/related but contains no initial boundary
     * @throws IOException if there is a problem reading the initial boundary from a multipart/related body
     */
    public void initialize(StashManager sm,
                                  ContentTypeHeader outerContentType, 
                                  InputStream body) 
            throws NoSuchPartException, IOException 
    {
        HttpRequestKnob reqKnob = (HttpRequestKnob)getKnob(HttpRequestKnob.class);
        HttpResponseKnob respKnob = (HttpResponseKnob)getKnob(HttpResponseKnob.class);
        rootFacet = null;
        rootFacet = new MimeFacet(this, sm, outerContentType, body);
        if (reqKnob != null) attachHttpRequestKnob(reqKnob);
        if (respKnob != null) attachHttpResponseKnob(respKnob);
    }

    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet and an XML facet initialized with
     * the specified Document.
     * <p>
     * With the exception of {@link HttpRequestKnob} and {@link HttpResponseKnob}s, which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param body the Document to replace this Message's current content
     */
    public void initialize(Document body)
    {
        try {
            HttpRequestKnob reqKnob = (HttpRequestKnob)getKnob(HttpRequestKnob.class);
            HttpResponseKnob respKnob = (HttpResponseKnob)getKnob(HttpResponseKnob.class);
            rootFacet = null;
            rootFacet = new MimeFacet(this, new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
            rootFacet = new XmlFacet(this, rootFacet);
            if (reqKnob != null) attachHttpRequestKnob(reqKnob);
            if (respKnob != null) attachHttpResponseKnob(respKnob);
            getXmlKnob().setDocument(body);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen, it's not multipart
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a byte array input stream
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen, the content type is set to xml
        }
    }

    /**
     * Initialize, or re-initialize, a Message using the specified Message as a source.  <b>Note:</b>This does not do
     * a deep copy of the source Message's facets -- it just takes them and uses them for this Message.
     * <p>
     * All existing facets of this Message will be removed; unlike other initialize() methods, no existing facets will
     * be preserved.
     *
     * @param msg a Message whose facets should be taken by this Message.  Must not be null.
     */
    public void initialize(Message msg)
    {
        rootFacet = msg.rootFacet;
    }

    public void setEnableOriginalDocument() {
        this.enableOriginalDocument = true;
    }

    public boolean isEnableOriginalDocument() {
        return enableOriginalDocument;
    }

    /**
     * Get the knob for the MIME facet of this Message, which must already be attached
     * to an InputStream.
     *  
     * @return the MimeKnob for this Message.  Never null.
     * @throws IllegalStateException if this Message has not yet been attached to an InputStream.
     */ 
    public MimeKnob getMimeKnob() throws IllegalStateException {
        MimeKnob mimeKnob = (MimeKnob)getKnob(MimeKnob.class);
        if (mimeKnob == null) throw new IllegalStateException("This Message has not yet been attached to an InputStream");
        return mimeKnob;
    }
    
    /**
     * Get the knob for the XML facet of this Message, creating one if necessary and possible.
     * If no XML facet is currently installed, one will be created if there is a MIME facet whose
     * first part's content type is text/xml. 
     *
     * @return the XmlKnob for this Message.  Never null.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IllegalStateException if this Message has not yet been attached to an InputStream.
     */ 
    public XmlKnob getXmlKnob() throws SAXException {
        XmlKnob xmlKnob = (XmlKnob)getKnob(XmlKnob.class);
        if (xmlKnob == null) {
            try {
                rootFacet = new XmlFacet(this, rootFacet);
                xmlKnob = (XmlKnob)getKnob(XmlKnob.class);
                if (xmlKnob == null) throw new IllegalStateException(); // can't happen, we just made one
            } catch (IOException e) {
                throw new CausedIllegalStateException(e); // can't happen, no XML facet yet
            }
        }
        return xmlKnob;
    }

    /**
     * Check if this message is declared as containing XML.  Does not actually parse the XML, if it's there.
     * No exceptions are thrown except IOException, and that only in a situation that would be fatal to the Message
     * anyway.
     * <p>
     * If this method returns true, an XmlKnob will be present on this Message.
     *
     * @return true if this message has a first part declared as text/xml;
     *         false if this message has no first part or its first part isn't declared as XML.
     * @throws IOException if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     */
    public boolean isXml() throws IOException {
        if (getKnob(XmlKnob.class) != null)
            return true;
        MimeKnob mimeKnob = (MimeKnob)getKnob(MimeKnob.class);
        if (mimeKnob == null)
            return false;
        if (!mimeKnob.getFirstPart().getContentType().isXml())
            return false;

        // It's declared as XML.  Create the XML knob while we are here (won't actually try to parse yet)
        try {
            getXmlKnob();
            return true;
        } catch (SAXException e) {
            throw new CausedIllegalStateException("First part not XML", e); // can't happen here
        }
    }

    /**
     * Get the knob for the SOAP facet of this Message, creating one if necessary and possible.
     * If no SOAP facet is currently installed, one will be created if an XML facet can be created
     * and the message appears to be SOAP.
     *
     * @return the SoapKnob for this Message.  Never null.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
     * @throws MessageNotSoapException if there is an XML document but it doesn't look like a valid SOAP envelope
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    public SoapKnob getSoapKnob() throws SAXException, IOException, MessageNotSoapException {
        SoapKnob soapKnob = (SoapKnob)getKnob(SoapKnob.class);
        if (soapKnob == null) {
            if (!isSoap())
                throw new MessageNotSoapException();
            soapKnob = (SoapKnob)getKnob(SoapKnob.class);
            if (soapKnob == null)
                throw new IllegalStateException("isSoap() is true but there's no SoapKnob");
        }
        return soapKnob;
    }

    /**
     * Check if this message appears to contain a SOAP envelope.  This will fail fast if the message isn't XML,
     * but may need to begin parsing the XML if it is.
     * <p>
     * If this method returns true, a SoapKnob will be present on this Message.
     *
     * @return true if this mesage appears to contain SOAP.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    public boolean isSoap() throws IOException, SAXException {
        if (getKnob(SoapKnob.class) != null)
            return true;
        if (!isXml())
            return false;

        // We have an XML knob but no SOAP knob.  See if we can create a SOAP knob.
        SoapInfo info = null;
        try {
            info = SoapFacet.getSoapInfo(this);
        } catch (NoSuchPartException e) {
            throw new CausedIllegalStateException(e);
        }
        if (info == null)
            return false;

        rootFacet = new SoapFacet(this, rootFacet, info);
        return true;
    }

    /**
     * Check if this message is a Http request.
     * @return true if the message is a Http request
     */
    public boolean isHttpRequest() {
        if(getKnob(HttpRequestKnob.class) != null) {
            return true;
        }
        return false;
    }

    /**
     * Configure this Message as an HTTP request.  This attaches sources of TCP and HTTP request transport metadata to this
     * Message.  A Message may have at most one HTTP request knob.  An HTTP request knob may cooexist with
     * an HTTP response knob.
     *
     * @param httpRequestKnob  the source of HTTP request transport metadata.  May not be null.
     * @throws IllegalStateException if this Message is already configured as an HTTP request
     */
    public void attachHttpRequestKnob(HttpRequestKnob httpRequestKnob) throws IllegalStateException {
        if (getKnob(HttpRequestKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as an HTTP request");
        rootFacet = new HttpRequestFacet(this, rootFacet, httpRequestKnob);
    }

    /**
     * Configure this Message as a JMS Message.  This attaches a {@link javax.jms.Message} to this
     * Message.  A Message may have at most one JMS knob.
     *
     * @param jmsKnob source of JMS message.  May not be null.
     * @throws IllegalStateException if this Message is already configured as a JMS Message
     */
    public void attachJmsKnob(JmsKnob jmsKnob) throws IllegalStateException {
        if (getKnob(JmsKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as a JMS Message");
        rootFacet = new JmsFacet(this, rootFacet, jmsKnob);
    }

    /**
     * Obtain the source for HTTP request transport metadata.  This assumes that this Message has already been
     * configured as an HTTP request by calling {@link #attachHttpRequestKnob}.
     *
     * @return an {@link HttpRequestKnob} ready to act as a source for HTTP request transport metadata.  Never null.
     * @throws IllegalStateException if this Message is not configured as an HTTP request.
     */
    public HttpRequestKnob getHttpRequestKnob() throws IllegalStateException {
        HttpRequestKnob knob = (HttpRequestKnob)getKnob(HttpRequestKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as an HTTP request");
        return knob;
    }

    /**
     * Configure this Message as an HTTP response.  This attaches a sink for HTTP response transport metadata to this
     * Message.  A message may have at most one HTTP response knob.  An HTTP response knob may cooexist with an
     * HTTP request knob.
     *
     * @param httpResponseKnob  the sink for HTTP response transport metadata.  May not be null.
     * @throws IllegalStateException if this Message is already configured as an HTTP response
     */
    public void attachHttpResponseKnob(HttpResponseKnob httpResponseKnob) throws IllegalStateException {
        if (getKnob(HttpResponseKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as an HTTP response");
        rootFacet = new HttpResponseFacet(this, rootFacet, httpResponseKnob);
    }

    /**
     * Obtain the sink for HTTP response transport metadata.  This assumes that this Message has already been
     * configured as an HTTP response by calling {@link #attachHttpResponseKnob}.
     *
     * @return an {@link HttpResponseKnob} ready to act as a sink for HTTP response transport metadata.  Never null.
     * @throws IllegalStateException if this Message is not configured as an HTTP response
     */
    public HttpResponseKnob getHttpResponseKnob() throws IllegalStateException {
        HttpResponseKnob knob = (HttpResponseKnob)getKnob(HttpResponseKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as an HTTP response");
        return knob;
    }

    /**
     * Obtain the information about the TCP connection this message came over.  This assumes that this Message
     * has already been configured as having arrived via a TCP connection.
     *
     * @return the {@link TcpKnob}.  Never null
     * @throws IllegalStateException if this message is not configured as having arrived over TCP.
     */
    public TcpKnob getTcpKnob() throws IllegalStateException {
        TcpKnob knob = (TcpKnob)getKnob(TcpKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as having arrived over TCP");
        return knob;
    }

    public JmsKnob getJmsKnob() {
        JmsKnob knob = (JmsKnob)getKnob(JmsKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as having arrived over JMS");
        return knob;
    }

    /**
     * Attach the specified knob to this message if and only if it does not already provide that knob.
     *
     * @param knobClass the class of the interface provided by this knob implementation.
     * @param knob the knob to attach.  It will be attached in a new facet.  Must not be null.
     */
    public void attachKnob(Class knobClass, MessageKnob knob) {
        if (getKnob(knobClass) != null)
            throw new IllegalStateException("An implementation of the knob " + knobClass + " is already attached to this Message.");
        if (!knobClass.isAssignableFrom(knob.getClass()))
            throw new IllegalArgumentException("knob was not an implementation of knobClass " + knobClass);
        rootFacet = new KnobHolderFacet(this, rootFacet, knobClass, knob);
        if (getKnob(knobClass) == null)
            throw new IllegalArgumentException("knob failed to provide an implementation of knobClass" + knobClass); // can't happen
    }

    /**
     * Get the specified knob, which must already be provided.
     *
     * @param c a Class derived from MessageKnob.  Must be non-null.
     * @return the requested MessageKnob.  Never null.
     * @throws IllegalStateException if no such knob is currently available from this Message.
     */
    public MessageKnob getKnobAlways(Class c) throws IllegalStateException {
        MessageKnob got = getKnob(c);
        if (got == null)
            throw new IllegalStateException("This Message is not currently configured with " + c.getName());
        return got;
    }

    /**
     * If this Message has the specified knob, then return it.  Will not attempt to create any facets
     * that haven't yet been installed, even assuming it might be possible to do so.
     *
     * @param c a Class derived from MessageKnob.  Must be non-null.
     * @return the requested MessageKnob, if its facet is installed on this Message, or null.
     */
    public MessageKnob getKnob(Class c) {
        if (c == null) throw new NullPointerException();
        if (rootFacet == null) return null;
        return rootFacet.getKnob(c);
    }

    /**
     * Free any resources being used by this Message or any of its facets.
     */
    public void close() {
        try {
            if (rootFacet != null)
                rootFacet.close();
        } finally {
            rootFacet = null;
        }
    }

}
