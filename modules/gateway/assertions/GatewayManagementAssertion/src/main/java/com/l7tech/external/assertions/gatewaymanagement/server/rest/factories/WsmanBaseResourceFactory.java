package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.TemplateFactory;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.util.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is a wiseman base resource factory. It used the wiseman resource factories in order to perform crud operations
 * on entities.
 *
 * @author Victor Kazakov
 */
public abstract class WsmanBaseResourceFactory<R extends ManagedObject, F extends ResourceFactory<R>> implements RestResourceFactory<R>, TemplateFactory<R> {

    /**
     * The wiseman resource factory
     */
    protected F factory;

    /**
     * Sets the wiseman resource factory. Likely set using injection.
     *
     * @param factory The wiseman resource factory
     */
    public abstract void setFactory(F factory);

    /**
     * Creates a new resource
     *
     * @param resource The new resource to create
     * @return The new resource id.
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @Override
    public String createResource(@NotNull R resource) throws ResourceFactory.InvalidResourceException {
        //validate that the resource is appropriate for create.
        validateCreateResource(null, resource);
        Map<String, String> selectorMap = factory.createResource(resource);
        return selectorMap.get("id");
    }

    /**
     * Creates a new resource with a given id.
     *
     * @param id       The id to create the resource with
     * @param resource The new resource to create.
     */
    @Override
    public void createResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.InvalidResourceException {
        //validate that the resource is appropriate for create.
        validateCreateResource(id, resource);
        factory.createResource(id, resource);
    }

    /**
     * Updates a resource with the given id
     *
     * @param id       The id of the resource to update
     * @param resource The updated resource
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @Override
    public void updateResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        validateUpdateResource(id, resource);
        factory.putResource(buildSelectorMap(id), resource);
    }

    /**
     * Returns the resource with the given id.
     *
     * @param id The id of the resource to retrieve
     * @return The resource with the given id
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @Override
    public R getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {
        return factory.getResource(buildSelectorMap(id));
    }

    /**
     * Lists resources
     *
     * @param offset  The offset to start listing from
     * @param count   The number of resources to list
     * @param filters The collection of filters specifying which entities to include.
     * @return The list of resource id's
     */
    @Override
    public List<String> listResources(@NotNull Integer offset, @NotNull Integer count, @Nullable Map<String, List<String>> filters) {
        final List<Map<String, String>> resources;
        if (filters != null) {
            //TODO: implement filtering
            throw new NotImplementedException("Not yet implemented");
//            resources = factory.getResources(offset, count, filters);
        } else {
            resources = factory.getResources(offset, count);
        }

        //get the list of resource id's
        List<String> resourceList = new ArrayList<>(resources.size());
        for (Map<String, String> selectorMap : resources) {
            resourceList.add(selectorMap.get("id"));
        }

        return resourceList;
    }

    /**
     * Delete a resource with the given id.
     *
     * @param id The id of the resource to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @Override
    public void deleteResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(buildSelectorMap(id));
    }

    /**
     * Validates that a resource can be used for create. Checks to see if the id is set correctly
     *
     * @param id       The id to create the resource with.
     * @param resource The resource to create
     */
    private void validateCreateResource(@Nullable String id, R resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when creating a new entity, or id must equal new entity id");
        }
    }

    /**
     * Validates that a resource can be used for update. Checks to see if the id is set correctly
     *
     * @param id       The id of the resource to update.
     * @param resource The resource to update
     */
    private void validateUpdateResource(String id, R resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when updating a new entity, or id must equal new entity id");
        }
    }

    /**
     * Creates a wiseman selector map with only the id selector
     *
     * @param id The resource id
     * @return A wiseman selector map
     */
    protected static Map<String, String> buildSelectorMap(String id) {
        return CollectionUtils.MapBuilder.<String, String>builder().put("id", id).map();
    }
}
