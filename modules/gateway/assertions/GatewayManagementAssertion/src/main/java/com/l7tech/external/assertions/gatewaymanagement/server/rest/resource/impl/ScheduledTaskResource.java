package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ScheduledTaskAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ScheduledTaskTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Scheduled tasks allow the Gateway to schedule policy consumption.
 *
 * Scheduled Task
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ScheduledTaskResource.scheduledTasks_URI)
@Singleton
@Since(RestManVersion.VERSION_1_0_2)
public class ScheduledTaskResource extends RestEntityResource<ScheduledTaskMO, ScheduledTaskAPIResourceFactory, ScheduledTaskTransformer> {

    protected static final String scheduledTasks_URI = "scheduledTasks";

    @Override
    @SpringBean
    public void setFactory(ScheduledTaskAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(ScheduledTaskTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new Scheduled task
     *
     * @param resource The Scheduled task to create
     * @return A reference to the newly created Scheduled task
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ScheduledTaskMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a Scheduled task with the given ID.
     *
     * @param id The ID of the Scheduled task to return
     * @return The Scheduled task.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ScheduledTaskMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of Scheduled tasks. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/scheduledTasks?name=MyScheduledTask</pre></div>
     * <p>Returns Scheduled task with name "MyScheduledTask".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param nodes           Node filter; "all" or "one"
     * @param types           Job Type filter
     * @param status          Job Status filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of Scheduled tasks. If the list is empty then no Scheduled tasks were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<ScheduledTaskMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("node") List<String> nodes,
            @QueryParam("type") List<String> types,
            @QueryParam("status") List<String> status,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(),
                Arrays.asList("name", "node","type","status", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (nodes != null && !nodes.isEmpty()) {
            filters.put("useOneNode", (List) Functions.map(nodes,new Functions.Unary<Object, String>() {
                @Override
                public Object call(String s) {
                    return "one".equalsIgnoreCase(s);
                }
            }));
        }
        if (types != null && !types.isEmpty()) {
            filters.put("jobType", (List)Functions.map(types, new Functions.UnaryThrows<JobType, String, InvalidArgumentException>() {
                @Override
                public JobType call(String type) {
                    switch (type) {
                        case "One time":
                            return JobType.ONE_TIME;
                        case "Recurring":
                            return JobType.RECURRING;
                        default:
                            throw new InvalidArgumentException("type", "Invalid job type '" + type + "'. Expected either: One time or Recurring");
                    }
                }
            }));
        }
        if (status != null && !status.isEmpty()) {
            filters.put("jobStatus", (List)Functions.map(status, new Functions.UnaryThrows<JobStatus, String, InvalidArgumentException>() {
                @Override
                public JobStatus call(String type) {
                    switch (type) {
                        case "Scheduled":
                            return JobStatus.SCHEDULED;
                        case "Completed":
                            return JobStatus.COMPLETED;
                        case "Disabled":
                            return JobStatus.DISABLED;
                        default:
                            throw new InvalidArgumentException("type", "Invalid job status '" + type + "'. Expected either: Scheduled, Completed, or Disabled");
                    }
                }
            }));
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing Scheduled task. If a Scheduled task with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Scheduled task to create or update
     * @param id       ID of the Scheduled task to create or update
     * @return A reference to the newly created or updated Scheduled task.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(ScheduledTaskMO resource, @PathParam("id") String id)
            throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing Scheduled task.
     *
     * @param id The ID of the Scheduled task to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example Scheduled task that can be used as a reference for what Scheduled task
     * objects should look like.
     *
     * @return The template Scheduled task.
     */
    @GET
    @Path("template")
    public Item<ScheduledTaskMO> template() {
        ScheduledTaskMO scheduledTaskMO = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTaskMO.setName("TemplateScheduledTask");
        scheduledTaskMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class,"policyId"));
        scheduledTaskMO.setJobType(ScheduledTaskMO.ScheduledTaskJobType.ONE_TIME);
        scheduledTaskMO.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.COMPLETED);
        scheduledTaskMO.setExecutionDate(new Date());
        return super.createTemplateItem(scheduledTaskMO);
    }
}