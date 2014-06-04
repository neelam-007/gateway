package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.CollectionUtils;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * This is a provider for dependencies. It finds the dependencies of entities that can have dependencies.
 */
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

    private final EntityHeader entityHeader;

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
    public Item get() throws FindException, CannotRetrieveDependenciesException {
        rbacAccessService.validateFullAdministrator();
        if (entityHeader == null) {
            throw new IllegalStateException("Cannot find dependencies, no entity set.");
        }
        DependencyListMO dependencyListMO = transformer.convertToMO( dependencyAnalyzer.getDependencies(entityHeader, CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false).map()));
        return new ItemBuilder<>(transformer.convertToItem(dependencyListMO))
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }
}
