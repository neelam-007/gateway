package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import org.jetbrains.annotations.NotNull;

/**
 * Handles transformation between ApiResource and PortalManagedService.
 */
public class ApiResourceTransformer implements ResourceTransformer<ApiResource, PortalManagedService> {
    public static ApiResourceTransformer getInstance() {
        if (instance == null) {
            instance = new ApiResourceTransformer();
        }
        return instance;
    }

    @Override
    public PortalManagedService resourceToEntity(final @NotNull ApiResource resource) {
        final PortalManagedService entity = new PortalManagedService();
        entity.setName(resource.getApiId());
        entity.setDescription(resource.getServiceOid());
        entity.setApiGroup(resource.getApiGroup());
        return entity;
    }

    @Override
    public ApiResource entityToResource(final @NotNull PortalManagedService entity) {
        return new ApiResource(entity.getName(), entity.getApiGroup(), entity.getDescription());
    }

    private static ApiResourceTransformer instance;
}
