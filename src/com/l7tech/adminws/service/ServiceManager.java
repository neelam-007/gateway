package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import com.l7tech.objectmodel.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Class ServiceManager.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public interface ServiceManager extends Remote {
    String resolveWsdlTarget(String url) throws RemoteException;

    PublishedService findServiceByPrimaryKey(long oid) throws RemoteException, FindException;

    EntityHeader[] findAllPublishedServices() throws RemoteException, FindException;

    EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws RemoteException, FindException;

    long savePublishedService(PublishedService service)
                    throws RemoteException, UpdateException, SaveException, VersionException;

    void deletePublishedService(long oid) throws RemoteException, DeleteException;

    ServiceStatistics getStatistics( long oid ) throws RemoteException;
}
