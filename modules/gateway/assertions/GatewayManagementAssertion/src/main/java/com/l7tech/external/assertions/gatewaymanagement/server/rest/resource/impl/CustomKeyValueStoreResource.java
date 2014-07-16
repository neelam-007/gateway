package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CustomKeyValueStoreAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CustomKeyValueStoreTransformer;
import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * A Custom key value is an item that can be stored by a custom assertion.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + CustomKeyValueStoreResource.customKeyValue_URI)
@Singleton
public class CustomKeyValueStoreResource extends RestEntityResource<CustomKeyValueStoreMO, CustomKeyValueStoreAPIResourceFactory, CustomKeyValueStoreTransformer> {

    protected static final String customKeyValue_URI = "customKeyValues";

    @Override
    @SpringBean
    public void setFactory(CustomKeyValueStoreAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(CustomKeyValueStoreTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new custom key value.
     *
     * @param resource The custom key value to create
     * @return A reference to the newly created custom key value
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(CustomKeyValueStoreMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a custom key value with the given ID.
     *
     * @param id The ID of the custom key value to return
     * @return The custom key value.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<CustomKeyValueStoreMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of custom key values. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent">/restman/1.0/customKeyValues?key=MyKey</div>
     * <p>Returns custom key value with key "MyKey".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort  Key to sort the list by.
     * @param order Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *              ascending if not specified
     * @param keys  Key filter
     * @return A list of custom key values. If the list is empty then no custom key values were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<CustomKeyValueStoreMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("key") List<String> keys) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("key"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (keys != null && !keys.isEmpty()) {
            filters.put("name", (List) keys);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing custom key value. If a custom key value with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Custom key value to create or update
     * @param id       ID of the custom key value to create or update
     * @return A reference to the newly created or updated custom key value.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(CustomKeyValueStoreMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing custom key value.
     *
     * @param id The ID of the custom key value to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example custom key value that can be used as a reference for what custom key
     * value objects should look like.
     *
     * @return The template custom key value.
     */
    @GET
    @Path("template")
    public Item<CustomKeyValueStoreMO> template() {
        CustomKeyValueStoreMO keyValueMO = ManagedObjectFactory.createCustomKeyValueStore();
        keyValueMO.setKey("TemplateKey");
        keyValueMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        keyValueMO.setValue("TemplateValue".getBytes());
        return super.createTemplateItem(keyValueMO);
    }
}
