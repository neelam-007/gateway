package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RoleRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * The rbac role resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RoleResource.ROLES_URI)
public class RoleResource extends RestEntityResource<RbacRoleMO, RoleRestResourceFactory> {

    protected static final String ROLES_URI = "roles";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(RoleRestResourceFactory factory) {
        super.factory = factory;
    }

    @Path("{id}/assignments")
    public RoleAssignmentsResource assignment(@PathParam("id") String id){
        return resourceContext.initResource(new RoleAssignmentsResource(id));
    }
}
