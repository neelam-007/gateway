package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import org.jetbrains.annotations.NotNull;

/**
 * Handles transformation between ApiKeyResource and ApiKeyData.
 */
public class ApiKeyResourceTransformer implements ResourceTransformer<ApiKeyResource, ApiKey> {
    public static ApiKeyResourceTransformer getInstance() {
        if (instance == null) {
            instance = new ApiKeyResourceTransformer();
        }
        return instance;
    }

    public ApiKey resourceToEntity(@NotNull final ApiKeyResource resource) {
        final ApiKey entity = new ApiKey();
        entity.setName(resource.getKey());
        entity.setLabel(resource.getLabel());
        entity.setPlatform(resource.getPlatform());
        entity.setSecret(resource.getSecret());
        entity.setStatus(resource.getStatus());
        entity.setServiceIds(resource.getApis());
        entity.setLastUpdate(resource.getLastUpdate());
        entity.setAccountPlanMappingId(resource.getAccountPlanMappingId());
        entity.setCustomMetaData(resource.getCustomMetaData());
        if (resource.getSecurity() != null && resource.getSecurity().getOauth() != null) {
            entity.setOauthCallbackUrl(resource.getSecurity().getOauth().getCallbackUrl());
            entity.setOauthScope(resource.getSecurity().getOauth().getScope());
            entity.setOauthType(resource.getSecurity().getOauth().getType());
        }
        return entity;
    }

    @Override
    public ApiKeyResource entityToResource(@NotNull final ApiKey entity) {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey(entity.getName());
        resource.setLabel(entity.getLabel());
        resource.setPlatform(entity.getPlatform());
        resource.setSecret(entity.getSecret());
        resource.setStatus(entity.getStatus());
        resource.setApis(entity.getServiceIds());
        resource.setLastUpdate(entity.getLastUpdate());
        resource.setAccountPlanMappingId(entity.getAccountPlanMappingId());
        resource.setCustomMetaData(entity.getCustomMetaData());
        if (entity.getOauthCallbackUrl() == null && entity.getOauthScope() == null && entity.getOauthType() == null) {
            resource.setSecurity(new SecurityDetails());
        } else {
            resource.setSecurity(new SecurityDetails(new OAuthDetails(entity.getOauthCallbackUrl(), entity.getOauthScope(), entity.getOauthType())));
        }
        return resource;
    }

    private static ApiKeyResourceTransformer instance;
}
