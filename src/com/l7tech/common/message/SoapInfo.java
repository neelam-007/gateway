/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.xml.tarari.TarariMessageContext;

/**
 * Represents information extracted from a SOAP document that can be used for future service resolution.
 * The same SoapInfo class is used for both software or hardware processing.
 */
public class SoapInfo {
    protected SoapInfo(String payloadNsUri, boolean hasSecurityNode) {
        this.payloadNsUri = payloadNsUri;
        this.hasSecurityNode = hasSecurityNode;
    }

    /**
     * @return the TarariMessageContext for this message, or null if there is none.
     */
    protected TarariMessageContext getContext() {
        return null;
    }

    final String payloadNsUri;
    final boolean hasSecurityNode;
}
