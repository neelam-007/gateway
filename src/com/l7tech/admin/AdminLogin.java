/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import java.rmi.Remote;
import java.rmi.RemoteException;

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
     * @param password The password of the user.
     * @return A reference to a proxy of the server object.
     * @throws SecurityException on failed login.
     * @throws RemoteException on remote communicatiOn error 
     */
    public AdminContext login(String username, String password)
      throws RemoteException, SecurityException;
}