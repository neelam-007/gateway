package com.l7tech.identity.provider;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager {
    public IdentityProviderConfig findByPrimaryKey( long oid );
    public long save( IdentityProviderConfig identityProviderConfig );
    public void delete( IdentityProviderConfig identityProviderConfig );
}
