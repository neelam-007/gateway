package com.l7tech.identity.internal.imp;

import com.l7tech.identity.Group;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.translation.TypeTranslator;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 13, 2003
 *
 */
public class InternalGroupManagerClient implements com.l7tech.identity.GroupManager {

    public InternalGroupManagerClient(long identityProviderConfigId){
        this.identityProviderConfigId = identityProviderConfigId;
    }

    public Group findByPrimaryKey(long oid) {
        try {
            return TypeTranslator.serviceGroupToGenGroup(getStub().findGroupByPrimaryKey(identityProviderConfigId, oid));
        } catch (java.rmi.RemoteException e) {
            // todo, handle exception
            e.printStackTrace();
        }
        return null;
    }

    public void delete(Group group) {
        try {
            getStub().deleteGroup(identityProviderConfigId, group.getOid());
        } catch (java.rmi.RemoteException e) {
            // todo, handle exception
            e.printStackTrace();
        }
    }

    public long save(Group group) {
        try {
            return getStub().saveGroup(identityProviderConfigId, TypeTranslator.genGroupToServiceGroup(group));
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setIdentityProviderOid(long oid) {
        // what should i do with this?
    }

    public Collection findAllHeaders() {
        try {
            return TypeTranslator.headerArrayToCollection(getStub().findAllGroups(identityProviderConfigId));
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) {
        try {
            return TypeTranslator.headerArrayToCollection(getStub().findAllGroupsByOffset(identityProviderConfigId, offset, windowSize));
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Collection findAll() {
        return null;
    }

    public Collection findAll(int offset, int windowSize) {
        return null;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private Identity getStub() {
        if (localStub == null) {
            IdentityService service = new IdentityServiceLocator();
            try {
                localStub = service.getidentities(new java.net.URL(getServiceURL()));
            }
            catch (Exception e) {
                // todo, show nice user message?
                System.err.println(e.getMessage());
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
