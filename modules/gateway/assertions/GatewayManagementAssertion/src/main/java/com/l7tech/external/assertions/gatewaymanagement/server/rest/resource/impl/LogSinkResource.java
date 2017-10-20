package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.LogSinkAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.LogSinkTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.LogSinkFilter;
import com.l7tech.gateway.api.LogSinkMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Log Sink Configurations defines where the Gateway pushes logs to.
 *
 * @title Log Sink Configuration
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + LogSinkResource.LogSinks_URI)
@Since( value= RestManVersion.VERSION_1_0_4)
@Singleton
public class LogSinkResource extends DependentRestEntityResource<LogSinkMO, LogSinkAPIResourceFactory, LogSinkTransformer> {

    protected static final String LogSinks_URI = "logSinks";

    @Override
    @SpringBean
    public void setFactory(LogSinkAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(LogSinkTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new Log Sink Configuration
     *
     * @param resource The Log Sink Configuration to create
     * @return A reference to the newly created Log Sink Configuration
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(LogSinkMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a Log Sink Configuration with the given ID.
     *
     * @param id The ID of the Log Sink Configuration to return
     * @return The Log Sink Configuration.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<LogSinkMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of Log Sink Configurations. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/LogSinks?name=MyLogSink</pre></div>
     * <p>Returns Log Sink Configuration with name "MyLogSink".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param types           Type filter
     * @param descriptions    Description filter
     * @param enabled         Enabled filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of Log Sink Configurations. If the list is empty then no Log Sink Configurations were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<LogSinkMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("type") List<String> types,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("description") List<String> descriptions,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "type", "description", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (types != null && !types.isEmpty()) {
            filters.put("type",   (List) Functions.map(types, new Functions.UnaryThrows<SinkConfiguration.SinkType, String, InvalidArgumentException>() {
                @Override
                public SinkConfiguration.SinkType call(String type) {
                    try {
                        return SinkConfiguration.SinkType.valueOf(type);
                    } catch (IllegalArgumentException e){
                        throw new InvalidArgumentException("type", "Invalid sink type '" + type + "'. Expected either: FILE or SYSLOG");
                    }
                }
            }));
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (descriptions != null && !descriptions.isEmpty()) {
            filters.put("description", (List) descriptions);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing Log Sink Configuration. If a Log Sink Configuration with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Log Sink Configuration to create or update
     * @param id       ID of the Log Sink Configuration to create or update
     * @return A reference to the newly created or updated Log Sink Configuration.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(LogSinkMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing Log Sink Configuration.
     *
     * @param id The ID of the Log Sink Configuration to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example Log Sink Configuration that can be used as a reference for what Log Sink Configuration
     * objects should look like.
     *
     * @return The template Log Sink Configuration.
     */
    @GET
    @Path("template")
    public Item<LogSinkMO> template() {
        LogSinkMO logSinkMO = ManagedObjectFactory.createLogSinkMO();
        logSinkMO.setName("TemplateLogSink");
        logSinkMO.setDescription("Description");
        logSinkMO.setCategories(CollectionUtils.list(LogSinkMO.Category.AUDIT, LogSinkMO.Category.LOG));
        logSinkMO.setEnabled(true);
        logSinkMO.setType(LogSinkMO.SinkType.FILE);
        logSinkMO.setSeverity(LogSinkMO.SeverityThreshold.INFO);
        logSinkMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("file.format", "STANDARD").put("file.logCount", "10").put("file.maxSize", "20000").map());
        LogSinkFilter filter = ManagedObjectFactory.createLogSinkFilter();
        filter.setType(GatewayDiagnosticContextKeys.LOGGER_NAME);
        filter.setValues( CollectionUtils.list("com.myorg*"));
        logSinkMO.setFilters(CollectionUtils.list(filter));
        return super.createTemplateItem(logSinkMO);
    }
}
