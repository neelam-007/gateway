/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.logging;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author emil
 * @version Oct 20, 2004
 */
public interface Foo extends Remote {
    void echo(String msg) throws RemoteException;
}