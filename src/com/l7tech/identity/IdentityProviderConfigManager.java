package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.IdentityProviderConfig;

import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager {
    IdentityProvider getInternalIdentityProvider();
    IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException;
    long save( IdentityProviderConfig identityProviderConfig ) throws SaveException;
    void update( IdentityProviderConfig identityProviderConfig ) throws UpdateException;
    void delete( IdentityProviderConfig identityProviderConfig ) throws DeleteException;
    Collection findAllIdentityProviders() throws FindException;
}
