package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManager;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * REST-like handler for ApiKeyResource.
 */
public class ApiKeyResourceHandler extends AbstractResourceHandler<ApiKeyResource, ApiKey> {
    public static final String APIKEY_STATUS = "apiKeyStatus";

    public static ApiKeyResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiKeyResourceHandler(context);
        }
        return instance;
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        boolean valid = true;
        if (!(entity instanceof ApiKey)) {
            valid = false;
        } else if (filters != null && filters.containsKey(APIKEY_STATUS) && (filters.get(APIKEY_STATUS) != null)) {
            final ApiKey apikey = (ApiKey) entity;
            if (!filters.get(APIKEY_STATUS).equalsIgnoreCase(apikey.getStatus())) {
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public ApiKeyResource get(@NotNull final String id) throws FindException {
        return doGet(id);
    }

    @Override
    public ApiKeyResource put(@NotNull final ApiKeyResource resource) throws FindException, UpdateException, SaveException {
        resource.setLastUpdate(new Date());
        return doPut(resource);
    }

    @Override
    public List<ApiKeyResource> get(@Nullable final Map<String, String> filters) throws FindException {
        return doGet(filters);
    }

    @Override
    public void delete(@NotNull final String id) throws DeleteException, FindException {
        doDelete(id);
    }

    @Override
    public List<ApiKeyResource> put(@NotNull final List<ApiKeyResource> resources, final boolean removeOmitted) throws ObjectModelException {
        final Date now = new Date();
        for (final ApiKeyResource resource : resources) {
            // overwrite any last update with today's date
            resource.setLastUpdate(now);
        }
        return doPut(resources, removeOmitted);
    }

    ApiKeyResourceHandler(@NotNull final ApplicationContext context) {
        super(ApiKeyManager.getInstance(context), ApiKeyResourceTransformer.getInstance());
    }

    ApiKeyResourceHandler(@NotNull final PortalGenericEntityManager manager, @NotNull final ResourceTransformer transformer) {
        super(manager, transformer);
    }

    private static ApiKeyResourceHandler instance;
}
