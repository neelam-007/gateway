package com.l7tech.external.assertions.apiportalintegration.server.resource;


import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManagerImpl;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * REST-like resource handler for ApiResource.
 */
public class ApiResourceHandler extends AbstractResourceHandler<ApiResource, PortalManagedService> {
    public static final String API_GROUP = "apiGroup";

    public static ApiResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiResourceHandler(context);
        }
        return instance;
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        boolean valid = true;
        if (!(entity instanceof PortalManagedService)) {
            valid = false;
        } else if (filters != null && filters.containsKey(API_GROUP) && (filters.get(API_GROUP) != null)) {
            final PortalManagedService service = (PortalManagedService) entity;
            if (!filters.get(API_GROUP).equals(service.getApiGroup())) {
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public List<ApiResource> get(@Nullable final Map<String, String> filters) throws FindException {
        return doGet(filters);
    }

    @Override
    public ApiResource get(@NotNull final String id) throws FindException {
        throw new UnsupportedOperationException("Get single resource not supported for ApiResource");
    }

    @Override
    public void delete(@NotNull final String id) throws DeleteException, FindException {
        throw new UnsupportedOperationException("Delete not supported for ApiResource.");
    }

    @Override
    public List<ApiResource> put(@NotNull final List<ApiResource> resources, final boolean removeOmitted) throws ObjectModelException {
        throw new UnsupportedOperationException("Put list of resources not supported for ApiResource.");
    }

    @Override
    public ApiResource put(@NotNull final ApiResource resource) {
        throw new UnsupportedOperationException("Put single resource not supported for ApiResource");
    }

    ApiResourceHandler(@NotNull final ApplicationContext context) {
        super(PortalManagedServiceManagerImpl.getInstance(context), ApiResourceTransformer.getInstance());
    }

    ApiResourceHandler(@NotNull final PortalGenericEntityManager<PortalManagedService> manager,
                       @NotNull final ResourceTransformer<ApiResource, PortalManagedService> transformer) {
        super(manager, transformer);
    }

    private static ApiResourceHandler instance;
}
