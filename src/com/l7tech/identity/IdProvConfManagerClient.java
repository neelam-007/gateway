package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.adminws.identity.Client;
import com.l7tech.adminws.identity.IdentityProviderClient;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;

import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
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

    // ************************************************
    // PRIVATES
    // ************************************************
    private Client getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL(), getAdminUsername(), getAdminPassword());
            } catch (Exception e) {
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

    private String getAdminUsername() {
        return getCredentialManager().getUsername();
    }

    private String getAdminPassword() {
        return getCredentialManager().getPassword();
    }

    private ClientCredentialManager getCredentialManager() {
        ClientCredentialManager credentialManager =
          (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        if (credentialManager == null) { // bug
            throw new RuntimeException("No credential manager configured in services");
        }
        return credentialManager;
    }

    private Client localStub = null;
}
