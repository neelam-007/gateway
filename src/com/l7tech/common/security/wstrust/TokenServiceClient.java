/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.wstrust;

import com.l7tech.common.http.*;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.SamlException;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.*;
import com.l7tech.common.xml.saml.SamlAssertion;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLException;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds request messages for TokenService and helps parse the responses.
 * TODO this class still needs some refactoring
 */
public class TokenServiceClient {
    public static final Logger log = Logger.getLogger(TokenServiceClient.class.getName());

    /**
     * Create a signed SOAP message containing a WS-Trust RequestSecurityToken message asking for the
     * specified token type.
     *
     * @param clientCertificate  the certificate to use to sign the request
     * @param clientPrivateKey   the private key of the certificate to use to sign the request
     * @param desiredTokenType   the token type being applied for
     * @param base
     * @param appliesToAddress   wsa:Address to use for wsp:AppliesTo, or null to leave out the AppliesTo
     * @param wstIssuerAddress   wsa:Address to use for wst:Issuer, or null to leave out the Issuer
     * @return a signed SOAP message containing a wst:RequestSecurityToken
     * @throws CertificateException if ther eis a problem with the clientCertificate
     */
    public static Document createRequestSecurityTokenMessage(X509Certificate clientCertificate,
                                                             PrivateKey clientPrivateKey,
                                                             SecurityTokenType desiredTokenType,
                                                             WsTrustRequestType requestType,
                                                             SecurityToken base,
                                                             String appliesToAddress,
                                                             String wstIssuerAddress,
                                                             Date timestampCreatedDate)
            throws CertificateException
    {
        try {
            Document msg = requestSecurityTokenMessageTemplate(desiredTokenType,
                    requestType, appliesToAddress, wstIssuerAddress, base);
            Element env = msg.getDocumentElement();
            Element body = XmlUtil.findFirstChildElementByName(env, env.getNamespaceURI(), "Body");

            // Maybe sign it
            WssDecorator wssDecorator = new WssDecoratorImpl();
            DecorationRequirements req = new DecorationRequirements();
            if (clientPrivateKey != null && clientCertificate != null) {
                req.setSenderMessageSigningCertificate(clientCertificate);
                req.setSenderMessageSigningPrivateKey(clientPrivateKey);
                req.setSignTimestamp();
                req.getElementsToSign().add(body);
            }
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
                                                                WsTrustRequestType requestType,
                                                                String appliesToAddress,
                                                                String wstIssuerAddress, SecurityToken base)
            throws IOException, SAXException
    {
        // TODO fix or remove this hack: if a saml: qname will be used, declare saml NS in root element
        String extraNs = "";
        if (desiredTokenType != null && SamlSecurityToken.class.isAssignableFrom(desiredTokenType.getInterfaceClass()))
            extraNs += " xmlns:saml=\"" + desiredTokenType.getWstPrototypeElementNs() + "\"";

        Document msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\"" + extraNs + ">" +
                                                    "<soap:Header/><soap:Body>" +
                                                    "<wst:RequestSecurityToken xmlns:wst=\"" + SoapUtil.WST_NAMESPACE + "\">" +
                                                    "</wst:RequestSecurityToken>" +
                                                    "</soap:Body></soap:Envelope>");
        Element env = msg.getDocumentElement();
        Element body = XmlUtil.findFirstChildElementByName(env, env.getNamespaceURI(), "Body");
        Element rst = XmlUtil.findFirstChildElement(body);

        // Add AppliesTo, if provided
        if (appliesToAddress != null && appliesToAddress.length() > 0) {
            Element appliesTo = XmlUtil.createAndAppendElementNS(rst, "AppliesTo", SoapUtil.WSP_NAMESPACE, "wsp");
            Element endpointRef = XmlUtil.createAndAppendElementNS(appliesTo, "EndpointReference", SoapUtil.WSA_NAMESPACE, "wsa");
            Element address = XmlUtil.createAndAppendElementNS(endpointRef, "Address", SoapUtil.WSA_NAMESPACE, "wsa");
            address.appendChild(XmlUtil.createTextNode(address, appliesToAddress));
        }

        // Add Issuer, if provided
        if (wstIssuerAddress != null && wstIssuerAddress.length() > 0) {
            Element issuer = XmlUtil.createAndAppendElementNS(rst, "Issuer", SoapUtil.WST_NAMESPACE, "wst");
            Element address = XmlUtil.createAndAppendElementNS(issuer, "Address", SoapUtil.WSA_NAMESPACE, "wsa");
            address.appendChild(XmlUtil.createTextNode(address, wstIssuerAddress));
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
            Element tokenEl = base.asElement();

            // Ensure all prefixes inherited from token's original context are available to the token
            Node n = tokenEl;
            Map declaredTokenNamespaces = new HashMap();
            Map usedTokenNamespaces = new HashMap();
            while (n != null) {
                if (n.getPrefix() != null && n.getNamespaceURI() != null)
                    usedTokenNamespaces.put(n.getPrefix(), n.getNamespaceURI());
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    NamedNodeMap attrs = ((Element)n).getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Attr attr = (Attr) attrs.item(i);
                        if ("xmlns".equalsIgnoreCase(attr.getPrefix())) {
                            declaredTokenNamespaces.put(attr.getLocalName(), attr.getValue());
                        } else if (attr.getPrefix() != null && attr.getNamespaceURI() != null) {
                            usedTokenNamespaces.put(attr.getPrefix(), attr.getNamespaceURI());
                        }
                    }
                }
                Node next = n.getNextSibling();
                if (next == null) next = n.getFirstChild();
                n = next;
            }

            for (Iterator i = usedTokenNamespaces.keySet().iterator(); i.hasNext();) {
                String prefix = (String) i.next();
                String uri = (String)usedTokenNamespaces.get(prefix);
                if (declaredTokenNamespaces.containsKey(prefix) && declaredTokenNamespaces.get(prefix).equals(uri)) {
                    // Already there
                } else {
                    String newPrefix = XmlUtil.findUnusedNamespacePrefix(tokenEl, prefix);
                    tokenEl.setAttribute("xmlns:" + newPrefix, uri);
                }
            }

            baseEl.appendChild(msg.importNode(tokenEl, true));
        }

        // Add RequestType
        {
            Element rt = XmlUtil.createAndAppendElementNS(rst, "RequestType", rst.getNamespaceURI(), "wst");
            rt.appendChild(XmlUtil.createTextNode(msg, requestType.getUri()));
        }

        return msg;
    }

    public static Document createRequestSecurityTokenMessage(SecurityTokenType desiredTokenType, WsTrustRequestType requestType, SecurityToken base, String appliesToAddress, String wstIssuerAddress) {
        try {
            return requestSecurityTokenMessageTemplate(desiredTokenType,
                    requestType,
                    appliesToAddress,
                    wstIssuerAddress,
                    base);
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
     * Requests a SecureConversation context token. The request is authenticated using an xml digital signature and
     * the response is required to be signed.
     */
    public static SecureConversationSession obtainSecureConversationSessionUsingWssSignature(
            GenericHttpClient httpClient, URL url, Date timestampCreatedDate,
            X509Certificate serverCertificate, X509Certificate clientCertificate,
            PrivateKey clientPrivateKey)
            throws IOException, GeneralSecurityException
    {
        Document requestDoc = createRequestSecurityTokenMessage(clientCertificate, clientPrivateKey,
                                                                SecurityTokenType.WSSC_CONTEXT, WsTrustRequestType.ISSUE, null, null, null, timestampCreatedDate);
        Object result = obtainResponse(httpClient, clientCertificate, url, requestDoc, clientPrivateKey, serverCertificate, null, true);

        if (!(result instanceof SecureConversationSession))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SecureConversationSession)result;
    }

    /**
     * Requests a SecureConversation context token. The request is transport-secured (ssl) and transport authenticated.
     */
    public static SecureConversationSession obtainSecureConversationSessionWithSslAndOptionalHttpBasic(
            GenericHttpClient httpClient, PasswordAuthentication httpBasicCredentials,
            URL url, X509Certificate serverCertificate)
            throws IOException, GeneralSecurityException
    {
        if (!("https".equals(url.getProtocol()))) throw new IllegalArgumentException("URL must be HTTPS");
        Document requestDoc = createRequestSecurityTokenMessage(SecurityTokenType.WSSC_CONTEXT, WsTrustRequestType.ISSUE, null, null, null);
        Object result = obtainResponse(httpClient, null, url, requestDoc, null, serverCertificate, httpBasicCredentials, false);

        if (!(result instanceof SecureConversationSession))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SecureConversationSession)result;
    }

    /**
     * Obtain a SAML token using WS-Trust.  The request will be signed if a client cert, client key, and recipient
     * cert are provided.  Response will be required to be signed if requireWssSignedResponse is true.
     *
     * @param httpClient
     * @param httpBasicCredentials
     * @param url
     * @param serverCertificate
     * @param timestampCreatedDate
     * @param clientCertificate
     * @param clientPrivateKey
     * @param requestType
     * @param tokenType
     * @param base
     * @param appliesToAddress
     * @param wstIssuerAddress
     * @param requireWssSignedResponse
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static SamlAssertion obtainSamlAssertion(GenericHttpClient httpClient,
                                                    PasswordAuthentication httpBasicCredentials,
                                                    URL url,
                                                    X509Certificate serverCertificate,
                                                    Date timestampCreatedDate,
                                                    X509Certificate clientCertificate,
                                                    PrivateKey clientPrivateKey,
                                                    WsTrustRequestType requestType,
                                                    SecurityTokenType tokenType,
                                                    SecurityToken base,
                                                    String appliesToAddress,
                                                    String wstIssuerAddress,
                                                    boolean requireWssSignedResponse)
            throws IOException, GeneralSecurityException
    {
        if (requireWssSignedResponse && serverCertificate == null)
            throw new IllegalArgumentException("requireWssSignedResponse, but no server cert provided");
        if (timestampCreatedDate == null) timestampCreatedDate = new Date();
        Document requestDoc = createRequestSecurityTokenMessage(clientCertificate,
                                                                clientPrivateKey,
                                                                tokenType,
                                                                requestType,
                                                                base,
                                                                appliesToAddress,
                                                                wstIssuerAddress,
                                                                timestampCreatedDate);
        requestDoc.getDocumentElement().setAttribute("xmlns:saml", SamlConstants.NS_SAML);
        Object result = obtainResponse(httpClient, clientCertificate, url, requestDoc, clientPrivateKey, serverCertificate, httpBasicCredentials, requireWssSignedResponse);

        if (!(result instanceof SamlAssertion))
            throw new IOException("Token server returned unwanted token type " + result.getClass());
        return (SamlAssertion)result;
    }

    private static Object obtainResponse(GenericHttpClient httpClient,
                                         X509Certificate clientCertificate,
                                         URL url,
                                         Document requestDoc,
                                         PrivateKey clientPrivateKey,
                                         X509Certificate serverCertificate,
                                         PasswordAuthentication httpBasicCredentials,
                                         boolean requireWssSignedResponse)
            throws IOException, GeneralSecurityException
    {
        Document response = null;
        try {
            String clientName = "current user";
            if (clientCertificate != null)
                clientName = clientCertificate.getSubjectDN().getName();
            log.log(Level.INFO, "Applying for new Security Token for " + clientName +
                                " with token server " + url.toString());

            GenericHttpRequestParamsImpl params = new GenericHttpRequestParamsImpl(url);
            if (httpBasicCredentials != null)
                params.setPasswordAuthentication(httpBasicCredentials);
            params.setContentType(ContentTypeHeader.XML_DEFAULT);
            params.setExtraHeaders(new HttpHeader[] {
                new GenericHttpHeader(SoapUtil.SOAPACTION, "\"\""),
            });

            //if (httpBasicCredentials != null)
            //    params.setPasswordAuthentication(httpBasicCredentials);

            SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
            SimpleHttpClient.SimpleXmlResponse conn = simpleClient.postXml(params, requestDoc);

            Long len = conn.getContentLength();
            log.log(Level.FINEST, "Token server response content length=" + len);
            response = conn.getDocument();
        } catch (SAXException e) {
            throw new CausedIOException("Unable to parse RequestSecurityTokenResponse from security token service: " + e.getMessage(), e);
        } catch (GenericHttpException e) {
            Throwable sslException = ExceptionUtils.getCauseIfCausedBy(e, SSLException.class);
            if (sslException instanceof SSLException)
                throw (SSLException)sslException; // rethrow as SSLException so server cert can be discovered if necessary
            throw e; // let it through as-is
        } catch (IOException e) {
            throw new CausedIOException("Unable to obtain a token from the security token server: " + e.getMessage(), e);
        }

        //log.log(Level.FINE,  "Got response from token server (reformatted): " + XmlUtil.nodeToFormattedString(response));

        try {
            checkForSoapFault(response);
            if (serverCertificate != null && requireWssSignedResponse) {
                if (clientCertificate != null && clientPrivateKey != null) {
                    return parseSignedRequestSecurityTokenResponse(response,
                                                                   clientCertificate,
                                                                   clientPrivateKey,
                                                                   serverCertificate);
                }
                return parseSignedRequestSecurityTokenResponse(response, null, null, serverCertificate);
            }
            return parseUnsignedRequestSecurityTokenResponse(response);
        } catch (ProcessorException e) {
            throw new CausedIOException("Unable to obtain a token from the security token server: " + e.getMessage(), e);
        } catch (InvalidDocumentFormatException e) {
            throw new CausedIOException(e.getMessage(), e);
        }
    }

    private static void checkForSoapFault(Document response) throws InvalidDocumentFormatException {
        {
            // check for fault message from server
            Element payload = SoapUtil.getPayloadElement(response);
            if (payload == null)
                throw new MissingRequiredElementException("Token server response is missing SOAP Body or payload element");
            if (response.getDocumentElement().getNamespaceURI().equals(payload.getNamespaceURI()) && "Fault".equals(payload.getLocalName())) {
                SoapFaultDetail sfd = SoapFaultUtils.gatherSoapFaultDetail(response);
                throw new InvalidDocumentFormatException("Unexpected SOAP fault from token service: " + sfd.getFaultCode() + ": " + sfd.getFaultString());
            }
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
            return processSecurityContextToken(scTokenEl, rstr, clientCertificate, clientPrivateKey);
        }

        Element samlTokenEl = XmlUtil.findOnlyOneChildElementByName(rst, SamlConstants.NS_SAML, "Assertion");
        if (samlTokenEl != null) {
            // It's a signed SAML assertion
            try {
                return new SamlAssertion(samlTokenEl);
            } catch (SAXException e) {
                throw new InvalidDocumentFormatException(e);
            } catch (SamlException e) {
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
            result = wssProcessor.undecorateMessage(new Message(response),
                                                    clientCertificate,
                                                    clientPrivateKey,
                                                    null);
        } catch (BadSecurityContextException e) {
            throw new InvalidDocumentFormatException("Response attempted to use a WS-SecureConversation SecurityContextToken, which we don't support when talking to the token server itself", e);
        } catch (IOException e) {
            throw new ProcessorException(e); // probably can't happen here
        } catch (SAXException e) {
            throw new InvalidDocumentFormatException(e);
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
            if (clientCertificate != null)
                XencUtil.checkKeyInfo(encryptedKeyEl, clientCertificate);
            else
                log.log(Level.FINER, "Not checking KeyIdentifier: client cert not available");

            // verify that the algo is supported
            XencUtil.checkEncryptionMethod(encryptedKeyEl);

            // Extract the encrypted key
            if (clientPrivateKey == null)
                throw new InvalidDocumentFormatException("Was not expecting to receive an encrypted token: client private key not available");
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
