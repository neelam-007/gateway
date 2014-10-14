package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SampleMessageAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SampleMessageTransformer;
import com.l7tech.gateway.api.SampleMessageMO;
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
* A sample message is a message to help configure assertions.
*/
@Since(RestManVersion.VERSION_1_0_1)
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SampleMessageResource.sampleMessage_URI)
@Singleton
public class SampleMessageResource extends RestEntityResource<SampleMessageMO, SampleMessageAPIResourceFactory, SampleMessageTransformer> {

    protected static final String sampleMessage_URI = "sampleMessages";

    @Override
    @SpringBean
    public void setFactory(SampleMessageAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(SampleMessageTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new sample message
     *
     * @param resource The sample message to create
     * @return A reference to the newly created sample message
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(SampleMessageMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a sample message with the given ID.
     *
     * @param id The ID of the sample message to return
     * @return The sample message.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<SampleMessageMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of sample messages. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/sampleMessages?name=MySampleMessage</pre></div>
     * <p>Returns sample message with name "MySampleMessage".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param operationNames  Operation filter
     * @param serviceIds      Service ID filter
     * @return A list of sample messages. If the list is empty then no sample messages were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<SampleMessageMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "operationName","service.id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("operationName") List<String> operationNames,
            @QueryParam("service.id") List<Goid> serviceIds) {

        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "operationName","service.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }

        if (operationNames != null && !operationNames.isEmpty()) {
            filters.put("operationName", (List) operationNames);
        }
        if (serviceIds != null && !serviceIds.isEmpty()) {
            filters.put("serviceGoid", (List) serviceIds);
        }

        return super.list(convertSort(sort), ascendingSort,
                filters.map());
    }

    private String convertSort(String sort) {
        if (sort == null) return null;
        switch (sort) {
            case "protocol":
                return "scheme";
            default:
                return sort;
        }
    }

    /**
     * Creates or Updates an existing sample message. If a sample message with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Sample message to create or update
     * @param id       ID of the sample message to create or update
     * @return A reference to the newly created or updated sample message.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(SampleMessageMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing sample message.
     *
     * @param id The ID of the sample message to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example sample message that can be used as a reference for what sample message objects
     * should look like.
     *
     * @return The template sample message.
     */
    @GET
    @Path("template")
    public Item<SampleMessageMO> template() {
        SampleMessageMO emailListenerMO = ManagedObjectFactory.createSampleMessageMO();
        emailListenerMO.setName("TemplateSampleMessage");
        emailListenerMO.setServiceId(Goid.DEFAULT_GOID.toString());
        emailListenerMO.setOperation("operation");
        emailListenerMO.setXml("<xml/>");
        return super.createTemplateItem(emailListenerMO);
    }
}
