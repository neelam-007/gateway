package com.l7tech.identity.imp;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.objectmodel.imp.EntityHeaderImp;
import com.l7tech.objectmodel.EntityHeader;

import java.util.Collection;

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
    public IdentityProviderConfig findByPrimaryKey(long oid) {
        com.l7tech.adminws.identity.IdentityProviderConfig ipcStubFormat = null;
        try {
            ipcStubFormat = getStub().findIdentityProviderConfigByPrimaryKey(oid);
        }
        catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
        if (ipcStubFormat != null) {
            return TypeTranslator.serviceIdentityProviderConfigToGenericOne(ipcStubFormat);
        }
        else return null;
    }

    public long save(IdentityProviderConfig identityProviderConfig) {
        try {
            return getStub().saveIdentityProviderConfig(TypeTranslator.genericToServiceIdProviderConfig(identityProviderConfig));
        }
        catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
        return 0;
    }

    public void delete(IdentityProviderConfig identityProviderConfig) {
        try {
            getStub().deleteIdentityProviderConfig(identityProviderConfig.getOid());
        }
        catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
    }

    public Collection findAllHeaders() {
        com.l7tech.adminws.identity.Header[] array = null;
        try {
            array = getStub().findAlllIdentityProviderConfig();
        } catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
        return TypeTranslator.headerArrayToCollection(array);
    }

    public Collection findAllHeaders(int offset, int windowSize) {
        com.l7tech.adminws.identity.Header[] array = null;
        try {
            array = getStub().findAllIdentityProviderConfigByOffset(offset, windowSize);
        } catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
        return TypeTranslator.headerArrayToCollection(array);
    }

    public Collection findAll() {
        // todo, throw exception instead ?
        return findAllHeaders();
    }

    public Collection findAll(int offset, int windowSize) {
        // todo, throw exception instead ?
        return findAllHeaders(offset, windowSize);
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

    private Identity localStub = null;
}
