package com.l7tech.external.assertions.apiportalintegration.server.apiplan.manager;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.util.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ConcurrentMap;

public class ApiPlanManager extends AbstractPortalGenericEntityManager<ApiPlan> {
    public ApiPlanManager(@NotNull final ApplicationContext applicationContext) {
        super(applicationContext);
        final GenericEntityManager genericEntityManager = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(ApiPlan.class);
        entityManager = genericEntityManager.getEntityManager(ApiPlan.class);
    }

    public static ApiPlanManager getInstance(final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiPlanManager(context);
        }
        return instance;
    }

    @Override
    public String getCacheWipeIntervalConfigProperty() {
        return "apiPlanManager.cacheWipeInterval";
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public Object[] getUpdateLocks() {
        return updateLocks;
    }

    /**
     * Provide restricted access to the cache for unit tests.
     */
    ConcurrentMap<String, AbstractPortalGenericEntity> getCache() {
        return cache;
    }

    /**
     * Provide restricted access to the name cache for unit tests.
     */
    ConcurrentMap<Long, String> getNameCache() {
        return nameCache;
    }
    private static ApiPlanManager instance;
    private final EntityManager<ApiPlan, GenericEntityHeader> entityManager;
    private static final int NUM_UPDATE_LOCKS = ConfigFactory.getIntProperty("apiPlanManager.numUpdateLocks", DEFAULT_NUM_UPDATE_LOCKS);
    private static final Object[] updateLocks = new Object[NUM_UPDATE_LOCKS];

    static {
        for (int i = 0; i < updateLocks.length; i++) {
            updateLocks[i] = new Object();
        }
    }
}
