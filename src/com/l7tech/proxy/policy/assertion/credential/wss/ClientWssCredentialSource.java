/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.AxisFault;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.policy.assertion.ClientAssertion;

import javax.xml.soap.SOAPException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientWssCredentialSource implements ClientAssertion {
    /**
     * gets the security element out of header. creates one if necessary
     * @param request
     * @return
     */
    protected SOAPHeaderElement getSecurityElement(PendingRequest request) throws SOAPException {
        SOAPHeaderElement secHeader = null;
        try {
            secHeader = request.getSoapEnvelope().getHeaderByName(SECURITY_NAMESPACE, SECURITY_NAME);
        } catch (AxisFault e) {
            throw new SOAPException(e);
        }
        if (secHeader == null) {
            secHeader = new SOAPHeaderElement(SECURITY_NAMESPACE, SECURITY_NAME);
            request.getSoapEnvelope().addHeader(secHeader);
        }
        return secHeader;
    }

    protected static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/04/secext";
    protected static final String SECURITY_NAME = "Security";
}
