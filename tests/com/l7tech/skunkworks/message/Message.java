/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.common.xml.MessageNotSoapException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an abstract Message in the system.  This can be a request or a reply; over HTTP or JMS or transport
 * not yet determined; using SOAP, MIME, XML, or not yet set.  Any message at all.
 */
public final class Message {
    private MessageFacet rootFacet;

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
        attachInputStream(sm, outerContentType, body);
    }

    /**
     * Initialize, or re-initialize, a Message with a MIME facet attached to the specified InputStream.
     * With the exception of {@link HttpRequestKnob} and {@link HttpResponseKnob}s, which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param sm  the StashManager to use for stashing MIME parts temporarily.  Must not be null.
     * @param outerContentType  the content type of the body InputStream.  Must not be null.
     * @param body an InputStream positioned at the first byte of body content for this Message.
     * @throws NoSuchPartException if the message is multipart/related but contains no initial boundary
     * @throws IOException if there is a problem reading the initial boundary from a multipart/related body
     */
    public void attachInputStream(StashManager sm, 
                                  ContentTypeHeader outerContentType, 
                                  InputStream body) 
            throws NoSuchPartException, IOException 
    {
        HttpRequestKnob reqKnob = (HttpRequestKnob)getKnob(HttpRequestKnob.class);
        HttpResponseKnob respKnob = (HttpResponseKnob)getKnob(HttpResponseKnob.class);
        rootFacet = new MimeFacet(this, sm, outerContentType, body);
        if (reqKnob != null) attachHttpRequestKnob(reqKnob);
        if (respKnob != null) attachHttpResponseKnob(respKnob);
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
     * Get the knob for the SOAP facet of this Message, creating one if necessary and possible.
     * If no SOAP facet is currently installed, one will be created if an XML facet can be created
     * and the message appears to be SOAP.
     *
     * @return the SoapKnob for this Message.  Never null.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
     * @throws MessageNotSoapException if there is an XML document but it doesn't look like a valid SOAP envelope
     */
    public SoapKnob getSoapKnob() throws SAXException, IOException, MessageNotSoapException {
        SoapKnob soapKnob = (SoapKnob)getKnob(SoapKnob.class);
        if (soapKnob == null) {
            getXmlKnob(); // guarantee XML facet exists somewhere
            rootFacet = new SoapFacet(this, rootFacet);
            soapKnob = (SoapKnob)getKnob(SoapKnob.class);
            if (soapKnob == null) throw new IllegalStateException(); // can't happen, we just made one
        }
        return soapKnob;
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

    public void close() {
        try {
            if (rootFacet != null)
                rootFacet.close();
        } finally {
            rootFacet = null;
        }
    }
}
