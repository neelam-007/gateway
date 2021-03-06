package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServerModuleFileAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServerModuleFileTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleDigest;
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
import java.util.Collections;
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
    private static final String TEMPLATE_SAMPLE_SHA256 = ModuleDigest.hexEncodedDigest(TEMPLATE_SAMPLE_BYTES);

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
     * <p>Creates a new ServerModuleFile. The properties are optional but you may specify:</p>
     * <table class="properties-table" cellpadding="0" cellspacing="0">
     * <tr><th>Key</th><th>Type</th><th>Description</th></tr>
     * <tr>
     * <td>moduleFileName</td>
     * <td>String</td>
     * <td>The module original File Name.
     * Optional and if not specified empty text will be shown under Manage Server Module Files dialog in the Policy Manager.</td>
     * </tr>
     * <tr>
     * <td>moduleSize</td>
     * <td>String</td>
     * <td>This is the module data-bytes size in bytes.
     * Should not be assumed to be 100% reliable, and is intended to be used for display purposes.
     * Optional and if not specified the size will be calculated from the specified module data-bytes.</td>
     * </tr>
     * <tr>
     * <td>moduleAssertions</td>
     * <td>String</td>
     * <td>Comma separated list of Module Assertion ClassNames.
     * Optional and if not specified empty text will be shown under Server Module Files Properties dialog in the Policy Manager.</td>
     * </tr>
     * </table>
     * <p class="italicize">Example request:</p>
     * <div class="code">
     * <pre>
     * &lt;l7:ServerModuleFile xmlns:l7=&quot;http://ns.l7tech.com/2010/04/gateway-management&quot;&gt;
     *     &lt;l7:Name&gt;module name&lt;/l7:Name&gt;
     *     .......
     *     &lt;l7:Properties&gt;
     *         &lt;l7:Property key=&quot;moduleAssertions&quot;&gt;
     *              &lt;l7:StringValue&gt;TestAssertion1,TestAssertion2&lt;/l7:StringValue&gt;
     *         &lt;/l7:Property&gt;
     *         &lt;l7:Property key=&quot;moduleFileName&quot;&gt;
     *              &lt;l7:StringValue&gt;testAssertion.jar&lt;/l7:StringValue&gt;
     *         &lt;/l7:Property&gt;
     *         &lt;l7:Property key=&quot;moduleSize&quot;&gt;
     *              &lt;l7:StringValue&gt;38191&lt;/l7:StringValue&gt;
     *         &lt;/l7:Property&gt;
     *     &lt;/l7:Properties&gt;
     * &lt;/l7:ServerModuleFile&gt;
     * </pre>
     * </div>
     * <p>When ServerModuleFile functionality is disabled this method will fail with FORBIDDEN (403).</p>
     * <p>This responds with a reference to the newly created ServerModuleFile.</p>
     *
     * @param resource The ServerModuleFile to create.
     * @return A reference to the newly created ServerModuleFile.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws DisabledException when ServerModuleFile functionality is disabled.
     */
    @POST
    public Response upload(
            final ServerModuleFileMO resource
    ) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Creates a new ServerModuleFile with specified ID. <br/>
     * If a ServerModuleFile with the given ID does not exist one will be created and its data uploaded,
     * otherwise method will fail with FORBIDDEN (403).<br/>
     * Note that module data update is not currently supported and this method will fail with FORBIDDEN (403).
     * To update the module data delete and re-upload the module with newer version again.
     * <p>When ServerModuleFile functionality is disabled this method will fail with FORBIDDEN (403).</p>
     *
     * @param resource ServerModuleFile to create or update
     * @param id       ID of the ServerModuleFile to create or update
     * @return A reference to the newly created or updated ServerModuleFile.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.DuplicateResourceAccessException If a ServerModuleFile with the given ID does exist.
     * @throws DisabledException when ServerModuleFile functionality is disabled.
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(
            final ServerModuleFileMO resource,
            @PathParam("id") final String id
    ) throws ResourceFactory.ResourceFactoryException {
        boolean resourceExists = factory.resourceExists(id);
        if (resourceExists) {
            throw new ResourceFactory.DuplicateResourceAccessException("ServerModuleFile update not supported");
        }
        factory.createResource(id, resource);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, true);
    }

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
            @QueryParam("type") @ChoiceParam(value = {"Custom", "Modular"}, caseSensitive = false) final List<String> types,
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
                                    if (type.equalsIgnoreCase("Custom")) {
                                        return ModuleType.CUSTOM_ASSERTION;
                                    } else if (type.equalsIgnoreCase("Modular")) {
                                        return ModuleType.MODULAR_ASSERTION;
                                    } else {
                                        throw new InvalidArgumentException("type", "Invalid Module Type '" + type + "'. Expected either: 'Custom' or 'Modular'");
                                    }
                                }
                            })
            );
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
     * <p>When ServerModuleFile functionality is disabled this method will fail with FORBIDDEN (403).</p>
     *
     * @param id The id of the ServerModuleFile to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws DisabledException when ServerModuleFile functionality is disabled.
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
        final CollectionUtils.MapBuilder<String, String> builder = CollectionUtils.MapBuilder.builder();
        for (final String key : SignerUtils.ALL_SIGNING_PROPERTIES) {
            builder.put(
                    key,
                    "HPKkZ+bVCDJbtZxAcTKLNS+4OhC+AKIPvrihKaew9UJfkKa2u4pXZgp8CMTBgvGBBEtImjY5IUrI&#xD;\n" +
                            "2cWHnw2t9ahaQ8wBq6nK4CwntLqExDeOxVMVAtBZu2PyLi7qnUPswO0r/ZKudlVrn9EnrieHu+ju&#xD;\n" +
                            "vcXKDa36yTnNLS+Je6BA1cdAtuO9GBYjl2nr29pVwo69aHNpc1oNiZdxo5HmNrdrkMbUSUdP2XtX&#xD;\n" +
                            "xOx0jecRhRJU7K4Yw7ONrP4zFpau1lYIfQJEAnnD7BHAIfNu8LgG1nvckSz/2DYpeA9mpQZVj1HJ&#xD;\n" +
                            "rI0PoEjvI3O+mHCSxAPzqVMKNACQ+AYAx4NbIg=="
            );
        }
        serverModuleFileMO.setSignatureProperties(Collections.unmodifiableMap(builder.map()));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerModuleFile.PROP_SIZE, "4194378") // ~ 4 MB
                .put(ServerModuleFile.PROP_ASSERTIONS, "TestAssertion1,TestAssertion2")
                .put(ServerModuleFile.PROP_FILE_NAME, "TestAssertion.aar")
                .map()
        );
        return super.createTemplateItem(serverModuleFileMO);
    }

    /**
     * Runtime exception indicating {@link com.l7tech.gateway.common.module.ServerModuleFile} functionality is disabled.<br/>
     * When disabled, calls to {@code POST}, {@code PUT} and {@code DELETE} will fail with this exception.
     *
     * @see com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceAccessException
     */
    @SuppressWarnings("serial")
    public static class DisabledException extends ResourceFactory.ResourceAccessException {
        public DisabledException() {
            super("Server Module Files functionality has been disabled. To enable, set the cluster property 'serverModuleFile.upload.enable' to 'true'");
        }
    }

}
