package com.l7tech.server;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.server.saml.HolderOfKeyHelper;
import com.l7tech.server.saml.SamlAssertionGenerator;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles WS Trust RequestSecurityToken requests as well as SAML token requests.
 * The request is originally received by the TokenServiceServlet.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 6, 2004<br/>
 * $Id$<br/>
 */
public class TokenServiceImpl implements TokenService {

    public class TokenServiceException extends Exception {
        public TokenServiceException(Throwable cause) {
            super(cause);
        }
        public TokenServiceException(String message) {
            super(message);
        }
        public TokenServiceException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Handles token service requests using a PolicyEnforcementContext. The policy enforced allows for requests
     * secured in the message or secured in the transport layer. If the response contains a secret, it will be
     * secured as part of the message layer if the request was secured that same way. This allows for such
     * requests to occur over SSL without the need for client certs.
     *
     * @param context contains the request at entry and contains a response document if everything goes well.
     */
    public void respondToSecurityTokenRequest(PolicyEnforcementContext context) throws InvalidDocumentFormatException,
                                                                                TokenServiceImpl.TokenServiceException,
                                                                                ProcessorException {
        throw new UnsupportedOperationException("TODO!");
        // todo, run a policy that allows for WSS signature, SAML auth, and transport level authentication
        // if the request is signed, then the response's secret will contain a response
    }

    /**
     * specify the server key and cert at construction time instead of letting the object try to retreive them
     */
    public TokenServiceImpl(PrivateKey privateServerKey, X509Certificate serverCert) {
        if (privateServerKey == null || serverCert == null) {
            throw new IllegalArgumentException("Server key and server cert must be provided to create a TokenService");
        }
        this.privateServerKey = privateServerKey;
        this.serverCert = serverCert;
    }

    /**
     * Handles the request for a security token (either secure conversation context or saml thing).
     * @param request the request for the secure conversation context
     * @param authenticator resolved credentials such as an X509Certificate to an actual user to associate the context with
     * @return
     */
    public Document respondToRequestSecurityToken(Document request, CredentialsAuthenticator authenticator, String clientAddress)
                                                    throws InvalidDocumentFormatException, TokenServiceException,
                                                           ProcessorException, GeneralSecurityException,
                                                           AuthenticationException, BadSecurityContextException {
        // Pass request to the trogdorminator!
        WssProcessor trogdor = new WssProcessorImpl();
        X509Certificate serverSSLcert = getServerCert();
        PrivateKey sslPrivateKey = getServerKey();

        // Authenticate the request, check who signed it
        ProcessorResult wssOutput = trogdor.undecorateMessage(request,
                                                                           serverSSLcert,
                                                                           sslPrivateKey,
                                                                           SecureConversationContextManager.getInstance());
        SecurityToken[] tokens = wssOutput.getSecurityTokens();
        X509Certificate clientCert = null;
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token instanceof X509SecurityToken) {
                X509SecurityToken x509token = (X509SecurityToken)token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        String msg = "Request included more than one X509 security token whose key ownership " +
                                     "was proven";
                        logger.log(Level.WARNING,  msg);
                        throw new TokenServiceException(msg);
                    }
                    clientCert = x509token.asX509Certificate();
                }
            }
        }

        final LoginCredentials creds = LoginCredentials.makeCertificateCredentials(clientCert,RequestWssX509Cert.class);
        User authenticatedUser = authenticator.authenticate(creds);
        if (authenticatedUser == null) {
            logger.info("Throwing AuthenticationException because credentials cannot be authenticated.");
            throw new AuthenticationException("Cert found, but cannot associate to a user.");
            
        }
        // Actually handle the request
        Document response = null;
        if (isValidRequestForSecureConversationContext(wssOutput.getUndecoratedMessage(), wssOutput)) {
            response = handleSecureConversationContextRequest(authenticatedUser, clientCert);
        } else if (isValidRequestForSAMLToken(wssOutput.getUndecoratedMessage(), wssOutput)) {
            response = handleSamlRequest(creds, clientAddress);
        } else {
            throw new InvalidDocumentFormatException("This request cannot be recognized as a valid " +
                                                     "RequestSecurityToken");
        }
        return response;
    }

    private Document handleSamlRequest(LoginCredentials creds, String clientAddress)
        throws TokenServiceException, GeneralSecurityException
    {
        StringBuffer responseXml = new StringBuffer(WST_RST_RESPONSE_PREFIX);
        try {
            SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
            if (clientAddress != null) options.setClientAddress(InetAddress.getByName(clientAddress));
            options.setSignAssertion(true);
            SignerInfo signerInfo = new SignerInfo(getServerKey(), new X509Certificate[] { getServerCert() });
            HolderOfKeyHelper hok = new HolderOfKeyHelper(null, options, creds, signerInfo);
            Document signedAssertionDoc = hok.createSignedAssertion(null); // TODO use a better ID!
            responseXml.append(XmlUtil.nodeToString(signedAssertionDoc));
            responseXml.append(WST_RST_RESPONSE_INFIX);
            responseXml.append(WST_RST_RESPONSE_SUFFIX);
            Document response = XmlUtil.stringToDocument(responseXml.toString());
            return prepareSignedResponse(response);
        } catch ( UnknownHostException e ) {
            throw new TokenServiceException("Couldn't resolve client IP address", e);
        } catch ( IOException e ) {
            throw new TokenServiceException("Couldn't read signing key", e);
        } catch ( SAXException e ) {
            throw new TokenServiceException("Couldn't read signing key", e);
        }
    }

    private Document handleSecureConversationContextRequest(User requestor, X509Certificate requestorCert)
                                                                throws TokenServiceException, GeneralSecurityException {
        SecureConversationSession newSession = null;
        try {
            newSession = SecureConversationContextManager.getInstance().createContextForUser(requestor);
        } catch (DuplicateSessionException e) {
            throw new TokenServiceException(e);
        }
        // xml newSession
        Document response = null;
        Calendar exp = Calendar.getInstance();
        exp.setTimeInMillis(newSession.getExpiration());
        String encryptedKeyRawXML = produceEncryptedKeyXml(newSession.getSharedSecret(), requestorCert);
        try {
            String xmlStr = WST_RST_RESPONSE_PREFIX +
                                      "<wsc:SecurityContextToken>" +
                                        "<wsc:Identifier>" + newSession.getIdentifier() + "</wsc:Identifier>" +
                                      "</wsc:SecurityContextToken>" +
                            WST_RST_RESPONSE_INFIX +
                                    "<wst:RequestedProofToken>" +
                                      encryptedKeyRawXML +
                                    "</wst:RequestedProofToken>" +
                                    "<wst:Lifetime>" +
                                      "<wsu:Expires>" + ISO8601Date.format(exp.getTime()) + "</wsu:Expires>" +
                                    "</wst:Lifetime>" +
                                  WST_RST_RESPONSE_SUFFIX;
            response = XmlUtil.stringToDocument(xmlStr);
        } catch (IOException e) {
            throw new TokenServiceException(e);
        } catch (SAXException e) {
            throw new TokenServiceException(e);
        }

        return prepareSignedResponse( response );
    }

    private Document prepareSignedResponse( Document response ) throws TokenServiceException {
        Element body = null;
        try {
            body = SoapUtil.getBodyElement(response);
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        }

        X509Certificate serverSSLcert = getServerCert();
        PrivateKey sslPrivateKey = getServerKey();

        WssDecorator wssDecorator = new WssDecoratorImpl();
        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp(true);
        req.setSenderCertificate(serverSSLcert);
        req.setSenderPrivateKey(sslPrivateKey);
        req.getElementsToSign().add(body);

        try {
            wssDecorator.decorateMessage(response, req);
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        } catch (GeneralSecurityException e) {
            throw new TokenServiceException(e);
        } catch (DecoratorException e) {
            throw new TokenServiceException(e);
        }
        return response;
    }

    private String produceEncryptedKeyXml(SecretKey sharedSecret, X509Certificate requestorCert) throws GeneralSecurityException {
        StringBuffer encryptedKeyXml = new StringBuffer();
        // Key info and all
        encryptedKeyXml.append("<xenc:EncryptedKey wsu:Id=\"newProof\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">" +
                                 "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\" />");

        // append ski if applicable
        byte[] recipSki = requestorCert.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
        if (recipSki != null && recipSki.length > 4) {
            byte[] goodSki = new byte[recipSki.length - 4];
            System.arraycopy(recipSki, 4, goodSki, 0, goodSki.length);
            // add the ski
            String recipSkiB64 = HexUtils.encodeBase64(goodSki, true);
            String skiRef = "<wsse:SecurityTokenReference>" +
                              "<wsse:KeyIdentifier ValueType=\"" + SoapUtil.VALUETYPE_SKI + "\">" +
                                recipSkiB64 +
                              "</wsse:KeyIdentifier>" +
                            "</wsse:SecurityTokenReference>";

            encryptedKeyXml.append("<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");
            encryptedKeyXml.append(skiRef);
            encryptedKeyXml.append("</KeyInfo>");
        } else {
            // add a full cert ?
            // todo
        }
        encryptedKeyXml.append("<xenc:CipherData>" +
                                "<xenc:CipherValue>");
        String encryptedKeyValue = XencUtil.encryptKeyWithRsaAndPad(sharedSecret.getEncoded(),
                                                                    requestorCert.getPublicKey(),
                                                                    rand);
        encryptedKeyXml.append(encryptedKeyValue);
        encryptedKeyXml.append("</xenc:CipherValue>" +
                             "</xenc:CipherData>" +
                           "</xenc:EncryptedKey>");
        return encryptedKeyXml.toString();
    }

    private boolean isValidRequestForSecureConversationContext(Document request,
                                                               ProcessorResult wssOutput)
                                                                        throws InvalidDocumentFormatException {
        Element body = SoapUtil.getBodyElement(request);
        // body must include wst:RequestSecurityToken element
        Element maybeRSTEl = XmlUtil.findFirstChildElement(body);
        if (!maybeRSTEl.getLocalName().equals(RST_ELNAME)) {
            logger.fine("Body's child does not seem to be a RST (" + maybeRSTEl.getLocalName() + ")");
            return false;
        }
        if (!maybeRSTEl.getNamespaceURI().equals(SoapUtil.WST_NAMESPACE)) {
            logger.fine("Trust namespace not recognized (" + maybeRSTEl.getNamespaceURI() + ")");
            return false;
        }
        // validate <wst:TokenType>http://schemas.xmlsoap.org/ws/2004/04/sct</wst:TokenType>
        Element tokenTypeEl = XmlUtil.findOnlyOneChildElementByName(maybeRSTEl, SoapUtil.WST_NAMESPACE, TOKTYPE_ELNAME);
        if (tokenTypeEl == null) {
            logger.warning("Token type not specified. This is not supported.");
            return false;
        }
        String value = XmlUtil.getTextValue(tokenTypeEl);
        if (!value.equals("http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct")) {
            return false;
        }
        // validate <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>
        Element reqTypeEl = XmlUtil.findOnlyOneChildElementByName(maybeRSTEl, SoapUtil.WST_NAMESPACE, REQTYPE_ELNAME);
        if (reqTypeEl == null) {
            logger.warning("Request type not specified. This is not supported.");
            return false;
        }
        value = XmlUtil.getTextValue(reqTypeEl);
        if (!value.equals("http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue")) {
            logger.warning("RequestType not supported." + value);
            return false;
        }
        // make sure body was signed
        SignedElement[] signedElements = wssOutput.getElementsThatWereSigned();
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            if (signedElement.asElement() == body) {
                return true;
            }
        }
        logger.warning("Seems like the body was not signed.");
        return false;
    }

    private boolean isValidRequestForSAMLToken(Document request, ProcessorResult wssOutput) throws InvalidDocumentFormatException {
        Element body = SoapUtil.getBodyElement(request);
        Element maybeRSTEl = XmlUtil.findFirstChildElement(body);

        // body must include wst:RequestSecurityToken element
        if (!maybeRSTEl.getLocalName().equals(RST_ELNAME)) {
            logger.fine("Body's child does not seem to be a RST (" + maybeRSTEl.getLocalName() + ")");
            return false;
        }
        if (!maybeRSTEl.getNamespaceURI().equals(SoapUtil.WST_NAMESPACE)) {
            logger.fine("Trust namespace not recognized (" + maybeRSTEl.getNamespaceURI() + ")");
            return false;
        }
        Element tokenTypeEl = XmlUtil.findOnlyOneChildElementByName(maybeRSTEl, SoapUtil.WST_NAMESPACE, TOKTYPE_ELNAME);
        if (tokenTypeEl == null) {
            logger.warning("Token type not specified. This is not supported.");
            return false;
        }

        // validate <wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>
        Element reqTypeEl = XmlUtil.findOnlyOneChildElementByName(maybeRSTEl, SoapUtil.WST_NAMESPACE, REQTYPE_ELNAME);
        if (reqTypeEl == null) {
            logger.warning("Request type not specified. This is not supported.");
            return false;
        }
        String value = XmlUtil.getTextValue(reqTypeEl);
        if (!value.equals("http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue")) {
            logger.warning("RequestType '" + value + "' not supported.");
            return false;
        }
        // make sure body was signed
        boolean signed = false;
        SignedElement[] signedElements = wssOutput.getElementsThatWereSigned();
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            if (signedElement.asElement() == body) {
                signed = true;
            }
        }
        if (!signed) {
            logger.warning("Seems like the body was not signed.");
            return false;
        }

        // validate <wst:TokenType>saml:Assertion</wst:TokenType>
        String qname = XmlUtil.getTextValue(tokenTypeEl);
        Map namespaces = XmlUtil.getAncestorNamespaces(tokenTypeEl);
        String samlPrefix = (String)namespaces.get(SamlConstants.NS_SAML);
        int cpos = qname.indexOf(":");
        if (cpos > 0) {
            String qprefix = qname.substring(0,cpos);
            String qlpart = qname.substring(cpos+1);
            if (qprefix.equals(samlPrefix) && SamlConstants.ELEMENT_ASSERTION.equals(qlpart)) {
                return true;
            }
        }

        logger.warning("TokenType '" + qname + "' is not a valid saml:Assertion QName");
        return false;
    }

    private synchronized PrivateKey getServerKey() {
        return privateServerKey;
    }

    private synchronized X509Certificate getServerCert() {
        return serverCert;
    }

    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final static String RST_ELNAME = "RequestSecurityToken";
    private final static String TOKTYPE_ELNAME = "TokenType";
    private final static String REQTYPE_ELNAME = "RequestType";
    private final static SecureRandom rand = new SecureRandom();

    private final String WST_RST_RESPONSE_PREFIX = "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                    "<soap:Body>" +
                                      "<wst:RequestSecurityTokenResponse xmlns:wst=\"" + SoapUtil.WST_NAMESPACE + "\" " +
                                                                        "xmlns:wsu=\"" + SoapUtil.WSU_NAMESPACE + "\" " +
                                                                        "xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\" " +
                                                                        "xmlns:wsc=\"" + SoapUtil.WSSC_NAMESPACE + "\">" +
                                        "<wst:RequestedSecurityToken>";

    private static final String WST_RST_RESPONSE_INFIX = "</wst:RequestedSecurityToken>";
    private static final String WST_RST_RESPONSE_SUFFIX = "</wst:RequestSecurityTokenResponse>" +
                                                "</soap:Body>" +
                                            "</soap:Envelope>";
}
