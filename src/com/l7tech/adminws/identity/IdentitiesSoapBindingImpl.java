/**
 * IdentitiesSoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.l7tech.adminws.identity;

import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.misc.Locator;

public class IdentitiesSoapBindingImpl implements com.l7tech.adminws.identity.Identity{
    public com.l7tech.adminws.identity.Header[] findAlllIdentityProviderConfig() throws java.rmi.RemoteException {
        return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManager().findAllHeaders());
    }

    public com.l7tech.adminws.identity.Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManager().findAllHeaders(offset, windowSize));
    }

    public com.l7tech.adminws.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        return TypeTranslator.genericToServiceIdProviderConfig(getIdentityProviderConfigManager().findByPrimaryKey(oid));
    }

    public long saveIdentityProviderConfig(com.l7tech.adminws.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        return getIdentityProviderConfigManager().save(TypeTranslator.serviceIdentityProviderConfigToGenericOne(identityProviderConfig));
    }

    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        getIdentityProviderConfigManager().delete(getIdentityProviderConfigManager().findByPrimaryKey(oid));
    }

    public com.l7tech.adminws.identity.User findUserByPrimaryKey(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        return null;
    }

    public void deleteUser(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
    }

    public long saveUser(long identityProviderConfigId, com.l7tech.adminws.identity.User user) throws java.rmi.RemoteException {
        return -3;
    }

    public com.l7tech.adminws.identity.Header[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }

    public com.l7tech.adminws.identity.Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }

    public com.l7tech.adminws.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        return null;
    }

    public void deleteGroup(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
    }

    public long saveGroup(long identityProviderConfigId, com.l7tech.adminws.identity.Group group) throws java.rmi.RemoteException {
        return -3;
    }

    public com.l7tech.adminws.identity.Header[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }

    public com.l7tech.adminws.identity.Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfigManager getIdentityProviderConfigManager() {
        if (identityProviderConfigManager == null){
            // instantiate the server-side manager
            identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getInstance().locate(com.l7tech.identity.IdentityProviderConfigManager.class);
        }
        return identityProviderConfigManager;
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
}
