package com.l7tech.identity;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.identity.IdentityProviderConfig;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager {
    public IdentityProviderConfig findByPrimaryKey( long oid );
    public long save( IdentityProviderConfig identityProviderConfig );
    public void delete( IdentityProviderConfig identityProviderConfig );
}
