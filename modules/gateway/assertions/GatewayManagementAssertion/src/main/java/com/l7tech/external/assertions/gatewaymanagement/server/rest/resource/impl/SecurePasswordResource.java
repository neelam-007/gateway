package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurePasswordAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SecurePasswordTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Secure passwords are used to securely store passwords and plain text PEM private keys in the Gateway database.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SecurePasswordResource.securePassword_URI)
@Singleton
public class SecurePasswordResource extends RestEntityResource<StoredPasswordMO, SecurePasswordAPIResourceFactory, SecurePasswordTransformer> {

    protected static final String securePassword_URI = "passwords";

    @Override
    @SpringBean
    public void setFactory(SecurePasswordAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(SecurePasswordTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new secure password
     *
     * @param resource The secure password to create
     * @return A reference to the newly created secure password
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(StoredPasswordMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a secure password with the given ID.
     *
     * @param id The ID of the secure password to return
     * @return The secure password.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<StoredPasswordMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of secure passwords. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/passwords?name=MyPassword</pre></div>
     * <p>Returns secure password with name "MySFTPPollingListener".</p>
     * <div class="code indent"><pre>/restman/1.0/passwords?type=Password&name=MyPassword&name=MyOtherPassword</pre></div>
     * <p>Returns secure password of Password type with name either "MyPassword" or
     * "MyOtherPassword"</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort  Key to sort the list by
     * @param order Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *              ascending if not specified
     * @param names Name filter
     * @param types Type filter
     * @return A list of secure passwords. If the list is empty then no secure passwords were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<StoredPasswordMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("type") @ChoiceParam({"Password", "PEM Private Key"}) List<String> types) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "type"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (types != null && !types.isEmpty()) {
            filters.put("type", (List) convertTypes(types));
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    private List<SecurePassword.SecurePasswordType> convertTypes(List<String> types) {
        List<SecurePassword.SecurePasswordType> passwordTypes = new ArrayList<>(types.size());
        for (String typeString : types) {
            switch (typeString) {
                case "Password":
                    passwordTypes.add(SecurePassword.SecurePasswordType.PASSWORD);
                    break;
                case "PEM Private Key":
                    passwordTypes.add(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                    break;
                default:
                    throw new InvalidArgumentException("type", "Type is expected to be either 'Password' or 'PEM Private Key'");
            }
        }
        return passwordTypes;
    }

    /**
     * Creates or Updates an existing secure password. If a secure password with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Secure password to create or update
     * @param id       ID of the secure password to create or update
     * @return A reference to the newly created or updated secure password.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(StoredPasswordMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing secure password.
     *
     * @param id The ID of the secure password to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example secure password that can be used as a reference for what secure password
     * objects should look like.
     *
     * @return The template secure password.
     */
    @GET
    @Path("template")
    public Item<StoredPasswordMO> template() {
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("Template Name");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("description", "My Password Description")
                .put("type", "Password")
                .map());
        return super.createTemplateItem(storedPasswordMO);
    }
}
