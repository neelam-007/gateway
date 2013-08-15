package com.l7tech.identity;


import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntityManager;
import com.l7tech.objectmodel.RoleAwareGoidEntityManager;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends GoidEntityManager<IdentityProviderConfig, EntityHeader>, RoleAwareGoidEntityManager<IdentityProviderConfig> {
    // since this provider config is not persisted, we need a special id to identify it for certain operations
    Goid INTERNALPROVIDER_SPECIAL_GOID = new Goid(0,-2L);
    long INTERNALPROVIDER_SPECIAL_OLD_OID = -2L;

}
