package com.l7tech.identity;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.identity.IdentityProviderType;

/**
 * @author alex
 */
public interface IdentityProviderTypeManager extends EntityManager {
    public IdentityProviderType findByPrimaryKey( long oid );
    public void delete( IdentityProviderType identityProviderType );
    public long save( IdentityProviderType identityProviderType );
}
