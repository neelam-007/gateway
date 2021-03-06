package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyAliasAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyAliasTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * A policy alias allows a policy to appear in more than one folder in the Services and Policies list. The policy alias
 * is a linked copy of the original policy.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PolicyAliasResource.policyAlias_URI)
@Singleton
public class PolicyAliasResource extends RestEntityResource<PolicyAliasMO, PolicyAliasAPIResourceFactory, PolicyAliasTransformer> {

    protected static final String policyAlias_URI = "policyAliases";

    @Override
    @SpringBean
    public void setFactory(PolicyAliasAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PolicyAliasTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new policy alias
     *
     * @param resource The policy alias to create
     * @return A reference to the newly created policy alias
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(PolicyAliasMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a policy alias with the given ID.
     *
     * @param id The ID of the policy alias to return
     * @return The policy alias.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<PolicyAliasMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of policy aliases. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/policyAliases?policy.id=26df9b0abc4dd6780fd9da5929cde13e</pre></div>
     * <p>Returns policy aliases for policy with ID "26df9b0abc4dd6780fd9da5929cde13e".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param policyIds       Policy id filter
     * @param folderIds       Folder id filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of policy aliases. If the list is empty then no policy aliases were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<PolicyAliasMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "policy.id", "folder.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("policy.id") List<Goid> policyIds,
            @QueryParam("folder.id") List<Goid> folderIds,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("policy.id", "folder.id", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (policyIds != null && !policyIds.isEmpty()) {
            filters.put("entityGoid", (List) policyIds);
        }
        if (folderIds != null && !folderIds.isEmpty()) {
            filters.put("folder.id", (List) folderIds);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(convertSort(sort), ascendingSort,
                filters.map());
    }

    private String convertSort(String sort) {
        if (sort == null) return null;
        switch (sort) {
            case "policy.id":
                return "entityGoid";
            default:
                return sort;
        }
    }

    /**
     * Creates or Updates an existing policy alias. If a policy alias with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Policy alias to create or update
     * @param id       ID of the policy alias to create or update
     * @return A reference to the newly created or updated policy alias.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response updateOrCreate(PolicyAliasMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing policy alias.
     *
     * @param id The ID of the policy alias to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example policy alias that can be used as a reference for what policy alias
     * objects should look like.
     *
     * @return The template policy alias.
     */
    @GET
    @Path("template")
    public Item<PolicyAliasMO> template() {
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId("Folder ID");
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyAliasMO.class, new Goid(3, 1).toString()));
        return super.createTemplateItem(policyAliasMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final PolicyAliasMO policyAliasMO) {
        List<Link> links = super.getRelatedLinks(policyAliasMO);
        if (policyAliasMO != null) {
            links.add(ManagedObjectFactory.createLink("parentFolder", getUrlString(FolderResource.class, policyAliasMO.getFolderId() != null ? policyAliasMO.getFolderId() : Folder.ROOT_FOLDER_ID.toString())));
            links.add(ManagedObjectFactory.createLink("policy", getUrlString(PolicyResource.class, policyAliasMO.getPolicyReference().getId())));
        }
        return links;
    }
}
