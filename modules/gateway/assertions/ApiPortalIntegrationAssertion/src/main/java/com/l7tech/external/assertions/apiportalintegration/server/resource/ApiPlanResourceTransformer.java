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
        return plan;
    }

    @Override
    public ApiPlanResource entityToResource(@NotNull final ApiPlan entity) {
        return new ApiPlanResource(entity.getName(), entity.getDescription(), entity.getLastUpdate(), entity.getPolicyXml(), entity.isDefaultPlan());
    }

    private static ApiPlanResourceTransformer instance;
}
