/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.message;

/**
 * Represents information extracted from an XML document that can be used for future service resolution.
 * The same SoapInfo class is used for both software or hardware processing.
 */
public class SoapInfo {
    /**
     * Create a new SoapInfo.
     *
     * @param soap  true if message is soap
     * @param payloadNsUris  list of payload namespace URIs.  May be empty, but must not be null.  Must not contain nulls or empty strings.
     * @param hasSecurityNode true if this message is soap and contains at least one wsse:Security soap header.
     */
    public SoapInfo(boolean soap, String[] payloadNsUris, boolean hasSecurityNode) {
        this.soap = soap;
        this.payloadNsUris = payloadNsUris;
        this.hasSecurityNode = hasSecurityNode;
    }

    public boolean isSoap() {
        return soap;
    }

    /** @return payload namespace URIs.  Might be empty but never null.  If non-empty, will not contain nulls or empty strings. */
    public String[] getPayloadNsUris() {
        return payloadNsUris;
    }

    public boolean isHasSecurityNode() {
        return hasSecurityNode;
    }

    final boolean soap;
    final String[] payloadNsUris;
    final boolean hasSecurityNode;
}
