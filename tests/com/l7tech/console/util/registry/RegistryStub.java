package com.l7tech.console.util.registry;

import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.service.ServiceManager;
import com.l7tech.util.Locator;

import java.io.FileNotFoundException;


/**
 * Test, stub registry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryStub extends Registry {
    /**
     * default constructor
     */
    public RegistryStub() {
    }
    /**
     * @return the identity provider config manager
     */
    public IdentityProviderConfigManager getProviderConfigManager() {
        return cm;
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

    /**
     * @return the service managerr
     */
    public ServiceManager getServiceManager() {
        return (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
    }

    StubDataStore dataStore = null;
    {
        try {
          dataStore = new StubDataStore(StubDataStore.PATH);
        } catch(FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private IdentityProviderConfigManager cm = new IdentityProviderConfigManagerStub(dataStore);
    private UserManager um = new UserManagerStub(dataStore);
    private GroupManager gm = new GroupManagerStub(dataStore);

}
