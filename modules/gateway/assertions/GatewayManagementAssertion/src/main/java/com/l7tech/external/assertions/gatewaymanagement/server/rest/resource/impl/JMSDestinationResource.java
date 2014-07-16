package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JMSDestinationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.JMSDestinationTransformer;
import com.l7tech.gateway.api.*;
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
 * A JMS destination is used to configure a connection to a JMS service. This is used for both inbound and outbound
 * configurations.
 *
 * @title JMS Destination
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + JMSDestinationResource.jmsDestination_URI)
@Singleton
public class JMSDestinationResource extends RestEntityResource<JMSDestinationMO, JMSDestinationAPIResourceFactory, JMSDestinationTransformer> {

    protected static final String jmsDestination_URI = "jmsDestinations";

    @Override
    @SpringBean
    public void setFactory(JMSDestinationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(JMSDestinationTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new JMS destination
     *
     * @param resource The JMS destination to create
     * @return A reference to the newly created JMS destination
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(JMSDestinationMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an JMS destination with the given ID.
     *
     * @param id The ID of the JMS destination to return
     * @return The JMS destination.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<JMSDestinationMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of JMS destinations. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/jmsDestinations?name=MyJMSDestination</pre></div>
     * <p>Returns JMS destination with name "MyJMSDestination".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param enabled         Enabled filter
     * @param inbound         Inbound filter
     * @param template        Template filter
     * @param destinations    Destination filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of JMS destinations. If the list is empty then no JMS destinations were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<JMSDestinationMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("inbound") Boolean inbound,
            @QueryParam("template") Boolean template,
            @QueryParam("destination") List<String> destinations,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "inbound", "template", "destination", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("disabled", (List) Arrays.asList(!enabled));
        }
        if (inbound != null) {
            filters.put("messageSource", (List) Arrays.asList(inbound));
        }
        if (template != null) {
            filters.put("template", (List) Arrays.asList(template));
        }
        if (destinations != null && !destinations.isEmpty()) {
            filters.put("destinationName", (List) destinations);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing JMS destination. If an JMS destination with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource JMS destination to create or update
     * @param id       ID of the JMS destination to create or update
     * @return A reference to the newly created or updated JMS destination.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(JMSDestinationMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing JMS destination.
     *
     * @param id The ID of the JMS destination to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example JMS destination that can be used as a reference for what JMS destination
     * objects should look like.
     *
     * @return The template JMS destination.
     */
    @GET
    @Path("template")
    public Item<JMSDestinationMO> template() {
        JMSDestinationMO jmsDestinationMO = ManagedObjectFactory.createJMSDestination();
        JMSDestinationDetail jmsDetails = ManagedObjectFactory.createJMSDestinationDetails();
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();

        jmsDetails.setName("TemplateJMSDestination");
        jmsDetails.setDestinationName("TemplateDestinationName");
        jmsConnection.setProviderType(JMSConnection.JMSProviderType.TIBCO_EMS);

        jmsDestinationMO.setJmsDestinationDetail(jmsDetails);
        jmsDestinationMO.setJmsConnection(jmsConnection);
        return super.createTemplateItem(jmsDestinationMO);
    }
}
