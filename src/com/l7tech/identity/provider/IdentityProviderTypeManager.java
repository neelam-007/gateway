package com.l7tech.identity.provider;

import com.l7tech.objectmodel.Manager;

/**
 * @author alex
 */
public interface IdentityProviderTypeManager extends Manager {
    public void delete( IdentityProviderType identityProviderType );
    public IdentityProviderType create();
}
