package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CassandraConnectionAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CassandraConnectionTransformer;
import com.l7tech.gateway.api.CassandraConnectionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
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
 * Cassandra connections allow the Gateway to query external databases and then use the query results during policy
 * consumption.
 *
 * Cassandra Connection
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + CassandraConnectionResource.cassandraConnections_URI)
@Singleton
@Since(RestManVersion.VERSION_1_0_1)
public class CassandraConnectionResource extends RestEntityResource<CassandraConnectionMO, CassandraConnectionAPIResourceFactory, CassandraConnectionTransformer> {

    protected static final String cassandraConnections_URI = "cassandraConnections";

    @Override
    @SpringBean
    public void setFactory(CassandraConnectionAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(CassandraConnectionTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new Cassandra connection
     *
     * @param resource The Cassandra connection to create
     * @return A reference to the newly created Cassandra connection
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(CassandraConnectionMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a Cassandra connection with the given ID.
     *
     * @param id The ID of the Cassandra connection to return
     * @return The Cassandra connection.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<CassandraConnectionMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of Cassandra connections. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/cassandraConnections?name=MyCassandraConnection</pre></div>
     * <p>Returns Cassandra connection with name "MyCassandraConnection".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param keyspaces       Keyspace filter
     * @param contactPoints   Contact point filter
     * @param ports           Port filter
     * @param usernames       User name filter
     * @param compressions    Compression filter
     * @param isSsl           SSL filter
     * @param enabled         Enabled filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of Cassandra connections. If the list is empty then no Cassandra connections were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<CassandraConnectionMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("keyspace") List<String> keyspaces,
            @QueryParam("contactPoint") List<String> contactPoints,
            @QueryParam("port") List<String> ports,
            @QueryParam("username") List<String> usernames,
            @QueryParam("compression") List<String> compressions,
            @QueryParam("ssl") Boolean isSsl,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(),
                Arrays.asList("name", "keyspace", "contactPoint", "port", "username", "compression", "ssl", "enabled", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (keyspaces != null && !keyspaces.isEmpty()) {
            filters.put("keyspaceName", (List) keyspaces);
        }
        if (contactPoints != null && !contactPoints.isEmpty()) {
            filters.put("contactPoints", (List) contactPoints);
        }
        if (ports != null && !ports.isEmpty()) {
            filters.put("port", (List) ports);
        }
        if (usernames != null && !usernames.isEmpty()) {
            filters.put("username", (List) usernames);
        }
        if (compressions != null && !compressions.isEmpty()) {
            filters.put("compression", (List) compressions);
        }
        if (isSsl != null) {
            filters.put("ssl", (List) Arrays.asList(isSsl));
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing Cassandra connection. If a Cassandra connection with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Cassandra connection to create or update
     * @param id       ID of the Cassandra connection to create or update
     * @return A reference to the newly created or updated Cassandra connection.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(CassandraConnectionMO resource, @PathParam("id") String id)
            throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing Cassandra connection.
     *
     * @param id The ID of the Cassandra connection to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example Cassandra connection that can be used as a reference for what Cassandra connection
     * objects should look like.
     *
     * @return The template Cassandra connection.
     */
    @GET
    @Path("template")
    public Item<CassandraConnectionMO> template() {
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName("TemplateCassandraConnection");
        cassandraConnectionMO.setKeyspace("example.keyspace");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setPort("9042");
        cassandraConnectionMO.setUsername("gateway");
        cassandraConnectionMO.setPasswordId("");
        cassandraConnectionMO.setCompression("NONE");
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setTlsciphers(null);
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMO.setProperties(
                CollectionUtils.MapBuilder.<String, String>builder().put("ConnectionProperty", "PropertyValue").map());
        return super.createTemplateItem(cassandraConnectionMO);
    }
}