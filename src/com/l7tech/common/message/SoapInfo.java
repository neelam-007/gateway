/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

/**
 * Represents information extracted from a SOAP document that can be used for future service resolution.
 * The same SoapInfo class is used for both software or hardware processing.
 */
public class SoapInfo {
    public SoapInfo(String payloadNsUri, boolean hasSecurityNode) {
        this.payloadNsUri = payloadNsUri;
        this.hasSecurityNode = hasSecurityNode;
    }

    final String payloadNsUri;
    final boolean hasSecurityNode;
}
