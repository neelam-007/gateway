package com.l7tech.external.assertions.apiportalintegration.server.resource;


import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedEncassManagerImpl;
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
 * REST-like resource handler for ApiFragmentResource.
 */
public class ApiFragmentResourceHandler extends AbstractResourceHandler<ApiFragmentResource, PortalManagedService> {
    public static final String GUID = "guid";

    public static ApiFragmentResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiFragmentResourceHandler(context);
        }
        return instance;
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        boolean valid = true;
        if (!(entity instanceof PortalManagedEncass)) {
            valid = false;
        } else if (filters != null && filters.containsKey(GUID) && (filters.get(GUID) != null)) {
            final PortalManagedEncass service = (PortalManagedEncass) entity;
            if (!filters.get(GUID).equals(service.getEncassGuid())) {
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public List<ApiFragmentResource> get(@Nullable final Map<String, String> filters) throws FindException {
        return doGet(filters);
    }

    @Override
    public ApiFragmentResource get(@NotNull final String id) throws FindException {
        throw new UnsupportedOperationException("Get single resource not supported for ApiFragmentResource");
    }

    @Override
    public void delete(@NotNull final String id) throws DeleteException, FindException {
        throw new UnsupportedOperationException("Delete not supported for ApiFragmentResource.");
    }

    @Override
    public List<ApiFragmentResource> put(@NotNull final List<ApiFragmentResource> resources, final boolean removeOmitted) throws ObjectModelException {
        throw new UnsupportedOperationException("Put list of resources not supported for ApiFragmentResource.");
    }

    @Override
    public ApiFragmentResource put(@NotNull final ApiFragmentResource resource) {
        throw new UnsupportedOperationException("Put single resource not supported for ApiFragmentResource");
    }

    ApiFragmentResourceHandler(@NotNull final ApplicationContext context) {
        super(PortalManagedEncassManagerImpl.getInstance(context), ApiFragmentResourceTransformer.getInstance());
    }

    ApiFragmentResourceHandler(@NotNull final PortalGenericEntityManager<PortalManagedEncass> manager,
                               @NotNull final ResourceTransformer<ApiFragmentResource, PortalManagedEncass> transformer) {
        super(manager, transformer);
    }

    private static ApiFragmentResourceHandler instance;
}
