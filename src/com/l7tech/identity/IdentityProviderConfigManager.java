package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.IdentityProviderConfig;

import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager {
    public IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException;
    public long save( IdentityProviderConfig identityProviderConfig ) throws SaveException;
    public void delete( IdentityProviderConfig identityProviderConfig ) throws DeleteException;

    public Collection findAllIdentityProviders() throws FindException;
}
