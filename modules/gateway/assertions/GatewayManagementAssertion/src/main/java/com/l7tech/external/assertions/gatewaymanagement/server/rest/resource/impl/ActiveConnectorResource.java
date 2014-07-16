package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ActiveConnectorAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ActiveConnectorTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * Active connectors are connectors that poll for messages. For example MQ Native Queues and SFTP Polling Listeners are
 * examples of Active Connectors.
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
     * Creates a new active connector.
     *
     * @param resource The active connector to create
     * @return A reference to the newly created active connector
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ActiveConnectorMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an active connector with the given ID.
     *
     * @param id The ID of the active connector to return
     * @return The active connector.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ActiveConnectorMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of active connectors. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent">/restman/1.0/activeConnectors?name=MySFTPPollingListener</div>
     * <p>Returns active connector with name "MySFTPPollingListener".</p>
     * <div class="code indent">/restman/1.0/activeConnectors?type=SFTP&name=MySFTPPollingListener&name=MyOtherSFTPPollingListener</div>
     * <p>Returns active connector of SFTP type with name either "MySFTPPollingListener" or
     * "MyOtherSFTPPollingListener"</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort                  Key to sort the list by
     * @param order                 Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                              ascending if not specified
     * @param names                 Name filter
     * @param enabled               Enabled filter
     * @param types                 Type filter
     * @param hardwiredServiceGoids Service ID filter
     * @param securityZoneIds       Security zone ID filter
     * @return A list of active connectors. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
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
     * Creates or Updates an existing active connector. If an active connector with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Active connector to create or update
     * @param id       ID of the active connector to create or update
     * @return A reference to the newly created or updated active connector.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(ActiveConnectorMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing active connector.
     *
     * @param id ID of the active connector to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example active connector that can be used as a reference for what active
     * connector objects should look like.
     *
     * @return The template active connector.
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

    /**
     * Adds the hardwired service to the list of links if it is set.
     *
     * @param activeConnectorMO The active connect get the related links for
     * @return The relate links for the given active connector
     */
    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final ActiveConnectorMO activeConnectorMO) {
        List<Link> links = super.getRelatedLinks(activeConnectorMO);
        if (activeConnectorMO != null && activeConnectorMO.getHardwiredId() != null) {
            links.add(ManagedObjectFactory.createLink("hardwiredServiceId", getUrlString(PublishedServiceResource.class, activeConnectorMO.getHardwiredId())));
        }
        return links;
    }
}
