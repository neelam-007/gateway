package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * REST-like interface for portal resources.
 *
 * @param <T> the type of portal resource.
 */
public interface ResourceHandler<T extends Resource> {
    /**
     * Retrieve a list of resources.
     *
     * @param filters a map of filters which can restrict the resources that are retrieved.
     * @return a list of resources filtered by the given filters.
     * @throws FindException if an error occurs trying to retrieve the resources.
     */
    List<T> get(@Nullable final Map<String, String> filters) throws FindException;

    T get(@NotNull final String id) throws FindException;

    /**
     * Delete a resource.
     *
     * @param id the id of the resource to delete.
     * @throws DeleteException if an error occurs trying to delete the resource.
     * @throws FindException   if an error occurs trying to find the resource to delete.
     */
    void delete(@NotNull final String id) throws DeleteException, FindException;

    /**
     * Creates or updates a list of resources. If a resource does not yet exist, it will be created.
     * Otherwise it will be udpated.
     *
     * @param resources     the list of resources to create or update.
     * @param removeOmitted true if any existing resources that are not in the given list of resources to create or update should be deleted.
     * @return the list of resources that were created or updated.
     * @throws ObjectModelException if an error occurs trying to create/update/delete a resource.
     */
    List<T> put(@NotNull final List<T> resources, final boolean removeOmitted) throws ObjectModelException;

    T put(@NotNull final T resource) throws FindException, UpdateException, SaveException;
}
