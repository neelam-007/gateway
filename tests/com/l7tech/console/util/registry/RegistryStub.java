package com.l7tech.console.util.registry;

import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;


/**
 * Test, stub registry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryStub extends Registry {
    /**
     * @return the identity provider config manager
     */
    public IdentityProviderConfigManager getProviderConfigManager() {
        return null;
    }

    /**
     * @return the internal identity provider
     */
    public IdentityProvider getInternalProvider() {
        return null;
    }

    /**
     * @return the internal user manager
     */
    public UserManager getInternalUserManager() {
     return um;
    }

    /**
     * @return the internal group manager
     */
    public GroupManager getInternalGroupManager() {
        return gm;
    }

    private UserManager um = new UserManagerStub();
    private GroupManager gm = new GroupManagerStub();
}
