package com.l7tech.identity;

import com.l7tech.objectmodel.*;

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
        return (IdentityProviderConfig) dataStore.getIdentityProviderConfigs().get(new Long(oid));
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
        IdentityProviderConfig config;
        while (i.hasNext()) {
            config = (IdentityProviderConfig) i.next();
            providers.add(makeProvider(config));
        }
        return Collections.unmodifiableList(providers);
    }

    private Object makeProvider(IdentityProviderConfig config) {
        IdentityProvider provider = new IdentityProviderStub();
        provider.initialize(config);
        return provider;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> o
     * bjects for all instances of the entity class corresponding to this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
          dataStore.getIdentityProviderConfigs().keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromIdentityProviderConfig((IdentityProviderConfig) dataStore.getIdentityProviderConfigs().get(key)));
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
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromIdentityProviderConfig((IdentityProviderConfig) dataStore.getIdentityProviderConfigs().get(key)));
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
            Long key = (Long) i.next();
            list.add(dataStore.getIdentityProviderConfigs().get(key));
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
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(dataStore.getIdentityProviderConfigs().get(key));
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
        try {
            for (Iterator it = findAllIdentityProviders().iterator(); it.hasNext();) {
               IdentityProvider provider = (IdentityProvider)it.next();
                if (IdentityProviderType.INTERNAL.equals(provider.getConfig().type())) {
                    return provider;
                }
            }
            throw new RuntimeException(); //bug, no internal provider
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}
