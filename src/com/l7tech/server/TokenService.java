package com.l7tech.server;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssProcessorImpl;
import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.security.xml.WssDecoratorImpl;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import javax.xml.soap.SOAPConstants;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;

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
public class TokenService {
    public interface CredentialsAuthenticator {
        User authenticate(LoginCredentials creds);
    }

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


    public TokenService() {}

    /**
     * specify the server key and cert at construction time instead of letting the object try to retreive them
     */
    public TokenService(PrivateKey privateServerKey, X509Certificate serverCert) {
        this.privateServerKey = privateServerKey;
        this.serverCert = serverCert;
    }

    /**
     * Handles the request for a security token (either secure conversation context or saml thing).
     * @param request the request for the secure conversation context
     * @param authenticator resolved credentials such as an X509Certificate to an actual user to associate the context with
     * @return
     */
    public Document respondToRequestSecurityToken(Document request, CredentialsAuthenticator authenticator)
                                                    throws InvalidDocumentFormatException, TokenServiceException,
                                                           WssProcessor.ProcessorException, GeneralSecurityException {
        // Pass request to the trogdorminator!
        WssProcessor trogdor = new WssProcessorImpl();
        X509Certificate serverSSLcert = null;
        PrivateKey sslPrivateKey = null;
        try {
            serverSSLcert = getServerCert();
            sslPrivateKey = getServerKey();
        } catch (CertificateException e) {
            String msg = "Error getting server cert/private key";
            logger.log(Level.SEVERE, msg, e);
            throw new TokenServiceException(msg, e);
        } catch (KeyStoreException e) {
            String msg = "Error getting server cert/private key";
            logger.log(Level.SEVERE, msg, e);
            throw new TokenServiceException(msg, e);
        } catch (IOException e){
            String msg = "Error getting server cert/private key";
            logger.log(Level.SEVERE, msg, e);
            throw new TokenServiceException(msg, e);
        }
        // Authenticate the request, check who signed it
        WssProcessor.ProcessorResult wssOutput = trogdor.undecorateMessage(request,
                                                                           serverSSLcert,
                                                                           sslPrivateKey,
                                                                           SecureConversationContextManager.getInstance());
        WssProcessor.SecurityToken[] tokens = wssOutput.getSecurityTokens();
        X509Certificate clientCert = null;
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken token = tokens[i];
            if (token instanceof WssProcessor.X509SecurityToken) {
                WssProcessor.X509SecurityToken x509token = (WssProcessor.X509SecurityToken)token;
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
        String certCN = null;
        try {
            X500Name x500name = new X500Name(clientCert.getSubjectX500Principal().getName());
            certCN = x500name.getCommonName();
        } catch (IOException e) {
            throw new TokenServiceException("cannot get cert subject", e);
        }
        User authenticatedUser = authenticator.authenticate(new LoginCredentials(certCN,
                                                                                 null,
                                                                                 CredentialFormat.CLIENTCERT,
                                                                                 null,
                                                                                 null,
                                                                                 clientCert));
        if (authenticatedUser == null) {
            // todo, some authentication exception, dont allow creation of a context not associated with an identity
            
        }
        // Actually handle the request
        Document response = null;
        if (isValidRequestForSecureConversationContext(wssOutput.getUndecoratedMessage(), wssOutput)) {
            response = handleSecureConversationContextRequest(authenticatedUser, clientCert);
        } else if (isValidRequestForSAMLToken(request, wssOutput)) {
            // todo, plug in your saml handling here alex --fla
        } else {
            throw new InvalidDocumentFormatException("This request cannot be recognized as a valid " +
                                                     "RequestSecurityToken");
        }
        return response;
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
            String xmlStr = "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                "<soap:Body>" +
                                  "<wst:RequestSecurityTokenResponse xmlns:wst=\"" + SoapUtil.WST_NAMESPACE + "\" " +
                                                                    "xmlns:wsu=\"" + SoapUtil.WSU_NAMESPACE + "\" " +
                                                                    "xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\" " +
                                                                    "xmlns:wsc=\"" + SoapUtil.WSSC_NAMESPACE + "\">" +
                                    "<wst:RequestedSecurityToken>" +
                                        "<wsc:Identifier>" + newSession.getIdentifier() + "</wsc:Identifier>" +
                                    "</wst:RequestedSecurityToken>" +
                                    "<wst:RequestedProofToken>" +
                                      encryptedKeyRawXML +
                                    "</wst:RequestedProofToken>" +
                                    "<wst:Lifetime>" +
                                      "<wsu:Expires>" + ISO8601Date.format(exp.getTime()) + "</wsu:Expires>" +
                                    "</wst:Lifetime>" +
                                  "</wst:RequestSecurityTokenResponse>" +
                                "</soap:Body>" +
                            "</soap:Envelope>";
            response = XmlUtil.stringToDocument(xmlStr);
        } catch (IOException e) {
            throw new TokenServiceException(e);
        } catch (SAXException e) {
            throw new TokenServiceException(e);
        }

        Element body = null;
        try {
            body = SoapUtil.getBodyElement(response);
        } catch (InvalidDocumentFormatException e) {
            throw new TokenServiceException(e);
        }


        X509Certificate serverSSLcert = null;
        PrivateKey sslPrivateKey = null;
        try {
            serverSSLcert = getServerCert();
            sslPrivateKey = getServerKey();
        } catch (CertificateException e) {
            String msg = "Error getting server cert/private key";
            logger.log(Level.SEVERE, msg, e);
            throw new TokenServiceException(msg, e);
        } catch (KeyStoreException e) {
            String msg = "Error getting server cert/private key";
            logger.log(Level.SEVERE, msg, e);
            throw new TokenServiceException(msg, e);
        } catch (IOException e){
            String msg = "Error getting server cert/private key";
            logger.log(Level.SEVERE, msg, e);
            throw new TokenServiceException(msg, e);
        }

        WssDecorator wssDecorator = new WssDecoratorImpl();
        WssDecorator.DecorationRequirements req = new WssDecorator.DecorationRequirements();
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
        } catch (WssDecorator.DecoratorException e) {
            throw new TokenServiceException(e);
        }
        return response;
    }

    private String produceEncryptedKeyXml(SecretKey sharedSecret, X509Certificate requestorCert) throws GeneralSecurityException {
        StringBuffer encryptedKeyXml = new StringBuffer();
        // Key info and all
        encryptedKeyXml.append("<xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">" +
                                 "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\" />" +
                                 "<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");

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
            encryptedKeyXml.append(skiRef);
        } else {
            // add a full cert ?
            // todo
        }
        encryptedKeyXml.append("</KeyInfo>");
        encryptedKeyXml.append("<xenc:CipherData>" +
                                "<xenc:CipherValue>");
        encryptedKeyXml.append(encryptWithRsa(sharedSecret.getEncoded(), requestorCert.getPublicKey()));
        encryptedKeyXml.append("</xenc:CipherValue>" +
                             "</xenc:CipherData>" +
                           "</xenc:EncryptedKey>");
        return encryptedKeyXml.toString();
    }

    // todo, this was copied from WssDecoratorImpl, should generalize
    private String encryptWithRsa(byte[] keyBytes, PublicKey publicKey) throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        if (!(publicKey instanceof RSAPublicKey))
            throw new KeyException("Unable to encrypt -- unsupported recipient public key type " +
                                   publicKey.getClass().getName());
        final int modulusLength = ((RSAPublicKey)publicKey).getModulus().toByteArray().length;
        byte[] paddedKeyBytes = padSymmetricKeyForRsaEncryption(keyBytes, modulusLength);
        byte[] encrypted = rsa.doFinal(paddedKeyBytes);
        return HexUtils.encodeBase64(encrypted, true);
    }

    // todo, this was copied from WssDecoratorImpl, should generalize
    private byte[] padSymmetricKeyForRsaEncryption(byte[] keyBytes, int modulusBytes) throws KeyException {
        int padbytes = modulusBytes - 3 - keyBytes.length;
        // Check just in case, although this should never happen in real life
        if (padbytes < 8)
            throw new KeyException("Recipient RSA public key has too few bits to encode this symmetric key");
        byte[] padded = new byte[modulusBytes - 1];
        int pos = 0;
        padded[pos++] = 2;
        while (padbytes > 0) {
            padded[pos++] = (byte)(rand.nextInt(255) + 1);
            padbytes--;
        }
        padded[pos++] = 0;
        System.arraycopy(keyBytes, 0, padded, pos, keyBytes.length);
        return padded;
    }

    private boolean isValidRequestForSecureConversationContext(Document request,
                                                               WssProcessor.ProcessorResult wssOutput)
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
            logger.warning("TokenType not supported." + value);
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
        WssProcessor.SignedElement[] signedElements = wssOutput.getElementsThatWereSigned();
        for (int i = 0; i < signedElements.length; i++) {
            WssProcessor.SignedElement signedElement = signedElements[i];
            if (signedElement.asElement() == body) {
                return true;
            }
        }
        logger.warning("Seems like the body was not signed.");
        return false;
    }

    private boolean isValidRequestForSAMLToken(Document request, WssProcessor.ProcessorResult wssOutput) {
        // todo, alex what makes this request a saml token request?
        return false;
    }

    private synchronized PrivateKey getServerKey() throws KeyStoreException {
        if (privateServerKey == null) {
            privateServerKey = KeystoreUtils.getInstance().getSSLPrivateKey();
        }
        return privateServerKey;
    }

    private synchronized X509Certificate getServerCert() throws IOException, CertificateException {
        if (serverCert == null) {
            byte[] buf = KeystoreUtils.getInstance().readSSLCert();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            serverCert = (X509Certificate)(CertificateFactory.getInstance("X.509").generateCertificate(bais));
        }
        return serverCert;
    }

    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final static String RST_ELNAME = "RequestSecurityToken";
    private final static String TOKTYPE_ELNAME = "TokenType";
    private final static String REQTYPE_ELNAME = "RequestType";
    private final static SecureRandom rand = new SecureRandom();
}
