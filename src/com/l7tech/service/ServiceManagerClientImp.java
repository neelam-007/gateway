package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.adminws.service.Client;
import com.l7tech.message.Request;

import java.util.Collection;
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

    public Collection findAllHeaders() throws FindException {
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return null;
    }

    public Collection findAll() throws FindException {
        return null;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }

    public long save(PublishedService service) throws SaveException {
        return 0;
    }

    public void update(PublishedService service) throws UpdateException {
    }

    public void delete(PublishedService service) throws DeleteException {
    }

    public PublishedService resolveService(Request request) {
        // not implemented at run time
        return null;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    public static void main(String[] args) throws Exception {
        com.l7tech.adminws.ClientCredentialManager.setCachedUsername("ssgadmin");
        com.l7tech.adminws.ClientCredentialManager.setCachedPasswd("ssgadminpasswd");
        ServiceManagerClientImp me = new ServiceManagerClientImp();
        System.out.println(me.resolveWsdlTarget("tralala"));
        System.out.println(me.findByPrimaryKey(555));
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
