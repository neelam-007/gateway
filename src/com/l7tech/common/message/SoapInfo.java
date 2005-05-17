/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.message;

import com.l7tech.common.xml.tarari.TarariMessageContext;

/**
 * Represents information extracted from an XML document that can be used for future service resolution.
 * The same SoapInfo class is used for both software or hardware processing.
 */
public class SoapInfo {
    protected SoapInfo(boolean soap, String payloadNsUri, boolean hasSecurityNode) {
        this.soap = soap;
        this.payloadNsUri = payloadNsUri;
        this.hasSecurityNode = hasSecurityNode;
    }

    /**
     * @return the TarariMessageContext for this message, or null if there is none.
     */
    protected TarariMessageContext getContext() {
        return null;
    }

    public boolean isSoap() {
        return soap;
    }

    public String getPayloadNsUri() {
        return payloadNsUri;
    }

    public boolean isHasSecurityNode() {
        return hasSecurityNode;
    }

    final boolean soap;
    final String payloadNsUri;
    final boolean hasSecurityNode;
}
