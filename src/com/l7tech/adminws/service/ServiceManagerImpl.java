package com.l7tech.adminws.service;

import com.l7tech.jini.export.RemoteService;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * The class is the admin service manager implementaiton.
 *
 */
public class ServiceManagerImpl extends RemoteService implements ServiceManager {

    public ServiceManagerImpl (String[] configOptions, LifeCycle lc)
            throws ConfigurationException, IOException {
        super(configOptions, lc);
        delegate = new Service();
    }

    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException {
        return delegate.resolveWsdlTarget(url);
    }

    public PublishedService findServiceByPrimaryKey(long oid) throws java.rmi.RemoteException {
        return delegate.findServiceByPrimaryKey(oid);
    }

    public EntityHeader[] findAllPublishedServices() throws java.rmi.RemoteException {
        return delegate.findAllPublishedServices();
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        return delegate.findAllPublishedServicesByOffset(offset, windowSize);
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or upodated
     * @throws java.rmi.RemoteException
     */
    public long savePublishedService(PublishedService service) throws java.rmi.RemoteException {
        return delegate.savePublishedService(service);
    }

    public void deletePublishedService(long oid) throws java.rmi.RemoteException {
        delegate.deletePublishedService(oid);
    }

    public ServiceStatistics getStatistics(long oid) throws RemoteException {
        return delegate.getStatistics( oid );
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private ServiceManager delegate = null;
}
