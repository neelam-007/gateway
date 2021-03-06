package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.util.ConfigFactory;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ConcurrentMap;

/**
 * An API key manager that uses a generic entity manager to store/retrieve API keys.
 * <p/>
 * Loaded API keys will be cached in RAM.  The cache is cleared occasionally (daily by default), and specific
 * entries are removed whenever their corresponding generic entity is updated (anywhere on the cluster).
 * <p/>
 * No caching of failed lookups (negative caching) is currently performed.
 */
public class ApiKeyManagerImpl extends AbstractPortalGenericEntityManager<ApiKeyData> {

    private final GenericEntityManager genericEntityManager;

    public ApiKeyManagerImpl(final ApplicationContext context) {
        super(context);
        genericEntityManager = context.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(ApiKeyData.class, PORTAL_GENERIC_ENTITY_METADATA);
        entityManager = genericEntityManager.getEntityManager(ApiKeyData.class);

    }

    @Override
    public void unRegister() {
        genericEntityManager.unRegisterClass(ApiKeyData.class.getName());
    }


    @Override
    public EntityManager<ApiKeyData, GenericEntityHeader> getEntityManager() {
        return entityManager;
    }
                                                                                                                                                                                      ;
    @Override
    public Object[] getUpdateLocks() {
        return updateLocks;
    }

    @Override
    public String getCacheWipeIntervalConfigProperty() {
        return "apiKeyManager.cacheWipeInterval";
    }

    /**
     * Provide restricted access to the name cache for unit tests.
     */
    ConcurrentMap<Goid, String> getNameCache() {
        return nameCache;
    }

    /**
     * Provide restricted access to the cache for unit tests.
     */
    ConcurrentMap<String, AbstractPortalGenericEntity> getCache() {
        return cache;
    }

    private final EntityManager<ApiKeyData, GenericEntityHeader> entityManager;
    private static final int NUM_UPDATE_LOCKS = ConfigFactory.getIntProperty("com.l7tech.apiportal.ApiKeyManager.numUpdateLocks", DEFAULT_NUM_UPDATE_LOCKS);

    private static final Object[] updateLocks = new Object[NUM_UPDATE_LOCKS];

    static {
        for (int i = 0; i < updateLocks.length; i++) {
            updateLocks[i] = new Object();
        }
    }
}
