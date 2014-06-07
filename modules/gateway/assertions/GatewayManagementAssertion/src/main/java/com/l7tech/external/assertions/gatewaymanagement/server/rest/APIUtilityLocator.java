package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The APIUtilityLocator is used to find api utilities. These include APIResourceFactory and APITransformer.
 * It is a centralized point that will be able to retrieve the utilities.
 */
public interface APIUtilityLocator {
    /**
     * Returns an APIResourceFactory for the given resource type.
     *
     * @param resourceType The resource type to return the factory for.
     * @return The APIResourceFactory for the given resource type, or null if there is no APIResourceFactory for that type.
     */
    @Nullable
    APIResourceFactory findFactoryByResourceType(@NotNull String resourceType);

    /**
     * Returns an EntityAPITransformer for the given resource type.
     *
     * @param resourceType The resource type to return the transformer for.
     * @return The EntityAPITransformer for the given resource type, or null if there is no EntityAPITransformer for that type.
     */
    @Nullable
    EntityAPITransformer findTransformerByResourceType(@NotNull String resourceType);
}
