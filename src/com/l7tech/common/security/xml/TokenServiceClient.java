/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.ParseException;

/**
 * Builds request messages for TokenService and helps parse the responses.
 */
public class TokenServiceClient {
    public static final Logger log = Logger.getLogger(TokenServiceClient.class.getName());
    public static final String TOKENTYPE_SECURITYCONTEXT = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";
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
                                                                     PrivateKey clientPrivateKey,
                                                                     X509Certificate serverCertificate)
            throws IOException, GeneralSecurityException, InvalidDocumentFormatException, WssProcessor.ProcessorException
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
        Document response = null;
        try {
            response = XmlUtil.parse(conn.getInputStream());
        } catch (SAXException e) {
            throw new InvalidDocumentFormatException("Unable to parse RequestSecurityTokenResponse", e);
        }
        Object result = parseRequestSecurityTokenResponse(response,
                                                          clientCertificate,
                                                          clientPrivateKey,
                                                          serverCertificate);
        if (!(result instanceof SecureConversationSession))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SecureConversationSession)result;
    }


    public Object parseRequestSecurityTokenResponse(Document response,
                                                    X509Certificate clientCertificate,
                                                    PrivateKey clientPrivateKey,
                                                    X509Certificate serverCertificate)
            throws InvalidDocumentFormatException, GeneralSecurityException, WssProcessor.ProcessorException
    {
        WssProcessor wssProcessor = new WssProcessorImpl();
        WssProcessor.ProcessorResult result = wssProcessor.undecorateMessage(response,
                                                                             clientCertificate,
                                                                             clientPrivateKey,
                                                                             null);
        response = result.getUndecoratedMessage();
        Element env = response.getDocumentElement();
        if (env == null) throw new InvalidDocumentFormatException("Response had no document element"); // can't happen
        Element body = XmlUtil.findOnlyOneChildElementByName(env, env.getNamespaceURI(), "Body");
        if (body == null) throw new MessageNotSoapException("Response has no SOAP Body");
        Element rstr = XmlUtil.findOnlyOneChildElementByName(body, SoapUtil.WST_NAMESPACE, "RequestSecurityTokenResponse");
        if (rstr == null) throw new InvalidDocumentFormatException("Response body does not contain wst:RequestSecurityTokenResponse");

        WssProcessor.SignedElement[] signedElements = result.getElementsThatWereSigned();
        WssProcessor.SecurityToken signingSecurityToken = null;
        for (int i = 0; i < signedElements.length; i++) {
            WssProcessor.SignedElement signedElement = signedElements[i];
            if (XmlUtil.isElementAncestor(rstr, signedElement.asElement())) {
                if (signingSecurityToken != null)
                    throw new InvalidDocumentFormatException("Response body was signed with more than one security token");
                signingSecurityToken = signedElement.getSigningSecurityToken();
                if (!(signingSecurityToken instanceof WssProcessor.X509SecurityToken))
                    throw new InvalidDocumentFormatException("Response body was signed, but not with an X509 Security Token");
                WssProcessor.X509SecurityToken x509Token = null;
                x509Token = (WssProcessor.X509SecurityToken)signingSecurityToken;
                X509Certificate signingCert = x509Token.asX509Certificate();
                byte[] signingPublicKeyBytes = signingCert.getPublicKey().getEncoded();
                byte[] desiredPublicKeyBytes = serverCertificate.getPublicKey().getEncoded();
                if (!Arrays.equals(signingPublicKeyBytes, desiredPublicKeyBytes))
                    throw new InvalidDocumentFormatException("Response body was signed with an X509 certificate, but it wasn't the server certificate.");
            }
        }

        if (signingSecurityToken == null)
            throw new InvalidDocumentFormatException("Response body was not signed.");

        Element rst = XmlUtil.findOnlyOneChildElementByName(rstr, SoapUtil.WST_NAMESPACE, "RequestedSecurityToken");
        if (rst == null) throw new InvalidDocumentFormatException("Response contained no RequestedSecurityToken");

        // Extract session ID
        Element scTokenEl = XmlUtil.findOnlyOneChildElementByName(rst, SoapUtil.WSSC_NAMESPACE, "SecurityContextToken");
        if (scTokenEl == null) throw new InvalidDocumentFormatException("Response contained no wsc:SecurityContextToken");
        Element identifierEl = XmlUtil.findOnlyOneChildElementByName(scTokenEl, SoapUtil.WSSC_NAMESPACE, "Identifier");
        if (identifierEl == null) throw new InvalidDocumentFormatException("Response contained no wsc:Identifier");
        String identifier = XmlUtil.getTextValue(identifierEl).trim();
        if (identifier == null || identifier.length() < 4) throw new InvalidDocumentFormatException("Response wsc:Identifier was empty or too short");

        // Extract optional expiry date
        Element lifeTimeEl = XmlUtil.findOnlyOneChildElementByName(rstr, SoapUtil.WST_NAMESPACE, "Lifetime");
        Date expires = null;
        if (lifeTimeEl != null) {
            Element expiresEl = XmlUtil.findOnlyOneChildElementByName(lifeTimeEl, SoapUtil.WSU_URIS_ARRAY, "Expires");
            if (expiresEl != null) {
                String expiresStr = XmlUtil.getTextValue(expiresEl).trim();
                if (expiresStr != null && expiresStr.length() > 3) {
                    try {
                        expires = ISO8601Date.parse(expiresStr);
                    } catch (ParseException e) {
                        throw new InvalidDocumentFormatException("Response contained an invalid IDO 8601 Expires date", e);
                    }
                }
            }
        }

        // Extract shared secret
        Element rpt = XmlUtil.findOnlyOneChildElementByName(rstr, SoapUtil.WST_NAMESPACE, "RequestedProofToken");
        if (rpt == null) throw new InvalidDocumentFormatException("Response contained no RequestedProofToken");

        Element encryptedKey = XmlUtil.findOnlyOneChildElementByName(rpt, SoapUtil.XMLENC_NS, "EncryptedKey");
        if (encryptedKey == null) throw new InvalidDocumentFormatException("Response RequestedProofToken did not contain an EncryptedKey");

        // If there's a KeyIdentifier, log whether it's talking about our key
        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        XencUtil.checkKeyInfo(encryptedKey, clientCertificate);

        // verify that the algo is supported
        XencUtil.checkEncryptionMethod(encryptedKey);

        // Extract the encrypted key
        final byte[] unencryptedKey = XencUtil.decryptKey(encryptedKey, clientPrivateKey);

        final Date finalExpires = expires;
        final String finalIdentifier = identifier;
        return new SecureConversationSession() {
            public String getSessionId() {
                return finalIdentifier;
            }

            public byte[] getSharedSecret() {
                return unencryptedKey;
            }

            public Date getExpiryDate() {
                return finalExpires;
            }
        };
    }
}
