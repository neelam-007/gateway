package com.l7tech.identity;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;

import java.util.*;

/**
 * Class IdentityProviderConfigManagerStub.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class IdentityProviderConfigManagerStub implements IdentityProviderConfigManager {
    private StubDataStore dataStore;

    public IdentityProviderConfigManagerStub() {
        this.dataStore = StubDataStore.defaultStore();
    }


    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        return (IdentityProviderConfig)dataStore.getIdentityProviderConfigs().get(new Long(oid));
    }

    /**
     * @param oid the identity provider id to look for
     * @return the identoty provider for a given id, or <code>null</code>
     * @throws FindException if there was an persistence error 
     */
    public IdentityProvider getIdentityProvider(long oid) throws FindException {
        IdentityProviderConfig conf = findByPrimaryKey(oid);
        if (conf == null) return null;
        return makeProvider(conf);
    }

    public void test(IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException {
        throw new InvalidIdProviderCfgException("not implemented");
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        LdapIdentityProviderConfig[] output = (new LdapConfigTemplateManager()).getTemplates();
        if (output.length < 1) {
            throw new FindException("could not locate ldap template files. set value of " +
                        "com.l7tech.server.ldapTemplatesPath to the path containing those template files");
        }
        return output;
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        long oid = dataStore.nextObjectId();
        identityProviderConfig.setOid(oid);
        Long key = new Long(oid);
        if (dataStore.getIdentityProviderConfigs().get(key) != null) {
            throw new SaveException("Record exists, IdentityProviderConfig oid= " + identityProviderConfig.getOid());
        }
        dataStore.getIdentityProviderConfigs().put(key, identityProviderConfig);
        return oid;
    }

    public void update(IdentityProviderConfig c) throws UpdateException {
        Long key = new Long(c.getOid());
        if (dataStore.getIdentityProviderConfigs().get(key) == null) {
            throw new UpdateException("Record missing, IdentityProviderConfig oid= " + c.getOid());
        }
        dataStore.getIdentityProviderConfigs().remove(key);
        dataStore.getIdentityProviderConfigs().put(key, c);
    }

    public void delete(IdentityProviderConfig c) throws DeleteException {
        if (dataStore.getIdentityProviderConfigs().remove(new Long(c.getOid())) == null) {
            throw new DeleteException("Could not find IdentityProviderConfig oid= " + c.getOid());
        }
    }

    public Collection findAllIdentityProviders()
      throws FindException {
        List providers = new ArrayList();
        Iterator i = findAll().iterator();

        while (i.hasNext()) {
            IdentityProviderConfig
              config = (IdentityProviderConfig)i.next();
            providers.add(makeProvider(config));
        }
        return Collections.unmodifiableList(providers);
    }

    private IdentityProvider makeProvider(IdentityProviderConfig config) {
        IdentityProvider provider = new IdentityProviderStub(config);
        return provider;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects
     * for all instances of the entity class corresponding to this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
          dataStore.getIdentityProviderConfigs().keySet().iterator(); i.hasNext();) {
            Long key = (Long)i.next();
            IdentityProviderConfig c = (IdentityProviderConfig)dataStore.getIdentityProviderConfigs().get(key);
            list.add(fromIdentityProviderConfig(c));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
          dataStore.getIdentityProviderConfigs().keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long)i.next();
            if (index >= offset && count <= windowSize) {
                IdentityProviderConfig c =
                  (IdentityProviderConfig)dataStore.getIdentityProviderConfigs().get(key);
                list.add(fromIdentityProviderConfig(c));
                count++;
            }
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code>
     * objects for all instances of the entity class corresponding
     * to this Manager.
     *
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection findAll() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
          dataStore.getIdentityProviderConfigs().keySet().iterator(); i.hasNext();) {
            Long key = (Long)i.next();
            IdentityProviderConfig c = (IdentityProviderConfig)dataStore.getIdentityProviderConfigs().get(key);
            list.add(c);
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
          dataStore.getIdentityProviderConfigs().keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long)i.next();
            if (index >= offset && count <= windowSize) {
                IdentityProviderConfig c =
                  (IdentityProviderConfig)dataStore.getIdentityProviderConfigs().get(key);
                list.add(c);
                count++;
            }
        }
        return list;
    }

    private EntityHeader fromIdentityProviderConfig(IdentityProviderConfig c) {
        return
          new EntityHeader(c.getOid(), EntityType.ID_PROVIDER_CONFIG, c.getName(), null);
    }

    public IdentityProvider getInternalIdentityProvider() {
        for (Iterator i =
          dataStore.getIdentityProviderConfigs().keySet().iterator(); i.hasNext();) {
            Long key = (Long)i.next();
            IdentityProviderConfig c = (IdentityProviderConfig)dataStore.getIdentityProviderConfigs().get(key);
            if (IdentityProviderType.INTERNAL.equals(c.type()))
                return makeProvider(c);
        }
        throw new RuntimeException(); //bug, no internal provider
    }
}
