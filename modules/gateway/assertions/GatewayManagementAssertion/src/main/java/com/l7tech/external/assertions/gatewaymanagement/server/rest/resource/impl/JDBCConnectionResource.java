package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JDBCConnectionAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.JDBCConnectionTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
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
 * JDBC connections allow the Gateway to query external databases and then use the query results during policy
 * consumption.
 *
 * @title JDBC Connection
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + JDBCConnectionResource.jdbcConnections_URI)
@Singleton
public class JDBCConnectionResource extends RestEntityResource<JDBCConnectionMO, JDBCConnectionAPIResourceFactory, JDBCConnectionTransformer> {

    protected static final String jdbcConnections_URI = "jdbcConnections";

    @Override
    @SpringBean
    public void setFactory(JDBCConnectionAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(JDBCConnectionTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new JDBC connection
     *
     * @param resource The JDBC connection to create
     * @return A reference to the newly created JDBC connection
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(JDBCConnectionMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a JDBC connection with the given ID.
     *
     * @param id The ID of the JDBC connection to return
     * @return The JDBC connection.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<JDBCConnectionMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of JDBC connections. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/jdbcConnections?name=MyJDBCConnection</pre></div>
     * <p>Returns JDBC connection with name "MyJDBCConnection".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param enabled         Enabled filter
     * @param jdbcUrls        JDBC URL filter
     * @param driverClasses   Driver class name filter
     * @param userNames       User name filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of JDBC connections. If the list is empty then no JDBC connections were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<JDBCConnectionMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("jdbcUrl") List<String> jdbcUrls,
            @QueryParam("driverClass") List<String> driverClasses,
            @QueryParam("userName") List<String> userNames,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "jdbcUrl", "driverClass", "userName", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (jdbcUrls != null && !jdbcUrls.isEmpty()) {
            filters.put("jdbcUrl", (List) jdbcUrls);
        }
        if (driverClasses != null && !driverClasses.isEmpty()) {
            filters.put("driverClass", (List) driverClasses);
        }
        if (userNames != null && !userNames.isEmpty()) {
            filters.put("userName", (List) userNames);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing JDBC connection. If a JDBC connection with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource JDBC connection to create or update
     * @param id       ID of the JDBC connection to create or update
     * @return A reference to the newly created or updated JDBC connection.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(JDBCConnectionMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing JDBC connection.
     *
     * @param id The ID of the JDBC connection to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example JDBC connection that can be used as a reference for what JDBC connection
     * objects should look like.
     *
     * @return The template JDBC connection.
     */
    @GET
    @Path("template")
    public Item<JDBCConnectionMO> template() {
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionMO.setName("TemplateJDBCConnection");
        jdbcConnectionMO.setDriverClass("com.my.driver.class");
        jdbcConnectionMO.setEnabled(true);
        jdbcConnectionMO.setJdbcUrl("example.connection.url");
        jdbcConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ConnectionProperty", "PropertyValue").map());
        return super.createTemplateItem(jdbcConnectionMO);
    }
}
