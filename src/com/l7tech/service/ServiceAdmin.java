package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidatorResult;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Class ServiceAdmin
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 *
 * Interface for the remote administration of published services.
 */
public interface ServiceAdmin extends Remote {
    String resolveWsdlTarget(String url) throws RemoteException;

    PublishedService findServiceByPrimaryKey(long oid) throws RemoteException, FindException;

    EntityHeader[] findAllPublishedServices() throws RemoteException, FindException;

    EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws RemoteException, FindException;

    long savePublishedService(PublishedService service)
                    throws RemoteException, UpdateException, SaveException,
                           VersionException, ResolutionParameterTooLongException;

    void deletePublishedService(long oid) throws RemoteException, DeleteException;

    PolicyValidatorResult validatePolicy(String policyXml, long serviceId) throws RemoteException;
}
