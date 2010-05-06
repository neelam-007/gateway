/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.admin;

import com.l7tech.identity.AuthenticationException;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.InvalidPasswordException;

import javax.security.auth.login.LoginException;
import java.security.AccessControlException;
import java.security.cert.X509Certificate;

/**
 * Interface used to establish and manage an admin session.
 *
 * <p>Some of the methods of this interface are explicitly excluded from admin
 * session security requirements.</p>
 *
 * <p>If you add a new method, be sure to add its name to the if statement in
 * SecureRemoteInvocationExecutor.</p>
 *
 * @author emil
 * @version Dec 2, 2004
 */
@Secured
@Administrative
public interface AdminLogin {
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
     */
    @Administrative(authenticated =false, licensed=false)
    byte[] getServerCertificate(String username)
            throws AccessControlException;

    /**
     * Method that allows admins to login, returning an interface to
     * the server.
     *
     * @param username The name of the user.
     * @param password The password of the user.
     * @return An {@link AdminLoginResult} if the login was successful, or throws. Never null.
     * @throws AccessControlException on access denied for the given credentials
     * @throws LoginException on failed login
     */
    @Administrative(authenticated =false, licensed=false)
    AdminLoginResult login(String username, String password)
            throws AccessControlException, LoginException;

    /**
     * Method that allows admins to change the expired password and login in one pass.  The new password will have to be
     * STIG compiliant inorder to successfully make the change and proceed with login process.  If the new password is
     * not STIG compiliant, then it will fail the entire login and password change process.  This method is generally
     * used when the password has expired and require the user to change to a new password before logging into the
     * manager.
     *
     * @param username  The name of the user
     * @param oldPassword   The old password used for authentication (so cannot change other people's password)
     * @param newPassword   The new password to be changed to (must be STIG compiliant)
     * @return  An {@link AdminLoginResult} if both the password change and login was successful, or throws exceptions.
     *          Never returns NULL.
     * @throws AccessControlException   Access denied for the given login credentials
     * @throws LoginException           Failed to login
     * @throws InvalidPasswordException Password is not STIG compilant
     */
    @Administrative(authenticated=false, licensed=false)
    AdminLoginResult loginWithPasswordUpdate(String username, String oldPassword, String newPassword)
            throws AccessControlException, LoginException, InvalidPasswordException;

    /**
     * Method that allows admin to login using an existing session.
     *
     * @param sessionId the session to resume
     * @return an AdminLoginResult describing the resumed session.  Never null.
     * @throws AuthenticationException if the specified session ID is invalid or no longer valid
     */
    @Administrative(authenticated =false, licensed=false)
    AdminLoginResult resume(String sessionId)
            throws AuthenticationException;

    /**
     * Method that allows admin to destroy an existing session.  After this method, the specified session ID
     * will not work for resume or for authentication of admin requests.
     */
    @Administrative(licensed=false)
    void logout();

    /**
     * Change administrator password.
     *
     * <p>This changes the password of the currently logged-in administrator.<p>
     *
     * @param currentPassword The users current password.
     * @param newPassword The new password for the user.
     * @throws LoginException if the current password is not valid
     * @throws IllegalArgumentException if the newPassword is not acceptable
     * @throws IllegalStateException if the password cannot be changed for the user
     */
    @Administrative(licensed=false)
    void changePassword(String currentPassword, String newPassword)
            throws LoginException;
}