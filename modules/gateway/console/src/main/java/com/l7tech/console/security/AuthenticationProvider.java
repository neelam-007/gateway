/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import com.l7tech.gateway.common.VersionException;
import com.l7tech.objectmodel.InvalidPasswordException;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
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
     * <br/>
     * If the newPassword field is provided, then it will attempt to change the user's password to the new password
     * and then perform the logon process.  If the new password is not valid, then an exception is thrown and the
     * logon and change password fails.  If the newPassword field is NULL, then it will perform as a normal logon prcoess.
     *
     * @param creds the credentials to authenticate
     * @param host the host to authenticate with
     * @param validateHost true to validate the hosts name against its certificate
     * @param newPassword   If not NULL, then it will perform to change password first then do the logon afterwards.
     * @throws InvalidPasswordException If the new password is no valid.  Generally not STIG compiliant.
     *
     * @see com.l7tech.console.security.SecurityProviderImpl
     */
    void login(PasswordAuthentication creds, String host, boolean validateHost, String newPassword)
      throws LoginException, VersionException, InvalidPasswordException;

    /**
     * Connect to a session that is already established and waiting for us.
     *
     * @param sessionId  the session ID to connect to.  Must not be null.
     * @param host the host to authenticate with.  Must not be null.
     *
     * @see com.l7tech.console.security.SecurityProviderImpl
     */
    void login(String sessionId, String host)
      throws LoginException, VersionException;

    /**
     * Change password.
     *
     * @param auth The crededentials to authenticate
     * @param newAuth The credentials to use from now on.
     * @throws LoginException On bad credentials
     * @throws IllegalStateException If the users password cannot be changed.
     * @throws IllegalArgumentException If the new credentials are not acceptable.
     */
    void changePassword(PasswordAuthentication auth, PasswordAuthentication newAuth) 
      throws LoginException;

    /**
     * Logoff the session
     */
    void logoff();

    /**
     *
     */
    void acceptServerCertificate( X509Certificate certificate );

}