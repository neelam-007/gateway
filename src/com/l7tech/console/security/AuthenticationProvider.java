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
     * @param host the host to authenticate with
     * @param validateHost true to validate the hosts name against its certificate
     *
     * @see com.l7tech.console.security.SecurityProviderImpl
     */
    void login(PasswordAuthentication creds, String host, boolean validateHost)
      throws LoginException, VersionException, RemoteException;

    /**
     * Connect to a session that is already established and waiting for us.
     *
     * @param sessionId  the session ID to connect to.  Must not be null.
     * @param host the host to authenticate with.  Must not be null.
     *
     * @see com.l7tech.console.security.SecurityProviderImpl
     */
    public void login(String sessionId, String host)
      throws LoginException, VersionException, RemoteException;

    /**
     * Logoff the session
     */
    void logoff();

}