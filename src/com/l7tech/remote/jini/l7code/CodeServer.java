package com.l7tech.remote.jini.l7code;

import java.io.IOException;
import java.rmi.Remote;

/**
 * <code>CodeServer</code> implementations provide the code downloading
 * service to the clients. 
 * @author emil
 * @version 24-Mar-2004
 */
public interface CodeServer extends Remote {
    public byte[] getResource(String resource) throws IOException;
}
