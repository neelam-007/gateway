/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.common.xml.MessageNotSoapException;

import java.io.InputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

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
     * Any previously existing facets of this Message will be lost and replaced with a single MIME facet.
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
        rootFacet = new MimeFacet(this, sm, outerContentType, body);        
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
     * If this Message has the specified knob, then return it.  Will not attempt to create any facets
     * that haven't yet been installed, even assuming it might be possible to do so.
     *
     * @param c a Class derived from Knob.  Must be non-null.
     * @return the requested Knob, if its facet is installed on this Message, or null.
     */
    Knob getKnob(Class c) {
        if (c == null) throw new NullPointerException();
        if (rootFacet == null) return null;
        return rootFacet.getKnob(c);
    }
}
