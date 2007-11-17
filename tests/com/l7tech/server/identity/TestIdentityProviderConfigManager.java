package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityManagerStub;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
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
        extends EntityManagerStub<IdentityProviderConfig>
        implements IdentityProviderConfigManager, InitializingBean
{
    private IdentityProviderFactory identityProviderFactory;
    private IdentityProvider idprovider;

    private Map versionMap = new HashMap();
    public IdentityProvider getInternalIdentityProvider() {
        return idprovider;
    }

    public Collection<IdentityProvider> findAllIdentityProviders() throws FindException {
        return Arrays.asList(idprovider);
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        throw new UnsupportedOperationException("not implemented");
    }

    public IdentityProvider getIdentityProvider(long oid) throws FindException {
        if (oid != idprovider.getConfig().getOid()) throw new IllegalArgumentException();
        return idprovider;
    }

    public void test(IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException {
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

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
         this.identityProviderFactory = identityProviderFactory;
     }

    public void afterPropertiesSet() throws Exception {
        if (identityProviderFactory == null) {
            throw new IllegalArgumentException("Identity Provider Factory is required");
        }
        final IdentityProviderConfig config = TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG;
        idprovider =  identityProviderFactory.createProviderInstance(config);
        this.entities.put(config.getOid(), config);
    }
}
