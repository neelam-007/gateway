package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.accountplan.AccountPlan;
import org.jetbrains.annotations.NotNull;

/**
 * Handles transformation between AccountPlanResource and AccountPlan.
 */
public class AccountPlanResourceTransformer implements ResourceTransformer<AccountPlanResource, AccountPlan> {
    public static AccountPlanResourceTransformer getInstance() {
        if (instance == null) {
            instance = new AccountPlanResourceTransformer();
        }
        return instance;
    }

    @Override
    public AccountPlan resourceToEntity(@NotNull final AccountPlanResource resource) {
        final AccountPlan plan = new AccountPlan();
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

        plan.setIds(resource.getPlanMapping().getIds());
        return plan;
    }

    @Override
    public AccountPlanResource entityToResource(@NotNull final AccountPlan entity) {
        return new AccountPlanResource(entity.getName(), entity.getDescription(), entity.getLastUpdate(),
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
                ),
                new AccountPlanMapping(entity.getIds()), entity.getPolicyXml());
    }

    private static AccountPlanResourceTransformer instance;
}
