package com.l7tech.console.util.registry;

import com.l7tech.identity.*;
import com.l7tech.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceManager;

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
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }
        return ipc;
    }

    /**
     * @return the identity provider config manager
     */
    private GlobalIdProviderConfManager getGlobalIdProviderConfManager()
      throws RuntimeException {
        GlobalIdProviderConfManager ipc =
          (GlobalIdProviderConfManager)Locator.
          getDefault().lookup(GlobalIdProviderConfManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }
        return ipc;
    }


    /**
     * @return the internal identity provider
     */
    public IdentityProvider getInternalProvider() {
        GlobalIdProviderConfManager ipc = getGlobalIdProviderConfManager();
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + GlobalIdProviderConfManager.class);
        }
        return ipc.getInternalIdentityProvider();
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

    /**
     * @return the service managerr
     */
    public ServiceManager getServiceManager() {
        ServiceManager sm =
          (ServiceManager)Locator.
          getDefault().lookup(ServiceManager.class);
        if (sm == null) {
            throw new RuntimeException("Could not find registered " + ServiceManager.class);
        }
        return sm;
    }
}
