package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.RoleAwareEntityManager;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends EntityManager<IdentityProviderConfig, EntityHeader>, RoleAwareEntityManager<IdentityProviderConfig> {
    // since this provider config is not persisted, we need a special id to identify it for certain operations
    long INTERNALPROVIDER_SPECIAL_OID = -2L;

}
