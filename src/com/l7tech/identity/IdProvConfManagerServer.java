package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfigManager;
import com.l7tech.identity.internal.imp.InternalIdentityProviderImp;

import java.util.Collection;
import java.util.ArrayList;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 20, 2003
 *
 * This IdentityProviderConfigManager is the server side manager who manages the one and only
 * internal identity provider as well as the other providers (ldap) configured by the administrator.
 *
 */
public class IdProvConfManagerServer implements GlobalIdProviderConfManager {

    // since this provider config is not persisted, we need a special id to identify it for certain operations
    public static final long INTERNALPROVIDER_SPECIAL_OID = -2;

    public IdProvConfManagerServer() {
        // construct the ldapidentity provider
        ldapConfigManager = new LdapIdentityProviderConfigManager();

        // construct the internal id provider
        internalProvider = new InternalIdentityProviderImp();
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setOid(INTERNALPROVIDER_SPECIAL_OID);
        internalProvider.initialize(cfg);
    }

    public IdentityProvider getInternalIdentityProvider() {
        return internalProvider;
    }

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        if (oid == INTERNALPROVIDER_SPECIAL_OID) return internalProvider.getConfig();
        else return ldapConfigManager.findByPrimaryKey(oid);
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        // check the type and save accordingly
        if (identityProviderConfig.type() == IdentityProviderType.LDAP) {
            return ldapConfigManager.save(identityProviderConfig);
        }
        // handle other types as they are added
        throw new SaveException("this type of config cannot be saved");
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // check the type and save accordingly
        if (identityProviderConfig.type() == IdentityProviderType.LDAP) {
            ldapConfigManager.update(identityProviderConfig);
        }
        // handle other types as they are added
        throw new UpdateException("this type of config cannot be updated");
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        // check the type and delete accordingly
        if (identityProviderConfig.type() == IdentityProviderType.LDAP) {
            ldapConfigManager.delete(identityProviderConfig);
        }
        // handle other types as they are added
        throw new DeleteException("this type of config cannot be deleted");
    }

    public Collection findAllIdentityProviders() throws FindException {
        // return collection of ldap ones and add the internal one
        return null;
    }

    public Collection findAllHeaders() throws FindException {
        // return collection of ldap ones and add the internal one
        // this is an unmodifiable collection so i must construct a new one
        Collection out = new ArrayList(ldapConfigManager.findAllHeaders());
        out.add(headerFromConfig(internalProvider.getConfig()));
        return out;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        // return collection of ldap ones and add the internal one
        Collection out = new ArrayList(ldapConfigManager.findAllHeaders(offset, windowSize-1));
        out.add(headerFromConfig(internalProvider.getConfig()));
        return out;
    }

    public Collection findAll() throws FindException {
        Collection out = new ArrayList(ldapConfigManager.findAll());
        out.add(internalProvider.getConfig());
        return out;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection out = new ArrayList(ldapConfigManager.findAll(offset, windowSize));
        out.add(internalProvider.getConfig());
        return out;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    protected EntityHeader headerFromConfig(IdentityProviderConfig cfg) {
        EntityHeader out = new EntityHeader();
        out.setDescription(cfg.getDescription());
        out.setName(cfg.getName());
        out.setOid(cfg.getOid());
        out.setType(EntityType.ID_PROVIDER_CONFIG);
        return out;
    }
    protected LdapIdentityProviderConfigManager ldapConfigManager;
    protected InternalIdentityProviderImp internalProvider;

}
