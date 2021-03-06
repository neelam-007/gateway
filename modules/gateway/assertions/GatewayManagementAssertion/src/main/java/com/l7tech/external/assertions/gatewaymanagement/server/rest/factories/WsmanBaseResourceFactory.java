package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * This is a wiseman base resource factory. It used the wiseman resource factories in order to perform crud operations
 * on entities.
 *
 * @author Victor Kazakov
 */
public abstract class WsmanBaseResourceFactory<R extends ManagedObject, F extends ResourceFactory<R,?>> implements APIResourceFactory<R> {

    /**
     * The wiseman resource factory
     */
    protected F factory;

    /**
     * Creates a new wsman based resource factory. This is a resource factory that used the wiseman resources to perform
     * its work.
     */
    public WsmanBaseResourceFactory() {
    }

    /**
     * Sets the wiseman resource factory. Likely set using injection.
     *
     * @param factory The wiseman resource factory
     */
    public abstract void setFactory(F factory);

    @Override
    public String createResource(@NotNull R resource) throws ResourceFactory.InvalidResourceException {
        //validate that the resource is appropriate for create.
        validateCreateResource(null, resource);
        Map<String, String> selectorMap = factory.createResource(resource);
        resource.setId(selectorMap.get("id"));
        return selectorMap.get("id");
    }

    @Override
    public void createResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.ResourceFactoryException {
        //validate that the resource is appropriate for create.
        validateCreateResource(id, resource);
        factory.createResource(id, resource);
        resource.setId(id);
    }

    @Override
    public void updateResource(@NotNull String id, @NotNull R resource) throws ResourceFactory.ResourceFactoryException {
        validateUpdateResource(id, resource);
        factory.putResource(buildSelectorMap(id), resource);
        resource.setId(id);
    }

    @Override
    public R getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {
        return factory.getResource(buildSelectorMap(id));
    }

    public boolean resourceExists(@NotNull String id) {
        return factory.resourceExists(buildSelectorMap(id));
    }


    @Override
    public List<R> listResources(@Nullable String sort, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> filters) {
        return factory.getResources(sort, ascending, filters);
    }

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
            throw new InvalidArgumentException("id", "Must not specify an ID when creating a new entity, or id must equal new entity id");
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
            throw new InvalidArgumentException("id", "Must not specify an ID when updating a new entity, or id must equal entity id");
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

    @Override
    public Mapping buildMapping(@NotNull R resource, @Nullable Mapping.Action defaultAction, @Nullable String defaultMapBy) {
        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setType(getResourceType());
        mapping.setAction(defaultAction);
        mapping.setSrcId(resource.getId());
        if (!"id".equals(defaultMapBy)) {
            mapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("MapBy", defaultMapBy).map());
        }
        return mapping;
    }
}
