package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.imp.InternalIdentityProviderClient;
import com.l7tech.adminws.identity.Client;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 * The console-side implementation of the IdentityProviderConfigManager. It calls the
 * server-side implementation of the same interface (IdentityProviderConfigManagerImp)
 * through the admin web service.
 */
public class IdentityProviderConfigManagerClient implements IdentityProviderConfigManager {
    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        try {
            return getStub().findIdentityProviderConfigByPrimaryKey(oid);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public Collection findAllIdentityProviders() throws FindException {
        Collection headers = findAllHeaders();
        Collection output = new java.util.ArrayList(headers.size());
        Iterator iter = headers.iterator();
        while (iter.hasNext()) {
            EntityHeader thisHeader = (EntityHeader)iter.next();
            IdentityProviderConfig conf = findByPrimaryKey(thisHeader.getOid());
            InternalIdentityProviderClient provider = new InternalIdentityProviderClient();
            provider.initialize(conf);
            output.add(provider);
        }
        return output;
        //return IdentityProviderFactory.findAllIdentityProviders(this);
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        try {
            return getStub().saveIdentityProviderConfig(identityProviderConfig);
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // at other hand, save will check if object exists and call update instead
        try {
            save(identityProviderConfig);
        } catch (SaveException e) {
            throw new UpdateException("SaveException in save", e);
        }
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        try {
            getStub().deleteIdentityProviderConfig(identityProviderConfig.getOid());
        } catch (RemoteException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public Collection findAllHeaders() throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllIdentityProviderConfig();
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
            array = getStub().findAllIdentityProviderConfigByOffset(offset, windowSize);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAll() throws FindException {
        Collection headers = findAllHeaders();
        Collection output = new java.util.ArrayList(headers.size());
        Iterator iter = headers.iterator();
        while (iter.hasNext()) {
            EntityHeader thisHeader = (EntityHeader)iter.next();
            IdentityProviderConfig conf = findByPrimaryKey(thisHeader.getOid());
            output.add(conf);
        }
        return output;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection headers = findAllHeaders(offset, windowSize);
        Collection output = new java.util.ArrayList(headers.size());
        Iterator iter = headers.iterator();
        while (iter.hasNext()) {
            EntityHeader thisHeader = (EntityHeader)iter.next();
            IdentityProviderConfig conf = findByPrimaryKey(thisHeader.getOid());
            output.add(conf);
        }
        return output;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private Client getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL());
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
        prefUrl += "/services/identityAdmin";
        return prefUrl;
        //return "http://localhost:8080/UneasyRooster/services/identities";
    }

    private Client localStub = null;
}
