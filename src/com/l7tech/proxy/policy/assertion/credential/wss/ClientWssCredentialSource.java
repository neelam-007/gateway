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

    // todo, move this to util class
    protected Element getOrMakeHeader(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, HEADER_EL_NAME);
        if (list.getLength() < 1) {
            String soapEnvNamespacePrefix = soapMsg.getDocumentElement().getPrefix();
            Element header = soapMsg.createElementNS(soapEnvNS, HEADER_EL_NAME);
            Element body = getOrMakeBody(soapMsg);
            header.setPrefix(soapEnvNamespacePrefix);
            soapMsg.getDocumentElement().insertBefore(header, body);
            return header;
        }
        else return (Element)list.item(0);
    }

    // todo, move this to util class
    protected Element getOrMakeBody(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, BODY_EL_NAME);
        if (list.getLength() < 1) {
            String soapEnvNamespacePrefix = soapMsg.getDocumentElement().getPrefix();
            Element body = soapMsg.createElementNS(soapEnvNS, BODY_EL_NAME);
            body.setPrefix(soapEnvNamespacePrefix);
            soapMsg.getDocumentElement().appendChild(body);
            return body;
        }
        else return (Element)list.item(0);
    }

    protected static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/04/secext";
    protected static final String SECURITY_NAME = "Security";

    private static final String HEADER_EL_NAME = "Header";
    private static final String BODY_EL_NAME = "Body";
}
