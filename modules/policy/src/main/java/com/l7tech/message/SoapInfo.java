/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.message;

import javax.xml.namespace.QName;

/**
 * Represents information extracted from an XML document that can be used for future service resolution.
 * The same SoapInfo class is used for both software or hardware processing.
 */
public class SoapInfo {
    /**
     * Create a new SoapInfo.
     *
     * @param soap  true if message is soap
     * @param soapAction the SOAPAction value from the transport layer, if any.  May be null.
     * @param payloadNsUris  list of payload namespace URIs.  May be empty, but must not be null.  Must not contain nulls or empty strings.
     * @param hasSecurityNode true if this message is soap and contains at least one wsse:Security soap header.
     * @param envelopeNamespaceUri the envelope namespace URI, or null if this information is not known (or the message isn't SOAP).
     */
    public SoapInfo(boolean soap, String soapAction, QName[] payloadNsUris, boolean hasSecurityNode, String envelopeNamespaceUri) {
        if (payloadNsUris == null) throw new NullPointerException("payloadNsUris is required");
        this.soap = soap;
        this.soapAction = soapAction;
        this.payloadNames = payloadNsUris;
        this.hasSecurityNode = hasSecurityNode;
        this.envelopeNamespaceUri = envelopeNamespaceUri;
    }

    public boolean isSoap() {
        return soap;
    }

    /** @return payload namespace URIs.  Might be empty but never null.  If non-empty, will not contain nulls or empty strings. */
    public QName[] getPayloadNames() {
        return payloadNames;
    }

    public boolean isHasSecurityNode() {
        return hasSecurityNode;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public String getEnvelopeNamespaceUri() {
        return envelopeNamespaceUri;
    }

    final String soapAction;
    final boolean soap;
    final QName[] payloadNames;
    final boolean hasSecurityNode;
    final String envelopeNamespaceUri;
}
