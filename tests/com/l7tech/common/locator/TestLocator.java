package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A locator to be used by test classes.
 * To make it current, do the following:
 * System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.TestLocator");
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 24, 2004<br/>
 * $Id$<br/>
 */
public class TestLocator extends Locator {
    public Matches lookup(Template template) {
        logger.warning("lookup called but not implemented!");
        return null;
    }

    public Object lookup(Class clazz) {
        if (clazz.equals(IdentityProviderConfigManager.class)) {
            return getTestIdProvConfMan();
        } else {
            logger.warning("Being asked for unsupported type: " + clazz.getName());
        }
        return null;
    }

    private IdentityProviderConfigManager getTestIdProvConfMan() {
        return new IdentityProviderConfigManager() {
            public IdentityProvider getInternalIdentityProvider() {
                return idprovider;
            }
            public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
                return idprovider.getConfig();
            }
            public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
                throw new UnsupportedOperationException("not implemented");
            }
            public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {}
            public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {}
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
            public void test(IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException {}
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
            public Integer getVersion(long oid) throws FindException {return new Integer(1);}
            public Map findVersionMap() throws FindException {
                if (versionMap.isEmpty()) {
                    versionMap.put(new Long(TestIdentityProvider.PROVIDER_ID),
                                  new Integer(TestIdentityProvider.PROVIDER_VERSION));
                }
                return versionMap;
            }

            public Entity getCachedEntity( long o, int maxAge ) throws FindException, CacheVeto {
                throw new UnsupportedOperationException("not implemented");
            }

            private Map versionMap = new HashMap();
        };
    }

    private static Logger logger = Logger.getLogger(TestLocator.class.getName());
    private static final IdentityProvider idprovider = new TestIdentityProvider();
}
