package com.l7tech.console.util.registry;

import com.l7tech.common.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.service.ServiceAdmin;


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
        return cm.getInternalIdentityProvider();
    }

    public IdentityProvider getIdentityProvider(long idProviderOid) {
       // todo, does the stub mode support anything other than the internal id provider?
        return cm.getInternalIdentityProvider();
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
    public ServiceAdmin getServiceManager() {
        return (ServiceAdmin)Locator.getDefault().lookup(ServiceAdmin.class);
    }

    StubDataStore dataStore = StubDataStore.defaultStore();

    private IdentityProviderConfigManager cm = new IdentityProviderConfigManagerStub();
    private UserManager um = new UserManagerStub(dataStore);
    private GroupManager gm = new GroupManagerStub(dataStore);

}
