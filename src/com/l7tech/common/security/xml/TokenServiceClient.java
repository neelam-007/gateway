/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.io.IOException;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;

import javax.xml.soap.SOAPConstants;

/**
 * Builds request messages for TokenService and helps parse the responses.
 */
public class TokenServiceClient {
    public static final String TOKENTYPE_SECURITYCONTEXT = "http://schemas.xmlsoap.org/ws/2004/04/sct";
    public static final String TOKENTYPE_SAML = "urn:oasis:names:tc:SAML:1.0:assertion";

    /**
     * Create a signed SOAP message containing a WS-Trust RequestSecurityToken message asking for the
     * specified token type.
     *
     * @param clientCertificate  the certificate to use to sign the request
     * @param clientPrivateKey   the private key of the certificate to use to sign the request
     * @param desiredTokenType   the token type being applied for
     * @return a signed SOAP message containing a wst:RequestSecurityToken
     * @throws CertificateException
     */
    public Document createRequestSecurityTokenMessage(X509Certificate clientCertificate,
                                                      PrivateKey clientPrivateKey,
                                                      String desiredTokenType)
            throws CertificateException
    {
        try {
            Document msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                                    "<soap:Body>" +
                                                    "<wst:RequestSecurityToken xmlns:wst=\"" + SoapUtil.WST_NAMESPACE + "\">" +
                                                    "<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>" +
                                                    "</wst:RequestSecurityToken>" +
                                                    "</soap:Body></soap:Envelope>");
            Element env = msg.getDocumentElement();
            Element body = XmlUtil.findFirstChildElement(env);
            Element rst = XmlUtil.findFirstChildElement(body);
            Element tokenType = XmlUtil.createAndPrependElementNS(rst, "TokenType", SoapUtil.WST_NAMESPACE, "wst");
            tokenType.appendChild(msg.createTextNode(desiredTokenType));

            // Sign it
            WssDecorator wssDecorator = new WssDecoratorImpl();
            WssDecorator.DecorationRequirements req = new WssDecorator.DecorationRequirements();
            req.setSignTimestamp(true);
            req.setSenderCertificate(clientCertificate);
            req.setSenderPrivateKey(clientPrivateKey);
            req.getElementsToSign().add(body);
            wssDecorator.decorateMessage(msg, req);

            return msg;
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (CertificateException e) {
            throw e;  // invalid certificate
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e); // shouldn't happen
        } catch (WssDecorator.DecoratorException e) {
            throw new RuntimeException(e); // shouldn't happen
        }
    }
}
