package com.l7tech.remote.jini.l7code;

import java.rmi.RemoteException;

/**
 * @author emil
 * @version 24-Mar-2004
 */
public interface CodeServer {
    public byte[] geResource(String resource) throws RemoteException;
}
