package com.l7tech.server;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssProcessorImpl;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import org.w3c.dom.Document;
import sun.security.x509.X500Name;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
public class TokenService {
    public interface CredentialsAuthenticator {
        User authenticate(LoginCredentials creds);
    }

    /**
     * Handles the request for a security token (either secure conversation context or saml thing).
     * @param request the request for the secure conversation context
     * @param authenticator resolved credentials such as an X509Certificate to an actual user to associate the context with
     * @return
     */
    public Document respondToRequestSecurityToken(Document request, CredentialsAuthenticator authenticator) throws InvalidDocumentFormatException {
        // Pass request to the trogdorminator!
        WssProcessor trogdor = new WssProcessorImpl();
        X509Certificate serverSSLcert = null;
        PrivateKey sslPrivateKey = null;
        try {
            serverSSLcert = getServerCert();
            sslPrivateKey = getServerKey();
        } catch (CertificateException e) {
            logger.log(Level.SEVERE, "Error getting server cert/private key", e);
            // todo, some error
        } catch (KeyStoreException e) {
            logger.log(Level.SEVERE, "Error getting server cert/private key", e);
            // todo, some error
        } catch (IOException e){
            logger.log(Level.SEVERE, "Error getting server cert/private key", e);
            // todo, some error
        }
        // Authenticate the request, check who signed it
        WssProcessor.ProcessorResult wssOutput = null;
        try {
            wssOutput = trogdor.undecorateMessage(request,
                                                  serverSSLcert,
                                                  sslPrivateKey,
                                                  SecureConversationContextManager.getInstance());
        } catch (WssProcessor.ProcessorException e) {
            logger.log(Level.SEVERE, "Error in WSS processing of request", e);
            // todo, some error
        } catch (InvalidDocumentFormatException e) {
            logger.log(Level.SEVERE, "Error in WSS processing of request", e);
            // todo, some error
        } catch (GeneralSecurityException e) {
            logger.log(Level.SEVERE, "Error in WSS processing of request", e);
            // todo, some error
        }
        WssProcessor.SecurityToken[] tokens = wssOutput.getSecurityTokens();
        X509Certificate clientCert = null;
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken token = tokens[i];
            if (token instanceof WssProcessor.X509SecurityToken) {
                WssProcessor.X509SecurityToken x509token = (WssProcessor.X509SecurityToken)token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        logger.log( Level.WARNING, "Request included more than one X509 security token whose key ownership was proven" );
                        // todo, some error
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
            // todo, some error
        }
        User authenticatedUser = authenticator.authenticate(new LoginCredentials(certCN,
                                                                                 null,
                                                                                 CredentialFormat.CLIENTCERT,
                                                                                 null,
                                                                                 null,
                                                                                 clientCert));

        if (isValidRequestForSecureConversationContext(request, wssOutput)) {
            Document response = handleSecureConversationContextRequest(authenticatedUser, clientCert);
        } else if (isValidRequestForSAMLToken(request, wssOutput)) {
            // todo, plug in your saml handling here alex --fla
        } else {
            throw new InvalidDocumentFormatException("This request cannot be recognized as a valid " +
                                                     "RequestSecurityToken");
        }
        return null;
    }

    private Document handleSecureConversationContextRequest(User requestor, X509Certificate requestorCert) {
        // todo
        return null;
    }

    private boolean isValidRequestForSecureConversationContext(Document request, WssProcessor.ProcessorResult wssOutput) {
        // todo
        return true;
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
}
