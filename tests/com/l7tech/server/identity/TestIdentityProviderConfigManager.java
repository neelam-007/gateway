package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityManagerStub;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.EntityHeader;

import org.springframework.beans.factory.InitializingBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author emil
 * @version 14-Dec-2004
 */
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

    public EntityType getEntityType() {
        return EntityType.ID_PROVIDER_CONFIG;
    }

    public String getTableName() {
        return "identity_provider";
    }

    public void afterPropertiesSet() throws Exception {
        final IdentityProviderConfig config = TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG;
        this.entities.put(config.getOid(), config);
    }
}
