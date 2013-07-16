package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import org.jetbrains.annotations.NotNull;

/**
 * Handles transformation between ApiKeyDataResource and ApiKeyData.
 */
public class ApiKeyDataResourceTransformer implements ResourceTransformer<ApiKeyResource, ApiKeyData> {
    public static ApiKeyDataResourceTransformer getInstance() {
        if (instance == null) {
            instance = new ApiKeyDataResourceTransformer();
        }
        return instance;
    }

    public ApiKeyData resourceToEntity(@NotNull final ApiKeyResource resource) {
        final ApiKeyData entity = new ApiKeyData();
        entity.setName(resource.getKey());
        entity.setLabel(resource.getLabel());
        entity.setPlatform(resource.getPlatform());
        entity.setSecret(resource.getSecret());
        entity.setStatus(resource.getStatus());
        entity.setServiceIds(resource.getApis());
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
    public ApiKeyResource entityToResource(@NotNull final ApiKeyData entity) {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey(entity.getName());
        resource.setLabel(entity.getLabel());
        resource.setPlatform(entity.getPlatform());
        resource.setSecret(entity.getSecret());
        resource.setStatus(entity.getStatus());
        resource.setApis(entity.getServiceIds());
        resource.setAccountPlanMappingId(entity.getAccountPlanMappingId());
        resource.setCustomMetaData(entity.getCustomMetaData());
        if (entity.getOauthCallbackUrl() == null && entity.getOauthScope() == null && entity.getOauthType() == null) {
            resource.setSecurity(new SecurityDetails());
        } else {
            resource.setSecurity(new SecurityDetails(new OAuthDetails(entity.getOauthCallbackUrl(), entity.getOauthScope(), entity.getOauthType())));
        }
        return resource;
    }

    private static ApiKeyDataResourceTransformer instance;
}
