package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.imp.InternalIdentityProviderClient;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.objectmodel.imp.EntityHeaderImp;
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
            return getStub().saveIdentityProviderConfig(TypeTranslator.genericToServiceIdProviderConfig(identityProviderConfig));
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // todo (save?)
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
        try {
            return TypeTranslator.headerArrayToCollection(array);
        } catch (ClassNotFoundException e) {
            throw new FindException("ClassNotFoundException in TypeTranslator.headerArrayToCollection", e);
        }
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        com.l7tech.adminws.identity.Header[] array = null;
        try {
            array = getStub().findAllIdentityProviderConfigByOffset(offset, windowSize);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        try {
            return TypeTranslator.headerArrayToCollection(array);
        } catch (ClassNotFoundException e) {
            throw new FindException("ClassNotFoundException in TypeTranslator.headerArrayToCollection", e);
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
