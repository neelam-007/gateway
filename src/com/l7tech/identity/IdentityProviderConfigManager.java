package com.l7tech.identity;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager {
    // since this provider config is not persisted, we need a special id to identify it for certain operations
    long INTERNALPROVIDER_SPECIAL_OID = -2;

    IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException;
    long save( IdentityProviderConfig identityProviderConfig ) throws SaveException;
    void update( IdentityProviderConfig identityProviderConfig ) throws UpdateException;
    void delete( IdentityProviderConfig identityProviderConfig ) throws DeleteException;
    Collection findAllIdentityProviders() throws FindException;
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
}
