/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds request messages for TokenService and helps parse the responses.
 */
public class TokenServiceClient {
    public static final Logger log = Logger.getLogger(TokenServiceClient.class.getName());

    public static final class RequestType {
        public static final RequestType ISSUE = new RequestType("http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue");
        public static final RequestType VALIDATE = new RequestType("http://schemas.xmlsoap.org/ws/2004/04/security/trust/Validate");
                                        
        private final String uri;
        private RequestType(String uri) { this.uri = uri; }
        String getUri() { return uri; }
    }
    
    /**
     * Create a signed SOAP message containing a WS-Trust RequestSecurityToken message asking for the
     * specified token type.
     *
     * @param clientCertificate  the certificate to use to sign the request
     * @param clientPrivateKey   the private key of the certificate to use to sign the request
     * @param desiredTokenType   the token type being applied for
     * @param base
     * @param appliesToAddress   wsa:Address to use for wsp:AppliesTo, or null to leave out the AppliesTo
     * @return a signed SOAP message containing a wst:RequestSecurityToken
     * @throws CertificateException if ther eis a problem with the clientCertificate
     */
    public static Document createRequestSecurityTokenMessage(X509Certificate clientCertificate,
                                                             PrivateKey clientPrivateKey,
                                                             SecurityTokenType desiredTokenType,
                                                             RequestType requestType,
                                                             SecurityToken base,
                                                             String appliesToAddress,
                                                             Date timestampCreatedDate)
            throws CertificateException
    {
        try {
            Document msg = requestSecurityTokenMessageTemplate(desiredTokenType, requestType, appliesToAddress, base);
            Element env = msg.getDocumentElement();
            Element body = XmlUtil.findFirstChildElement(env);

            // Sign it
            WssDecorator wssDecorator = new WssDecoratorImpl();
            DecorationRequirements req = new DecorationRequirements();
            req.setSignTimestamp(true);
            req.setSenderCertificate(clientCertificate);
            req.setSenderPrivateKey(clientPrivateKey);
            req.getElementsToSign().add(body);
            req.setTimestampCreatedDate(timestampCreatedDate);
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
        } catch (DecoratorException e) {
            throw new RuntimeException(e); // shouldn't happen
        }
    }

    private static Document requestSecurityTokenMessageTemplate(SecurityTokenType desiredTokenType,
                                                                RequestType requestType,
                                                                String appliesToAddress,
                                                                SecurityToken base)
            throws IOException, SAXException 
    {
        // TODO fix or remove this hack: if a saml: qname will be used, declare saml NS in root element
        String extraNs = "";
        if (desiredTokenType != null && SamlSecurityToken.class.isAssignableFrom(desiredTokenType.getInterfaceClass()))
            extraNs += " xmlns:saml=\"" + desiredTokenType.getWstPrototypeElementNs() + "\"";

        Document msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\"" + extraNs + ">" +
                                                    "<soap:Body>" +
                                                    "<wst:RequestSecurityToken xmlns:wst=\"" + SoapUtil.WST_NAMESPACE + "\">" +
                                                    "</wst:RequestSecurityToken>" +
                                                    "</soap:Body></soap:Envelope>");
        Element env = msg.getDocumentElement();
        Element body = XmlUtil.findFirstChildElement(env);
        Element rst = XmlUtil.findFirstChildElement(body);

        // Add AppliesTo, if provided
        if (appliesToAddress != null && appliesToAddress.length() > 0) {
            Element appliesTo = XmlUtil.createAndAppendElementNS(rst, "AppliesTo", SoapUtil.WSP_NAMESPACE, "wsp");
            Element endpointRef = XmlUtil.createAndAppendElementNS(appliesTo, "EndpointReference", SoapUtil.WSA_NAMESPACE, "wsa");
            Element address = XmlUtil.createAndAppendElementNS(endpointRef, "Address", SoapUtil.WSA_NAMESPACE, "wsa");
            address.appendChild(XmlUtil.createTextNode(address, appliesToAddress));
        }

        // Add TokenType, if meaningful with this token type
        if (desiredTokenType != null) {
            final String tokenTypeUri = desiredTokenType.getWstTokenTypeUri();
            if (tokenTypeUri != null) {
                // Add TokenType element
                Element tokenType = XmlUtil.createAndPrependElementNS(rst, "TokenType", SoapUtil.WST_NAMESPACE, "wst");
                tokenType.appendChild(XmlUtil.createTextNode(msg, tokenTypeUri));
            }
        }

        // Add Base, if provided.  Base is not required to be the same token type as the token type we are requesting.
        if (base != null) {
            Element baseEl = XmlUtil.createAndPrependElementNS(rst, "Base", SoapUtil.WST_NAMESPACE, "wst");
            baseEl.appendChild(msg.importNode(base.asElement(), true));
        }

        // Add RequestType
        {
            Element rt = XmlUtil.createAndAppendElementNS(rst, "RequestType", rst.getNamespaceURI(), "wst");
            rt.appendChild(XmlUtil.createTextNode(msg, requestType.getUri()));
        }

        return msg;
    }

    public static Document createRequestSecurityTokenIssueMessage(SecurityTokenType desiredTokenType, RequestType requestType, SecurityToken base, String appliesToAddress) {
        try {
            return requestSecurityTokenMessageTemplate(desiredTokenType, requestType, appliesToAddress, base);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public interface SecureConversationSession {
        String getSessionId();
        byte[] getSharedSecret();
        Date getExpiryDate();
    }

    /**
     * Requests a SecureConversation context token. The request is authenticated using an xml digital signature.
     */
    public static SecureConversationSession obtainSecureConversationSession(Ssg ssg,
                                                                            X509Certificate clientCertificate,
                                                                            PrivateKey clientPrivateKey,
                                                                            X509Certificate serverCertificate)
            throws IOException, GeneralSecurityException
    {
        URL url = new URL("http", ssg.getSsgAddress(), ssg.getSsgPort(), SecureSpanConstants.TOKEN_SERVICE_FILE);
        Date timestampCreatedDate = ssg.dateTranslatorToSsg().translate(new Date());
        Document requestDoc = createRequestSecurityTokenMessage(clientCertificate, clientPrivateKey,
                                                                SecurityTokenType.WSSC, RequestType.ISSUE, null, null, timestampCreatedDate);
        Object result = obtainResponse(clientCertificate, url, ssg, requestDoc, clientPrivateKey, serverCertificate);

        if (!(result instanceof SecureConversationSession))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SecureConversationSession)result;
    }

    /**
     * Requests a SecureConversation context token. The request is transport-secured (ssl) and transport authenticated.
     */
    public static SecureConversationSession obtainSecureConversationSession(Ssg ssg, X509Certificate serverCertificate)
            throws IOException, GeneralSecurityException, OperationCanceledException {
        URL url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), SecureSpanConstants.TOKEN_SERVICE_FILE);

        Document requestDoc = createRequestSecurityTokenIssueMessage(SecurityTokenType.WSSC, RequestType.ISSUE, null, null);
        Object result = obtainResponse(url, ssg, requestDoc, serverCertificate);

        if (!(result instanceof SecureConversationSession))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SecureConversationSession)result;
    }
    
    public static SamlAssertion obtainSamlAssertion(Ssg ssg,
                                                    X509Certificate clientCertificate,
                                                    PrivateKey clientPrivateKey,
                                                    X509Certificate serverCertificate, 
                                                    RequestType requestType)
            throws IOException, GeneralSecurityException
    {
        URL url = new URL("http", ssg.getSsgAddress(), ssg.getSsgPort(), SecureSpanConstants.TOKEN_SERVICE_FILE);
        Date timestampCreatedDate = ssg.dateTranslatorToSsg().translate(new Date());
        Document requestDoc = createRequestSecurityTokenMessage(clientCertificate, clientPrivateKey,
                                                                SecurityTokenType.SAML_AUTHENTICATION,
                                                                requestType,
                                                                null, null, timestampCreatedDate);
        requestDoc.getDocumentElement().setAttribute("xmlns:saml", SamlConstants.NS_SAML);
        Object result = obtainResponse(clientCertificate, url, ssg, requestDoc, clientPrivateKey, serverCertificate);

        if (!(result instanceof SamlAssertion))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SamlAssertion)result;
    }

    private static Object obtainResponse(X509Certificate clientCertificate,
                                         URL url,
                                         Ssg ssg,
                                         Document requestDoc,
                                         PrivateKey clientPrivateKey,
                                         X509Certificate serverCertificate)
            throws IOException, GeneralSecurityException
    {
        try {
            log.log(Level.INFO, "Applying for new Security Token for " + clientCertificate.getSubjectDN() +
                                " with token server " + url.toString());

            CurrentRequest.setPeerSsg(ssg);
            URLConnection conn = url.openConnection();
            CurrentRequest.setPeerSsg(null);
            conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setRequestProperty(MimeUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
            XmlUtil.nodeToOutputStream(requestDoc, conn.getOutputStream());
            int len = conn.getContentLength();
            log.log(Level.FINEST, "Token server response content length=" + len);
            String contentType = conn.getContentType();
            if (contentType == null || contentType.indexOf(XmlUtil.TEXT_XML) < 0)
                throw new IOException("Token server returned unsupported content type " + conn.getContentType());
            Document response = null;
            response = XmlUtil.parse(conn.getInputStream());
            Object result = null;
            result = parseSignedRequestSecurityTokenResponse(response,
                                                       clientCertificate,
                                                       clientPrivateKey,
                                                       serverCertificate);
            return result;
        } catch (SAXException e) {
            throw new CausedIOException("Unable to parse RequestSecurityTokenResponse from security token service: " + e.getMessage(), e);
        } catch (InvalidDocumentFormatException e) {
            throw new CausedIOException("Unable to process response from security token service: " + e.getMessage(), e);
        } catch (ProcessorException e) {
            throw new CausedIOException("Unable to obtain a token from the security token server: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CausedIOException("Unable to obtain a token from the security token server: " + e.getMessage(), e);
        }
    }

    /**
     * Get a response from the TokenService over https
     * @param url must be https
     */
    private static Object obtainResponse(URL url, Ssg ssg, Document requestDoc, X509Certificate serverCertificate)
                                                                      throws IOException, GeneralSecurityException,
                                                                             OperationCanceledException {
        HttpsURLConnection sslConn = null;
        try {
            log.log(Level.INFO, "Applying for new Security Token with token server " + url.toString());

            CurrentRequest.setPeerSsg(ssg);
            // enforce https and provide password credentials
            PasswordAuthentication pw = Managers.getCredentialManager().getCredentials(ssg);
            String encodedbasicauthvalue = HexUtils.encodeBase64((pw.getUserName() + ":" +
                                                                  new String(pw.getPassword())).getBytes());
            // here, we must use the special
            CurrentRequest.setPeerSsg(ssg);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Authorization", "Basic " + encodedbasicauthvalue);
            if (conn instanceof HttpsURLConnection) {
                sslConn = (HttpsURLConnection)conn;
                sslConn.setSSLSocketFactory(ClientProxySecureProtocolSocketFactory.getInstance());
            } else {
                throw new GeneralSecurityException("Cannot send this request over insecure channel.");
            }
            conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setRequestProperty(MimeUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
            XmlUtil.nodeToOutputStream(requestDoc, conn.getOutputStream());
            int len = conn.getContentLength();
            log.log(Level.FINEST, "Token server response content length=" + len);
            String contentType = conn.getContentType();
            if (contentType == null || contentType.indexOf(XmlUtil.TEXT_XML) < 0)
                throw new IOException("Token server returned unsupported content type " + conn.getContentType());
            Document response = null;
            response = XmlUtil.parse(conn.getInputStream());
            Object result = null;
            result = parseSignedRequestSecurityTokenResponse(response, null, null, serverCertificate);
            return result;
        } catch (SAXException e) {
            throw new CausedIOException("Unable to parse RequestSecurityTokenResponse from security token service: " + e.getMessage(), e);
        } catch (InvalidDocumentFormatException e) {
            throw new CausedIOException("Unable to process response from security token service: " + e.getMessage(), e);
        } catch (ProcessorException e) {
            throw new CausedIOException("Unable to obtain a token from the security token server: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CausedIOException("Unable to obtain a token from the security token server: " + e.getMessage(), e);
        } finally {
            sslConn.disconnect();
            sslConn = null;
        }
    }

    /**
     * Parse an unsigned RequestSecurityTokenResponse returned from a WS-Trust token service.  The response
     * message is required to be in SOAP format, but is not required to be signed.  It is the caller's responsibility
     * to ensure, perhaps using SSL, that the message arrived from the expected token issuer with its integrity intact.
     *
     * @param response the response to process.  Must be a full SOAP Envelope.
     *                 Body must contain a RequestSecurityTokenResponse.  May not be null.
     * @return an Object representing the parsed security token.  At the moment this will be an instance of either
     *         SamlAssertion or SecureConversationSession.  Never null; will either succeed or throw.
     */
    public static Object parseUnsignedRequestSecurityTokenResponse(Document response)
            throws InvalidDocumentFormatException
    {
        try {
            return parseRequestSecurityTokenResponse(response, null, null, null);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e); // can't happen
        } catch (ProcessorException e) {
            throw new RuntimeException(e); // can't happen, server cert was null
        }
    }

    /**
     * Parse a signed RequestSecurityTokenResponse returned from a WS-Trust token service.  The response message
     * is requried to be in SOAP format, to be a Basic Security Profile SECURE_ENVELOPE signed by the
     * issuer certificate, and to have a signed SOAP Body.
     * <p>
     * The response signature will be verified, and checked to ensure that it was signed with the private key
     * corresponding to the public key in the passed in serverCertificate.
     *
     * @param response    the response to process.  Must be a full SOAP Envelope, complete with Security header and signed Body.
     *                    Body must contain a RequestSecurityTokenResponse.  May not be null.
     * @param clientCertificate    the client certificate that was used to apply for this security token.  May not be null.
     * @param clientPrivateKey     the private key corresponding to this client certificate, to decrypt any EncryptedKey.  May not be null.
     * @param serverCertificate    the certificate of the token issuer, to verify the identity of the signer of the returned token.  May not be null.
     * @return an Object representing the parsed security token.  At the moment this will be an instance of either
     *         SamlAssertion or SecureConversationSession.  Never returns null; will either succeed or throw.
     * @throws InvalidDocumentFormatException  if there is a problem with the format of the response document
     * @throws GeneralSecurityException  if there is a problem with a certificate, key, or signature
     * @throws com.l7tech.common.security.xml.processor.ProcessorException   if there is a problem undecorating the signed message
     */
    public static Object parseSignedRequestSecurityTokenResponse(Document response,
                                                           X509Certificate clientCertificate,
                                                           PrivateKey clientPrivateKey,
                                                           X509Certificate serverCertificate)
            throws InvalidDocumentFormatException, GeneralSecurityException, ProcessorException
    {
        if (clientCertificate == null || clientPrivateKey == null || serverCertificate == null) throw new NullPointerException();
        return parseRequestSecurityTokenResponse(response, clientCertificate, clientPrivateKey, serverCertificate);
    }

    private static Object parseRequestSecurityTokenResponse(Document response,
                                                            X509Certificate clientCertificate,
                                                            PrivateKey clientPrivateKey,
                                                            X509Certificate serverCertificate)
            throws InvalidDocumentFormatException, GeneralSecurityException, ProcessorException
    {
        Element env = response.getDocumentElement();
        if (env == null) throw new InvalidDocumentFormatException("Response had no document element"); // can't happen
        Element body = XmlUtil.findOnlyOneChildElementByName(env, env.getNamespaceURI(), "Body");
        if (body == null) throw new MessageNotSoapException("Response has no SOAP Body");
        Element rstr = XmlUtil.findOnlyOneChildElementByName(body, SoapUtil.WST_NAMESPACE, "RequestSecurityTokenResponse");
        if (rstr == null) throw new InvalidDocumentFormatException("Response body does not contain wst:RequestSecurityTokenResponse");

        if (serverCertificate != null)
            verifySignature(rstr, serverCertificate, response, clientCertificate, clientPrivateKey);

        Element rst = XmlUtil.findOnlyOneChildElementByName(rstr, SoapUtil.WST_NAMESPACE, "RequestedSecurityToken");
        if (rst == null) throw new InvalidDocumentFormatException("Response contained no RequestedSecurityToken");

        // See what kind of requested security token we got

        // Check for SecurityContextToken
        Element scTokenEl = XmlUtil.findOnlyOneChildElementByName(rst, SoapUtil.WSSC_NAMESPACE,
                                                                  SoapUtil.SECURITY_CONTEXT_TOK_EL_NAME);
        if (scTokenEl != null) {
            // It's a SecurityContextToken
            if (clientPrivateKey == null || clientCertificate == null)
                throw new ProcessorException("Was not expecting to receive a SecurityContextToken"); // TODO relax this if we need to do WS-SC without a client cert

            return processSecurityContextToken(scTokenEl, rstr, clientCertificate, clientPrivateKey);
        }

        Element samlTokenEl = XmlUtil.findOnlyOneChildElementByName(rst, SamlConstants.NS_SAML, "Assertion");
        if (samlTokenEl != null) {
            // It's a signed SAML assertion
            try {
                return new SamlAssertion(samlTokenEl);
            } catch (SAXException e) {
                throw new InvalidDocumentFormatException(e);
            }
        }

        Element what = XmlUtil.findFirstChildElement(rst);
        throw new InvalidDocumentFormatException("Token server returned unrecognized security token " + what.getLocalName() +
                                                 " (namespace=" + what.getNamespaceURI() + ")");
    }

    private static void verifySignature(Element rstr,
                                        X509Certificate serverCertificate,
                                        Document response,
                                        X509Certificate clientCertificate,
                                        PrivateKey clientPrivateKey)
            throws InvalidDocumentFormatException, GeneralSecurityException, ProcessorException
    {
        ProcessorResult result = null;
        try {
            WssProcessor wssProcessor = new WssProcessorImpl();
            result = wssProcessor.undecorateMessage(response,
                                                    clientCertificate,
                                                    clientPrivateKey,
                                                    null);
        } catch (BadSecurityContextException e) {
            throw new InvalidDocumentFormatException("Response attempted to use a WS-SecureConversation SecurityContextToken, which we don't support when talking to the token server itself", e);
        }

        SignedElement[] signedElements = result.getElementsThatWereSigned();
        SecurityToken signingSecurityToken = null;
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            if (XmlUtil.isElementAncestor(rstr, signedElement.asElement())) {
                if (signingSecurityToken != null)
                    throw new InvalidDocumentFormatException("Response body was signed with more than one security token");
                signingSecurityToken = signedElement.getSigningSecurityToken();
                if (!(signingSecurityToken instanceof X509SecurityToken))
                    throw new InvalidDocumentFormatException("Response body was signed, but not with an X509 Security Token");
                X509SecurityToken x509Token = null;
                x509Token = (X509SecurityToken)signingSecurityToken;
                X509Certificate signingCert = x509Token.asX509Certificate();
                byte[] signingPublicKeyBytes = signingCert.getPublicKey().getEncoded();
                byte[] desiredPublicKeyBytes = serverCertificate.getPublicKey().getEncoded();
                if (!Arrays.equals(signingPublicKeyBytes, desiredPublicKeyBytes))
                    throw new InvalidDocumentFormatException("Response body was signed with an X509 certificate, but it wasn't the server certificate.");
            }
        }

        if (signingSecurityToken == null)
            throw new InvalidDocumentFormatException("Response body was not signed.");
    }

    private static Object processSecurityContextToken(Element scTokenEl,
                                                      Element rstr,
                                                      X509Certificate clientCertificate,
                                                      PrivateKey clientPrivateKey)
                                            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        if (clientPrivateKey == null) throw new NullPointerException();
        if (clientCertificate == null) throw new NullPointerException();

        // Extract session ID
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

        Element encryptedKeyEl = XmlUtil.findOnlyOneChildElementByName(rpt, SoapUtil.XMLENC_NS, "EncryptedKey");
        Element binarySecretEl = XmlUtil.findOnlyOneChildElementByName(rpt, SoapUtil.WST_NAMESPACE, "BinarySecret");
        byte[] sharedSecret = null;
        if (encryptedKeyEl != null) {
            // If there's a KeyIdentifier, log whether it's talking about our key
            // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
            XencUtil.checkKeyInfo(encryptedKeyEl, clientCertificate);

            // verify that the algo is supported
            XencUtil.checkEncryptionMethod(encryptedKeyEl);

            // Extract the encrypted key
            sharedSecret = XencUtil.decryptKey(encryptedKeyEl, clientPrivateKey);
        } else if (binarySecretEl != null && clientPrivateKey == null) {
            String base64edsecret = XmlUtil.getTextValue(binarySecretEl);
            try {
                sharedSecret = HexUtils.decodeBase64(base64edsecret, true);
            } catch (IOException e) {
                throw new InvalidDocumentFormatException(e);
            }
        } else if (binarySecretEl != null && clientPrivateKey != null) {
            throw new InvalidDocumentFormatException("Response RequestedProofToken contained a BinarySecret element " +
                                                     "but should contain an EncryptedKey instead since this client has " +
                                                     "a private key.");
        } else {
            throw new InvalidDocumentFormatException("Response RequestedProofToken did not contain an EncryptedKey " +
                                                     "element nor a BinarySecret element.");
        }

        final Date finalExpires = expires;
        final String finalIdentifier = identifier;
        final byte[] finalSecret = sharedSecret;
        return new SecureConversationSession() {
            public String getSessionId() {
                return finalIdentifier;
            }

            public byte[] getSharedSecret() {
                return finalSecret;
            }

            public Date getExpiryDate() {
                return finalExpires;
            }
        };
    }
}
