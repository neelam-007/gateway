package com.l7tech.console.util.registry;

import com.l7tech.common.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;



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
     * @return the internal identity provider
     */
    public IdentityProvider getInternalProvider() {
        IdentityProviderConfigManager ipc = getProviderConfigManager();
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }
        return ipc.getInternalIdentityProvider();
    }

    /**
      * @return the identity provider given the oid of the identity provider
      */
     public IdentityProvider getIdentityProvider(long idProviderOid) {
         IdentityProviderConfigManager ipc = getProviderConfigManager();
         try {
             return ipc.getIdentityProvider(idProviderOid);
         } catch (FindException e) {
             throw new RuntimeException("could not find related identity provider", e);
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

    /**
     * @return the service manager
     */
    public ServiceAdmin getServiceManager() {
        ServiceAdmin sm = (ServiceAdmin)Locator.getDefault().lookup(ServiceAdmin.class);
        if (sm == null) {
            throw new RuntimeException("Could not find registered " + ServiceAdmin.class);
        }
        return sm;
    }

    /**
     * @return the JMS manager
     */
    public JmsAdmin getJmsManager() {
        JmsAdmin ja = (JmsAdmin)Locator.getDefault().lookup(JmsAdmin.class);
        if (ja == null) {
            throw new RuntimeException("Could not find registered " + JmsAdmin.class);
        }
        return ja;
    }

    /**
     * @return the custome assertions registrar
     */
    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        CustomAssertionsRegistrar cr =
          (CustomAssertionsRegistrar)Locator.getDefault().lookup(CustomAssertionsRegistrar.class);
        if (cr == null) {
            throw new RuntimeException("Could not find registered " + CustomAssertionsRegistrar.class);
        }
        return cr;
    }
}
