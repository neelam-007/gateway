package com.l7tech.server.transport.ftp;

import com.jscape.inet.ftp.Ftp;
import com.jscape.inet.ftp.FtpException;
import com.jscape.inet.ftps.Ftps;
import com.jscape.inet.ftps.FtpsCertificateVerifier;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Utility class for building Ftp/Ftps clients in the connected state, ready to use for file transfers.
 *
 * Non Ftps functionality is provided by com.l7tech.gateway.common.transport.ftp.FtpUtils
 *
 * @author jbufu
 */
public class FtpClientUtils {

    private static final Logger logger = Logger.getLogger(FtpClientUtils.class.getName());
    private static final Random random = new Random(System.currentTimeMillis());
    private static final SecureRandom secureRandom = new SecureRandom();


    private FtpClientUtils() {
    }

    /**
     * Convenience proxy method for {@link com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl#newFtpConfig(String)}
     *
     * @param host
     * @return
     */
    public static FtpClientConfig newConfig(String host) {
        return FtpClientConfigImpl.newFtpConfig(host);
    }

    public static Ftps newFtpsClient(FtpClientConfig config) throws FtpException {
        return newFtpsClient(config, null, null, null);
    }

    public static Ftps newFtpsClient(FtpClientConfig config, DefaultKey keyFinder) throws FtpException {
        return newFtpsClient(config, keyFinder, null, null);
    }

    public static Ftps newFtpsClient(FtpClientConfig config, X509TrustManager trustManager, HostnameVerifier hostnameVerifier) throws FtpException {
        return newFtpsClient(config, null, trustManager, hostnameVerifier);
    }

    /**
     * Creates a new, connected FTPS client using the provided FTP configuration.
     */
    public static Ftps newFtpsClient( final FtpClientConfig config,
                                      final DefaultKey keyFinder,
                                      final X509TrustManager trustManager,
                                      final HostnameVerifier hostnameVerifier ) throws FtpException {
        if (FtpCredentialsSource.SPECIFIED != config.getCredentialsSource())
            throw new IllegalStateException("Cannot create FTP connection if credentials are not specified.");

        if (FtpSecurity.FTP_UNSECURED == config.getSecurity())
            throw new NullPointerException("Cannot generate FTPS connection for the configured FTP configuration.");

        if (config.isVerifyServerCert() && trustManager == null)
            throw new NullPointerException("Server certificate manager cannot be null if sever certificate validation is required.");

        final Ftps ftps = new Ftps(config.getHost(), config.getUser(), config.getPass(), config.getPort());
        if (config.getDebugStream() != null) {
            ftps.setDebugStream(config.getDebugStream());
            ftps.setDebug(true);
        }
        ftps.setTimeout(config.getTimeout());

        CertificateVerifier certificateVerifier = null;
        if (config.isVerifyServerCert()) {
            certificateVerifier = new CertificateVerifier(trustManager, hostnameVerifier, config.getHost());
            ftps.setFtpsCertificateVerifier(certificateVerifier);
        }

        final SsgKeyEntry keyEntry;
        if (config.isUseClientCert()) {
            if (keyFinder == null)
                throw new IllegalArgumentException("Authentication with client certificate requested, but (default) key finder was not provided.");

            try {
                // Retrieves the private key and cert.
                keyEntry = keyFinder.lookupKeyByKeyAlias(config.getClientCertAlias(), config.getClientCertId());
                final X509Certificate[] certChain = keyEntry.getCertificateChain();
                final PrivateKey privateKey = keyEntry.getPrivateKey();

                // Creates a fake in-memory KeyStore with a weakly-random passphrase.
                final byte[] randomBytes = new byte[16];
                random.nextBytes(randomBytes);
                final String privateKeyPassword = HexUtils.encodeBase64(randomBytes);
                Provider keyprov = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_KEYSTORE_PKCS12);
                final KeyStore keyStore = keyprov == null ? KeyStore.getInstance("PKCS12") : KeyStore.getInstance("PKCS12", keyprov);
                keyStore.load(null, null);
                final String alias = "ftp";
                keyStore.setKeyEntry(alias, privateKey, privateKeyPassword.toCharArray(), certChain);

                ftps.setClientCertificates(keyStore, privateKeyPassword);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Assigned private key and certificate for FTPS client authentication. (key alias=" + config.getClientCertAlias() + ")");
                }
            } catch (Exception e) {
                final StringBuilder msg = new StringBuilder("Cannot create keystore from private key (key alias=" + config.getClientCertAlias() + ") for authentication: " + e.toString());
                if (e.getCause() != null) {
                    msg.append(": ").append(e.getCause().toString());
                }
                throw new FtpException(msg.toString());
            }
        } else {
            keyEntry = null;
        }

        if (FtpSecurity.FTPS_EXPLICIT == config.getSecurity()) {
            // Try AUTH TLS first. If that fails, then try AUTH SSL.
            // We cannot use FEAT to check since not implemented by all FTP servers.
            try {
                ftpsConfigureSslContext(ftps, keyEntry, trustManager);
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
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Unable to connect using FTPS AUTH TLS. Retrying with AUTH SSL.");
                }
                ftps.setConnectionType(Ftps.AUTH_SSL);
                ftpsConnect(ftps);
            } catch (GeneralSecurityException e) {
                throw new FtpException("Unable to initialize TLS SSLContext: " + ExceptionUtils.getMessage(e), e);
            }
        } else {
            ftps.setConnectionType(Ftps.IMPLICIT_SSL);
            try {
                ftpsConfigureSslContext(ftps, keyEntry, trustManager);
                ftpsConnect(ftps);
            } catch (FtpException e) {
                if (certificateVerifier != null) {
                    // If the failure was caused by cert problem, then replaces the
                    // exception with our own because ours has more specific message.
                    certificateVerifier.throwIfFailed();
                }
                throw e;
            } catch (GeneralSecurityException e) {
                throw new FtpException("Unable to initialize TLS SSLContext: " + ExceptionUtils.getMessage(e), e);
            }
        }

        String directory = config.getDirectory();
        try {
            if (directory != null && directory.length() != 0) {
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

    private static void ftpsConfigureSslContext(Ftps ftps, SsgKeyEntry keyEntry, TrustManager trustManager) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        // Always use TLS 1.0 provider (SunJSSE) for outbound FTPS (Bug #8630)
        Provider tlsProv = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_TLS10);
        SSLContext sslContext = tlsProv == null ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", tlsProv);
        JceProvider.getInstance().prepareSslContext( sslContext );

        List<KeyManager> keymans = new ArrayList<KeyManager>();
        if (keyEntry != null) {
            keymans.add(new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivateKey() ));
        }

        List<TrustManager> trustmans = new ArrayList<TrustManager>();
        if (trustManager != null) {
            // TODO using the real trust manager here may cause validation to be done twice, but seems safer than not doing it
            trustmans.add(trustManager);
        } else {
            trustmans.add(new PermissiveX509TrustManager());
        }

        sslContext.init(keymans.toArray(new KeyManager[keymans.size()]), trustmans.toArray(new TrustManager[trustmans.size()]), secureRandom);

        ftps.setContext(sslContext);
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
     * Tests connection to FTPS server and tries "cd" into remote directory.
     *
     * @throws com.l7tech.gateway.common.transport.ftp.FtpTestException if connection test failed
     */
    public static void testFtpsConnection( final FtpClientConfig config,
                                           final DefaultKey keyFinder,
                                           final X509TrustManager trustManager,
                                           final HostnameVerifier hostnameVerifier ) throws FtpTestException {
        // provide our own debug if the client is not watching the logs
        ByteArrayOutputStream baos = null;
        if (config.getDebugStream() == null) {
            baos = new ByteArrayOutputStream();
            config.setDebugStream(new PrintStream(baos));
        }

        Ftps ftps = null;
        try {
            ftps = newFtpsClient(config, keyFinder, trustManager, hostnameVerifier);
        } catch (FtpException e) {
            throw new FtpTestException(e.getMessage(), baos != null ? baos.toString() : null);
        } finally {
            if (ftps != null) ftps.disconnect();
            if (baos != null) {
                config.getDebugStream().close();
                config.setDebugStream(null);
            }
        }
    }

    public static void upload(FtpClientConfig config, InputStream is, long count, String filename) throws FtpException {
        upload(config, is, count, filename, null, null, null);
    }

    public static void upload( final FtpClientConfig config,
                               final InputStream is,
                               final long count,
                               final String filename,
                               final DefaultKey keyFinder,
                               final X509TrustManager trustManager,
                               final HostnameVerifier hostnameVerifier ) throws FtpException {

        if (FtpSecurity.FTP_UNSECURED == config.getSecurity()) {
            FtpUtils.upload(config, is, count, filename);
        } else {
            final Ftps ftps = FtpClientUtils.newFtpsClient(config, keyFinder, trustManager, hostnameVerifier);

            final FtpUtils.FtpUploadSizeListener listener = new FtpUtils.FtpUploadSizeListener();
            try {
                ftps.addFtpListener( listener );
                ftps.upload(is, filename);
            } finally {
                ftps.disconnect();
            }

            if ( listener.isError() ) {
                throw new FtpException("File '"+filename+"' upload error '"+listener.getError()+"'.");
            } else if ( listener.getSize() < count ) {
               throw new FtpException("File '"+filename+"' upload truncated to " + listener.getSize() + " bytes.");
            }
        }
    }

    public static OutputStream getUploadOutputStream(FtpClientConfig config, String filename) throws FtpException {
        return getUploadOutputStream(config, filename, null, null, null);
    }

    public static OutputStream getUploadOutputStream( final FtpClientConfig config,
                                                      final String filename,
                                                      final DefaultKey keyFinder,
                                                      final X509TrustManager trustManager,
                                                      final HostnameVerifier hostnameVerifier ) throws FtpException {
        if (FtpSecurity.FTP_UNSECURED == config.getSecurity()) {
            Ftp ftp = FtpUtils.newFtpClient(config);
            return ftp.getOutputStream(filename, 0, false);
        } else {
            Ftps ftps = FtpClientUtils.newFtpsClient(config, keyFinder, trustManager, hostnameVerifier);
            try {
            return ftps.getOutputStream(filename, 0, false);
            } catch (IOException e) {
                throw new FtpException("Error getting upload output stream.", e);
            }
        }
    }

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
        private final HostnameVerifier _hostnameVerifier;
        private final String _hostName;
        private boolean _authorized = false;
        private FtpException _exception;

        public CertificateVerifier(X509TrustManager trustManager, HostnameVerifier hostnameVerifier, String hostName) {
            assert(trustManager != null);
            assert(hostName != null);
            _trustManager = trustManager;
            _hostnameVerifier = hostnameVerifier;
            _hostName = hostName;
        }

        @Override
        public boolean authorized() {
            return _authorized;
        }

        @Override
        public void verify(SSLSession sslSession) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Verifying FTP server (" + _hostName + ") SSL certificate using trusted certificate store.");
            }
            Certificate[] certs;
            try {
                certs = sslSession.getPeerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_NO_CERT.getMessage(), _hostName, e.getMessage()));
                return;
            }
            final X509Certificate[] x509certs = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; ++ i) {
                if (certs[i] instanceof X509Certificate) {
                    x509certs[i] = (X509Certificate)certs[i];
                } else {
                    _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_NOT_X509.getMessage(), _hostName));
                    return;
                }
            }

            try {
                _trustManager.checkServerTrusted(x509certs, CertUtils.extractAuthType(sslSession.getCipherSuite()));
                if ( _hostnameVerifier == null || _hostnameVerifier.verify( _hostName, sslSession ) ) {
                    _authorized = true;
                    _exception = null;
                } else {
                    _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_UNTRUSTED.getMessage(), _hostName, "Hostname verification failed"));
                }
            } catch (CertificateException e) {
                _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_UNTRUSTED.getMessage(), _hostName, ExceptionUtils.getMessage(e)));
            }
        }

        /**
         * @throws FtpException if an exception was encountered during {@link #verify}.
         */
        public void throwIfFailed() throws FtpException {
            if (_exception != null) throw _exception;
        }
    }
}
