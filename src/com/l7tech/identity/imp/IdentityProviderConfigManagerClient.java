package com.l7tech.identity.imp;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.adminws.clientstub.Identity;
import com.l7tech.adminws.clientstub.IdentityService;
import com.l7tech.adminws.clientstub.IdentityServiceLocator;
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
        com.l7tech.adminws.clientstub.IdentityProviderConfig ipcStubFormat = null;
        try {
            ipcStubFormat = getStub().findIdentityProviderConfigByPrimaryKey(oid);
        }
        catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
        if (ipcStubFormat != null) {
            return transferStubIdentityProviderConfigToGenericOne(ipcStubFormat);
        }
        return null;
    }

    public long save(IdentityProviderConfig identityProviderConfig) {
        try {
            return getStub().saveIdentityProviderConfig(transferGenericIdentityProviderConfigToStubOne(identityProviderConfig));
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
        com.l7tech.adminws.clientstub.Header[] array = null;
        try {
            array = getStub().findAlllIdentityProviderConfig();
        } catch (Exception e) {
            // todo, show nice user message?
            System.err.println(e.getMessage());
        }
        if (array != null && array.length > 0) {
            Collection ret = new java.util.ArrayList(array.length);
            for (int i = 0; i < array.length; i++) {
                // add the header
                ret.add(transferStubHeaderToGenHeader(array[i]));
            }
            return ret;
        }
        return new java.util.ArrayList();
    }

    public Collection findAllHeaders(int offset, int windowSize) {
        return null;
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
    static com.l7tech.identity.IdentityProviderConfig transferStubIdentityProviderConfigToGenericOne(com.l7tech.adminws.clientstub.IdentityProviderConfig stub) {
        IdentityProviderConfigImp ret = new IdentityProviderConfigImp();
        ret.setDescription(stub.getDescription());
        ret.setName(stub.getName());
        ret.setOid(stub.getOid());
        IdentityProviderTypeImp retType = new IdentityProviderTypeImp();
        retType.setClassName(stub.getTypeClassName());
        retType.setDescription(stub.getTypeDescription());
        retType.setName(stub.getTypeName());
        retType.setOid(stub.getTypeOid());
        ret.setType(retType);
        return ret;
    }

    static com.l7tech.adminws.clientstub.IdentityProviderConfig transferGenericIdentityProviderConfigToStubOne(com.l7tech.identity.IdentityProviderConfig gen) {
        com.l7tech.adminws.clientstub.IdentityProviderConfig ret = new com.l7tech.adminws.clientstub.IdentityProviderConfig();
        ret.setDescription(gen.getDescription());
        ret.setName(gen.getName());
        ret.setOid(gen.getOid());
        ret.setTypeClassName(gen.getType().getClassName());
        ret.setTypeClassName(gen.getType().getClassName());
        ret.setTypeDescription(gen.getType().getDescription());
        ret.setTypeName(gen.getType().getName());
        ret.setTypeOid(gen.getType().getOid());
        return ret;
    }

    static com.l7tech.objectmodel.EntityHeader transferStubHeaderToGenHeader(com.l7tech.adminws.clientstub.Header stubHeader) {
        EntityHeaderImp ret = new EntityHeaderImp();
        ret.setName(stubHeader.getName());
        ret.setOid(stubHeader.getOid());
        try {
            ret.setType(Class.forName(stubHeader.getType()));
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return ret;
    }

    private Identity localStub = null;
}
