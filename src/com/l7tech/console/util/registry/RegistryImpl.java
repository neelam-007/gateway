package com.l7tech.console.util.registry;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;



/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryImpl extends Registry {
    /**
     * @return the {@link IdentityAdmin} implementation
     */
    public IdentityAdmin getIdentityAdmin() {
        IdentityAdmin admin = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
        if (admin == null) throw new RuntimeException("Could not get " + IdentityAdmin.class);
        return admin;
    }

    /**
     * @return the internal identity provider
     */
    public IdentityProviderConfig getInternalProviderConfig() {
        IdentityAdmin admin = getIdentityAdmin();
        if (admin == null) {
            throw new RuntimeException("Could not get " + IdentityAdmin.class);
        }
        try {
            return admin.findIdentityProviderConfigByPrimaryKey(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        } catch (Exception e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the service manager
     */
    public ServiceAdmin getServiceManager() {
        ServiceAdmin sm = (ServiceAdmin)Locator.getDefault().lookup(ServiceAdmin.class);
        if (sm == null) {
            throw new RuntimeException("Could not get " + ServiceAdmin.class);
        }
        return sm;
    }

    /**
     * @return the JMS manager
     */
    public JmsAdmin getJmsManager() {
        JmsAdmin ja = (JmsAdmin)Locator.getDefault().lookup(JmsAdmin.class);
        if (ja == null) {
            throw new RuntimeException("Could not get " + JmsAdmin.class);
        }
        return ja;
    }

    /**
     * @return the Trusted Cert Manager
     */
    public TrustedCertAdmin getTrustedCertManager() {
        TrustedCertAdmin tca = (TrustedCertAdmin)Locator.getDefault().lookup(TrustedCertAdmin.class);
        if (tca == null) {
            throw new RuntimeException("Could not get " + TrustedCertAdmin.class);
        }
        return tca;
    }

    /**
     * @return the custome assertions registrar
     */
    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        CustomAssertionsRegistrar cr =
          (CustomAssertionsRegistrar)Locator.getDefault().lookup(CustomAssertionsRegistrar.class);
        if (cr == null) {
            throw new RuntimeException("Could not get " + CustomAssertionsRegistrar.class);
        }
        return cr;
    }
}
