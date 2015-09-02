package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyBackedServiceAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyBackedServiceTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyBackedServiceMO;
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
 * Policy Backed Services allow for custom policies to be referred to from gateway processes.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PolicyBackedServiceResource.policyBackedService_URI)
@Singleton
@Since(RestManVersion.VERSION_1_0_2)
public class PolicyBackedServiceResource extends RestEntityResource<PolicyBackedServiceMO, PolicyBackedServiceAPIResourceFactory, PolicyBackedServiceTransformer> {

    protected static final String policyBackedService_URI = "policyBackedServices";

    @Override
    @SpringBean
    public void setFactory(PolicyBackedServiceAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PolicyBackedServiceTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new Policy Backed Service
     *
     * @param resource The Policy Backed Service to create
     * @return A reference to the newly created Policy Backed Service
     */
    @POST
    public Response create(PolicyBackedServiceMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a Policy Backed Service with the given ID.
     *
     * @param id The ID of the Policy Backed Service to return
     * @return The Policy Backed Service.
     */
    @GET
    @Path("{id}")
    public Item<PolicyBackedServiceMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of Policy Backed Services. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/policyBackedServices?name=MyPolicyBackedService</pre></div>
     * <p>Returns Policy Backed Service with name "MyPolicyBackedService".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param interfaces      Interfaces filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of Policy Backed Services. If the list is empty then no Policy Backed Services were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<PolicyBackedServiceMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("interface") List<String> interfaces,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(),
                Arrays.asList("name", "interface", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (interfaces != null && !interfaces.isEmpty()) {
            filters.put("serviceInterfaceName", (List) interfaces);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing Policy Backed Service. If a Policy Backed Service with the given ID does not exist
     * one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Policy Backed Service to create or update
     * @param id       ID of the Policy Backed Service to create or update
     * @return A reference to the newly created or updated Policy Backed Service.
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(PolicyBackedServiceMO resource, @PathParam("id") String id)
            throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing Policy Backed Service.
     *
     * @param id The ID of the Policy Backed Service to delete.
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example Policy Backed Service that can be used as a reference for what Policy Backed Service
     * objects should look like.
     *
     * @return The template Policy Backed Service.
     */
    @GET
    @Path("template")
    public Item<PolicyBackedServiceMO> template() {
        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setName("TemplatePolicyBackedService");
        policyBackedServiceMO.setInterfaceName("my.interface.name");
        PolicyBackedServiceMO.PolicyBackedServiceOperation operation = new PolicyBackedServiceMO.PolicyBackedServiceOperation();
        operation.setPolicyId("bf5bbf25b64acf3cc74b5a2bf7bc1cde");
        operation.setOperationName("myOperation");
        policyBackedServiceMO.setPolicyBackedServiceOperations(Arrays.asList(operation));
        return super.createTemplateItem(policyBackedServiceMO);
    }
}
