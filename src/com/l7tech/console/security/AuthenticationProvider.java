/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import com.l7tech.common.VersionException;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.security.cert.X509Certificate;

/**
 * The SSM <code>AuthenticationProvider</code> implementations provide authentications
 * for administrators.
 *
 * @author emil
 * @version Sep 7, 2004
 */
public interface AuthenticationProvider {
    /**
     * Subclasses implement this method to provide the concrete login implementation.
     *
     * @param creds the credentials to authenticate
     * @see com.l7tech.console.security.SecurityProviderImpl
     * @param namingURL the service naming URL
     */
    void login(PasswordAuthentication creds, String namingURL)
      throws LoginException, VersionException, RemoteException;

    /**
     * Retrieve the targewt server certificate
     *
     * @param serverCertificate
     * @see com.l7tech.console.security.SecurityProviderImpl
     */
    void validateServerCertificate(PasswordAuthentication credentials, X509Certificate serverCertificate, String namingURL)
      throws RemoteException, SecurityException;

    /**
     * Logoff the session
     */
    void logoff();

}