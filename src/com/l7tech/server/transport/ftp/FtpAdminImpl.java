/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.server.transport.ftp;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.ftp.FtpTestException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.policy.ServerAssertionRegistry;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpAdminImpl implements FtpAdmin {
    private static final Logger _logger = Logger.getLogger(FtpAdminImpl.class.getName());
    private final LicenseManager _licenseManager;
    private final TrustedCertManager _trustedCertManager;
    private final ServerAssertionRegistry _serverAssertionRegistry;

    public FtpAdminImpl(LicenseManager licenseManager, TrustedCertManager trustedCertManager, ServerAssertionRegistry serverAssertionRegistry) {
        _licenseManager = licenseManager;
        _trustedCertManager = trustedCertManager;
        _serverAssertionRegistry = serverAssertionRegistry;
    }

    private void checkLicense() throws RemoteException {
        try {
            _licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    /**
     * Test connection to the specified FTP server.
     *
     * @throws RemoteException
     * @throws FtpTestException if a test connection could not be established
     */
    public void testConnection(boolean isFtps,
                               boolean isExplicit,
                               boolean isVerifyServerCert,
                               String hostName,
                               int port,
                               String userName,
                               String password,
                               String directory,
                               int timeout) throws RemoteException, FtpTestException {
        checkLicense();
        try {
            // Need to use reflection for modular assertion.
            Assertion ftpRoutingAssertion = _serverAssertionRegistry.findByExternalName("FtpRoutingAssertion");
            String serverFtpRoutingAssertionClassname = (String)ftpRoutingAssertion.meta().get(AssertionMetadata.SERVER_ASSERTION_CLASSNAME);
            Class serverFtpRoutingAssertionClass = ftpRoutingAssertion.getClass().getClassLoader().loadClass(serverFtpRoutingAssertionClassname);
            serverFtpRoutingAssertionClass.getMethod("testConnection",
                                                     Boolean.TYPE,
                                                     Boolean.TYPE,
                                                     Boolean.TYPE,
                                                     String.class,
                                                     Integer.TYPE,
                                                     String.class,
                                                     String.class,
                                                     String.class,
                                                     Integer.TYPE,
                                                     TrustedCertManager.class)
                                          .invoke(null /* static method */,
                                                  isFtps,
                                                  isVerifyServerCert,
                                                  isExplicit,
                                                  hostName,
                                                  port,
                                                  userName,
                                                  password,
                                                  directory,
                                                  timeout,
                                                  _trustedCertManager);
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
