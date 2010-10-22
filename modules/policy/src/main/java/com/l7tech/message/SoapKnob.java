/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.soap.SoapVersion;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
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

    /**
     * Get the payload namespace URIs for this soap document.
     *
     * @return an array of namespace URIs.  May contain duplicates.  Never null or empty.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     */
    QName[] getPayloadNames() throws IOException, SAXException, NoSuchPartException;

    /**
     * Check if this SOAP message is actually a SOAP fault.  If this returns true, fault details will be
     * available from {@link #getFaultDetail}.
     *
     * @return true if this SOAP message appears to be a SOAP fault.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
     * @throws com.l7tech.util.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
     */
    boolean isFault() throws SAXException, IOException, InvalidDocumentFormatException;

    /**
     * Get the details for this SOAP fault, if {@link #isFault} returned true.
     *
     * @return the SOAP fault details.  Never null
     * @throws IllegalStateException if this Message is not a SOAP fault
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws com.l7tech.util.InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
     * @throws com.l7tech.util.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
     */
    SoapFaultDetail getFaultDetail() throws SAXException, IOException, InvalidDocumentFormatException;

    /**
     * Turn this SOAP message into a SOAP fault containing the specified fault details.
     *
     * @param faultDetail  the fault details to use for the new document.  May not be null.  Must include an actor at this point.
     */
    void setFaultDetail(SoapFaultDetail faultDetail);

    /**
     * True if a Security header was noticed.
     */
    boolean isSecurityHeaderPresent() throws NoSuchPartException, IOException, SAXException;

    /**
     * Notify this SoapKnob that its caches need to be cleared
     */
    void invalidate();

    /**
     * @return the SOAP version of this message.  Never null, but may be SoapVersion.UNKNOWN if the SOAP envelope namespace URI
     *         is not one of the standard URIs for SOAP 1.1 or SOAP 1.2.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if there is a problem reading from or writing to a stash
     */
    SoapVersion getSoapVersion() throws IOException, SAXException;

    /**
     * @return the namespace URI of the document element of this message.  Never null, and normally can never be empty
     *         (unless the system is somehow configured to accept "" as a SOAP namespae URI).
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if there is a problem reading from or writing to a stash
     */
    String getSoapEnvelopeUri() throws IOException, SAXException;
}
