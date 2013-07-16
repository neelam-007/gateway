package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerFactory;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * REST-like handler for ApiKeyDataResource.
 */
public class ApiKeyDataResourceHandler extends AbstractResourceHandler<ApiKeyResource, ApiKeyData> {
    public static final String APIKEY_STATUS = "apiKeyStatus";

    public static ApiKeyDataResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new ApiKeyDataResourceHandler(context);
        }
        return instance;
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        boolean valid = true;
        if (!(entity instanceof ApiKeyData)) {
            valid = false;
        } else if (filters != null && filters.containsKey(APIKEY_STATUS) && (filters.get(APIKEY_STATUS) != null)) {
            final ApiKeyData apikey = (ApiKeyData) entity;
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
        throw new UnsupportedOperationException("Put list of resources not supported for ApiKeyResource");
    }

    ApiKeyDataResourceHandler(@NotNull final ApplicationContext context) {
        super(ApiKeyManagerFactory.getInstance(), ApiKeyDataResourceTransformer.getInstance());
    }

    ApiKeyDataResourceHandler(@NotNull final PortalGenericEntityManager manager, @NotNull final ResourceTransformer transformer) {
        super(manager, transformer);
    }

    private static ApiKeyDataResourceHandler instance;
}
