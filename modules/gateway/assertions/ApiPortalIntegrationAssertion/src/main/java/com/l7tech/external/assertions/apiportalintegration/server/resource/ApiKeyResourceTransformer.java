package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

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
        entity.setAccountPlanMappingName(resource.getAccountPlanMappingName());
        entity.setCustomMetaData(resource.getCustomMetaData());
        entity.setApplicationId(resource.getApplicationId());
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
        resource.setAccountPlanMappingName(entity.getAccountPlanMappingName());
        resource.setCustomMetaData(entity.getCustomMetaData());
        resource.setApplicationId(entity.getApplicationId());
        if (entity.getOauthCallbackUrl() == null && entity.getOauthScope() == null && entity.getOauthType() == null) {
            resource.setSecurity(new SecurityDetails());
        } else {
            resource.setSecurity(new SecurityDetails(new OAuthDetails(entity.getOauthCallbackUrl(), entity.getOauthScope(), entity.getOauthType())));
        }
        return resource;
    }

    public ApiKey resourceToEntity(@NotNull final ApplicationEntity resource) {
        final ApiKey entity = new ApiKey();
        entity.setName(resource.getKey());
        entity.setLabel(resource.getLabel());
        entity.setPlatform(StringUtils.EMPTY);
        entity.setSecret(resource.getSecret());
        entity.setStatus(resource.getStatus());
        List<ApplicationApi> apis = resource.getApis();
        for (ApplicationApi api : apis) {
            entity.getServiceIds().put(api.getId(), "not-used");
        }
        entity.setLastUpdate(new Date());
        entity.setAccountPlanMappingId(resource.getOrganizationId());
        entity.setAccountPlanMappingName(resource.getOrganizationName());
        entity.setCustomMetaData(StringUtils.EMPTY);
        entity.setApplicationId(resource.getId());
        if (resource.getOauthCallbackUrl() != null) {
            entity.setOauthCallbackUrl(resource.getOauthCallbackUrl());
        }
        if (resource.getOauthType() != null) {
            entity.setOauthType(resource.getOauthType());
        }
        if (resource.getOauthScope() != null) {
            entity.setOauthScope(resource.getOauthScope());
        }
        return entity;
    }

    private static ApiKeyResourceTransformer instance;
}
