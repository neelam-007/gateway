package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.EntityManagerStub;
import org.springframework.beans.factory.InitializingBean;

public class TestIdentityProviderConfigManager
        extends EntityManagerStub<IdentityProviderConfig, EntityHeader>
        implements IdentityProviderConfigManager, InitializingBean
{
    public TestIdentityProviderConfigManager() {
    }

    public TestIdentityProviderConfigManager( final IdentityProviderConfig... entitiesIn ) {
        super( entitiesIn );
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
        this.entities.put(config.getGoid(), config);
    }
}
