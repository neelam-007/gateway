/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Aspect of Message that represents a SOAP envelope.
 */
public interface SoapKnob extends MessageKnob {
    /*
    Element getPayload();
    Iterator getHeaders();
    void addHeader(Element headerElement);
    */

    String getPayloadNamespaceUri() throws IOException, SAXException;

    /**
     * Check if this SOAP message is actually a SOAP fault.  If this returns true, fault details will be
     * available from {@link #getFaultDetail}.
     *
     * @return true if this SOAP message appears to be a SOAP fault.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
     * @throws com.l7tech.common.xml.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
     */
    boolean isFault() throws SAXException, IOException, InvalidDocumentFormatException;

    /**
     * Get the details for this SOAP fault, if {@link #isFault} returned true.
     *
     * @return the SOAP fault details.  Never null
     * @throws IllegalStateException if this Message is not a SOAP fault
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
     * @throws com.l7tech.common.xml.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
     */
    SoapFaultDetail getFaultDetail() throws SAXException, IOException, InvalidDocumentFormatException;

    /**
     * Turn this SOAP message into a SOAP fault containing the specified fault details.
     *
     * @param faultDetail  the fault details to use for the new document.  May not be null.  Must include an actor at this point.
     */
    void setFaultDetail(SoapFaultDetail faultDetail);
}
