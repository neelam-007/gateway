/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.apache.axis.message.SOAPHeaderElement;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientWssCredentialSource implements ClientAssertion {
    protected static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/04/secext";
    protected static final String SECURITY_NAME = "Security";
}
