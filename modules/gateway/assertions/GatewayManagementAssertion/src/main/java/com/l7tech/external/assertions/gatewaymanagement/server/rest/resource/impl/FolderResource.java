package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.FolderAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.FolderTransformer;
import com.l7tech.gateway.api.*;
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

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * Folders are used to organize the policies, services, and aliases you have on the Gateway.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + FolderResource.FOLDERS_URI)
@Singleton
public class FolderResource extends DependentRestEntityResource<FolderMO, FolderAPIResourceFactory, FolderTransformer> {

    protected static final String FOLDERS_URI = "folders";

    @Override
    @SpringBean
    public void setFactory(FolderAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(FolderTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new folder
     *
     * @param resource The folder to create
     * @return A reference to the newly created folder
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(FolderMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a folder with the given ID.
     *
     * @param id The ID of the folder to return
     * @return The folder.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<FolderMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of folders. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/folders?name=MyFolder</pre></div>
     * <p>Returns folder with name "MyFolder".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param parentFolderIds The parent folder filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of folders. If the list is empty then no folders were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<FolderMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "parentFolder.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("parentFolder.id") List<Goid> parentFolderIds,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "parentFolder.id", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }

        if (parentFolderIds != null && !parentFolderIds.isEmpty()) {
            filters.put("folder.id", (List) parentFolderIds);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing folder. If a folder with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Folder to create or update
     * @param id       ID of the folder to create or update
     * @return A reference to the newly created or updated folder.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(FolderMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing folder.
     *
     * @param id    The id of the folder to delete.
     * @param force If true, deletes folder and its contents
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id,
                       @QueryParam("force") @DefaultValue("false") final boolean force) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id, force);
    }

    /**
     * Returns a template, which is an example folder that can be used as a reference for what folder objects should
     * look like.
     *
     * @return The template folder.
     */
    @GET
    @Path("template")
    public Item<FolderMO> template() {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setName("Folder Template");
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        return super.createTemplateItem(folderMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final FolderMO folder) {
        List<Link> links = super.getRelatedLinks(folder);
        if (folder != null && folder.getFolderId() != null) {
            links.add(ManagedObjectFactory.createLink("parentFolder", getUrlString(folder.getFolderId())));
        }
        return links;
    }
}
