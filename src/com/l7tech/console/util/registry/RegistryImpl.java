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
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryImpl extends Registry {

    /**
     * @return the identity provider config manager
     */
    public IdentityProviderConfigManager getProviderConfigManager()
      throws RuntimeException {
        IdentityProviderConfigManager ipc =
        (IdentityProviderConfigManager)Locator.
                getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered "+IdentityProviderConfigManager.class);
        }
        return ipc;
    }

    /**
     * @return the internal identity provider
     */
    public IdentityProvider getInternalProvider() {
        try {
            IdentityProviderConfigManager ipc = getProviderConfigManager();
            if (ipc == null) {
                throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
            }
            Collection ips = ipc.findAllIdentityProviders();

            if (ips.isEmpty()) {
                throw new RuntimeException("Could not retrieve identity providers.");
            }
            return (IdentityProvider) ips.iterator().next();
        } catch (FindException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @return the internal user manager
     */
    public UserManager getInternalUserManager() {
     return getInternalProvider().getUserManager();
    }

    /**
     * @return the internal group manager
     */
    public GroupManager getInternalGroupManager() {
     return getInternalProvider().getGroupManager();
    }
}
