/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds request messages for TokenService and helps parse the responses.
 */
public class TokenServiceClient {
    public static final Logger log = Logger.getLogger(TokenServiceClient.class.getName());
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

    public interface SecureConversationSession {
        String getSessionId();
        byte[] getSharedSecret();
        Date getExpiryDate();
    }

    public SecureConversationSession obtainSecureConversationSession(String ssgHostname,
                                                                     X509Certificate clientCertificate,
                                                                     PrivateKey clientPrivateKey)
            throws IOException, CertificateException, SAXException
    {
        URL url = new URL("http", ssgHostname, SecureSpanConstants.TOKEN_SERVICE_FILE);
        Document requestDoc = createRequestSecurityTokenMessage(clientCertificate, clientPrivateKey,
                                                                TOKENTYPE_SECURITYCONTEXT);
        log.log(Level.INFO, "Applying for new WS-SecureConversation SecurityContextToken for " + clientCertificate.getSubjectDN());

        URLConnection conn = url.openConnection();
        conn.setAllowUserInteraction(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty(XmlUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
        XmlUtil.nodeToOutputStream(requestDoc, conn.getOutputStream());
        if (!XmlUtil.TEXT_XML.equalsIgnoreCase(conn.getContentType()))
            throw new IOException("Token server returned unsupported content type " + conn.getContentType());
        Document response = XmlUtil.parse(conn.getInputStream());
        Object result = parseRequestSecurityTokenResponse(response,
                                                          clientCertificate,
                                                          clientPrivateKey);
        if (!(result instanceof SecureConversationSession))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SecureConversationSession)result;
    }


    public Object parseRequestSecurityTokenResponse(Document response,
                                                    X509Certificate clientCertificate,
                                                    PrivateKey clientPrivateKey)
    {
        // TODO parse response and build object
        return null;
    }
}
