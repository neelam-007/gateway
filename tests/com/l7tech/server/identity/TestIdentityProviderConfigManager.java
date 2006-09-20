package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
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

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        return idprovider.getConfig();
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
    }

    public Collection findAllIdentityProviders() throws FindException {
        Collection output = new ArrayList();
        output.add(idprovider);
        return output;
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        throw new UnsupportedOperationException("not implemented");
    }

    public IdentityProvider getIdentityProvider(long oid) throws FindException {
        return idprovider;
    }

    public void test(IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException {
    }

    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException {
        // No-op for stub mode
    }

    public Collection findAll() throws FindException {
        Collection output = new ArrayList();
        output.add(idprovider.getConfig());
        return output;
    }

    public Integer getVersion(long oid) throws FindException {
        return new Integer(1);
    }

    public Map findVersionMap() throws FindException {
        if (versionMap.isEmpty()) {
            versionMap.put(new Long(TestIdentityProvider.PROVIDER_ID),
              new Integer(TestIdentityProvider.PROVIDER_VERSION));
        }
        return versionMap;
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
        idprovider =  identityProviderFactory.createProviderInstance(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
    }
}
