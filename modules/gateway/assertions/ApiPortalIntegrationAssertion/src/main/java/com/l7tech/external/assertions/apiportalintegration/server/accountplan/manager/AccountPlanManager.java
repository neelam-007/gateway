package com.l7tech.external.assertions.apiportalintegration.server.accountplan.manager;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.accountplan.AccountPlan;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.util.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ConcurrentMap;

public class AccountPlanManager extends AbstractPortalGenericEntityManager<AccountPlan> {
    public AccountPlanManager(@NotNull final ApplicationContext applicationContext) {
        super(applicationContext);
        final GenericEntityManager genericEntityManager = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(AccountPlan.class);
        entityManager = genericEntityManager.getEntityManager(AccountPlan.class);
    }

    public static AccountPlanManager getInstance(final ApplicationContext context) {
        if (instance == null) {
            instance = new AccountPlanManager(context);
        }
        return instance;
    }

    @Override
    public String getCacheWipeIntervalConfigProperty() {
        return "accountPlanManager.cacheWipeInterval";
    }

    @Override
    public EntityManager<AccountPlan, GenericEntityHeader> getEntityManager() {
        return entityManager;
    }

    @Override
    public Object[] getUpdateLocks() {
        return updateLocks;
    }

    @Override
    public AccountPlan find(final String name) throws FindException {
        return find(name, true);//we don't want to cache account plans
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
    ConcurrentMap<Goid, String> getNameCache() {
        return nameCache;
    }
    private static AccountPlanManager instance;
    private final EntityManager<AccountPlan, GenericEntityHeader> entityManager;
    private static final int NUM_UPDATE_LOCKS = ConfigFactory.getIntProperty("accountPlanManager.numUpdateLocks", DEFAULT_NUM_UPDATE_LOCKS);
    private static final Object[] updateLocks = new Object[NUM_UPDATE_LOCKS];

    static {
        for (int i = 0; i < updateLocks.length; i++) {
            updateLocks[i] = new Object();
        }
    }
}