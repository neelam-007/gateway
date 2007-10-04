/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.server.transport.ftp;

import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.ftp.FtpTestException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpAdminImpl implements FtpAdmin {
    private static final Logger _logger = Logger.getLogger(FtpAdminImpl.class.getName());
    private final X509TrustManager _x509TrustManager;
    private final SsgKeyStoreManager _ssgKeyStoreManager;
    private final ServerAssertionRegistry _serverAssertionRegistry;

    public FtpAdminImpl(X509TrustManager x509TrustManager,
                        SsgKeyStoreManager ssgKeyStoreManager,
                        ServerAssertionRegistry serverAssertionRegistry) {
        _x509TrustManager = x509TrustManager;
        _ssgKeyStoreManager = ssgKeyStoreManager;
        _serverAssertionRegistry = serverAssertionRegistry;
    }

    /**
     * Tests connection to FTP(S) server and tries "cd" into remote directory.
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
    public void testConnection(boolean isFtps,
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
                               int timeout) throws FtpTestException {
        try {
            // Need to use reflection for modular assertion.
            Assertion ftpRoutingAssertion = _serverAssertionRegistry.findByExternalName("FtpRoutingAssertion");
            String serverFtpRoutingAssertionClassname = (String)ftpRoutingAssertion.meta().get(AssertionMetadata.SERVER_ASSERTION_CLASSNAME);
            Class serverFtpRoutingAssertionClass = ftpRoutingAssertion.getClass().getClassLoader().loadClass(serverFtpRoutingAssertionClassname);
            serverFtpRoutingAssertionClass.getMethod("testConnection",
                                                     Boolean.TYPE,              // isFtps
                                                     Boolean.TYPE,              // isExplicit
                                                     Boolean.TYPE,              // isVerifyServerCert
                                                     String.class,              // hostName
                                                     Integer.TYPE,              // port
                                                     String.class,              // userName
                                                     String.class,              // password
                                                     Boolean.TYPE,              // useClientCert
                                                     Long.TYPE,                 // clientCertKeystoreId
                                                     String.class,              // clientCertKeyAlias
                                                     String.class,              // directory
                                                     Integer.TYPE,              // timeout
                                                     X509TrustManager.class,    // x509TrustManager
                                                     SsgKeyStoreManager.class)  // ssgKeyStoreManager
                                          .invoke(null /* static method */,
                                                  isFtps,
                                                  isExplicit,
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
                                                  _x509TrustManager,
                                                  _ssgKeyStoreManager);
        } catch (ClassNotFoundException e) {
            _logger.log(Level.INFO, "Caught ClassNotFoundException while testing connection.", e);
            throw new FtpTestException(e.toString(), null);
        } catch (NoSuchMethodException e) {
            _logger.log(Level.INFO, "Caught NoSuchMethodException while testing connection.", e);
            throw new FtpTestException(e.toString(), null);
        } catch (IllegalAccessException e) {
            _logger.log(Level.INFO, "Caught IllegalAccessException while testing connection.", e);
            throw new FtpTestException(e.toString(), null);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof FtpTestException) {
                throw (FtpTestException)e.getCause();
            } else {
                throw new FtpTestException(e.getMessage(), null);
            }
        }
    }
}
