package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.adminws.service.Client;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.ServiceResolutionException;

import java.util.Collection;
import java.io.IOException;
import java.rmi.RemoteException;


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
        return 0;
    }

    public void update(PublishedService service) throws UpdateException {
    }

    public void delete(PublishedService service) throws DeleteException {
    }

    public PublishedService resolveService(Request request) throws ServiceResolutionException {
        throw new ServiceResolutionException("No console side implementation.");
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    public static void main(String[] args) throws Exception {
        com.l7tech.adminws.ClientCredentialManager.setCachedUsername("ssgadmin");
        com.l7tech.adminws.ClientCredentialManager.setCachedPasswd("ssgadminpasswd");
        ServiceManagerClientImp me = new ServiceManagerClientImp();
        //System.out.println(me.resolveWsdlTarget("tralala"));
        System.out.println(me.findByPrimaryKey(555));
        Collection res = me.findAll();
        System.out.println(res.toString());
        res = me.findAll(21, 654);
        System.out.println(res.toString());
    }

    private Client getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL(), getAdminUsername(), getAdminPassword());
            }
            catch (Exception e) {
                throw new java.rmi.RemoteException("Exception getting admin ws stub", e);
            }
            if (localStub == null) throw new java.rmi.RemoteException("Exception getting admin ws stub");
        }
        return localStub;
    }
    private String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            System.err.println("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            prefUrl = "http://localhost:8080/ssg";
        }
        prefUrl += "/services/serviceAdmin";
        return prefUrl;
        //return "http://localhost:8080/UneasyRooster/services/identities";
    }
    private String getAdminUsername() throws IOException {
        return com.l7tech.adminws.ClientCredentialManager.getCachedUsername();
    }
    private String getAdminPassword() throws IOException {
        return com.l7tech.adminws.ClientCredentialManager.getCachedPasswd();
    }

    private Client localStub = null;
}
