/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.xml.SoapFaultDetail;

/**
 * @author mike
 */
public class BadSecurityContextException extends Exception implements SoapFaultDetail {
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
    public String getFaultDetails() {
        return getFaultString();
    }
    private String id;
}
