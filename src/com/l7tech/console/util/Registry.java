package com.l7tech.console.util;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;


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
    /**
     * default instance
     */
    private static Registry instance;

    /**
     * Static method to obtain the global locator.
     *
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

    /**
     * Empty constructor for use by subclasses.
     */
    protected Registry() {
    }

    /**
     * @return the {@link IdentityAdmin} implementation
     */
    public abstract IdentityAdmin getIdentityAdmin();

    /**
     * @return the {@link IdentityProviderConfig} object for the internal identity provider
     */
    abstract public IdentityProviderConfig getInternalProviderConfig();

    /**
     * @return the service managerr
     */
    abstract public ServiceAdmin getServiceManager();

    /**
     * @return the jms provider manager
     */
    abstract public JmsAdmin getJmsManager();

    abstract public TrustedCertAdmin getTrustedCertManager();
    /**
     * @return the custome assertions registrar
     */
    abstract public CustomAssertionsRegistrar getCustomAssertionsRegistrar();


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


        public IdentityAdmin getIdentityAdmin() {
            return null;
        }

        public IdentityProviderConfig getInternalProviderConfig() {
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

        public JmsAdmin getJmsManager() {
            return null;
        }

        public TrustedCertAdmin getTrustedCertManager() {
            return null;
        }

        /**
         * @return the custome assertions registrar
         */
        public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
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
