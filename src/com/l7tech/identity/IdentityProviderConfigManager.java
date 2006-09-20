package com.l7tech.identity;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager<IdentityProviderConfig, EntityHeader> {
    // since this provider config is not persisted, we need a special id to identify it for certain operations
    long INTERNALPROVIDER_SPECIAL_OID = -2;

    Collection<IdentityProvider> findAllIdentityProviders() throws FindException;

    LdapIdentityProviderConfig[] getLdapTemplates() throws FindException;

    /**
     * @param oid the identity provider id to look for
     * @return the identoty provider for a given id, or <code>null</code>
     * @throws FindException if there was an persistence error
     */
    IdentityProvider getIdentityProvider(long oid) throws FindException;

    /**
     * Allows the administrator to test the validity of a new IPC before saving
     * it.
     *
     * If the IPC is not valid an InvalidIdProviderCfgException is thrown
     * 
     * @param identityProviderConfig the new config object (not yet saved)
     */
    void test(IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException;

    /**
     * Create a new Role for the specified IdentityProviderConfig.
     *
     * @param config  the config for which a new Role is to be created.  Must not be null, and must not already have a Role.
     * @throws com.l7tech.objectmodel.SaveException if there was a problem saving the new Role.
     */
    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException;
}
