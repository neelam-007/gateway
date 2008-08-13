package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.SaveException;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager<IdentityProviderConfig, EntityHeader> {
    // since this provider config is not persisted, we need a special id to identify it for certain operations
    long INTERNALPROVIDER_SPECIAL_OID = -2;

    /**
     * Create a new Role for the specified IdentityProviderConfig.
     *
     * @param config  the config for which a new Role is to be created.  Must not be null, and must not already have a Role.
     * @throws com.l7tech.objectmodel.SaveException if there was a problem saving the new Role.
     */
    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException;
}
