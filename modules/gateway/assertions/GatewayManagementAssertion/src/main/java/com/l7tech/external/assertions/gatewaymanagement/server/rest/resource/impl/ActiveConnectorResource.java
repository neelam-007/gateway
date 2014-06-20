package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ActiveConnectorAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ActiveConnectorTransformer;
import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
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
 * The active connector resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ActiveConnectorResource.activeConnectors_URI)
@Singleton
public class ActiveConnectorResource extends RestEntityResource<ActiveConnectorMO, ActiveConnectorAPIResourceFactory, ActiveConnectorTransformer> {

    protected static final String activeConnectors_URI = "activeConnectors";

    @Override
    @SpringBean
    public void setFactory(ActiveConnectorAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(ActiveConnectorTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new active connector
     *
     * @param resource The active connector to create
     * @return a reference to the newly created active connector
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @response.representation.201.qname {http://ns.l7tech.com/2010/04/gateway-management}Item
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response create(ActiveConnectorMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an active connector by a given id.
     *
     * @param id The identity of the active connector to return
     * @return The active connector.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ActiveConnectorMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * This will return a list of active connector references. A sort can be specified to allow the resulting list to be
     * sorted in either ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/services?name=MyService
     * <p/>
     * Returns services with name = "MyService"
     * <p/>
     * /restman/storedpasswords?type=password&name=DevPassword&name=ProdPassword
     * <p/>
     * Returns stored passwords of password type with name either "DevPassword" or "ProdPassword"
     * <p/>
     * If a parameter is not a valid search value a bad request error will be returned.
     *
     * @param sort                  the key to sort the list by.
     * @param order                 the order to sort the list. true for ascending, false for descending. null implies
     *                              ascending
     * @param names                 The name filter
     * @param enabled               the enabled filter
     * @param types                 The type filter
     * @param hardwiredServiceGoids The service id filter
     * @param securityZoneIds       the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<ActiveConnectorMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("type") List<String> types,
            @QueryParam("hardwiredServiceId") List<Goid> hardwiredServiceGoids,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "type", "hardwiredServiceId", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (types != null && !types.isEmpty()) {
            filters.put("type", (List) types);
        }
        if (hardwiredServiceGoids != null && !hardwiredServiceGoids.isEmpty()) {
            filters.put("hardwiredServiceGoid", (List) hardwiredServiceGoids);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Updates an existing active connector
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update
     * @return a reference to the newly updated or created entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @response.representation.200.qname {http://ns.l7tech.com/2010/04/gateway-management}Item
     * @response.representation.200.doc This is returned if an existing entity is updated.
     * @response.representation.201.qname {http://ns.l7tech.com/2010/04/gateway-management}Item
     * @response.representation.201.doc This is returned if a new entity is created.
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response update(ActiveConnectorMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
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
    public Item<ActiveConnectorMO> template() {
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("TemplateActiveConnector");
        activeConnectorMO.setType("SFTP");
        activeConnectorMO.setEnabled(true);
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("ConnectorProperty", "PropertyValue").map());
        return super.createTemplateItem(activeConnectorMO);
    }
}
