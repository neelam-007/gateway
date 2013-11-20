package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
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
public interface RestResourceFactory<R> {

    /**
     * This will create a new resource.
     *
     * @param resource The new resource to create
     * @return The id of the newly created resource
     * @throws ResourceFactory.InvalidResourceException
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
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    public void updateResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException;

    /**
     * This will retrieve a resource with the given id.
     *
     * @param id The id of the resource to retrieve
     * @return The resource
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    public R getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException;

    /**
     * This will list resources given an offset and count. The optional filter is used to specify which resources to use
     * in the list
     *
     * @param offset  The offset to start listing from
     * @param count   The number of resources to list
     * @param filters The collection of filters specifying which entities to include.
     * @return The list of entity references.
     */
    public List<String> listResources(@NotNull Integer offset, @NotNull Integer count, @Nullable Map<String, List<String>> filters);

    /**
     * This will delete a resource with the given id.
     *
     * @param id The id of the resource to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    public void deleteResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException;
}
