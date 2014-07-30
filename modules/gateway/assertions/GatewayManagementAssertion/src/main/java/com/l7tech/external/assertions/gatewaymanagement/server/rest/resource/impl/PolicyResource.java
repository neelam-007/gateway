package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * This resource is used to manage policies. These can be internal, global or other special purpose policies.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PolicyResource.POLICIES_URI)
@Singleton
public class PolicyResource extends DependentRestEntityResource<PolicyMO, PolicyAPIResourceFactory, PolicyTransformer> {

    protected static final String POLICIES_URI = "policies";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(PolicyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PolicyTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Shows the policy versions
     *
     * @param id The policy id
     * @return The policyVersion resource for handling policy version requests.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @Path("{id}/" + PolicyVersionResource.VERSIONS_URI)
    public PolicyVersionResource versions(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new PolicyVersionResource(Either.<String, String>right(id)));
    }

    /**
     * Creates a new policy
     *
     * @param resource The policy to create
     * @param comment  The comment to add to the policy version when creating the policy
     * @return A reference to the newly created policy
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(PolicyMO resource, @QueryParam("versionComment") String comment) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.createResource(resource, comment);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, true);
    }

    /**
     * Returns a policy with the given ID.
     *
     * @param id The ID of the policy to return
     * @return The policy.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<PolicyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of policies. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/policies?name=MyPolicy</pre></div>
     * <p>Returns policy with name "MyPolicy".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param guids           Guid filter
     * @param types           Type filter
     * @param soap            Soap filter
     * @param parentFolderIds Parent folder ID filter.
     * @param securityZoneIds Security zone ID filter
     * @return A list of policies. If the list is empty then no policies were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<PolicyMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "parentFolder.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("guid") List<String> guids,
            @QueryParam("type") @ChoiceParam({"Include", "Internal", "Global"}) List<String> types,
            @QueryParam("soap") Boolean soap,
            @QueryParam("parentFolder.id") List<Goid> parentFolderIds,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "guid", "type", "soap", "parentFolder.id", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (guids != null && !guids.isEmpty()) {
            filters.put("guid", (List) guids);
        }
        if (types != null && !types.isEmpty()) {
            filters.put("type", (List) convertTypes(types));
        }
        if (soap != null) {
            filters.put("soap", (List) Arrays.asList(soap));
        }
        if (parentFolderIds != null && !parentFolderIds.isEmpty()) {
            filters.put("folder.id", (List) parentFolderIds);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(convertSort(sort), ascendingSort,
                filters.map());
    }

    private List<PolicyType> convertTypes(List<String> types) {
        return Functions.map(types, new Functions.UnaryThrows<PolicyType, String, InvalidArgumentException>() {
            @Override
            public PolicyType call(String type) {
                switch (type) {
                    case "Include":
                        return PolicyType.INCLUDE_FRAGMENT;
                    case "Internal":
                        return PolicyType.INTERNAL;
                    case "Global":
                        return PolicyType.GLOBAL_FRAGMENT;
                    default:
                        throw new InvalidArgumentException("type", "Invalid policy type '" + type + "'. Expected either: Include, Internal, or Global");
                }
            }
        });
    }

    private String convertSort(String sort) {
        if (sort == null) return null;
        switch (sort) {
            case "parentFolder.id":
                return "folder.id";
            default:
                return sort;
        }
    }

    /**
     * Creates or Updates an existing policy. If a policy with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Policy to create or update
     * @param id       ID of the policy to create or update
     * @param active   Should the policy be activated after the update.
     * @param comment  The comment to add to the policy version when updating the policy
     * @return A reference to the newly created or updated policy.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response updateOrCreate(PolicyMO resource,
                           @PathParam("id") String id,
                           @QueryParam("active") @DefaultValue("true") Boolean active,
                           @QueryParam("versionComment") String comment) throws ResourceFactory.ResourceFactoryException {

        boolean resourceExists = factory.resourceExists(id);
        if (resourceExists) {
            factory.updateResource(id, resource, comment, active);
        } else {
            factory.createResource(id, resource, comment);
        }
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, !resourceExists);
    }

    /**
     * Deletes an existing policy.
     *
     * @param id The ID of the policy to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example policy that can be used as a reference for what policy objects should
     * look like.
     *
     * @return The template policy.
     */
    @GET
    @Path("template")
    public Item<PolicyMO> template() {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setGuid("guid-8757cdae-d1ad-4ad5-bc08-b16b2d370759");

        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setGuid("guid-8757cdae-d1ad-4ad5-bc08-b16b2d370759");
        policyDetail.setFolderId("FolderID");
        policyDetail.setName("Policy Name");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("PropertyKey", "PropertyValue").map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        policyMO.setPolicyDetail(policyDetail);
        return super.createTemplateItem(policyMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final PolicyMO policy) {
        List<Link> links = super.getRelatedLinks(policy);
        if (policy != null) {
            links.add(ManagedObjectFactory.createLink("versions", getUrlString(policy.getId() + "/" + PolicyVersionResource.VERSIONS_URI)));
            links.add(ManagedObjectFactory.createLink("parentFolder", getUrlString(FolderResource.class, policy.getPolicyDetail().getFolderId() != null ? policy.getPolicyDetail().getFolderId() : Folder.ROOT_FOLDER_ID.toString())));
        }
        return links;
    }
}
