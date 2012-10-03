package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManager;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * REST-like handler for ApiKeyResource.
 */
public class ApiKeyResourceHandler extends AbstractResourceHandler<ApiKeyResource, ApiKey> {
    public static ApiKeyResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiKeyResourceHandler(context);
        }
        return instance;
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        throw new UnsupportedOperationException("doFilter not supported for ApiKeyResource");
    }

    @Override
    public ApiKeyResource get(@NotNull final String id) throws FindException {
        return doGet(id);
    }

    @Override
    public ApiKeyResource put(@NotNull final ApiKeyResource resource) throws FindException, UpdateException, SaveException {
        return doPut(resource);
    }

    @Override
    public List<ApiKeyResource> get(@Nullable final Map<String, String> filters) throws FindException {
        throw new UnsupportedOperationException("Get not supported for ApiKeyResource");
    }

    @Override
    public void delete(@NotNull final String id) throws DeleteException, FindException {
        doDelete(id);
    }

    @Override
    public List<ApiKeyResource> put(@NotNull final List<ApiKeyResource> resources, final boolean removeOmitted) throws ObjectModelException {
        throw new UnsupportedOperationException("Put list of resources not supported for ApiKeyResource");
    }

    ApiKeyResourceHandler(@NotNull final ApplicationContext context) {
        super(ApiKeyManager.getInstance(context), ApiKeyResourceTransformer.getInstance());
    }

    ApiKeyResourceHandler(@NotNull final PortalGenericEntityManager manager, @NotNull final ResourceTransformer transformer) {
        super(manager, transformer);
    }

    private static ApiKeyResourceHandler instance;
}
