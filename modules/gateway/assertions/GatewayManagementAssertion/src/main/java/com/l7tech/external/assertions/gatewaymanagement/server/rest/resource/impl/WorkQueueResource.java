package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.WorkQueueAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.WorkQueueTransformer;
import com.l7tech.gateway.api.*;
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
 * Work Queue is a per-node-in-memory queue that holds policy fragments for asynchronous execution.
 *
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + WorkQueueResource.workQueues_URI)
@Singleton
@Since(RestManVersion.VERSION_1_0_1)
public class WorkQueueResource extends RestEntityResource<WorkQueueMO, WorkQueueAPIResourceFactory, WorkQueueTransformer> {

    protected static final String workQueues_URI = "workQueues";

    @Override
    @SpringBean
    public void setFactory(WorkQueueAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(WorkQueueTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new work queue
     *
     * @param resource The work queue to create
     * @return A reference to the newly created work queue
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(WorkQueueMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a work queue with the given ID.
     *
     * @param id The ID of the Cassandra connection to return
     * @return The work queue.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<WorkQueueMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of  work queues. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/workQueues?name=MyWorkQueue</pre></div>
     * <p>Returns work queue with name "MyWorkQueue".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param maxQueueSize    Maximum queue size filter
     * @param threadPoolMax   Maximum thread pool filter
     * @param rejectPolicy    Reject policy filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of work queues. If the list is empty then no work queues were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<WorkQueueMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("maxQueueSize") List<Integer> maxQueueSize,
            @QueryParam("threadPoolMax") List<Integer> threadPoolMax,
            @QueryParam("rejectPolicy") List<String> rejectPolicy,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(),
                Arrays.asList("name", "maxQueueSize", "threadPoolMax", "rejectPolicy", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (maxQueueSize != null && !maxQueueSize.isEmpty()) {
            filters.put("maxQueueSize", (List) maxQueueSize);
        }
        if (threadPoolMax != null && !threadPoolMax.isEmpty()) {
            filters.put("threadPoolMax", (List) threadPoolMax);
        }
        if (rejectPolicy != null && !rejectPolicy.isEmpty()) {
            filters.put("rejectPolicy", (List) rejectPolicy);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing work queue. If a work queue with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Work queue to create or update
     * @param id       ID of the work queue to create or update
     * @return A reference to the newly created or updated work queue.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(WorkQueueMO resource, @PathParam("id") String id)
            throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing work queue.
     *
     * @param id The ID of the work queue to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example work queue that can be used as a reference for what work queue
     * objects should look like.
     *
     * @return The template work queue.
     */
    @GET
    @Path("template")
    public Item<WorkQueueMO> template() {
        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setName("TemplateWorkQueue");
        workQueueMO.setMaxQueueSize(1000);
        workQueueMO.setThreadPoolMax(100);
        workQueueMO.setRejectPolicy("WAIT_FOR_ROOM");
        return super.createTemplateItem(workQueueMO);
    }
}