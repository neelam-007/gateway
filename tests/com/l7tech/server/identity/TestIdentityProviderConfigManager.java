package com.l7tech.server.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.*;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * @author emil
 * @version 14-Dec-2004
 */
public class TestIdentityProviderConfigManager implements IdentityProviderConfigManager {
    public IdentityProvider getInternalIdentityProvider() {
        return idprovider;
    }

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        return idprovider.getConfig();
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        throw new UnsupportedOperationException("not implemented");
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
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

    public Collection findAllHeaders() throws FindException {
        throw new UnsupportedOperationException("not implemented");
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException("not implemented");
    }

    public Collection findAll() throws FindException {
        Collection output = new ArrayList();
        output.add(idprovider.getConfig());
        return output;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException("not implemented");
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

    public Entity getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
        throw new UnsupportedOperationException("not implemented");
    }

    private static final IdentityProvider idprovider = new TestIdentityProvider();

    private Map versionMap = new HashMap();
}
