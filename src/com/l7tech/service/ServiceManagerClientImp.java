package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.adminws.service.Client;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.ServiceResolutionException;
import java.util.Collection;
import java.rmi.RemoteException;
import java.io.IOException;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class ServiceManagerClientImp implements ServiceManager {
    public PublishedService findByPrimaryKey(long oid) throws FindException {
        try {
            return getStub().findServiceByPrimaryKey(oid);
        } catch (java.rmi.RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException {
        return getStub().resolveWsdlTarget(url);
    }

    public void addServiceListener( ServiceListener sl ) {
        // Not relevant on client (yet?)
    }

    public Collection findAllHeaders() throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllPublishedServices();
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllPublishedServicesByOffset(offset, windowSize);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAll() throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllPublishedServices();
            Collection output = new java.util.ArrayList();
            for (int i = 0; i < array.length; i++) {
                output.add(getStub().findServiceByPrimaryKey(array[i].getOid()));
            }
            return output;
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllPublishedServicesByOffset(offset, windowSize);
            Collection output = new java.util.ArrayList();
            for (int i = 0; i < array.length; i++) {
                output.add(getStub().findServiceByPrimaryKey(array[i].getOid()));
            }
            return output;
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public long save(PublishedService service) throws SaveException {
        try {
            long res = getStub().savePublishedService(service);
            if (res > 0) service.setOid(res);
            return res;
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public int getCurrentPolicyVersion(long policyId) throws FindException {
        throw new FindException("not implemented");
    }

    public void update(PublishedService service) throws UpdateException {
        try {
            getStub().savePublishedService(service);
        } catch (RemoteException e) {
            throw new UpdateException(e.getMessage(), e);
        }
    }

    public void delete(PublishedService service) throws DeleteException {
        try {
            getStub().deletePublishedService(service.getOid());
        } catch (RemoteException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public PublishedService resolveService(Request request) throws ServiceResolutionException {
        throw new ServiceResolutionException("No console side implementation.");
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private Client getStub() {
        if (localStub == null) {
            localStub = new Client();
        }
        return localStub;
    }

    private Client localStub = null;
}
