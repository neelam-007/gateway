package com.l7tech.console.util.registry;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrarStub;
import com.l7tech.service.JmsAdminStub;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceAdminStub;


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
        return identityProviderConfigManager;
    }

    /**
     * @return the internal identity provider
     */
    public IdentityProvider getInternalProvider() {
        return identityProviderConfigManager.getInternalIdentityProvider();
    }

    public IdentityProvider getIdentityProvider(long idProviderOid) {
       // todo, does the stub mode support anything other than the internal id provider?
        return identityProviderConfigManager.getInternalIdentityProvider();
    }

    /**
     * @return the internal user manager
     */
    public UserManager getInternalUserManager() {
     return userManager;
    }

    /**
     * @return the internal group manager
     */
    public GroupManager getInternalGroupManager() {
        return groupManager;
    }

    /**
     * @return the service managerr
     */
    public ServiceAdmin getServiceManager() {
        return serviceManager;
    }

    /**
     * @return the jms provider manager
     */
    public JmsAdmin getJmsManager() {
        return jmsAdmin;
    }

    public TrustedCertAdmin getTrustedCertManager() {
        return trustedCertAdmin;
    }

    /**
     * @return the custome assertions registrar
     */
    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        return customAssertionsRegistrar;
    }

    StubDataStore dataStore = StubDataStore.defaultStore();

    private IdentityProviderConfigManager identityProviderConfigManager = new IdentityProviderConfigManagerStub();
    private UserManager userManager = new UserManagerStub(dataStore);
    private GroupManager groupManager = new GroupManagerStub(dataStore);
    private ServiceAdmin serviceManager = new ServiceAdminStub();
    private JmsAdmin jmsAdmin = new JmsAdminStub();
    private CustomAssertionsRegistrar customAssertionsRegistrar = new CustomAssertionsRegistrarStub();

    private TrustedCertAdmin trustedCertAdmin;
}
