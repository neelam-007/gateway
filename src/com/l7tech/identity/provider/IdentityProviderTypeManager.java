package com.l7tech.identity.provider;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface IdentityProviderTypeManager extends EntityManager {
    public IdentityProviderType findByPrimaryKey( long oid );
    public void delete( IdentityProviderType identityProviderType );
    public long save( IdentityProviderType identityProviderType );
}
