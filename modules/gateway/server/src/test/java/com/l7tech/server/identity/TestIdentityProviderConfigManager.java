package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.EntityManagerStub;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

public class TestIdentityProviderConfigManager
        extends EntityManagerStub<IdentityProviderConfig, EntityHeader>
        implements IdentityProviderConfigManager, InitializingBean
{
    private IdentityProvider idprovider;

    private Map versionMap = new HashMap();
    public IdentityProvider getInternalIdentityProvider() {
        return idprovider;
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        throw new UnsupportedOperationException("not implemented");
    }

    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException {
        // No-op for stub mode
    }

    public Class getImpClass() {
        return IdentityProviderConfig.class;
    }

    public Class getInterfaceClass() {
        return IdentityProviderConfig.class;
    }

    public void afterPropertiesSet() throws Exception {
        final IdentityProviderConfig config = TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG;
        this.entities.put(config.getOid(), config);
    }
}
