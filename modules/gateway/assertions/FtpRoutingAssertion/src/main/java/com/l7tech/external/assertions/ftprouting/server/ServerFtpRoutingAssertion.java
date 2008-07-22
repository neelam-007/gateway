/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.Ftp;
import com.jscape.inet.ftp.FtpException;
import com.jscape.inet.ftps.Ftps;
import com.jscape.inet.ftps.FtpsCertificateVerifier;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.ftp.FtpTestException;
import com.l7tech.util.CausedIOException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.external.assertions.ftprouting.FtpCredentialsSource;
import com.l7tech.external.assertions.ftprouting.FtpFileNameSource;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.external.assertions.ftprouting.FtpSecurity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assertion that routes the request to an FTP server.
 *
 * @since SecureSpan 4.0
 * @author rmak
 */
public class ServerFtpRoutingAssertion extends ServerRoutingAssertion<FtpRoutingAssertion> {
    private static final Logger _logger = Logger.getLogger(ServerFtpRoutingAssertion.class.getName());
    private static final Random _random = new Random(System.currentTimeMillis());
    private final Auditor _auditor;
    private final X509TrustManager _trustManager;
    private final SsgKeyStoreManager _ssgKeyStoreManager;


    /**
     * FTPS certificate verifier.
     *
     * <p>This is the way a verifier works: During {@link Ftps#connect}, the
     * authorized() method gets invoked first. If authorized() returns false,
     * then the verify() method will be invoked to verify the certificate,
     * after which the authorized() method is invoked again. If it still
     * returns false, an FtpException is thrown with the message
     * "Could not authenticate when has not been authorized". It's not a very
     * clear message so we provide a {@link #throwIfFailed} method to return a
     * better exception message.
     */
    private static class CertificateVerifier implements FtpsCertificateVerifier {
        private final X509TrustManager _trustManager;
        private final String _hostName;
        private boolean _authorized = false;
        private FtpException _exception;

        public CertificateVerifier(X509TrustManager trustManager, String hostName) {
            assert(trustManager != null);
            assert(hostName != null);
            _trustManager = trustManager;
            _hostName = hostName;
        }

        public boolean authorized() {
            return _authorized;
        }

        public void verify(SSLSession sslSession) {
            if (_logger.isLoggable(Level.FINEST)) {
                _logger.finest("Verifying FTP server (" + _hostName + ") SSL certificate using trusted certificate store.");
            }
            Certificate[] certs = null;
            try {
                certs = sslSession.getPeerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                _exception = new FtpException(MessageFormat.format(AssertionMessages.FTP_ROUTING_SSL_NO_CERT.getMessage(), _hostName, e.getMessage()));
                return;
            }
            final X509Certificate[] x509certs = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; ++ i) {
                if (certs[i] instanceof X509Certificate) {
                    x509certs[i] = (X509Certificate)certs[i];
                } else {
                    _exception = new FtpException(MessageFormat.format(AssertionMessages.FTP_ROUTING_SSL_NOT_X509.getMessage(), _hostName));
                    return;
                }
            }
            try {
                _trustManager.checkServerTrusted(x509certs, CertUtils.extractAuthType(sslSession.getCipherSuite()));
                _authorized = true;
                _exception = null;
            } catch (CertificateException e) {
                _exception = new FtpException(MessageFormat.format(AssertionMessages.FTP_ROUTING_SSL_UNTRUSTED.getMessage(), _hostName, e.getMessage()));
                return;
            }
        }

        /**
         * @throws FtpException if an exception was encountered during {@link #verify}.
         */
        public void throwIfFailed() throws FtpException {
            if (_exception != null) throw _exception;
        }
    }

    public ServerFtpRoutingAssertion(FtpRoutingAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext, _logger);
        _auditor = new Auditor(this, applicationContext, _logger);
        _trustManager = (X509TrustManager)applicationContext.getBean("routingTrustManager", X509TrustManager.class);
        _ssgKeyStoreManager = (SsgKeyStoreManager)applicationContext.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);

    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();

        final MimeKnob mimeKnob = (MimeKnob) request.getKnob(MimeKnob.class);

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        if (request.isXml()) {
            try {
                handleProcessedSecurityHeader(context,
                                              data.getCurrentSecurityHeaderHandling(),
                                              data.getXmlSecurityActorToPromote());
            } catch(SAXException se) {
                _logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
            }
        }

        String userName = null;
        String password = null;
        if (assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            final LoginCredentials credentials = context.getLastCredentials();
            if (credentials != null) {
                userName = credentials.getName();
                password = new String(credentials.getCredentials());
            }
            if (userName == null) {
                _auditor.logAndAudit(AssertionMessages.FTP_ROUTING_PASSTHRU_NO_USERNAME);
                return AssertionStatus.FAILED;
            }
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            userName = assertion.getUserName();
            password = assertion.getPassword();
        }

        String fileName = null;
        if (assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
            // Cannot use STOU because
            // {@link com.jscape.inet.ftp.Ftp.uploadUnique(InputStream, String)}
            // sends a parameter as filename seed, which causes IIS to respond
            // with "500 'STOU seed': Invalid number of parameters".
            // This was reported (2007-05-07) to JSCAPE, who said they will add
            // a method to control STOU parameter.
            fileName = context.getRequestId().toString();
        } else if (assertion.getFileNameSource() == FtpFileNameSource.PATTERN) {
            fileName = expandVariables(context, assertion.getFileNamePattern());
        }

        try {
            final InputStream messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream();
            final FtpSecurity security = assertion.getSecurity();
            if (security == FtpSecurity.FTP_UNSECURED) {
                doFtp(context, userName, password, messageBodyStream, fileName);
            } else if (security == FtpSecurity.FTPS_EXPLICIT) {
                doFtps(context, true, userName, password, messageBodyStream, fileName);
            } else if (security == FtpSecurity.FTPS_IMPLICIT) {
                doFtps(context, false, userName, password, messageBodyStream, fileName);
            }
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Unable to get request body.", e);
        } catch (FtpException e) {
            _auditor.logAndAudit(AssertionMessages.FTP_ROUTING_FAILED_UPLOAD, assertion.getHostName(), e.getMessage());
            return AssertionStatus.FAILED;
        }
    }

    private String getDirectory(PolicyEnforcementContext context, FtpRoutingAssertion assertion) {
        return expandVariables(context, assertion.getDirectory());
    }

    private void doFtp(PolicyEnforcementContext context,
                       String userName,
                       String password,
                       InputStream is,
                       String fileName) throws FtpException {
        final Ftp ftp = newFtpConnection(assertion.getHostName(),
                                         assertion.getPort(),
                                         userName,
                                         password,
                                         getDirectory(context, assertion),
                                         assertion.getTimeout(),
                                         null);
        try {
            ftp.upload(is, fileName);
        } finally {
            ftp.disconnect();
        }
    }

    private void doFtps(PolicyEnforcementContext context,
                        boolean isExplicit,
                        String userName,
                        String password,
                        InputStream is,
                        String fileName) throws FtpException {
        final Ftps ftps = newFtpsConnection(isExplicit,
                                            assertion.isVerifyServerCert(),
                                            assertion.getHostName(),
                                            assertion.getPort(),
                                            userName,
                                            password,
                                            assertion.isUseClientCert(),
                                            assertion.getClientCertKeystoreId(),
                                            assertion.getClientCertKeyAlias(),
                                            getDirectory(context, assertion),
                                            assertion.getTimeout(),
                                            null,
                                            _trustManager,
                                            _ssgKeyStoreManager);
        try {
            ftps.upload(is, fileName);
        } finally {
            ftps.disconnect();
        }
    }

    private String expandVariables(PolicyEnforcementContext context, String pattern) {
        final String[] variablesUsed = Syntax.getReferencedNames(pattern);
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, _auditor);
        return ExpandVariables.process(pattern, vars, _auditor);
    }

    /**
     * Creates a new FTP connection.
     *
     * @param directory     remote directory to "cd" into; supply empty string if no "cd" wanted
     * @param debugStream   an opened stream to receive server responses; can be null
     * @return a new Ftp object in connected state
     */
    private static Ftp newFtpConnection(String hostName,
                                        int port,
                                        String userName,
                                        String password,
                                        String directory,
                                        int timeout,
                                        PrintStream debugStream) throws FtpException {
        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(directory != null);

        final Ftp ftp = new Ftp(hostName, userName, password, port);
        if (debugStream != null) {
            ftp.setDebugStream(debugStream);
            ftp.setDebug(true);
        }
        ftp.setTimeout(timeout);
        ftpConnect(ftp);
        try {
            if (directory.length() != 0) {
                ftp.setDir(directory);
            }
            ftp.setAuto(false);
            ftp.setBinary();
        } catch (FtpException e) {
            ftp.disconnect();   // Closes connection before letting exception bubble up.
            throw e;
        }
        return ftp;
    }

    /**
     * Creates a new FTPS connection.
     *
     * @param isExplicit            if FTPS: true if explicit FTPS, false if implicit FTPS
     * @param isVerifyServerCert    whether to verify FTPS server certificate using trusted certificate store; applies only if isFtps is true
     * @param hostName              host name of FTP(S) server
     * @param port                  port number of FTP(S) server
     * @param userName              user name to login in as
     * @param password              password to login with
     * @param useClientCert         whether to use client cert and private key for authentication
     * @param clientCertKeystoreId  ID of keystore to use if useClientCert is true; must be a valid ID if useClientCert is true
     * @param clientCertKeyAlias    key alias in keystore to use if useClientCert is true; must not be null if useClientCert is true
     * @param directory             remote directory to "cd" into; supply empty string if no "cd" wanted
     * @param timeout               connection timeout in milliseconds
     * @param debugStream           an opened stream to receive server responses; can be null
     * @param trustManager          must not be null if isVerifyServerCert is true
     * @param ssgKeyStoreManager    must not be null if useClientCert is true
     * @return a new Ftps object in connected state
     * @throws FtpException if failure
     */
    private static Ftps newFtpsConnection(boolean isExplicit,
                                          boolean isVerifyServerCert,
                                          String hostName,
                                          int port,
                                          String userName,
                                          String password,
                                          boolean useClientCert,
                                          long clientCertKeystoreId,
                                          String clientCertKeyAlias,
                                          String directory,
                                          int timeout,
                                          PrintStream debugStream,
                                          X509TrustManager trustManager,
                                          SsgKeyStoreManager ssgKeyStoreManager) throws FtpException {
        assert(!isVerifyServerCert || trustManager != null);
        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(!useClientCert || (clientCertKeystoreId != -1 && clientCertKeyAlias != null));
        assert(directory != null);

        final Ftps ftps = new Ftps(hostName, userName, password, port);
        if (debugStream != null) {
            ftps.setDebugStream(debugStream);
            ftps.setDebug(true);
        }
        ftps.setTimeout(timeout);

        CertificateVerifier certificateVerifier = null;
        if (isVerifyServerCert) {
            certificateVerifier = new CertificateVerifier(trustManager, hostName);
            ftps.setFtpsCertificateVerifier(certificateVerifier);
        }

        if (useClientCert) {
            try {
                // Retrieves the private key and cert.
                final SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(clientCertKeystoreId);
                final SsgKeyEntry keyEntry = keyFinder.getCertificateChain(clientCertKeyAlias);
                final X509Certificate[] certChain = keyEntry.getCertificateChain();
                final PrivateKey privateKey = keyEntry.getPrivateKey();

                // Creates a KeyStore object with a random password.
                final byte[] randomBytes = new byte[16];
                _random.nextBytes(randomBytes);
                final String privateKeyPassword = HexUtils.encodeBase64(randomBytes);
                final KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                final String alias = "ftp";
                keyStore.setKeyEntry(alias, privateKey, privateKeyPassword.toCharArray(), certChain);

                ftps.setClientCertificates(keyStore, privateKeyPassword);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Assigned private key and certificate for FTPS client authentication. (key alias=" + clientCertKeyAlias + ")");
                }
            } catch (Exception e) {
                final StringBuilder msg = new StringBuilder("Cannot create keystore from private key (key alias=" + clientCertKeyAlias + ") for authentication: " + e.toString());
                if (e.getCause() != null) {
                    msg.append(": " + e.getCause().toString());
                }
                throw new FtpException(msg.toString());
            }
        }

        if (isExplicit) {
            // Try AUTH TLS first. If that fails, then try AUTH SSL.
            // We cannot use FEAT to check since not implemented by all FTP servers.
            try {
                ftpsConnect(ftps);  // Connects using AUTH TLS (default).
            } catch (FtpException e) {
                if (e.getException() instanceof UnknownHostException) {
                    // If the failure was caused by host name problem, no need to
                    // retry using AUTH SSL.
                    throw e;
                }
                if (certificateVerifier != null) {
                    // If the failure was caused by cert problem, no need to
                    // retry using AUTH SSL; and we will replace the exception
                    // with our own because ours has more specific message.
                    certificateVerifier.throwIfFailed();
                }
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Unable to connect using FTPS AUTH TLS. Retrying with AUTH SSL.");
                }
                ftps.setConnectionType(Ftps.AUTH_SSL);
                ftpsConnect(ftps);
            }
        } else {
            ftps.setConnectionType(Ftps.IMPLICIT_SSL);
            try {
                ftpsConnect(ftps);
            } catch (FtpException e) {
                if (certificateVerifier != null) {
                    // If the failure was caused by cert problem, then replaces the
                    // exception with our own because ours has more specific message.
                    certificateVerifier.throwIfFailed();
                }
                throw e;
            }
        }

        try {
            if (directory.length() != 0) {
                ftps.setDir(directory);
            }
            ftps.setAuto(false);
            ftps.setBinary();
        } catch (FtpException e) {
            ftps.disconnect();  // Closes connection before letting exception bubble up.
            throw e;
        }
        return ftps;
    }

    /**
     * Wrapper method to call {@link Ftp#connect} that throws a better exception.
     *
     * The problem with calling {@link Ftp#connect} directly is that when the
     * host is unavailable, it throws an exception with a message containing
     * just the host name with no description. This wrapper replaces that with a
     * clearer message.
     */
    private static void ftpConnect(Ftp ftp) throws FtpException {
        try {
            ftp.connect();
        } catch (FtpException e) {
            final Exception cause = e.getException();
            if (cause instanceof UnknownHostException) {
                e = new FtpException("Unknown host: " + ftp.getHostname(), cause);
            }
            throw e;
        }
    }

    /**
     * Wrapper method to call {@link Ftps#connect} that throws a better exception.
     *
     * The problem with calling {@link Ftps#connect} directly is that when the
     * host is unavailable, it throws an exception with a message containing
     * just the host name with no description. This wrapper replaces that with a
     * clearer message.
     */
    private static void ftpsConnect(Ftps ftps) throws FtpException {
        try {
            ftps.connect();
        } catch (FtpException e) {
            final Exception cause = e.getException();
            if (cause instanceof UnknownHostException) {
                e = new FtpException("Unknown host: " + ftps.getHostname(), cause);
            }
            throw e;
        }
    }

    /**
     * Tests connection to FTP(S) server and tries "cd" into remote directory.
     *
     * Called by {@link com.l7tech.server.transport.ftp.FtpAdminImpl#testConnection} using reflection.
     *
     * @param isFtps                true if FTPS; false if FTP (unsecured)
     * @param isVerifyServerCert    whether to verify FTP server certificate using trusted certificate store
     * @param isExplicit            if FTPS: true if explicit FTPS, false if implicit FTPS
     * @param hostName              host name of FTP(S) server
     * @param port                  port number of FTP(S) server
     * @param userName              user name to login in as
     * @param password              password to login with
     * @param useClientCert         whether to use client cert and private key for authentication if isFtps is true
     * @param clientCertKeystoreId  ID of keystore to use if useClientCert is true; must be a valid ID if useClientCert is true
     * @param clientCertKeyAlias    key alias in keystore to use if useClientCert is true; must not be null if useClientCert is true
     * @param directory             remote directory to "cd" into; supply empty string if no "cd" wanted
     * @param timeout               connection timeout in milliseconds
     * @param trustManager          must not be null if isVerifyServerCert is true
     * @param ssgKeyStoreManager    must not be null if useClientCert is true
     * @throws FtpTestException if connection test failed
     */
    public static void testConnection(boolean isFtps,
                                      boolean isExplicit,
                                      boolean isVerifyServerCert,
                                      String hostName,
                                      int port,
                                      String userName,
                                      String password,
                                      boolean useClientCert,
                                      long clientCertKeystoreId,
                                      String clientCertKeyAlias,
                                      String directory,
                                      int timeout,
                                      X509TrustManager trustManager,
                                      SsgKeyStoreManager ssgKeyStoreManager) throws FtpTestException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream debugStream = new PrintStream(baos);
        Ftp ftp = null;
        Ftps ftps = null;
        try {
            if (isFtps) {
                ftps = newFtpsConnection(isExplicit,
                                         isVerifyServerCert,
                                         hostName,
                                         port,
                                         userName,
                                         password,
                                         useClientCert,
                                         clientCertKeystoreId,
                                         clientCertKeyAlias,
                                         directory,
                                         timeout,
                                         debugStream,
                                         trustManager,
                                         ssgKeyStoreManager);
            } else {
                ftp = newFtpConnection(hostName,
                                       port,
                                       userName,
                                       password,
                                       directory,
                                       timeout,
                                       debugStream);
            }
        } catch (FtpException e) {
            throw new FtpTestException(e.getMessage(), baos.toString());
        } finally {
            if (ftp != null) ftp.disconnect();
            if (ftps != null) ftps.disconnect();
            debugStream.close();
        }
    }
}
