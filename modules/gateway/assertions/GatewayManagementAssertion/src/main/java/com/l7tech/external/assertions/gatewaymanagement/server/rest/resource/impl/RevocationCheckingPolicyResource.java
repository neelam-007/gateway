package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RevocationCheckingPolicyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.RevocationCheckingPolicyTransformer;
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
 * A revocation checking policy defines the strategies used by the Gateway to determine whether a certificate has been
 * revoked.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + RevocationCheckingPolicyResource.revocationCheckingPolicies_URI)
@Singleton
public class RevocationCheckingPolicyResource extends RestEntityResource<RevocationCheckingPolicyMO, RevocationCheckingPolicyAPIResourceFactory, RevocationCheckingPolicyTransformer> {

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
     * <p>Returns a list of revocation checking policies. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/revocationCheckingPolicies?name=MyRevocationCheckPolicy</pre></div>
     * <p>Returns revocation checking policy with name "MyRevocationCheckPolicy".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of revocation checking policies. If the list is empty then no revocation checking policies were
     * found.
     */
    @SuppressWarnings("unchecked")
    @GET
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
        return super.list(sort, ascendingSort, filters.map());
    }

    /**
     * Returns a revocation checking policy with the given ID.
     *
     * @param id The ID of the revocation checking policy to return
     * @return The revocation checking policy.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<RevocationCheckingPolicyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }


    /**
     * Creates a new revocation checking policy
     *
     * @param resource The revocation checking policy to create
     * @return A reference to the newly created revocation checking policy
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(RevocationCheckingPolicyMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Creates or Updates an existing revocation checking policy. If an revocation checking policy with the given ID
     * does not exist one will be created, otherwise the existing one will be updated.
     *
     * @param resource Revocation checking policy to create or update
     * @param id       ID of the revocation checking policy to create or update
     * @return A reference to the newly created or updated revocation checking policy.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(RevocationCheckingPolicyMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing revocation checking policy.
     *
     * @param id The ID of the revocation checking policy to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example revocation checking policy that can be used as a reference for what
     * revocation checking policy objects should look like.
     *
     * @return The template revocation checking policy.
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
