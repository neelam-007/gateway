package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PasswordPolicyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PasswordPolicyTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PasswordPolicyMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Password Rules
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PasswordPolicyResource.passwordPolicy_URI)
@Singleton
public class PasswordPolicyResource extends RestEntityResource<PasswordPolicyMO, PasswordPolicyAPIResourceFactory, PasswordPolicyTransformer> {

    protected static final String passwordPolicy_URI = "passwordPolicy";

    @Override
    @SpringBean
    public void setFactory(PasswordPolicyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PasswordPolicyTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new password rule
     *
     * @param resource The password rule to create
     * @return A reference to the newly created password rule
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(PasswordPolicyMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a password rule with the given ID.
     *
     * @param id The ID of the password rule to return
     * @return The secure password.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<PasswordPolicyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }




    /**
     * Creates or Updates an existing password rule. If password rule with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Password rule to create or update
     * @param id       ID of the password rule to create or update
     * @return A reference to the newly created or updated secure password.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(PasswordPolicyMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing password rule.
     *
     * @param id The ID of the password rule to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example password rule that can be used as a reference for what password rule
     * objects should look like.
     *
     * @return The template password rule.
     */
    @GET
    @Path("template")
    public Item<PasswordPolicyMO> template() {
        PasswordPolicyMO passwordPolicyMO = ManagedObjectFactory.createPasswordPolicy();
        passwordPolicyMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("serializedProps", null)
                .map());
        return super.createTemplateItem(passwordPolicyMO);
    }
}
