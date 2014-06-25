package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RoleAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.RoleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The rbac role resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + RoleResource.ROLES_URI)
@Singleton
public class RoleResource extends RestEntityResource<RbacRoleMO, RoleAPIResourceFactory, RoleTransformer> {

    protected static final String ROLES_URI = "roles";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(RoleAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(RoleTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Access to the rbac role assignments resource
     * @param id The ID of the role to change assignments for.
     * @return The role assignments resource
     */
    @Path("{id}/assignments")
    public RoleAssignmentsResource assignments(@PathParam("id") String id) {
        return resourceContext.initResource(new RoleAssignmentsResource(id));
    }

    /**
     * Creates a new role
     *
     * @param resource The role to create
     * @return a reference to the newly created role
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(RbacRoleMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a role.
     *
     * @param id The id of the role to return
     * @return The role.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<RbacRoleMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * This will return a list of role references. A sort can be specified to allow the resulting list to be sorted in
     * either ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/roles?name=MyRole
     * <p/>
     * Returns role with name = "MyRole"
     * <p/>
     * /restman/roles?userCreated=true&name=MyDevRole&name=MyProdRole
     * <p/>
     * Returns roles that are user created with name either "MyDevRole" or "MyProdRole"
     * <p/>
     * If a parameter is not a valid search value an error will be returned.
     *
     * @param sort        the key to sort the list by.
     * @param order       the order to sort the list. true for ascending, false for descending. null implies ascending
     * @param names       The name filter
     * @param userCreated The userCreated filter
     * @return A list of roles. If the list is empty then no roles were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<RbacRoleMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("userCreated") Boolean userCreated) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "userCreated"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (userCreated != null) {
            filters.put("userCreated", (List) Arrays.asList(userCreated));
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Updates an existing role
     *
     * @param resource The updated role
     * @param id       The id of the role to update
     * @return a reference to the newly updated role
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(RbacRoleMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing role.
     *
     * @param id The id of the role to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * This will return a template, example role that can be used as a reference for what role objects should look
     * like.
     *
     * @return The template role.
     */
    @GET
    @Path("template")
    public Item<RbacRoleMO> template() {
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setName("TemplateRole");
        roleMO.setDescription("This is an example description");

        RbacRolePermissionMO permissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        permissionMO.setEntityType("ExampleEntity");
        permissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);

        RbacRolePredicateMO rolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();
        rolePredicateMO.setType(RbacRolePredicateMO.Type.AttributePredicate);
        rolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("ExampleAttributeName", "ExampleAttributeValue").map());
        permissionMO.setScope(Arrays.asList(rolePredicateMO));

        roleMO.setPermissions(Arrays.asList(permissionMO));

        RbacRoleAssignmentMO roleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
        roleAssignmentMO.setEntityType("User or Group");
        roleAssignmentMO.setIdentityName("Name");
        roleAssignmentMO.setProviderId("ProviderID");

        roleMO.setAssignments(Arrays.asList(roleAssignmentMO));
        return super.createTemplateItem(roleMO);
    }
}
