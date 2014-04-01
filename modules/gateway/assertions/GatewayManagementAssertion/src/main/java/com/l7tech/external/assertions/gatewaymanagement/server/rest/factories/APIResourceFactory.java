package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.Mapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * This is the resource factory interface for a factory that manages an entity. If defines the methods for all CRUD
 * operations on the rest entity.
 *
 * @author Victor Kazakov
 */
public interface APIResourceFactory<R> {

    /**
     * This will create a new resource.
     *
     * @param resource The new resource to create
     * @return The id of the newly created resource
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     *
     */
    public String createResource(@NotNull R resource) throws ResourceFactory.InvalidResourceException;

    /**
     * This will create a new resource with the given id.
     *
     * @param id       The id to create the resource with
     * @param resource The new resource to create.
     */
    public void createResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.InvalidResourceException;

    /**
     * This will update a resource with this given id.
     *
     * @param id       The id of the resource to update
     * @param resource The updated resource
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     *
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     *
     */
    public void updateResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException;

    /**
     * This will retrieve a resource with the given id.
     *
     * @param id The id of the resource to retrieve
     * @return The resource
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     *
     */
    public R getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException;

    /**
     * This will return true if the resource with the given id exists. returns false otherwise.
     *
     * @param id The id of the resource to find
     * @return true it the resource exists.
     *
     */
    public boolean resourceExists(@NotNull String id);

    /**
     * Returns a list of resources. It can optionally be sorted by the given sort key in either ascending or descending
     * order. The filters given are used to restrict the returned resources to only those entities that match the
     * filters.
     *
     * @param sortKey   The attribute to sort the entities by. Null for no sorting
     * @param ascending The order to sort the entities
     * @param filters   The collection of filters specifying which entities to include.
     * @return The list of resources matching the given parameters
     */
    List<R> listResources(@Nullable String sortKey, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> filters);

    /**
     * This will delete a resource with the given id.
     *
     * @param id The id of the resource to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     *
     */
    public void deleteResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException;

    /**
     * This will return the default mapping for the entity.
     *
     * @param resource        The resource to create the mapping for
     * @param defaultAction   The default action given
     * @param defaultMapBy    The default map by given
     * @return The mapping for the resource
     */
    public Mapping buildMapping(@NotNull R resource, @Nullable Mapping.Action defaultAction, @Nullable String defaultMapBy);

    /**
     * Returns the type of the resource
     *
     * @return The resource type
     */
    @NotNull
    public abstract String getResourceType();
}
