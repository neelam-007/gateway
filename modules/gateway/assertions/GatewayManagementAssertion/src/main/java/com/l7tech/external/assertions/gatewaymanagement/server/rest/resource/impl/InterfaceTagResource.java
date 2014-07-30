package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.InterfaceTagAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.InterfaceTagTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * Interfaces are used to specify IP addresses that can be monitored by a listen port. Defining an interface gives you
 * greater control over the IP addresses that will be monitored.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + InterfaceTagResource.interfaceTags_URI)
@Singleton
public class InterfaceTagResource extends RestEntityResource<InterfaceTagMO, InterfaceTagAPIResourceFactory, InterfaceTagTransformer> {

    protected static final String interfaceTags_URI = "interfaceTags";

    @Override
    @SpringBean
    public void setFactory(InterfaceTagAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(InterfaceTagTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new interface. In order to create a new interface tag you must have read and write access to cluster
     * properties.
     *
     * @param resource The interface to create
     * @return A reference to the newly created interface
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(InterfaceTagMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Retrieves an interface tag given it's ID. In order to get an interface tag you must have read access to cluster
     * properties.
     *
     * @param id The identity of the interface tag to select
     * @return The interface tag.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<InterfaceTagMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * Returns a list of interface tags. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/interfaceTags?name=MyInterface</pre></div>
     * <p>Returns interface with name = "MyInterface"</p>
     * <div class="code indent"><pre>/restman/interfaceTags?name=MyInterface&name=MyInterfaceProd</pre></div>
     * <p>Returns interfaces with name either "MyInterface" or "MyInterfaceProd"</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort  Key to sort the list by
     * @param order Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *              ascending if not specified
     * @param names Name filter. This will return interfaces with the specified names.
     * @return A list of interfaces. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<InterfaceTagMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Updates an existing interface tag. You cannot change the name of an interface, you can only update its address
     * patterns. In order to update an interface tag you must have read and write access to cluster properties.
     *
     * @param resource The updated interface tag
     * @param id       The ID of the interface tag to update
     * @return a reference to the newly updated interface tag.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(InterfaceTagMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        boolean resourceExists = factory.resourceExists(id);
        if (resourceExists) {
            factory.updateResource(id, resource);
            return Response.ok().entity(new ItemBuilder<>(transformer.convertToItem(resource))
                    .setContent(null)
                    .addLink(getLink(resource))
                    .addLinks(getRelatedLinks(resource))
                    .build()).build();
        } else {
            throw new ResourceFactory.ResourceAccessException("Cannot create an Interface Tag with a specific Id");
        }
    }

    /**
     * Deletes an existing interface tag.
     *
     * @param id The ID of the interface tag to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example interface tag that can be used as a reference for what interface tag
     * objects should look like.
     *
     * @return The template interface tag.
     */
    @GET
    @Path("template")
    public Item<InterfaceTagMO> template() {
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setName("TemplateInterfaceTag");
        interfaceTagMO.setAddressPatterns(CollectionUtils.list("0.0.0.0/00"));
        return super.createTemplateItem(interfaceTagMO);
    }
}
