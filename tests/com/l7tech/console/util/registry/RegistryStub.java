package com.l7tech.console.util.registry;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;


/**
 * Test, stub registry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryStub extends Registry {
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
     return null;
    }

    /**
     * @return the internal group manager
     */
    public GroupManager getInternalGroupManager() {
        return null;
    }

}
