package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.EncapsulatedAssertionAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.EncapsulatedAssertionTransformer;
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
 * Encapsulated Assertion lets you turn any policy fragment into a self-contained "assertion" that accepts input values
 * and sets output values.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + EncapsulatedAssertionResource.ENCAPSULATED_ASSERTION_URI)
@Singleton
public class EncapsulatedAssertionResource extends DependentRestEntityResource<EncapsulatedAssertionMO, EncapsulatedAssertionAPIResourceFactory, EncapsulatedAssertionTransformer> {

    protected static final String ENCAPSULATED_ASSERTION_URI = "encapsulatedAssertions";

    @Override
    @SpringBean
    public void setFactory(EncapsulatedAssertionAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(EncapsulatedAssertionTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new encapsulated assertion
     *
     * @param resource The encapsulated assertion to create
     * @return A reference to the newly created encapsulated assertion
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(EncapsulatedAssertionMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an encapsulated assertion with the given ID.
     *
     * @param id The ID of the encapsulated assertion to return
     * @return The encapsulated assertion.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<EncapsulatedAssertionMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of encapsulated assertions. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent">/restman/1.0/activeConnectors?name=MyEncass</div>
     * <p>Returns encapsulated assertion with name "MyEncass".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by.
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param policyIds       Service id filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of encapsulated assertions. If the list is empty then no encapsulated assertions were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<EncapsulatedAssertionMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("policy.id") List<Goid> policyIds,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "policy.id", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (policyIds != null && !policyIds.isEmpty()) {
            filters.put("policy.id", (List) policyIds);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing encapsulated assertion. If an encapsulated assertion with the given ID does not
     * exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Encapsulated assertion to create or update
     * @param id       ID of the encapsulated assertion to create or update
     * @return A reference to the newly created or updated encapsulated assertion.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(EncapsulatedAssertionMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing encapsulated assertion.
     *
     * @param id The ID of the encapsulated assertion to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example encapsulated assertion that can be used as a reference for what
     * encapsulated assertion objects should look like.
     *
     * @return The template encapsulated assertion.
     */
    @GET
    @Path("template")
    public Item<EncapsulatedAssertionMO> template() {
        EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setName("Template Cluster Property Name");
        encapsulatedAssertionMO.setGuid("Encapsulated Assertion Guid");
        return super.createTemplateItem(encapsulatedAssertionMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final EncapsulatedAssertionMO encapsulatedAssertionMO) {
        List<Link> links = super.getRelatedLinks(encapsulatedAssertionMO);
        if (encapsulatedAssertionMO != null && encapsulatedAssertionMO.getPolicyReference() != null) {
            links.add(ManagedObjectFactory.createLink("policy", getUrlString(PolicyResource.class, encapsulatedAssertionMO.getPolicyReference().getId())));
        }
        return links;
    }
}
