package com.l7tech.console.util.registry;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditAdminStub;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.identity.*;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrarStub;
import com.l7tech.service.JmsAdminStub;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceAdminStub;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterStatusAdminStub;


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

    public IdentityAdmin getIdentityAdmin() {
        return identityAdmin;
    }

    public IdentityProviderConfig getInternalProviderConfig() {
        try {
            return identityAdmin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
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

    public AuditAdmin getAuditAdmin() {
        return auditAdmin;
    }

    public ClusterStatusAdmin getClusterStatusAdmin() {
        return clusterStatusAdmin;
    }

    public SecurityProvider getSecurityProvider() {
        return null; //todo: stub implementation!
    }

    StubDataStore dataStore = StubDataStore.defaultStore();

    private IdentityAdmin identityAdmin = new IdentityAdminStub();
    private ServiceAdmin serviceManager = new ServiceAdminStub();
    private JmsAdmin jmsAdmin = new JmsAdminStub();
    private CustomAssertionsRegistrar customAssertionsRegistrar = new CustomAssertionsRegistrarStub();
    private AuditAdmin auditAdmin = new AuditAdminStub();
    private ClusterStatusAdmin clusterStatusAdmin = new ClusterStatusAdminStub();
    private TrustedCertAdmin trustedCertAdmin;
}
