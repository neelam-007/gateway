package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class ApiPlanResourceTransformerTest {
    private static final Date LAST_UPDATE = new Date();
    public static final boolean THROUGHPUT_QUOTA_ENABLED = true;
    public static final int QUOTA_10 = 10;
    public static final int TIME_UNIT_1 = 1;
    public static final int COUNTER_STRATEGY_1 = 1;
    public static final boolean RATE_LIMIT_ENABLED = true;
    public static final int MAX_REQUEST_RATE = 100;
    public static final int WINDOW_SIZE = 60;
    public static final boolean HARD_LIMIT = true;
    private ApiPlanResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new ApiPlanResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final ApiPlan entity = transformer.resourceToEntity(new ApiPlanResource("id", "name", LAST_UPDATE, "policy xml", true, new PlanDetails(
                        new ThroughputQuotaDetails(
                                THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1
                        ),
                        new RateLimitDetails(
                                RATE_LIMIT_ENABLED, MAX_REQUEST_RATE, WINDOW_SIZE, HARD_LIMIT
                        )
                )));

        assertEquals("id", entity.getName());
        assertEquals("name", entity.getDescription());
        assertEquals(LAST_UPDATE, entity.getLastUpdate());
        assertTrue(entity.isDefaultPlan());

        assertEquals(THROUGHPUT_QUOTA_ENABLED, entity.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10, entity.getQuota());
        assertEquals(TIME_UNIT_1, entity.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, entity.getCounterStrategy());
        assertEquals(RATE_LIMIT_ENABLED, entity.isRateLimitEnabled());
        assertEquals(MAX_REQUEST_RATE, entity.getMaxRequestRate());
        assertEquals(WINDOW_SIZE, entity.getWindowSizeInSeconds());
        assertEquals(HARD_LIMIT, entity.isHardLimit());
        assertEquals("policy xml", entity.getPolicyXml());
    }

    @Test
    public void entityToResource() {
        final ApiPlan entity = new ApiPlan();
        entity.setName("id");
        entity.setDescription("name");
        entity.setLastUpdate(LAST_UPDATE);
        entity.setDefaultPlan(true);
        entity.setThroughputQuotaEnabled(THROUGHPUT_QUOTA_ENABLED);
        entity.setQuota(QUOTA_10);
        entity.setTimeUnit(TIME_UNIT_1);
        entity.setCounterStrategy(COUNTER_STRATEGY_1);
        entity.setRateLimitEnabled(RATE_LIMIT_ENABLED);
        entity.setMaxRequestRate(MAX_REQUEST_RATE);
        entity.setWindowSizeInSeconds(WINDOW_SIZE);
        entity.setHardLimit(HARD_LIMIT);
        entity.setPolicyXml("policy xml");
        final ApiPlanResource resource = transformer.entityToResource(entity);

        assertEquals("id", resource.getPlanId());
        assertEquals("name", resource.getPlanName());
        assertEquals(LAST_UPDATE, resource.getLastUpdate());
        assertTrue(resource.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, resource.getPlanDetails().getThroughputQuota().isEnabled());
        assertEquals(QUOTA_10, resource.getPlanDetails().getThroughputQuota().getQuota());
        assertEquals(TIME_UNIT_1, resource.getPlanDetails().getThroughputQuota().getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, resource.getPlanDetails().getThroughputQuota().getCounterStrategy());
        assertEquals(RATE_LIMIT_ENABLED, resource.getPlanDetails().getRateLimit().isEnabled());
        assertEquals(MAX_REQUEST_RATE, resource.getPlanDetails().getRateLimit().getMaxRequestRate());
        assertEquals(WINDOW_SIZE, resource.getPlanDetails().getRateLimit().getWindowSizeInSeconds());
        assertEquals(HARD_LIMIT, resource.getPlanDetails().getRateLimit().isHardLimit());
        assertEquals("policy xml", resource.getPolicyXml());
    }
}
