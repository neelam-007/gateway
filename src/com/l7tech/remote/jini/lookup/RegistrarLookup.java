package com.l7tech.remote.jini.lookup;

import net.jini.core.lookup.ServiceRegistrar;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.IOException;

/**
 * Returns the registrar that the <code>LookupLocator</code> will resolve
 * in the VM where the service is hosted.
 * @author emil
 * @version 16-May-2004
 */
public interface RegistrarLookup extends Remote {
    /**
     * the uuid of the the exported server. 
     * */
    String REGISTRAR_UUID = "ac8969e4-ee15-4ecc-a335-11f6d46e10c2";

    /**
     * Obtain the <code>ServiceRegistrar</code> from the remote service implementaiton.
     * @return the serviice registrar
     * @throws RemoteException on remote related error
     */
    ServiceRegistrar getRegistrar() throws RemoteException, IOException;
}
