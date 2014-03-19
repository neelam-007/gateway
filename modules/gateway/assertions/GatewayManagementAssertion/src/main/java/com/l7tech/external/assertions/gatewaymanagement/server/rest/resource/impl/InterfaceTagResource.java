package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.InterfaceTagAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.NotEmpty;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.InterfaceTagTransformer;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The interface tag resource
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
     * Creates a new entity
     *
     * @param resource The entity to create
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResource(InterfaceTagMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.createResource(resource);
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<InterfaceTagMO> getResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.getResource(id);
    }

    /**
     * This will return a list of entity references. It will return a maximum of {@code count} references, it can return
     * fewer references if there are fewer then {@code count} entities found. Setting an offset will start listing
     * entities from the given offset. A sort can be specified to allow the resulting list to be sorted in either
     * ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/services?name=MyService
     * <p/>
     * Returns services with name = "MyService"
     * <p/>
     * /restman/storedpasswords?type=password&name=DevPassword,ProdPassword
     * <p/>
     * Returns stored passwords of password type with name either "DevPassword" or "ProdPassword"
     * <p/>
     * If a parameter is not a valid search value it will be ignored.
     *
     * @param offset The offset to start the listing from
     * @param count  The offset ot start the listing from
     * @param sort   the key to sort the list by.
     * @param order  the order to sort the list. true for ascending, false for descending. null implies ascending
     * @param names  The name filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<InterfaceTagMO> listResources(
            @QueryParam("offset") @DefaultValue("0") @NotEmpty Integer offset,
            @QueryParam("count") @DefaultValue("100") @NotEmpty Integer count,
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names) {
        ParameterValidationUtils.validateOffsetCount(offset, count);
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("name"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        return super.listResources(offset, count, sort, ascendingSort,
                filters.map());
    }

    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response updateResource(InterfaceTagMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.updateResource(id, resource);
        return Response.ok().entity(new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build()).build();
    }

    /**
     * Deletes an existing active connector.
     *
     * @param id The id of the active connector to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void deleteResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.deleteResource(id);
    }

    /**
     * This will return a template, example entity that can be used as a base to creating a new entity.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Item<InterfaceTagMO> getResourceTemplate() {
        return super.getResourceTemplate();
    }
}
