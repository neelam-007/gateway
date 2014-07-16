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
 * A listen port is a TCP port that "listens" for incoming messages that are then passed to the Gateway message
 * processor.
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
     * Creates a new listen port
     *
     * @param resource The listen port to create
     * @return A reference to the newly created listen port
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ListenPortMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a listen port with the given ID.
     *
     * @param id The ID of the listen port to return
     * @return The listen port.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ListenPortMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of listen ports. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent">/restman/1.0/listenPorts?name=MyListenPort</div>
     * <p>Returns listen port with name "MyListenPort".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param enabled         Enabled filter
     * @param protocol        Protocol filter
     * @param ports           Port filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of listen ports. If the list is empty then no listen ports were found.
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
     * Creates or Updates an existing listen port. If a listen port with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Listen port to create or update
     * @param id       ID of the listen port to create or update
     * @return A reference to the newly created or updated listen port.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(ListenPortMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing listen port.
     *
     * @param id The ID of the listen port to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example listen port that can be used as a reference for what listen port objects
     * should look like.
     *
     * @return The template listen port.
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
