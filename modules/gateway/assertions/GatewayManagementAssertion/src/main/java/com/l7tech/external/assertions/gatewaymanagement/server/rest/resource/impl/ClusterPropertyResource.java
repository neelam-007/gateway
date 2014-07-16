package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ClusterPropertyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ClusterPropertyTransformer;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * Cluster properties are used to set global properties. Example cluster properties include "cluster.hostname" and
 * "log.levels"
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ClusterPropertyResource.CLUSTER_PROPERTIES_URI)
@Singleton
public class ClusterPropertyResource extends RestEntityResource<ClusterPropertyMO, ClusterPropertyAPIResourceFactory, ClusterPropertyTransformer> {

    protected static final String CLUSTER_PROPERTIES_URI = "clusterProperties";

    @Override
    @SpringBean
    public void setFactory(ClusterPropertyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(ClusterPropertyTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new cluster property.
     *
     * @param resource The cluster property to create
     * @return a reference to the newly created cluster property
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ClusterPropertyMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a cluster property with the given ID.
     *
     * @param id The ID of the cluster property to return
     * @return The cluster property.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ClusterPropertyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of cluster properties. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/clusterProperties?name=MyProperty</pre></div>
     * <p>Returns cluster property with name "MyProperty".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort  Key to sort the list by
     * @param order Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *              ascending if not specified
     * @param names Name filter
     * @return A list of cluster properties. If the list is empty then no cluster properties were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<ClusterPropertyMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing cluster property. If a cluster property with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Cluster property to create or update
     * @param id       ID of the cluster property to create or update
     * @return A reference to the newly created or updated cluster property.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(ClusterPropertyMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing cluster property.
     *
     * @param id The ID of the cluster property to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example cluster property that can be used as a reference for what cluster
     * property objects should look like.
     *
     * @return The template cluster property.
     */
    @GET
    @Path("template")
    public Item<ClusterPropertyMO> template() {
        ClusterPropertyMO clusterPropertyMO = ManagedObjectFactory.createClusterProperty();
        clusterPropertyMO.setName("Template Cluster Property Name");
        clusterPropertyMO.setValue("Template Cluster Property Value");
        return super.createTemplateItem(clusterPropertyMO);
    }
}
