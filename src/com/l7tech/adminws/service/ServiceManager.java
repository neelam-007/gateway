package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;

import java.rmi.Remote;

/**
 * Class ServiceManager.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public interface ServiceManager extends Remote {
    String resolveWsdlTarget(String url) throws java.rmi.RemoteException;

    PublishedService findServiceByPrimaryKey(long oid) throws java.rmi.RemoteException;

    com.l7tech.objectmodel.EntityHeader[] findAllPublishedServices() throws java.rmi.RemoteException;

    com.l7tech.objectmodel.EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws java.rmi.RemoteException;

    long savePublishedService(PublishedService service) throws java.rmi.RemoteException;

    void deletePublishedService(long oid) throws java.rmi.RemoteException;
}
