package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurityZoneAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SecurityZoneTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * Security zones are used to partition the Gateway into portions that can then be managed by other users. A security
 * zone is a collection of related entities (for example: services, policies, folders, trusted certificates).
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SecurityZoneResource.securityZone_URI)
@Singleton
public class SecurityZoneResource extends RestEntityResource<SecurityZoneMO, SecurityZoneAPIResourceFactory, SecurityZoneTransformer> {

    protected static final String securityZone_URI = "securityZones";

    @Override
    @SpringBean
    public void setFactory(SecurityZoneAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(SecurityZoneTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new security zone
     *
     * @param resource The security zone to create
     * @return A reference to the newly created security zone
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(SecurityZoneMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a security zone with the given ID.
     *
     * @param id The ID of the security zone to return
     * @return The security zone
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<SecurityZoneMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of security zones. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/activeConnectors?name=MySecurityZone</pre></div>
     * <p>Returns security zone with name "MySecurityZone".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort  Key to sort the list by
     * @param order Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *              ascending if not specified
     * @param names Name filter
     * @return A list of security zones. If the list is empty then no security zones were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<SecurityZoneMO> list(
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
     * Creates or Updates an existing security zone. If an security zone with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Security zone to create or update
     * @param id       ID of the security zone to create or update
     * @return A reference to the newly created or updated security zone.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(SecurityZoneMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing security zone.
     *
     * @param id The ID of the security zone to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example security zone that can be used as a reference for what security zone
     * objects should look like.
     *
     * @return The template security zone
     */
    @GET
    @Path("template")
    public Item<SecurityZoneMO> template() {
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("Template Name");
        return super.createTemplateItem(securityZoneMO);
    }
}
