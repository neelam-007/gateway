package com.l7tech.server;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssProcessorImpl;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.message.HttpSoapRequest;
import com.l7tech.message.HttpTransportMetadata;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import com.l7tech.identity.User;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKey;
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
 * The servlet handling WS Trust RequestSecurityToken requests.
 * The SSA requests such a token when it desires to establish a Secure Conversation.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 5, 2004<br/>
 * $Id$<br/>
 */
public class TokenServiceServlet extends HttpServlet {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; context requests must use POST");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpTransportMetadata htm = new HttpTransportMetadata(req, res);
        HttpSoapRequest soapReq = new HttpSoapRequest(htm);
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
        }
        // Authenticate the request, check who signed it
        WssProcessor.ProcessorResult wssOutput = null;
        try {
            wssOutput = trogdor.undecorateMessage(soapReq.getDocument(),
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
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Error getting xml document from request", e);
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
        outputRequestSecurityTokenResponse(session, res, clientCert);
    }

    private void outputRequestSecurityTokenResponse(SecureConversationSession session,
                                                    HttpServletResponse res,
                                                    X509Certificate requestorCert) {
        // todo, send back the RequestSecurityTokenResponse to the requestor
    }

    private User getUserFromCert(X509Certificate clientCert) {
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
    private final Logger logger = Logger.getLogger(getClass().getName());
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs?
}
