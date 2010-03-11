package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.EntityManagerStub;
import org.springframework.beans.factory.InitializingBean;

public class TestIdentityProviderConfigManager
        extends EntityManagerStub<IdentityProviderConfig, EntityHeader>
        implements IdentityProviderConfigManager, InitializingBean
{
    public TestIdentityProviderConfigManager() {
        super();
    }

    public TestIdentityProviderConfigManager( final IdentityProviderConfig... entitiesIn ) {
        super( entitiesIn );
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException {
        // No-op for stub mode
    }

    @Override
    public Class<IdentityProviderConfig> getImpClass() {
        return IdentityProviderConfig.class;
    }

    @Override
    public Class<IdentityProviderConfig> getInterfaceClass() {
        return IdentityProviderConfig.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final IdentityProviderConfig config = TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG;
        this.entities.put(config.getOid(), config);
    }
}
