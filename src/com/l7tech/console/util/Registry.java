package com.l7tech.console.util;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.common.util.Locator;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.console.tree.EntityHeaderNode;


/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class Registry {
    /**
     * A dummy registry that never returns any services.
     */
    public static final Registry EMPTY = new Empty();
    /** default instance */
    private static Registry instance;

    /**
     * Static method to obtain the global locator.
     * @return the global lookup in the system
     */
    public static synchronized Registry getDefault() {
        if (instance != null) {
            return instance;
        }
        instance = (Registry)Locator.getDefault().lookup(Registry.class);
        if (instance == null) {
            instance = EMPTY;
        }
        return instance;
    }

    /** Empty constructor for use by subclasses. */
    protected Registry() {
    }

    /**
     * @return the current window manager
     */
    public ComponentRegistry getComponentRegistry() {
        return ComponentRegistry.getInstance();
    }

    /**
     * @return the identity provider config manager
     */
    abstract public IdentityProviderConfigManager getProviderConfigManager();

    /**
     * @return the internal identity provider
     */
    abstract public IdentityProvider getInternalProvider();

    /**
     * @return the identity provider to which this node belongs to
     */
    abstract public IdentityProvider getIdentityProvider(EntityHeaderNode node);

    /**
     * @return the internal user manager
     */
    abstract public UserManager getInternalUserManager();

    /**
     * @return the internal group manager
     */
    abstract public GroupManager getInternalGroupManager();

    /**
     * @return the service managerr
     */
    abstract public ServiceAdmin getServiceManager();


    /**
     * Implementation of the default 'no-op' registry
     */
    private static final class Empty extends Registry {
        Empty() {
        }

        /**
         * @return the identity provider config manager
         */
        public IdentityProviderConfigManager getProviderConfigManager() {
            return null;
        }

        public IdentityProvider getIdentityProvider(EntityHeaderNode node) {
            return null;
        }

        public IdentityProvider getInternalProvider() {
            return null;
        }

        public UserManager getInternalUserManager() {
            return null;
        }

        public GroupManager getInternalGroupManager() {
            return null;
        }

        /**
         * @return the service managerr
         */
        public ServiceAdmin getServiceManager() {
            return null;
        }
    }

    /**
     * recycle the current locator, (used by testing)
     */
    protected static void recycle() {
        instance = null;
    }

}
