package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ext.Provider;

/**
 * The rbac role resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RoleResource.ROLES_URI)
public class RoleResource extends RestWsmanEntityResource<RbacRoleMO, RbacRoleResourceFactory> {

    protected static final String ROLES_URI = "roles";

    @Override
    @SpringBean
    public void setFactory(RbacRoleResourceFactory factory) {
        super.factory = factory;
    }

    @Path("{id}/assignments")
    public RoleAssignmentsResource assignment(@PathParam("id") String id, @QueryParam("idType") @DefaultValue("id") IdType idType){
        return new RoleAssignmentsResource(buildSelectorMap(id, idType), factory);
    }
}
