package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.accountplan.AccountPlan;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountPlanResourceTransformerTest {

    private static final String PLAN_NAME = "The Plan";
    private static final String POLICY_XML = "the xml";
    private static final String ORG_IDS = "1,2";
    private static final Date DATE = new Date();
    public static final boolean DEFAULT_PLAN_ENABLED = true;
    public static final boolean THROUGHPUT_QUOTA_ENABLED = true;
    public static final int QUOTA_10 = 10;
    public static final int TIME_UNIT_1 = 1;
    public static final int COUNTER_STRATEGY_1 = 1;
    public static final boolean RATE_LIMIT_ENABLED = true;
    public static final int MAX_REQUEST_RATE = 100;
    public static final int WINDOW_SIZE = 60;
    public static final boolean HARD_LIMIT = true;

    private AccountPlanResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new AccountPlanResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final AccountPlan entity = transformer.resourceToEntity(
                new AccountPlanResource("id", "name", DATE, true, new PlanDetails(
                        new ThroughputQuotaDetails(
                                DEFAULT_PLAN_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1
                        ),
                        new RateLimitDetails(
                                RATE_LIMIT_ENABLED, MAX_REQUEST_RATE, WINDOW_SIZE, HARD_LIMIT
                        )
                ), new AccountPlanMapping(ORG_IDS), "policy xml"));

        assertEquals("id", entity.getName());
        assertEquals("name", entity.getDescription());
        assertEquals(DATE, entity.getLastUpdate());
        assertTrue(entity.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, entity.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10, entity.getQuota());
        assertEquals(TIME_UNIT_1, entity.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, entity.getCounterStrategy());
        assertEquals(RATE_LIMIT_ENABLED, entity.isRateLimitEnabled());
        assertEquals(MAX_REQUEST_RATE, entity.getMaxRequestRate());
        assertEquals(WINDOW_SIZE, entity.getWindowSizeInSeconds());
        assertEquals(HARD_LIMIT, entity.isHardLimit());
        assertEquals(ORG_IDS, entity.getIds());
        assertTrue(ORG_IDS.equals(entity.getIds()));
        assertEquals("policy xml", entity.getPolicyXml());
    }

    @Test
    public void entityToResource() {
        final AccountPlan entity = new AccountPlan();
        entity.setName("id");
        entity.setDescription("name");
        entity.setLastUpdate(DATE);
        entity.setDefaultPlan(true);
        entity.setThroughputQuotaEnabled(THROUGHPUT_QUOTA_ENABLED);
        entity.setQuota(QUOTA_10);
        entity.setTimeUnit(TIME_UNIT_1);
        entity.setCounterStrategy(COUNTER_STRATEGY_1);
        entity.setRateLimitEnabled(RATE_LIMIT_ENABLED);
        entity.setMaxRequestRate(MAX_REQUEST_RATE);
        entity.setWindowSizeInSeconds(WINDOW_SIZE);
        entity.setHardLimit(HARD_LIMIT);
        entity.setIds(ORG_IDS);
        entity.setPolicyXml("policy xml");
        final AccountPlanResource resource = transformer.entityToResource(entity);

        assertEquals("id", resource.getPlanId());
        assertEquals("name", resource.getPlanName());
        assertEquals(DATE, resource.getLastUpdate());
        assertTrue(resource.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, resource.getPlanDetails().getThroughputQuota().isEnabled());
        assertEquals(QUOTA_10, resource.getPlanDetails().getThroughputQuota().getQuota());
        assertEquals(TIME_UNIT_1, resource.getPlanDetails().getThroughputQuota().getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, resource.getPlanDetails().getThroughputQuota().getCounterStrategy());
        assertEquals(RATE_LIMIT_ENABLED, resource.getPlanDetails().getRateLimit().isEnabled());
        assertEquals(MAX_REQUEST_RATE, resource.getPlanDetails().getRateLimit().getMaxRequestRate());
        assertEquals(WINDOW_SIZE, resource.getPlanDetails().getRateLimit().getWindowSizeInSeconds());
        assertEquals(HARD_LIMIT, resource.getPlanDetails().getRateLimit().isHardLimit());
        assertEquals(ORG_IDS, resource.getPlanMapping().getIds());
        assertTrue(ORG_IDS.equals(resource.getPlanMapping().getIds()));
        assertEquals("policy xml", resource.getPolicyXml());

    }
}
