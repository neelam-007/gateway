package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.adminws.identity.Client;
import com.l7tech.adminws.identity.IdentityProviderClient;
import java.util.Collection;
import java.util.Iterator;
import java.rmi.RemoteException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 * Main entry point for the console for everything that has to do with identity including the internal id provider
 * and the ldap identity provider.
 *
 */
public class IdProvConfManagerClient implements IdentityProviderConfigManager {

    public IdentityProvider getInternalIdentityProvider() {
        IdentityProviderClient internalProvider = new IdentityProviderClient();
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setOid(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
        cfg.setDescription("Internal identity provider");
        internalProvider.initialize(cfg);
        return internalProvider;
    }

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        try {
            return getStub().findIdentityProviderConfigByPrimaryKey(oid);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        try {
            long res = getStub().saveIdentityProviderConfig(identityProviderConfig);
            identityProviderConfig.setOid(res);
            return res;
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // at other end, save will check if object exists and call update instead
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

    public Collection findAllIdentityProviders() throws FindException {
        Collection headers = findAllHeaders();
        Collection output = new java.util.ArrayList(headers.size());
        Iterator iter = headers.iterator();
        while (iter.hasNext()) {
            EntityHeader thisHeader = (EntityHeader)iter.next();
            IdentityProviderConfig conf = findByPrimaryKey(thisHeader.getOid());
            IdentityProvider provider = null;
            provider = new IdentityProviderClient();
            provider.initialize(conf);
            output.add(provider);
        }
        return output;
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

    public Collection search(String searchString) throws FindException {
        return null;
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
