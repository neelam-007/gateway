package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.util.CollectionUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * This is a provider for dependencies. It finds the dependencies of entities that can have dependencies.
 */
@Provider
@Path(DependencyResource.Version_URI + "dependencies")
public class DependencyResource {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private DependencyTransformer transformer;

    @SpringBean
    private RbacAccessService rbacAccessService;

    @Context
    private UriInfo uriInfo;

    private EntityHeader entityHeader;

    public static enum ReturnType {
        List, Tree
    }

    public DependencyResource() {
        //TODO: need a way to specify all gateway dependencies.
    }

    public DependencyResource(EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    /**
     * Returns the dependency analysis for the resource
     *
     * @return The dependencies of this entity.
     * @throws FindException
     *
     * @title Get Dependencies
     */
    @GET
    public Item get() throws FindException {
        rbacAccessService.validateFullAdministrator();
        if (entityHeader == null) {
            throw new IllegalStateException("Cannot find dependencies, no entity set.");
        }

        return new ItemBuilder<DependencyListMO>(entityHeader.toString() + " dependencies", "Dependency")
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .setContent(transformer.toDependencyListObject(dependencyAnalyzer.getDependencies(entityHeader, CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false).map())))
                .build();
    }
}
