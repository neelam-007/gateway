package com.l7tech.identity.internal.imp;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.adminws.identity.Client;
import java.util.Collection;
import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 14, 2003
 *
 */
public class InternalUserManagerClient implements com.l7tech.identity.UserManager {

    public InternalUserManagerClient(long identityProviderConfigId){
        this.identityProviderConfigId = identityProviderConfigId;
    }

    public User findByPrimaryKey(long oid) throws FindException {
        try {
            return getStub().findUserByPrimaryKey(identityProviderConfigId, oid);
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findUserByPrimaryKey", e);
        }
    }

    public void delete(User user) throws DeleteException {
        try {
            getStub().deleteUser(identityProviderConfigId, user.getOid());
        } catch (java.rmi.RemoteException e) {
            throw new DeleteException("RemoteException in delete", e);
        }
    }

    public long save(User user) throws SaveException {
        try {
            return getStub().saveUser(identityProviderConfigId, user);
        } catch (java.rmi.RemoteException e) {
            throw new SaveException("RemoteException in save", e);
        }
    }

    public void update(User user) throws UpdateException {
        // at other hand, save will check if object exists and call update instead
        try {
            save(user);
        } catch (SaveException e) {
            throw new UpdateException("SaveException in save", e);
        }
    }

    public void setIdentityProviderOid(long oid) {
        // what should i do with this?
    }

    public Collection findAllHeaders() throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllUsers(identityProviderConfigId);
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllUsersByOffset(identityProviderConfigId, offset, windowSize);
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAll() throws FindException {
        throw new FindException("Operation not supported in this manager implementation");
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new FindException("Operation not supported in this manager implementation");
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private Client getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL(), getAdminUsername(), getAdminPassword());
            }
            catch (Exception e) {
                throw new java.rmi.RemoteException("cannot instantiate the admin service stub", e);
            }
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
    private String getAdminUsername() throws IOException {
        // todo, read this from somewhere
        return null;
    }
    private String getAdminPassword() throws IOException {
        // todo, read this from somewhere
        return null;
    }

    private long identityProviderConfigId;
    private Client localStub = null;
}
