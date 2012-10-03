package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.manager.ApiPlanManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * REST-like resource handler for ApiPlanResource.
 */
public class ApiPlanResourceHandler extends AbstractResourceHandler<ApiPlanResource, ApiPlan> {
    public static ApiPlanResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiPlanResourceHandler(context);
        }
        return instance;
    }

    @Override
    public List<ApiPlanResource> get(@Nullable final Map<String, String> filters) throws FindException {
        return doGet(filters);
    }

    @Override
    public ApiPlanResource get(@NotNull final String id) throws FindException {
        throw new UnsupportedOperationException("Get single resource not supported for ApiPlanResource");
    }

    @Override
    public void delete(@NotNull final String id) throws DeleteException, FindException {
        doDelete(id);
    }

    @Override
    public List<ApiPlanResource> put(@NotNull final List<ApiPlanResource> resources, final boolean removeOmitted) throws ObjectModelException {
        final Date now = new Date();
        for (final ApiPlanResource resource : resources) {
            // overwrite any last update with today's date
            resource.setLastUpdate(now);
        }
        return doPut(resources, removeOmitted);
    }

    @Override
    public ApiPlanResource put(@NotNull final ApiPlanResource resource) {
        throw new UnsupportedOperationException("Put single resource not supported for ApiPlanResource.");
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        return entity instanceof ApiPlan;
    }

    ApiPlanResourceHandler(@NotNull final ApplicationContext context) {
        super(ApiPlanManager.getInstance(context), ApiPlanResourceTransformer.getInstance());
    }

    ApiPlanResourceHandler(@NotNull final PortalGenericEntityManager<ApiPlan> manager,
                           @NotNull final ResourceTransformer<ApiPlanResource, ApiPlan> transformer) {
        super(manager, transformer);
    }

    private static ApiPlanResourceHandler instance;
}
