package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.IdentityProviderType;

/**
 * @author alex
 */
public interface IdentityProviderTypeManager extends EntityManager {
    public IdentityProviderType findByPrimaryKey( long oid ) throws FindException;
    public void delete( IdentityProviderType identityProviderType ) throws DeleteException;
    public long save( IdentityProviderType identityProviderType ) throws SaveException;
    public void update( IdentityProviderType identityProviderType ) throws UpdateException;
}
