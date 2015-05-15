package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServerModuleFileAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServerModuleFileTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * A ServerModuleFile represents a Modular or a Custom Assertion(s) Module.
 */
@Provider
@Path(ServerModuleFileResource.RestEntityResource_version_URI + ServerModuleFileResource.serverModuleFiles_URI)
@Singleton
@Since(RestManVersion.VERSION_1_0_2)
public class ServerModuleFileResource extends RestEntityResource<ServerModuleFileMO, ServerModuleFileAPIResourceFactory, ServerModuleFileTransformer> {

    private static final byte[] TEMPLATE_SAMPLE_BYTES = "base 64 encoded module bytes goes here".getBytes(Charsets.UTF8);
    private static final String TEMPLATE_SAMPLE_SHA256 = ServerModuleFile.calcBytesChecksum(TEMPLATE_SAMPLE_BYTES);

    protected static final String serverModuleFiles_URI = "serverModuleFiles";

    @Override
    @SpringBean
    public void setFactory(final ServerModuleFileAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(final ServerModuleFileTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates new ServerModuleFile.
     *
     * @param resource The ServerModuleFile to create.
     * @return A reference to the newly created ServerModuleFile.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response upload(
            final ServerModuleFileMO resource
    ) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

//    /**
//     * Creates or Updates an existing ServerModuleFile. <br/>
//     * If a ServerModuleFile with the given ID does not exist one will be created and its data uploaded,
//     * otherwise the existing module name will be updated.<br/>
//     * Note that module data update is not currently supported (module data will be ignored if specified),
//     * only the module name can/will be updated.
//     * To update the module data delete and re-upload the module again.
//     *
//     * @param resource ServerModuleFile to create or update
//     * @param id       ID of the scheduled task to create or update
//     * @return A reference to the newly created or updated Scheduled task.
//     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
//     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
//     */
//    @PUT
//    @Path("{id}")
//    public Response createOrUpdate(
//            final ServerModuleFileMO resource,
//            @PathParam("id") final String id
//    ) throws ResourceFactory.ResourceFactoryException {
//        return super.update(resource, id);
//    }

    /**
     * Returns a ServerModuleFile with the given id.
     *
     * @param id            The id of the ServerModuleFile to retrieve.
     * @param includeData   Optionally include the module data bytes, defaults to false if not specified.
     * @return The ServerModuleFile associated with the given id.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ServerModuleFileMO> get(
            @PathParam("id") final String id,
            @QueryParam("includeData") @DefaultValue("false") final Boolean includeData
    ) throws ResourceFactory.ResourceNotFoundException {
        final ServerModuleFileMO resource = factory.getResource(id, includeData != null ? includeData : false);
        return RestEntityResourceUtils.createGetResponseItem(resource, transformer, this);
    }

    /**
     * <p>Returns a list of ServerModuleFiles. Can optionally sort the resulting list in ascending or
     * descending order and optionally include the module data. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/serverModuleFiles?name=MyServerModuleFile</pre></div>
     * <p>Returns ServerModuleFile with name "MyServerModuleFile".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort         The sort key to sort the list by; 'id'=GOID, 'name'=name, 'type'=module_type.
     * @param order        Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to ascending if not specified.
     * @param names        Name filter.
     * @param types        Module Type filter.
     * @param includeData  Optionally include the module data bytes, defaults to false if not specified.
     * @return A list of ServerModuleFiles. If the list is empty then no ServerModuleFiles were found.
     */
    @GET
    public ItemsList<ServerModuleFileMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "type"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) final String order,
            @QueryParam("name") final List<String> names,
            @QueryParam("type") final List<String> types,
            @QueryParam("includeData") @DefaultValue("false") final Boolean includeData
    ) {
        final Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(
                uriInfo.getQueryParameters(),
                Arrays.asList("name", "type", "includeData")
        );

        final CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            //noinspection unchecked
            filters.put("name", (List) names);
        }
        if (types != null && !types.isEmpty()) {
            //noinspection unchecked
            filters.put(
                    "moduleType",
                    (List) Functions.map(
                            types,
                            new Functions.UnaryThrows<ModuleType, String, InvalidArgumentException>() {
                                @Override
                                public ModuleType call(final String type) {
                                    switch (type) {
                                        case "Custom Assertion":
                                            return ModuleType.CUSTOM_ASSERTION;
                                        case "Modular Assertion":
                                            return ModuleType.MODULAR_ASSERTION;
                                        default:
                                            throw new InvalidArgumentException("type", "Invalid Module Type '" + type + "'. Expected either: 'Custom Assertion' or 'Modular Assertion'");
                                    }
                                }
                            }));
        }

        if (StringUtils.equalsIgnoreCase("type", sort)) {
            sort = "moduleType";
        }

        return RestEntityResourceUtils.createItemsList(
                factory.listResources(sort, ascendingSort, filters.map(), includeData),
                transformer,
                this,
                uriInfo.getRequestUri().toString());
    }

    /**
     * Deletes an existing ServerModuleFile.
     *
     * @param id The id of the ServerModuleFile to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(
            @PathParam("id") final String id
    ) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example ServerModuleFile that can be used as a reference
     * for what ServerModuleFile objects should look like.
     *
     * @return The template ServerModuleFile.
     */
    @GET
    @Path("template")
    public Item<ServerModuleFileMO> template() {
        final ServerModuleFileMO serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("TemplateServerModuleFile");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION);
        serverModuleFileMO.setModuleSha256(TEMPLATE_SAMPLE_SHA256);
        serverModuleFileMO.setModuleData(TEMPLATE_SAMPLE_BYTES);
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerModuleFile.PROP_SIZE, "4194378") // ~ 4 MB
                .put(ServerModuleFile.PROP_ASSERTIONS, "TestAssertion1,TestAssertion2")
                .map()
        );
        return super.createTemplateItem(serverModuleFileMO);
    }
}
