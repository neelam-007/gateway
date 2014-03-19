package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import java.util.logging.Logger;

/**
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpAdminImpl implements FtpAdmin {
    private static final Logger logger = Logger.getLogger(FtpAdminImpl.class.getName());

    private final X509TrustManager _x509TrustManager;
    private final HostnameVerifier _hostnameVerifier;
    private final DefaultKey _keyFinder;

    public FtpAdminImpl(X509TrustManager x509TrustManager,
                        HostnameVerifier hostnameVerifier, 
                        DefaultKey keyFinder) {
        _x509TrustManager = x509TrustManager;
        _hostnameVerifier = hostnameVerifier;
        _keyFinder = keyFinder;
    }

    /**
     * Tests connection to FTP(S) server and tries "cd" into remote directory.
     * Convenience proxy bean for the FtpClientBuilder, configured with SSG's certificate and key store.
     *
     * @param isFtps                true if FTPS; false if FTP (unsecured)
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
     * @throws FtpTestException if connection test failed
     */
    @Override
    public void testConnection(boolean isFtps,
                               boolean isExplicit,
                               boolean isVerifyServerCert,
                               String hostName,
                               int port,
                               String userName,
                               String password,
                               Goid passwordGoid,
                               boolean useClientCert,
                               @Nullable Goid clientCertKeystoreId,
                               String clientCertKeyAlias,
                               String directory,
                               int timeout) throws FtpTestException {

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);

        config.setSecurity(!isFtps ? FtpSecurity.FTP_UNSECURED :
                                      isExplicit ? FtpSecurity.FTPS_EXPLICIT : FtpSecurity.FTPS_IMPLICIT);

        String expandedPassword;

        try {
            if (null == passwordGoid) {
                expandedPassword = ServerVariables.expandPasswordOnlyVariable(new LoggingAudit(logger), password);
            } else {
                expandedPassword = ServerVariables.getSecurePasswordByGoid(new LoggingAudit(logger), passwordGoid);
            }
        } catch (FindException e) {
            final String msg = "Unable to look up secure password reference: " + ExceptionUtils.getMessage(e);
            throw (FtpTestException) new FtpTestException(msg, msg).initCause(e);
        }

        config.setPort(port).setUser(userName).setPass(expandedPassword).setDirectory(directory).setTimeout(timeout);

        X509TrustManager trustManager = null;

        if (isVerifyServerCert) {
            config.setVerifyServerCert(true);
            trustManager = _x509TrustManager;
        }

        DefaultKey keyFinder = null;
        if (useClientCert) {
            config.setUseClientCert(true).setClientCertId(clientCertKeystoreId).setClientCertAlias(clientCertKeyAlias);
            keyFinder = _keyFinder;
        }

        if (isFtps)
            FtpClientUtils.testFtpsConnection(config, keyFinder, trustManager, _hostnameVerifier);
        else
            FtpUtils.testFtpConnection(config);
    }
}
