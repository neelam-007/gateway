/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.io.IOException;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.security.xml.WssDecoratorImpl;

import javax.xml.soap.SOAPConstants;

/**
 * Builds request messages for the PolicyService and helps parse the responses.
 */
public class PolicyServiceClient {


    public static Document createGetPolicyRequest(String serviceId,
                                                  X509Certificate clientCert,
                                                  PrivateKey clientKey)
            throws GeneralSecurityException
    {
        Document msg = createGetPolicyRequest(serviceId);
        WssDecorator decorator = new WssDecoratorImpl();
        WssDecorator.DecorationRequirements req = new WssDecorator.DecorationRequirements();
        req.setSenderCertificate(clientCert);
        req.setSenderPrivateKey(clientKey);
        req.setSignTimestamp(true);
        try {
            Element header = SoapUtil.getHeaderElement(msg);
            Element sid = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                SoapUtil.L7_SERVICEID_ELEMENT);
            req.getElementsToSign().add(sid);
            decorator.decorateMessage(msg, req);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (WssDecorator.DecoratorException e) {
            throw new RuntimeException(e); // shouldn't happen
        }
        return msg;
    }

    public static Document createGetPolicyRequest(String serviceId) {
        Document msg;
        try {
            msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                           "<soap:Header>" +
                                           "<L7a:" + SoapUtil.L7_SERVICEID_ELEMENT + " " +
                                           "xmlns:L7a=\"" + SoapUtil.L7_MESSAGEID_NAMESPACE + "\"/>" +
                                           "</soap:Header>" +
                                           "<soap:Body>" +
                                           "<wsx:GetPolicy xmlns:wsx=\"" + SoapUtil.WSX_NAMESPACE + "\"/>" +
                                           "</soap:Body></soap:Envelope>");
            Element header = SoapUtil.getHeaderElement(msg);
            Element sid = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                SoapUtil.L7_SERVICEID_ELEMENT);
            sid.appendChild(msg.createTextNode(serviceId));

        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
        return msg;
    }
}
