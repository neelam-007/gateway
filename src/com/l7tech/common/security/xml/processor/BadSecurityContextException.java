/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.xml.SoapFaultDetail;
import org.w3c.dom.Element;

/**
 * Exception used to indicate that a referenced WS-SecureConversation security context was not found.
 */
public class BadSecurityContextException extends Exception implements SoapFaultDetail {
    private String id;
    private String faultactor = null;

    public BadSecurityContextException(String contextId) {
        super("The context " + contextId + " cannot be resolved.");
        id = contextId;
    }

    public String getFaultCode() {
        return SecureSpanConstants.FAULTCODE_BADCONTEXTTOKEN;
    }

    public String getFaultString() {
        return "The soap message referred to a secure conversation context that was not recognized. " + id;
    }

    public Element getFaultDetail() {
        return null;
    }

    public String getFaultActor() {
        return faultactor;
    }

    public String getFaultActor(String defaultActor) {
        if (faultactor == null)
            faultactor = defaultActor;
        return faultactor;
    }

    public void setFaultActor(String faultActor) {
        this.faultactor = faultActor;
    }
}
