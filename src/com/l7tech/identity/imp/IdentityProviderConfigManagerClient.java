package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.objectmodel.imp.EntityHeaderImp;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DeleteException;

import java.util.Collection;
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
        com.l7tech.adminws.identity.IdentityProviderConfig ipcStubFormat = null;
        try {
            ipcStubFormat = getStub().findIdentityProviderConfigByPrimaryKey(oid);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        if (ipcStubFormat == null) return null;
        return TypeTranslator.serviceIdentityProviderConfigToGenericOne(ipcStubFormat);
    }

    public Collection findAllIdentityProviders() throws FindException {
        return IdentityProviderFactory.findAllIdentityProviders(this);
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        try {
            return getStub().saveIdentityProviderConfig(TypeTranslator.genericToServiceIdProviderConfig(identityProviderConfig));
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
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
        com.l7tech.adminws.identity.Header[] array = null;
        try {
            array = getStub().findAlllIdentityProviderConfig();
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        return TypeTranslator.headerArrayToCollection(array);
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        com.l7tech.adminws.identity.Header[] array = null;
        try {
            array = getStub().findAllIdentityProviderConfigByOffset(offset, windowSize);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        return TypeTranslator.headerArrayToCollection(array);
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
                throw new java.rmi.RemoteException("Exception getting admin ws stub", e);
            }
            if (localStub == null) throw new java.rmi.RemoteException("Exception getting admin ws stub");
        }
        return localStub;
    }
    private String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        prefUrl += "/services/identities";
        return prefUrl;
        //return "http://localhost:8080/UneasyRooster/services/identities";
    }

    private Identity localStub = null;
}
