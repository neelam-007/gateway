package com.l7tech.server.secureconversation;

import org.w3c.dom.Document;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssProcessorImpl;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import javax.crypto.SecretKey;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * Hhandles WS Trust RequestSecurityToken requests.
 * The SSA requests such a token when it desires to establish a Secure Conversation.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 6, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationTokenService {

    public interface CredentialsAuthenticator {
        User authenticate(LoginCredentials creds);
    }

    /**
     * Handles the request for a secure conversation security token.
     * @param request the request for the secure conversation context
     * @param manager the manager for the secure conversation context
     * @param creds resolved credentials such as an X509Certificate to an actual user to associate the context with
     * @return
     */
    public Document respondToRequestSecurityToken(Document request, SecureConversationContextManager manager, CredentialsAuthenticator creds) {
        // todo
        return null;
    }


    /*

    public SecureConversationSession getNewContext(Document request) {
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

        // This makes sure that the right elements were signed and that the body of the request is making sense
        validateRequest(wssOutput);

        // For now, we only support signed requests
        if (clientCert == null) {
            // todo, some error (401?)
        }
        User authenticatedUser = getUserFromCert(clientCert);
        if (authenticatedUser == null) {
            // todo, some error (401?)
        }

        String newSessionIdentifier = "http://www.layer7tech.com/uuid/" + randomuuid();
        final byte[] sharedSecret = generateNewSecret();
        // make up a new session identifier and shared secret (using some random generator)
        SecureConversationSession session = new SecureConversationSession();
        session.setCreation(System.currentTimeMillis());
        session.setExpiration(System.currentTimeMillis() + DEFAULT_SESSION_DURATION);
        session.setIdentifier(newSessionIdentifier);
        session.setSharedSecret(new SecretKey() {
            public byte[] getEncoded() {
                return sharedSecret;
            }
            public String getAlgorithm() {
                return "l7 shared secret";
            }
            public String getFormat() {
                return "l7 shared secret";
            }
        });
        session.setUsedBy(authenticatedUser);
        try {
            SecureConversationContextManager.getInstance().saveSession(session);
        } catch (DuplicateSessionException e) {
            // todo, some error
        }

        return null;
    }

    public Document sessionToRequestSecurityTokenResponse(SecureConversationSession session) {
        // todo
        return null;
    }

    private User getUserFromCert(X509Certificate clientCert) {
        IdentityProviderConfigManager idpcm = (IdentityProviderConfigManager)Locator.getDefault().lookup(IdentityProviderConfigManager);
        Collection providers = null;
        try {
            providers = IdentityProviderFactory.findAllIdentityProviders(idpcm);
        } catch (FindException e) {

        }
        // todo, go through providers, find user matching this cert
        return null;
    }

    private void validateRequest(WssProcessor.ProcessorResult wssOutput) {
        // todo, verify that the right elements were signed
        // todo, verify that this is indeed a RequestSecurityToken requests (by inspecting the body)
    }

    private byte[] generateNewSecret() {
        // todo, return some random secret
        return new byte[0];
    }

    private String randomuuid() {
        // todo, return some random uuid
        return null;
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
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs?

    private final Logger logger = Logger.getLogger(getClass().getName());*/
}
