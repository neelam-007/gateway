/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import javax.security.auth.login.LoginException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessControlException;

/**
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
     * @return A reference to a proxy of the server object.
     * @throws AccessControlException on access denied for the given credentials
     * @throws LoginException on failed login
     * @throws RemoteException on remote communication error
     * @param password The password of the user.
     */
    public AdminContext login(String username, String password)
      throws RemoteException, AccessControlException, LoginException;
}