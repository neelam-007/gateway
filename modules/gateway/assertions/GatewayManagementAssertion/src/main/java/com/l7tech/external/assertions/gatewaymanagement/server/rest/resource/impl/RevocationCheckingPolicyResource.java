package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RevocationCheckingPolicyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.RevocationCheckingPolicyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + RevocationCheckingPolicyResource.revocationCheckingPolicies_URI)
@Singleton
public class RevocationCheckingPolicyResource extends RestEntityResource<RevocationCheckingPolicyMO,RevocationCheckingPolicyAPIResourceFactory,RevocationCheckingPolicyTransformer> {

    protected static final String revocationCheckingPolicies_URI = "revocationCheckingPolicies";

    @Override
    @SpringBean
    public void setFactory(RevocationCheckingPolicyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(RevocationCheckingPolicyTransformer transformer) {
        super.transformer = transformer;
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
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<RevocationCheckingPolicyMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
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
        List<Item<RevocationCheckingPolicyMO>> items = Functions.map(factory.listResources(sort, ascendingSort, filters.map()), new Functions.Unary<Item<RevocationCheckingPolicyMO>, RevocationCheckingPolicyMO>() {
            @Override
            public Item<RevocationCheckingPolicyMO> call(RevocationCheckingPolicyMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<RevocationCheckingPolicyMO>(factory.getResourceType() + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(null))
                .build();
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
    public Item<RevocationCheckingPolicyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        RevocationCheckingPolicyMO resource = factory.getResource(id);
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build();
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
    public Response create(RevocationCheckingPolicyMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
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
    public Response update(RevocationCheckingPolicyMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
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
    public Item<RevocationCheckingPolicyMO> template() {
        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("TemplateRevocationCheckingPolicy");
        checkPolicyMO.setDefaultPolicy(false);
        checkPolicyMO.setContinueOnServerUnavailable(false);
        checkPolicyMO.setDefaultSuccess(false);
        RevocationCheckingPolicyItemMO checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_CERTIFICATE);
        checkItem.setUrl("TemplateItemUrl");
        checkItem.setTrustedSigners(CollectionUtils.list("TrustedCertId"));
        checkItem.setAllowIssuerSignature(false);
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        return super.createTemplateItem(checkPolicyMO);
    }
}
