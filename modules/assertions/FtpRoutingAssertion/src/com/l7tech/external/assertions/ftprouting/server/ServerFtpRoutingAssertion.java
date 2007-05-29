/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.Ftp;
import com.jscape.inet.ftp.FtpException;
import com.jscape.inet.ftps.Ftps;
import com.jscape.inet.ftps.FtpsCertificateVerifier;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.transport.ftp.FtpTestException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.external.assertions.ftprouting.FtpCredentialsSource;
import com.l7tech.external.assertions.ftprouting.FtpFileNameSource;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.external.assertions.ftprouting.FtpSecurity;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.VariableMap;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
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
    private final Auditor _auditor;
    private final TrustedCertManager _trustedCertManager;

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
        private final TrustedCertManager _trustedCertManager;
        private final String _hostName;
        private boolean _authorized = false;
        private FtpException _exception;

        public CertificateVerifier(TrustedCertManager trustedCertManager, String hostName) {
            assert(trustedCertManager != null);
            assert(hostName != null);
            _trustedCertManager = trustedCertManager;
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
                _trustedCertManager.checkSslTrust(x509certs);
                _authorized = true;
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
        _trustedCertManager = (TrustedCertManager)applicationContext.getBean("trustedCertManager", TrustedCertManager.class);
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
            final LoginCredentials credentials = context.getCredentials();
            if (credentials != null) {
                userName = credentials.getName();
                password = new String(credentials.getCredentials());
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
                doFtp(userName, password, messageBodyStream, fileName);
            } else if (security == FtpSecurity.FTPS_EXPLICIT) {
                doFtps(true, userName, password, messageBodyStream, fileName);
            } else if (security == FtpSecurity.FTPS_IMPLICIT) {
                doFtps(false, userName, password, messageBodyStream, fileName);
            }
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Unable to get request body.", e);
        } catch (FtpException e) {
            _auditor.logAndAudit(AssertionMessages.FTP_ROUTING_FAILED_UPLOAD, assertion.getHostName(), e.getMessage());
            return AssertionStatus.FAILED;
        }
    }

    private void doFtp(String userName,
                       String password,
                       InputStream is,
                       String fileName) throws FtpException {
        final Ftp ftp = newFtpConnection(assertion.getHostName(),
                                         assertion.getPort(),
                                         userName,
                                         password,
                                         assertion.getDirectory(),
                                         assertion.getTimeout(),
                                         null);
        try {
            ftp.upload(is, fileName);
        } finally {
            ftp.disconnect();
        }
    }

    private void doFtps(boolean isExplicit,
                        String userName,
                        String password,
                        InputStream is,
                        String fileName) throws FtpException {
        final Ftps ftps = newFtpsConnection(assertion.isVerifyServerCert(),
                                            isExplicit,
                                            assertion.getHostName(),
                                            assertion.getPort(),
                                            userName,
                                            password,
                                            assertion.getDirectory(),
                                            assertion.getTimeout(),
                                            null,
                                            _trustedCertManager);
        try {
            ftps.upload(is, fileName);
        } finally {
            ftps.disconnect();
        }
    }

    private String expandVariables(PolicyEnforcementContext context, String pattern) {
        final String[] variablesUsed = ExpandVariables.getReferencedNames(pattern);
        final VariableMap vars = context.getVariableMap(variablesUsed, _auditor);
        return ExpandVariables.process(pattern, vars);
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
     * @param directory             remote directory to "cd" into; supply empty string if no "cd" wanted
     * @param debugStream           an opened stream to receive server responses; can be null
     * @param trustedCertManager    must not be null if <code>isVerifyServerCert</code> is true
     * @return a new Ftps object in connected state
     * @throws FtpException if failure
     */
    private static Ftps newFtpsConnection(boolean isVerifyServerCert,
                                          boolean isExplicit,
                                          String hostName,
                                          int port,
                                          String userName,
                                          String password,
                                          String directory,
                                          int timeout,
                                          PrintStream debugStream,
                                          TrustedCertManager trustedCertManager) throws FtpException {
        assert(!isVerifyServerCert || trustedCertManager != null);
        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(directory != null);

        final Ftps ftps = new Ftps(hostName, userName, password, port);
        if (debugStream != null) {
            ftps.setDebugStream(debugStream);
            ftps.setDebug(true);
        }
        ftps.setTimeout(timeout);

        CertificateVerifier certificateVerifier = null;
        if (isVerifyServerCert) {
            certificateVerifier = new CertificateVerifier(trustedCertManager, hostName);
            ftps.setFtpsCertificateVerifier(certificateVerifier);
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
     * Called by {@link com.l7tech.server.transport.ftp.FtpAdminImpl#testConnection} using reflection.
     *
     * @param isFtps                true if FTPS; false if FTP (unsecured)
     * @param isVerifyServerCert    whether to verify FTP server certificate using trusted certificate store
     * @param isExplicit            if FTPS: true if explicit FTPS, false if implicit FTPS
     * @param directory             remote directory to "cd" into; supply empty string if no "cd" wanted
     * @param trustedCertManager    must not be null if <code>isVerifyServerCert</code> is true
     */
    public static void testConnection(boolean isFtps,
                                      boolean isVerifyServerCert,
                                      boolean isExplicit,
                                      String hostName,
                                      int port,
                                      String userName,
                                      String password,
                                      String directory,
                                      int timeout,
                                      TrustedCertManager trustedCertManager) throws FtpTestException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream debugStream = new PrintStream(baos);
        Ftp ftp = null;
        Ftps ftps = null;
        try {
            if (isFtps) {
                ftps = newFtpsConnection(isVerifyServerCert,
                                         isExplicit,
                                         hostName,
                                         port,
                                         userName,
                                         password,
                                         directory,
                                         timeout,
                                         debugStream,
                                         trustedCertManager);
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
