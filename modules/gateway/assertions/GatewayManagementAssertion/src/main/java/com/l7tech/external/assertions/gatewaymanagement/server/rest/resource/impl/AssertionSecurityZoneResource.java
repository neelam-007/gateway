package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.AssertionSecurityZoneAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.AssertionSecurityZoneTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * Assertion Security Zones entities are used to apply security zones to assertions. By default, assertions do not have
 * a security zone applied to them.
 */
@Provider
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + AssertionSecurityZoneResource.activeConnectors_URI)
public class AssertionSecurityZoneResource implements URLAccessible<AssertionSecurityZoneMO> {
    protected static final String activeConnectors_URI = "assertionSecurityZones";
    private AssertionSecurityZoneAPIResourceFactory factory;
    private AssertionSecurityZoneTransformer transformer;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(AssertionSecurityZoneAPIResourceFactory factory) {
        this.factory = factory;
    }

    @SpringBean
    public void setTransformer(AssertionSecurityZoneTransformer transformer) {
        this.transformer = transformer;
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.ASSERTION_ACCESS.toString();
    }

    /**
     * <p>Returns a list of assertion security zones. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/assertionSecurityZones?name=com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion</pre></div>
     * <p>Returns assertion security zone of the Jdbc Query Assertion. The name of the assertion security zone is the
     * fully qualified name of the assertion.</p>
     * <div class="code indent"><pre>/restman/1.0/assertionSecurityZones?securityZone.id=0e028eafc5c66c3af755a2e470734948</pre></div>
     * <p>Returns assertion security zones that have security zone ID "0e028eafc5c66c3af755a2e470734948"</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param securityZoneIds Security zone ID filter. To list all assertions with no security zones applied use the
     *                        default ID: 0000000000000000ffffffffffffffff
     * @return List of assertion security zones. If the list is empty then no assertion security zones were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<AssertionSecurityZoneMO> list(
            @QueryParam("sort") @ChoiceParam({"name", "securityZone.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return RestEntityResourceUtils.createItemsList(factory.listResources(sort, ascendingSort, filters.map()), transformer, this, uriInfo.getRequestUri().toString());
    }

    /**
     * Returns the assertion security zone for the assertion with the given fully qualified name.
     *
     * @param name Fully qualified name of the assertion
     * @return Assertion security zone for the assertion
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{name}")
    public Item<AssertionSecurityZoneMO> get(@PathParam("name") String name) throws ResourceFactory.ResourceNotFoundException {
        AssertionSecurityZoneMO resource = factory.getResourceByName(name);
        return RestEntityResourceUtils.createGetResponseItem(resource, transformer, this);
    }


    /**
     * Returns a template, which is an example  assertion security zone that can be used as a reference for what
     * assertion security zone objects should look like.
     *
     * @return The template assertion security zone.
     */
    @GET
    @Path("template")
    public Item<AssertionSecurityZoneMO> template() {
        AssertionSecurityZoneMO assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName("TemplateAssertionSecurityZone");
        assertionSecurityZoneMO.setSecurityZoneId("SecurityZoneID");

        return RestEntityResourceUtils.createTemplateItem(assertionSecurityZoneMO, this, uriInfo.getRequestUri().toString());
    }

    /**
     * Updates an assertion security zone
     *
     * @param resource Updated assertion security zone for an assertion
     * @param name     Fully qualified name of the assertion
     * @return A reference to the newly updated assertion security zone.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{name}")
    public Response update(AssertionSecurityZoneMO resource, @PathParam("name") String name) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        AssertionSecurityZoneMO updatedResource = factory.updateResourceByName(name, resource);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(updatedResource, transformer, this, false);
    }

    @NotNull
    @Override
    public Link getLink(@NotNull AssertionSecurityZoneMO resource) {
        return ManagedObjectFactory.createLink(Link.LINK_REL_SELF, getUrl(resource));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable AssertionSecurityZoneMO resource) {
        final ArrayList<Link> links = new ArrayList<>();
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_TEMPLATE, getUrlString("template")));
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_LIST, getUrlString(null)));
        return links;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull AssertionSecurityZoneMO resource) {
        return getUrlString(resource.getName());
    }

    @NotNull
    @Override
    public String getUrl(@NotNull EntityHeader header) {
        return getUrlString(header.getName());
    }

    /**
     * Returns the Url of this resource with the given id
     *
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    private String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if (id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
