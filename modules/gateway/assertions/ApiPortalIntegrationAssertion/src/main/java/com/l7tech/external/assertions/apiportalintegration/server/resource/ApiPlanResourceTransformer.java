package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import org.jetbrains.annotations.NotNull;

/**
 * Handles transformation between ApiPlanResource and ApiPlan.
 */
public class ApiPlanResourceTransformer implements ResourceTransformer<ApiPlanResource, ApiPlan> {
    public static ApiPlanResourceTransformer getInstance() {
        if (instance == null) {
            instance = new ApiPlanResourceTransformer();
        }
        return instance;
    }

    @Override
    public ApiPlan resourceToEntity(@NotNull final ApiPlanResource resource) {
        final ApiPlan plan = new ApiPlan();
        plan.setName(resource.getPlanId());
        plan.setDescription(resource.getPlanName());
        plan.setPolicyXml(resource.getPolicyXml());
        plan.setDefaultPlan(resource.isDefaultPlan());
        plan.setLastUpdate(resource.getLastUpdate());

        ThroughputQuotaDetails quotaDetails = resource.getPlanDetails().getThroughputQuota();
        plan.setThroughputQuotaEnabled(quotaDetails.isEnabled());
        plan.setQuota(quotaDetails.getQuota());
        plan.setTimeUnit(quotaDetails.getTimeUnit());
        plan.setCounterStrategy(quotaDetails.getCounterStrategy());

        RateLimitDetails rateLimitDetails = resource.getPlanDetails().getRateLimit();
        plan.setRateLimitEnabled(rateLimitDetails.isEnabled());
        plan.setMaxRequestRate(rateLimitDetails.getMaxRequestRate());
        plan.setWindowSizeInSeconds(rateLimitDetails.getWindowSizeInSeconds());
        plan.setHardLimit(rateLimitDetails.isHardLimit());

        return plan;
    }

    @Override
    public ApiPlanResource entityToResource(@NotNull final ApiPlan entity) {
        return new ApiPlanResource(entity.getName(), entity.getDescription(), entity.getLastUpdate(), entity.getPolicyXml(), 
            entity.isDefaultPlan(), new PlanDetails(
                new ThroughputQuotaDetails(
                    entity.isThroughputQuotaEnabled(),
                    entity.getQuota(),
                    entity.getTimeUnit(),
                    entity.getCounterStrategy()),
                new RateLimitDetails(
                    entity.isRateLimitEnabled(),
                    entity.getMaxRequestRate(),
                    entity.getWindowSizeInSeconds(),
                    entity.isHardLimit()
                )
            ));
    }

    private static ApiPlanResourceTransformer instance;
}
