package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ListenPortAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ListenPortTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The listen port resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ListenPortResource.listenPort_URI)
@Singleton
public class ListenPortResource extends RestEntityResource<ListenPortMO, ListenPortAPIResourceFactory, ListenPortTransformer> {

    protected static final String listenPort_URI = "listenPorts";

    @Override
    @SpringBean
    public void setFactory(ListenPortAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(ListenPortTransformer transformer) {
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
    public Response create(ListenPortMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
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
    public Item<ListenPortMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * This will return a list of entity references. A sort can be specified to allow the resulting list to be sorted in
     * either ascending or descending order. Other params given will be used as search values. Examples:
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
     * @param sort            the key to sort the list by.
     * @param order           the order to sort the list. true for ascending, false for descending. null implies
     *                        ascending
     * @param names           The name filter
     * @param enabled         the enabled filter
     * @param protocol        The protocol filter
     * @param ports           The port filter
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<ListenPortMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "enabled", "protocol", "port"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("protocol") List<String> protocol,
            @QueryParam("port") List<Integer> ports,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "protocol", "port", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (protocol != null && !protocol.isEmpty()) {
            filters.put("scheme", (List) protocol);
        }
        if (ports != null && !ports.isEmpty()) {
            filters.put("port", (List) ports);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(convertSort(sort), ascendingSort,
                filters.map());
    }

    private String convertSort(String sort) {
        if (sort == null) return null;
        switch (sort) {
            case "protocol":
                return "scheme";
            default:
                return sort;
        }
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
    public Response update(ListenPortMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing active connector.
     *
     * @param id The id of the active connector to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * This will return a template, example entity that can be used as a reference for what entity objects should look
     * like.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Item<ListenPortMO> template() {
        ListenPortMO emailListenerMO = ManagedObjectFactory.createListenPort();
        emailListenerMO.setName("TemplateListenPort");
        emailListenerMO.setPort(1234);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ConnectorProperty", "PropertyValue").map());
        return super.createTemplateItem(emailListenerMO);
    }
}
