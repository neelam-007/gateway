package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PublishedServiceTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The published service resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PublishedServiceResource.SERVICES_URI)
@Singleton
public class PublishedServiceResource extends DependentRestEntityResource<ServiceMO, ServiceAPIResourceFactory, PublishedServiceTransformer> {

    protected static final String SERVICES_URI = "services";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(ServiceAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PublishedServiceTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Shows the service versions
     *
     * @param id The service id
     * @return The policyVersion resource for handling policy version requests.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @Path("{id}/" + PolicyVersionResource.VERSIONS_URI)
    public PolicyVersionResource versions(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new PolicyVersionResource(Either.<String, String>left(id)));
    }

    /**
     * Creates a new entity
     *
     * @param resource The entity to create
     * @param comment  The comment to add to the policy version when creating the service
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ServiceMO resource, @QueryParam("versionComment") String comment) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.createResource(resource, comment);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, true);
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
    public Item<ServiceMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
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
     * @param guids           the guid filter
     * @param enabled         The enabled filter
     * @param soap            The soap filter
     * @param parentFolderIds The parent folder id filter
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<ServiceMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "parentFolder.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("guid") List<String> guids,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("soap") Boolean soap,
            @QueryParam("parentFolder.id") List<Goid> parentFolderIds,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "guid", "enabled", "soap", "parentFolder.id", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("disabled", (List) Arrays.asList(!enabled));
        }
        if (soap != null) {
            filters.put("soap", (List) Arrays.asList(soap));
        }
        if (guids != null && !guids.isEmpty()) {
            filters.put("guid", (List) guids);
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
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update
     * @param active   Should the service be activated after the update
     * @param comment  The comment to add to the policy version when creating the service
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(ServiceMO resource,
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
     * Deletes an existing active connector.
     *
     * @param id The id of the active connector to delete.
     * @throws ResourceFactory.ResourceNotFoundException
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
    public Item<ServiceMO> template() {
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId("FolderID");
        serviceMO.setServiceDetail(serviceDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent("Policy XML");
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        return super.createTemplateItem(serviceMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final ServiceMO serviceMO) {
        ArrayList<Link> links = new ArrayList<>(super.getRelatedLinks(serviceMO));
        if(serviceMO != null) {
            links.addAll(Arrays.asList(
                    ManagedObjectFactory.createLink("versions", getUrlString(serviceMO.getId() + "/" + PolicyVersionResource.VERSIONS_URI)),
                    ManagedObjectFactory.createLink("parentFolder", getUrlString(FolderResource.class, serviceMO.getServiceDetail().getFolderId() != null ? serviceMO.getServiceDetail().getFolderId() : Folder.ROOT_FOLDER_ID.toString()))));
        }
        return links;
    }
}
