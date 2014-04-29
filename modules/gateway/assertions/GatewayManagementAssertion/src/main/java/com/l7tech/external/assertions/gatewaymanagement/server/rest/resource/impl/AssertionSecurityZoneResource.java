package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.AssertionSecurityZoneAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.AssertionSecurityZoneTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The assertion security zone resource
 */
@Provider
@Path(AssertionSecurityZoneResource.Version_URI + AssertionSecurityZoneResource.activeConnectors_URI)
public class AssertionSecurityZoneResource implements URLAccessible<AssertionSecurityZoneMO> {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;
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
     * @param sort            the key to sort the list by.
     * @param order           the order to sort the list. true for ascending, false for descending. null implies
     *                        ascending
     * @param names           The name filter
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<AssertionSecurityZoneMO> list(
            @QueryParam("sort") @ChoiceParam({"name", "securityZone.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("name", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        List<Item<AssertionSecurityZoneMO>> items = Functions.map(factory.listResources(sort, ascendingSort, filters.map()), new Functions.Unary<Item<AssertionSecurityZoneMO>, AssertionSecurityZoneMO>() {
            @Override
            public Item<AssertionSecurityZoneMO> call(AssertionSecurityZoneMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<AssertionSecurityZoneMO>(EntityType.ASSERTION_ACCESS + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(null))
                .build();
    }

    /**
     * Returns the assertion security zone for the assertion with the given name.
     *
     * @param name The name of the assertion
     * @return The Assertion security zone for the assertion
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{name}")
    public Item<AssertionSecurityZoneMO> get(@PathParam("name") String name) throws ResourceFactory.ResourceNotFoundException {
        AssertionSecurityZoneMO resource = factory.getResourceByName(name);
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build();
    }


    /**
     * This will return a template, example entity that can be used as a reference for what entity objects should look
     * like.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Item<AssertionSecurityZoneMO> template() {
        AssertionSecurityZoneMO assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName("TemplateAssertionSecurityZone");
        assertionSecurityZoneMO.setSecurityZoneId("SecurityZoneID");

        return new ItemBuilder<AssertionSecurityZoneMO>(getResourceType() + " Template", getResourceType())
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(assertionSecurityZoneMO))
                .setContent(assertionSecurityZoneMO)
                .build();
    }

    /**
     * Updates an assertion security zone
     *
     * @param resource The updated assertion security zone
     * @param name     The name of the assertion to update the security zone of.
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{name}")
    public Response update(AssertionSecurityZoneMO resource, @PathParam("name") String name) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        AssertionSecurityZoneMO updatedResource = factory.updateResourceByName(name, resource);
        return Response.ok().entity(new ItemBuilder<>(transformer.convertToItem(updatedResource))
                .setContent(null)
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(updatedResource))
                .build()).build();
    }

    @NotNull
    @Override
    public Link getLink(@NotNull AssertionSecurityZoneMO resource) {
        return ManagedObjectFactory.createLink("self", getUrl(resource));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable AssertionSecurityZoneMO resource) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("template", getUrlString("template")),
                ManagedObjectFactory.createLink("list", getUrlString(null))
        );
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
