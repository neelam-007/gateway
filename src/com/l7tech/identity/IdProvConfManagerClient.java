package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.rmi.RemoteException;

/**
 * Main entry point for the console for everything that has to do with identity including the internal id provider
 * and the ldap identity provider.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascellesv
 * Date: Jun 23, 2003
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
        } catch (UpdateException e) {
            throw new SaveException(e.getMessage(), e);
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
        Collection output = new ArrayList(headers.size());
        Iterator iter = headers.iterator();
        while (iter.hasNext()) {
            EntityHeader thisHeader = (EntityHeader)iter.next();
            IdentityProviderConfig conf = findByPrimaryKey(thisHeader.getOid());
            if (conf != null) {
                IdentityProvider provider = new IdentityProviderClient();
                provider.initialize(conf);
                output.add(provider);
            }
        }
        return output;
    }

    /**
     * @param oid the identity provider id to look for
     * @return the identoty provider for a given id, or <code>null</code>
     * @throws FindException if there was an persistence error
     */
    public IdentityProvider getIdentityProvider(long oid) throws FindException {
        IdentityProviderConfig conf = findByPrimaryKey(oid);
        if (conf == null) return null;
        IdentityProvider provider = new IdentityProviderClient();
        provider.initialize(conf);
        return provider;
    }

    public void test(IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException {
        try {
            getStub().testIdProviderConfig(identityProviderConfig);
        } catch (RemoteException e) {
            Throwable cause = getTypedCause(e, InvalidIdProviderCfgException.class);
            if (cause instanceof InvalidIdProviderCfgException) {
                throw (InvalidIdProviderCfgException)(cause);
            }
            else throw new InvalidIdProviderCfgException(e);
        }
    }

    public Collection findAllHeaders() throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllIdentityProviderConfig();
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        Collection output = new ArrayList();
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
        Collection output = new ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAll() throws FindException {
        Collection headers = findAllHeaders();
        Collection output = new ArrayList(headers.size());
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
        Collection output = new ArrayList(headers.size());
        Iterator iter = headers.iterator();
        while (iter.hasNext()) {
            EntityHeader thisHeader = (EntityHeader)iter.next();
            IdentityProviderConfig conf = findByPrimaryKey(thisHeader.getOid());
            output.add(conf);
        }
        return output;
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        try {
            return getStub().getLdapTemplates();
        } catch (RemoteException e) {
            throw new FindException("can't get remote IdentityAdmin", e);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityAdmin getStub() throws RemoteException {
        IdentityAdmin svc = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
        if (svc == null) {
            throw new RemoteException("Unable to obtain the remote service");
        }
        return svc;
    }

    private static Throwable getTypedCause(Throwable e, Class typeWanted) {
        Throwable ex = e;
        while (ex != null){
            if (ex.getClass().equals(typeWanted)) {
                return ex;
            }
            ex = ex.getCause();
        }
        return e;
    }

}
