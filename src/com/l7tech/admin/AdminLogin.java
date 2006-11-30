/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import com.l7tech.identity.AuthenticationException;

import javax.security.auth.login.LoginException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessControlException;

/**
 * Interface used to establish an admin session.  The methods of this interface are explicitly excluded from admin
 * session security requirements.  If you add a new method, be sure to add its name to the if statement in
 * SecureRemoteInvocationExecutor.
 *
 * @author emil
 * @version Dec 2, 2004
 */
public interface AdminLogin extends Remote {
    /**
     * Method that returns the SHA-1 hash over admin certificate and the admin
     * password.
     * This then provides a way for the admin to validate the server certificate
     * against the server certificate that has been obtained through SSL session
     * for example or out of bound.
     *
     * @param username The name of the user.
     * @return The Server certificate.
     * @throws AccessControlException on access denied, if the user is not of
     *         any admin role
     * @throws RemoteException on remote communication error
     */
    public byte[] getServerCertificate(String username)
      throws RemoteException, AccessControlException;

    /**
     * Method that allows admins to login, returning an interface to
     * the server.
     *
     * @param username The name of the user.
     * @return An {@link AdminLoginResult} if the login was successful, or throws. Never null.
     * @throws AccessControlException on access denied for the given credentials
     * @throws LoginException on failed login
     * @throws RemoteException on remote communication error
     * @param password The password of the user.
     */
    public AdminLoginResult login(String username, String password)
            throws RemoteException, AccessControlException, LoginException;

    /**
     * Method that allows admin to login using an existing session.
     *
     * @param sessionId the session to resume
     * @return an AdminLoginResult describing the resumed session.  Never null.
     * @throws AuthenticationException if the specified session ID is invalid or no longer valid
     * @throws RemoteException  on remote communication error
     */
    public AdminLoginResult resume(String sessionId)
            throws RemoteException, AuthenticationException;


    /**
     * Method that allows admin to destroy an existing session.  After this method, the specified session ID
     * will not work for resume or for authentication of admin requests.
     *
     * @param sessionId the session to resume.  If this is not a valid, current session ID this method will
     *                  silently take no action.
     * @throws RemoteException  on remote communication error
     */
    public void logout(String sessionId) throws RemoteException;
}