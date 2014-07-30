package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GenericEntityAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GenericEntityTransformer;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * A generic entities is used by some modular assertions to represent any entity.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + GenericEntityResource.genericEntity_URI)
@Singleton
public class GenericEntityResource extends RestEntityResource<GenericEntityMO, GenericEntityAPIResourceFactory, GenericEntityTransformer> {

    protected static final String genericEntity_URI = "genericEntities";

    @Override
    @SpringBean
    public void setFactory(GenericEntityAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(GenericEntityTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new generic entity.
     *
     * @param resource The generic entity to create
     * @return A reference to the newly created generic entity
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(GenericEntityMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Retrieves a generic entity given its ID
     *
     * @param id The ID of the generic entity to retrieve
     * @return The generic entity.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<GenericEntityMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of generic entities. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/activeConnectors?name=MyGenericEntity</pre></div>
     * <p>Returns generic entity with name "MyGenericEntity".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort             Key to sort the list by
     * @param order            Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                         ascending if not specified
     * @param names            Name filter
     * @param enabled          Enabled filter
     * @param entityClassNames Entity class name filter
     * @return A list of generic entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<GenericEntityMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "entityClassName"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("entityClassName") List<String> entityClassNames) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "entityClassName"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (entityClassNames != null && !entityClassNames.isEmpty()) {
            filters.put("entityClassName", (List) entityClassNames);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing generic entity. If a generic entity with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Generic entity to create or update
     * @param id       ID of the generic entity to create or update
     * @return A reference to the newly created or updated generic entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(GenericEntityMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing generic entity.
     *
     * @param id The ID of the generic entity to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example generic entity that can be used as a reference for what generic entity
     * objects should look like.
     *
     * @return The template generic entity.
     */
    @GET
    @Path("template")
    public Item<GenericEntityMO> template() {
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("TemplateGenericEntity");
        genericEntityMO.setEntityClassName("com.foo");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setValueXml("value_xml");
        return super.createTemplateItem(genericEntityMO);
    }
}
