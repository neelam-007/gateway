package com.l7tech.service.rmi;

import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * The class is the admin service manager implementaiton.
 *
 */
public class ServiceAdminImpl extends RemoteService implements ServiceAdmin {

    public ServiceAdminImpl (String[] configOptions, LifeCycle lc)
            throws ConfigurationException, IOException {
        super(configOptions, lc);
        delegate = new com.l7tech.service.ws.ServiceAdminImpl();
    }

    public String resolveWsdlTarget(String url) throws RemoteException {
        return delegate.resolveWsdlTarget(url);
    }

    public PublishedService findServiceByPrimaryKey(long oid) throws RemoteException, FindException {
        return delegate.findServiceByPrimaryKey(oid);
    }

    public EntityHeader[] findAllPublishedServices() throws RemoteException, FindException {
        return delegate.findAllPublishedServices();
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws RemoteException, FindException {
        return delegate.findAllPublishedServicesByOffset(offset, windowSize);
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or upodated
     * @throws RemoteException
     */
    public long savePublishedService(PublishedService service)
                    throws RemoteException, UpdateException, SaveException, VersionException {
        return delegate.savePublishedService(service);
    }

    public void deletePublishedService(long oid) throws RemoteException, DeleteException {
        delegate.deletePublishedService(oid);
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private ServiceAdmin delegate = null;
}
