package com.l7tech.identity.internal.imp;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.translation.TypeTranslator;

import java.util.Collection;

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
            return TypeTranslator.serviceUserToGenUser(getStub().findUserByPrimaryKey(identityProviderConfigId, oid));
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findByPrimaryKey", e);
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
            return getStub().saveUser(identityProviderConfigId, TypeTranslator.genUserToServiceUser(user));
        } catch (java.rmi.RemoteException e) {
            throw new SaveException("RemoteException in save", e);
        }
    }

    public void setIdentityProviderOid(long oid) {
        // what should i do with this?
    }

    public Collection findAllHeaders() throws FindException {
        try {
            return TypeTranslator.headerArrayToCollection(getStub().findAllUsers(identityProviderConfigId));
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        try {
            return TypeTranslator.headerArrayToCollection(getStub().findAllUsersByOffset(identityProviderConfigId, offset, windowSize));
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
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
    private Identity getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            IdentityService service = new IdentityServiceLocator();
            try {
                localStub = service.getidentities(new java.net.URL(getServiceURL()));
            }
            catch (Exception e) {
                throw new java.rmi.RemoteException("cannot instantiate the admin service stub", e);
            }
        }
        return localStub;
    }
    private String getServiceURL() {
        // todo, read this url from a properties file
        // maybe com.l7tech.console.util.Preferences
        return "http://localhost:8080/UneasyRooster/services/identities";
    }

    private long identityProviderConfigId;
    private Identity localStub = null;
}
