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
     * Method that allows admins to login, returning an interface to
     * the server.
     *
     * @param username The name of the user.
     * @return A reference to a proxy of the server object.
     * @throws AccessControlException on access denied for the given credentials
     * @throws LoginException on failed login
     * @throws RemoteException on remote communicatiOn error
     * @param password The password of the user.
     */
    public AdminContext login(String username, String password)
      throws RemoteException, AccessControlException, LoginException;
}