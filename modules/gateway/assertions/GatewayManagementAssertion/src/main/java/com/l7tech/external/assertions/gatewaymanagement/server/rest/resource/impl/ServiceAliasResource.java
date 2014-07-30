package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceAliasAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServiceAliasTransformer;
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
 * A service alias allows a service to appear in more than one folder in the Services and Policies list. The service
 * alias is a linked copy of the original service.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ServiceAliasResource.publishedServiceAlias_URI)
@Singleton
public class ServiceAliasResource extends RestEntityResource<ServiceAliasMO, ServiceAliasAPIResourceFactory, ServiceAliasTransformer> {

    protected static final String publishedServiceAlias_URI = "serviceAliases";

    @Override
    @SpringBean
    public void setFactory(ServiceAliasAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(ServiceAliasTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new service alias
     *
     * @param resource The service alias to create
     * @return A reference to the newly created service alias
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ServiceAliasMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a service alias with the given ID.
     *
     * @param id The ID of the service alias to return
     * @return The service alias.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ServiceAliasMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of service aliases. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/serviceAliases?service.id=26df9b0abc4dd6780fd9da5929cde13e</pre></div>
     * <p>Returns service aliases for service with ID "26df9b0abc4dd6780fd9da5929cde13e".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param serviceIds      Service id filter
     * @param folderIds       Folder id filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of service aliases. If the list is empty then no service aliases were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<ServiceAliasMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "service.id", "folder.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("service.id") List<Goid> serviceIds,
            @QueryParam("folder.id") List<Goid> folderIds,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("service.id", "folder.id", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (serviceIds != null && !serviceIds.isEmpty()) {
            filters.put("entityGoid", (List) serviceIds);
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
            case "service.id":
                return "entityGoid";
            default:
                return sort;
        }
    }

    /**
     * Creates or Updates an existing service alias. If a service alias with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Service alias to create or update
     * @param id       ID of the service alias to create or update
     * @return A reference to the newly created or updated service alias.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response updateOrCreate(ServiceAliasMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing service alias.
     *
     * @param id The ID of the service alias to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example service alias that can be used as a reference for what service alias
     * objects should look like.
     *
     * @return The template service alias.
     */
    @GET
    @Path("template")
    public Item<ServiceAliasMO> template() {
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId("Folder ID");
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, new Goid(3, 1).toString()));
        return super.createTemplateItem(serviceAliasMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final ServiceAliasMO serviceAliasMO) {
        List<Link> links = super.getRelatedLinks(serviceAliasMO);
        if (serviceAliasMO != null) {
            links.add(ManagedObjectFactory.createLink("parentFolder", getUrlString(FolderResource.class, serviceAliasMO.getFolderId() != null ? serviceAliasMO.getFolderId() : Folder.ROOT_FOLDER_ID.toString())));
            links.add(ManagedObjectFactory.createLink("service", getUrlString(PublishedServiceResource.class, serviceAliasMO.getServiceReference().getId())));
        }
        return links;
    }
}
