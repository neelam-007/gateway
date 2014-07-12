/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.GatewayConfiguration;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.objectmodel.InvalidPasswordException;

import javax.security.auth.login.LoginException;
import java.io.Serializable;
import java.security.AccessControlException;

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
    public final static String ERR_MSG_USERNAME_PSWD_BOTH_REQUIRED = "Username and password are both required.";
    /**
     * Return value from {@link AdminLogin#getServerCertificateVerificationInfo(String, byte[])}.
     */
    public static class ServerCertificateVerificationInfo implements Serializable {
        public final String verifierFormat;
        public final byte[] serverNonce;
        public final byte[] userSalt;
        public final byte[] checkHash;

        public ServerCertificateVerificationInfo(String verifierFormat, byte[] serverNonce, byte[] userSalt, byte[] checkHash) {
            this.verifierFormat = verifierFormat;
            this.serverNonce = serverNonce;
            this.userSalt = userSalt;
            this.checkHash = checkHash;
        }
    }

    /**
     * Method that returns the SHA-512 hash over admin certificate and the admin
     * password.
     * This then provides a way for the admin to validate the server certificate
     * against the server certificate that has been obtained through SSL session
     * for example or out of bound.
     *
     * @param username The name of the admin user.  Must be a user for whom the password is available.  Required.
     * @param clientNonce random bytes from client to add to hash.  Required.
     * @return a Pair< SALT, CHECKHASH > where SALT is the salt value for the user's hashed password and CHECKHASH is the server certificate check hash bytes.
     * @throws AccessControlException on access denied, if the user is not of
     *         any admin role
     */
    @Administrative(authenticated =false, licensed=false)
    ServerCertificateVerificationInfo getServerCertificateVerificationInfo(String username, byte[] clientNonce)
            throws AccessControlException;

    /**
     * No longer supported, pre 6.0 Managers will use this method prior to version checks.
     */
    @Deprecated
    @Administrative(authenticated =false, licensed=false)
    byte[] getServerCertificate(String username);

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
    AdminLoginResult loginNew(String username, String password)
            throws AccessControlException, LoginException;


    /**
     * Legacy login method called by pre-Icefish SSMs.  Older SSMs do not support GOIDs so will not be able to
     * deserialize the User inside a successful AdminLoginResult.  They certainly won't be able to successfully
     * consume any APIs, even with correct credentials.  So, rather than let the logon succeed and return a result
     * that they won't be able to deserialize, we will simulate a logon failure with an error message suggesting
     * the SSM version is out of date.
     * <p/>
     * This method always fails with a "version out of date" error regardless of passed-in credentials.
     * <p/>
     * <b>DO NOT USE</b>
     *
     * @deprecated use {@link #loginNew} instead
     * @param username The name of the user.
     * @param password The password of the user.
     * @return An {@link AdminLoginResult} if the login was successful, or throws. Never null.
     * @throws AccessControlException on access denied for the given credentials
     * @throws LoginException on failed login
     */
    @Administrative(authenticated =false, licensed=false)
    @Deprecated
    AdminLoginResult login(String username, String password)
            throws AccessControlException, LoginException;

    /**
     * Method that allows admins to change the expired password and login in one pass.  The new password will have to be
     * password policy compliant in order to successfully make the change and proceed with login process.  If the new password is
     * not password policy compliant, then it will fail the entire login and password change process.  This method is generally
     * used when the password has expired and require the user to change to a new password before logging into the
     * manager.
     *
     * @param username  The name of the user
     * @param oldPassword   The old password used for authentication (so cannot change other people's password)
     * @param newPassword   The new password to be changed to (must be password policy compliant)
     * @return  An {@link AdminLoginResult} if both the password change and login was successful, or throws exceptions.
     *          Never returns NULL.
     * @throws AccessControlException   Access denied for the given login credentials
     * @throws LoginException           Failed to login
     * @throws InvalidPasswordException Password is not password policy compliant
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
     * <p>This changes the password of the currently logged-in administrator.
     * Passsword is subject to all policy rules. A password change will be recorded if the change is successful.
     * <p>
     *
     * @param currentPassword The users current password.
     * @param newPassword The new password for the user.
     * @throws LoginException if the current password is not valid
     * @throws InvalidPasswordException if the newPassword is not acceptable
     * @throws IllegalStateException if the password cannot be changed for the user
     */
    @Administrative(licensed=false)
    void changePassword(String currentPassword, String newPassword)
            throws LoginException, InvalidPasswordException;

    /**
     * Can be called periodically to prevent administrative session timeout.
     */
    @Administrative(licensed=false)
    void ping();
}